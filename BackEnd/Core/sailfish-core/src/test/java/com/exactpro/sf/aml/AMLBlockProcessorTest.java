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

import com.exactpro.sf.aml.generator.Alert;
import com.exactpro.sf.aml.generator.AlertType;
import com.exactpro.sf.aml.iomatrix.AdvancedMatrixReader;
import com.exactpro.sf.aml.reader.AMLReader;
import com.exactpro.sf.aml.reader.struct.AMLMatrix;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.scriptrunner.actionmanager.ActionInfo;
import com.exactpro.sf.scriptrunner.actionmanager.ActionManager;
import com.exactpro.sf.scriptrunner.actionmanager.ActionRequirements;
import com.exactpro.sf.util.AbstractTest;
import com.google.common.collect.ListMultimap;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.util.Collection;
import java.util.List;

public class AMLBlockProcessorTest extends AbstractTest {
    private static AMLSettings settings = new AMLSettings();
    private static ActionRequirements actionRequirements = Mockito.mock(ActionRequirements.class);
    private static ActionInfo actionInfo = Mockito.mock(ActionInfo.class);
    private static ActionManager actionManager = Mockito.mock(ActionManager.class);

    @BeforeClass
    public static void init() {
        settings.setLanguageURI(SailfishURI.unsafeParse("AML_v3"));

        Mockito.when(actionInfo.getRequirements()).thenReturn(actionRequirements);
        Mockito.when(actionManager.getActionInfo(Mockito.any(SailfishURI.class), Mockito.any(SailfishURI.class))).thenReturn(actionInfo);
    }

    @Test
    public void positiveTest() throws Exception {
        settings.setTestCasesRange("1, 3-");

        File matrixFile = new File(BASE_DIR, "src/test/resources/aml/block-processor-positive-test.csv");

        try(AdvancedMatrixReader matrixReader = new AdvancedMatrixReader(matrixFile)) {
            AMLMatrix matrix = AMLReader.read(matrixReader);
            ListMultimap<AMLBlockType, AMLTestCase> blocks = AMLConverter.convert(matrix, settings, actionManager);
            blocks = AMLBlockProcessor.process(blocks, settings, actionManager);

            Assert.assertTrue(blocks.get(AMLBlockType.GlobalBlock).isEmpty());
            Assert.assertTrue(blocks.get(AMLBlockType.Block).isEmpty());
            Assert.assertTrue(blocks.get(AMLBlockType.FirstBlock).isEmpty());
            Assert.assertTrue(blocks.get(AMLBlockType.LastBlock).isEmpty());

            List<AMLTestCase> testCases = blocks.get(AMLBlockType.BeforeTCBlock);

            Assert.assertEquals(1, testCases.size());
            Assert.assertEquals(1, testCases.get(0).getActions().size());
            Assert.assertNull(testCases.get(0).findActionByRef("afc"));

            testCases = blocks.get(AMLBlockType.AfterTCBlock);

            Assert.assertEquals(1, testCases.size());
            Assert.assertEquals(1, testCases.get(0).getActions().size());
            Assert.assertNull(testCases.get(0).findActionByRef("afc"));

            testCases = blocks.get(AMLBlockType.TestCase);

            Assert.assertEquals(5, testCases.size());

            AMLTestCase testCase = testCases.get(0);

            Assert.assertEquals(AMLBlockType.FirstBlock, testCase.getBlockType());
            Assert.assertEquals(4, testCase.getActions().size());
            Assert.assertNull(testCase.findActionByRef("afc"));
            Assert.assertFalse(testCase.findActionByRef("sm2").isAddToReport());

            testCase = testCases.get(1);

            Assert.assertEquals(AMLBlockType.TestCase, testCase.getBlockType());
            Assert.assertEquals(1, testCase.getMatrixOrder());
            Assert.assertEquals(9, testCase.getActions().size());
            Assert.assertNotNull(testCase.findActionByRef("afc"));

            testCase = testCases.get(2);

            Assert.assertEquals(AMLBlockType.TestCase, testCase.getBlockType());
            Assert.assertEquals(4, testCase.getMatrixOrder());
            Assert.assertEquals(10, testCase.getActions().size());
            Assert.assertNotNull(testCase.findActionByRef("afc"));
            Assert.assertNotNull(testCase.findActionByRef("s1"));

            testCase = testCases.get(3);

            Assert.assertEquals(AMLBlockType.TestCase, testCase.getBlockType());
            Assert.assertEquals(5, testCase.getMatrixOrder());
            Assert.assertEquals(7, testCase.getActions().size());
            Assert.assertNotNull(testCase.findActionByRef("afc"));
            Assert.assertNotNull(testCase.findActionByRef("s1"));
            Assert.assertNotNull(testCase.findActionByRef("s2"));

            AMLAction ref1 = testCase.findActionByRef("ref1");
            AMLAction ref2 = testCase.findActionByRef("ref2");
            AMLAction ref3 = testCase.findActionByRef("ref3");
            AMLAction ref4 = testCase.findActionByRef("ref4");

            Assert.assertNotNull(ref1);
            Assert.assertNotNull(ref2);
            Assert.assertNotNull(ref3);
            Assert.assertNotNull(ref4);

            Assert.assertTrue(ref1.isLastOutcome());
            Assert.assertFalse(ref1.isGroupFinished());
            Assert.assertTrue(ref2.isLastOutcome());
            Assert.assertTrue(ref2.isGroupFinished());
            Assert.assertTrue(ref3.isLastOutcome());
            Assert.assertFalse(ref3.isGroupFinished());
            Assert.assertTrue(ref4.isLastOutcome());
            Assert.assertTrue(ref4.isGroupFinished());

            testCase = testCases.get(4);

            Assert.assertEquals(AMLBlockType.LastBlock, testCase.getBlockType());
            Assert.assertEquals(3, testCase.getActions().size());
            Assert.assertNull(testCase.findActionByRef("afc"));
            Assert.assertNotNull(testCase.findActionByRef("s1"));
            Assert.assertNotNull(testCase.findActionByRef("s2"));
        }
    }

    @Test
    public void negativeTest() throws Exception {
        settings.setTestCasesRange("");

        File matrixFile = new File(BASE_DIR, "src/test/resources/aml/block-processor-negative-test.csv");

        try(AdvancedMatrixReader matrixReader = new AdvancedMatrixReader(matrixFile)) {
            AMLMatrix matrix = AMLReader.read(matrixReader);
            ListMultimap<AMLBlockType, AMLTestCase> blocks = AMLConverter.convert(matrix, settings, actionManager);

            AMLBlockProcessor.process(blocks, settings, actionManager);
            Assert.fail("No errors were detected");
        } catch(AMLException e) {
            Collection<Alert> errors = e.getAlertCollector().getAlerts();

            Assert.assertEquals(9, errors.size());

            Alert error = new Alert(11, "tc1", "#reference", "Duplicate reference at line: 6", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(-1, null, null, "Duplicated reference found in lines: 7, 3: 's1'", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(-1, null, null, "Duplicated reference found in lines: 12, 3: 's1'", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(19, "", null, "Recursion detected: b2 -> b1 -> b2", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(23, "", null, "Recursion detected: b1 -> b2 -> b1", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(27, "", "#template", "Reference to unknown block: unknown", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(13, "", "#template", "Reference to unknown block: unknown", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(31, "", "#dependencies", "Dependency on unknown action: unknown", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));
            error = new Alert(32, "ref", "#dependencies", "Action cannot depend on itself", AlertType.ERROR);
            Assert.assertTrue(error.toString(), errors.remove(error));

            Assert.assertEquals(0, errors.size());
        }
    }
}
