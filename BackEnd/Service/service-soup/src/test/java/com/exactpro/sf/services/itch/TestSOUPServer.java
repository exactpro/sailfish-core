/******************************************************************************
 * Copyright 2009-2021 Exactpro (Exactpro Systems Limited)
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

import static com.exactpro.sf.services.itch.soup.SOUPTcpMessageHelper.MESSAGE_LOGIN_REQUEST_PACKET;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.aml.script.ActionContext;
import com.exactpro.sf.aml.script.actions.WaitAction;
import com.exactpro.sf.center.ISFContext;
import com.exactpro.sf.center.impl.SFLocalContext;
import com.exactpro.sf.common.impl.messages.DefaultMessageFactory;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.common.util.Pair;
import com.exactpro.sf.comparison.ComparatorSettings;
import com.exactpro.sf.comparison.ComparisonResult;
import com.exactpro.sf.comparison.conversion.MultiConverter;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.scriptrunner.DebugController;
import com.exactpro.sf.scriptrunner.EnvironmentSettings.RelevantMessagesSortingMode;
import com.exactpro.sf.scriptrunner.IScriptConfig;
import com.exactpro.sf.scriptrunner.IScriptProgress;
import com.exactpro.sf.scriptrunner.IScriptReport;
import com.exactpro.sf.scriptrunner.ScriptContext;
import com.exactpro.sf.scriptrunner.ScriptSettings;
import com.exactpro.sf.scriptrunner.impl.DefaultScriptConfig;
import com.exactpro.sf.scriptrunner.impl.htmlreport.HtmlReport;
import com.exactpro.sf.services.CollectorServiceHandler;
import com.exactpro.sf.services.IServiceMonitor;
import com.exactpro.sf.services.MessageHelper;
import com.exactpro.sf.services.ServiceHandlerRoute;
import com.exactpro.sf.services.ServiceStatus;
import com.exactpro.sf.services.itch.soup.SOUPTcpClient;
import com.exactpro.sf.services.itch.soup.SOUPTcpClientSettings;
import com.exactpro.sf.services.itch.soup.SOUPTcpMessageHelper;
import com.exactpro.sf.services.itch.soup.SOUPTcpServer;
import com.exactpro.sf.services.itch.soup.SOUPTcpServerSettings;
import com.exactpro.sf.util.AbstractTest;

public class TestSOUPServer extends AbstractTest {
    private static final Logger logger = LoggerFactory.getLogger(TestSOUPServer.class);


    private static final int                CLIENT_HEARTBEATS_TIMEOUT_IN_SECONDS = 1;
    private static final int                SERVER_READ_HEARTBEATS_TIMEOUT_IN_SECONDS = CLIENT_HEARTBEATS_TIMEOUT_IN_SECONDS * 5;
    private static final int                SERVICE_SEND_HEARTBEATS_TIMEOUT_IN_SECONDS = 1;

    private static final int                WAIT_LOGIN_TIMEOUT_IN_MILLISECONDS = 1000;

    private static final boolean            SERVER_SEND_HEARTBEATS = true;
    private static final boolean            CLIENT_SEND_HEARTBEATS = true;

    private static final String             SERVER_ADDRESS = "localhost";
    private static final int                START_SERVER_PORT = 9880;

    private static final String             USERNAME = "Test";
    private static final String             PASSWORD = "Test";

    private static final SailfishURI        DICTIONARY_SURI = SailfishURI.unsafeParse("OUCH_TEST");

    private static final int                ADD_FOR_THREAD_SLEEP_ON_WAIT_IN_MILLISECONDS = 1000;

    public static final int                 WAITING_START_MILLISECONDS = 50;

    private static AtomicInteger NEXT_SERVER_PORT = new AtomicInteger(START_SERVER_PORT);
    private int CURRENT_SERVER_PORT = NEXT_SERVER_PORT.getAndIncrement();

    private final MessageHelper messageHelper = new SOUPTcpMessageHelper();
    private SOUPTcpServer server;
    private SOUPTcpClient client;
    private IMessageFactory messageFactory;
    private IServiceMonitor serviceMonitor;
    private IDictionaryStructure dictionary;

    private ActionContext actionContext;
    private Thread th;

    @Before
    public void setUp() throws Exception {


        init();

        ScriptContext scriptContext = createScriptContext();

        ServiceName clientServiceName = new ServiceName("SOUPTestClient");
        ServiceName serverServiceName = new ServiceName("SOUPTestServer");

        actionContext = createActionContext(scriptContext, clientServiceName.getServiceName(), 1000);

        initAndStartServer(serverServiceName);
        initAndStartClient(clientServiceName);
    }

    @After
    public void tearDown() {
        try {
            if (client != null) {
                if (client.isConnected()) {
                    client.dispose();
                }
                client = null;
            }
        } catch (Exception e) {
            logger.error("", e);
        }

        try {
            if (server != null) {
                server.dispose();
                server = null;
            }
        } catch (Exception e) {
            logger.error("", e);
        }
    }

    @Test
    public void testCustomLogin() throws InterruptedException {

        final IMessage[] message = new IMessage[1];

        IMessage login = createMessage(MESSAGE_LOGIN_REQUEST_PACKET,
                createMap(
                        "Username", USERNAME,
                        "Password", PASSWORD,
                        "RequestedSession", RandomStringUtils.randomAlphanumeric(10),
                        "RequestedSequenceNumber", 0
                )
        );

        th = new Thread(() -> {
            IMessage filter = createMessage("LoginAcceptedPacket", createMap("Session", login.getField("RequestedSession")));
            List<Pair<IMessage, ComparisonResult>> messages = waitMessages(client, ServiceHandlerRoute.FROM_ADMIN, filter, 1000);
            synchronized (message) {
                message[0] = messages.isEmpty() ? null : messages.get(0).getFirst();
            }
        }, "Waiter");

        th.start();

        assertTrue(client.sendMessage(login));
        th.join(WAIT_LOGIN_TIMEOUT_IN_MILLISECONDS + ADD_FOR_THREAD_SLEEP_ON_WAIT_IN_MILLISECONDS);
        synchronized (message) {
            assertNotNull(message[0]);
            assertTrue(login.getField("RequestedSession").equals(message[0].getField("Session")));
            assertTrue(client.isConnected());
        }
    }

    @Test
    public void testHeartBeats() throws InterruptedException {
        sendLogin(USERNAME, PASSWORD);
        Thread.sleep(SERVER_READ_HEARTBEATS_TIMEOUT_IN_SECONDS * 2 * 1000);
        assertEquals(SERVER_SEND_HEARTBEATS && CLIENT_SEND_HEARTBEATS, client.isConnected());
    }

    @Test
    public void testWrongUserName() throws InterruptedException {
        sendLogin(SOUPTcpServer.WRONG_USERNAME, PASSWORD);
        List<?> result = waitMessages(client, ServiceHandlerRoute.FROM_ADMIN, createMessage("LoginRejectPacket", createMap("RejectReasonCode", "A")), 1000);
        assertEquals(1, result.size());
        assertTrue(client.isConnected());
    }

    @Test
    public void testLogoutMessage() throws InterruptedException {
        client.sendMessage(createMessage("LogoutRequestPacket", Collections.emptyMap()));
        Thread.sleep(100);
        assertFalse(client.isConnected());
    }

    @Test
    public void testNotLogin() throws InterruptedException {
        Thread.sleep(WAIT_LOGIN_TIMEOUT_IN_MILLISECONDS + ADD_FOR_THREAD_SLEEP_ON_WAIT_IN_MILLISECONDS);
        assertFalse(client.isConnected());
    }

    @Test
    public void testServerSendMessage() throws InterruptedException {
        sendLogin(USERNAME, PASSWORD);
        server.getSession().send(createMessage("TestMessage", createMap("TestChar", "A", "TestNumber", 12)));
        List<Pair<IMessage, ComparisonResult>> serverMessages = waitMessages(client, ServiceHandlerRoute.FROM_APP, createMessage("TestMessage", Collections.emptyMap()), 1000);
        assertEquals(1, serverMessages.size());
        IMessage testMessage = serverMessages.get(0).getFirst();
        assertNotEquals(null, testMessage);
        assertEquals("TestMessage", testMessage.getName());
        assertEquals("A", testMessage.getField("TestChar"));
        assertEquals(Integer.valueOf(12), testMessage.getField("TestNumber"));
    }

    private static void setIfPresented(IMessageStructure messageStructure, IMessage message, String field, Object value) {
        if (value == null) {
            return;
        }
        IFieldStructure fieldStructure = messageStructure.getFields().get(field);
        if (fieldStructure != null) {
            try {
                Class<?> clazz = Class.forName(fieldStructure.getJavaType().value());
                value = MultiConverter.convert(value, clazz);
                message.addField(field, value);
            } catch (ClassNotFoundException e) {
                throw new EPSCommonException("Cannot associate  [" + fieldStructure.getJavaType().value() + "] with any class");
            }
        }
    }

    private ScriptContext createScriptContext() {
        ISFContext sfContext = SFLocalContext.getDefault();
        IScriptReport report = new HtmlReport(1000, null, "report", sfContext.getWorkspaceDispatcher(), sfContext.getDictionaryManager(), RelevantMessagesSortingMode.ARRIVAL_TIME);
        ScriptContext sContext = new ScriptContext(sfContext, mock(IScriptProgress.class), report, mock(DebugController.class), "Test", 1);

        ScriptSettings scriptSettings = new ScriptSettings();
        IScriptConfig config = null;
        try {
            config = new DefaultScriptConfig(scriptSettings, "", "", logger);
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
        sContext.setScriptConfig(config);
        return sContext;
    }

    private ActionContext createActionContext(ScriptContext sContext, String serviceName, int timeout) {
        ActionContext actionContext = new ActionContext(sContext, true);
        actionContext.setReference("TestReference");
        actionContext.setDescription("Test description");
        actionContext.setCheckGroupsOrder(false);
        actionContext.setAddToReport(false);
        actionContext.setReorderGroups(false);
        actionContext.setServiceName(serviceName);
        actionContext.setTimeout(timeout);

        return actionContext;
    }

    private void init(){
        messageFactory = DefaultMessageFactory.getFactory();
        dictionary = serviceContext.getDictionaryManager().getDictionary(DICTIONARY_SURI);
        messageHelper.init(messageFactory, dictionary);
        messageHelper.getCodec(serviceContext);
        serviceMonitor = mock(IServiceMonitor.class);
    }

    private void initAndStartServer(ServiceName serverServiceName) {
        if (server == null) {
            ITCHCodecSettings codecSettings = new ITCHCodecSettings();
            codecSettings.setDictionaryURI(DICTIONARY_SURI);

            SOUPTcpServerSettings serverSettings = new SOUPTcpServerSettings();
            serverSettings.setHost(SERVER_ADDRESS);
            serverSettings.setPort(CURRENT_SERVER_PORT);
            serverSettings.setReadHeartBeatTimeout(SERVER_READ_HEARTBEATS_TIMEOUT_IN_SECONDS);
            serverSettings.setSendHeartBeatTimeout(SERVICE_SEND_HEARTBEATS_TIMEOUT_IN_SECONDS);
            serverSettings.setWaitLoginTimeout(WAIT_LOGIN_TIMEOUT_IN_MILLISECONDS);
            serverSettings.setSendHeartBeats(SERVER_SEND_HEARTBEATS);
            serverSettings.setDictionaryName(DICTIONARY_SURI);
            serverSettings.setFilterValues("");

            server = new SOUPTcpServer();

            server.init(serviceContext, serviceMonitor, new CollectorServiceHandler(), serverSettings, serverServiceName);
            server.start();

            while (server.getStatus() != ServiceStatus.STARTED) {
                try {
                    Thread.sleep(WAITING_START_MILLISECONDS);
                } catch (InterruptedException e) {
                    logger.debug("Waiting to start server was interrupt");
                }
            }
        }
    }

    private void initAndStartClient(ServiceName clientServiceName) {
        SOUPTcpClientSettings clientSettings = new SOUPTcpClientSettings();
        clientSettings.setAddress(SERVER_ADDRESS);
        clientSettings.setPort(CURRENT_SERVER_PORT);
        clientSettings.setDictionaryName(DICTIONARY_SURI);
        clientSettings.setUsername(USERNAME);
        clientSettings.setPassword(PASSWORD);
        clientSettings.setHeartbeatTimeout(CLIENT_HEARTBEATS_TIMEOUT_IN_SECONDS);
        clientSettings.setSendHeartBeats(CLIENT_SEND_HEARTBEATS);
        clientSettings.setDoLoginOnStart(false);

        client = new SOUPTcpClient();

        client.init(serviceContext, serviceMonitor, new CollectorServiceHandler(), clientSettings, clientServiceName);

        client.start();

        while (client.getStatus() != ServiceStatus.STARTED) {
            try {
                Thread.sleep(WAITING_START_MILLISECONDS);
            } catch (InterruptedException e) {
                logger.debug("Waiting to start server was interrupt");
            }
        }
    }

    private Map<String, Object> createMap(Object... objects) {
        Map<String, Object> result = new HashMap<>();
        for (int i = 0; i < objects.length - (objects.length % 2 == 0 ? 0 : 1); i++) {
            result.put((String) objects[i++], objects[i]);
        }
        return result;
    }

    private IMessage createMessage(String name, Map<String, Object> fields) {
        IMessageStructure messageStructure = dictionary.getMessages().get(name);
        IMessage message = messageFactory.createMessage(name, dictionary.getNamespace());
        for (Entry<String, Object> field : fields.entrySet()) {
            setIfPresented(messageStructure, message, field.getKey(), field.getValue());
        }
        return messageHelper.prepareMessageToEncode(message, null);
    }

    private List<Pair<IMessage, ComparisonResult>> waitMessages(SOUPTcpClient client, ServiceHandlerRoute route, IMessage filter, int timeout) {
        try {
            return WaitAction.waitMessage(client.getServiceHandler(), client.getSession(), route, actionContext.getCheckPoint(), timeout, filter, new ComparatorSettings(), Collections.emptySet(), false);
        } catch (InterruptedException e) {
            logger.debug("Interrupted waiting message");
            return Collections.emptyList();
        }
    }

    private boolean sendLogin(String username, String password) throws InterruptedException {
        IMessage login = createMessage(MESSAGE_LOGIN_REQUEST_PACKET,
                createMap(
                        "Username", username,
                        "Password", password,
                        "RequestedSession", RandomStringUtils.randomAlphanumeric(10),
                        "RequestedSequenceNumber", 0
                )
        );

        return client.sendMessage(login);
    }
}