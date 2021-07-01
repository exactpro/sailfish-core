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
package com.exactpro.sf.services.netty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.impl.messages.BaseMessage;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMetadata;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.common.util.SendMessageFailedException;
import com.exactpro.sf.services.ISession;
import com.exactpro.sf.services.netty.internal.RawDataHolder;
import com.exactpro.sf.services.netty.sessions.AbstractNettySession;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;


/**
 * @deprecated Please use {@link AbstractNettySession}
 */
@Deprecated
public class NettySession implements ISession {

	protected final Logger logger = LoggerFactory
            .getLogger(getClass().getName() + '@' + Integer.toHexString(hashCode()));
	private final NettyClientService client;

    protected final long sendMessageTimeout;

    public NettySession(NettyClientService client) {
        if(client == null) {
			throw new NullPointerException();
		}
		this.client = client;
        this.sendMessageTimeout = client.getSettings().getSendMessageTimeout();
	}

	@Override
	public String getName() {
		return client.getName();
	}

    @Override
    public IMessage send(Object message) throws InterruptedException {
        return send(message, sendMessageTimeout);
    }
	
	@Override
    public IMessage send(Object message, long timeout) throws InterruptedException {
        checkChannelIsReady();
        if (timeout < 1) {
            throw new EPSCommonException("Illegal timeout value: " + timeout);
        }
        if (message instanceof IMessage) {
            IMessage msg = (IMessage)message;
            realSend(msg, timeout);
            return msg;
        }
        if (message instanceof BaseMessage) {
            BaseMessage baseMessage = (BaseMessage)message;
            IMessage msg = baseMessage.getMessage();
            realSend(baseMessage, timeout);
            return msg;
        }

        throw new EPSCommonException("Illegal type of Message: " + message.getClass().getCanonicalName());
    }

    @Override
    public void sendRaw(byte[] rawData, IMetadata extraMetadata) throws InterruptedException {
        checkChannelIsReady();
        realSend(new RawDataHolder(rawData, extraMetadata), sendMessageTimeout);
    }

    private void checkChannelIsReady() {
        if (client.getChannel() == null) {
            throw new EPSCommonException("Channel not ready (channel == null)");
        }
    }

    private void realSend(Object msg, long timeout) throws InterruptedException {
        ChannelFuture future = client.getChannel().writeAndFlush(msg)
                .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
        boolean isSendSuccess = true;
        StringBuilder errorMsg = new StringBuilder("Cause: ");

        if (future.await(timeout)) {
            if (!future.isDone()) {
                errorMsg.append("Send operation isn't done.\n");
                logger.error("Send operation isn't done. Session: {}", this);
                isSendSuccess = false;
            }
            if (!future.isSuccess()) {
                errorMsg.append("Write operation was not successful.\n");
                logger.error("Write operation was not successful. Session: {}", this);
                isSendSuccess = false;
            }
        } else {
            errorMsg.append("Send operation isn't completed.\n");
            logger.error("Send operation isn't completed. Session: {}", this);
            isSendSuccess = false;
        }
        if(future.cause() != null) {
            throw new EPSCommonException("Message sent failed. Session: " + this, future.cause());
        }

        if (!isSendSuccess) {
            throw new SendMessageFailedException("Message wasn't send during " + timeout + " mls." + errorMsg + " Session: " + this);
        }
    }

    @Override
	public IMessage sendDirty(Object message) throws InterruptedException {
		throw new UnsupportedOperationException();
	}

	public void onExceptionCaught(Throwable cause) {
		logger.error("Exception caught in netty's pipeline", cause);
		client.onExceptionCaught(cause);
	}

	@Override
	public void close() {
		client.stop("Close session", null);
	}

	@Override
	public boolean isClosed() {
        return client.getChannel() == null || !(client.getChannel().isActive() || client.getChannel().isOpen());
    }

	@Override
	public boolean isLoggedOn() {
		throw new UnsupportedOperationException();
	}

	public NettyClientService getClient() {
		return client;
	}
    
    public void onExceptionCaught(Channel channel, Throwable cause) {
        onExceptionCaught(cause);
    }
}
