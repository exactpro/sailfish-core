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
package com.exactpro.sf.services.itch;

import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.services.CollectorServiceHandler;
import com.exactpro.sf.services.ServiceException;
import com.exactpro.sf.services.ServiceStatus;
import com.exactpro.sf.util.TestITCHClientBase;
import junit.framework.Assert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestITCHClientNegative extends TestITCHClientBase {

    private static final Logger logger = LoggerFactory.getLogger(TestITCHClientNegative.class);

    private static final int WAITING_TIMEOUT = 500;
    private static final int DISCONNECT_WAITING_TIMEOUT = 3000;
    private static final int RECONNECTION_TIMEOUT = 3000;
    private static final int LOOP_TIMEOUT = 50;

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
        client.dispose();
    }


    @Test
    public void testInvalidInitialization() {
        logger.info("Start testInvalidInitialization()");
        ITCHTCPClientSettings settingsClient = new ITCHTCPClientSettings();
        settingsClient.setDictionaryName(dictionaryName);
        settingsClient.setAddress(host);
        settingsClient.setPort(port);
        settingsClient.setIdleTimeout(1);
        settingsClient.setDoLoginOnStart(false);
        settingsClient.setHeartbeatTimeout(WAITING_TIMEOUT);
        settingsClient.setUsername("Test");
        settingsClient.setSendHeartBeats(true);
        settingsClient.setDoLiteLoginOnStart(false);
        settingsClient.setDisposeWhenSessionClosed(true);
        client = new ITCHTcpClient();
        handlerClient = new CollectorServiceHandler();

        try {
            client.init(serviceContext, null, handlerClient, settingsClient, serviceName);
            Assert.fail("There is no exception was thrown");
        } catch(NullPointerException e) {
            Assert.assertEquals("serviceMonitor cannot be null", e.getMessage());
        }

        try {
            client.init(serviceContext, mockedMonitor, handlerClient, settingsClient, null);
            Assert.fail("There is no exception was thrown");
        } catch(NullPointerException e) {
            Assert.assertEquals("serviceName cannot be null", e.getMessage());
        }

        try {
            client.init(null, mockedMonitor, handlerClient, settingsClient, serviceName);
            Assert.fail("There is no exception was thrown");
        } catch(NullPointerException e) {
            Assert.assertEquals("serviceContext cannot be null", e.getMessage());
        }

        try {
            client.init(serviceContext, mockedMonitor, handlerClient, null, serviceName);
            Assert.fail("There is no exception was thrown");
        } catch (ServiceException e) {
            checkExceptionText(e, "settings cannot be null");
        }

        try {
            client.init(serviceContext, mockedMonitor, null, settingsClient, serviceName);
            Assert.fail("There is no exception was thrown");
        } catch (ServiceException e) {
            checkExceptionText(e, "handler cannot be null");
        }
    }

    /**
     * @param e
     */
    private void checkExceptionText(ServiceException e, String text) {
        Assert.assertEquals("Failed to initialize service", e.getMessage());
        Assert.assertEquals(text, e.getCause().getMessage());
    }


    @Test
    public void testConnectToNotStartedServer() throws Exception {
        logger.info("Start testConnectToNotStartedServer()");
        try {
            server.dispose();
            long time = DISCONNECT_WAITING_TIMEOUT + System.currentTimeMillis();
            while(System.currentTimeMillis()<time && ServiceStatus.STARTED.equals(server.getStatus())){
                Thread.sleep(LOOP_TIMEOUT);
            }
            Assert.assertEquals(ServiceStatus.DISPOSED, server.getStatus());
            initClient(false, true, false, WAITING_TIMEOUT, RECONNECTION_TIMEOUT);
            try {
                client.start();
                Assert.fail("There is no exception was thrown");
            } catch (Exception e) {
                Assert.assertEquals("Cannot establish session to address: localhost:9801", e.getCause().getMessage());
            } finally {
                if (client.isConnected()) {
                    client.dispose();
                    Assert.assertEquals(ServiceStatus.DISPOSED, client.getStatus());
                }
                server.start();
                while(System.currentTimeMillis()<time && ServiceStatus.DISPOSED.equals(server.getStatus())){
                    Thread.sleep(LOOP_TIMEOUT);
                }
                Assert.assertEquals(ServiceStatus.STARTED, server.getStatus());
            }
        } catch (Exception e) {
            logger.error("Test fail because: {}", e.getMessage(), e);
            throw e;
        }
    }


    @Test
    public void testSentMessageBeforeConnect() throws Exception {
        logger.info("Start testSentMessageBeforeConnect()");
        try {
            initClient(false, false, false, WAITING_TIMEOUT, RECONNECTION_TIMEOUT);
            if(client.isConnected()){
                client.disconnect();
                long time = DISCONNECT_WAITING_TIMEOUT + System.currentTimeMillis();
                while(System.currentTimeMillis()<time && client.isConnected()){
                    Thread.sleep(LOOP_TIMEOUT);
                }
                Assert.assertFalse(client.isConnected());
            }
            try {
                sentAddOrder(true);
                Assert.fail("There is no exception was thrown");
            } catch (EPSCommonException e) {
                Assert.assertEquals("Could not send message. Client is not connected", e.getMessage());
            }
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            throw e;
        } finally {
            if(ServiceStatus.STARTED.equals(client.getStatus())){
                client.dispose();
                Assert.assertEquals(ServiceStatus.DISPOSED, client.getStatus());
            }
        }
    }
}
