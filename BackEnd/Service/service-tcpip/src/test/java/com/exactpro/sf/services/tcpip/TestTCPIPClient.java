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
package com.exactpro.sf.services.tcpip;

import org.apache.mina.core.session.IoSession;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.impl.messages.DefaultMessageFactory;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.util.Pair;
import com.exactpro.sf.comparison.ComparisonResult;
import com.exactpro.sf.scriptrunner.StatusType;
import com.exactpro.sf.services.ServiceHandlerRoute;
import com.exactpro.sf.services.ServiceStatus;
import com.exactpro.sf.util.TestTCPIPClientBase;

import junit.framework.Assert;

public class TestTCPIPClient extends TestTCPIPClientBase {

    private static final Logger logger = LoggerFactory.getLogger(TestTCPIPClient.class);
    private static final int LOGIN_TIMEOUT = 500*2;
    private static final int WAITING_TIMEOUT = 500*2;

    @BeforeClass
    public static void setup() throws Exception {
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

    @Test
    public void testConnection() throws Exception {
        logger.info("start testConnection()");
        try {
            startServices(LOGIN_TIMEOUT);
            Assert.assertTrue(client.isConnected());

            IMessage msg = DefaultMessageFactory.getFactory().createMessage(TCPIPMessageHelper.INCOMING_MESSAGE_NAME_AND_NAMESPACE, TCPIPMessageHelper.INCOMING_MESSAGE_NAME_AND_NAMESPACE);
            msg.addField("MessageType", 123);
            msg.addField("Qty", 123.123);
            msg.addField("ClOrdID", "");

            client.getSession().send(msg);

            if (logger.isDebugEnabled()) {
                logger.debug("start to waiting messages on server; {} millisec", WAITING_TIMEOUT);
            }
            Pair<IMessage, ComparisonResult> result = waitMessage(server, msg, WAITING_TIMEOUT, ServiceHandlerRoute.FROM_APP, dictionaryName);
            ComparisonResult comparison=result.getSecond();
            Assert.assertEquals("Status of comparison isn't " + StatusType.PASSED + "; see: \n" + comparison, StatusType.PASSED,
                    comparison.getStatus());
            if (logger.isDebugEnabled()) {
                logger.debug("end to waiting messages on server. Result: {} messages found", result.getFirst());
            }

            for (IoSession session:server.getSessionMap().keySet()){
                session.write(msg);
            }

            if (logger.isDebugEnabled()) {
                logger.debug("start to waiting messages on client; {} millisec", WAITING_TIMEOUT);
            }
            result = waitMessage(client, msg, WAITING_TIMEOUT, ServiceHandlerRoute.FROM_APP, dictionaryName);
            comparison = result.getSecond();
            Assert.assertEquals("Status of comparison isn't " + StatusType.PASSED + "; see: \n" + comparison, StatusType.PASSED,
                    comparison.getStatus());
            if (logger.isDebugEnabled()) {
                logger.debug("end to waiting messages on client. Result: {} messages found", result.getFirst());
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        } finally {
            logger.info("end testConnection()");
            client.dispose();
            Assert.assertEquals(ServiceStatus.DISPOSED, client.getStatus());
        }
    }

}
