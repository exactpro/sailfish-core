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
package com.exactpro.sf.services.netty.handlers;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.services.ServiceInfo;
import com.exactpro.sf.storage.IMessageStorage;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

public class MessagePersisterHandler extends ChannelDuplexHandler {

	private final IMessageStorage storage;
	private final ServiceInfo serviceInfo;


	public MessagePersisterHandler(IMessageStorage storage, ServiceInfo serviceInfo) {
		this.storage = storage;
		this.serviceInfo = serviceInfo;
	}

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    	if (msg instanceof IMessage) {
    		persistRecievedMessage((IMessage) msg);
    	}
        ctx.fireChannelRead(msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
    	if (msg instanceof IMessage) {
    		persistRecievedMessage((IMessage) msg);
    	}
        ctx.write(msg, promise);
    }

	protected void persistRecievedMessage(final IMessage message) {
		message.getMetaData().setServiceInfo(serviceInfo);
		storage.storeMessage(message);
	}
}
