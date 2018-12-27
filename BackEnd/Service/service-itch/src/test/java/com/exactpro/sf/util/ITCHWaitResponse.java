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
import com.exactpro.sf.services.IServiceHandler;
import com.exactpro.sf.services.ISession;
import com.exactpro.sf.services.ServiceHandlerRoute;
import com.exactpro.sf.services.tcpip.TCPIPServer;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

public class ITCHWaitResponse implements Runnable {

    private int timeout = 5000;
    private int sleepTimeout = 100;
    private final IMessage messageReply;
    private final String logonMessageName;
    private final TCPIPServer server;
    private static final String INCLUDED_MESSAGES = "IncludedMessages";
    private static final Logger logger = LoggerFactory.getLogger(ITCHWaitResponse.class);

    public ITCHWaitResponse(int timeout, IMessage messageReply, String logonMessageName, TCPIPServer server) {

        this.timeout = timeout;
        this.messageReply = messageReply;
        this.logonMessageName = logonMessageName;
        this.server = server;
    }

    @Override
    public void run() {
        IServiceHandler handler = server.getServiceHandler();
        ISession iSession = server.getSession();
        Set<IoSession> sessions = server.getSessionMap().keySet();
        long timeEnd = System.currentTimeMillis() + timeout;
        while (timeEnd > System.currentTimeMillis()) {
            for (IoSession session : sessions) {
                List<IMessage> handlerMessages = handler.getMessages(iSession, ServiceHandlerRoute.FROM_APP, null);
found:          for(IMessage message : handlerMessages) {
                    Object field = message.getField(INCLUDED_MESSAGES);
                    if (field != null && field instanceof List) {
                        for (Object value : (List<?>) field) {
                            if (value instanceof IMessage && logonMessageName.equals(((IMessage) value).getName())) {
                                session.write(messageReply);
                                break found;
                            }
                        }
                    }
                }
            }
            try {
                Thread.sleep(sleepTimeout);
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }
}