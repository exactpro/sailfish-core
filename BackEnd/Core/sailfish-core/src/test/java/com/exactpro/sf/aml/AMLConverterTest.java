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
import java.util.Collection;
import java.util.HashMap;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import com.exactpro.sf.aml.generator.Alert;
import com.exactpro.sf.aml.generator.AlertType;
import com.exactpro.sf.aml.generator.matrix.JavaStatement;
import com.exactpro.sf.aml.iomatrix.AdvancedMatrixReader;
import com.exactpro.sf.aml.reader.AMLReader;
import com.exactpro.sf.aml.reader.struct.AMLMatrix;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.scriptrunner.actionmanager.ActionInfo;
import com.exactpro.sf.scriptrunner.actionmanager.ActionManager;
import com.exactpro.sf.scriptrunner.actionmanager.ActionRequirements;
import com.exactpro.sf.util.AbstractTest;
import com.google.common.collect.ListMultimap;

public class AMLConverterTest extends AbstractTest {
    private static AMLSettings settings = new AMLSettings();
    private static ActionRequirements actionRequirements = Mockito.mock(ActionRequirements.class);
    private static ActionInfo actionInfo = Mockito.mock(ActionInfo.class);
    private static ActionInfo actionInfoVoid = Mockito.mock(ActionInfo.class);
    private static ActionManager actionManager = Mockito.mock(ActionManager.class);

    @SuppressWarnings("serial")
    @BeforeClass
    public static void init() {
        settings.setLanguageURI(SailfishURI.unsafeParse("AML_v3"));
        settings.setSuppressAskForContinue(true);
        settings.setStaticVariables(new HashMap<String, String>() {{ put("sv", "876"); }});

        Mockito.doReturn(void.class).when(actionInfoVoid).getReturnType();
        Mockito.when(actionInfo.getRequirements()).thenReturn(actionRequirements);
        Mockito.when(actionInfoVoid.getRequirements()).thenReturn(actionRequirements);
        Mockito.when(actionInfoVoid.getAllowedMessageTypes()).thenReturn(ArrayUtils.toArray("SimpleMessage"));

        Mockito.when(actionManager.getActionInfo(Mockito.any(SailfishURI.class), Mockito.any(SailfishURI.class))).thenReturn(actionInfo);
        Mockito.when(actionManager.getActionInfo(Mockito.eq(SailfishURI.unsafeParse("foobar")), Mockito.any(SailfishURI.class))).thenReturn(null);
        Mockito.when(actionManager.getActionInfo(Mockito.eq(SailfishURI.unsafeParse("count")), Mockito.any(SailfishURI.class))).thenReturn(actionInfoVoid);
        Mockito.when(actionManager.getActionInfo(Mockito.eq(SailfishURI.unsafeParse("count")), Mockito.any(SailfishURI.class))).thenReturn(actionInfoVoid);
    }

    @Test
    public void positiveTest() throws Exception {
        File matrixFile = new File(BASE_DIR, "src/test/resources/aml/converter-positive-test.csv");

        try(AdvancedMatrixReader matrixReader = new AdvancedMatrixReader(matrixFile)) {
            AMLMatrix matrix = AMLReader.read(matrixReader);

            ListMultimap<AMLBlockType, AMLTestCase> blocks = AMLConverter.convert(matrix, settings, actionManager);

            Assert.assertEquals(0, blocks.get(AMLBlockType.FirstBlock).size());
            Assert.assertEquals(1, blocks.get(AMLBlockType.GlobalBlock).size());

            AMLTestCase globalBlock = blocks.get(AMLBlockType.GlobalBlock).get(0);

            Assert.assertEquals(1, globalBlock.getActions().size());

            AMLAction action = globalBlock.getActions().get(0);

            Assert.assertEquals(JavaStatement.SET_STATIC, JavaStatement.value(action.getActionURI()));
            Assert.assertEquals("876", action.getStaticValue().getValue());
            Assert.assertEquals("10", action.getStaticValue().getOrigValue());

            Assert.assertEquals(3, blocks.get(AMLBlockType.TestCase).size());

            AMLTestCase testCase = blocks.get(AMLBlockType.TestCase).get(0);

            Assert.assertEquals(13, testCase.getActions().size());
            Assert.assertNull(testCase.findActionByRef("rsm1"));
            Assert.assertTrue(testCase.isAddToReport());
            Assert.assertEquals(1, testCase.getMatrixOrder());

            AMLAction initMapAction = testCase.findActionByRef("hm");

            Assert.assertEquals(2, initMapAction.getParameters().size());
            Assert.assertTrue(initMapAction.getParameters().containsKey("Value1"));
            Assert.assertTrue(initMapAction.getParameters().containsKey("Value3"));

            AMLAction receiveAction = testCase.findActionByRef("rsm2");

            Assert.assertEquals("og:on2", receiveAction.getOutcome());
            Assert.assertEquals("og", receiveAction.getOutcomeGroup());
            Assert.assertEquals("on2", receiveAction.getOutcomeName());
            Assert.assertEquals(2, receiveAction.getVerificationsOrder().size());
            Assert.assertEquals("Value1:PASSED", receiveAction.getVerificationsOrder().get(0));
            Assert.assertEquals("Value3:FAILED", receiveAction.getVerificationsOrder().get(1));

            Assert.assertEquals(1, blocks.get(AMLBlockType.LastBlock).size());

            AMLTestCase lastBlock = blocks.get(AMLBlockType.LastBlock).get(0);

            Assert.assertEquals(1, lastBlock.getActions().size());
            Assert.assertFalse(lastBlock.isAddToReport());

            AMLAction sleepAction = lastBlock.findActionByRef("slp");

            Assert.assertFalse(sleepAction.isAddToReport());

            testCase = blocks.get(AMLBlockType.TestCase).get(1);

            Assert.assertEquals(3, testCase.getMatrixOrder());
        }
    }

