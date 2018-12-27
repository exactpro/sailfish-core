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

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;

import com.exactpro.sf.storage.MessageFilter;
import com.exactpro.sf.storage.MessageRow;
import com.exactpro.sf.util.CHMInterner;
import com.exactpro.sf.util.Interner;

/**
 *
 */
public abstract class MessageRowLoaderBase<T> implements Iterable<MessageRow> {
    protected static final int DEFAULT_BUFFER_SIZE = 10_000;

    protected final MessageFilter filter;
    protected final int count;
    protected final boolean isHex;
    protected final int bufferSize;

    protected final Queue<T> buffer;

    public MessageRowLoaderBase(MessageFilter filter, int count, int bufferSize) {
        if (count > 0 && count < bufferSize) {
            bufferSize = count;
        }
        this.filter = filter;
        this.count = count;
        this.bufferSize = bufferSize;
        if (filter.getRawMessage() != null) {
            this.isHex = "hex".equals(filter.getRawMessage());
        } else {
            this.isHex = true;
        }
        this.buffer = new ArrayDeque<>(this.bufferSize);
    }

    public MessageRowLoaderBase(MessageFilter filter, int count) {
        this(filter, count, DEFAULT_BUFFER_SIZE);
    }

    protected abstract void retrieveMessages(Queue<T> forMessages, int count, long lastID);

    protected abstract MessageRow convert(T msg, Interner<String> interner);

    protected abstract long getID(T msg);

    @Override
    public Iterator<MessageRow> iterator() {

        return new Iterator<MessageRow>() {

            int counter = 0;
            private long lastID;
            private final Interner<String> interner = new CHMInterner<>();
            {
                lastID = (Boolean.FALSE.equals(filter.getSortOrder()))
                        ? Long.MAX_VALUE
                        : 0;
                retrieveMessages(MessageRowLoaderBase.this.buffer, MessageRowLoaderBase.this.bufferSize, this.lastID);
            }

            @Override
            public boolean hasNext() {
                return needMessages() && !buffer.isEmpty();
            }

            @Override
            public MessageRow next() {
                this.counter++;
                T tmp = buffer.remove();
                this.lastID = getID(tmp);
                if (MessageRowLoaderBase.this.buffer.isEmpty() && needMessages()) {
                    retrieveMessages(MessageRowLoaderBase.this.buffer, MessageRowLoaderBase.this.bufferSize, this.lastID);
                }
                return convert(tmp, this.interner);
            }

            private boolean needMessages() {
                return (this.counter < MessageRowLoaderBase.this.count || MessageRowLoaderBase.this.count < 0);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();

            }
        };
    }
}
