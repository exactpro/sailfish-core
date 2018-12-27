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
package com.exactpro.sf.aml;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ExecutionException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.exactpro.sf.aml.generator.Alert;
import com.exactpro.sf.aml.generator.AlertType;
import com.exactpro.sf.center.impl.SFLocalContext;
import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.scriptrunner.IConnectionManager;

public class TestAML3_0 extends TestAML3Base {

	private static String VALID_TEST_PATH = "src" + File.separator + "test" + File.separator + "resources" + File.separator + "aml3_0" + File.separator + "validTest" + File.separator;
	private static String INVALID_TEST_PATH = "src" + File.separator + "test" + File.separator + "resources" + File.separator + "aml3_0" + File.separator + "invalidTest" + File.separator;

	@Before
    public void initTestAML3_0() throws InterruptedException, ExecutionException {
        IConnectionManager conManager = SFLocalContext.getDefault().getConnectionManager();
        addService(conManager, SailfishURI.unsafeParse("FAKE_CLIENT_SERVICE"), "service");
        addService(conManager, SailfishURI.unsafeParse("FAKE_CLIENT_SERVICE"), "fake");
    }

	@Test
	public void test_valid_all() throws Exception
	{
		AML aml = executeTest(VALID_TEST_PATH + "validTests.csv");

		Assert.assertEquals(0, aml.getAlertCollector().getCount(AlertType.ERROR));
        Assert.assertEquals(34, aml.getTestCases().size());
		int n = 0;
		Assert.assertEquals(3, aml.getTestCases().get(n++).getActions().size());
		Assert.assertEquals(3, aml.getTestCases().get(n++).getActions().size());
		Assert.assertEquals(4, aml.getTestCases().get(n++).getActions().size());
		Assert.assertEquals(2, aml.getTestCases().get(n++).getActions().size());
		Assert.assertEquals(4, aml.getTestCases().get(n++).getActions().size());
		Assert.assertEquals(4, aml.getTestCases().get(n++).getActions().size());
		Assert.assertEquals(13, aml.getTestCases().get(n++).getActions().size());
		Assert.assertEquals(8, aml.getTestCases().get(n++).getActions().size());
		Assert.assertEquals(6, aml.getTestCases().get(n++).getActions().size());
		Assert.assertEquals(14, aml.getTestCases().get(n++).getActions().size());
		Assert.assertEquals(10, aml.getTestCases().get(n++).getActions().size());
		Assert.assertEquals(13, aml.getTestCases().get(n++).getActions().size());
		Assert.assertEquals(6, aml.getTestCases().get(n++).getActions().size());
		Assert.assertEquals(4, aml.getTestCases().get(n++).getActions().size());
		Assert.assertEquals(6, aml.getTestCases().get(n++).getActions().size());
		Assert.assertEquals(5, aml.getTestCases().get(n++).getActions().size());
		Assert.assertEquals(44, aml.getTestCases().get(n++).getActions().size());
		Assert.assertEquals(24, aml.getTestCases().get(n++).getActions().size());
		Assert.assertEquals(28, aml.getTestCases().get(n++).getActions().size());
		Assert.assertEquals(29, aml.getTestCases().get(n++).getActions().size());
		Assert.assertEquals(26, aml.getTestCases().get(n++).getActions().size());
		Assert.assertEquals(40, aml.getTestCases().get(n++).getActions().size());
		Assert.assertEquals(29, aml.getTestCases().get(n++).getActions().size());
        Assert.assertEquals(28, aml.getTestCases().get(n++).getActions().size());
        Assert.assertEquals("fake", aml.getTestCases().get(n++).findActionByRef("s1").getServiceName());
        Assert.assertEquals(29, aml.getTestCases().get(n++).getActions().size());
        Assert.assertEquals(29, aml.getTestCases().get(n++).getActions().size());
        Assert.assertEquals(28, aml.getTestCases().get(n++).getActions().size());
        Assert.assertEquals(31, aml.getTestCases().get(n++).getActions().size());
        Assert.assertEquals(29, aml.getTestCases().get(n++).getActions().size());
        Assert.assertEquals(29, aml.getTestCases().get(n++).getActions().size());
        Assert.assertEquals(31, aml.getTestCases().get(n++).getActions().size());
        Assert.assertEquals(29, aml.getTestCases().get(n++).getActions().size());
        Assert.assertEquals(28, aml.getTestCases().get(n++).getActions().size());
        Assert.assertEquals(n, aml.getTestCases().size());

		Collection<Alert> source = aml.getAlertCollector().getAlerts();
        Assert.assertEquals("List errors size", 3, source.size());

        Alert alert = null;
        alert = new Alert(141, "", "#check_point", "Unable to check checkpoint at compile time", AlertType.WARNING);
        Assert.assertTrue(alert.toString(), source.remove(alert));
        alert = new Alert(292, "", "FInteger", "Reference to unknown column 'idleTimeout' is found in column 'FInteger': '${settings.idleTimeout}'.", AlertType.WARNING);
        Assert.assertTrue(alert.toString(), source.remove(alert));
        alert = new Alert(306, "", "MessageArray", "Reference to unknown column 'MessageArray[0]' is found in column 'MessageArray': '[${block4.MessageArray[0]}]'.", AlertType.WARNING);
        Assert.assertTrue(alert.toString(), source.remove(alert));

        Assert.assertEquals("List errors size", 0, source.size());
	}

