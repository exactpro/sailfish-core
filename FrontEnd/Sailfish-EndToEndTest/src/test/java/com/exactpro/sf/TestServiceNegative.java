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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.exceptions.APICallException;
import com.exactpro.sf.exceptions.APIResponseException;

public class TestServiceNegative extends AbstractSFTest {

    private static SFAPIClient sfapi;
    private static String environment = "testEnvironment";
    private static String serviceName = "testService";
    private String serviceFile = CLIENT;
    private static final Logger logger = LoggerFactory.getLogger(TestServiceNegative.class);

    @BeforeClass
    public static void setUpClass() {
        logger.info("Start negative tests of services");
        try {
            sfapi = new SFAPIClient(SF_GUI_URL);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }
    }

    @AfterClass
    public static void tearDownClass() {
        logger.info("Finish negative tests of services");
        try {
            Map<String, Service> services = sfapi.getServices(environment);
            if (services.get(serviceName) != null) {
                sfapi.deleteService(environment, services.get(serviceName));
            }
            if (sfapi.getEnvironmentList().contains(environment)) {
                sfapi.deleteEnvironment(environment);
            }
            sfapi.close();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }

    }

    @Before
    public void setUp() throws Exception {
        if (sfapi.getEnvironmentList().contains(environment))
            sfapi.deleteEnvironment(environment);
    }

    /**
     * Try to import lacking service
     */
    @Test
    public void testImportServiceWithIncorrectName() throws Exception {
        logger.info("Start testImportServiceWithIncorrectName()");
        try {
            sfapi.createEnvironment(environment);
            byte[] content = getByteContent(serviceFile);

            InputStream inputStream = new ByteArrayInputStream(content);
            List<ServiceImportResult> importedServices = null;

            String serviceName = "noService";
            try {
                importedServices = sfapi.importServices(serviceName, environment, inputStream, false, false);
                for (ServiceImportResult result : importedServices) {
                    if (serviceName.equals(result.getServiceName()))
                        Assert.fail("There is no service with name \"noService\", but it was imported.");
                }
            } catch (APIResponseException e) {
                Assert.assertTrue("Method importService() with APIResponseException return not null", importedServices == null);
                Assert.assertTrue("Environments contain sevice noService", sfapi.getServices(environment).get(serviceName) == null);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Try to import service from null InputStream
     */
    @Test
    public void testImportServiceWithIncorrectStream() throws Exception {
        logger.info("Start testImportServiceWithIncorrectStream()");
        try {
            sfapi.createEnvironment(environment);
            List<ServiceImportResult> importedServices = null;
            try {
                importedServices = sfapi.importServices(serviceFile, environment, null, false, false);
                Assert.fail("Service noService can't be import, but it has been imported.");
            } catch (APICallException e) {
                checkErrorMessage(e.getCause(), "Input stream may not be null");
            }
            Assert.assertTrue("Method importService() with APIResponseException return not null", importedServices == null);
            Assert.assertTrue("Environment " + environment + " contain services", sfapi.getServices(environment).isEmpty());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Try to start lacking service
     */
    @Test
    public void testStartIncorrectService() throws Exception {
        logger.info("Start testStartIncorrectService()");
        try {
            List<String> environments = sfapi.getEnvironmentList();
            if (!environments.contains(environment))
                sfapi.createEnvironment(environment);
            try {
                sfapi.startService(environment, "noService");
                Assert.fail("Service noService can't be started, but it has been started");
            } catch (APIResponseException e) {
                checkErrorMessage(e.getCause(), "unknown service;");
            }
            Assert.assertTrue("Environment " + environment + " contain services", sfapi.getServices(environment).isEmpty());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Try to delete lacking service
     */
    @Test
    public void testDeleteIncorrectService() throws Exception {
        logger.info("Start testDeleteIncorrectService()");
        try {
            List<String> environments = sfapi.getEnvironmentList();
            if (!environments.contains(environment))
                sfapi.createEnvironment(environment);
            try {
                sfapi.deleteService(environment, "noService");
                Assert.fail("Service noService can't be deleted, but it has been deleted");
            } catch (APIResponseException e) {
                checkErrorMessage(e.getCause(), "noService was not found;");
            }
            Assert.assertTrue("Environment " + environment + " contain services", sfapi.getServices(environment).isEmpty());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Try to delete lacking service using additional arguments
     */
    @Test
    public void testDeleteIncorrectServiceWithArgs() throws Exception {
        logger.info("Start testDeleteIncorrectServiceWithArgs()");
        try {
            List<String> environments = sfapi.getEnvironmentList();
            if (!environments.contains(environment))
                sfapi.createEnvironment(environment);
            try {
                sfapi.deleteService("environment=" + environment + "&service=" + "noService" + "&deleteOnDisk=false"
                        + "&replaceexisting=false&skipexisting=false");
                Assert.fail("Service noService can't be deleted, but it has been deleted");
            } catch (APIResponseException e) {
                checkErrorMessage(e.getCause(), "noService was not found;");
            }
            Assert.assertTrue("Environment " + environment + " contain services", sfapi.getServices(environment).isEmpty());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }
}