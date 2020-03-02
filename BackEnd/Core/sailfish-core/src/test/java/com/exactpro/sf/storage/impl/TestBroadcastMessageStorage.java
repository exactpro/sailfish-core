/*
 *  Copyright 2009-2020 Exactpro (Exactpro Systems Limited)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.exactpro.sf.storage.impl;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import java.util.ArrayList;
import java.util.function.Function;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Before;
import org.junit.Test;

import com.exactpro.sf.common.messages.IHumanMessage;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.configuration.IDictionaryManager;
import com.exactpro.sf.storage.MessageFilter;
import com.exactpro.sf.storage.MessageRow;
import com.exactpro.sf.storage.ScriptRun;
import com.exactpro.sf.util.AbstractTest;

public class TestBroadcastMessageStorage extends AbstractTest {

    private BroadcastMessageStorage broadcastMessageStorage;
    private FakeMessageStorage primaryMessageStorage;
    private FakeMessageStorage secondaryMessageStorage;

    @SuppressWarnings("unchecked")
    @Before
    public void init() {
        primaryMessageStorage = new FakeMessageStorage(serviceContext.getDictionaryManager(), mock(ArrayList.class), mock(ScriptRun.class));
        secondaryMessageStorage = new FakeMessageStorage(serviceContext.getDictionaryManager(), mock(ArrayList.class), mock(ScriptRun.class));

        broadcastMessageStorage = new BroadcastMessageStorage(primaryMessageStorage, secondaryMessageStorage);
    }

    @Test
    public void testOpenScriptRun() {
        ScriptRun scriptRun = broadcastMessageStorage.openScriptRun("name", "description");
        assertEquals(primaryMessageStorage.getScriptRun(), scriptRun);
        assertTrue(primaryMessageStorage.isOpenScriptRun());
        assertNotEquals(secondaryMessageStorage.getScriptRun(), scriptRun);
        assertFalse(secondaryMessageStorage.isOpenScriptRun());
    }

    @Test
    public void testCloseScriptRun() {
        broadcastMessageStorage.closeScriptRun(new ScriptRun());
        assertTrue(primaryMessageStorage.isCloseScriptRun());
        assertFalse(secondaryMessageStorage.isCloseScriptRun());
    }

    @Test
    public void testStoreMessage() {
        broadcastMessageStorage.storeMessage(mock(IMessage.class), mock(IHumanMessage.class), "JSON");
        verifyExecution(FakeMessageStorage::isStoreMessageExecuted);
    }

    @Test
    public void testGetMessagesFilter() {
        Iterable<MessageRow> messages = broadcastMessageStorage.getMessages(0, mock(MessageFilter.class));
        assertEquals(primaryMessageStorage.getMessages(), messages);
        assertTrue(primaryMessageStorage.isGetMessagesFilter());
        assertNotEquals(secondaryMessageStorage.getMessages(), messages);
        assertFalse(secondaryMessageStorage.isGetMessagesFilter());
    }

    @Test
    public void testGetMessagesWhere() {
        Iterable<MessageRow> messages = broadcastMessageStorage.getMessages(0, 0, "were");
        assertEquals(primaryMessageStorage.getMessages(), messages);
        assertTrue(primaryMessageStorage.isGetMessagesWhere());
        assertNotEquals(secondaryMessageStorage.getMessages(), messages);
        assertFalse(secondaryMessageStorage.isGetMessagesWhere());
    }

    @Test
    public void testRemoveMessagesOlderThan() {
        broadcastMessageStorage.removeMessages(Instant.now());
        verifyExecution(FakeMessageStorage::isRemoveMessagesOlderThan);
    }

    @Test
    public void testRemoveMessagesServiceID() {
        broadcastMessageStorage.removeMessages("");
        verifyExecution(FakeMessageStorage::isRemoveMessagesServiceID);
    }

    @Test
    public void testClear() {
        broadcastMessageStorage.clear();
        verifyExecution(FakeMessageStorage::isClear);
    }

    @Test
    public void testDispose() {
        broadcastMessageStorage.dispose();
        verifyExecution(FakeMessageStorage::isDispose);
    }

    private void verifyExecution(Function<FakeMessageStorage, Boolean> isExecutedFunction) {
        for (FakeMessageStorage messageStorage :
                ArrayUtils.toArray(primaryMessageStorage, secondaryMessageStorage)) {
            assertTrue(isExecutedFunction.apply(messageStorage));
        }
    }

    @SuppressWarnings("deprecation")
    private static class FakeMessageStorage extends AbstractMessageStorage {

        private boolean storeMessage;
        private boolean removeMessagesServiceID;
        private boolean removeMessagesOlderThan;
        private boolean clear;
        private boolean dispose;
        private boolean getMessagesWhere;
        private boolean openScriptRun;
        private boolean closeScriptRun;
        private boolean getMessagesFilter;

        private final Iterable<MessageRow> messages;
        private final ScriptRun scriptRun;

        public FakeMessageStorage(IDictionaryManager dictionaryManager, Iterable<MessageRow> messages, ScriptRun scriptRun) {
            super(dictionaryManager);
            this.messages = messages;
            this.scriptRun = scriptRun;
        }

        @Override
        protected void storeMessage(IMessage message, IHumanMessage humanMessage, String jsonMessage) {
            storeMessage = true;
        }

        @Override
        public ScriptRun openScriptRun(String name, String description) {
            openScriptRun = true;
            return scriptRun;
        }

        @Override
        public void closeScriptRun(ScriptRun scriptRun) {
            closeScriptRun = true;
        }

        @Override
        public Iterable<MessageRow> getMessages(int count, MessageFilter filter) {
            getMessagesFilter = true;
            return messages;
        }

        @Override
        public Iterable<MessageRow> getMessages(int offset, int count, String where) {
            getMessagesWhere = true;
            return messages;
        }

        @Override
        public void removeMessages(Instant olderThan) {
            removeMessagesOlderThan = true;
        }

        @Override
        public void removeMessages(String serviceID) {
            removeMessagesServiceID = true;
        }

        @Override
        public void clear() {
            clear = true;
        }

        @Override
        public void dispose() {
            dispose = true;
        }

        public boolean isStoreMessageExecuted() {
            return storeMessage;
        }

        public boolean isRemoveMessagesServiceID() {
            return removeMessagesServiceID;
        }

        public boolean isRemoveMessagesOlderThan() {
            return removeMessagesOlderThan;
        }

        public boolean isClear() {
            return clear;
        }

        public boolean isDispose() {
            return dispose;
        }

        public boolean isGetMessagesWhere() {
            return getMessagesWhere;
        }

        public boolean isOpenScriptRun() {
            return openScriptRun;
        }

        public boolean isCloseScriptRun() {
            return closeScriptRun;
        }

        public boolean isGetMessagesFilter() {
            return getMessagesFilter;
        }

        public Iterable<MessageRow> getMessages() {
            return messages;
        }

        public ScriptRun getScriptRun() {
            return scriptRun;
        }
    }
}