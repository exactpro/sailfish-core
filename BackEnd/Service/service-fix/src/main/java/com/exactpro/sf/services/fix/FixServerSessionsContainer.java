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

package com.exactpro.sf.services.fix;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.util.SendMessageFailedException;
import com.exactpro.sf.services.ISession;
import org.apache.commons.lang3.StringUtils;
import org.quickfixj.CharsetSupport;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;

public class FixServerSessionsContainer implements ISession {

    private final IQuickfixApplication application;

    public FixServerSessionsContainer(IQuickfixApplication application) {
        this.application = application;
    }

    @Override
    public String getName() {
        return "FixServerSessions";
    }

    @Override
    public IMessage send(Object message) throws InterruptedException {
        tryActionToSessions(() -> "No sessions to send message: " + message,
                session -> session.send(message));
        return message instanceof IMessage ? (IMessage)message : null;
    }

    @Override
    public IMessage sendDirty(Object message) throws InterruptedException {
        for(ISession session : application.getSessions()) {
            session.sendDirty(message);
        }
        return message instanceof IMessage ? (IMessage)message : null;
    }

    @Override
    public void sendRaw(byte[] rawData) throws InterruptedException {
        tryActionToSessions(() -> "No sessions to send raw message: " + CharsetSupport.getCharsetInstance().decode(ByteBuffer.wrap(rawData)),
                session -> session.sendRaw(rawData));
    }

    @Override
    public void close() {
        for(ISession session : application.getSessions()) {
            session.close();
        }
    }

    @Override
    public boolean isClosed() {
        for(ISession session : application.getSessions()) {
            if (!session.isClosed()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isLoggedOn() {
        throw new UnsupportedOperationException();
    }

    public void addExpectedSenderNum(int seq) {
        for (ISession session : application.getSessions()) {
            ((FIXSession)session).addExpectedSenderNum(seq);
        }
    }

    private void tryActionToSessions(Supplier<String> onEmptyMessage, SessionAction actionForSession) {
        Collection<ISession> connectedSessions = application.getSessions();

        if (connectedSessions.isEmpty()) {
            throw new SendMessageFailedException(onEmptyMessage.get());
        }

        Map<ISession, Exception> sentErrors = new HashMap<>();
        for (ISession session : connectedSessions) {
            try {
                actionForSession.accept(session);
            } catch (Exception e) {
                sentErrors.put(session, e);
            }
        }

        if (!sentErrors.isEmpty()) {
            StringBuilder errorBuilder = new StringBuilder("Not all message successfully sent: ");
            for (Entry<ISession, Exception> error : sentErrors.entrySet()) {
                Exception ex = error.getValue();
                String errorMsg = ex.getCause() == null ? ex.getMessage() : String.format("%s; cause: %s", ex.getMessage(), ex.getCause());
                errorBuilder.append(StringUtils.LF).append(String.format("%s : [%s]", error.getKey(), errorMsg));
            }
            SendMessageFailedException exception = new SendMessageFailedException(errorBuilder.toString());
            sentErrors.values().forEach(exception::addSuppressed);
            throw exception;
        }
    }

    private interface SessionAction {
        void accept(ISession session) throws Exception;
    }
}
