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
package com.exactpro.sf.util;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.services.CollectorServiceHandler;
import com.exactpro.sf.services.IServiceSettings;
import com.exactpro.sf.services.ServiceStatus;
import com.exactpro.sf.services.itch.ITCHCodec;
import com.exactpro.sf.services.itch.ITCHMessageHelper;
import com.exactpro.sf.services.itch.ITCHTCPClientSettings;
import com.exactpro.sf.services.itch.ITCHTcpClient;
import com.exactpro.sf.services.tcpip.DefaultFieldConverter;
import com.exactpro.sf.services.tcpip.TCPIPServerSettings;
import junit.framework.Assert;
import org.apache.mina.core.session.IoSession;
import org.junit.After;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * @author sergey.vasiliev
 *
 */
public abstract class TestITCHClientBase extends TestClientBase {

    protected static ITCHTcpClient client;
    protected static String namespace = "ITCH_CLIENT";

    protected static int maxHeartbeats = 1;
    protected static int port = 9801;
    protected static SailfishURI dictionaryName = SailfishURI.unsafeParse("ITCH_CLIENT");
    protected Thread waitThread;

    private static final Logger logger = LoggerFactory.getLogger(TestITCHClientBase.class);

    @After
    public void cleanUp() {
        if (waitThread != null && !waitThread.isInterrupted()) {
            waitThread.interrupt();
        }
        logger.info("WaitThread interrupted");
    }

    protected void startServices(boolean doLiteLoginOnStart, boolean doLoginOnStart, boolean doReplyLogin, boolean reconnect, int timeout) {
        initClient(doLiteLoginOnStart, doLoginOnStart, reconnect, timeout, 5000);
        client.start();
        waitThread = null;
        if (doReplyLogin) {
            ITCHWaitResponse wait = new ITCHWaitResponse(10000, getMessageList(getLogonResponse()), "LoginRequest", server);
            waitThread = new Thread(wait);
            waitThread.start();
        }
        if (doReplyLogin) {
            Assert.assertEquals(ServiceStatus.STARTED, client.getStatus());
        }
    }

    protected static void initClient(boolean doLiteLoginOnStart, boolean doLoginOnStart, boolean reconnect, int timeout, int timeoutReconnect) {
        ITCHTCPClientSettings settingsClient = new ITCHTCPClientSettings();
        settingsClient.setDictionaryName(dictionaryName);
        settingsClient.setAddress(host);
        settingsClient.setPort(port);
        settingsClient.setIdleTimeout(1);
        settingsClient.setDoLoginOnStart(doLoginOnStart);
        settingsClient.setHeartbeatTimeout(timeout);
        settingsClient.setUsername("Test");
        settingsClient.setSendHeartBeats(true);
        settingsClient.setDoLiteLoginOnStart(doLiteLoginOnStart);
        settingsClient.setDisposeWhenSessionClosed(true);
        settingsClient.setReconnecting(reconnect);
        settingsClient.setReconnectingTimeout(timeoutReconnect);

        client = new ITCHTcpClient();
        handlerClient = new CollectorServiceHandler();
        client.init(serviceContext, mockedMonitor, handlerClient, settingsClient, serviceName);
        Assert.assertEquals(ServiceStatus.INITIALIZED, client.getStatus());
    }

    protected static IServiceSettings getServerSettings() {
        dictionaryManager = serviceContext.getDictionaryManager();
        TCPIPServerSettings settings = new TCPIPServerSettingsForITCH();
        settings.setDictionaryName(dictionaryName);
        settings.setCodecClassName(ITCHCodec.class.getCanonicalName());
        settings.setFieldConverterClassName(DefaultFieldConverter.class.getCanonicalName());
        settings.setPort(port);
        settings.setHost(host);
        settings.setDecodeByDictionary(true);
        return settings;
    }

    protected void sentHeartbeat(int messageNumber) {
        IMessage unitHeader = getUnitHeader(0, 0);
        for (int loop = 0; loop < messageNumber; loop++) {
            for (IoSession session : server.getSessionMap().keySet()) {
                session.write(unitHeader);
            }
        }
    }

