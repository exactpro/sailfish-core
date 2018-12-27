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

public interface IServiceHandler {

    void sessionOpened(ISession session) throws ServiceHandlerException;

    void sessionClosed(ISession session) throws ServiceHandlerException;

    void sessionIdle(ISession session, IdleStatus status) throws ServiceHandlerException;

    void exceptionCaught(ISession session, Throwable cause);

    void putMessage(ISession session, ServiceHandlerRoute route, IMessage message) throws ServiceHandlerException;

    CSHIterator<IMessage> getIterator(ISession session, ServiceHandlerRoute route, CheckPoint checkPoint);

    List<IMessage> getMessages(ISession session, ServiceHandlerRoute route, CheckPoint checkPoint);

    void registerCheckPoint(ISession session, ServiceHandlerRoute route, CheckPoint checkPoint);

    void cleanMessages(ServiceHandlerRoute... routes);
}
