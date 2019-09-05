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

public class TestEnvironmentPositive extends AbstractSFTest {

    private SFAPIClient sfapi;

    private String firstEnvironment = "firstEnvironment";
    private String secondEnvironment = "secondEnvironment";

    private static final Logger logger = LoggerFactory.getLogger(TestEnvironmentPositive.class);

    @BeforeClass
    public static void setUpClass() {
        logger.info("Start positive tests of environment");
    }

    @AfterClass
    public static void tearDownClass() {
        logger.info("Finish positive test of environment");
    }

    @Before
    public void setUp() throws Exception {
        sfapi = new SFAPIClient(SF_GUI_URL);
    }

    @After
    public void tearDown() {
        try {
            List<String> environments = sfapi.getEnvironmentList();
            if (environments.contains(firstEnvironment)) {
                sfapi.deleteEnvironment(firstEnvironment);
            }
            if (sfapi.getEnvironmentList().contains(secondEnvironment)) {
                sfapi.deleteEnvironment(secondEnvironment);
            }
            sfapi.close();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Try to create environment and check it's existence
     */
    @Test
    public void testCreateEnviroment() throws Exception {
        logger.info("Start testCreateEnviroment()");
        try {
            List<String> environments = sfapi.getEnvironmentList();
            if (environments.contains(firstEnvironment))
                sfapi.deleteEnvironment(firstEnvironment);
            sfapi.createEnvironment(firstEnvironment);
            environments = sfapi.getEnvironmentList();
            Assert.assertTrue("Environments doen't contain " + firstEnvironment, environments.contains(firstEnvironment));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Try to rename environment and check it's name
     */
    @Test
    public void testRenameEnvironment() throws Exception {
        logger.info("Start testRenameEnvironment()");
        try {
            List<String> environments = sfapi.getEnvironmentList();
            if (environments.contains(secondEnvironment)) {
                sfapi.deleteEnvironment(secondEnvironment);
            }
            if (!environments.contains(firstEnvironment)) {
                sfapi.createEnvironment(firstEnvironment);
            }
            sfapi.renameEnvironment(firstEnvironment, secondEnvironment);
            environments = sfapi.getEnvironmentList();
            Assert.assertTrue("Environments doesn't contain renamed environment " + secondEnvironment, environments.contains(secondEnvironment));
            Assert.assertFalse("Environments contain environment with old name " + firstEnvironment, environments.contains(firstEnvironment));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Try to delete environment and check it's absence
     */
    @Test
    public void testDeleteEnvironment() throws Exception {
        logger.info("Start testDeleteEnvironment()");
        try {
            List<String> environments = sfapi.getEnvironmentList();
            if (!environments.contains(firstEnvironment)) {
                sfapi.createEnvironment(firstEnvironment);
            }
            sfapi.deleteEnvironment(firstEnvironment);
            environments = sfapi.getEnvironmentList();
            Assert.assertFalse("Environment " + firstEnvironment + "wasn't deleted", environments.contains(firstEnvironment));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

}
