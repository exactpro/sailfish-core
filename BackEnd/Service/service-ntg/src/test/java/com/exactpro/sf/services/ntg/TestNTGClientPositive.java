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

import java.util.List;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.util.Pair;
import com.exactpro.sf.comparison.ComparisonResult;
import com.exactpro.sf.scriptrunner.StatusType;
import com.exactpro.sf.services.ServiceException;
import com.exactpro.sf.services.ServiceHandlerRoute;
import com.exactpro.sf.services.ServiceStatus;
import com.exactpro.sf.services.ntg.NTGClient.NTGClientState;
import com.exactpro.sf.util.TestNTGClientBase;
import com.exactpro.sf.util.WaitLogonResponse;

public class TestNTGClientPositive extends TestNTGClientBase {

    private static final Logger logger = LoggerFactory.getLogger(TestNTGClientPositive.class);
    private static final int LOGIN_TIMEOUT = 500;
    private static final int HEARTBEAT_MESSAGE_NUMBER = 1;
    private static final int WAITING_TIMEOUT = 500;
    private static final int LOOP_TIMEOUT = 10;

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
     * Test of connecting to server with doLogonOnStart=true and test of
     * heartbeat exchanging
     */
    @Test
    public void testConnectionWithLogin() {
        try {
            startServices(true, true, LOGIN_TIMEOUT);
            Assert.assertTrue(client.isConnected());
            Assert.assertEquals(NTGClientState.LoggedIn, client.getState());
            IMessage heartbeat=sentHeartbeat(HEARTBEAT_MESSAGE_NUMBER);
            Pair<IMessage, ComparisonResult> result = waitMessage(client, heartbeat, WAITING_TIMEOUT, ServiceHandlerRoute.TO_ADMIN,
                    dictionaryName);
            Assert.assertFalse("ComparisionResult is null", result.getSecond() == null);
            Assert.assertEquals("StatusType is " + result.getSecond().getStatus(), result.getSecond().getStatus(), StatusType.PASSED);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        } finally {
            client.dispose();
            Assert.assertEquals(ServiceStatus.DISPOSED, client.getStatus());
        }
    }

    /**
     * Test of connecting to server with doLogonOnStart=true and test sending
     * New Order
     */
    @Test
    public void testNewOrder() throws InterruptedException {
        try {
            startServices(true, true, LOGIN_TIMEOUT);
            Assert.assertTrue(client.isConnected());
            Assert.assertEquals(NTGClientState.LoggedIn, client.getState());
            IMessage messageNewOrder = sentNewOrder(true);
            Pair<IMessage, ComparisonResult> result = waitMessage(client, messageNewOrder, WAITING_TIMEOUT, ServiceHandlerRoute.TO_APP,
                    dictionaryName);
            Assert.assertFalse("ComparisionResult is null", result.getSecond() == null);
            Assert.assertEquals("StatusType is " + result.getSecond().getStatus(), result.getSecond().getStatus(), StatusType.PASSED);
            result = waitMessage(server, messageNewOrder, WAITING_TIMEOUT, ServiceHandlerRoute.FROM_APP,
                    dictionaryName);
            Assert.assertFalse("ComparisionResult is null", result.getSecond() == null);
            Assert.assertEquals("StatusType is " + result.getSecond().getStatus(), result.getSecond().getStatus(), StatusType.PASSED);
            messageNewOrder = sentNewOrder(false);
            result = waitMessage(client, messageNewOrder, WAITING_TIMEOUT, ServiceHandlerRoute.FROM_APP, dictionaryName);
            Assert.assertFalse("ComparisionResult is null", result.getSecond() == null);
            Assert.assertEquals("StatusType is " + result.getSecond().getStatus(), result.getSecond().getStatus(), StatusType.PASSED);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        } finally {
            client.dispose();
            Assert.assertEquals(ServiceStatus.DISPOSED, client.getStatus());
        }
    }

