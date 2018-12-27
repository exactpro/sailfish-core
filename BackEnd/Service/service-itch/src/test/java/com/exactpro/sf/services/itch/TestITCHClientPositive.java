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

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.util.Pair;
import com.exactpro.sf.comparison.ComparisonResult;
import com.exactpro.sf.scriptrunner.Outcome;
import com.exactpro.sf.scriptrunner.StatusType;
import com.exactpro.sf.services.ServiceHandlerRoute;
import com.exactpro.sf.services.ServiceStatus;
import com.exactpro.sf.util.TestITCHClientBase;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author sergey.vasiliev
 *
 */
public class TestITCHClientPositive extends TestITCHClientBase {

    private static final Logger logger = LoggerFactory.getLogger(TestITCHClientPositive.class);
    private static final int LOGIN_TIMEOUT = 1000;

    private static final int RECONNECTING_TIMEOUT = 600;
    private static final int TIMEOUT_HEARTBEAT_MESSAGE_NUMBER = 2;
    private static final int LOOP_TIMEOUT = 50;

    private static final int DISCONNECT_WAITING_TIMEOUT = 5000;
    private static final int WAITING_TIMEOUT = 500;

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

    @Override
    @After
    public void cleanServersCollection() {
        server.getServiceHandler().cleanMessages(ServiceHandlerRoute.values());
    }



    @Test
    public void testConnectionWithLogin() throws Exception {
        logger.info("Start testConnectionWithLogin()");
        try {
            startServices(false, true, true, false, LOGIN_TIMEOUT);
            IMessage loginRequest = getLoginRequest();
            Assert.assertTrue(client.isConnected());
            Assert.assertEquals(ServiceStatus.STARTED, client.getStatus());
            IMessage list = getMessageList(getUnitHeader((int)loginRequest.getField("Length"), 1), loginRequest);
            Pair<IMessage, ComparisonResult> result = waitMessage(server, list, WAITING_TIMEOUT, ServiceHandlerRoute.FROM_APP,
                    dictionaryName);
            Assert.assertFalse("ComparisionResult is null", result.getSecond() == null);
            Assert.assertEquals("StatusType is " + result.getSecond().getStatus(), StatusType.PASSED, result.getSecond().getStatus());

            list = getMessageList(getLogonResponse());
            result = waitMessage(server, list, WAITING_TIMEOUT, ServiceHandlerRoute.TO_APP, dictionaryName);
            Assert.assertFalse("ComparisionResult is null", result.getSecond() == null);
            Assert.assertEquals("StatusType is " + result.getSecond().getStatus(), StatusType.PASSED, result.getSecond().getStatus());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        } finally {
            if (client != null) {
                client.dispose();
                long time = DISCONNECT_WAITING_TIMEOUT + System.currentTimeMillis();
                while (System.currentTimeMillis() < time && !ServiceStatus.DISPOSED.equals(client.getStatus())) {
                    Thread.sleep(LOOP_TIMEOUT);
                }
                Assert.assertEquals(ServiceStatus.DISPOSED, client.getStatus());
            }
        }
    }



    @Test
    public void testHeartbeatTimeout() throws InterruptedException {
        logger.info("Start testHeartbeatTimeout()");
        try {
            int sessionTimeoutInSec = 1;
            startServices(false, false, false, false, sessionTimeoutInSec);
            try {
                // WAITING_TIMEOUT is added here to take into account overhead of heartbeat
                // sending mechanism which relies on MINA's sessionIdle hook
                Thread.sleep(TIMEOUT_HEARTBEAT_MESSAGE_NUMBER * sessionTimeoutInSec * 1000 + WAITING_TIMEOUT);
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
                throw e;
            }
            Outcome outcome = new Outcome(null, null);
            IMessage unitHeader = getUnitHeader(0, 0);
            int uhServerNumber = waitCountMessages(server, getMessageList(unitHeader), WAITING_TIMEOUT, ServiceHandlerRoute.FROM_APP, dictionaryName,
                    TIMEOUT_HEARTBEAT_MESSAGE_NUMBER,
                    outcome);
            Assert.assertEquals("Wrong number of UnitHeader (in server's collection) was sent", TIMEOUT_HEARTBEAT_MESSAGE_NUMBER,
                    uhServerNumber);
        } finally {
            if (client != null) {
                client.dispose();
                long time = DISCONNECT_WAITING_TIMEOUT + System.currentTimeMillis();
                while (System.currentTimeMillis() < time && !ServiceStatus.DISPOSED.equals(client.getStatus())) {
                    Thread.sleep(LOOP_TIMEOUT);
                }
                Assert.assertEquals(ServiceStatus.DISPOSED, client.getStatus());
            }
        }
    }



