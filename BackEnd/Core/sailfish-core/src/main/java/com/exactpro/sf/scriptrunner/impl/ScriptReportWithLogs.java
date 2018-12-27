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

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.exactpro.sf.aml.AMLBlockType;
import com.exactpro.sf.aml.generator.AggregateAlert;
import com.exactpro.sf.aml.script.CheckPoint;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.services.ServiceInfo;
import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.comparison.ComparisonResult;
import com.exactpro.sf.configuration.IDictionaryManager;
import com.exactpro.sf.configuration.IEnvironmentManager;
import com.exactpro.sf.scriptrunner.IReportStats;
import com.exactpro.sf.scriptrunner.IScriptReport;
import com.exactpro.sf.scriptrunner.LoggerRow;
import com.exactpro.sf.scriptrunner.MessageLevel;
import com.exactpro.sf.scriptrunner.OutcomeCollector;
import com.exactpro.sf.scriptrunner.ReportUtils;
import com.exactpro.sf.scriptrunner.ScriptContext;
import com.exactpro.sf.scriptrunner.StatusDescription;
import com.exactpro.sf.scriptrunner.StatusType;
import com.exactpro.sf.scriptrunner.reportbuilder.textformatter.TextColor;
import com.exactpro.sf.scriptrunner.reportbuilder.textformatter.TextStyle;
import com.exactpro.sf.storage.IServiceStorage;
import com.exactpro.sf.storage.MessageFilter;
import com.exactpro.sf.storage.MessageRow;
import com.exactpro.sf.storage.util.JsonMessageConverter;

public class ScriptReportWithLogs implements IScriptReport {

	private ScriptContext scriptContext;
	private final IScriptReport report; // wrapped report
	private final Set<String> excludedMessages;

	private ReportAppender reportAppender;
	private long startTime;
	private String lastTestCaseName;

	public ScriptReportWithLogs(final IScriptReport report, final Set<String> excludedMessages) {
		this.report = report;
		this.excludedMessages = excludedMessages.isEmpty() ? excludedMessages : excludedMessages.stream()
		        .map(String::toLowerCase)
		        .collect(Collectors.toSet());
	}

	private Logger getLogger() {
		return scriptContext.getScriptConfig().getLogger();
	}

	@Override
	public void createReport(ScriptContext scriptContext, String name, String description, long scriptRunId, String environmentName, String userName) {
		this.scriptContext = scriptContext;
		report.createReport(scriptContext, name, description, scriptRunId, environmentName, userName);
	}

	@Override
	public void addAlerts(Collection<AggregateAlert> alerts) {
		report.addAlerts(alerts);
	}

	@Override
	public void closeReport() {
		report.closeReport();
	}

	@Override
	public void flush() {
		report.flush();
	}

	@Override
    public void createTestCase(String reference, String description, int order, int matrixOrder, String tcId, int tcHash, AMLBlockType type) {
        this.lastTestCaseName = ReportUtils.generateTestCaseName(reference, matrixOrder, type);

		// Add report appender:
		reportAppender = new ReportAppender();
		org.apache.log4j.Logger.getLogger("com.exactpro.sf").addAppender(reportAppender);

		startTime = System.currentTimeMillis();

        report.createTestCase(reference, description, order, matrixOrder, tcId, tcHash, type);

        getLogger().info("TestCase [{}] started", lastTestCaseName);
	}

	@Override
	public void closeTestCase(StatusDescription status) {
		// removing report appender
		org.apache.log4j.Logger.getLogger("com.exactpro.sf").removeAppender(reportAppender);

		StatusDescription descr;
		switch (status.getStatus()) {
			case PASSED:
			    descr = new StatusDescription(StatusType.PASSED, "");
			    onTestCase(descr);
                break;
			case CONDITIONALLY_PASSED:
            descr = new StatusDescription(StatusType.CONDITIONALLY_PASSED, "", scriptContext.getKnownBugs());
			    onTestCase(descr);
				break;
			case FAILED:
			case CONDITIONALLY_FAILED:
            descr = new StatusDescription(StatusType.FAILED, status.getCause().getMessage(), status.getCause(), scriptContext.getKnownBugs());
				onTestCase(descr);
				break;
			case SKIPPED:
			case NA:
				break;
		}
	}

    private void onTestCase(StatusDescription status) {

        try {
            getLogger().info("TestCase [{}] finished with status {}", lastTestCaseName, status);

            if (scriptContext.getScriptConfig().isAddMessagesToReport()) {
                addMessageTable(scriptContext.getEnvironmentManager(), scriptContext.getDictionaryManager(), report, scriptContext.getServiceList());
            }

            // Reporting log messages:
            if (reportAppender.getRows().size() > 0) {
                report.createLogTable(reportAppender.getHeader(), reportAppender.getRows());
            }
        } finally {
            report.closeTestCase(status);
        }
    }

	@Override
    public void createAction(String name, String serviceName, String action, String msg, String description, Object inputParameters, CheckPoint checkPoint, String tag, int hash,
                             List<String> verificationsOrder) {
        report.createAction(name, serviceName, action, msg, description, inputParameters, checkPoint, tag, hash, verificationsOrder);
	}

    @Override
    public void createAction(String name, String serviceName, String action, String msg, String description, List<Object> inputParameters, CheckPoint checkPoint, String tag, int hash,
                             List<String> verificationsOrder) {
        report.createAction(name, serviceName, action, msg, description, inputParameters, checkPoint, tag, hash, verificationsOrder);
    }

