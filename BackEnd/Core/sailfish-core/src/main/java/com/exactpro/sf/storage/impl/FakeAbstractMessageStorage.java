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

package com.exactpro.sf.storage.impl;

import java.time.Instant;
import java.util.Collections;

import com.exactpro.sf.common.messages.IHumanMessage;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.configuration.DictionaryManager;
import com.exactpro.sf.storage.MessageFilter;
import com.exactpro.sf.storage.MessageRow;

public class FakeAbstractMessageStorage extends AbstractMessageStorage {

    public FakeAbstractMessageStorage(DictionaryManager dictionaryManager) {
        super(dictionaryManager);
    }

    @Override
    protected void storeMessage(IMessage message, IHumanMessage humanMessage, String jsonMessage) {
        logger.trace("Message {} skipped by service settings 'persistMessageToStorage'.", message.getName());
    }

    @Override
    public Iterable<MessageRow> getMessages(int count, MessageFilter filter) {
        return Collections.emptyList();
    }

    @Override
    public Iterable<MessageRow> getMessages(int offset, int count, String where) {
        return Collections.emptyList();
    }

    @Override
    public void clear() {

    }

    @Override
    public void dispose() {

    }

    @Override
    public void removeMessages(Instant olderThan) {

    }

    @Override
    public void removeMessages(String serviceID) {

    }


}
