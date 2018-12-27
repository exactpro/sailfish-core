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
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.csvreader.CsvWriter;
import com.exactpro.sf.scriptrunner.TestScriptDescription;
import com.exactpro.sf.scriptrunner.reporting.xml.XmlFunctionalReport;
import com.exactpro.sf.scriptrunner.reporting.xml.XmlTestCaseType;
import com.exactpro.sf.scriptrunner.reporting.xml.XmlTestStepType;

/**
 * Created by alexey.zarovny on 1/22/15.
 */
public class ETMReportCreator extends AbstractReportCreator {

    private static final Logger logger = LoggerFactory.getLogger(ETMReportCreator.class);

    private int headersCount;

    @Override
    protected String[] createHeaders() {
        List<String> headers = new ArrayList<>();

        headers.add("id");
        headers.add("file");
        headers.add("description");
        headers.add("status");
        headers.add("failure_reason");
        headers.add("failure_action/check");
        headers.add("start_time");
        headers.add("end_time");
        headers.add("test_executor");
        headers.add("box_name");
        headers.add("services_used");

        headersCount = headers.size();
        String[] result = new String[headersCount];
        headers.toArray(result);
        return result;
    }

    @Override
    protected String[] createScriptRecord(TestScriptDescription descr) {
        String[] scriptRecord = new String[headersCount];

        String hostname = "Unknown";
        String services = descr.getServices();

        if(services != null) {
        	services = services.substring(1, services.length() - 1);
        } else {
        	services = "";
        }

        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            logger.error(e.getMessage(), e);
        }

        scriptRecord[0] = Long.toString(descr.getId());
        scriptRecord[1] = descr.getMatrixFileName();
        scriptRecord[2] = descr.getDescription();
        scriptRecord[3] = "";
        scriptRecord[4] = "";
        scriptRecord[5] = "";
        scriptRecord[6] = Long.toString(descr.getStartedTime());
        scriptRecord[7] = Long.toString(descr.getFinishedTime());
        scriptRecord[8] = descr.getUsername();
        scriptRecord[9] = hostname;
        scriptRecord[10] = services;


        return scriptRecord;
    }

    @Override
    protected void createScriptContent(XmlFunctionalReport xmlReport, CsvWriter writer) throws IOException {
        for (XmlTestCaseType tc : xmlReport.getTestcases()) {
            writer.writeRecord(createTCRecord(tc));
        }

    }

    @Override
    protected String[] createTCRecord(XmlTestCaseType tc) {
        String[] result = new String[headersCount];

        result[0] = tc.getId();
        result[1] = tc.getTestCaseName();
        result[2] = tc.getDescription();
        result[3] = tc.getStatus().getStatus().toString();
        result[4] = tc.getStatus().getDescription();
        result[5] = getFailure(tc);
        result[6] = Long.toString(tc.getStartTime().toGregorianCalendar().getTime().getTime());
        result[7] = Long.toString(tc.getFinishTime().toGregorianCalendar().getTime().getTime());
        result[8] = "";
        result[9] = "";
        result[10] = "";

        return result;
    }

    private String getFailure(XmlTestCaseType tc) {
        List<String> result = new ArrayList<>();
        for (XmlTestStepType ts : tc.getTestSteps()) {
            if (ts.getAction() != null) {
                if ("FAILED".equals(ts.getAction().getStatus().getStatus().value())) {
                    String name = ts.getAction().getName();
                    int endIndex = name.indexOf(' ');
                    if (endIndex > 0) {
                        result.add(name.substring(0, endIndex));
                    } else {
                        result.add("");
                    }
                }
            }
        }
        return result.isEmpty() ? "" : result.toString().substring(1, result.toString().length() - 1);
    }
}
