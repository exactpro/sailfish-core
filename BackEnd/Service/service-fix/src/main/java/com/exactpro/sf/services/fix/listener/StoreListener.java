/*******************************************************************************
 * Copyright 2009-2020 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.services.fix.listener;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.configuration.ILoggingConfigurator;
import com.exactpro.sf.services.IServiceHandler;
import com.exactpro.sf.services.IServiceSettings;
import com.exactpro.sf.services.ISession;
import com.exactpro.sf.services.MessageHelper;
import com.exactpro.sf.services.ServiceHandlerRoute;
import com.exactpro.sf.storage.IMessageStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.exactpro.sf.services.ServiceHandlerRoute.FROM_ADMIN;
import static com.exactpro.sf.services.ServiceHandlerRoute.FROM_APP;
import static com.exactpro.sf.services.ServiceHandlerRoute.TO_ADMIN;
import static com.exactpro.sf.services.ServiceHandlerRoute.TO_APP;

public class StoreListener extends FIXListener {
    private final IMessageStorage messageStorage;
    private final boolean keepMessagesInMemory;
    private final IServiceHandler handler;

    public StoreListener(IMessageStorage messageStorage, boolean keepMessagesInMemory, IServiceHandler handler) {
        this.messageStorage = messageStorage;
        this.keepMessagesInMemory = keepMessagesInMemory;
        this.handler = handler;
    }

    @Override
    public void fromAdmin(ISession session, IMessage msg) {
        storeMessage(session, msg, FROM_ADMIN);
    }

    @Override
    public void toAdmin(ISession session, IMessage msg) {
        storeMessage(session, msg, TO_ADMIN);
    }

    @Override
    public void fromApp(ISession session, IMessage msg) {
        storeMessage(session, msg, FROM_APP);
    }

    @Override
    public void toApp(ISession session, IMessage msg) {
        storeMessage(session, msg, TO_APP);
    }

    @Override
    public void onMessageRejected(ISession session, IMessage msg, String reason) {
        storeMessage(session, msg, reason);
    }

    protected void storeMessage(ISession iSession, IMessage iMessage, ServiceHandlerRoute route) {
        storeMessage(iSession, iMessage, route, null);
    }

    protected void storeMessage(ISession iSession, IMessage iMessage, String reason) {
        storeMessage(iSession, iMessage, null, reason);
    }

    protected void storeMessage(ISession iSession, IMessage iMessage, ServiceHandlerRoute route, String reason) {
        if (keepMessagesInMemory) {
            try {
                iMessage.getMetaData().setRejectReason(reason);
                try {
                    messageStorage.storeMessage(iMessage);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }

                if (route != null) {
                    handler.putMessage(iSession, route, iMessage);
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                handler.exceptionCaught(iSession, e);
            }
        }
    }
}