	@Override
    public void closeAction(StatusDescription status, Object actionResult) {
        report.closeAction(status, actionResult);
	}

    @Override
    public void openGroup(String name, String description) {
        report.openGroup(name, description);
    }

    @Override
    public void closeGroup(StatusDescription status) {
        report.closeGroup(status);
    }

	@Override
    public void createVerification(String name, String description, StatusDescription status, ComparisonResult result) {
        report.createVerification(name, description, status, result);
	}

	@Override
	public void createMessage(MessageLevel level, String... messages) {
		report.createMessage(level, messages);
	}

	@Override
	public void createMessage(MessageLevel level, Throwable e, String... messages) {
		report.createMessage(level, e, messages);
	}

    @Override
    public void createMessage(TextColor color, TextStyle style, String... messages) {
        report.createMessage(color, style, messages);
    }

	@Override
	public void createException(Throwable cause) {
	    report.createException(cause);
	}

	@Override
    public void createTable(ReportTable table) {
        report.createTable(table);
	}

	@Override
	public void createLogTable(List<String> header, List<LoggerRow> rows) {
		report.createLogTable(header, rows);
	}

    @Override
    public void createParametersTable(String messageName, Object message) {
        report.createParametersTable(messageName, message);
    }

	@Override
	public void setOutcomes(OutcomeCollector outcomes) {
		report.setOutcomes(outcomes);
	}

	@Override
	public void createLinkToReport(String linkToReport) {
		report.createLinkToReport(linkToReport);
	}

	@Override
	public IReportStats getReportStats() {
		return report.getReportStats();
	}

    @Override
    public boolean isActionCreated() {
        throw new UnsupportedOperationException(); // This method is implemented in BroadcastScriptReport.calss
    }

    private void addMessageTable(IEnvironmentManager environmentManager, IDictionaryManager dictionaryManager, IScriptReport report, List<String> serviceNames) {
		MessageFilter filter = new MessageFilter();

		filter.setStartTime(new Timestamp(startTime));
		filter.setFinishTime(new Timestamp(new Date().getTime()));
		filter.setRawMessage("hex");

        IServiceStorage serviceStorage = environmentManager.getServiceStorage();
        Set<String> servicesId = new HashSet<>();

        for(String serviceName: serviceNames) {
            ServiceInfo serviceInfo = serviceStorage.lookupService(ServiceName.parse(serviceName));

            if(serviceInfo != null) {
                servicesId.add(serviceInfo.getID());
            } else {
                throw new EPSCommonException("Unknown service: " + serviceName);
            }
        }

        filter.setServicesIdSet(servicesId);

		Iterable<MessageRow> messages = environmentManager.getMessageStorage().getMessages(-1, filter);

		if (messages != null) {
			List<String> header = new ArrayList<>();

			header.add("Timestamp");
			header.add("MsgName");
			header.add("From");
			header.add("To");
			header.add("Content");
			header.add("RawMessage");
            header.add("CheckPoint");
            header.add("UnderCheckPoint");
            header.add("ContentJson");
            header.add("Id");

			ReportTable messagesTable = new ReportTable("Messages", header);

            String underCheckPoint = StringUtils.EMPTY;

            boolean hasCheckPoints = false;

			for (MessageRow msg : messages) {
                if (this.excludedMessages.contains(StringUtils.lowerCase(msg.getMsgName()))) {
					continue;
				}

                boolean checkPoint = false;

                if ("checkpoint".equalsIgnoreCase(msg.getMsgName())) {
                    checkPoint = true;
                    IMessage iMessage = JsonMessageConverter.fromJson(msg.getJson(), dictionaryManager, true);
                    underCheckPoint = msg.getMetaDataID();
                    msg.setMsgName(iMessage.getField("Name"));
                    hasCheckPoints = true;
                }

				markUnexpected(msg, scriptContext.getUnexpectedMessage());

				Map<String, String> row = new HashMap<>();

				row.put("Timestamp", msg.getTimestamp());
				row.put("MsgName", msg.getMsgName());
				row.put("From", msg.getFrom());
				row.put("To", msg.getTo());
				row.put("RawMessage", msg.getRawMessage());
                row.put("CheckPoint", String.valueOf(checkPoint));
                row.put("UnderCheckPoint", checkPoint ? StringUtils.EMPTY : underCheckPoint);
                String finalContent = msg.getContent();
                if (msg.getRejectReason() != null) {
                    finalContent+= "\nSAILFISH REJECT REASON: " + msg.getRejectReason();
                }
                row.put("Content", finalContent);
                row.put("ContentJson", msg.getJson());
                row.put("Id", msg.getMetaDataID());

				messagesTable.addRow(row);
			}

            messagesTable.setHasCheckPoints(hasCheckPoints);

            report.createTable(messagesTable);
		}
	}

    private void markUnexpected(MessageRow message, Set<Object> unexpectedMessages) {
        if (unexpectedMessages.isEmpty())
            return;

        Iterator<?> iter = unexpectedMessages.iterator();
        while (iter.hasNext()) {
            Object next = iter.next();
            if (!(next instanceof IMessage)) {
            	continue;
            }
            IMessage unMsg = (IMessage) next;

            if (message.getMetaDataID().equals(String.valueOf(unMsg.getMetaData().getId()))) {
                String old = message.getMsgName();
                message.setMsgName(old + "\n(Unexpected)");
                iter.remove(); // mark as unexpected only once per unexpected message
				break;
            }
        }
    }

}
