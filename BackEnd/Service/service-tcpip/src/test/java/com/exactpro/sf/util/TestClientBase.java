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

import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.aml.script.CheckPoint;
import com.exactpro.sf.aml.script.actions.WaitAction;
import com.exactpro.sf.aml.scriptutil.MessageCount;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.common.util.Pair;
import com.exactpro.sf.comparison.ComparatorSettings;
import com.exactpro.sf.comparison.ComparisonResult;
import com.exactpro.sf.comparison.ComparisonUtil;
import com.exactpro.sf.configuration.IDictionaryManager;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.scriptrunner.Outcome;
import com.exactpro.sf.scriptrunner.StatusType;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.IActionReport;
import com.exactpro.sf.services.CollectorServiceHandler;
import com.exactpro.sf.services.IEnvironmentMonitor;
import com.exactpro.sf.services.IInitiatorService;
import com.exactpro.sf.services.IServiceHandler;
import com.exactpro.sf.services.IServiceSettings;
import com.exactpro.sf.services.ISession;
import com.exactpro.sf.services.ServiceHandlerRoute;
import com.exactpro.sf.services.ServiceStatus;
import com.exactpro.sf.services.tcpip.TCPIPServer;
import com.exactpro.sf.storage.IMessageStorage;

import junit.framework.Assert;

/**
 * @author sergey.vasiliev
 *
 */
public abstract class TestClientBase extends AbstractTest {

    protected static TCPIPServer server;
    protected static IServiceHandler handlerServer;
    protected static IServiceHandler handlerClient;
    protected static IDictionaryManager dictionaryManager;
    protected static ServiceName serviceName;
    protected static IMessageStorage mockedStorage;
    protected static IEnvironmentMonitor mockedMonitor;
    protected static String host = "localhost";
    private static CheckPoint cp = new CheckPoint();
    private static IActionReport mockedReport;

    private static int sleepTimeout = 100;
    private static int timeout = 1000;

    private static final Logger logger = LoggerFactory.getLogger(TestClientBase.class);

    @After
    public void cleanServersCollection() {
        server.getServiceHandler().cleanMessages(ServiceHandlerRoute.values());
    }

    protected static void waitPortAvailable(int port) throws InterruptedException, SocketException, IOException {
        try {
            long time = System.currentTimeMillis() + 5000;
            while (System.currentTimeMillis() < time) {
                if (available(port)) {
                    break;
                }
                Thread.sleep(sleepTimeout);
            }
            Thread.sleep(timeout);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
        if (!available(port)) {
            Assert.fail("Port " + port + " is not available");
        }
    }

    protected static boolean available(int port) throws InterruptedException, SocketException, IOException {
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("Invalid start port: " + port);
        }
        boolean connected = false;
        long time = System.currentTimeMillis() + timeout;
        while (System.currentTimeMillis() < time) {
            ServerSocket ss = null;
            try {
                Thread.sleep(sleepTimeout);
                ss = new ServerSocket(port);
                ss.setReuseAddress(true);
                connected = true;
                break;
            } catch (IOException e) {
                if (System.currentTimeMillis() == time) {
                    logger.error(e.getMessage(), e);
                    throw e;
                } else
                    continue;
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
                throw e;
            } finally {
                if (ss != null) {
                    try {
                        ss.close();
                    } catch (IOException e) {
                        logger.error(e.getMessage(), e);
                        throw e;
                    }
                }
            }
        }
        return connected;
    }



    protected static void startServer(int port, SailfishURI dictionaryName, IServiceSettings settings) throws Exception {
        server = initializedServer(port, dictionaryName, settings);
        server.start();
        long time = System.currentTimeMillis() + 3000;
        while (System.currentTimeMillis() < time && !ServiceStatus.STARTED.equals(server.getStatus())) {
            Thread.sleep(sleepTimeout);
        }
        Assert.assertEquals(ServiceStatus.STARTED, server.getStatus());
    }

