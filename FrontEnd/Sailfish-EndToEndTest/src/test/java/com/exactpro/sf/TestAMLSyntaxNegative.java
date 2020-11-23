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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.testwebgui.restapi.xml.XmlTestCaseDescription;

public class TestAMLSyntaxNegative extends TestMatrix {

    private static SFAPIClient sfapi;

    private final static String INVALID_TEST_PATH = "aml3negative" + File.separator;

    private final static String TESTS_MATRIX = "invalidTests.csv";
    private final static String DOUBLE_START = "testDoubleStart.csv";
    private final static String INVALID_MATRIX = "testReqFields.csv";
    private final static String INCORRECT_CYCLES = "testIncorrectCycles.csv";
    private final static String INCORRECT_DEFINE_SERVICE = "testIncorrectDefineService.csv";
    private final static String FIRST_AND_LAST_BLOCK = "testFirstAndLastBlock.csv";
    private final static String INCLUDE_BLOCKS = "testIncludeBlocks.csv";
    private final static String INVALID_IF = "testInvalidIf.csv";
    private final static String DUBLICATED_TEST_CASES = "testDuplicatedTestCases.csv";
    private final static String EMPTY_TEST_MATRIX = "emptyTest.csv";
    private final static String DICTIONATY_MATRIX = "testIncorrectDictionaryName.csv";
    private final static String REFERENCE_FORMAT_MATRIX = "testIncorrectReferenceFormat.csv";
    private final static String REFERENCE_NAME_MATRIX = "testMissingReference.csv";
    private final static String SERVICE_NAME_MATRIX = "testIncorrectServiceName.csv";
    private final static String STATIC_FUNCTION_MATRIX = "testFunctionInStatic.csv";
    private final static String COLLECTION_MATRIX = "testIncorrectCollection.csv";
    private final static String REFERENCE_CHECKPOINT_MATRIX = "testCheckPointWithReference.csv";
    private final static String DUBLICATED_COLUMN_MATRIX = "testDublicatedColumn.csv";
    private final static String DEFINED_HEADER_MATRIX = "testIncorrectDefineHeader.csv";
    private final static String INVALID_FIELDORDER = "testInvalidFieldOrder.csv";

    private static List<String> errorsTestsMatrix;
    private static List<String> errorsDoubleStart;
    private static List<String> errorsInvalidMatrix;
    private static List<String> errorsIncorrectCycles;
    private static List<String> errorsIncorrectDefineService;
    private static List<String> errorsFirstAndLastBlock;
    private static List<String> errorsForbiddenReference;
    private static List<String> errorsInvalidIf;
    private static List<String> errorsIncludeBlocks;
    private static List<String> errorsDublicatedTestCases;
    private static List<String> errorsEmptyMatrix;
    private static List<String> errorsDictionaryMatrix;
    private static List<String> errorsReferenceFormatMatrix;
    private static List<String> errorsReferenceNameMatrix;
    private static List<String> errorsServiceNameMatrix;
    private static List<String> errorsStaticFunctionMatrix;
    private static List<String> errorsCollectionMatrix;
    private static List<String> errorsReferenceCheckpointMatrix;
    private static List<String> errorsDublicatedColumnMatrix;
    private static List<String> errorsDefinedHeaderMatrix;
    private static XmlTestCaseDescription descriptionFieldOrder;

    private static final Logger logger = LoggerFactory.getLogger(TestAMLSyntaxNegative.class);