    @Test
    public void testConnectChannel() throws Exception {
        logger.info("Start testConnectChannel()");
        try{
            startServices(false, false, true, false, LOGIN_TIMEOUT);
            IMessage loginRequest = getLoginRequest();
            Assert.assertTrue("Client isn't connected",client.isConnected());
            Assert.assertEquals(ServiceStatus.STARTED, client.getStatus());
            try {
                client.disconnect();
            } catch (Exception e) {
                logger.error(e.getMessage(),e);
                throw e;
            }
            long time = DISCONNECT_WAITING_TIMEOUT + System.currentTimeMillis();
            while (System.currentTimeMillis() < time && client.isConnected()) {
                Thread.sleep(LOOP_TIMEOUT);
            }

            Assert.assertFalse("Client is connected after disconnect()",client.isConnected());
            Thread.sleep(LOOP_TIMEOUT);
            client.connect(DISCONNECT_WAITING_TIMEOUT);
            time = DISCONNECT_WAITING_TIMEOUT + System.currentTimeMillis();
            while (System.currentTimeMillis() < time && !client.isConnected()) {
                Thread.sleep(LOOP_TIMEOUT);
            }
            Assert.assertTrue("Client isn't connected after connectChannel()", client.isConnected());
            Assert.assertEquals(ServiceStatus.STARTED, client.getStatus());
            client.sendLogin();
            IMessage list = getMessageList(getUnitHeader((int) loginRequest.getField("Length"), 1), loginRequest);
            Pair<IMessage, ComparisonResult> result = waitMessage(server, list, WAITING_TIMEOUT, ServiceHandlerRoute.FROM_APP, dictionaryName);
            Assert.assertFalse("ComparisionResult is null", result.getSecond() == null);
            Assert.assertEquals("StatusType is " + result.getSecond().getStatus(), StatusType.PASSED, result.getSecond().getStatus());

            list = getMessageList(getLogonResponse());
            result = waitMessage(server, list, WAITING_TIMEOUT, ServiceHandlerRoute.TO_APP, dictionaryName);
            Assert.assertFalse("ComparisionResult is null", result.getSecond() == null);
            Assert.assertEquals("StatusType is " + result.getSecond().getStatus(), StatusType.PASSED, result.getSecond().getStatus());
        }catch(Exception e){
            logger.error(e.getMessage(),e);
            throw e;
        }  finally {
            if (client != null) {
                client.dispose();
                long time = DISCONNECT_WAITING_TIMEOUT + System.currentTimeMillis();
                while (System.currentTimeMillis() < time && !ServiceStatus.DISPOSED.equals(client.getStatus())) {
                    Thread.sleep(LOOP_TIMEOUT);
                }
                Assert.assertEquals(ServiceStatus.DISPOSED, client.getStatus());
            }
        }
    }


    @Test
    public void testReconnectionSetting() throws Exception{
        logger.info("Start testReconnectionSetting()");
        try{
            initClient(false, true, true, LOGIN_TIMEOUT, RECONNECTING_TIMEOUT - 100);
            client.start();
            Assert.assertTrue("Client isn't connected",client.isConnected());
            Assert.assertEquals(ServiceStatus.STARTED, client.getStatus());
            Assert.assertTrue("Mina session is null",server.getSessionMap().values().iterator().hasNext());
            try {
                client.disconnected0();
            } catch (Exception e) {
                logger.error(e.getMessage(),e);
                throw e;
            }
            long time = DISCONNECT_WAITING_TIMEOUT + RECONNECTING_TIMEOUT + System.currentTimeMillis();
            while(System.currentTimeMillis() < time
                    && (!ServiceStatus.STARTED.equals(client.getStatus()) || !client.isConnected())) {
                Thread.sleep(LOOP_TIMEOUT);
            }
            Assert.assertTrue("Client isn't connected",client.isConnected());
            Assert.assertEquals(ServiceStatus.STARTED, client.getStatus());
            Assert.assertTrue("Mina session is null",server.getSessionMap().values().iterator().hasNext());
        }catch(Exception e){
            logger.error(e.getMessage(),e);
            throw e;
        }finally {
            if (client != null) {
                client.dispose();
                long time = DISCONNECT_WAITING_TIMEOUT + System.currentTimeMillis();
                while (System.currentTimeMillis() < time && !ServiceStatus.DISPOSED.equals(client.getStatus())) {
                    Thread.sleep(LOOP_TIMEOUT);
                }
                Assert.assertEquals(ServiceStatus.DISPOSED, client.getStatus());
            }
        }
    }