    protected void sentAddOrder(boolean clientSending) throws InterruptedException {
        IMessage messageAddOrder = getAddOrder();
        IMessage unitHeader = getUnitHeader(0, 1);
        unitHeader.addField("SequenceNumber", (long) 2);
        IMessage result = getMessageList(unitHeader, messageAddOrder);
        try {
            if (clientSending) {
                client.sendMessage(messageAddOrder);
            } else {
                for (IoSession session : server.getSessionMap().keySet()) {
                    session.write(result);
                }
            }
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    protected IMessage getAddOrder() {
        IMessageFactory messageFactory = dictionaryManager.createMessageFactory();
        IMessage messageAddOrder = messageFactory.createMessage("AddOrder", namespace);
        messageAddOrder.addField("Length", 3);
        messageAddOrder.addField(ITCHMessageHelper.FAKE_FIELD_UH_MARKET_DATA_GROUP, (short) 0);
        messageAddOrder.addField(ITCHMessageHelper.FAKE_FIELD_UH_SEQUENCE_NUMBER, 2);
        messageAddOrder.addField(ITCHMessageHelper.FAKE_FIELD_MESSAGE_SEQUENCE_NUMBER, 2);
        messageAddOrder.addField("MessageType", (short) 65);
        return messageAddOrder;
    }

    protected static IMessage getLogonResponse() {
        IMessageFactory messageFactory = dictionaryManager.createMessageFactory();
        IMessage messageReply = messageFactory.createMessage("LoginResponse", namespace);
        messageReply.addField("Status", (short)0);
        messageReply.addField("Length", 4);
        messageReply.addField("MessageType",(short)2);
        messageReply.addField(ITCHMessageHelper.FAKE_FIELD_UH_MARKET_DATA_GROUP, (short) 0);
        messageReply.addField(ITCHMessageHelper.FAKE_FIELD_UH_SEQUENCE_NUMBER, 55);
        messageReply.addField(ITCHMessageHelper.FAKE_FIELD_MESSAGE_SEQUENCE_NUMBER, 55);
        return messageReply;
    }

    protected static IMessage getLoginRequest() {
        IMessageFactory messageFactory = dictionaryManager.createMessageFactory();
        IMessage message = messageFactory.createMessage("LoginRequest", namespace);
        message.addField("Length", 11);
        message.addField("MessageType", (short) 1);
        message.addField("Username", client.getSettings().getUsername());
        return message;
    }

    protected static IMessage getLiteLoginRequest() {
        IMessageFactory messageFactory = dictionaryManager.createMessageFactory();
        IMessage message = messageFactory.createMessage("LoginRequestLite", namespace);
        message.addField("Length", 13);
        message.addField("MessageType", (short) 5);
        message.addField("Username", client.getSettings().getUsername());
        return message;
    }

    protected static IMessage getMessageList(IMessage... messages) {
        TestITCHHelper helper = new TestITCHHelper();
        List<IMessage> list = new LinkedList<>();
        list.addAll(new LinkedList<>(Arrays.asList(messages)));
        return helper.getMessageCreator().getMessageList(list);
    }

    protected IMessage getUnitHeader(int messageLength, int messageCount) {
        IMessageFactory messageFactory = dictionaryManager.createMessageFactory();
        IMessage unitHeader = messageFactory.createMessage("UnitHeader", namespace);
        unitHeader.addField("Length", 8 + messageLength);
        unitHeader.addField("MessageCount", (short) messageCount);
        unitHeader.addField("MarketDataGroup", (short) 0);
        return unitHeader;
    }

    public static class ServerThread implements Runnable {

        private final int timeout;

        public ServerThread(int timeout) {
            this.timeout=timeout;
        }

        @Override
        public void run() {
            try{
                Thread.sleep(timeout);
                server.start();
                Assert.assertEquals(ServiceStatus.STARTED, server.getStatus());
            }catch(Exception e){
                logger.error(e.getMessage(), e);
            }
        }

    }

}
