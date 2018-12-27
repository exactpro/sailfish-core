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
package com.exactpro.sf.services.mina;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.common.util.SendMessageFailedException;
import com.exactpro.sf.configuration.ILoggingConfigurator;
import com.exactpro.sf.services.ISession;
import com.exactpro.sf.services.ServiceException;

public class MINASession implements ISession {
    protected static final long DEFAULT_TIMEOUT = 1000L;

    protected final Logger logger = LoggerFactory.getLogger(getClass().getName() + "@" + Integer.toHexString(hashCode()));

	protected final IoSession session;
	protected final ServiceName serviceName;
	protected final ILoggingConfigurator configurator;

    protected volatile boolean loggedOn;

    public MINASession(ServiceName serviceName, IoSession session, ILoggingConfigurator configurator) {
        this.serviceName = Objects.requireNonNull(serviceName, "serviceName cannot be null");
        this.session = Objects.requireNonNull(session, "session cannot be null");
        this.configurator = Objects.requireNonNull(configurator, "configurator cannot be null");

        configurator.createIndividualAppender(logger.getName(), serviceName);
        logger.debug("Session created: {}", this);
	}

    @Override
	public String getName() {
        return this.serviceName.toString();
	}

    @Override
    public IMessage send(Object message) throws InterruptedException {
        return send(message, DEFAULT_TIMEOUT);
    }

    protected Object prepareMessage(Object message) {
        return message;
    }

    public IMessage send(Object message, long timeout) throws InterruptedException {
        if(!isConnected()) {
            throw new SendMessageFailedException("Session is not connected: " + this);
        }

        WriteFuture future = session.write(prepareMessage(message));

        if(future.await(timeout)) {
            if(!future.isDone()) {
                throw new SendMessageFailedException("Send operation is not done. Session: " + this);
            }

            if(!future.isWritten()) {
                throw new SendMessageFailedException("Write operation is not done. Session: " + this);
            }
        } else {
            throw new SendMessageFailedException("Send operation is not completed. Session: " + this);
        }

        if(future.getException() != null) {
            throw new SendMessageFailedException("Message send failed. Session: " + this, future.getException());
        }

        if(message instanceof IMessage) {
            return (IMessage)message;
        }

        return null;
    }

    @Override
    public IMessage sendDirty(Object message) throws InterruptedException {
        throw new UnsupportedOperationException("Dirty send is not supported");
    }

    @Override
    public void close() {
        logger.debug("Closing session: {}", this);

        session.close(true).addListener(future -> {
            logger.debug("Session closed: {}", this);
            configurator.destroyIndividualAppender(logger.getName(), serviceName);
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
