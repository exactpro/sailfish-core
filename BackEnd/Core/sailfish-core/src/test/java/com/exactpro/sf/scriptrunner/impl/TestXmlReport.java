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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.configuration.ConfigurationException;
import org.junit.Assert;
import org.junit.Test;

import com.exactpro.sf.aml.AMLBlockType;
import com.exactpro.sf.common.impl.messages.MapMessage;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.comparison.ComparatorSettings;
import com.exactpro.sf.comparison.ComparisonResult;
import com.exactpro.sf.comparison.MessageComparator;
import com.exactpro.sf.scriptrunner.IScriptReport;
import com.exactpro.sf.scriptrunner.MessageLevel;
import com.exactpro.sf.scriptrunner.Outcome;
import com.exactpro.sf.scriptrunner.Outcome.Status;
import com.exactpro.sf.scriptrunner.OutcomeCollector;
import com.exactpro.sf.scriptrunner.StatusDescription;
import com.exactpro.sf.scriptrunner.StatusType;
import com.exactpro.sf.scriptrunner.reporting.xml.XmlFunctionalReport;
import com.exactpro.sf.util.AbstractTest;

public class TestXmlReport extends AbstractTest {
	@Test
	public void testStreamXmlReport() throws ConfigurationException, InterruptedException, IOException, JAXBException {

		String fileName = "bin/reports/streamxmlreport.xml";
		File reportFile = new File(fileName);
		reportFile.getParentFile().mkdirs();
		reportFile.createNewFile();
		IScriptReport report = new XmlStreamReport(fileName);

		testReport(fileName, report);
	}

