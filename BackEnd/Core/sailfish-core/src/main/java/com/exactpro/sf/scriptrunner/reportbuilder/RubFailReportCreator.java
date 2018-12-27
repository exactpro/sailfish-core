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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.csvreader.CsvWriter;
import com.exactpro.sf.scriptrunner.TestScriptDescription;
import com.exactpro.sf.scriptrunner.reporting.xml.XmlActionType;
import com.exactpro.sf.scriptrunner.reporting.xml.XmlComparisonParameterType;
import com.exactpro.sf.scriptrunner.reporting.xml.XmlComparisonTableType;
import com.exactpro.sf.scriptrunner.reporting.xml.XmlFunctionalReport;
import com.exactpro.sf.scriptrunner.reporting.xml.XmlStatusType;
import com.exactpro.sf.scriptrunner.reporting.xml.XmlTestCaseType;
import com.exactpro.sf.scriptrunner.reporting.xml.XmlTestStepType;
import com.exactpro.sf.scriptrunner.reporting.xml.XmlVerificationType;

public class RubFailReportCreator extends AbstractReportCreator {
	
	private static final Logger logger = LoggerFactory.getLogger(RubFailReportCreator.class);
	//	private final static int HEADERS_COUNT = 10;
	private final static String CLORDID = "ClOrdID";
	private final static String TAG_TEXT = "Text";
	private final static String TIMEOUT = "Timeout";
	private final static int[] newColumns = { 4, 5 }; // should be sorted
	private AbstractReportCreator baseCreator;

	public RubFailReportCreator() {
		this.baseCreator = new BaseFailReportCreator();
	}
	
	@Override
	public void init(IXMLReportCreator xmlCreator) {
		super.init(xmlCreator);
		if (this.baseCreator != null) {
			this.baseCreator.init(xmlCreator);
		}
	};
	
	@Override
	protected String[] createHeaders() {
		return insertNewColumnsValues(baseCreator.createHeaders(), new String[] {"failure_reason","text"});
	}

	@Override
	protected String[] createScriptRecord(TestScriptDescription descr) {
		return insertNewColumnsValues(baseCreator.createScriptRecord(descr));
	}

	@Override 
	protected void createScriptContent (XmlFunctionalReport xmlReport, CsvWriter writer) throws IOException {
		for (XmlTestCaseType tc : xmlReport.getTestcases()) {
			writer.writeRecord(createTCRecord(tc));
			for (String[] record : createActionRecords(tc)) {
				writer.writeRecord(record);
			}
		}
	}

	@Override
	protected String[] createTCRecord(XmlTestCaseType testCase) {
		return baseCreator.createTCRecord(testCase);
	}

	@Override
	protected String[] createFooter() {
		return insertNewColumnsValues(baseCreator.createFooter());
	}

	private List<String[]> createActionRecords(XmlTestCaseType testCase) {
		List<String[]> result = new ArrayList<String[]>();
		for (XmlTestStepType testStep: testCase.getTestSteps()) {
			String[] failedActionRecord = createActionRecord(testStep);
			if (failedActionRecord!=null)
				result.add(failedActionRecord);
		}
		return result;
	}

	private String[] createActionRecord(XmlTestStepType testStep) {

		List<XmlComparisonParameterType> finalFailedTags = null;

		XmlActionType xmlAction = testStep.getAction();
		if (xmlAction != null && xmlAction.getStatus().getStatus().equals(XmlStatusType.FAILED)) {
			String failure_reason =  testStep.getAction().getStatus().getDescription();
			String tagText = "";
			if (isWaitAction(xmlAction) && isFixActionType(xmlAction) && failure_reason.equals(TIMEOUT)) {	// differentiate Timeout errors
				try {
					for (XmlTestStepType subStep : xmlAction.getSubSteps()) {
						XmlVerificationType vrfctn = subStep.getVerification();
						if (vrfctn.getStatus().getStatus().equals(XmlStatusType.FAILED)) {
							List<XmlComparisonParameterType> currFailedTags = getFailedTagsOfCmpTable(vrfctn.getComparisonTable());
							if (currFailedTags != null) {
								if (finalFailedTags != null) {
									finalFailedTags = (finalFailedTags.size() > currFailedTags.size()) ? currFailedTags : finalFailedTags ;
								} else {
									finalFailedTags = currFailedTags;
								}
							}
						}
					}
					if (finalFailedTags != null) {
						failure_reason = formatToFailReasonString(finalFailedTags, TAG_TEXT);
						tagText = getTag(finalFailedTags, TAG_TEXT);
					}
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
					failure_reason = "Sailfish : internal problem";
				}
			}

			String[] actionRecord = new String[6];
			actionRecord[0] = "";
			actionRecord[1] = xmlAction.getName();
			actionRecord[2] = xmlAction.getDescription();
			actionRecord[3] = XmlStatusType.FAILED.name();
			actionRecord[4] = failure_reason;
			actionRecord[5] = tagText;
			return actionRecord;

		}	// end Failed action 

		return null;
	}

	private List<XmlComparisonParameterType> getFailedTagsOfCmpTable(XmlComparisonTableType cmpTable) {

		List<XmlComparisonParameterType> failedTags = new ArrayList<XmlComparisonParameterType>();
		for (XmlComparisonParameterType p : cmpTable.getParameter()) {
			if (p.getResult().equals(XmlStatusType.FAILED)) {
				if (p.getName().equals(CLORDID)) {
					return null;
				}
				failedTags.add(p);
			}
		}
		return failedTags;
	}

	private String getTag(List<XmlComparisonParameterType> failedTags, String fieldName) {
		for (XmlComparisonParameterType p : failedTags) {
			if (fieldName.equals(p.getName())) {
				return p.getActual();
			}
		}
		return null;
	}
	
	private String formatToFailReasonString (List<XmlComparisonParameterType> failedTags, String ... ignore) {
		Set<String> ingnoreSet = new HashSet<String>(Arrays.asList(ignore));
		StringBuilder builder = new StringBuilder("{Name, Expected, Actual}");
		for (XmlComparisonParameterType p : failedTags) {
			if (!ingnoreSet.contains(p.getName())) {
				builder.append(" {");
				builder.append(p.getName());
				builder.append(", ");
				builder.append(p.getExpected());
				builder.append(", ");
				builder.append(p.getActual());
				builder.append("}");
			}
		}
		return builder.toString();
	}

	private boolean isWaitAction(XmlActionType xmlAction) {
		String name = xmlAction.getName();
		Pattern p = Pattern.compile("wait", Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(name);
		return m.find();
	}

	// newValues array should be sorted
	private String[] insertNewColumnsValues(String[] baseRecord, String[] newValues) {
		String[] newRecord = new String[baseRecord.length + newColumns.length];
		String defaultValue = "";
		int k = 0;	// newColumns array index
		int l = 0;	// baseRecord array index 
		for (int i=0; i<newRecord.length; i++) {
			if ((k < newColumns.length) && (i == newColumns[k])) {
				newRecord[i] = (newValues !=null) ? newValues[k] : defaultValue;
				k++;
			} else if (l < baseRecord.length) {
				newRecord[i] = baseRecord[l];
				l++;
			}
		}
		return newRecord;
	}

	private String[] insertNewColumnsValues(String[] baseRecord) {
		return insertNewColumnsValues(baseRecord, null);
	}
}
