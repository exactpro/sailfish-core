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
package com.exactpro.sf.services.netty.handlers;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.services.ServiceInfo;
import com.exactpro.sf.storage.IMessageStorage;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

public class MulticastMessagePersisterHandler extends MessagePersisterHandler {
    private final String serviceName;
    private final String remoteAddress;

    public MulticastMessagePersisterHandler(String serviceName, String remoteAddress, IMessageStorage storage, ServiceInfo serviceInfo) {
        super(storage, serviceInfo);
        this.serviceName = serviceName;
        this.remoteAddress = remoteAddress;
    }

    @Override
    public void channelRead(ChannelHandlerContext context, Object msg) throws Exception {
        if (msg instanceof IMessage) {
            ((IMessage)msg).getMetaData().setFromService(remoteAddress);
            ((IMessage)msg).getMetaData().setToService(serviceName);
        }
        super.channelRead(context, msg);
    }

    @Override
    public void write(ChannelHandlerContext context, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof IMessage) {
            ((IMessage)msg).getMetaData().setFromService(serviceName);
            ((IMessage)msg).getMetaData().setToService(remoteAddress);
        }
    }
}