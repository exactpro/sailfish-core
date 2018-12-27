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
package com.exactpro.sf.services;

import java.util.List;

import com.exactpro.sf.aml.script.CheckPoint;
import com.exactpro.sf.common.messages.IMessage;

public class OptionalServiceHandlerWrapper implements IServiceHandler {

    private static final IServiceHandler NULL_SERVICE_HANDLER = new EmptyStubServiceHandler();

    private volatile IServiceHandler currentServiceHandler;

    private volatile IServiceHandler originServiceHandler;

    public OptionalServiceHandlerWrapper() {
        this.currentServiceHandler = this.originServiceHandler = NULL_SERVICE_HANDLER;
    }

    @Override
    public void sessionOpened(ISession session) throws ServiceHandlerException {
        currentServiceHandler.sessionOpened(session);
    }

    @Override
    public void sessionClosed(ISession session) throws ServiceHandlerException {
        currentServiceHandler.sessionClosed(session);
    }

    @Override
    public void sessionIdle(ISession session, IdleStatus status) throws ServiceHandlerException {
        currentServiceHandler.sessionIdle(session, status);
    }

    @Override
    public void exceptionCaught(ISession session, Throwable cause) {
        currentServiceHandler.exceptionCaught(session, cause);
    }

    @Override
    public void putMessage(ISession session, ServiceHandlerRoute route, IMessage message) throws ServiceHandlerException {
        currentServiceHandler.putMessage(session, route, message);
    }

    @Override
    public CSHIterator<IMessage> getIterator(ISession session, ServiceHandlerRoute route, CheckPoint checkPoint) {
        return currentServiceHandler.getIterator(session, route, checkPoint);
    }

    @Override
    public List<IMessage> getMessages(ISession session, ServiceHandlerRoute route, CheckPoint checkPoint) {
        return currentServiceHandler.getMessages(session, route, checkPoint);
    }

    @Override
    public void registerCheckPoint(ISession session, ServiceHandlerRoute route, CheckPoint checkPoint) {
        currentServiceHandler.registerCheckPoint(session, route, checkPoint);
    }

    @Override
    public void cleanMessages(ServiceHandlerRoute... routes) {
        currentServiceHandler.cleanMessages(routes);
    }

    public void setOriginServiceHandler(IServiceHandler originServiceHandler) {
        this.originServiceHandler = originServiceHandler;
    }

    public void storeMessages(boolean store) {
        this.currentServiceHandler = store ? this.originServiceHandler : NULL_SERVICE_HANDLER;
    }
}
