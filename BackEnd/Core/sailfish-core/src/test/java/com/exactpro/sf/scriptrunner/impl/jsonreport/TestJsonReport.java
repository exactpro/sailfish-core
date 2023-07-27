/*
 * ****************************************************************************
 *  Copyright 2009-2019 Exactpro (Exactpro Systems Limited)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * ****************************************************************************
 */

package com.exactpro.sf.scriptrunner.impl.jsonreport;

import java.io.File;
import java.nio.file.Path;
import java.sql.Date;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.junit.Assert;
import org.junit.Test;

import com.exactpro.sf.aml.AMLBlockType;
import com.exactpro.sf.common.impl.messages.MapMessage;
import com.exactpro.sf.common.messages.MsgMetaData;
import com.exactpro.sf.comparison.ComparisonResult;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.scriptrunner.IScriptReport;
import com.exactpro.sf.scriptrunner.LoggerRow;
import com.exactpro.sf.scriptrunner.MessageLevel;
import com.exactpro.sf.scriptrunner.StatusDescription;
import com.exactpro.sf.scriptrunner.StatusType;
import com.exactpro.sf.scriptrunner.TestScriptDescription;
import com.exactpro.sf.scriptrunner.impl.ReportTable;
import com.exactpro.sf.scriptrunner.impl.jsonreport.beans.ReportRoot;
import com.exactpro.sf.scriptrunner.impl.jsonreport.beans.TestCase;
import com.exactpro.sf.scriptrunner.impl.jsonreport.beans.TestCaseMetadata;
import com.exactpro.sf.scriptrunner.reportbuilder.textformatter.TextColor;
import com.exactpro.sf.scriptrunner.reportbuilder.textformatter.TextStyle;
import com.exactpro.sf.scriptrunner.state.ScriptState;
import com.exactpro.sf.scriptrunner.state.ScriptStatus;
import com.exactpro.sf.util.AbstractTest;
import com.exactpro.sf.util.BugDescription;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.Sets;

public class TestJsonReport extends AbstractTest {

    private void generateReport(String workFolder) {
        TestScriptDescription testScriptDescription = new TestScriptDescription(
                null,
                Date.from(Instant.now()),
                workFolder,
                "matrix.csv",
                "1..2",
                true,
                "user");

        testScriptDescription.setState(ScriptState.RUNNING);
        testScriptDescription.setStatus(ScriptStatus.NONE);
        testScriptDescription.setContext(getScriptContext());
        testScriptDescription.setLanguageURI(SailfishURI.unsafeParse("plugin:class.name"));

        IScriptReport report = new JsonReport(-1, "report", workspaceDispatcher, testScriptDescription, serviceContext.getDictionaryManager());

        report.createReport(getScriptContext(), "name", "descr", 1, "environment", "user");

        report.createTestCase("someref", "descr", 1, 1, "id", 1, AMLBlockType.TestCase, Collections.emptySet());

        MapMessage message = new MapMessage("namespace", "messageName");
        message.addField("somefield", "somevalue");

        report.createAction(
                "1",
                "service",
                "name",
                "types",
                "descr",
                message,
                null,
                "tag",
                1,
                Arrays.asList("ver1", "ver2"),
                "outcome");

        report.openGroup("groupname", "descr");

        report.createException(new Exception("something happened"));

        report.createLinkToReport("link");

        LoggerRow row = new LoggerRow();
        row.setClazz("someclazz");
        row.setThrowable(new Exception("exception"));
        row.setLevel(Level.ERROR);
        row.setMessage("somemessage");
        row.setTimestamp(123123123);
        row.setThread("main");

        report.createLogTable(Arrays.asList("column1", "column2"), Collections.singletonList(row));

        report.createMessage(TextColor.BLACK, TextStyle.BOLD, "message");
        report.createMessage(MessageLevel.ERROR, new Exception("exception"), "message");

        report.createParametersTable(message);

        ComparisonResult result = new ComparisonResult("name");

        result.setActual(1);
        result.setExpected(1);

        BugDescription reproduced = new BugDescription("reproduced", "root", "cat1");

        result.setReproducedBugs(Sets.newHashSet(reproduced));

        result.setAllKnownBugs(Sets.newHashSet(
                new BugDescription("subject", "root", "cat1"),
                new BugDescription("subject1", "root"),
                reproduced));

        result.setStatus(StatusType.PASSED);

        result.setMetaData(new MsgMetaData("namespace", "name", Instant.now()));

        report.createVerification("name", "description", new StatusDescription(StatusType.NA, "descr"), result);

        report.closeGroup(new StatusDescription(StatusType.NA, "N/A"));

        report.closeAction(new StatusDescription(StatusType.NA, "N/A"), null);

        Map<String, String> map = new HashMap<>();
        map.put("Id", "1");
        map.put("Content", "{\"key\": \"value\"}");
        map.put("ContentJson", "{\"key\": \"value\"}");
        map.put("UnderCheckPoint", "1");
        map.put("RawMessage", "ffffff");
        map.put("From", "SF");
        map.put("To", "SUT");
        map.put("MsgName", "name");
        map.put("Timestamp", "12:00");

        report.createTable(new ReportTable("Messages",
                Arrays.asList("Id", "Content", "ContentJson", "UnderCheckPoint", "RawMessage", "From", "To", "MsgName", "Timestamp"))
                .addRow(map));

        report.closeTestCase(new StatusDescription(StatusType.NA, "N/A"));

        report.closeReport();
    }

    private void readReport(String reportDirPath) {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

        Path rootDir;
        try {
            rootDir = workspaceDispatcher.getFolder(FolderType.REPORT).toPath().resolve(reportDirPath).resolve("reportData");
        } catch (Exception e) {
            throw new Error("unable to get report root dir", e);
        }

        File rootFile = rootDir.resolve("report.json").toFile();
        ReportRoot root;
        try {
            root = mapper.readValue(rootFile, ReportRoot.class);
            Assert.assertNotNull(String.format("unable to parse report root file: %s", rootFile.getAbsolutePath()), root);
        } catch (Exception e) {
            throw new Error(String.format("unable to parse report root file: %s", rootFile.getAbsolutePath()), e);
        }

        for (TestCaseMetadata metadata : root.getMetadata()) {
            File testCase = rootDir.resolve(metadata.getJsonFileName()).toFile();

            try {
                Assert.assertNotNull(String.format("unable to parse test case file: %s", testCase.getAbsolutePath()), mapper.readValue(testCase, TestCase.class));
            } catch (Exception e) {
                throw new Error(String.format("unable to parse test case file: %s", testCase.getAbsolutePath()), e);
            }
        }
    }

    @Test
    public void testSerializationAndDeserialization() throws Throwable {
        initTestToolsTestCase();
        generateReport("report");
        readReport("report");
    }
}