    @Test
    public void testReconnectingTimeout() throws Exception{
        logger.info("Start testReconnectingTimeout()");
        Thread waitThread = null;
        try{
            server.dispose();
            initClient(false, false, true, LOGIN_TIMEOUT, 500);
            ServerThread wait = new ServerThread(RECONNECTING_TIMEOUT - 300);
            waitThread = new Thread(wait);
            waitThread.start();
            client.start();
            long time = RECONNECTING_TIMEOUT + System.currentTimeMillis();
            while(System.currentTimeMillis() < time 
                    && (!ServiceStatus.STARTED.equals(client.getStatus()) || !client.isConnected())) {
                Thread.sleep(LOOP_TIMEOUT);
            }
            Assert.assertTrue("Client isn't connected",client.isConnected());
            Assert.assertEquals(ServiceStatus.STARTED, client.getStatus());
        }catch(Exception e){
            logger.error(e.getMessage(),e);
            throw e;
        }finally {
            if (client != null) {
                client.dispose();
                long time = DISCONNECT_WAITING_TIMEOUT + System.currentTimeMillis();
                while (System.currentTimeMillis() < time && !ServiceStatus.DISPOSED.equals(client.getStatus())) {
                    Thread.sleep(LOOP_TIMEOUT);
                }
                Assert.assertEquals(ServiceStatus.DISPOSED, client.getStatus());
            }
            if (waitThread != null)
                waitThread.interrupt();
        }
    }



    @Test
    public void testConnectionDecompressionUser() throws Exception {
        logger.info("Start testConnectionDecompressionUser()");
        try {
            initClient(false, true, false, LOGIN_TIMEOUT, 5000);
            client.getSettings().setCompressionUsed(true);
            client.start();
            IMessage loginRequest = getLoginRequest();

            Assert.assertTrue(client.isConnected());
            Assert.assertEquals(ServiceStatus.STARTED, client.getStatus());
            IMessage list = getMessageList(getUnitHeader((int) loginRequest.getField("Length"), 1), loginRequest);
            Pair<IMessage, ComparisonResult> result = waitMessage(server, list, WAITING_TIMEOUT, ServiceHandlerRoute.FROM_APP, dictionaryName);
            Assert.assertFalse("ComparisionResult is null", result.getSecond() == null);
            Assert.assertEquals("StatusType is " + result.getSecond().getStatus(), StatusType.PASSED, result.getSecond().getStatus());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        } finally {
            if (client != null) {
                client.dispose();
                long time = DISCONNECT_WAITING_TIMEOUT + System.currentTimeMillis();
                while (System.currentTimeMillis() < time && !ServiceStatus.DISPOSED.equals(client.getStatus())) {
                    Thread.sleep(LOOP_TIMEOUT);
                }
                Assert.assertEquals(ServiceStatus.DISPOSED, client.getStatus());
            }
        }
    }



