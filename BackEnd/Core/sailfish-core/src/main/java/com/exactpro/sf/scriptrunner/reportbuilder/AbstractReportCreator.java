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
package com.exactpro.sf.scriptrunner.reportbuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.csvreader.CsvWriter;
import com.exactpro.sf.scriptrunner.ReportWriterOptions;
import com.exactpro.sf.scriptrunner.TestScriptDescription;
import com.exactpro.sf.scriptrunner.reporting.xml.XmlActionType;
import com.exactpro.sf.scriptrunner.reporting.xml.XmlFunctionalReport;
import com.exactpro.sf.scriptrunner.reporting.xml.XmlParameterType;
import com.exactpro.sf.scriptrunner.reporting.xml.XmlTestCaseType;

public abstract class AbstractReportCreator implements IReportCreator {

	private static final Logger logger = LoggerFactory.getLogger(AbstractReportCreator.class);
	private static final String[] EMPTY_ROW = {""};
	private IXMLReportCreator xmlCreator;

	protected abstract String[] createHeaders();

	protected abstract String[] createScriptRecord(TestScriptDescription descr);

	// variable part of the report creation algorithm, encapsulated part
	protected abstract void createScriptContent(XmlFunctionalReport xmlReport, CsvWriter writer) throws IOException;

	@Override
	public void init(IXMLReportCreator xmlCreator) {
		this.xmlCreator = xmlCreator;
	}

	@Override
	public void createReport(File file, List<TestScriptDescription> descriptions, ReportWriterOptions options) throws IOException {

		CsvWriter writer = null;
		try {
			writer = new CsvWriter(new FileWriter(file), ',');
			writer.writeRecord(createHeaders());

			for(TestScriptDescription descr: descriptions) {
				String[] scriptRecord = createScriptRecord(descr);
				if(scriptRecord != null) {
					writer.writeRecord(scriptRecord);

					if(options.isWriteDetails() && !descr.isLocked()) {
						XmlFunctionalReport xmlReport = null;
						try {
							xmlReport = xmlCreator.create(descr);
						}
						catch (JAXBException e) {
							File xmlReportFile = xmlCreator.getReportFileName();
							if (options.getSelectedDuration() != null) {
								logger.error("Report {} is broken and didn't added into aggregated report", xmlReportFile, e);
							} else {
								logger.error("Report {} is broken", xmlReportFile, e);
							}
							String[] brokenRow = new String[] { "", "", "This report is broken" } ;
							writer.writeRecord(brokenRow);
							continue;
						}
						createScriptContent(xmlReport, writer);
					}
					writer.writeRecord(EMPTY_ROW);
				}

				writer.writeRecord(EMPTY_ROW);
				writer.flush();
			}
		} finally {
			if (writer != null) {
				writer.writeRecord(createFooter());
				writer.flush();
				writer.close();
			}
		}
	}

	protected String[] createTCRecord(XmlTestCaseType testCase) {
		return EMPTY_ROW; 	// no implementation
	}

	protected String[] createFooter() {
		return EMPTY_ROW;	// no implementation
	}

	protected boolean isFixActionType (XmlActionType xmlAction) {
		for (XmlParameterType p : xmlAction.getInputParameters().getParameter()) {
			for(XmlParameterType subp : p.getSubParameters().getParameter()) {
				if ("BeginString".equalsIgnoreCase(subp.getName())) {
					return subp.getValue() != null && "fix".equalsIgnoreCase(subp.getValue().substring(0, 3));
				}
			}
		}
		return false;
	}
}
