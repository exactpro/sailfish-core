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
package com.exactpro.sf.externalapi;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.services.IdleStatus;
import com.exactpro.sf.services.ServiceEvent;
import com.exactpro.sf.services.ServiceHandlerRoute;

public interface IServiceListener {
    
    void sessionOpened(IServiceProxy service);

    void sessionClosed(IServiceProxy service);

    void sessionIdle(IServiceProxy service, IdleStatus status);

    void exceptionCaught(IServiceProxy service, Throwable cause);

    /**
     * This handler called after sending or receiving message.
     *
     * @param service instance of service which processed message
     * @param message 
     * @param rejected 
     * @param route the message route
     */
    void onMessage(IServiceProxy service, IMessage message, boolean rejected, ServiceHandlerRoute route);
    
    /**
     * This handler called on service state change.
     * @param service
     * @param event
     */
    void onEvent(IServiceProxy service, ServiceEvent event);

}