	@Test
	@Ignore
	public void test_valid_maxSendOrders() throws Exception
	{
		AML aml = executeTest(VALID_TEST_PATH + "03_test_maxSendOrders.csv");
		Assert.assertEquals(0, aml.getAlertCollector().getCount(AlertType.ERROR));
		System.gc();
		if (aml != null)
			aml.cleanup();
		System.gc();
	}

	@Test
	public void test_newGenerator1() throws Exception
	{
		AML aml = executeTest(VALID_TEST_PATH + "14_test_newGenerator.csv");

		Assert.assertEquals(0, aml.getAlertCollector().getCount(AlertType.ERROR));
		Assert.assertEquals(1, aml.getTestCases().size());
		Assert.assertEquals(14, aml.getTestCases().get(0).getActions().size());
	}

	@Test
	public void test_newGenerator2() throws Exception
	{
		AML aml = executeTest(VALID_TEST_PATH + "15_test_newGenerator.csv");

		Assert.assertEquals(0, aml.getAlertCollector().getCount(AlertType.ERROR));
		Assert.assertEquals(1, aml.getTestCases().size());
		Assert.assertEquals(16, aml.getTestCases().get(0).getActions().size());
	}

	@Test
	public void test_newGenerator3() throws Exception
	{
		AML aml = executeTest(VALID_TEST_PATH + "16_test_newGenerator.csv");

		Assert.assertEquals(0, aml.getAlertCollector().getCount(AlertType.ERROR));
		Assert.assertEquals(3, aml.getTestCases().size());
		Assert.assertEquals(3, aml.getTestCases().get(0).getActions().size());
		Assert.assertEquals(22, aml.getTestCases().get(1).getActions().size());
		Assert.assertEquals(17, aml.getTestCases().get(2).getActions().size());
	}

