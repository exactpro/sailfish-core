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
import java.text.SimpleDateFormat;

import org.apache.commons.lang3.time.DurationFormatUtils;

import com.csvreader.CsvWriter;
import com.exactpro.sf.scriptrunner.TestScriptDescription;
import com.exactpro.sf.scriptrunner.reporting.xml.XmlFunctionalReport;
import com.exactpro.sf.scriptrunner.reporting.xml.XmlTestCaseType;

public class BaseFailReportCreator extends AbstractReportCreator {

	private static final int HEADERS_COUNT = 8;

	private long totalPassed = 0;
	private long totalFailed = 0;
	private long totalTime   = 0;

	@Override
	protected String[] createHeaders() {
		return new String[] {"id","file","description","status","passed","failed","time_in_sec","start_time"};
	}

	@Override
	protected String[] createScriptRecord(TestScriptDescription descr) {
		String[] scriptRecord = new String[HEADERS_COUNT];

		long passed = descr.getContext().getScriptProgress().getPassed();
		long failed = descr.getContext().getScriptProgress().getFailed();
		long time;

		if(descr.getStartedTime() == 0) { // not started 
			time = 0;
		} else {
			if(descr.getFinishedTime() == 0) { // started, not finished
				time = System.currentTimeMillis() - descr.getStartedTime();
			} else {  // finished
				time = descr.getFinishedTime() - descr.getStartedTime();
			}
		}

		SimpleDateFormat runTimestampFormat = new SimpleDateFormat("MMM dd 'at' HH:mm:ss.SSS");

		totalPassed += passed;
		totalFailed += failed;
		totalTime   += time;

		scriptRecord[0] = Long.toString(descr.getId());
		scriptRecord[1] = descr.getMatrixFileName();
		scriptRecord[2] = descr.getDescription();
		scriptRecord[3] = ""; 
		scriptRecord[4] = Long.toString(passed);
		scriptRecord[5] = Long.toString(failed);
		scriptRecord[6] = Long.toString(time / 1000); // in seconds
		scriptRecord[7] = runTimestampFormat.format(descr.getTimestamp());

		return scriptRecord;
	}

	@Override
	protected void createScriptContent(XmlFunctionalReport xmlReport, CsvWriter writer) throws IOException {
		for (XmlTestCaseType tc : xmlReport.getTestcases()) {
			writer.writeRecord(createTCRecord(tc));
		}
	}

	@Override 
	protected String[] createFooter() {
		String[] footer = new String[HEADERS_COUNT];
		footer[0] = "";
		footer[1] = "TOTAL:";
		footer[2] = "";
		footer[3] = "";
		footer[4] = Long.toString(totalPassed);
		footer[5] = Long.toString(totalFailed);
		footer[6] = DurationFormatUtils.formatDurationHMS(totalTime);

		return footer;
	}
	
	@Override
	protected String[] createTCRecord(XmlTestCaseType testCase) {
		String[] tcRecord = new String[4];
		tcRecord[0] = "";
		tcRecord[1] = testCase.getTestCaseName();
		tcRecord[2] = testCase.getDescription();
		tcRecord[3] = testCase.getStatus().getStatus().name();

		return tcRecord;
	}


}