    @Test
    public void negativeTest() throws Exception {
        File matrixFile = new File(BASE_DIR, "src/test/resources/aml/converter-negative-test.csv");

        try(AdvancedMatrixReader matrixReader = new AdvancedMatrixReader(matrixFile)) {
            AMLMatrix matrix = AMLReader.read(matrixReader);

            AMLConverter.convert(matrix, settings, actionManager);
            Assert.fail("No errors were detected");
        } catch(AMLException e) {
            Collection<Alert> errors = e.getAlertCollector().getAlerts();

            Assert.assertEquals(60, errors.size());

            Alert error = new Alert(2, "29", "#reference", "Variable '29' start with a digit", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(3, null, "#messages_count", "Invalid value: -7", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(4, null, "#outcome", "Invalid value: foo (expected: <group>:<name>)", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(5, null, "#outcome", "Invalid outcome group: 1foo (Variable '1foo' start with a digit)", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(6, null, "#outcome", "Invalid outcome name: 1bar (Variable '1bar' start with a digit)", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(7, "foo", "#reference", "Invalid value: foo (expected: %{name})", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(8, "%{}", "#reference", "Invalid empty variable name", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(9, "!", "#reference", "Invalid empty variable name", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(10, "%{ref1}", "#reference", "Variable '%{ref1}' contain invalid character", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(10, "%{ref1}", "#reference_to_filter", "Variable '%{ref2}' contain invalid character", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(10, "%{ref1}", "#template", "Variable '%{ref3}' contain invalid character", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(11, "foo bar", "#reference", "Variable 'foo bar' contain white space", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(11, "foo bar", "#reference_to_filter", "Variable 'foo baz' contain white space", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(11, "foo bar", "#template", "Variable 'foo bax' contain white space", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(12, "public", "#reference", "Variable 'public' is reserved java word", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(12, "public", "#reference_to_filter", "Variable 'void' is reserved java word", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(12, "public", "#template", "Variable 'int' is reserved java word", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(13, "foo*bar", "#reference", "Variable 'foo*bar' contain invalid character", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(13, "foo*bar", "#reference_to_filter", "Variable 'foo/bar' contain invalid character", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(13, "foo*bar", "#template", "Variable 'foo:bar' contain invalid character", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(14, "_foobar", "#reference", "Variable '_foobar' start with invalid character '_'", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(14, "_foobar", "#reference_to_filter", "Variable '_foobaz' start with invalid character '_'", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(14, "_foobar", "#template", "Variable '_foobax' start with invalid character '_'", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(15, "0foobar", "#reference", "Variable '0foobar' start with a digit", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(15, "0foobar", "#reference_to_filter", "Variable '1foobar' start with a digit", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(15, "0foobar", "#template", "Variable '2foobar' start with a digit", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(16, "!foobar", "#reference", "Variable '!foobar' contain invalid character", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(16, "!foobar", "#reference_to_filter", "Variable '!foobar' contain invalid character", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(16, "!foobar", "#template", "Variable '!foobar' contain invalid character", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(17, null, "#service_name", "Variable 'foo bar' contain white space", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(18, null, "#service_name", "Variable 'public' is reserved java word", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(19, null, "#service_name", "Variable 'foo*bar' contain invalid character", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(20, null, "#service_name", "Variable '_foobar' start with invalid character '_'", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(21, null, "#service_name", "Variable '0foobar' start with a digit", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(22, null, "#service_name", "Invalid empty variable name", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(23, "s1", "#static_type", "Unknown type: foo", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(24, null, "#timeout", "Value must be positive: -1", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(25, null, "#timeout", "Value must be in long format or a static variable reference: foo", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(26, null, "#timeout", "Invalid empty variable name", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(27, "s2", "#static_type", "Required column is missing", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(28, null, "#action", "Unknown action: foobar", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(30, "sv", null, "Duplicated reference found in lines: 29, 30: 'sv'", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(32, "rf", null, "Duplicated reference found in lines: 31, 32: 'rf'", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(33, "r0", "#action", "Static action must have a name", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(34, null, "#reference", "Static action must have a reference or reference to filter", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(35, "r1", "#action", "Static action must return a value", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(36, "ref", "#reference", "#reference cannot be equal to #reference_to_filter", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(36, "ref", "#reference", "#reference cannot be equal to #template", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(36, "ref", "#reference_to_filter", "#reference_to_filter cannot be equal to #template", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(37, null, "#message_type", "Incompatible message type: ArrayMessage", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(38, null, "#dependencies", "Variable '123' start with a digit", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(39, null, "#verifications_order",
                                 "Invalid value: Value1: (expected: <field>:<status>)", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(40, null, "#verifications_order", "Invalid status name: FAKE (Value1:FAKE)", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(41, null, "#verifications_order",
                                 "Invalid message field name: Va lue1 (Variable 'Va lue1' contain white space)", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(42, null, "#verifications_order",
                                 "Invalid message field name: 1Value1 (Variable '1Value1' start with a digit)", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(43, null, "#verifications_order",
                                 "Invalid status name: 1PASSED (Variable '1PASSED' start with a digit)", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(44, null, "#messages_count", "Invalid value: -1", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(46, null, "#messages_count", "Invalid value: d", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(48, null, "#messages_count", "Invalid value: ${toInteger(\"1\")} +  1", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(50, null, "#messages_count", "Invalid value: [1..4]", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));

            Assert.assertEquals(0, errors.size());
        }
    }
}
