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

import java.util.Iterator;
import java.util.Set;

import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.services.CSHIterator;
import com.exactpro.sf.services.IServiceHandler;
import com.exactpro.sf.services.ISession;
import com.exactpro.sf.services.ServiceHandlerRoute;
import com.exactpro.sf.services.tcpip.TCPIPServer;

public class WaitLogonResponse implements Runnable {

    private int timeout = 5000;
    private final IMessage messageReply;
    private final String logonMessageName;
    private final TCPIPServer server;
    private static final Logger logger = LoggerFactory.getLogger(WaitLogonResponse.class);

    public WaitLogonResponse(int timeout, IMessage messageReply, String logonMessageName, TCPIPServer server) {

        this.timeout = timeout;
        this.messageReply = messageReply;
        this.logonMessageName = logonMessageName;
        this.server = server;
    }

    @Override
    public void run() {
        IServiceHandler handler = server.getServiceHandler();
        ISession iSession = server.getSession();
        long timeEnd = System.currentTimeMillis() + timeout;
        Set<IoSession> sessions = server.getSessionMap().keySet();
        CSHIterator<IMessage> messagesIterator = handler.getIterator(iSession, ServiceHandlerRoute.FROM_APP, null);

        try {
            while(messagesIterator.hasNext(timeEnd - System.currentTimeMillis())) {
                IMessage message = messagesIterator.next();

                if(logonMessageName.equals(message.getName())) {
                    Iterator<IoSession> it = sessions.iterator();

                    if(it.hasNext()) {
                        it.next().write(messageReply);
                    }
                }
            }
        } catch(Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

}