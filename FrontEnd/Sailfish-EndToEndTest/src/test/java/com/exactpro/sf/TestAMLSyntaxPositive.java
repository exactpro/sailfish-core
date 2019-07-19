/*******************************************************************************
 * Copyright 2009-2019 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf;

import java.io.File;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.testwebgui.restapi.xml.XmlTestCaseDescription;
import com.exactpro.sf.testwebgui.restapi.xml.XmlTestscriptRunDescription;

public class TestAMLSyntaxPositive extends TestMatrix {

    private static SFAPIClient sfapi;
    private final static String VALID_TEST_PATH = "aml3positive" + File.separator;
    private final static String VALID_TEST_MATRIX = "validTest.csv";
    private final static String SENDDIRTY_TEST_MATRIX = "testSendDirty.csv";
    private static int testScriptID;
    private static int testScriptSendDirtyID;
    private static final Logger logger = LoggerFactory.getLogger(TestAMLSyntaxPositive.class);

    @BeforeClass
    public static void setUpClass() throws Exception {
        logger.info("Start positive tests of AML syntax");
        try {
            sfapi = new SFAPIClient(TestMatrix.SF_GUI_URL);
            init(sfapi);
            testScriptID = runMatrix(sfapi, VALID_TEST_MATRIX, VALID_TEST_PATH);

            testScriptSendDirtyID = runMatrix(sfapi, SENDDIRTY_TEST_MATRIX, VALID_TEST_PATH);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        logger.info("Finish positive tests of AML syntax");
        try {
            destroy(sfapi);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    @Test
    public void testReferences() throws Exception {
        logger.info("Start testReferences()");
        try {
            XmlTestCaseDescription xmlCase = sfapi.getTestScriptRunInfo(testScriptID).getTestcases().get(0);
            Assert.assertEquals(STATUS_PASSED, xmlCase.getStatus());
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Test
    public void testCommentedTestCases() throws Exception {
        logger.info("Start testCommentedTestCases()");
        try {
            XmlTestCaseDescription xmlCase = sfapi.getTestScriptRunInfo(testScriptID).getTestcases().get(1);
            Assert.assertEquals(STATUS_PASSED, xmlCase.getStatus());
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Test
    public void testCycles() throws Exception {
        logger.info("Start testCycles()");
        try {
            XmlTestCaseDescription xmlCase1 = sfapi.getTestScriptRunInfo(testScriptID).getTestcases().get(2);
            XmlTestCaseDescription xmlCase2 = sfapi.getTestScriptRunInfo(testScriptID).getTestcases().get(3);
            XmlTestCaseDescription xmlCase3 = sfapi.getTestScriptRunInfo(testScriptID).getTestcases().get(4);
            Assert.assertEquals(STATUS_PASSED, xmlCase1.getStatus());
            Assert.assertEquals(STATUS_PASSED, xmlCase2.getStatus());
            Assert.assertEquals(STATUS_PASSED, xmlCase3.getStatus());
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Test
    public void testIncludeBlock() throws Exception {
        try {
            logger.info("Start testIncludeBlock()");
            XmlTestCaseDescription xmlCase1 = sfapi.getTestScriptRunInfo(testScriptID).getTestcases().get(5);
            XmlTestCaseDescription xmlCase2 = sfapi.getTestScriptRunInfo(testScriptID).getTestcases().get(6);
            Assert.assertEquals(STATUS_PASSED, xmlCase1.getStatus());
            Assert.assertEquals(STATUS_PASSED, xmlCase2.getStatus());
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Test
    public void testMultipleSubmessages() throws Exception {
        logger.info("Start testMultipleSubmessages()");
        try {
            XmlTestCaseDescription xmlCase = sfapi.getTestScriptRunInfo(testScriptID).getTestcases().get(7);
            Assert.assertEquals(STATUS_PASSED, xmlCase.getStatus());
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Test
    public void testCollections() throws Exception {
        logger.info("Start testCollections()");
        try {
            XmlTestCaseDescription xmlCase = sfapi.getTestScriptRunInfo(testScriptID).getTestcases().get(8);
            Assert.assertEquals(STATUS_PASSED, xmlCase.getStatus());
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Test
    public void testCheckPointWithReferences() throws Exception {
        logger.info("Start testCheckPointWithReferences()");
        try {
            XmlTestCaseDescription xmlCase = sfapi.getTestScriptRunInfo(testScriptID).getTestcases().get(9);
            Assert.assertEquals(STATUS_PASSED, xmlCase.getStatus());
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Test
    public void testStatic() throws Exception {
        logger.info("Start testStatic()");
        try {
            XmlTestCaseDescription xmlCase = sfapi.getTestScriptRunInfo(testScriptID).getTestcases().get(10);
            Assert.assertEquals(STATUS_PASSED, xmlCase.getStatus());
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Test
    public void testIfOperator() throws Exception {
        logger.info("Start testIfOperator()");
        try {
            XmlTestCaseDescription xmlCase = sfapi.getTestScriptRunInfo(testScriptID).getTestcases().get(11);
            Assert.assertEquals(STATUS_PASSED, xmlCase.getStatus());
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Test
    public void testDefineService() throws Exception {
        logger.info("Start testDefineService()");
        try {
            XmlTestCaseDescription xmlCase = sfapi.getTestScriptRunInfo(testScriptID).getTestcases().get(12);
            Assert.assertEquals(STATUS_PASSED, xmlCase.getStatus());
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Test
    public void testInternalIncludeBlock() throws Exception {
        logger.info("Start testInternalIncludeBlock()");
        try {
            XmlTestCaseDescription xmlCase = sfapi.getTestScriptRunInfo(testScriptID).getTestcases().get(13);
            Assert.assertEquals(STATUS_PASSED, xmlCase.getStatus());
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Test
    public void testSyntaxUtilMethod() throws Exception {
        logger.info("Start testSyntaxUtilMethod()");
        try {
            XmlTestCaseDescription xmlCase = sfapi.getTestScriptRunInfo(testScriptID).getTestcases().get(14);
            Assert.assertEquals(STATUS_PASSED, xmlCase.getStatus());
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Test
    public void testOperations() throws Exception {
        logger.info("Start testOperations()");
        try {
            XmlTestCaseDescription xmlCase = sfapi.getTestScriptRunInfo(testScriptID).getTestcases().get(15);
            Assert.assertEquals(STATUS_PASSED, xmlCase.getStatus());
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Test
    public void testDefineHeader() throws Exception {
        logger.info("Start testDefineHeader()");
        try {
            XmlTestCaseDescription xmlCase = sfapi.getTestScriptRunInfo(testScriptID).getTestcases().get(16);
            Assert.assertEquals(STATUS_PASSED, xmlCase.getStatus());
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Test
    public void testCommentedOutsideTestCase() throws Exception {
        logger.info("Start testCommentedOutsideTestCase()");
        try {
            XmlTestCaseDescription xmlCase = sfapi.getTestScriptRunInfo(testScriptID).getTestcases().get(17);
            Assert.assertEquals(STATUS_PASSED, xmlCase.getStatus());
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Test
    public void testCaseInsensitivity() throws Exception {
        logger.info("Start testCommentedOutsideTestCase()");
        try {
            XmlTestCaseDescription xmlCase = sfapi.getTestScriptRunInfo(testScriptID).getTestcases().get(18);
            Assert.assertEquals(STATUS_PASSED, xmlCase.getStatus());
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Test
    public void testAllValidMatrix() throws Exception {
        logger.info("Start testAllValidMatrix()");
        try {
            XmlTestscriptRunDescription xmlDescription = sfapi.getTestScriptRunInfo(testScriptID);
            Assert.assertEquals("Wrong number of the passed tests", 19, xmlDescription.getPassed());
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Test
    public void testSendDirtyNewOrder() throws Exception {
        logger.info("Start testSendDirtyNewOrder()");
        try {
            XmlTestCaseDescription xmlCase = sfapi.getTestScriptRunInfo(testScriptSendDirtyID).getTestcases().get(0);
            Assert.assertEquals(STATUS_PASSED, xmlCase.getStatus());
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Test
    public void testSendDirtyAdditionalTags() throws Exception {
        logger.info("Start testSendDirtyAdditionalTags()");
        try {
            XmlTestCaseDescription xmlCase = sfapi.getTestScriptRunInfo(testScriptSendDirtyID).getTestcases().get(1);
            Assert.assertEquals(STATUS_PASSED, xmlCase.getStatus());
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Test
    public void testSendDirtyFieldOrder() throws Exception {
        logger.info("Start testSendDirtyFieldOrder()");
        try {
            XmlTestCaseDescription xmlCase = sfapi.getTestScriptRunInfo(testScriptSendDirtyID).getTestcases().get(2);
            Assert.assertEquals(STATUS_PASSED, xmlCase.getStatus());
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Test
    public void testSendDirtyDuplicateTags() throws Exception {
        logger.info("Start testSendDirtyDuplicateTags()");
        try {
            XmlTestCaseDescription xmlCase = sfapi.getTestScriptRunInfo(testScriptSendDirtyID).getTestcases().get(3);
            Assert.assertEquals(STATUS_PASSED, xmlCase.getStatus());
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Test
    public void testSendDirtyStrings() throws Exception {
        logger.info("Start testSendDirtyStrings()");
        try {
            XmlTestCaseDescription xmlCase = sfapi.getTestScriptRunInfo(testScriptSendDirtyID).getTestcases().get(4);
            Assert.assertEquals(STATUS_PASSED, xmlCase.getStatus());
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Test
    public void testSendDirtyHeader() throws Exception {
        logger.info("Start testSendDirtyHeader()");
        try {
            XmlTestCaseDescription xmlCase = sfapi.getTestScriptRunInfo(testScriptSendDirtyID).getTestcases().get(5);
            Assert.assertEquals(STATUS_PASSED, xmlCase.getStatus());
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Test
    public void testSendDirtyRussianChar() throws Exception {
        logger.info("Start testSendDirtyRussianChar()");
        try {
            XmlTestCaseDescription xmlCase = sfapi.getTestScriptRunInfo(testScriptSendDirtyID).getTestcases().get(6);
            Assert.assertEquals(STATUS_PASSED, xmlCase.getStatus());
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Test
    public void testSendDirtyASCII() throws Exception {
        logger.info("Start testSendDirtyASCII()");
        try {
            XmlTestCaseDescription xmlCase = sfapi.getTestScriptRunInfo(testScriptSendDirtyID).getTestcases().get(7);
            Assert.assertEquals(STATUS_PASSED, xmlCase.getStatus());
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Test
    public void testSendDirtyCountersGroup() throws Exception {
        logger.info("Start testSendDirtyCountersGroup()");
        try {
            XmlTestCaseDescription xmlCase = sfapi.getTestScriptRunInfo(testScriptSendDirtyID).getTestcases().get(8);
            Assert.assertEquals(STATUS_PASSED, xmlCase.getStatus());
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Test
    public void testSendDirtyExcudedCountersGroup() throws Exception {
        logger.info("Start testSendDirtyExcudedCountersGroup()");
        try {
            XmlTestCaseDescription xmlCase = sfapi.getTestScriptRunInfo(testScriptSendDirtyID).getTestcases().get(9);
            Assert.assertEquals(STATUS_PASSED, xmlCase.getStatus());
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }
}