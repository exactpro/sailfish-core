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

import java.sql.Timestamp;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;

import org.apache.commons.lang3.ObjectUtils;

import com.exactpro.sf.storage.MessageFilter;
import com.exactpro.sf.storage.MessageRow;
import com.exactpro.sf.storage.StorageException;
import com.exactpro.sf.util.Interner;

public class FileMessageLoader extends MessageRowLoaderBase<FileMessage> {
    private final List<FileMessage> source;
    private final boolean ascending;
    private final long startTime;
    private final long finishTime;

    private int nextIndex = 0;

    public FileMessageLoader(List<FileMessage> source, MessageFilter filter, int count, int bufferSize) {
        super(filter, count, bufferSize);
        this.source = source;
        this.ascending = ObjectUtils.defaultIfNull(filter.getSortOrder(), true);
        this.startTime = (filter.getStartTime() != null ? filter.getStartTime().getTime() : 0) / 1000;
        this.finishTime = (filter.getFinishTime() != null ? filter.getFinishTime().getTime() : Long.MAX_VALUE) / 1000;
    }

    public FileMessageLoader(List<FileMessage> source, MessageFilter filter, int count) {
        this(source, filter, count, DEFAULT_BUFFER_SIZE);
    }

    @Override
    protected void retrieveMessages(Queue<FileMessage> forMessages, int count, long lastID) {
        if(lastID == -1 || lastID == source.size()) {
            return;
        }

        try {
            ListIterator<FileMessage> it = source.listIterator((int)lastID);

            while(count > 0 && (ascending ? it.hasNext() : it.hasPrevious())) {
                FileMessage message = ascending ? it.next() : it.previous();

                if(!checkMessage(message, filter)) {
                    continue;
                }

                forMessages.add(message);
                count--;
            }

            nextIndex = ascending ? it.nextIndex() : it.previousIndex();
        } catch(IndexOutOfBoundsException | NoSuchElementException e) {
            throw new StorageException("Message list probably have been cleared", e);
        }
    }

    @Override
    protected MessageRow convert(FileMessage msg, Interner<String> interner) {
        return FileMessageStorage.convert(msg, interner, isHex);
    }

    @Override
    protected long getID(FileMessage msg) {
        return nextIndex;
    }

    private boolean checkLastModified(FileMessage message) {
        long lastModified = message.getLastModified() / 1000;
        return startTime <= lastModified && lastModified <= finishTime;
    }

    private boolean checkMessage(FileMessage message, MessageFilter filter) {
        return checkLastModified(message) &&
                ge(message.getTimestamp(), filter.getStartTime()) &&
                le(message.getTimestamp(), filter.getFinishTime()) &&
                eq(message.isAdmin(), filter.getShowAdmin()) &&
                ilike(message.getFrom(), filter.getFrom()) &&
                ilike(message.getTo(), filter.getTo()) &&
                ilike(message.getName(), filter.getMsgName()) &&
                ilike(message.getNamespace(), filter.getMsgNameSpace()) &&
                ilike(message.getHumanMessage(), filter.getHumanMessage()) &&
                in(message.getServiceID(), filter.getServicesIdSet());
    }

    private boolean ilike(String value, String filter) {
        return filter != null ? filter.equalsIgnoreCase(value) : true;
    }

    private boolean eq(Boolean value, Boolean filter) {
        return filter != null ? filter.equals(value) : true;
    }

    private boolean ge(Timestamp value, Timestamp filter) {
        return filter != null ? filter.compareTo(value) <= 0 : true;
    }

    private boolean le(Timestamp value, Timestamp filter) {
        return filter != null ? filter.compareTo(value) >= 0 : true;
    }

    private boolean in(String value, Set<String> filter) {
        return filter != null ? filter.contains(value) : true;
    }
}
