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
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.exceptions.APICallException;
import com.exactpro.sf.exceptions.APIResponseException;

public class TestConnectionManagerSynchronization extends AbstractSFTest {

    private static SFAPIClient sfapi;
    private static final String PATH = "connectionManagerServices" + File.separator;
    private static List<ServiceImportResult> importedServices = new ArrayList<ServiceImportResult>();
    private static final Logger logger = LoggerFactory.getLogger(TestConnectionManagerSynchronization.class);

    @BeforeClass
    public static void setUpClass() throws Exception {
        logger.info("Start tests of connection");
        try {
            sfapi = new SFAPIClient(SF_GUI_URL);
            String serviceFile;
            for (int i = 1; i < 11; i++) {
                serviceFile = PATH + "fake" + i + ".xml";
                byte[] content = getByteContent(serviceFile);
                InputStream inputStream = new ByteArrayInputStream(content);
                importedServices.addAll(sfapi.importServices(serviceFile, "default", inputStream, false, false));
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        logger.info("Finish tests of connection");
        try {
            for (ServiceImportResult serviceImport : importedServices) {
                sfapi.deleteService("default", serviceImport.getServiceName());
            }
            sfapi.close();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    @Test
    public void testConnection() throws Exception {
        try {
            // ----Import----//
            if (importedServices.isEmpty())
                Assert.fail("There is no service in connectionManagerServices");
            try {
                int timewait = 0;
                while (timewait != 1000 && !this.isInitialized()) {
                    timewait++;
                    Thread.sleep(10);
                }
            } catch (InterruptedException e) {
                throw new APICallException(e);
            }
            Assert.assertTrue(this.isInitialized());

            // ----Start----//
            for (ServiceImportResult serviceImport : importedServices) {
                sfapi.startService("default", serviceImport.getServiceName());
            }
            try {
                int timewait = 0;
                while (timewait != 1000 && !this.isStarted()) {
                    timewait++;
                    Thread.sleep(10);
                }
            } catch (InterruptedException e) {
                throw new APICallException(e);
            }
            Assert.assertTrue(this.isStarted());

            // ----Stop----//
            for (ServiceImportResult serviceImport : importedServices) {
                sfapi.stopService("default", serviceImport.getServiceName());
            }
            try {
                int timewait = 0;
                while (timewait != 1000 && !this.isDisposed()) {
                    timewait++;
                    Thread.sleep(10);
                }
            } catch (InterruptedException e) {
                throw new APICallException(e);
            }
            Assert.assertTrue(this.isDisposed());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    private boolean isInitialized() throws APIResponseException, APICallException {
        for (ServiceImportResult serviceImport : importedServices) {
            if (!Service.Status.INITIALIZED.equals(sfapi.getServices("default").get(serviceImport.getServiceName()).getStatus()))
                return false;
        }
        return true;
    }

    private boolean isStarted() throws APIResponseException, APICallException {
        for (ServiceImportResult serviceImport : importedServices) {
            if (!Service.Status.STARTED.equals(sfapi.getServices("default").get(serviceImport.getServiceName()).getStatus()))
                return false;
        }
        return true;
    }

    private boolean isDisposed() throws APIResponseException, APICallException {
        for (ServiceImportResult serviceImport : importedServices) {
            if (!Service.Status.DISPOSED.equals(sfapi.getServices("default").get(serviceImport.getServiceName()).getStatus()))
                return false;
        }
        return true;
    }

}
