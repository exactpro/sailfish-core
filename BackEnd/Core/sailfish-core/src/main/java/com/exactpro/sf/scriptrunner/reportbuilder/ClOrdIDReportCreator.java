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

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.csvreader.CsvWriter;
import com.exactpro.sf.scriptrunner.TestScriptDescription;
import com.exactpro.sf.scriptrunner.reporting.xml.XmlActionType;
import com.exactpro.sf.scriptrunner.reporting.xml.XmlFunctionalReport;
import com.exactpro.sf.scriptrunner.reporting.xml.XmlParameterType;
import com.exactpro.sf.scriptrunner.reporting.xml.XmlTestCaseType;
import com.exactpro.sf.scriptrunner.reporting.xml.XmlTestStepType;

public class ClOrdIDReportCreator extends AbstractReportCreator {

	private static final int HEADERS_COUNT = 3;
	private static final String CLORDID = "ClOrdID";
	
	@Override
	protected String[] createHeaders() {
		return new String[] {"id", "file", CLORDID};
	}

	@Override
	protected String[] createScriptRecord(TestScriptDescription descr) {
		return new String[] {"", descr.getMatrixFileName(), ""};
	}

	@Override
	protected void createScriptContent(XmlFunctionalReport xmlReport, CsvWriter writer) throws IOException {
		
		for (XmlTestCaseType tc : xmlReport.getTestcases()) {
			StringBuilder tcClOrdIDs = new StringBuilder("{ ");
			for (XmlTestStepType tstep : tc.getTestSteps()) {
				XmlActionType xmlAction = tstep.getAction();
				if (xmlAction != null && isSendAction(xmlAction) && isFixActionType(xmlAction)) {
					String id = findClOrdID(xmlAction);
					if (id != null) {
						tcClOrdIDs.append(id);
						tcClOrdIDs.append(", ");
					}
				}
			}
		
			String[] tcRecord = new String [HEADERS_COUNT];
			tcRecord[0] = "";
			tcRecord[1] = tc.getTestCaseName();
			if (tcClOrdIDs.length() > 2) {
				tcClOrdIDs.delete(tcClOrdIDs.length()-2, tcClOrdIDs.length());
				tcClOrdIDs.append(" }");
				tcRecord[2] = tcClOrdIDs.toString();
			}
			writer.writeRecord(tcRecord);	
		}
	}

	private String findClOrdID(XmlActionType xmlAction) {
		for (XmlParameterType p : xmlAction.getInputParameters().getParameter()) {
			for(XmlParameterType subp : p.getSubParameters().getParameter()) {
				if (subp.getName().equals(CLORDID))
					return subp.getValue();
			}
		}
		return null;
	}

	private boolean isSendAction(XmlActionType xmlAction) {
		String name = xmlAction.getName();
		Pattern p = Pattern.compile("send", Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(name);
		return m.find();
	}

}