    @Test
    public void testConnectionWithoutLogin() throws InterruptedException {
        logger.info("Start testConnectionWithoutLogin()");
        try {
            startServices(false, false, false,false, LOGIN_TIMEOUT);
            IMessage loginRequest = getLoginRequest();
            client.sendLogin();
            Assert.assertTrue(client.isConnected());
            Assert.assertEquals(ServiceStatus.STARTED, client.getStatus());
            IMessage list = getMessageList(getUnitHeader((int) loginRequest.getField("Length"), 1), loginRequest);
            Pair<IMessage, ComparisonResult> result = waitMessage(server, list, WAITING_TIMEOUT, ServiceHandlerRoute.FROM_APP, dictionaryName);
            Assert.assertFalse("ComparisionResult is null", result.getSecond() == null);
            Assert.assertEquals("StatusType is " + result.getSecond().getStatus(), StatusType.PASSED, result.getSecond().getStatus());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        } finally {
            if (client != null) {
                client.dispose();
                long time = DISCONNECT_WAITING_TIMEOUT + System.currentTimeMillis();
                while (System.currentTimeMillis() < time && !ServiceStatus.DISPOSED.equals(client.getStatus())) {
                    Thread.sleep(LOOP_TIMEOUT);
                }
                Assert.assertEquals(ServiceStatus.DISPOSED, client.getStatus());
            }
        }
    }



    @Test
    public void testConnectionWithLiteLogin() throws InterruptedException {
        logger.info("Start testConnectionWithLiteLogin()");
        try {
            startServices(true, false, false,false, LOGIN_TIMEOUT);
            IMessage loginRequest = getLiteLoginRequest();
            Assert.assertTrue(client.isConnected());
            Assert.assertEquals(ServiceStatus.STARTED, client.getStatus());

            IMessage list = getMessageList(getUnitHeader((int) loginRequest.getField("Length"), 1), loginRequest);
            Pair<IMessage, ComparisonResult> result = waitMessage(server, list, WAITING_TIMEOUT, ServiceHandlerRoute.FROM_APP, dictionaryName);
            Assert.assertFalse("ComparisionResult is null", result.getSecond() == null);
            Assert.assertEquals("StatusType is " + result.getSecond().getStatus(), StatusType.PASSED, result.getSecond().getStatus());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        } finally {
            if (client != null) {
                client.dispose();
                long time = DISCONNECT_WAITING_TIMEOUT + System.currentTimeMillis();
                while (System.currentTimeMillis() < time && !ServiceStatus.DISPOSED.equals(client.getStatus())) {
                    Thread.sleep(LOOP_TIMEOUT);
                }
                Assert.assertEquals(ServiceStatus.DISPOSED, client.getStatus());
            }
        }
    }


    @Test
    public void testAddOrder() throws InterruptedException {
        logger.info("Start testAddOrder()");
        try {
            startServices(false, true, false,false, LOGIN_TIMEOUT);
            Assert.assertTrue(client.isConnected());
            Assert.assertEquals(ServiceStatus.STARTED, client.getStatus());
            IMessage loginRequest = getLoginRequest();
            Assert.assertTrue(client.isConnected());
            Assert.assertEquals(ServiceStatus.STARTED, client.getStatus());
            IMessage list = getMessageList(getUnitHeader((int) loginRequest.getField("Length"), 1), loginRequest);
            Pair<IMessage, ComparisonResult> result = waitMessage(server, list, WAITING_TIMEOUT, ServiceHandlerRoute.FROM_APP, dictionaryName);
            Assert.assertFalse("ComparisionResult is null", result.getSecond() == null);
            Assert.assertEquals("StatusType is " + result.getSecond().getStatus(), StatusType.PASSED, result.getSecond().getStatus());

            server.getServiceHandler().cleanMessages(ServiceHandlerRoute.FROM_APP, ServiceHandlerRoute.TO_APP);
            Assert.assertTrue(client.isConnected());
            sentAddOrder(true);
            list = getMessageList(getAddOrder());

            result = waitMessage(server, list, WAITING_TIMEOUT, ServiceHandlerRoute.FROM_APP, dictionaryName);
            Assert.assertFalse("ComparisionResult is null", result.getSecond() == null);
            Assert.assertEquals("StatusType is " + result.getSecond().getStatus(), StatusType.PASSED, result.getSecond().getStatus());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        } finally {
            if (client != null) {
                client.dispose();
                long time = DISCONNECT_WAITING_TIMEOUT + System.currentTimeMillis();
                while (System.currentTimeMillis() < time && !ServiceStatus.DISPOSED.equals(client.getStatus())) {
                    Thread.sleep(LOOP_TIMEOUT);
                }
                Assert.assertEquals(ServiceStatus.DISPOSED, client.getStatus());
            }
        }
    }
}
