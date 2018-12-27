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
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.io.FilenameUtils;
import java.time.Instant;

import com.exactpro.sf.common.messages.IHumanMessage;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.MessageUtil;
import com.exactpro.sf.common.messages.MsgMetaData;
import com.exactpro.sf.common.services.ServiceInfo;
import com.exactpro.sf.common.util.HexDumper;
import com.exactpro.sf.configuration.DictionaryManager;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.storage.IObjectFlusher;
import com.exactpro.sf.storage.MessageFilter;
import com.exactpro.sf.storage.MessageList;
import com.exactpro.sf.storage.MessageRow;
import com.exactpro.sf.storage.ScriptRun;
import com.exactpro.sf.storage.StorageException;
import com.exactpro.sf.util.CHMInterner;
import com.exactpro.sf.util.Interner;

public class FileMessageStorage extends AbstractMessageStorage {
    private static final String MESSAGES_DIR = "messages";

    private final List<FileMessage> messages;
    private final boolean storeAdminMessages;
    private final IObjectFlusher<FileMessage> flusher;
    private final AtomicLong scriptRunId;
    private final CHMInterner<String> interner;
    private final AtomicLong messageID;

    public FileMessageStorage(String path, boolean storeAdminMessages, IWorkspaceDispatcher dispatcher, DictionaryManager dictionaryManager) {
        super(dictionaryManager);
        
        Objects.requireNonNull(path, "path cannot be null");
        Objects.requireNonNull(dispatcher, "dispatcher cannot be null");

        this.messages = new MessageList(FilenameUtils.concat(path, MESSAGES_DIR), dispatcher);
        this.storeAdminMessages = storeAdminMessages;
        this.flusher = new ObjectFlusher<>(new ListFlushProvider<>(this.messages), BUFFER_SIZE);
        this.scriptRunId = new AtomicLong();
        this.interner = new CHMInterner<>();
        this.messageID = new AtomicLong(messages.size());

        this.flusher.start();
    }

    @Override
    public ScriptRun openScriptRun(String name, String description) {
        ScriptRun scriptRun = createScriptRun(name, description);
        scriptRun.setId(scriptRunId.getAndIncrement());
        return scriptRun;
    }

    @Override
    public void closeScriptRun(ScriptRun scriptRun) {
        scriptRun.setFinish(new Timestamp(System.currentTimeMillis()));
    }

    @Override
    protected void storeMessage(IMessage message, IHumanMessage humanMessage, String jsonMessage) {
        MsgMetaData metaData = message.getMetaData();

        if(!storeAdminMessages && metaData.isAdmin()) {
            return;
        }

        FileMessage fileMessage = new FileMessage();
        ServiceInfo serviceInfo = metaData.getServiceInfo();

        fileMessage.setID(messageID.getAndIncrement());
        fileMessage.setAdmin(metaData.isAdmin());
        fileMessage.setTimestamp(new Timestamp(metaData.getMsgTimestamp().getTime()));
        fileMessage.setFrom(metaData.getFromService());
        fileMessage.setTo(metaData.getToService());
        fileMessage.setName(metaData.isRejected() ? metaData.getMsgName() + MessageUtil.MESSAGE_REJECTED_POSTFIX : metaData.getMsgName());
        fileMessage.setNamespace(metaData.getMsgNamespace());
        fileMessage.setHumanMessage(humanMessage.toString());
        fileMessage.setJsonMessage(jsonMessage);
        fileMessage.setRawMessage(metaData.getRawMessage());
        fileMessage.setServiceID(serviceInfo != null ? serviceInfo.getID() : null);
        fileMessage.setMetaDataID(metaData.getId());
        fileMessage.setRejectReason(metaData.getRejectReason());

        flusher.add(fileMessage);
    }

    @Override
    public synchronized void dispose() {
        flusher.stop();
    }

    @Override
    public Iterable<MessageRow> getMessages(int count, MessageFilter filter) {
        flusher.flush();
        return new FileMessageLoader(messages, filter, count);
    }

    @Override
    public List<MessageRow> getMessages(int offset, int count, String where) {
        flusher.flush();

        try {
            List<MessageRow> result = new ArrayList<>(count);
            ListIterator<FileMessage> it = messages.listIterator(messages.size() - offset);

            while(--count >= 0 && it.hasPrevious()) {
                result.add(convert(it.previous(), interner, true));
            }

            return result;
        } catch(IndexOutOfBoundsException | NoSuchElementException e) {
            throw new StorageException("Message list probably have been cleared", e);
        }
    }

    public static MessageRow convert(FileMessage message, Interner<String> interner, boolean hex) {
        MessageRow row = new MessageRow();

        row.setID(interner.intern(String.valueOf(message.getID())));
        row.setMsgName(interner.intern(message.getName()));
        row.setMsgNamespace(interner.intern(message.getNamespace()));
        row.setTimestamp(interner.intern(MessageRow.TIMESTAMP_FORMAT.get().format(message.getTimestamp())));
        row.setFrom(interner.intern(message.getFrom()));
        row.setTo(interner.intern(message.getTo()));
        row.setJson(interner.intern(message.getJsonMessage()));
        row.setContent(interner.intern(message.getHumanMessage()));
        row.setMetaDataID(interner.intern(String.valueOf(message.getMetaDataID())));
        row.setRejectReason(interner.intern(message.getRejectReason()));

        if(message.getRawMessage() != null) {
            if(hex) {
                HexDumper dumper = new HexDumper(message.getRawMessage());
                row.setRawMessage(interner.intern(dumper.getHexdump()));
                row.setPrintableMessage(interner.intern(dumper.getPrintableString()));
            } else {
                row.setRawMessage(interner.intern(new String(message.getRawMessage())));
            }
        } else {
            row.setRawMessage(interner.intern("null"));
        }

        return row;
    }

    @Override
    public void removeMessages(Instant olderThan) {
        long epochMillis = olderThan.toEpochMilli();
        int toIndex = -1;

        for(int i = 0; i < messages.size(); i++) {
            FileMessage message = messages.get(i);

            if(message.getLastModified() < epochMillis || message.getTimestamp().getTime() < epochMillis) {
                toIndex++;
                continue;
            }

            break;
        }

        messages.subList(0, toIndex + 1).clear();
    }

    @Override
    public void removeMessages(String serviceID) {
        messages.removeIf(message -> message.getServiceID().equals(serviceID));
    }

    @Override
    public void clear() {
        messages.clear();
    }
}