    protected static TCPIPServer initializedServer(int port, SailfishURI dictionaryName, IServiceSettings settings) throws Exception {
        try {
            waitPortAvailable(port);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }

        serviceName = new ServiceName(ServiceName.DEFAULT_ENVIRONMENT, "TCPIPServer");
        handlerServer = new CollectorServiceHandler();
        mockedStorage = mock(IMessageStorage.class);
        mockedMonitor = mock(IEnvironmentMonitor.class);
        mockedReport = mock(IActionReport.class);

        if (dictionaryManager == null) {
            dictionaryManager = serviceContext.getDictionaryManager();
        }

        TCPIPServer server = new TCPIPServer();
        server.init(serviceContext, mockedMonitor, handlerServer, settings, serviceName);
        Assert.assertEquals(ServiceStatus.INITIALIZED, server.getStatus());
        return server;
    }

    protected static int countCollapse(IServiceHandler handler, ISession session, IMessage messageFilter, int timeout, ServiceHandlerRoute prov,
            SailfishURI dictionaryName, String serviceName, int messageNumber, Outcome outcome) {
        MessageCount count = MessageCount.fromString("=" + messageNumber);
        int result = -1;
        try {
            long time = System.currentTimeMillis() + timeout;
            while (time > System.currentTimeMillis()) {
                result = WaitAction.countMessages(mockedReport, serviceName, messageFilter, count, handler, session, cp, !prov.isAdmin(), getCompareSettings(dictionaryName));
                if (messageNumber == result) {
                    break;
                }
                Thread.sleep(sleepTimeout);
            }
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
        return result;
    }

    protected static Pair<IMessage, ComparisonResult> waitCollapse(IServiceHandler handler, ISession session, IMessage messageFilter, int timeout,
            ServiceHandlerRoute prov, SailfishURI dictionaryName, String serviceName, CheckPoint cp) {
        Pair<IMessage, ComparisonResult> result = new Pair<>(null, null);
        try {
            List<Pair<IMessage, ComparisonResult>> list = WaitAction.waitMessage(handler, session, prov, cp, timeout, messageFilter, getCompareSettings(dictionaryName));
            if (list.size() != 0) {
                IMessage resultMessage = list.get(list.size() - 1).getFirst();
                ComparisonResult comparision = list.get(list.size() - 1).getSecond();
                if (ComparisonUtil.getResultCount(comparision, StatusType.FAILED) == 0) {
                    comparision.setStatus(StatusType.PASSED);
                } else {
                    comparision.setStatus(StatusType.FAILED);
                }
                result.setFirst(resultMessage);
                result.setSecond(comparision);
            }
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
        return result;
    }

    protected static List<IMessage> getMessages(List<Object> objects, String name) {

        List<IMessage> messages = new ArrayList<>();
        for (Object o : objects) {
            if (o instanceof IMessage) {
                if (name.equals(((IMessage) o).getName())) {
                    messages.add((IMessage) o);
                }
            }
        }
        return messages;
    }

    protected static int waitCountMessages(IInitiatorService service, IMessage messageFilter, int timeout, ServiceHandlerRoute prov,
            SailfishURI dictionaryName, int messageNumber, Outcome outcome) {
        return countCollapse(service.getServiceHandler(), service.getSession(), messageFilter, timeout, prov, dictionaryName, service.getName(),
                messageNumber, outcome);
    }

    protected static Pair<IMessage,ComparisonResult> waitMessage(IInitiatorService service, IMessage messageFilter, int timeout, ServiceHandlerRoute prov,
            SailfishURI dictionaryName, CheckPoint cp) {
        return waitCollapse(service.getServiceHandler(), service.getSession(), messageFilter, timeout, prov, dictionaryName, service.getName(), cp);
    }

    protected static Pair<IMessage, ComparisonResult> waitMessage(IInitiatorService service, IMessage messageFilter, int timeout,
            ServiceHandlerRoute prov, SailfishURI dictionaryName) {
        return waitCollapse(service.getServiceHandler(), service.getSession(), messageFilter, timeout, prov, dictionaryName, service.getName(), cp);
    }

    private static ComparatorSettings getCompareSettings(SailfishURI dictionaryName) {
        ComparatorSettings compSettings = new ComparatorSettings();
        compSettings.setDictionaryStructure(dictionaryManager.getDictionary(dictionaryName));
        return compSettings;
    }
}
