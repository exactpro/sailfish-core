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
package com.exactpro.sf.storage.impl;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Instant;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.storage.IMessageStorage;
import com.exactpro.sf.storage.MessageFilter;
import com.exactpro.sf.storage.MessageRow;

public class FakeMessageStorage implements IMessageStorage {

    private static final Logger logger = LoggerFactory.getLogger(FakeMessageStorage.class);

	@Override
	public void storeMessage(IMessage message) {
        logger.trace("Message {} skipped by service settings 'persistMessageToStorage'.", message.getName());
	}

	@Override
	public List<MessageRow> getMessages(int count, MessageFilter filter) {
		return Collections.emptyList();
	}

	@Override
	public List<MessageRow> getMessages(int offset, int count, String where) {
		return Collections.emptyList();
	}

	@Override
	public void dispose() {
	}

	@Override
	public void clear() {
	}

    @Override
    public void removeMessages(Instant olderThan) {
    }

    @Override
    public void removeMessages(String serviceID) {
    }
}