    /**
     * Test of connecting to server with doLogonOnStart=false, log in after and
     * test of heartbeat exchanging
     */
    @Test
    public void testConnectionWithoutLogin() throws InterruptedException {
        Thread waitThread = null;
        try {
            startServices(false, false, LOGIN_TIMEOUT);
            WaitLogonResponse wait = new WaitLogonResponse(LOGIN_TIMEOUT, getLogonReply(), "Logon", server);
            waitThread = new Thread(wait);
            waitThread.start();
            client.login();
            long timeEnd = System.currentTimeMillis() + LOGIN_TIMEOUT;
            while (timeEnd > System.currentTimeMillis() && !"LoggedIn".equals(client.getState().toString())) {
                Thread.sleep(LOOP_TIMEOUT);
            }
            Assert.assertTrue("Client wasn't connected", client.isConnected());
            Assert.assertEquals(NTGClientState.LoggedIn, client.getState());
            IMessage heartbeat = sentHeartbeat(HEARTBEAT_MESSAGE_NUMBER);
            Pair<IMessage, ComparisonResult> result = waitMessage(client, heartbeat, WAITING_TIMEOUT, ServiceHandlerRoute.TO_ADMIN, dictionaryName);
            Assert.assertFalse("ComparisionResult is null", result.getSecond() == null);
            Assert.assertEquals("StatusType is " + result.getSecond().getStatus(), result.getSecond().getStatus(), StatusType.PASSED);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        } finally {
            client.dispose();
            Assert.assertEquals(ServiceStatus.DISPOSED, client.getStatus());
            if (waitThread != null)
                waitThread.interrupt();
        }
    }

    /**
     * Test of timeout of waiting LogonReply
     */
    @Test
    public void testLoginTimeout() {
        try {
            startServices(true, false, LOGIN_TIMEOUT);
        } catch(ServiceException e) {
            Assert.assertEquals(NTGClientState.SessionClosed, client.getState());
            Assert.assertEquals(ServiceStatus.ERROR, client.getStatus());
        } finally {
            client.dispose();
            Assert.assertEquals(ServiceStatus.DISPOSED, client.getStatus());
        }
    }

    /**
     * Test of heartbeat sending without logged in on server with setting
     * doLogonOnStart=true
     */
    @Test
    public void testSentHeartbeatBeforeLogin() {
        try {
            startServices(true, false, LOGIN_TIMEOUT);
        } catch(ServiceException e) {
            IMessage heartbeat = sentHeartbeat(HEARTBEAT_MESSAGE_NUMBER);
            Pair<IMessage, ComparisonResult> result = waitMessage(client, heartbeat, WAITING_TIMEOUT, ServiceHandlerRoute.TO_ADMIN, dictionaryName);
            Assert.assertTrue("ComparisionResult isn't null: " + result.getSecond(), result.getSecond() == null);
            Assert.assertEquals(ServiceStatus.ERROR, client.getStatus());
            Assert.assertEquals(NTGClientState.SessionClosed, client.getState());
        } finally {
            client.dispose();
            Assert.assertEquals(ServiceStatus.DISPOSED, client.getStatus());
        }
    }

    /**
     * Test of heartbeat sending without logged in on server with setting
     * doLogonOnStart=false
     */
    @Test
    public void testSentHeartbeatWithoutLogin() {
        try {
            startServices(false, false, LOGIN_TIMEOUT);
            IMessage heartbeat = sentHeartbeat(HEARTBEAT_MESSAGE_NUMBER);
            Pair<IMessage, ComparisonResult> result = waitMessage(client, heartbeat, WAITING_TIMEOUT, ServiceHandlerRoute.TO_ADMIN, dictionaryName);
            Assert.assertTrue("ComparisionResult isn't null: " + result.getSecond(), result.getSecond() == null);
            Assert.assertEquals(ServiceStatus.STARTED, client.getStatus());
            Assert.assertEquals(NTGClientState.SessionCreated, client.getState());
        } finally {
            client.dispose();
            Assert.assertEquals(ServiceStatus.DISPOSED, client.getStatus());
        }
    }

    /**
     * Test of connecting to server with doLogonOnStart=true, test logout and
     * test sending heartbeat after
     */
    @Test
    public void testLogout() throws InterruptedException {
        try {
            startServices(true, true, LOGIN_TIMEOUT);
            Assert.assertTrue(client.isConnected());
            Assert.assertEquals("LoggedIn", client.getState().toString());
            try {
                client.logout();
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
                throw e;
            }
            Assert.assertEquals(ServiceStatus.STARTED, client.getStatus());
            Assert.assertEquals(NTGClientState.LoggedOut, client.getState());
            IMessage heartbeat = sentHeartbeat(HEARTBEAT_MESSAGE_NUMBER);
            Pair<IMessage, ComparisonResult> result = waitMessage(client, heartbeat, WAITING_TIMEOUT, ServiceHandlerRoute.TO_ADMIN, dictionaryName);
            Assert.assertTrue("Heartbeat was sent after logout. ComparisionResult is null: " + result.getSecond(), result.getSecond() == null);
        } finally {
            client.dispose();
            Assert.assertEquals(ServiceStatus.DISPOSED, client.getStatus());
        }
    }

