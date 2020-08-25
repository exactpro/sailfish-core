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

import com.exactpro.sf.common.messages.IMessage;

public interface ISession
{

	/**
	 * @return session name
	 */
	String getName();

	/**
	 * Sends a message within send message timeout from service settings
	 * @param message object to be sent
	 */
	IMessage send(Object message) throws InterruptedException;

    /**
     * Sends a message within timeout
     * @param message object to be sent
     * @param timeout time in milliseconds for message sending. It should be greater than zero.
     */
    default IMessage send(Object message, long timeout) throws InterruptedException {
        return send(message);
    }

	/**
	 * Sends dirty message within send message timeout from service settings
	 * @param message object to be sent
	 */
	IMessage sendDirty(Object message)  throws InterruptedException;

	/**
	 * Sends dirty message within timeout
	 * @param message object to be sent
     * @param timeout time in milliseconds for message sending. It should be greater than zero.
	 */
	default IMessage sendDirty(Object message, long timeout)  throws InterruptedException {
	    return sendDirty(message);
    }

	/**
	 * close session
	 */
	void close();

	/**
	 * Check whether session is closed.
	 * @return return {@code true} if session is closed. {@code false} otherwise.
	 */
	boolean isClosed();

	/**
	 * Is the session logged on.
	 * @return true if logged on, false otherwise.
	 */
	boolean isLoggedOn();

    /**
     * close session without sending message on closing
     */
    default void forceClose() {
        throw new UnsupportedOperationException("Force close is not implemented");
    }
}
