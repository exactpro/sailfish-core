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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.Service.Status;

public class TestServicePositive extends AbstractSFTest {

    private static SFAPIClient sfapi;
    private static String environment = "testEnvironment1d";
    private static String serviceName = CLIENT_NAME;
    private static String serviceFile = CLIENT;
    private static String service = "equalNames.xml";
    private static String serviceEqualName = "names";
    private static List<ServiceImportResult> importedServices = null;
    private static final Logger logger = LoggerFactory.getLogger(TestServicePositive.class);

    @BeforeClass
    public static void setUpClass() throws Exception {
        logger.info("Start positive tests of services");
        try {
            sfapi = new SFAPIClient(SF_GUI_URL);
            if (sfapi.getEnvironmentList().contains(environment)) {
                sfapi.deleteEnvironment(environment);
            }
            List<String> environments = sfapi.getEnvironmentList();
            if (!environments.contains(environment)) {
                sfapi.createEnvironment(environment);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        logger.info("Finish positive tests of services");
        try {
            if (sfapi.getEnvironmentList().contains(environment)) {
                sfapi.deleteEnvironment(environment);
            }
            sfapi.close();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Try to import service, check it's existence and status
     */
    @Test
    public void testImportService() throws Exception {
        logger.info("Start testImportService()");
        try {
            Map<String, Service> services = sfapi.getServices(environment);
            if (services.get(serviceName) != null) {
                sfapi.deleteService(environment, serviceName);
            }
            byte[] content = getByteContent(serviceFile);
            InputStream inputStream = new ByteArrayInputStream(content);
            importedServices = sfapi.importServices(serviceFile, environment, inputStream, false, false);
            if (importedServices.isEmpty())
                Assert.fail("There is no service in " + serviceFile);
            boolean serviceExist = false;
            for (ServiceImportResult serviceImport : importedServices)
                if (serviceName.equals(serviceImport.getServiceName()))
                    serviceExist = true;
            Assert.assertTrue("Result of the method importService(...) doesn't contain " + serviceName, serviceExist);
            awaitServiceStatus(environment, serviceName, 1000, Status.INITIALIZED);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Try to start service and check status. If service isn't upload, try to
     * import it
     */
    @Test
    public void testStartService() throws Exception {
        logger.info("Start testStartService()");
        try {
            List<String> environments = sfapi.getEnvironmentList();
            if (!environments.contains(environment)) {
                sfapi.createEnvironment(environment);
            }
            startServer(sfapi, environment);
            Map<String, Service> services = sfapi.getServices(environment);
            boolean imported = false;
            if (services.get(serviceName) == null) {
                byte[] content = getByteContent(serviceFile);
                InputStream inputStream = new ByteArrayInputStream(content);
                sfapi.importServices(serviceFile, environment, inputStream, false, false);
                imported = true;
            }
            services = sfapi.getServices(environment);
            if (services.get(serviceName) != null) {
                if (imported) {
                    awaitServiceStatus(environment, serviceName, 3000, Status.INITIALIZED);
                }

                sfapi.startService(environment, serviceName);
                awaitServiceStatus(environment, serviceName, 10000, Status.STARTED);
            } else
                Assert.fail("Service " + serviceFile + " hasn't been imported");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Try to stop started service. If service isn't start, start it. If it
     * isn't upload - import it
     */
    @Test
    public void testStopService() throws Exception {
        logger.info("Start testStopService()");
        try {
            List<String> environments = sfapi.getEnvironmentList();
            if (!environments.contains(environment)) {
                sfapi.createEnvironment(environment);
            }
            startServer(sfapi, environment);
            Map<String, Service> services = sfapi.getServices(environment);
            boolean imported = false;
            if (services.get(serviceName) == null) {
                byte[] content = getByteContent(serviceFile);
                InputStream inputStream = new ByteArrayInputStream(content);
                sfapi.importServices(serviceFile, environment, inputStream, false, false);
                imported = true;
            }
            services = sfapi.getServices(environment);
            if (services.get(serviceName) != null) {

                if (imported) {
                    awaitServiceStatus(environment, serviceName, 3000, Status.INITIALIZED);
                }

                sfapi.startService(environment, serviceName);
                awaitServiceStatus(environment, serviceName, 10000, Status.STARTED);

                sfapi.stopService(environment, serviceName);
                awaitServiceStatus(environment, serviceName, 10000, Status.DISPOSED);
            } else
                Assert.fail("Service " + serviceFile + " hasn't been imported");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Try to delete service. If it isn't upload - import it
     */
    @Test
    public void testDeleteService() throws Exception {
        logger.info("Start testDeleteService()");
        try {
            List<String> environments = sfapi.getEnvironmentList();
            if (!environments.contains(environment))
                sfapi.createEnvironment(environment);
            Map<String, Service> services = sfapi.getServices(environment);
            if (services.get(serviceName) == null) {
                byte[] content = getByteContent(serviceFile);
                InputStream inputStream = new ByteArrayInputStream(content);
                sfapi.importServices(serviceFile, environment, inputStream, false, false);
            }
            services = sfapi.getServices(environment);
            if (services.get(serviceName) != null) {
                sfapi.deleteService(environment, serviceName);
                services = sfapi.getServices(environment);
                Assert.assertFalse("Services contain " + serviceName + " after deleting", services.containsKey(serviceName));
            } else
                Assert.fail("Service " + serviceName + " hasn't been found");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Try to delete service using arguments
     */
    @Test
    public void testDeleteServiceWithArgs() throws Exception {
        logger.info("Start testDeleteServiceWithArgs()");
        try {
            List<String> environments = sfapi.getEnvironmentList();
            if (!environments.contains(environment))
                sfapi.createEnvironment(environment);
            Map<String, Service> services = sfapi.getServices(environment);
            if (services.get(serviceName) == null) {
                byte[] content = getByteContent(serviceFile);
                InputStream inputStream = new ByteArrayInputStream(content);
                sfapi.importServices(serviceFile, environment, inputStream, false, false);
            }
            services = sfapi.getServices(environment);
            if (services.get(serviceName) != null) {
                sfapi.deleteService("environment=" + environment + "&service=" + serviceName + "&deleteOnDisk=false"
                        + "&replaceexisting=false&skipexisting=false");
                services = sfapi.getServices(environment);
                Assert.assertFalse("Services contain " + serviceName, services.containsKey(serviceName));
            } else
                Assert.fail("Service " + serviceName + " hasn't been found");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Try to delete all services on the special environment
     */
    @Test
    public void testDeleteAllServices() throws Exception {
        logger.info("Start testDeleteAllServices()");
        try {
            List<String> environments = sfapi.getEnvironmentList();
            if (!environments.contains(environment))
                sfapi.createEnvironment(environment);
            Map<String, Service> services = sfapi.getServices(environment);
            if (services.get(serviceName) == null) {
                byte[] content = getByteContent(serviceFile);
                InputStream inputStream = new ByteArrayInputStream(content);
                sfapi.importServices(serviceFile, environment, inputStream, false, false);
            }
            services = sfapi.getServices(environment);
            if (services.get(serviceName) != null) {
                sfapi.deleteAllServices(environment);
                services = sfapi.getServices(environment);
                Assert.assertTrue("Environent " + environment + " after deleteing all services contain some services", services.isEmpty());
            } else
                Assert.fail("Service " + serviceName + " hasn't been found");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Try to import a few services with equal name simultaneous
     *
     * @throws Exception
     */
    @Test
    public void testImportServicesWithEqualName() throws Exception {
        logger.info("Start testImportServicesWithEqualName()");
        try {
            CyclicBarrier barrier = new CyclicBarrier(5);
            List<ImportService> importServices = new ArrayList<>();

            for (int i = 0; i < 5; i++) {
                importServices.add(new ImportService(barrier));
            }
            for (ImportService thread : importServices) {
                thread.start();
            }

            for (ImportService thread : importServices) {
                if (!thread.getExceptions().isEmpty())
                    Assert.fail(thread.getExceptions().get(0).getMessage());
                thread.join(3000);
            }
            int count = 0;
            Map<String, Service> services = sfapi.getServices(environment);
            for (String name : services.keySet())
                if (serviceEqualName.equals(services.get(name).getName()))
                    count++;
            Assert.assertTrue("RESTAPI upload " + count + " services with equal names", count == 1);
            Assert.assertTrue("Services doesn't contain " + serviceEqualName, services.get(serviceEqualName) != null);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        } /*
           * finally{
           * if(sfapiLocal.get().getEnvironmentList().contains(environment)){
           * sfapiLocal.get().deleteEnvironment(environment); }
           * sfapiLocal.get().close(); }
           */
    }

    @Test
    public void testDoubleStartService() throws Exception {
        logger.info("Start testDoubleStartService()");
        try {
            List<String> environments = sfapi.getEnvironmentList();
            if (!environments.contains(environment)) {
                sfapi.createEnvironment(environment);
            }
            startServer(sfapi, environment);
            Map<String, Service> services = sfapi.getServices(environment);
            boolean imported = false;
            if (services.get(serviceName) == null) {
                byte[] content = getByteContent(serviceFile);
                InputStream inputStream = new ByteArrayInputStream(content);
                sfapi.importServices(serviceFile, environment, inputStream, false, false);
                imported = true;
            }
            services = sfapi.getServices(environment);
            if (services.get(serviceName) != null) {
                if (imported) {
                    awaitServiceStatus(environment, serviceName, 3000, Status.INITIALIZED);
                }

                if (!Status.STARTED.equals(services.get(serviceName).getStatus())) {
                    sfapi.startService(environment, serviceName);
                    awaitServiceStatus(environment, serviceName, 10000, Status.STARTED);
                }

                sfapi.stopService(environment, serviceName);
                awaitServiceStatus(environment, serviceName, 10000, Status.DISPOSED);

                sfapi.startService(environment, serviceName);
                awaitServiceStatus(environment, serviceName, 10000, Status.STARTED);

                sfapi.stopService(environment, serviceName);
                awaitServiceStatus(environment, serviceName, 10000, Status.DISPOSED);
            } else
                Assert.fail("Service " + serviceFile + " hasn't been imported");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    private void awaitServiceStatus(String environment, String serviceName, long await, Status expectedStatus) throws Exception {
        awaitServiceStatus(sfapi, environment, serviceName, await, expectedStatus);
        Map<String, Service> services = sfapi.getServices(environment);
        Assert.assertTrue("Services doesn't contain " + serviceName, services.containsKey(serviceName));
        Assert.assertEquals("Wrong service status", expectedStatus, services.get(serviceName).getStatus());
    }

    class ImportService extends Thread {


        private CyclicBarrier barrier;
        private ThreadLocal<SFAPIClient> sfapiLocal = new ThreadLocal<>();
        private List<Exception> exceptions = new ArrayList<>();
        public ImportService(CyclicBarrier barrier) {
            this.barrier = barrier;
        }

        public List<Exception> getExceptions() {
            return this.exceptions;
        }

        @Override
        public void run() {
            try {
                if (sfapiLocal.get() == null) {
                    sfapiLocal.set(new SFAPIClient(SF_GUI_URL));
                }
                byte[] content = getByteContent(service);
                InputStream inputStream = new ByteArrayInputStream(content);
                barrier.await();
                sfapiLocal.get().importServices(service, environment, inputStream, false, false);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                this.exceptions.add(e);
            }
        }

    }

    private void startServer(SFAPIClient sfapi, String environment) throws Exception {
        startService(sfapi, SERVER, SERVER_NAME, environment);
    }
}