    /**
     * Test of reject sending to client
     */
    @Test
    public void testReject() throws InterruptedException {
        try {
            IMessage reject = getReject();
            startServices(true, true, LOGIN_TIMEOUT);
            Assert.assertTrue(client.isConnected());
            Assert.assertEquals(NTGClientState.LoggedIn, client.getState());
            try {
                client.logout();
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
                throw e;
            }
            Assert.assertEquals(ServiceStatus.STARTED, client.getStatus());
            Assert.assertEquals(NTGClientState.LoggedOut, client.getState());
            IMessage heartbeat = sentHeartbeat(HEARTBEAT_MESSAGE_NUMBER);
            Pair<IMessage, ComparisonResult> result = waitMessage(client, heartbeat, WAITING_TIMEOUT, ServiceHandlerRoute.TO_ADMIN, dictionaryName);
            Assert.assertTrue("Heartbeat was sent after logout. ComparisionResult isn't null: " + result.getSecond(), result.getSecond() == null);
            Pair<IMessage, ComparisonResult> pair = waitMessage(server, reject, WAITING_TIMEOUT, ServiceHandlerRoute.FROM_APP, dictionaryName, null);
            Assert.assertFalse("ComparisionResult is null", pair.getSecond() == null);
            Assert.assertEquals("StatusType is " + pair.getSecond().getStatus(), pair.getSecond().getStatus(), StatusType.PASSED);
        } finally {
            client.dispose();
            Assert.assertEquals(ServiceStatus.DISPOSED, client.getStatus());
        }
    }

    /**
     * Test timeout of waiting any message from server before sending heartbeat
     */
    @Test
    public void testHeartbeatTimeout() throws InterruptedException {
        try {
            startServices(true, true, LOGIN_TIMEOUT);
            Assert.assertEquals(NTGClientState.LoggedIn, client.getState());
            try {
                Thread.sleep(((NTGClientSettings) client.getSettings()).getHeartbeatTimeout());
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
                throw e;
            }
            Assert.assertEquals(NTGClientState.SessionClosed, client.getState());
            IMessage heartbeat = sentHeartbeat(HEARTBEAT_MESSAGE_NUMBER);
            Assert.assertEquals(NTGClientState.SessionClosed, client.getState());
            Pair<IMessage, ComparisonResult> result = waitMessage(client, heartbeat, WAITING_TIMEOUT, ServiceHandlerRoute.TO_ADMIN, dictionaryName);
            Assert.assertTrue("Heartbeat was sent after timeout. ComparisionResult isn't null: " + result.getSecond(), result.getSecond() == null);
        } finally {
            client.dispose();
            Assert.assertEquals(ServiceStatus.DISPOSED, client.getStatus());
        }
    }

    /**
     * Test maximum of missed heartbeat before disconnect from server
     */
    @Test
    public void testMaxMissedHeartbeat() throws InterruptedException {
        try {
            startServices(true, true, WAITING_TIMEOUT);
            Thread.sleep(((NTGClientSettings) client.getSettings()).getHeartbeatTimeout());

            long timeEnd = System.currentTimeMillis() + WAITING_TIMEOUT;
            while (timeEnd > System.currentTimeMillis() && !NTGClientState.SessionClosed.equals(client.getState())) {
                Thread.sleep(LOOP_TIMEOUT);
            }

            List<IMessage> messages = handlerClient.getMessages(client.getSession(), ServiceHandlerRoute.TO_ADMIN, null);
            int heartbeats = 0;

            for(IMessage message : messages) {
                if ("Heartbeat".equals(message.getName())) {
                    heartbeats++;
                }
            }

            Assert.assertEquals(NTGClientState.SessionClosed, client.getState());
            Assert.assertFalse("Client still connected", client.isConnected());
            Assert.assertEquals(0, heartbeats);
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
            throw e;
        } finally {
            client.dispose();
            Assert.assertEquals(ServiceStatus.DISPOSED, client.getStatus());
        }
    }

}
