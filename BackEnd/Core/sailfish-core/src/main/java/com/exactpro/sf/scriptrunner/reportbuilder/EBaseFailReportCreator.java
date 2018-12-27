/*******************************************************************************
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

import com.csvreader.CsvWriter;
import com.exactpro.sf.common.util.Pair;
import com.exactpro.sf.scriptrunner.ReportWriterOptions;
import com.exactpro.sf.scriptrunner.TestScriptDescription;
import com.exactpro.sf.scriptrunner.reporting.xml.XmlBugDescription;
import com.exactpro.sf.scriptrunner.reporting.xml.XmlCategory;
import com.exactpro.sf.scriptrunner.reporting.xml.XmlFunctionalReport;
import com.exactpro.sf.scriptrunner.reporting.xml.XmlKnownBugs;
import com.exactpro.sf.scriptrunner.reporting.xml.XmlTestCaseType;
import com.exactpro.sf.util.BugDescription;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multiset;
import com.google.common.collect.SetMultimap;
import com.mchange.v2.collection.MapEntry;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class EBaseFailReportCreator implements IReportCreator {

    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName() + "@" + Integer.toHexString(hashCode()));
    private static final int HEADERS_COUNT = 10;
    private IXMLReportCreator xmlCreator;
    private final Map<String, TCTuple> testCases = new HashMap<>();

    protected String[] createHeaders() {
        return new String[] { "id", "file", "test_case_number", "description", "status", "reproduced", "not_reproduced", "exec_time_in_sec", "start_time", "user" };
    }

    protected void createScriptContent(XmlFunctionalReport xmlReport, CsvWriter writer, TestScriptDescription descr) throws IOException {
        for (XmlTestCaseType tc : xmlReport.getTestcases()) {
            String key = descr.getMatrixFileName() + "|" + tc.getId();
            TCTuple saved = testCases.get(key);
            if (!testCases.containsKey(key) || (saved != null && tc.getStartTime().compare(saved.testCase.getStartTime()) == 1)) {
                testCases.put(key, new TCTuple(descr.getUsername(), descr.getMatrixFileName(), tc));
            }
        }
    }

    protected String[] createTCRecord(XmlTestCaseType testCase, String matrixFileName, String user) {
        String[] tcRecord = new String[HEADERS_COUNT];
        tcRecord[0] = testCase.getId();
        tcRecord[1] = matrixFileName;
        tcRecord[2] = String.valueOf(testCase.getMatrixOrder());
        tcRecord[3] = testCase.getDescription();
        tcRecord[4] = testCase.getStatus().getStatus().name();

        if (testCase.getKnownBugs() != null) {

            List<XmlBugDescription> bugs = testCase.getKnownBugs().getBugDescription();
            Multimap<String, XmlBugDescription> bugsByCategory = HashMultimap.create();

            bugs.stream().forEach(it -> it.getCategory()
                                          .forEach(category -> bugsByCategory.put(category.getValue(), it)));

            bugs.stream()
                .filter(bug -> bug.getCategory().isEmpty())
                .forEach(it -> Arrays.asList("No category").forEach(category -> bugsByCategory.put(category, it)));

            Function<Collection<XmlBugDescription>, String> joiner = descriptions -> String
                    .join(", ", descriptions.stream().map(XmlBugDescription::getSubject).distinct().collect(Collectors.toList()));

            List<String> reproduced = new ArrayList<>();
            List<String> notReproduced = new ArrayList<>();



            for (String category : bugsByCategory.keySet()) {
                Collection<XmlBugDescription> descriptions = bugsByCategory.get(category);

                List<XmlBugDescription> rp = descriptions.stream().filter(it -> it.isReproduced()).collect(Collectors.toList());
                List<XmlBugDescription> nrp = descriptions.stream().filter(it -> !it.isReproduced()).collect(Collectors.toList());

                if (!rp.isEmpty()) {
                    reproduced.add(String.format("%s: %s", category, joiner.apply(rp)));
                }
                if (!nrp.isEmpty()) {
                    notReproduced.add(String.format("%s: %s", category, joiner.apply(nrp)));
                }
            }

            Comparator<String> comparator = String::compareTo;
            reproduced.sort(comparator);
            notReproduced.sort(comparator);
            tcRecord[5] = reproduced.isEmpty() ? "" : String.join("\n", reproduced);
            tcRecord[6] = notReproduced.isEmpty()? "" : String.join("\n", notReproduced);
        }
        tcRecord[7] = String.valueOf(ChronoUnit.SECONDS.between(convertDate(testCase.getStartTime()), convertDate(testCase.getFinishTime())));
        tcRecord[8] = String.valueOf(convertDate(testCase.getStartTime()));
        tcRecord[9] = user;

        return tcRecord;
    }

    private LocalDateTime convertDate(XMLGregorianCalendar xmlGregorianCalendar) {
        return xmlGregorianCalendar.normalize().toGregorianCalendar().toZonedDateTime().toLocalDateTime();
    }

    @Override
    public void createReport(File file, List<TestScriptDescription> descriptions, ReportWriterOptions options) throws IOException {

        CsvWriter writer = new CsvWriter(new FileWriter(file), ',');

        try (AutoCloseable a = writer::close) {

            writer.writeRecord(createHeaders());

            for (TestScriptDescription descr : descriptions) {

                if (options.isWriteDetails() && !descr.isLocked()) {
                    try {
                        XmlFunctionalReport xmlReport = xmlCreator.create(descr);
                        createScriptContent(xmlReport, writer, descr);
                    } catch (JAXBException e) {
                        File xmlReportFile = xmlCreator.getReportFileName();
                        if (options.getSelectedDuration() != null) {
                            logger.error("Report {} is broken and didn't added into aggregated report", xmlReportFile, e);
                        } else {
                            logger.error("Report {} is broken", xmlReportFile, e);
                        }
                        continue;
                    }
                }
            }

            for (Map.Entry<String, TCTuple> entry : testCases.entrySet()) {
                try {
                    TCTuple value = entry.getValue();
                    writer.writeRecord(createTCRecord(value.testCase, value.matrixFileName, value.user));
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public void init(IXMLReportCreator xmlCreator) {
        this.xmlCreator = xmlCreator;
    }

    private class TCTuple {
        String user;
        String matrixFileName;
        XmlTestCaseType testCase;

        TCTuple(String user, String matrixFileName, XmlTestCaseType testCase)  {
            this.user = user;
            this.matrixFileName = matrixFileName;
            this.testCase = testCase;
        }
    }
}
