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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.util.SendMessageFailedException;
import com.exactpro.sf.services.ISession;

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
        Map<ISession, Exception> sentErrors = new HashMap<>();
        Collection<ISession> connectedSessions = application.getSessions();
        if (connectedSessions.isEmpty()) {
            throw new SendMessageFailedException("No sessions to send message: " + message);
        }

        for (ISession session : connectedSessions) {
            try {
                session.send(message);
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
}