	private void testReport(String fileName, IScriptReport report) throws IOException, InterruptedException, JAXBException,
			FileNotFoundException, UnsupportedEncodingException {

        report.createReport(null, "FirstReport", "description", 0, null, System.getProperty("user.name"));

        report.createTestCase("FirstTestCase", "FirstTestCaseDescription", 1, 1, null, 0, AMLBlockType.TestCase);

		List<Object> inputParameters = new LinkedList<>();

		inputParameters.add(4.55);
		inputParameters.add("Max");
		inputParameters.add(5667);
		StringBuilder sb = new StringBuilder(256 * 256);
		for (int i = 0; i < 256 * 256; i++) {
			sb.append((char) i);
		}

		inputParameters.add(sb.toString());

		report.createMessage(MessageLevel.ERROR, "Message1");

		List<Object> inside = new LinkedList<>();

		inside.add(7.89);
		inside.add("Max2");
		inside.add(55667);
		inside.add(8.99);
		inside.add(-999);
		inside.add(-998);

		inputParameters.add(inside);

        report.createAction("Action1", null, "", null, "ActionDescription1", (Object)inputParameters, null, null, 0,
                            Collections.emptyList());

		report.createMessage(MessageLevel.ERROR, new String[] { "Message2", "Message3", "Message4" });

        StatusDescription descr = new StatusDescription(StatusType.PASSED, "Decs");

        report.closeAction(descr, null);

		Thread.sleep(1200);

		report.closeTestCase(descr);

        report.createTestCase("SecondTestCase", "SecondTestCaseDescription", 2, 2, null, 0, AMLBlockType.TestCase);

        report.createAction("Action1", null, "", null, "ActionDescription1", (Object)inputParameters, null, null, 0, Collections.emptyList());

		StatusDescription descr3 = new StatusDescription(StatusType.FAILED, "Descs", new EPSCommonException(
				"TestException", new RuntimeException("RunTime", new NullPointerException("Null"))), true);

		report.createMessage(MessageLevel.ERROR, new EPSCommonException("TestException",
                new RuntimeException("RunTime", new NullPointerException("Null"))), "Exception  occured");

		// invalid characters
		report.createMessage(MessageLevel.ERROR, new EPSCommonException("Test  Exception" + '\001' + '\001'), "Exception  occured");

        report.closeAction(descr3, null);

        StatusDescription descr2 = new StatusDescription(StatusType.FAILED, "Decs ");

		Thread.sleep(1566);

		report.closeTestCase(descr2);

        report.createTestCase("TestCase3", "TestCase3  description", 3, 3, null, 0, AMLBlockType.TestCase);

        StatusDescription descr5 = new StatusDescription(StatusType.FAILED, "Decs5");

        IMessage message = new MapMessage("", "FILTER");

        message.addField("Max1", "Max1");
        message.addField("Max2", "Max2");
        message.addField("Max3", "Max3");

		List<String> submessage = new ArrayList<>();
		submessage.add("1234");
		submessage.add("789");
		submessage.add("7890");
        message.addField("submessage", submessage);
        message.addField("Max4", "    Max4    with    spaces    ");
        message.addField("Max5", "\t\t\tMax5 with tabs\t\t\t");
        message.addField("Max6", "CONDITIONALLY_PASSED");
        message.addField("Max7", "CONDITIONALLY_FAILED");

        IMessage filter = new MapMessage("", "FILTER");

        filter.addField("Max1", "Max1");
        filter.addField("Max4", "Max4");
        filter.addField("Max3", "Max3");

		List<String> subfilter = new ArrayList<>();
		subfilter.add("1234");
		subfilter.add("789");
		subfilter.add("7890");
        filter.addField("submessage", subfilter);

        ComparatorSettings compSettings = new ComparatorSettings();
		// compSettings.setDoublePrecision(0.0);
        ComparisonResult res = MessageComparator.compare(message, filter, compSettings);

        for(ComparisonResult sc : res) {
			if ("CONDITIONALLY_PASSED".equals(sc.getActual())) {
				sc.setStatus(StatusType.CONDITIONALLY_PASSED);
			}
			if ("CONDITIONALLY_FAILED".equals(sc.getActual())) {
				sc.setStatus(StatusType.CONDITIONALLY_FAILED);
			}
		}

		System.out.println(res);
		Assert.assertNotNull(res);

		Outcome outcome;
        report.createVerification("Verication1", "Verification  1 Description", descr5, res);
		OutcomeCollector outcomes = new OutcomeCollector();

		outcome = new Outcome("group 1", "case 1");
		outcome.setStatus(Status.FAILED);
		outcomes.storeOutcome(outcome);
		outcomes.onOutcomeComplete("group 1", "case 1");

		outcome = new Outcome("group 1", "case 2");
		outcome.setStatus(Status.PASSED);
		outcomes.storeOutcome(outcome);
		outcomes.onOutcomeComplete("group 1", "case 2");
		outcomes.onGroupComplete("group 1");

		outcome = new Outcome("group 2", "case 1");
		outcome.setStatus(Status.FAILED);
		outcomes.storeOutcome(outcome);
		outcomes.onOutcomeComplete("group 2", "case 1");

		outcome = new Outcome("group 2", "case 2");
		outcome.setStatus(Status.PASSED);
		outcomes.storeOutcome(outcome);
		outcomes.onOutcomeComplete("group 2", "case 2");
		outcomes.onGroupComplete("group 2");

		outcome = new Outcome("group 3", "case 1");
		outcome.setStatus(Status.PASSED);
		outcomes.storeOutcome(outcome);
		outcomes.onOutcomeComplete("group 3", "case 1");
		outcomes.onGroupComplete("group 3");

		outcome = new Outcome("group 1", "case 1");
		outcome.setStatus(Status.PASSED);
		outcomes.storeOutcome(outcome);
		outcomes.onOutcomeComplete("group 1", "case 1");

		outcome = new Outcome("group 1", "case 2");
		outcome.setStatus(Status.PASSED);
		outcomes.storeOutcome(outcome);
		outcomes.onOutcomeComplete("group 1", "case 2");
		outcomes.onGroupComplete("group 1");

		report.setOutcomes(outcomes);

		ReportTable table = new ReportTable("TestTable", Arrays.asList("header1", "header2", "header3", "header4"));

		Map<String, String> row1 = new HashMap<>();

		row1.put("header1", "header1");
		row1.put("header2", "header2");
		row1.put("header3", "header3");
		row1.put("header4", "header4");

		table.addRow(row1);

		Map<String, String> row2 = new HashMap<>();

		row2.put("header1", "header1");
		row2.put("header2", "header2");
		row2.put("header3", "header3");
		row2.put("header4", "header4");

		table.addRow(row2);

		Map<String, String> row3 = new HashMap<>();

		row3.put("header1", "header1");
		row3.put("header2", "header2");
		row3.put("header3", "header3");
		row3.put("header4", "header4");

		table.addRow(row3);

		report.createTable(table);

        report.createAction("Action skipped", null, "", null, "ActionDescription1", (Object)inputParameters, null, null, 0, Collections.emptyList());

		report.createMessage(MessageLevel.ERROR, new EPSCommonException("TestException",
                new RuntimeException("RunTime", new NullPointerException("Null"))), "Exception occured");

        report.closeAction(new StatusDescription(StatusType.SKIPPED, "Action skipped"), null);

        report.closeTestCase(new StatusDescription(StatusType.FAILED, "Decs4"));

        report.createTestCase("TestCase5", "TestCase5 description", 3, 3, null, 0, AMLBlockType.TestCase);

		outcomes = new OutcomeCollector();

		outcome = new Outcome("group 1", "case 1");
		outcome.setStatus(Status.FAILED);
		outcomes.storeOutcome(outcome);
		outcomes.onOutcomeComplete("group 1", "case 1");

		report.setOutcomes(outcomes);

        report.closeTestCase(new StatusDescription(StatusType.FAILED, "Decs  5"));

		report.closeReport();

		// read report
		JAXBContext context = JAXBContextHolder.getJAXBContext();
		Unmarshaller unmarshaller = context.createUnmarshaller();

		InputStream inputStream = new FileInputStream(new File(fileName));
		Reader reader = new InputStreamReader(inputStream, "UTF-8");

		XmlFunctionalReport rep = (XmlFunctionalReport) unmarshaller.unmarshal(reader);
		Assert.assertNotNull(rep);
	}

}
