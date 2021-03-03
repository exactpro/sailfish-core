/*
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
 */
package com.exactpro.sf.services.mina;

import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMetadata;
import com.exactpro.sf.common.messages.MetadataExtensions;
import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.common.util.SendMessageFailedException;
import com.exactpro.sf.configuration.ILoggingConfigurator;
import com.exactpro.sf.services.ISession;
import com.exactpro.sf.services.ServiceException;

public class MINASession implements ISession {
    protected final Logger logger = LoggerFactory.getLogger(ILoggingConfigurator.getLoggerName(this));

	protected final IoSession session;
	protected final ServiceName serviceName;
    protected final long sendMessageTimeout;

    protected volatile boolean loggedOn;

    public MINASession(ServiceName serviceName, IoSession session, long sendMessageTimeout) {
        this.serviceName = Objects.requireNonNull(serviceName, "serviceName cannot be null");
        this.session = Objects.requireNonNull(session, "session cannot be null");
        this.sendMessageTimeout = sendMessageTimeout;
	}

    @Override
	public String getName() {
        return serviceName.toString();
	}

    @Override
    public IMessage send(Object message) throws InterruptedException {
        return send(message, sendMessageTimeout);
    }

    protected Object prepareMessage(Object message) {
        return message;
    }

    public IMessage send(Object message, long timeout) throws InterruptedException {
        if(!isConnected()) {
            throw new SendMessageFailedException("Session isn't connected: " + this);
        }
        if (timeout < 1) {
            throw new EPSCommonException("Illegal timeout value: " + timeout);
        }

        WriteFuture future = session.write(prepareMessage(message));

        if(future.await(timeout)) {
            if(!future.isDone()) {
                throw new SendMessageFailedException("Send operation isn't done. Session: " + this, future.getException());
            }

            if(!future.isWritten()) {
                throw new SendMessageFailedException("Write operation isn't done. Session: " + this, future.getException());
            }
        } else {
            throw new SendMessageFailedException("Send operation isn't completed. Session: " + this, future.getException());
        }

        if(future.getException() != null) {
            throw new SendMessageFailedException("Message send failed. Session: " + this, future.getException());
        }

        return message instanceof IMessage ? (IMessage)message : null;
    }

    @Override
    public IMessage sendDirty(Object message) throws InterruptedException {
        throw new UnsupportedOperationException("Dirty send is not supported");
    }

    @Override
    public void sendRaw(byte[] rawData, IMetadata extraMetadata) throws InterruptedException {
        IoFilter codecFilter = session.getFilterChain().get(AbstractMINAService.CODEC_FILTER_NAME);
        if (codecFilter == null) {
            throw new IllegalStateException("Cannot get filter '" + AbstractMINAService.CODEC_FILTER_NAME + "' from session " + session);
        }
        try {
            MessageNextFilter nextFilter = new MessageNextFilter();
            codecFilter.messageReceived(nextFilter, session, IoBuffer.wrap(rawData));
            if (!nextFilter.getExceptions().isEmpty()) {
                SendMessageFailedException exception = new SendMessageFailedException("Exception accurate during decoding");
                nextFilter.getExceptions().forEach(exception::addSuppressed);
                throw exception;
            }
            boolean sent = false;
            for (IMessage result : nextFilter.getResults()) {
                if (filterResultFromSendRaw(result)) {
                    continue;
                }
                removeSessionFields(result);
                MetadataExtensions.merge(result.getMetaData(), extraMetadata);
                send(result);
                sent = true;
            }
            if (!sent) {
                throw new SendMessageFailedException("No messages were sent to the system. Result size: "
                        + nextFilter.getResults().size()
                        + ". Messages in the result: " + nextFilter.getResults().stream()
                        .map(IMessage::getName)
                        .collect(Collectors.joining(", "))
                );
            }
        } catch (ProtocolDecoderException e) {
            throw new SendMessageFailedException("Cannot decode raw bytes to messages", e);
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                throw (InterruptedException)e;
            }
            throw new SendMessageFailedException("Cannot send message", e);
        }
    }

    protected boolean filterResultFromSendRaw(IMessage result) {
        // all messages by default are allowed for sending
        return false;
    }

    protected void removeSessionFields(IMessage result) {
        // do nothing by default
    }

    @Override
    public void close() {
        logger.debug("Closing session: {}", this);

        session.close(true).addListener(future -> {
            logger.debug("Session closed: {}", this);
        });
    }

    @Override
    public boolean isClosed() {
        return !isConnected();
    }

    @Override
    public boolean isLoggedOn() {
        return loggedOn;
    }

    public void setLoggedOn(boolean loggedOn) {
        if(!isConnected()) {
            throw new ServiceException("Session is not connected: " + this);
        }

        this.loggedOn = loggedOn;
    }

    public boolean isConnected() {
        return session.isConnected();
    }

    public boolean waitClose(long timeout) throws InterruptedException {
        long waitUntil = System.currentTimeMillis() + timeout;

        while(isConnected() && waitUntil > System.currentTimeMillis()) {
            Thread.sleep(1);
        }

        return isClosed();
    }

    @Override
    public String toString() {
        ToStringBuilder builder = new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE);

        builder.append("serviceName", serviceName);
        builder.append("session", session);
        builder.append("loggedOn", loggedOn);

        return builder.toString();
    }
}