	@Test
	public void testIncludeBlock() throws AMLException, IOException, InterruptedException {
        AML aml = executeTest(VALID_TEST_PATH + "IncludeBlock.csv");
        Collection<Alert> source = aml.getAlertCollector().getAlerts();

        Assert.assertEquals("List errors size", 7, source.size());
        Assert.assertEquals("Critical errors size", 0, aml.getAlertCollector().getCount(AlertType.ERROR));
        Assert.assertEquals(2, aml.getTestCases().size());
        Assert.assertEquals(17, aml.getTestCases().get(0).getActions().size());
        Assert.assertEquals(11, aml.getTestCases().get(1).getActions().size());

        Alert alert = null;
        alert = new Alert(24, "m3", "MarketDepthLevel_2_4", "Reference to unknown column 'm2' is found in column 'MarketDepthLevel_2_4': '[include1.m2.MarketDepthLevel_2_4]'.", AlertType.WARNING);
        Assert.assertTrue(alert.toString(), source.remove(alert));
        alert = new Alert(24, "m3", "MarketDepthLevel_2_4" ,"Reference to unknown column 'MarketDepthLevel_2_4' is found in column 'MarketDepthLevel_2_4': '[include1.m2.MarketDepthLevel_2_4]'.", AlertType.WARNING);
        Assert.assertTrue(alert.toString(), source.remove(alert));
        alert = new Alert(24, "m3", "Instrument", "Reference to unknown column 'm1' is found in column 'Instrument': '${include2.m1.Instrument}'.", AlertType.WARNING);
        Assert.assertTrue(alert.toString(), source.remove(alert));
        alert = new Alert(24, "m3", "Instrument", "Reference to unknown column 'Instrument' is found in column 'Instrument': '${include2.m1.Instrument}'.", AlertType.WARNING);
        Assert.assertTrue(alert.toString(), source.remove(alert));
        alert = new Alert(32, "z4", "FString", "Reference to unknown column 'c3' is found in column 'FString': '${c2.c3.z3.FString} + \"4\"'.", AlertType.WARNING);
        Assert.assertTrue(alert.toString(), source.remove(alert));
        alert = new Alert(32, "z4", "FString", "Reference to unknown column 'z3' is found in column 'FString': '${c2.c3.z3.FString} + \"4\"'.", AlertType.WARNING);
        Assert.assertTrue(alert.toString(), source.remove(alert));
        alert = new Alert(32, "z4", "FString", "Reference to unknown column 'FString' is found in column 'FString': '${c2.c3.z3.FString} + \"4\"'.", AlertType.WARNING);
        Assert.assertTrue(alert.toString(), source.remove(alert));

        Assert.assertEquals("List errors size", 0, source.size());

    }



