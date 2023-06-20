/*******************************************************************************
 * Copyright 2009-2023 Exactpro (Exactpro Systems Limited)
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

import com.exactpro.sf.scriptrunner.state.ScriptState;
import com.exactpro.sf.scriptrunner.state.ScriptStatus;
import com.exactpro.sf.testwebgui.restapi.xml.XmlTestscriptRunDescription;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class TestStaticReferencePositive extends TestMatrix {

    private static SFAPIClient sfapi;
    private final static String VALID_TEST_PATH = "aml3positive" + File.separator;
    private final static String VALID_TEST_MATRIX = "staticRefMessagesCount.csv";
    private static int testScriptID;

    private static final Logger logger = LoggerFactory.getLogger(TestStaticReferencePositive.class);

    @Rule
    public TestWatcher watcher = new TestWatcher() {
        @Override
        protected void starting(Description description) {
            super.starting(description);
            logger.info("Starting {}", description.getDisplayName());
        }
    };

    @BeforeClass
    public static void setUpClass() throws Exception {
        logger.info("Start positive static reference tests of AML syntax");
        try {
            sfapi = new SFAPIClient(TestMatrix.SF_GUI_URL);
            init(sfapi);
            testScriptID = runMatrix(sfapi, VALID_TEST_MATRIX, VALID_TEST_PATH);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    @Test
    public void testRuns() throws Exception {
        try {
            XmlTestscriptRunDescription testScript = sfapi.getTestScriptRunInfo(testScriptID);

            Assert.assertEquals(ScriptState.FINISHED, testScript.getScriptState());
            Assert.assertEquals(ScriptStatus.EXECUTED, testScript.getScriptStatus());
            Assert.assertEquals(7, testScript.getPassed());
            Assert.assertEquals(0, testScript.getFailed());
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        logger.info("Finish positive static reference tests of AML syntax");
        try {
            destroy(sfapi);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

}
