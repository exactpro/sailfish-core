/******************************************************************************
 * Copyright 2009-2018 Exactpro (Exactpro Systems Limited)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.exactpro.sf.scriptrunner.impl;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.xmlbeans.impl.common.XMLChar;

import com.exactpro.sf.aml.AMLBlockType;
import com.exactpro.sf.aml.generator.AggregateAlert;
import com.exactpro.sf.aml.script.CheckPoint;
import com.exactpro.sf.center.IVersion;
import com.exactpro.sf.center.impl.SFLocalContext;
import com.exactpro.sf.comparison.ComparisonResult;
import com.exactpro.sf.comparison.Convention;
import com.exactpro.sf.comparison.Formatter;
import com.exactpro.sf.scriptrunner.IReportStats;
import com.exactpro.sf.scriptrunner.IScriptReport;
import com.exactpro.sf.scriptrunner.LoggerRow;
import com.exactpro.sf.scriptrunner.MessageLevel;
import com.exactpro.sf.scriptrunner.OutcomeCollector;
import com.exactpro.sf.scriptrunner.ReportEntity;
import com.exactpro.sf.scriptrunner.ReportUtils;
import com.exactpro.sf.scriptrunner.ScriptContext;
import com.exactpro.sf.scriptrunner.ScriptRunException;
import com.exactpro.sf.scriptrunner.StatusDescription;
import com.exactpro.sf.scriptrunner.reporting.xml.XmlMessageLevelType;
import com.exactpro.sf.util.BugDescription;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

public class XmlStreamReport implements IScriptReport {

	private final XMLStreamWriter writer;

    private final BufferedOutputStream buffWriter;

	private final DatatypeFactory dtFactory;

	private Context context;

	private boolean isActionCreated;

    private IReportStats reportStats = new ReportStats();

    private ListMultimap<KnownBugStatus, BugDescription> actionBugs = ArrayListMultimap.create();
    private ListMultimap<KnownBugStatus, BugDescription> testCaseBugs = ArrayListMultimap.create();
    private ListMultimap<KnownBugStatus, BugDescription> reportBugs = ArrayListMultimap.create();

	/**
     * Operation with this collection should be synchronized
     */
    private final List<List<String>> warningsTable = new ArrayList<>();

	private final ThreadLocal<DateFormat> dateFormat = new ThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            return new SimpleDateFormat("yyyyMMdd-HH:mm:ss.SSS");
        };
    };

	private class Context {

        final Context prev;
        final ContextType cur;

        Context(Context prev, ContextType current) {
            this.prev = prev;
            this.cur = current;
        }
    }

	private enum ContextType {
        SCRIPT, TESTCASE, ACTION, ACTIONGROUP;
	}

	private enum KnownBugStatus {
	    REPRODUCED, NOT_REPRODUCED;
    }

	public XmlStreamReport(String fileName) {

		if (fileName == null) {
            throw new NullPointerException("File name wasn't set");
		}

		try {

			this.dtFactory = DatatypeFactory.newInstance();

			XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
            this.buffWriter = new BufferedOutputStream(new FileOutputStream(fileName));
			this.writer = outputFactory.createXMLStreamWriter(buffWriter, "UTF-8");

			this.context = null;

		} catch (Exception e) {
			throw new ScriptRunException(e);
		}
	}

	@Override
	public void createReport(ScriptContext scriptContext, String name, String description, long scriptRunId,
			String environmentName, String userName) {

		String hostName = null;

        try {

            InetAddress addr = InetAddress.getLocalHost();
            hostName = addr.getHostName();

        } catch (UnknownHostException e) {
            hostName = "unknown";
        }

		try {

			this.writer.writeStartDocument("UTF-8", "1.0");

			this.writer.writeStartElement("FunctionalReport");

			this.writer.writeAttribute("ScriptName", name);
			this.writer.writeAttribute("host", hostName);
			this.writer.writeAttribute("user", userName);
			this.writer.writeAttribute("ScriptRunId", Long.toString(scriptRunId));

			String version = SFLocalContext.getDefault().getVersion();
			String branchName = SFLocalContext.getDefault().getBranchName();

			if (!StringUtils.isEmpty(branchName)) {
				version += " [branch: " + branchName + "]";
			}

			writeCharElement("Version", version);

            if (SFLocalContext.getDefault().getPluginVersions().size() > 1) {

                try {
                    this.writer.writeStartElement("Plugins");

                    for (IVersion IVersion : SFLocalContext.getDefault().getPluginVersions()) {

                        if (IVersion.isGeneral()) {
                            continue;
                        }

                        try {
                            this.writer.writeStartElement("Plugin");
                            writeCharElement("Name", IVersion.getAlias());
                            writeCharElement("Version", IVersion.buildVersion());
                        } finally {
                            this.writer.writeEndElement();
                        }
                    }
                } finally {
                    this.writer.writeEndElement();
                }
            }

			writeCharElement("StartTime", convert(new Date()).toXMLFormat());

			writeCharElement("Description", replaceInvalidCharacters(description));

			this.context = new Context(null, ContextType.SCRIPT);

		} catch (XMLStreamException e) {
			throw new ScriptRunException("Could not write xml report", e);
		}
	}

	@Override
    public void createTestCase(String reference, String description, int order, int matrixOrder, String tcId, int tcHash, AMLBlockType type) {

		this.reportStats = new ReportStats();

		if (!ContextType.SCRIPT.equals(this.context.cur)) {
			throw new RuntimeException("Incorrect report state = [" + this.context.cur + "]");
		}

		try {
		    testCaseBugs.clear();

			this.writer.writeStartElement("TestCase");

            this.writer.writeAttribute("TestCaseName", ReportUtils.generateTestCaseName(reference, matrixOrder, type));

			writeCharElement("StartTime", convert(new Date()).toXMLFormat());

			writeCharElement("Order", Integer.toString(order));

			writeCharElement("MatrixOrder", Integer.toString(matrixOrder));

			writeCharElement("Id", tcId);

			writeCharElement("Hash", Integer.toString(tcHash));

			if (!StringUtils.isEmpty(description)) {
				writeCharElement("Description", replaceInvalidCharacters(description));
			}

			if (!StringUtils.isEmpty(tcId)) {
				writeCharElement("Id", tcId);
			}

			this.context = new Context(context, ContextType.TESTCASE);

		} catch (XMLStreamException e) {
			throw new ScriptRunException("Could not write xml report", e);
		}
	}

	@Override
	public void closeTestCase(StatusDescription status) {

		if (!ContextType.TESTCASE.equals(this.context.cur)) {
			throw new RuntimeException("Incorrect report state = [" + this.context.cur + "]");
		}

		try {

			this.writer.writeStartElement("Status");

			writeCharElement("Status", status.getStatus().name());
			writeCharElement("Description", replaceInvalidCharacters(status.getDescription()));

			this.writer.writeEndElement();

			writeCharElement("FinishTime", convert(new Date()).toXMLFormat());

			writeKnownBugsTable(testCaseBugs);

			this.writer.writeEndElement();

			this.context = context.prev;

			this.reportStats.updateTestCaseStatus(status.getStatus());

		} catch (XMLStreamException e) {
			throw new ScriptRunException("Could not write xml report", e);
		}
	}

    @Override
	public void addAlerts(Collection<AggregateAlert> aggregatedAlerts) {
		if (aggregatedAlerts == null || aggregatedAlerts.isEmpty()) {
			return;
		}

		synchronized (warningsTable) {
			// rows
			for (AggregateAlert aggregatedAlert : aggregatedAlerts) {
				warningsTable.add(Arrays.asList(
						aggregatedAlert.getType().toString(),
						aggregatedAlert.joinLines(),
						aggregatedAlert.getColumn(),
						aggregatedAlert.getMessage()));
			}
		}
	}

    private void writeParameter(ReportEntity entity) throws XMLStreamException {

        try {
            this.writer.writeStartElement("Parameter");

            writeCharElement("Name", entity.getName());

			if (!entity.hasFields()) {
                if (entity.getValue() != null) {
                    writeCharElement("Value", replaceInvalidCharacters(Formatter.formatForHtml(entity.getValue(), true)));
                } else {
                    writeCharElement("Value", "null");
                }
            } else {
                try {
                    this.writer.writeStartElement("SubParameters");

					for (ReportEntity element : entity) {
                        writeParameter(element);
                    }

                } finally {
                    this.writer.writeEndElement();
                }
            }
        } finally {
            this.writer.writeEndElement();
        }
	}

	private String replaceInvalidCharacters(String s) {
	    if(s == null) {
	        return "null";
	    }

		char[] res = s.toCharArray();

		for (int i = 0; i < res.length; i++) {
		    if (XMLChar.isInvalid(res[i])) {
				// in any case JAXB failed to parse sequences
	            // '&#xhhhh' with 'incorrect' codes
				res[i++] = Convention.CONV_MISSED_STRING.charAt(0);
			}
		}
		return new String(res);
	}

	@Override
	public boolean isActionCreated() throws UnsupportedOperationException {
		return this.isActionCreated;
	}

    @Override
    public void createAction(String name, String serviceName, String action, String msg, String description, Object inputParameters, CheckPoint checkPoint, String tag, int hash,
                             List<String> verificationsOrder) {
		if (!ContextType.TESTCASE.equals(this.context.cur) &&
                !ContextType.ACTION.equals(this.context.cur)) {

			throw new RuntimeException("Incorrect report state = [" + this.context.cur + "]");
		}

		try {

		    actionBugs.clear();

			if (ContextType.TESTCASE.equals(this.context.cur)) {
				this.writer.writeStartElement("TestSteps");
			} else {
				this.writer.writeStartElement("SubSteps");
            }

			this.writer.writeStartElement("Action");

			writeCharElement("StartTime", convert(new Date()).toXMLFormat());
			writeCharElement("Name", name);
			writeCharElement("Description", replaceInvalidCharacters(description));

			if (inputParameters != null) {

                try {
                    this.writer.writeStartElement("InputParameters");

					writeParameter(new ReportEntity("Parameters", inputParameters));
                } finally {
                    this.writer.writeEndElement();
                }
            }

			context = new Context(context, ContextType.ACTION);
            isActionCreated = true;

		} catch (XMLStreamException e) {
			throw new ScriptRunException("Could not write xml report", e);
		}
	}

	@Override
    public void createAction(String name, String serviceName, String action, String msg, String description, List<Object> inputParameters, CheckPoint checkPoint, String tag, int hash,
                             List<String> verificationsOrder) {
		if (!ContextType.TESTCASE.equals(this.context.cur) &&
                !ContextType.ACTION.equals(this.context.cur)) {

			throw new RuntimeException("Incorrect report state = [" + this.context.cur + "]");
		}

		try {

		    actionBugs.clear();

		    if (ContextType.TESTCASE.equals(this.context.cur)) {
				this.writer.writeStartElement("TestSteps");
			} else {
				this.writer.writeStartElement("SubSteps");
            }

			this.writer.writeStartElement("Action");

			writeCharElement("StartTime", convert(new Date()).toXMLFormat());
			writeCharElement("Name", name);
			writeCharElement("Description", replaceInvalidCharacters(description));

			if (inputParameters != null) {

                try {
                    this.writer.writeStartElement("InputParameters");

                    for (Object param : inputParameters) {
						writeParameter(new ReportEntity("Parameter", param));
                    }

                } finally {
                    this.writer.writeEndElement();
                }
            }

			context = new Context(context, ContextType.ACTION);
            isActionCreated = true;

		} catch (XMLStreamException e) {
			throw new ScriptRunException("Could not write xml report", e);
		}
	}

	@Override
    public void closeAction(StatusDescription status, Object actionResult) {
		try {

			writeStatus(status);

			writeCharElement("FinishTime", convert(new Date()).toXMLFormat());

			writeKnownBugsTable(actionBugs);

			this.writer.writeEndElement(); //Action
			this.writer.writeEndElement(); //Steps

			this.context = context.prev;

            // Ignore outcome actions
            if (status.isUpdateTestCaseStatus()) {
                this.reportStats.updateActions(status.getStatus());
            }

            this.isActionCreated = false;

		} catch (XMLStreamException e) {
			throw new ScriptRunException("Could not write xml report", e);
		}
	}

    @Override
    public void openGroup(String name, String description) {
        if(!ContextType.ACTION.equals(this.context.cur) && !ContextType.ACTIONGROUP.equals(this.context.cur)) {
            throw new RuntimeException("Incorrect report state = [" + this.context.cur + "]");
        }
        try {
            this.writer.writeStartElement("ActionGroup");
            writeCharElement("Name", name);
            writeCharElement("Description", replaceInvalidCharacters(description));
            this.context = new Context(context, ContextType.ACTIONGROUP);
        } catch (XMLStreamException e) {
            throw new ScriptRunException("Could not write xml report", e);
        }
    }

    @Override
    public void closeGroup(StatusDescription status) {
        if (!ContextType.ACTIONGROUP.equals(this.context.cur)) {
            throw new RuntimeException("Incorrect report state = [" + this.context.cur + "]");
        }
        try {
            this.writer.writeEndElement();
            this.context = context.prev;
        } catch (XMLStreamException e) {
            throw new ScriptRunException("Could not write xml report", e);
        }
    }

    @Override
    public void createVerification(String name, String description, StatusDescription status, ComparisonResult result) {

        if (!ContextType.TESTCASE.equals(this.context.cur) && !ContextType.ACTION.equals(this.context.cur)
                && !ContextType.ACTIONGROUP.equals(this.context.cur)) {

            throw new RuntimeException("Incorrect report state = [" + this.context.cur + "]");
        }

        try {

            if (result != null) {
                Set<BugDescription> reproducesBugs = result.getReproducedBugs();
                Iterable<BugDescription> difference = Sets.difference(result.getAllKnownBugs(), reproducesBugs);
    
                reportBugs.putAll(KnownBugStatus.REPRODUCED, reproducesBugs);
    
                reportBugs.putAll(KnownBugStatus.NOT_REPRODUCED, difference);
    
                testCaseBugs.putAll(KnownBugStatus.REPRODUCED, reproducesBugs);
                testCaseBugs.putAll(KnownBugStatus.NOT_REPRODUCED, difference);
    
                actionBugs.putAll(KnownBugStatus.REPRODUCED, reproducesBugs);
                actionBugs.putAll(KnownBugStatus.NOT_REPRODUCED, difference);
            }

            try {
                if (ContextType.TESTCASE.equals(this.context.cur)) {
                    this.writer.writeStartElement("TestSteps");
                } else {
                    this.writer.writeStartElement("SubSteps");
                }

                this.writer.writeStartElement("Verification");

                writeCharElement("Name", name);
                writeCharElement("Description", replaceInvalidCharacters(description));

                writeStatus(status);

                if (result != null) {
                    writeComparisonTable(result);
                }
            } finally {
                this.writer.writeEndElement(); //Verification
                this.writer.writeEndElement(); //Steps
            }

			if (status.isUpdateTestCaseStatus()) {
                this.reportStats.updateVerifications(status.getStatus());
            }

		} catch (XMLStreamException e) {
			throw new ScriptRunException("Could not write xml report", e);
		}
	}

	private void writeComparisonTable(ComparisonResult result) throws XMLStreamException {

            this.writer.writeStartElement("ComparisonTable");

		try {
			if (result.hasResults()) {
                writeComparisonResult(result);
            } else {
				for (ComparisonResult subResult : result) {
					writeComparisonResult(subResult);
                }
            }
        } finally {
            this.writer.writeEndElement();
        }
	}

	private void writeComparisonResult(ComparisonResult result) throws XMLStreamException {

        try {
            this.writer.writeStartElement("Parameter");

            writeCharElement("Name", result.getName());
            if (result.getStatus() != null) {
                writeCharElement("Result", result.getStatus().name());
            }

			if (result.hasResults()) {

                if (result.getActual() != null) {
                    writeCharElement("Actual", Formatter.formatForHtml(result.getActual(), false));
                }

				if (result.getExpected() != null) {
                    writeCharElement("Expected", Formatter.formatExpected(result));
                }

                try {
                    this.writer.writeStartElement("SubParameters");

					for (ComparisonResult subResult : result) {
						writeComparisonResult(subResult);
                    }
                } finally {
                    this.writer.writeEndElement();
                }

            } else {

                writeCharElement("Actual", Formatter.formatForHtml(result.getActual(), false));
                writeCharElement("Expected", Formatter.formatExpected(result));
				if (result.getDoublePrecision() != null) {
					writeCharElement("Precision", result.getDoublePrecision().toString());
                }
				if (result.getSystemPrecision() != null) {
					writeCharElement("SystemPrecision", result.getSystemPrecision().toString());
                }
            }
        } finally {
            this.writer.writeEndElement();
        }

	}

	private void writeStatus(StatusDescription status) throws XMLStreamException {

        try {
            this.writer.writeStartElement("Status");

            writeCharElement("Status", status.getStatus().name());
            writeCharElement("Description", replaceInvalidCharacters(status.getDescription()));

            writeException(status.getCause(), false);
        } finally {
            this.writer.writeEndElement();
        }
	}

	@Override
    public void createMessage(MessageLevel level, String... messages) {
	    createMessage(level, null, messages);
	}

	@Override
	public void createMessage(MessageLevel level, Throwable e, String... messages) {
	    if(ArrayUtils.isEmpty(messages)) {
            throw new ScriptRunException("Message array is empty");
        }

	    try {
            try {

                writeMessages(level, messages);

                if (e != null) {
                    writeException(e, false);
                }
            } finally {
                this.writer.writeEndElement(); //Info
                this.writer.writeEndElement(); //Step
            }
		} catch (XMLStreamException e1) {
			throw new ScriptRunException("Could not write xml report", e);
		}
	}

	private void writeMessages(MessageLevel level, String... messages) throws XMLStreamException {

		if (!ContextType.TESTCASE.equals(this.context.cur) &&
			!ContextType.ACTION.equals(this.context.cur) &&
                !ContextType.ACTIONGROUP.equals(this.context.cur)) {

			throw new RuntimeException("Incorrect report state = [" + this.context.cur + "]");
		}

		if (ContextType.TESTCASE.equals(this.context.cur)) {
			this.writer.writeStartElement("TestSteps");
		} else {
			this.writer.writeStartElement("SubSteps");
        }

		this.writer.writeStartElement("Info");

		this.writer.writeAttribute("Level", convert(level).name());

		for (String message : messages) {
			writeCharElement("Messages", replaceInvalidCharacters(message));
		}
	}

	@Override
	public void createException(Throwable cause) {
        if(context.cur == ContextType.SCRIPT) {
            this.reportStats = new ReportStats();
        }

        try {
			writeException(cause, false);
		} catch (XMLStreamException e) {
			throw new ScriptRunException("Could not write xml report", e);
		}
	}

	private void writeException(Throwable exception, boolean cause) throws XMLStreamException {

		if (exception == null) {
            return;
		}

        try {
            this.writer.writeStartElement(cause ? "Cause" : "Exception");

            writeCharElement("Description", Formatter.formatForHtml(exception.getClass() + ": " + replaceInvalidCharacters(exception.getMessage()), true));

            for (StackTraceElement el : exception.getStackTrace()) {

                String line = el.getClassName() + "." + el.getMethodName() + "(" + ((el.getFileName() == null) ?
                        "Unknown Source" :
                        el.getFileName() + ":" + el.getLineNumber()) + ")";

                writeCharElement("StackTrace", line);
            }

            writeException(exception.getCause(), true);
        } finally {
            this.writer.writeEndElement();
        }
	}

	@Override
    public void createTable(ReportTable table) {

		if (!ContextType.TESTCASE.equals(this.context.cur) &&
			!ContextType.ACTION.equals(this.context.cur) &&
                !ContextType.ACTIONGROUP.equals(this.context.cur) &&
			!ContextType.SCRIPT.equals(this.context.cur)) {

			throw new RuntimeException("Incorrect report state = [" + this.context.cur + "]");
		}

		try {

            try {
                if (ContextType.TESTCASE.equals(this.context.cur)) {
                    this.writer.writeStartElement("TestSteps");
                } else if (ContextType.ACTION.equals(this.context.cur) || ContextType.ACTIONGROUP.equals(this.context.cur)) {
                    this.writer.writeStartElement("SubSteps");
                }

                this.writer.writeStartElement("Info");
                this.writer.writeAttribute("Level", convert(MessageLevel.INFO).name());

                try {
                    this.writer.writeStartElement("Table");
                    this.writer.writeAttribute("Name", table.getName());

                    List<String> header = table.getHeader();

                    for (String column : header) {
                        writeCharElement("Header", column);
                    }

                    for (Map<String, String> row : table.getRows()) {

                        try {
                            this.writer.writeStartElement("Rows");

                            for (String col : header) {
                                writeCharElement("Cols", Formatter.formatForHtml(row.get(col), true));
                            }
                        } finally {
                            this.writer.writeEndElement();
                        }
                    }
                } finally {
                    this.writer.writeEndElement(); //Table
                }
            } finally {
                this.writer.writeEndElement(); //Info
                this.writer.writeEndElement(); //Steps
            }
		} catch (XMLStreamException e) {
			throw new ScriptRunException("Could not write xml report", e);
		}
	}

	@Override
	public void createLogTable(List<String> headers, List<LoggerRow> rows) {

		if (!ContextType.TESTCASE.equals(this.context.cur) &&
			!ContextType.ACTION.equals(this.context.cur) &&
                !ContextType.ACTIONGROUP.equals(this.context.cur)) {

			throw new RuntimeException("Incorrect report state = [" + this.context.cur + "]");
		}

		try {

            try {
                if (ContextType.TESTCASE.equals(this.context.cur)) {
                    this.writer.writeStartElement("TestSteps");
                } else if (ContextType.ACTION.equals(this.context.cur)) {
                    this.writer.writeStartElement("SubSteps");
                }

                this.writer.writeStartElement("Info");
                this.writer.writeAttribute("Level", convert(MessageLevel.INFO).name());

                this.writer.writeStartElement("LogTable");
                this.writer.writeAttribute("Name", "Log Messages");

                for (String header : headers) {
                    writeCharElement("Header", header);
                }

                for (int i = 0; i < rows.size(); ++i) {
                    writeLoggerRow(rows.get(i));
                }
            } finally {
                this.writer.writeEndElement(); //LogTable
                this.writer.writeEndElement(); //Info
                this.writer.writeEndElement(); //Steps
            }
		} catch (XMLStreamException e) {
			throw new ScriptRunException("Could not write xml report", e);
		}
	}

	private void writeLoggerRow(LoggerRow row) throws XMLStreamException {

        try {
            this.writer.writeStartElement("Rows");

            try {
                this.writer.writeStartElement("TimeStamp");
                writeCharElement("Time", this.dateFormat.get().format(new Date(row.getTimestamp())));
            } finally {
                this.writer.writeEndElement();
            }

            this.writer.writeStartElement("Level");
            writeCharElement("LevelName", row.getLevel().toString());
            this.writer.writeEndElement();

            writeCharElement("Thread", row.getThread());

            this.writer.writeStartElement("Class");
            writeCharElement("ClassName", row.getClazz());
            this.writer.writeEndElement();

            writeCharElement("Message", Formatter.formatForHtml(row.getMessage(), true));

            writeException(row.getEx(), false);
        } finally {
            this.writer.writeEndElement(); //Rows
        }
	}

	@Override
	public void setOutcomes(OutcomeCollector outcomes) {
		// Do nothing
	}

	@Override
	public void createLinkToReport(String linkToReport) {
		// Do nothing
	}

	@Override
	public IReportStats getReportStats() {
		return this.reportStats;
	}

	@Override
	public void flush() {
		try {
			this.writer.flush();
		} catch (XMLStreamException e) {
			throw new ScriptRunException("Could not flush xml report", e);
		}
	}

	@Override
	public void closeReport() {

		if (this.context.prev != null || !ContextType.SCRIPT.equals(this.context.cur)) {
            throw new ScriptRunException("Report filled incorrectly [" + this.context.cur + "]");
		}

		try {
			writeCharElement("FinishTime", convert(new Date()).toXMLFormat());

			writeKnownBugsTable(reportBugs);

            // this.writer.writeEndElement();

			this.writer.writeEndDocument();

			this.writer.close();

            this.buffWriter.close();

		} catch (XMLStreamException | IOException e) {
			throw new ScriptRunException("Could not close xml report", e);
		}
	}

	private void writeCharElement(String name, String value) throws XMLStreamException {

		if (value == null) {

			this.writer.writeEmptyElement(name);

		} else {
			this.writer.writeStartElement(name);
			this.writer.writeCharacters(value);
			this.writer.writeEndElement();
		}
	}

	private XMLGregorianCalendar convert(Date date) {
        GregorianCalendar calendar = new GregorianCalendar();

        calendar.setTime(date);

        return this.dtFactory.newXMLGregorianCalendar(calendar);
    }

    private void writeKnownBugsTable(Multimap<KnownBugStatus, BugDescription> bugs) throws XMLStreamException {
	    writer.writeStartElement("KnownBugs");
	    for (Map.Entry<KnownBugStatus, BugDescription> entry: bugs.entries()) {
            writeBugDescription(entry.getValue(), KnownBugStatus.REPRODUCED == entry.getKey());
        }
	    writer.writeEndElement();
    }

    private void writeBugDescription(BugDescription bugDescription, Boolean reproduced) throws XMLStreamException {
        writer.writeStartElement("BugDescription");
        writer.writeAttribute("Subject", bugDescription.getSubject().toUpperCase());
        writer.writeAttribute("Reproduced", reproduced.toString());

        List<String> list = bugDescription.getCategories().list();
        if (!list.isEmpty()) {
            for (String category : list) {
                writer.writeStartElement("Category");
                writer.writeAttribute("value", category);
                writer.writeEndElement();
            }
        }
        writer.writeEndElement();
    }

	private static XmlMessageLevelType convert(MessageLevel level) {
        if (level == MessageLevel.DEBUG)
            return XmlMessageLevelType.DEBUG;

        if (level == MessageLevel.ERROR)
            return XmlMessageLevelType.ERROR;

        if (level == MessageLevel.INFO)
            return XmlMessageLevelType.INFO;

        if (level == MessageLevel.WARN)
            return XmlMessageLevelType.WARN;

        if (level == MessageLevel.FAIL)
            return XmlMessageLevelType.FAIL;

        return null;
    }
}