    @Test
	public void test_invalid_all() throws Exception
	{
		try {
			executeTest(INVALID_TEST_PATH + "invalidTests.csv");
		} catch (AMLException e) {
			Collection<Alert> source = e.getAlertCollector().getAlerts();

			Alert alert = null;

			alert = new Alert(36, "", "#check_point", "Unable to check checkpoint at compile time", AlertType.WARNING);
			Assert.assertTrue(alert.toString(), source.remove(alert));
			alert = new Alert(37, "", "#check_point", "Unable to check checkpoint at compile time", AlertType.WARNING);
			Assert.assertTrue(alert.toString(), source.remove(alert));
			alert = new Alert(4, "", null, "Checkpoint referred to undefined checkpoint '123'. Please check that '123' checkpoint was defined earlier.", AlertType.ERROR);
			Assert.assertTrue(alert.toString(), source.remove(alert));
			alert = new Alert(7, "", "Side", "Unbalansed brackets in column 'Side'.", AlertType.ERROR);
			Assert.assertTrue(alert.toString(), source.remove(alert));
			alert = new Alert(8, "", "Side", "Column 'Side': Reference 'ord2' is not defined in matrix", AlertType.ERROR);
			Assert.assertTrue(alert.toString(), source.remove(alert));
			alert = new Alert(8, "", "Side", "Reference to unknown action 'ord2' is found in column 'Side': '${ord2:Side}'.", AlertType.ERROR);
			Assert.assertTrue(alert.toString(), source.remove(alert));
			alert = new Alert(9, "", "Side", "Reference is empty in column 'Side'.", AlertType.ERROR);
			Assert.assertTrue(alert.toString(), source.remove(alert));
			alert = new Alert(11, "", "Side", "Reference to row is missed in column 'Side': '${:Side}'.", AlertType.ERROR);
			Assert.assertTrue(alert.toString(), source.remove(alert));
			alert = new Alert(12, "", "Side", "Invalid reference format in column 'Side': ':'. Expected format: ${reference:column} or ${reference}.", AlertType.ERROR);
			Assert.assertTrue(alert.toString(), source.remove(alert));
			alert = new Alert(13, "", "Side", "Column 'Side': Reference to unknown column 'foo bar' in reference 'ord1:foo bar'", AlertType.ERROR);
			Assert.assertTrue(alert.toString(), source.remove(alert));
			alert = new Alert(13, "", "Side", "Reference to unknown column 'foo bar' is found in column 'Side': '${ord1:foo bar}'.", AlertType.ERROR);
			Assert.assertTrue(alert.toString(), source.remove(alert));
            alert = new Alert(16, "statistics", "VWAP", "Column 'VWAP': Reference 'statistics' is not defined in matrix", AlertType.ERROR);
			Assert.assertTrue(alert.toString(), source.remove(alert));
            alert = new Alert(16, "statistics", "VWAP", "Reference 'statistics' is not yet defined in column 'VWAP': '${statistics:Turnover}/${statistics:Volume}'.", AlertType.ERROR);
			Assert.assertTrue(alert.toString(), source.remove(alert));
            alert = new Alert(15, "", "VWAP", "Reference 'statistics' is not yet defined in column 'VWAP': '${statistics:Turnover}/${statistics:Volume}'.", AlertType.ERROR);
			Assert.assertTrue(alert.toString(), source.remove(alert));
			alert = new Alert(15, "", "VWAP", "Subaction must predefined to use references to it's fields.", AlertType.ERROR);
			Assert.assertTrue(alert.toString(), source.remove(alert));
			alert = new Alert(18, "ss1", "Unable to resolve utility function: random", AlertType.ERROR);
			Assert.assertTrue(alert.toString(), source.remove(alert));
			alert = new Alert(19, "ss2", "#static_value", "Unbalansed brackets in column '#static_value'.", AlertType.ERROR);
			Assert.assertTrue(alert.toString(), source.remove(alert));
			alert = new Alert(20, "ss3", "#static_value", "Syntaxis error in column '#static_value': missed open bracket '('.", AlertType.ERROR);
			Assert.assertTrue(alert.toString(), source.remove(alert));
            alert = new Alert(21, "ss4", "#static_value", "Invalid URI: com.exactpro.sf.actions.MiscUtils.random", AlertType.ERROR);
			Assert.assertTrue(alert.toString(), source.remove(alert));
            alert = new Alert(27, "ref1", "FInteger", "Subaction must predefined to use references to it's fields.", AlertType.ERROR);
			Assert.assertTrue(alert.toString(), source.remove(alert));
            alert = new Alert(27, "ref1", "FInteger", "Reference 'rs1' is not yet defined in column 'FInteger': '10 + ${rs1:FInteger}'.", AlertType.ERROR);
			Assert.assertTrue(alert.toString(), source.remove(alert));
            alert = new Alert(28, "ref2", "FInteger", "Subaction must predefined to use references to it's fields.", AlertType.ERROR);
			Assert.assertTrue(alert.toString(), source.remove(alert));
            alert = new Alert(28, "ref2", "FInteger", "Reference 'rs2' is not yet defined in column 'FInteger': '10 + ${rs2:FInteger}'.", AlertType.ERROR);
			Assert.assertTrue(alert.toString(), source.remove(alert));
            alert = new Alert(29, "ref3", "FInteger", "Subaction must predefined to use references to it's fields.", AlertType.ERROR);
			Assert.assertTrue(alert.toString(), source.remove(alert));
            alert = new Alert(29, "ref3", "FInteger", "Reference 'rs3' is not yet defined in column 'FInteger': '10 + ${rs3:TInteger}'.", AlertType.ERROR);
			Assert.assertTrue(alert.toString(), source.remove(alert));
            alert = new Alert(31, "ref4", "BooleanArray", "Column 'BooleanArray': Invalid collection format 'true, false, true, true'", AlertType.ERROR);
			Assert.assertTrue(alert.toString(), source.remove(alert));
            alert = new Alert(31, "ref4", "MessageArray", "Column 'MessageArray': Invalid reference format: ref1", AlertType.ERROR);
			Assert.assertTrue(alert.toString(), source.remove(alert));
            alert = new Alert(33, "ref5", "FByteEnumArray", "Column 'FByteEnumArray': Invalid collection format '1, 2, 3'", AlertType.ERROR);
			Assert.assertTrue(alert.toString(), source.remove(alert));
			alert = new Alert(37, "", "#check_point", "Undefined reference [s2] in column '#check_point': '%{s2}'.", AlertType.ERROR);
			Assert.assertTrue(alert.toString(), source.remove(alert));
			alert = new Alert(40, "m2", "InstrumentStatus", "Field should be collection InstrumentStatus in column 'InstrumentStatus': '${m1.InstrumentStatus[0]}'.", AlertType.ERROR);
			Assert.assertTrue(alert.toString(), source.remove(alert));
			alert = new Alert(41, "m3", "InstrumentStatus", "Field should be collection InstrumentStatus in column 'InstrumentStatus': '${m1.InstrumentStatus[0].field}'.", AlertType.ERROR);
			Assert.assertTrue(alert.toString(), source.remove(alert));
			alert = new Alert(41, "m3", "InstrumentStatus", "Type InstrumentStatus is not complex in the column reference fieldcolumn 'InstrumentStatus': '${m1.InstrumentStatus[0].field}'.", AlertType.ERROR);
			Assert.assertTrue(alert.toString(), source.remove(alert));
			alert = new Alert(41, "m3", "InstrumentStatus", "Reference to unknown column 'InstrumentStatus[0]' is found in column 'InstrumentStatus': '${m1.InstrumentStatus[0].field}'.", AlertType.ERROR);
			Assert.assertTrue(alert.toString(), source.remove(alert));
			alert = new Alert(42, "m4", "InstrumentStatus", "Reference to field that is not collection MarketDepthLevel_2_4 in column 'InstrumentStatus': '${m1.MarketDepthLevel_2_4.AskPrice}'.", AlertType.ERROR);
			Assert.assertTrue(alert.toString(), source.remove(alert));
			alert = new Alert(44, "", "InstrumentStatus", "Field should be collection InstrumentStatus in column 'InstrumentStatus': '${m1.InstrumentStatus[0]}'.", AlertType.ERROR);
			Assert.assertTrue(alert.toString(), source.remove(alert));
			alert = new Alert(45, "", "InstrumentStatus", "Field should be collection InstrumentStatus in column 'InstrumentStatus': '${m1.InstrumentStatus[0].field}'.", AlertType.ERROR);
			Assert.assertTrue(alert.toString(), source.remove(alert));
			alert = new Alert(45, "", "InstrumentStatus", "Type InstrumentStatus is not complex in the column reference fieldcolumn 'InstrumentStatus': '${m1.InstrumentStatus[0].field}'.", AlertType.ERROR);
			Assert.assertTrue(alert.toString(), source.remove(alert));
			alert = new Alert(45, "", "InstrumentStatus", "Reference to unknown column 'InstrumentStatus[0]' is found in column 'InstrumentStatus': '${m1.InstrumentStatus[0].field}'.", AlertType.ERROR);
			Assert.assertTrue(alert.toString(), source.remove(alert));
			alert = new Alert(46, "", "InstrumentStatus", "Reference to field that is not collection MarketDepthLevel_2_4 in column 'InstrumentStatus': '${m1.MarketDepthLevel_2_4.AskPrice}'.", AlertType.ERROR);
			Assert.assertTrue(alert.toString(), source.remove(alert));
			alert = new Alert(48, "", null, "Can't find dictionary [FIX_6_0]", AlertType.ERROR);
			Assert.assertTrue(alert.toString(), source.remove(alert));
            alert = new Alert(51, "simple", "FInteger", "Invalid reference format to static variable in column 'FInteger': 's1.Field'. Expected format: %{reference}.", AlertType.ERROR);
            Assert.assertTrue(alert.toString(), source.remove(alert));
            alert = new Alert(52, "", "FInteger", "Reference to a non-static action: simple", AlertType.ERROR);
            Assert.assertTrue(alert.toString(), source.remove(alert));
            alert = new Alert(53, "", "FInteger", "Static reference to a not generated action: sg", AlertType.ERROR);
            Assert.assertTrue(alert.toString(), source.remove(alert));
            alert = new Alert(55, null, "#service_name", "Unknown service: unknown");
            Assert.assertTrue(alert.toString(), source.remove(alert));
            alert = new Alert(56, null, "#service_name", "Unknown service: unknown");
            Assert.assertTrue(alert.toString(), source.remove(alert));
            alert = new Alert(57, null, "#service_name", "Unknown service: unknown");
            Assert.assertTrue(alert.toString(), source.remove(alert));
            alert = new Alert(58, null, "#service_name", "Unknown service: %{unknown}");
            Assert.assertTrue(alert.toString(), source.remove(alert));
            alert = new Alert(60, null, "#service_name", "Unknown service name reference: %{unknown}");
            Assert.assertTrue(alert.toString(), source.remove(alert));
            alert = new Alert(61, null, "#service_name", "Unknown service name reference: %{unknown}");
            Assert.assertTrue(alert.toString(), source.remove(alert));
            alert = new Alert(63, null, "ServiceName", "Unknown service: unknown");
            Assert.assertTrue(alert.toString(), source.remove(alert));
            alert = new Alert(64, null, "ServiceName", "Unknown service name reference: %{unknown}");
            Assert.assertTrue(alert.toString(), source.remove(alert));
            alert = new Alert(65, "", "ServiceName", "Value cannot be a collection: [unknown]");
            Assert.assertTrue(alert.toString(), source.remove(alert));
            alert = new Alert(66, null, "ServiceNames", "Unknown service: unknown");
            Assert.assertTrue(alert.toString(), source.remove(alert));
            alert = new Alert(67, null, "ServiceNames", "Unknown service name reference: %{unknown}");
            Assert.assertTrue(alert.toString(), source.remove(alert));
            alert = new Alert(68, "", "ServiceNames", "Value is not a collection: unknown");
            Assert.assertTrue(alert.toString(), source.remove(alert));
            alert = new Alert(71, "", "#template", "Cannot find template action with reference: refz");
            Assert.assertTrue(alert.toString(), source.remove(alert));
            alert = new Alert(72, "", "#template", "Template action is not generated: ref");
            Assert.assertTrue(alert.toString(), source.remove(alert));
            alert = new Alert(74, "", "#template", "Template's dictionary 'TestAML2' differs from this action's dictionary 'TestAML'. Template: em");
            Assert.assertTrue(alert.toString(), source.remove(alert));
            alert = new Alert(76, "", "#template", "Template's message type 'ArrayMessage' differs from this action's message type 'SimpleMessage'. Template: am");
            Assert.assertTrue(alert.toString(), source.remove(alert));
            alert = new Alert(78, null, null, "Invalid long value: zzz");
            Assert.assertTrue(alert.toString(), source.remove(alert));
            alert = new Alert(79, null, null, "Invalid long value: zzz");
            Assert.assertTrue(alert.toString(), source.remove(alert));
            alert = new Alert(82, "", "MessageArray", "Incompatible value types: cannot use HashMap instead of IMessage");
            Assert.assertTrue(alert.toString(), source.remove(alert));

			Assert.assertEquals("Unchecked error", 0, source.size());
		}
	}

