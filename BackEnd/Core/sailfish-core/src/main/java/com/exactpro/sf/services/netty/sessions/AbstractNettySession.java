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

import java.net.SocketAddress;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.configuration.ILoggingConfigurator;
import com.exactpro.sf.services.ISession;
import com.exactpro.sf.services.ServiceException;
import com.exactpro.sf.services.netty.AbstractNettyService;

import io.netty.channel.Channel;

public abstract class AbstractNettySession implements ISession {
    
    protected final Logger logger = LoggerFactory.getLogger(ILoggingConfigurator.getLoggerName(this));
    
    @NotNull
    protected final AbstractNettyService service;

    protected final long sendMessageTimeout;

    protected Channel channel;
    
    private final ReadWriteLock channelLock = new ReentrantReadWriteLock();
    
    //TODO pass service settings and exception handler instead service
    public AbstractNettySession(@NotNull AbstractNettyService service, @NotNull Channel channel) {
        this.service = Objects.requireNonNull(service, "Service must not be null");
        this.channel = Objects.requireNonNull(channel, "Channel must not be null");
        this.sendMessageTimeout = service.getSettings().getSendMessageTimeout();
    }
    
    @Override
    public String getName() {
        return service.getName();
    }
    
    @Override
    public IMessage sendDirty(Object message) throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    public void onExceptionCaught(Throwable cause) {
        logger.error("Exception caught in netty's pipeline", cause);
        service.onExceptionCaught(channel, cause);
    }
    
    @Override
    public void close() {
        try {
            channelLock.writeLock().lock();
            if (channel.isActive() || channel.isOpen()) {
                disposeChannel(channel);
            }
        } finally {
            channelLock.writeLock().unlock();
        }
    }
    
    private void disposeChannel(Channel channel) {
        if (channel.isOpen() && !channel.close().awaitUninterruptibly(5, TimeUnit.SECONDS)) {
            throw new ServiceException("Channel '" + getName() + "' has not been closed for 5 seconds");
        }
    }
    
    @Override
    public boolean isClosed() {
        try {
            channelLock.readLock().lock();
            return !(channel.isActive() || channel.isOpen());
        } finally {
            channelLock.readLock().unlock();
        }
    }
    
    @Override
    public boolean isLoggedOn() {
        throw new UnsupportedOperationException();
    }
    
    public AbstractNettyService getService() {
        return service;
    }
    
    public void withWriteLock(BiConsumer<AbstractNettySession, Channel> consumer) {
        try {
            channelLock.writeLock().lock();
            consumer.accept(this, channel);
        } finally {
            channelLock.writeLock().unlock();
        }
    }
    
    public void withReadLock(BiConsumer<AbstractNettySession, Channel> consumer) {
        try {
            channelLock.readLock().lock();
            consumer.accept(this, channel);
        } finally {
            channelLock.readLock().unlock();
        }
    }
    
    @Nullable
    public SocketAddress localAddress() {
        return channel.localAddress();
    }
    
    @Nullable
    public SocketAddress remoteAddress() {
        return channel.remoteAddress();
    }
}