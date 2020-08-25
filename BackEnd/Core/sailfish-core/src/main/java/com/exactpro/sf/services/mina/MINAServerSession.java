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

package com.exactpro.sf.services.mina;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.util.SendMessageFailedException;
import com.exactpro.sf.configuration.ILoggingConfigurator;
import com.exactpro.sf.services.ISession;
import com.exactpro.sf.services.util.CloseSessionException;

public class MINAServerSession implements ISession {

    protected final Logger logger = LoggerFactory.getLogger(ILoggingConfigurator.getLoggerName(this));

    protected final AbstractMINATCPServer server;

    public MINAServerSession(AbstractMINATCPServer server) {
        this.server = Objects.requireNonNull(server, "Server can't be null");
    }

    @Override
    public String getName() {
        return server.getServiceName().toString();
    }

    protected Object prepareMessage(Object message) {
        return message;
    }

    @Override
    public IMessage send(Object message) throws InterruptedException {
        return send(prepareMessage(message), server.getSettings().getSendMessageTimeout());
    }

    public IMessage send(Object message, long timeout) throws InterruptedException {
        List<WriteFuture> futures = new ArrayList<>();
        Set<String> errorSending = new HashSet<>();

        for (IoSession session : server.sessions.keySet()) {
            futures.add(session.write(message));
        }

        long waitUntil = System.currentTimeMillis() + timeout;
        for (WriteFuture future : futures) {
            future.await(waitUntil - System.currentTimeMillis());
            if (!future.isDone() || !future.isWritten()) {
                errorSending.add(future.getSession().toString());
            }
        }

        if (!errorSending.isEmpty()) {
            throw new SendMessageFailedException(String.format("Message wasn't send during %d milliseconds. %s", timeout, String.join(System.lineSeparator(), errorSending)));
        }

        return message instanceof IMessage ? (IMessage) message : null;
    }

    @Override
    public IMessage sendDirty(Object message) {
        throw new UnsupportedOperationException("Dirty send is not supported");
    }

    @Override
    public void close() {
        Set<String> errorClosing = new HashSet<>();

        server.sessions.forEach((ioSession, minaSession) -> {
            try {
                minaSession.close();
            } catch (Exception e) {
                errorClosing.add(ioSession.toString());
            }
        });

        if (!errorClosing.isEmpty()) {
            throw new CloseSessionException("Sessions send error on close." + String.join(System.lineSeparator(), errorClosing));
        }
    }

    public boolean isConnected() {
        return !isClosed();
    }

    @Override
    public boolean isClosed() {
        return server.sessions.isEmpty() || server.sessions.values().stream().allMatch(MINASession::isClosed);
    }

    @Override
    public boolean isLoggedOn() {
        return server.sessions.values().stream().anyMatch(MINASession::isLoggedOn);
    }
}