    @BeforeClass
    public static void setUpClass() throws Exception {
        logger.info("Start negative tests of AML syntax");
        try {
            sfapi = new SFAPIClient(TestMatrix.SF_GUI_URL);
            init(sfapi);

            errorsTestsMatrix = getList(sfapi.getTestScriptRunInfo(runMatrix(sfapi, TESTS_MATRIX, INVALID_TEST_PATH)).getProblem().trim());
            errorsDoubleStart = getList(sfapi.getTestScriptRunInfo(runMatrix(sfapi, DOUBLE_START, INVALID_TEST_PATH)).getProblem().trim());
            errorsInvalidMatrix = getList(sfapi.getTestScriptRunInfo(runMatrix(sfapi, INVALID_MATRIX, INVALID_TEST_PATH)).getProblem().trim());
            errorsIncorrectCycles = getList(sfapi.getTestScriptRunInfo(runMatrix(sfapi, INCORRECT_CYCLES, INVALID_TEST_PATH)).getProblem().trim());
            errorsIncorrectDefineService = getList(sfapi.getTestScriptRunInfo(runMatrix(sfapi, INCORRECT_DEFINE_SERVICE, INVALID_TEST_PATH)).getProblem().trim());
            errorsFirstAndLastBlock = getList(sfapi.getTestScriptRunInfo(runMatrix(sfapi, FIRST_AND_LAST_BLOCK, INVALID_TEST_PATH)).getProblem().trim());
            errorsInvalidIf = getList(sfapi.getTestScriptRunInfo(runMatrix(sfapi, INVALID_IF, INVALID_TEST_PATH)).getProblem().trim());
            errorsDublicatedTestCases = getList(sfapi.getTestScriptRunInfo(runMatrix(sfapi, DUBLICATED_TEST_CASES, INVALID_TEST_PATH)).getProblem().trim());
            //errorsForbiddenReference = getList(sfapi.getTestScriptRunInfo(runMatrix(sfapi, FORBIDDEN_REFERENCE, INVALID_TEST_PATH)).().trim());
            errorsIncludeBlocks = getList(sfapi.getTestScriptRunInfo(runMatrix(sfapi, INCLUDE_BLOCKS, INVALID_TEST_PATH)).getProblem().trim());
            errorsEmptyMatrix = getList(sfapi.getTestScriptRunInfo(runMatrix(sfapi, EMPTY_TEST_MATRIX, INVALID_TEST_PATH)).getProblem().trim());
            errorsDictionaryMatrix = getList(sfapi.getTestScriptRunInfo(runMatrix(sfapi, DICTIONATY_MATRIX, INVALID_TEST_PATH)).getProblem().trim());
            errorsReferenceFormatMatrix = getList(
                    sfapi.getTestScriptRunInfo(runMatrix(sfapi, REFERENCE_FORMAT_MATRIX, INVALID_TEST_PATH)).getProblem().trim());
            errorsServiceNameMatrix = getList(
                    sfapi.getTestScriptRunInfo(runMatrix(sfapi, SERVICE_NAME_MATRIX, INVALID_TEST_PATH)).getProblem().trim());
            errorsStaticFunctionMatrix = getList(
                    sfapi.getTestScriptRunInfo(runMatrix(sfapi, STATIC_FUNCTION_MATRIX, INVALID_TEST_PATH)).getProblem().trim());
            errorsCollectionMatrix = getList(sfapi.getTestScriptRunInfo(runMatrix(sfapi, COLLECTION_MATRIX, INVALID_TEST_PATH)).getProblem().trim());
            errorsReferenceCheckpointMatrix = getList(
                    sfapi.getTestScriptRunInfo(runMatrix(sfapi, REFERENCE_CHECKPOINT_MATRIX, INVALID_TEST_PATH)).getProblem().trim());
            errorsDublicatedColumnMatrix = getList(
                    sfapi.getTestScriptRunInfo(runMatrix(sfapi, DUBLICATED_COLUMN_MATRIX, INVALID_TEST_PATH)).getProblem().trim());
            errorsDefinedHeaderMatrix = getList(
                    sfapi.getTestScriptRunInfo(runMatrix(sfapi, DEFINED_HEADER_MATRIX, INVALID_TEST_PATH)).getCause().trim());
            errorsReferenceNameMatrix = getList(
                    sfapi.getTestScriptRunInfo(runMatrix(sfapi, REFERENCE_NAME_MATRIX, INVALID_TEST_PATH)).getCause().trim());
            descriptionFieldOrder = sfapi.getTestScriptRunInfo(runMatrix(sfapi, INVALID_FIELDORDER, INVALID_TEST_PATH)).getTestcases().get(0);

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        logger.info("Finish negative tests of AML syntax");
        try {
            destroy(sfapi);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    @Test
    public void testIncorrectDictionaryName() throws Exception {
        logger.info("Start testIncorrectDictionaryName()");
        try {
            checkErrorPresents("Error in line 4: Can't find dictionary [unknown_dictionaty]", errorsDictionaryMatrix);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    @Test
    public void testIncorrectReferenceFormat() {
        logger.info("Start testIncorrectReferenceFormat()");
        try {
            checkErrorPresents("Error in line 5 column 'ClOrdID': Unbalansed brackets in column 'ClOrdID'.", errorsReferenceFormatMatrix);

            checkErrorPresents("Error in line 6 column 'ClOrdID': Column 'ClOrdID': Reference 'ord23232' is not defined in matrix", errorsReferenceFormatMatrix);

            checkErrorPresents("Error in line 6 column 'ClOrdID': Reference to unknown action 'ord23232' is found in column 'ClOrdID': '${ord23232:ClOrdID}'.", errorsReferenceFormatMatrix);

            checkErrorPresents("Error in line 7 column 'ClOrdID': Reference is empty in column 'ClOrdID'.", errorsReferenceFormatMatrix);

            checkErrorPresents("Error in line 9 column 'ClOrdID': Reference to row is missed in column 'ClOrdID': '${:ClOrdID}'.", errorsReferenceFormatMatrix);

            checkErrorPresents("Error in line 10 column 'ClOrdID': Invalid reference format in column 'ClOrdID': ':'. Expected format: ${reference:column} or ${reference}.", errorsReferenceFormatMatrix);

            checkErrorPresents("Error in line 11 column 'ClOrdID': Column 'ClOrdID': Reference to unknown column 'foo bar' in reference 'ord20:foo bar'", errorsReferenceFormatMatrix);

            checkErrorPresents("Error in line 11 column 'ClOrdID': Reference to unknown column 'foo bar' is found in column 'ClOrdID': '${ord20:foo bar}'.", errorsReferenceFormatMatrix);

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    @Test
    public void testIncorrectServiceName() {
        logger.info("Start testIncorrectServiceName()");
        try {
            checkErrorPresents("Error in line 4 column '#service_name': Variable 'Fa ke' contain white space", errorsServiceNameMatrix);

            checkErrorPresents("Error in line 5 column '#service_name': Variable '1fake' start with a digit", errorsServiceNameMatrix);

            checkErrorPresents("Error in line 6 column '#service_name': Variable 'fa{ke' contain invalid character", errorsServiceNameMatrix);

            checkErrorPresents("Error in line 7 column '#service_name': Variable 'fak>e' contain invalid character", errorsServiceNameMatrix);

            checkErrorPresents("Error in line 8 column '#service_name': Variable '_fake' start with invalid character '_'", errorsServiceNameMatrix);

            checkErrorPresents("Error in line 9 column '#service_name': Variable 'Fak%e' contain invalid character", errorsServiceNameMatrix);

            checkErrorPresents("Error in line 10 column '#service_name': Variable 'fa$ke' contain invalid character", errorsServiceNameMatrix);

            checkErrorPresents("Error in line 11 column '#service_name': Variable 'Fa-ke' contain invalid character", errorsServiceNameMatrix);

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    @Test
    public void testMissingReference() {
        logger.info("Start testMissingReference()");
        try {
            checkErrorPresents("Error in line 4 column 'ClOrdID': Sub-action must be predefined to use references to its fields.", errorsReferenceNameMatrix);

            checkErrorPresents("Found 2 error(s) during preparing script:Error in line 4 column 'ClOrdID': Reference 'new_order' is not yet defined in column 'ClOrdID': '${new_order:ClOrdID}'.", errorsReferenceNameMatrix);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    @Test
    public void testFunctionInStatic() {
        logger.info("Start testFunctionInStatic()");
        try {
            checkErrorPresents("Error in line 5 reference 'ss2' column '#static_value': Unbalansed brackets in column '#static_value'.", errorsStaticFunctionMatrix);

            checkErrorPresents("Error in line 7 reference 'ss4' column '#static_value': Invalid URI: com.exactpro.sf.actions.MiscUtils.random", errorsStaticFunctionMatrix);

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    @Test
    public void testIncorrectCollection() {
        logger.info("Start testIncorrectCollection()");
        try {
            checkErrorPresents("Error in line 6 reference 'ns' column 'NoPartyIDs': Column 'NoPartyIDs': Invalid reference format: npids1, npids2, 1", errorsCollectionMatrix);

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    @Test
    public void testCheckPointWithReference() {
        logger.info("Start testCheckPointWithReference()");
        try {
            checkErrorPresents("Error in line 6 column '#check_point': Undefined reference [s2] in column '#check_point': '%{s2}'.", errorsReferenceCheckpointMatrix);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    @Test
    public void testDublicatedColumn() {
        logger.info("Start testDublicatedColumn()");
        try {
            checkErrorPresents("Error: Invalid matrix structure. Detected duplicated fields at A1, B1,  positions. Field name is #service_name", errorsDublicatedColumnMatrix);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    @Test
    public void emptyTest() {
        logger.info("Start emptyTest()");
        try {
            checkErrorPresents("Error: Nothing to execute", errorsEmptyMatrix);

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    @Test
    public void testDoubleStart() {
        logger.info("Start testDoubleStart()");
        try {
            checkErrorPresents("Error in line 4: Unclosed block at line: 3", errorsDoubleStart);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    @Test
    public void testRequiredFields() {
        logger.info("Start testRequiredFields()");
        try {
            checkErrorPresents("Error in line 4 column '#reference': Column is missing for action: GetCheckPoint", errorsInvalidMatrix);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    @Test
    public void testForbiddenFields() {
        logger.info("Start testForbiddenFields()");
        try {
            checkErrorPresents("Error in line 8 reference '123' column '#reference': Variable '123' start with a digit", errorsInvalidMatrix);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    @Test
    public void testUnknownAction() {
        logger.info("Start testUnknownAction()");
        try {
            checkErrorPresents("Error in line 12 column '#action': Unknown action: no_such_action", errorsInvalidMatrix);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    @Test
    public void testDublicatedReference() {
        logger.info("Start testDublicatedReference()");
        try {
            checkErrorPresents("Error in line 17 reference 'ref1': Duplicated reference found in lines: 16, 17: 'ref1'", errorsInvalidMatrix);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    @Test
    public void testDublicatedTestCases() {
        logger.info("Start testDublicatedTestCases()");
        try {
            checkErrorPresents("Error in line 6 reference 'Test1' column '#reference': Duplicate reference at line: 3", errorsDublicatedTestCases);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    @Test
    public void testDublicatedIncludeBlock() {
        logger.info("Start testDublicatedIncludeBlock()");
        try {
            checkErrorPresents("Error in line 6 reference 'Test2' column '#reference': Duplicate reference at line: 3", errorsIncludeBlocks);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    @Test
    public void testIncorrectParameters() {
        logger.info("Start testIncorrectParameters()");
        try {
            checkErrorPresents("Error in line 35 column '#timeout': Value must be positive: -1", errorsInvalidMatrix);

            checkErrorPresents("Error in line 36 column '#timeout': Value must be in long format or a static variable reference: foo bar", errorsInvalidMatrix);

            checkErrorPresents("Error in line 37 reference 'ref2' column '#timeout': Value must be in long format or a static variable reference: 1.1", errorsInvalidMatrix);

            checkErrorPresents("Error in line 38 column '#messages_count': Invalid value: -1", errorsInvalidMatrix);

            checkErrorPresents("Error in line 39 column '#messages_count': Invalid value: abc", errorsInvalidMatrix);

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    @Test
    public void testMissedIncludedBlock() {
        logger.info("Start testMissedIncludedBlock()");
        try {
            checkErrorPresents("Error in line 12 reference 'ref1' column '#template': Reference to unknown block: missed_include", errorsIncludeBlocks);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    @Test
    public void testIncorrectCycles() {
        logger.info("Start testIncorrectCycles()");
        try {
            checkErrorPresents("Error in line 32: Unclosed block at line: 28", errorsIncorrectCycles);

            checkErrorPresents("Error in line 32: Unclosed block at line: 28", errorsIncorrectCycles);

            checkErrorPresents("Error in line 34: Unclosed block at line: 28", errorsIncorrectCycles);

            checkErrorPresents("Error in line 37: Unclosed block at line: 35", errorsIncorrectCycles);

            checkErrorPresents("Error in line 39: Unclosed block at line: 35", errorsIncorrectCycles);

            checkErrorPresents("Error in line 42: Unclosed block at line: 28", errorsIncorrectCycles);

            checkErrorPresents("Error in line 44: Unclosed block at line: 28", errorsIncorrectCycles);

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    @Test
    public void testIncorrectDefineService() {
        logger.info("Start testIncorrectDefineService()");
        try {
            checkErrorPresents("Error in line 3 column '#reference': Missing column", errorsIncorrectDefineService);
            checkErrorPresents("Error in line 3 column '#service_name': Missing column", errorsIncorrectDefineService);
            checkErrorPresents("Error in line 4 reference '%{ref}' column '#service_name': Unknown service: nonexistent", errorsIncorrectDefineService);
            checkErrorPresents("Error in line 5 reference 'ref' column '#reference': Invalid reference format. Expected %{ref}", errorsIncorrectDefineService);
            checkErrorPresents("Error in line 6 reference '%{r e f}' column '#reference': Invalid reference format: Variable 'r e f' contain white space", errorsIncorrectDefineService);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    @Test
    public void testIncorrectOutcomes() {
        logger.info("Start testIncorrectOutcomes()");
        try {
            checkErrorPresents("Error in line 4 column '#outcome': Invalid value: : (expected: <group>:<name>)", errorsTestsMatrix);

            checkErrorPresents("Error in line 5 column '#outcome': Invalid value: o (expected: <group>:<name>)", errorsTestsMatrix);

            checkErrorPresents("Error in line 6 column '#outcome': Invalid value: o: (expected: <group>:<name>)", errorsTestsMatrix);

            checkErrorPresents("Error in line 7 column '#outcome': Invalid outcome group:  (Invalid empty variable name)", errorsTestsMatrix);
            //no errors in line 8 o:o: == o:o, parse like [o, o]
            checkErrorPresents("Error in line 9 column '#outcome': Invalid outcome group:  (Invalid empty variable name)", errorsTestsMatrix);

            checkErrorPresents("Error in line 10 column '#outcome': Invalid value: o:o:o (expected: <group>:<name>)", errorsTestsMatrix);

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    @Test
    public void testIncorrectTypeStatic() {
        logger.info("Start testIncorrectTypeStatic()");
        try {
            checkErrorPresents("Error in line 14 reference 's1' column '#static_type': Unknown type: zzz", errorsTestsMatrix);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    @Test
    @Ignore("Test case valid, now print warning")
    public void testForbiddenReference() {
        logger.info("Start testForbiddenReference()");
        try {
            checkErrorPresents("Error in line 140 [ref] column '#reference': Column is forbidden for action: Sleep", errorsForbiddenReference);

            checkErrorPresents("Error in line 12 [123] column '#reference': Column is forbidden for action: Sleep", errorsForbiddenReference);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    @Test
    public void testFirstAndLastBlock() {
        logger.info("Start testFirstAndLastBlock()");
        try {
            checkErrorPresents("Error in line 3: No block to close", errorsFirstAndLastBlock);

            checkErrorPresents("Error in line 4: No block to close", errorsFirstAndLastBlock);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    @Test
    public void testIncorrectMessageCount() {
        logger.info("Start testIncorrectMessageCount()");
        try {
            checkErrorPresents("Error in line 18 column '#messages_count': Invalid value: #{random(10)}", errorsTestsMatrix);

            checkErrorPresents("Error in line 19 column '#messages_count': Invalid value: %{s1}", errorsTestsMatrix);

            checkErrorPresents("Error in line 20 column '#messages_count': Invalid value: >=", errorsTestsMatrix);

            checkErrorPresents("Error in line 21 column '#messages_count': Invalid value: [1..]", errorsTestsMatrix);

            checkErrorPresents("Error in line 22 column '#messages_count': Invalid value: [1..2", errorsTestsMatrix);

            checkErrorPresents("Error in line 23 column '#messages_count': Invalid value: [1-2]", errorsTestsMatrix);

            checkErrorPresents("Error in line 24 column '#messages_count': Invalid value: [${ref:count}..#{random(10)}]", errorsTestsMatrix);

            checkErrorPresents("Error in line 25 column '#messages_count': Invalid value: >1 + 3", errorsTestsMatrix);

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    @Test
    public void testInvalidIf() {
        logger.info("Start testInvalidIf()");
        try {
            checkErrorPresents("Error in line 8: Missing 'IF' statement", errorsInvalidIf);

            checkErrorPresents("Error in line 10: Missing 'IF', 'ELIF' or 'ELSE' statement", errorsInvalidIf);

            checkErrorPresents("Error in line 12: Missing 'IF' statement", errorsInvalidIf);

            checkErrorPresents("Error in line 14: Missing 'IF', 'ELIF' or 'ELSE' statement", errorsInvalidIf);

            checkErrorPresents("Error in line 18: Unclosed block at line: 16", errorsInvalidIf);

            checkErrorPresents("Error in line 19: Unclosed block at line: 16", errorsInvalidIf);

            checkErrorPresents("Error in line 19: Unclosed block at line: 3", errorsInvalidIf);

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    @Test
    public void testIncludeBlockRecursion() {
        logger.info("Start testIncludeBlockRecursion()");
        try {
            checkErrorPresents("Error in line 17: Recursion detected: b2 -> b1 -> b2", errorsIncludeBlocks);

            checkErrorPresents("Error in line 22: Recursion detected: b1 -> b2 -> b1", errorsIncludeBlocks);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    @Test
    public void testInvalidActionWithoutSettings() {
        logger.info("Start testInvalidActionWithoutSettings()");
        try {
            checkErrorPresents("Error in line 30 column '#action': Unknown action: INVALID_NoSettings", errorsTestsMatrix);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    @Test
    public void testIncorrectReferenceName() {
        logger.info("Start testIncorrectReferenceName()");
        try {
            checkErrorPresents("Error in line 47 reference 'New order' column '#reference': Variable 'New order' contain white space", errorsTestsMatrix);

            checkErrorPresents("Error in line 48 reference '1New order' column '#reference': Variable '1New order' contain white space", errorsTestsMatrix);

            checkErrorPresents("Error in line 49 reference '<' column '#reference': Variable '<' contain invalid character", errorsTestsMatrix);

            checkErrorPresents("Error in line 50 reference '_reference' column '#reference': Variable '_reference' start with invalid character '_'", errorsTestsMatrix);

            checkErrorPresents("Error in line 51 reference 'new_o#der' column '#reference': Variable 'new_o#der' contain invalid character", errorsTestsMatrix);

            checkErrorPresents("Error in line 52 reference 'new_ord%er' column '#reference': Variable 'new_ord%er' contain invalid character", errorsTestsMatrix);

            checkErrorPresents("Error in line 53 reference 'new_or der' column '#reference': Variable 'new_or der' contain white space", errorsTestsMatrix);

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    @Test
    public void testIncorrectReferenceToFilterName() {
        logger.info("Start testIncorrectReferenceToFilterName()");
        try {
            checkErrorPresents("Error in line 60 reference 'New order' column '#reference_to_filter': Variable 'New order' contain white space", errorsTestsMatrix);

            checkErrorPresents("Error in line 61 reference '1New order' column '#reference_to_filter': Variable '1New order' contain white space", errorsTestsMatrix);

            checkErrorPresents("Error in line 62 reference '<' column '#reference_to_filter': Variable '<' contain invalid character", errorsTestsMatrix);

            checkErrorPresents("Error in line 63 reference '_reference' column '#reference_to_filter': Variable '_reference' start with invalid character '_'", errorsTestsMatrix);

            checkErrorPresents("Error in line 64 reference 'new_o#der' column '#reference_to_filter': Variable 'new_o#der' contain invalid character", errorsTestsMatrix);

            checkErrorPresents("Error in line 65 reference 'new_ord%er' column '#reference_to_filter': Variable 'new_ord%er' contain invalid character", errorsTestsMatrix);

            checkErrorPresents("Error in line 66 reference 'new_or der' column '#reference_to_filter': Variable 'new_or der' contain white space", errorsTestsMatrix);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    @Test
    public void testIncorrectDefineHeader() {
        logger.info("Start testIncorrectDefineHeader()");
        try {
            checkErrorPresents("Returned message is \"Invalid matrix structure. Detected duplicated fields at S4, Y4,  positions. Field name is #itsnotacolumnmrDurden\"",
                    errorsDefinedHeaderMatrix);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    private static List<String> getList(String errors) {
        String[] problems = errors.split("\n");
        List<String> result = new ArrayList<>(Arrays.asList(problems));
        return result;
    }

    @Test
    public void testInvalidFieldOrder() {
        logger.info("Start testInvalidFieldOrder(");
        try {
            String errorMsg = "Line: 4, Column: FieldOrder, Error: [Error: unresolvable property or identifier: ClOrdID]";
            Assert.assertTrue("Test case in matrix " + INVALID_FIELDORDER + " wasn't failed", STATUS_FAILED.equals(descriptionFieldOrder.getStatus()));
            Assert.assertTrue("Wrong failed action cause", descriptionFieldOrder.getFailedAction().getCause().startsWith(errorMsg));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    private void checkErrorPresents(String expectError, List<String> errorsServiceNameMatrix) {
        Assert.assertTrue("Can't find \"" + expectError + "\" in array " + errorsServiceNameMatrix.toString(),
                errorsServiceNameMatrix.contains(expectError));
    }
}