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
package com.exactpro.sf.storage;

import java.time.Instant;

import com.exactpro.sf.center.IDisposable;
import com.exactpro.sf.common.messages.IMessage;

public interface IMessageStorage extends IDisposable {
    public static final int BUFFER_SIZE = 50;

	@Deprecated // move to proper class
    default ScriptRun openScriptRun(String name, String description) {
        return null;
    }

	@Deprecated // move to proper class
    default void closeScriptRun(ScriptRun scriptRun) {
    }

	/**
	 * @param message - Message to save (can be null for exceptions/unknown messages)
	 */
	void storeMessage(IMessage message);

	Iterable<MessageRow> getMessages(int count, MessageFilter filter);

	Iterable<MessageRow> getMessages(int offset, int count, String where);

    default void removeMessages(Instant olderThan) {
        throw new UnsupportedOperationException("Removing messages by timestamp is not supported");
    }

    default void removeMessages(String serviceID) {
        throw new UnsupportedOperationException("Removing messages by service ID is not supported");
    }

	void clear();

}
