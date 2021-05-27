/******************************************************************************
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
package com.exactpro.sf.services.itch.soup;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.services.mina.AbstractMINATCPServer;
import com.exactpro.sf.services.mina.MINAServerSession;

public class SOUPTcpServerSession extends MINAServerSession {

    private volatile int sequenceNumber;
    private final Map<Integer, Object> messages;

    public SOUPTcpServerSession(AbstractMINATCPServer server) {
        super(server);
        messages = new ConcurrentHashMap<>();
    }

    public synchronized int incrementAndGetSequenceNumber() {
        return ++sequenceNumber;
    }

    public Object getMessageWithSequenceNumber(int sequenceNumber) {
        return messages.get(sequenceNumber);
    }

    private void putMessageWithSequenceNumber(Object message, int sequenceNumber) {
        messages.put(sequenceNumber, message);
    }

    public IMessage sendSequenceMessage(Object message) throws InterruptedException {
        putMessageWithSequenceNumber(message, incrementAndGetSequenceNumber());
        return send(message);
    }

    public IMessage sendSequenceMessage(Object message, String sequenceNumberField) throws InterruptedException {
        if (message instanceof IMessage) {
            int tmpSequenceNumber = incrementAndGetSequenceNumber();
            ((IMessage)message).addField(sequenceNumberField, tmpSequenceNumber);
            putMessageWithSequenceNumber(message, tmpSequenceNumber);
        } else {
            putMessageWithSequenceNumber(message, incrementAndGetSequenceNumber());
        }
        return send(message);
    }
}
