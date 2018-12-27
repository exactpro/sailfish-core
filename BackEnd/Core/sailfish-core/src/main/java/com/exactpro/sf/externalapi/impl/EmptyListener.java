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
package com.exactpro.sf.externalapi.impl;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.externalapi.IServiceListener;
import com.exactpro.sf.externalapi.IServiceProxy;
import com.exactpro.sf.services.IdleStatus;
import com.exactpro.sf.services.ServiceEvent;
import com.exactpro.sf.services.ServiceHandlerRoute;

/**
 * @author sergey.smirnov
 *
 */
public class EmptyListener implements IServiceListener {

    /*
     * (non-Javadoc)
     *
     * @see com.exactpro.sf.externalapi.IServiceListener#
     * sessionOpened(com.exactpro.sf.externalapi.
     * IServiceProxy)
     */
    @Override
    public void sessionOpened(IServiceProxy service) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see com.exactpro.sf.externalapi.IServiceListener#
     * sessionClosed(com.exactpro.sf.externalapi.
     * IServiceProxy)
     */
    @Override
    public void sessionClosed(IServiceProxy service) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see com.exactpro.sf.externalapi.IServiceListener#
     * sessionIdle(com.exactpro.sf.externalapi.IServiceProxy,
     * com.exactpro.sf.services.IdleStatus)
     */
    @Override
    public void sessionIdle(IServiceProxy service, IdleStatus status) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see com.exactpro.sf.externalapi.IServiceListener#
     * exceptionCaught(com.exactpro.sf.externalapi.
     * IServiceProxy, java.lang.Throwable)
     */
    @Override
    public void exceptionCaught(IServiceProxy service, Throwable cause) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.exactpro.sf.externalapi.IServiceListener#onMessage(
     * com.exactpro.sf.externalapi.IServiceProxy,
     * com.exactpro.sf.common.messages.IMessage,
     * com.exactpro.sf.services.ServiceHandlerRoute)
     */
    @Override
    public void onMessage(IServiceProxy service, IMessage message, boolean rejected, ServiceHandlerRoute route) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.exactpro.sf.externalapi.IServiceListener#onEvent(
     * com.exactpro.sf.externalapi.IServiceProxy,
     * com.exactpro.sf.services.ServiceEvent)
     */
    @Override
    public void onEvent(IServiceProxy service, ServiceEvent event) {
        // TODO Auto-generated method stub

    }
}
