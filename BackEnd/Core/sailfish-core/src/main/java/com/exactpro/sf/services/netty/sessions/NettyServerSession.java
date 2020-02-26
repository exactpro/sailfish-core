/******************************************************************************
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
package com.exactpro.sf.services.netty.sessions;

import java.util.Collection;
import java.util.HashSet;

import org.jetbrains.annotations.NotNull;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.common.util.SendMessageFailedException;
import com.exactpro.sf.configuration.ILoggingConfigurator;
import com.exactpro.sf.services.netty.AbstractNettyServer;
import com.exactpro.sf.services.netty.AbstractNettyService;

import io.netty.channel.Channel;

public class NettyServerSession extends AbstractNettySession {
    
    private final AbstractNettyServer server = (AbstractNettyServer)service;
    
    public NettyServerSession(@NotNull AbstractNettyService service, @NotNull ILoggingConfigurator logConfigurator, @NotNull Channel channel) {
        super(service, logConfigurator, channel);
    }
    
    @Override
    public IMessage send(Object message) throws InterruptedException {
        if (!(message instanceof IMessage)) {
            throw new EPSCommonException("Illegal type of Message");
        }
        IMessage msg = (IMessage)message;
        Iterable<NettyClientSession> sendingSessions = getSendingSessions(msg);
        Collection<AbstractNettySession> errorSending = new HashSet<>();
        for (NettyClientSession session : sendingSessions) {
            try {
                session.send(msg);
            } catch (RuntimeException e) {
                errorSending.add(session);
            }
        }
        if (!errorSending.isEmpty()) {
            StringBuilder errors = new StringBuilder("For sessions: ");
            for (AbstractNettySession session : errorSending) {
                errors.append(System.lineSeparator()).append(session);
            }
            throw new SendMessageFailedException("Message wasn't send. " + errors.toString().trim());
        }
        return msg;
    }
    
    protected Iterable<NettyClientSession> getSendingSessions(IMessage msg) {//TODO change to supplier in constructor
        return server.getActiveSessionMap().values();
    }
    
    @Override
    public String toString() {
        return "NettyServerSession{" +
                "Service=" + getName() +
                ", Remote address=" + channel.remoteAddress() +
                ", ChannelId=" + channel.id() +
                '}';
    }
}