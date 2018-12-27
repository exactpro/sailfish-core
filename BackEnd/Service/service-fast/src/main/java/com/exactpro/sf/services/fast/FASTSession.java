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
package com.exactpro.sf.services.fast;

import org.apache.commons.lang3.builder.ToStringBuilder;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.services.ISession;
import com.exactpro.sf.services.MessageHelper;

class FASTSession implements ISession {

	private final FASTAbstractClient fastClient;
	private final MessageHelper messageHelper;

	/**
	 * @param fastAbstractClient
	 */
	FASTSession(FASTAbstractClient fastAbstractClient, MessageHelper messageHelper) {
		this.fastClient = fastAbstractClient;
		this.messageHelper = messageHelper;
	}

	@Override
	public String getName() {
		return fastClient.getName();
	}

	@Override
	public IMessage send(Object message) throws InterruptedException {
	    Object preparedMessage = null;
	    
	    if (message instanceof IMessage) {
	        IMessage iMessage = (IMessage) message;
	        preparedMessage = messageHelper.prepareMessageToEncode(iMessage, null);
	        internalSend(preparedMessage);
	            return (IMessage)preparedMessage;
	    }

	    throw new EPSCommonException("Unknown type of message: " + message.getClass().getCanonicalName());
	}

	@Override
    public IMessage sendDirty(Object message) throws InterruptedException {
        throw new UnsupportedOperationException("Dirty send unsupported for FAST Client");
    }

    @Override
	public void close() {
        this.fastClient.closeSession();
	}

	@Override
	public boolean isClosed() {
	    return this.fastClient.isSessionClosed();
	}

	@Override
	public boolean isLoggedOn() {
		return fastClient.isLoggedOn();
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this).toString();
	}

	protected void internalSend(Object msg) throws InterruptedException {
	    synchronized (this.fastClient) {
            this.fastClient.send(msg);
	    }
	}
}