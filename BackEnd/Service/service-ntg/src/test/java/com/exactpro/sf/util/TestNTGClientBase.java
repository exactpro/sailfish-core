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

import org.apache.mina.core.session.IoSession;
import org.junit.After;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.impl.messages.DefaultMessageFactory;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.services.CollectorServiceHandler;
import com.exactpro.sf.services.IServiceSettings;
import com.exactpro.sf.services.ServiceStatus;
import com.exactpro.sf.services.ntg.NTGClient;
import com.exactpro.sf.services.ntg.NTGClientSettings;
import com.exactpro.sf.services.ntg.NTGCodec;
import com.exactpro.sf.services.tcpip.DefaultFieldConverter;
import com.exactpro.sf.services.tcpip.TCPIPServerSettings;

import junit.framework.Assert;

public abstract class TestNTGClientBase extends TestClientBase {

    protected static NTGClient client;
    protected static String namespace = "NTG";

    protected static int maxHeartbeats = 1;
    protected static int port = 9882;
    protected static SailfishURI dictionaryName = SailfishURI.unsafeParse("NTG");

    private static final Logger logger = LoggerFactory.getLogger(TestNTGClientBase.class);

    protected static int incorrectPort = 9888;

    protected Thread waitThread;

    @After
    public void stopWaitThread() {
        if (waitThread != null && !waitThread.isInterrupted()) {
            waitThread.interrupt();
        }
    }

    protected void startServices(boolean doLoginOnStart, boolean doReplyLogin, int timeout) {
        NTGClientSettings settingsClient = new NTGClientSettings();
        settingsClient.setDictionaryName(dictionaryName);
        settingsClient.setServerIP(host);
        settingsClient.setServerPort(port);
        settingsClient.setLogin("Test");
        settingsClient.setPassword("Test");
        settingsClient.setLoginTimeout(timeout);
        settingsClient.setLogoutTimeout(timeout);
        settingsClient.setDoLoginOnStart(doLoginOnStart);
        settingsClient.setHeartbeatTimeout(2 * timeout);
        settingsClient.setMaxMissedHeartbeats(maxHeartbeats);
        client = new NTGClient();
        handlerClient = new CollectorServiceHandler();
        client.init(serviceContext, mockedMonitor, handlerClient, settingsClient, serviceName);
        Assert.assertEquals(ServiceStatus.INITIALIZED, client.getStatus());
        if (doReplyLogin) {
            WaitLogonResponse wait = new WaitLogonResponse(timeout, getLogonReply(), "Logon", server);
            waitThread = new Thread(wait);
            waitThread.start();
        }
        client.start();
        if (doReplyLogin) {
            Assert.assertEquals(ServiceStatus.STARTED, client.getStatus());
        }
    }


    protected IMessage sentHeartbeat(int messageNumber) {
        IMessageFactory messageFactory = DefaultMessageFactory.getFactory();
        IMessage messageHearbeat = messageFactory.createMessage("Heartbeat", namespace);
        IMessage messageHeader = messageFactory.createMessage("MessageHeader", namespace);
        messageHeader.addField("StartOfMessage", 2);
        messageHeader.addField("MessageLength", 1);
        messageHeader.addField("MessageType", "0");
        messageHearbeat.addField("MessageHeader", messageHeader);
        for (int loop = 0; loop < messageNumber; loop++) {
            for (IoSession session : server.getSessionMap().keySet()) {
                session.write(messageHearbeat);
            }
        }
        return messageHearbeat;
    }

    protected void sentReject(int rejectCode, String rejectReason) throws InterruptedException {
        IMessageFactory messageFactory = dictionaryManager.createMessageFactory();
        IMessage messageReject = messageFactory.createMessage("Reject", namespace);
        IMessage messageHeader = messageFactory.createMessage("MessageHeader", namespace);
        messageHeader.addField("StartOfMessage", 2);
        messageHeader.addField("MessageLength", 35);
        messageHeader.addField("MessageType", "3");
        messageReject.addField("MessageHeader", messageHeader);
        messageReject.addField("RejectReason", rejectReason);
        messageReject.addField("RejectCode", rejectCode);
        messageReject.addField("RejectedMessageType", "0");
        try {
            client.sendMessage(messageReject);
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    protected IMessage sentNewOrder(boolean clientSending) throws InterruptedException {
        IMessageFactory messageFactory = dictionaryManager.createMessageFactory();
        IMessage messageNewOrder = messageFactory.createMessage("NewOrder", namespace);
        IMessage messageHeader = messageFactory.createMessage("MessageHeader", namespace);
        messageHeader.addField("StartOfMessage", 2);
        messageHeader.addField("MessageLength", 5);
        messageHeader.addField("MessageType", "D");
        messageNewOrder.addField("MessageHeader", messageHeader);
        messageNewOrder.addField("TraderID", "test");
        try {
            if (clientSending) {
                client.sendMessage(messageNewOrder);
            } else {
                for (IoSession session : server.getSessionMap().keySet()) {
                    session.write(messageNewOrder);
                }
            }
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
        return messageNewOrder;
    }

    protected static IServiceSettings getServerSettings() {
        TCPIPServerSettings settings = new TCPIPServerSettings();
        settings.setDictionaryName(dictionaryName);
        settings.setCodecClassName(NTGCodec.class.getCanonicalName());
        settings.setFieldConverterClassName(DefaultFieldConverter.class.getCanonicalName());
        settings.setPort(port);
        settings.setHost(host);
        settings.setDecodeByDictionary(true);
        return settings;
    }

    protected static IMessage getReject() {
        IMessageFactory messageFactory = dictionaryManager.createMessageFactory();
        IMessage message = messageFactory.createMessage("Reject", namespace);
        message.addField("RejectCode", 4);
        message.addField("RejectReason", "NotLoggedIn");
        return message;
    }

    protected static IMessage getLogonReply() {
        IMessageFactory messageFactory = dictionaryManager.createMessageFactory();
        IMessage messageReply = messageFactory.createMessage("LogonReply", namespace);
        IMessage messageHeader = messageFactory.createMessage("MessageHeader", namespace);
        messageHeader.addField("StartOfMessage", 2);
        messageHeader.addField("MessageLength", 35);
        messageHeader.addField("MessageType", "B");
        messageReply.addField("MessageHeader", messageHeader);
        messageReply.addField("RejectCode", 0);
        return messageReply;
    }
}
