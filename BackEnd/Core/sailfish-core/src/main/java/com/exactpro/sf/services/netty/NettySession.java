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
package com.exactpro.sf.services.netty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.common.util.SendMessageFailedException;
import com.exactpro.sf.configuration.ILoggingConfigurator;
import com.exactpro.sf.services.ISession;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

public class NettySession implements ISession {

	protected final Logger logger = LoggerFactory
			.getLogger(this.getClass().getName() + "@" + Integer.toHexString(hashCode()));
	private final ILoggingConfigurator logConfigurator;
	private final NettyClientService client;

	public NettySession(final NettyClientService client, ILoggingConfigurator logConfigurator) {
		super();
		if (client == null) {
			throw new NullPointerException();
		}
		this.client = client;

		this.logConfigurator = logConfigurator;
		logConfigurator.createIndividualAppender(this.getClass().getName() + "@" + Integer.toHexString(hashCode()),
				client.getServiceName());
	}

	@Override
	public String getName() {
		return client.getName();
	}

	@Override
	public IMessage send(Object message) throws InterruptedException {
		if (client.getChannel() == null) {
			throw new EPSCommonException("Channel not ready (channel == null)");
		}
		if (!(message instanceof IMessage)) {
			throw new EPSCommonException("Illegal type of Message");
		}

		IMessage msg = (IMessage) message;
		ChannelFuture future = client.getChannel().writeAndFlush(msg)
				.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
		boolean isSendSuccess = true;
		StringBuilder errorMsg = new StringBuilder("Cause: ");

		if (future.await(1000)) {
			if (!future.isDone()) {
				errorMsg.append("Send operation is not done.\n");
				logger.error("Send operation is not done. Session: {}", this);
				isSendSuccess = false;
			}
			if (!future.isSuccess()) {
				errorMsg.append("Write operation was not successful.\n");
				logger.error("Write operation was not successful. Session: {}", this);
				isSendSuccess = false;
			}
		} else {
			errorMsg.append("Send operation is not completed.\n");
			logger.error("Send operation is not completed. Session: {}", this);
			isSendSuccess = false;
		}
		if (future.cause() != null)
			throw new EPSCommonException("Message sent failed. Session: " + this, future.cause());

		if (!isSendSuccess) {
			throw new SendMessageFailedException(
					"Message wasn't send during 1 second." + errorMsg.toString() + " Session: " + this);
		}

		return msg;
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
		logConfigurator.destroyIndividualAppender(this.getClass().getName() + "@" + Integer.toHexString(hashCode()),
				client.getServiceName());
	}

	@Override
	public boolean isClosed() {
		if (client.getChannel() == null)
			return true;
		return !(client.getChannel().isActive() || client.getChannel().isOpen());
	}

	@Override
	public boolean isLoggedOn() {
		throw new UnsupportedOperationException();
	}

	public NettyClientService getClient() {
		return client;
	}

}
