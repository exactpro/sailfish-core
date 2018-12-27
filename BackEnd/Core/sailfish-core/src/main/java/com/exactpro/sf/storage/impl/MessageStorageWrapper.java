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

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.storage.IMessageStorage;
import com.exactpro.sf.storage.MessageFilter;
import com.exactpro.sf.storage.MessageRow;

public abstract class MessageStorageWrapper implements IMessageStorage {

    protected final IMessageStorage messageStorage;
    
    public MessageStorageWrapper(IMessageStorage messageStorage) {
        this.messageStorage = messageStorage;
    }
    
    @Override
    public void dispose() {
        this.messageStorage.dispose();
    }

    @Override
    public void storeMessage(IMessage message) {
        this.messageStorage.storeMessage(message);
    }

    @Override
    public Iterable<MessageRow> getMessages(int count, MessageFilter filter) {
        return this.messageStorage.getMessages(count, filter);
    }

    @Override
    public Iterable<MessageRow> getMessages(int offset, int count, String where) {
        return this.messageStorage.getMessages(offset, count, where);
    }

    @Override
    public void clear() {
        this.messageStorage.clear();
    }

}
