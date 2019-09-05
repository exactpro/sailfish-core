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

import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.exceptions.APIResponseException;

public class TestEnvironmentNegative extends AbstractSFTest {

    private SFAPIClient sfapi;

    private String firstEnvironment = "firstEnvironment";
    private static final Logger logger = LoggerFactory.getLogger(TestEnvironmentNegative.class);

    @BeforeClass
    public static void setUpClass() {
        logger.info("Start negative test of environment");
    }

    @AfterClass
    public static void tearDownClass() {
        logger.info("Finish negative test of environment");
    }

    @Before
    public void setUp() throws Exception {
        sfapi = new SFAPIClient(SF_GUI_URL);
    }

    @After
    public void tearDown() {
        try {
            if (sfapi.getEnvironmentList().contains(firstEnvironment)) {
                sfapi.deleteEnvironment(firstEnvironment);
            }
            sfapi.close();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Try to create environment with empty name.
     */
    @Test
    public void testCreateIncorrectEnviroment() throws Exception {
        logger.info("Start testCreateIncorrectEnviroment()");
        try {
            try {
                sfapi.createEnvironment("");
                Assert.fail("Can't create environment with empty name, but creating was successful");
            } catch (APIResponseException e) {
                checkErrorMessage(e.getCause(), "Can't create environment with empty name");
            }
            Assert.assertFalse("Environments contain environment with empty name", sfapi.getEnvironmentList().contains(""));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Try to create existing environment.
     */
    @Test
    public void testCreateExistingEnviroment() throws Exception {
        logger.info("Start testCreateExistingEnviroment()");
        try {
            if (!sfapi.getEnvironmentList().contains(firstEnvironment)) {
                sfapi.createEnvironment(firstEnvironment);
            }
            Assert.assertTrue("Environment " + firstEnvironment + " wasn't created", sfapi.getEnvironmentList().contains(firstEnvironment));
            try {
                sfapi.createEnvironment("firstEnvironment");
                Assert.fail("Can't create existing environment " + firstEnvironment + ", but creating was successful");
            } catch (APIResponseException e) {
            }
            Assert.assertTrue("Environments doesn't contain environment " + firstEnvironment, sfapi.getEnvironmentList().contains(firstEnvironment));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Try to change environment's name to empty name.
     */
    @Test
    public void testRenameIncorrectEnvironment() throws Exception {
        logger.info("Start testRenameIncorrectEnvironment()");
        try {
            List<String> environments = sfapi.getEnvironmentList();
            if (!environments.contains(firstEnvironment)) {
                sfapi.createEnvironment(firstEnvironment);
            }
            Assert.assertTrue("Environment " + firstEnvironment + " wasn't created", sfapi.getEnvironmentList().contains(firstEnvironment));
            try {
                sfapi.renameEnvironment(firstEnvironment, "");
                Assert.fail("Can't rename environment " + firstEnvironment + " to environment with empty name, but renaming was successful");
            } catch (APIResponseException e) {
                checkErrorMessage(e.getCause(), "Can't rename environment to empty name");
            }
            Assert.assertFalse(environments.contains(firstEnvironment));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Try to delete environment with empty name
     */
    @Test
    public void testDeleteIncorrectEnvironment() throws Exception {
        logger.info("Start testDeleteIncorrectEnvironment()");
        try {
            try {
                sfapi.deleteEnvironment("");
                Assert.fail("Can't delete with empty name, but removal was successful");
            } catch (APIResponseException e) {
                checkErrorMessage(e.getCause(), "Can't remove environment with empty name");
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

}
