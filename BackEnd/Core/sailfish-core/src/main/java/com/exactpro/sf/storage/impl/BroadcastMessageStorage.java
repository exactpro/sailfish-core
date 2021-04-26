/*******************************************************************************
 * Copyright 2009-2021 Exactpro (Exactpro Systems Limited)
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import com.exactpro.sf.common.messages.IHumanMessage;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.storage.MessageFilter;
import com.exactpro.sf.storage.MessageRow;
import com.exactpro.sf.storage.ScriptRun;

public class BroadcastMessageStorage extends AbstractMessageStorage {

    private static final String ERROR_MESSAGE_ON_STORE = "Can`t store message to all storage";
    private static final String ERROR_MESSAGE_ON_CLEAR = "Can`t clear message from all storage";
    private static final String ERROR_MESSAGE_ON_DISPOSE = "Can`t dispose all storages";

    private final AbstractMessageStorage primaryStorage;
    private final List<AbstractMessageStorage> writableStorages;

    public BroadcastMessageStorage(AbstractMessageStorage primaryStorage, List<AbstractMessageStorage> secondaryStorages) {
        super(primaryStorage.dictionaryManager);
        this.primaryStorage = Objects.requireNonNull(primaryStorage, "Primary storage can`t be null");
        writableStorages = new ArrayList<>();
        writableStorages.add(this.primaryStorage);
        writableStorages.addAll(Objects.requireNonNull(secondaryStorages, "Secondary storages can`t be null"));
    }

    public BroadcastMessageStorage(AbstractMessageStorage primaryStorage, AbstractMessageStorage... secondaryStorages) {
        this(primaryStorage, secondaryStorages.length > 0 ? Arrays.asList(secondaryStorages) : Collections.emptyList());
    }

    @Override
    public ScriptRun openScriptRun(String name, String description) {
        return primaryStorage.openScriptRun(name, description);
    }

    @Override
    public void closeScriptRun(ScriptRun scriptRun) {
        primaryStorage.closeScriptRun(scriptRun);
    }

    @Override
    protected void storeMessage(IMessage message, IHumanMessage humanMessage, String jsonMessage) {
        execute(messageStorage -> messageStorage.storeMessage(message, humanMessage, jsonMessage), ERROR_MESSAGE_ON_STORE);
    }

    @Override
    public Iterable<MessageRow> getMessages(int count, MessageFilter filter) {
        return primaryStorage.getMessages(count, filter);
    }

    @Override
    public Iterable<MessageRow> getMessages(int offset, int count, String where) {
        return primaryStorage.getMessages(offset, count, where);
    }

    @Override
    public void removeMessages(Instant olderThan) {
        execute(messageStorage -> messageStorage.removeMessages(olderThan), "Can`t remove message older than '" + olderThan.toString() + "' from all storages");
    }

    @Override
    public void removeMessages(String serviceID) {
        execute(messageStorage -> messageStorage.removeMessages(serviceID), "Can`t remove message of '" + serviceID + "' service  from all storages");
    }

    @Override
    public void clear() {
        execute(AbstractMessageStorage::clear, ERROR_MESSAGE_ON_CLEAR);
    }

    @Override
    public void dispose() {
        execute(AbstractMessageStorage::dispose, ERROR_MESSAGE_ON_DISPOSE);
    }

    private void execute(Consumer<AbstractMessageStorage> action, String errorMessage) {
        EPSCommonException exception = null;
        for (AbstractMessageStorage storage : writableStorages) {
            try {
                action.accept(storage);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                if (exception == null) {
                    exception = new EPSCommonException(errorMessage);
                }
                exception.addSuppressed(e);
            }
        }

        if (exception != null && exception.getSuppressed().length > 0) {
            throw exception;
        }
    }
}