    @Test
    public void testInvalidSimpleValue() throws Exception {
        try {
            executeTest(INVALID_TEST_PATH + "invalid-simple-value.csv");
        } catch(AMLException e) {
            Collection<Alert> source = e.getAlertCollector().getAlerts();

            Alert alert = new Alert(3, "", "FInteger", "Invalid value in column 'FInteger': *", AlertType.ERROR);
            Assert.assertTrue(alert.toString(), source.remove(alert));
            alert = new Alert(3, "", "FIntegerEnum", "Invalid value in column 'FIntegerEnum': *", AlertType.ERROR);
            Assert.assertTrue(alert.toString(), source.remove(alert));

            Assert.assertEquals("Unchecked error", 0, source.size());
        }
    }

	@Override
	protected AMLSettings createSettings()
	{
		AMLSettings settings = new AMLSettings();
		settings.setAutoStart(true);
		settings.setBaseDir(BIN_FOLDER_PATH);
		settings.setContinueOnFailed(true);
		settings.setLanguageURI(AML3LanguageFactory.URI);
		return settings;
	}

	private void addService(IConnectionManager conManager, SailfishURI serviceURI, String name) throws InterruptedException, ExecutionException {
	    ServiceName serviceName = new ServiceName(ServiceName.DEFAULT_ENVIRONMENT, name);
	    conManager.getService(serviceName);
	    if (conManager.getService(serviceName) == null) {
	    	conManager.addService(serviceName, serviceURI, null, null).get();
	    }
	}
}
