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
package com.exactpro.sf.services.ntg;

import static org.mockito.Mockito.mock;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.services.CollectorServiceHandler;
import com.exactpro.sf.services.IEnvironmentMonitor;
import com.exactpro.sf.services.ServiceException;
import com.exactpro.sf.services.ServiceStatus;
import com.exactpro.sf.services.ntg.exceptions.NotLoggedInException;
import com.exactpro.sf.util.TestNTGClientBase;

public class TestNTGClientNegative extends TestNTGClientBase {

    private static final int TIMEOUT = 500;

    private static final Logger logger = LoggerFactory.getLogger(TestNTGClientNegative.class);

    @BeforeClass
    public static void setUpClass() throws Exception {
        try {
            startServer(port, dictionaryName, getServerSettings());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    @AfterClass
    public static void tearDownClass() {
        server.dispose();
        Assert.assertEquals(ServiceStatus.DISPOSED, server.getStatus());
    }

    /**
     * Try to invalid initialize NTGClient
     */

    @Test
    public void testInvalidInitialization() {
        ServiceName serviceName = new ServiceName(ServiceName.DEFAULT_ENVIRONMENT, "NTGTCPIPServer");
        handlerServer = new CollectorServiceHandler();
        IEnvironmentMonitor mockedMonitor = mock(IEnvironmentMonitor.class);
        dictionaryManager = serviceContext.getDictionaryManager();
        NTGClientSettings settingsClient = new NTGClientSettings();
        settingsClient.setDictionaryName(dictionaryName);
        settingsClient.setServerIP(host);
        settingsClient.setServerPort(port);
        settingsClient.setLogin("Test");
        settingsClient.setPassword("Test");
        settingsClient.setLoginTimeout(TIMEOUT);
        settingsClient.setDoLoginOnStart(false);
        settingsClient.setHeartbeatTimeout(TIMEOUT);
        settingsClient.setMaxMissedHeartbeats(maxHeartbeats);

        client = new NTGClient();
        handlerClient = new CollectorServiceHandler();

        try {
            client.init(serviceContext, null, handlerClient, settingsClient, serviceName);
            Assert.fail("There is no exception was thrown");
        } catch (NullPointerException e) {
            Assert.assertEquals("serviceMonitor cannot be null", e.getMessage());
        }

        try {
            client.init(serviceContext, mockedMonitor, handlerClient, null, serviceName);
            Assert.fail("There is no exception was thrown");
        } catch(ServiceException e) {
            Assert.assertEquals("settings cannot be null", e.getCause().getMessage());
        }

        try {
            client.init(serviceContext, mockedMonitor, null, settingsClient, serviceName);
            Assert.fail("There is no exception was thrown");
        } catch(ServiceException e) {
            Assert.assertEquals("handler cannot be null", e.getCause().getMessage());
        }
    }

    /**
     * Try to sent New Order from client before logged in with
     * LowLevelService=false
     * @throws Exception
     */

    @Test
    public void testSentMessageBeforeLogin() throws Exception {
        try {
            startServices(false, false, TIMEOUT);
            ((NTGClientSettings) client.getSettings()).setLowLevelService(false);
            try {
                sentNewOrder(true);
                Assert.fail("There is no exception was thrown");
            } catch (NotLoggedInException e) {
            }
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            throw e;
        } finally {
            client.dispose();
            Assert.assertEquals(ServiceStatus.DISPOSED, client.getStatus());

        }
    }

    /**
     * Try to start client and start again
     * @throws Exception
     */

    @Test
    public void testDoubleStart() throws Exception {
        try {
            startServices(true, true, TIMEOUT);
            try {
                client.start();
                Assert.fail("There is no exception was thrown");
            } catch(ServiceException e) {
                Assert.assertEquals("Service should be initialized before starting", e.getCause().getMessage());
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        } finally {
            client.dispose();
            Assert.assertEquals(ServiceStatus.DISPOSED, client.getStatus());
        }
    }

    /**
     * Try to connect by client to not started server
     */

    @Test
    public void testConnectToNotStartedServer() throws Exception {
        try {
            initializedServer(incorrectPort, dictionaryName, getServerSettings());
            ServiceName serviceName = new ServiceName(ServiceName.DEFAULT_ENVIRONMENT, "NTGTCPIPServer");
            handlerServer = new CollectorServiceHandler();
            IEnvironmentMonitor mockedMonitor = mock(IEnvironmentMonitor.class);
            dictionaryManager = serviceContext.getDictionaryManager();
            NTGClientSettings settingsClient = new NTGClientSettings();
            settingsClient.setDictionaryName(dictionaryName);
            settingsClient.setServerIP(host);
            settingsClient.setServerPort(incorrectPort);
            settingsClient.setLogin("Test");
            settingsClient.setPassword("Test");
            settingsClient.setLoginTimeout(TIMEOUT);
            settingsClient.setDoLoginOnStart(false);
            settingsClient.setHeartbeatTimeout(TIMEOUT);
            settingsClient.setMaxMissedHeartbeats(maxHeartbeats);

            client = new NTGClient();
            handlerClient = new CollectorServiceHandler();
            client.init(serviceContext, mockedMonitor, handlerClient, settingsClient, serviceName);
            try {
                client.start();
                Assert.fail("There is no exception was thrown");
            } catch(ServiceException e) {
                Assert.assertEquals("Cannot establish session to address: localhost:9888", e.getCause().getMessage());
            } finally {
                if (client.isConnected()) {
                    client.dispose();
                    Assert.assertEquals(ServiceStatus.DISPOSED, client.getStatus());
                }
            }

        } catch (Exception e) {
            logger.error("Test fail because: {}", e.getMessage(), e);
            throw e;
        }
    }

}
