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

import org.apache.commons.lang3.ClassUtils;
import org.jetbrains.annotations.NotNull;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.common.util.SendMessageFailedException;
import com.exactpro.sf.services.netty.AbstractNettyService;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

public class NettyClientSession extends AbstractNettySession {
    
    public NettyClientSession(@NotNull AbstractNettyService service, @NotNull Channel channel) {
        super(service, channel);
    }

    @Override
    public IMessage send(Object message) throws InterruptedException {
        return send(message, sendMessageTimeout);
    }

    @Override
    public IMessage send(Object message, long timeout) throws InterruptedException {
        if (!(message instanceof IMessage)) {
            throw new EPSCommonException("Illegal type of Message: " + ClassUtils.getName(message));
        }
        if (timeout < 1) {
            throw new EPSCommonException("Illegal timeout value: " + timeout);
        }
        
        IMessage msg = (IMessage)message;
        ChannelFuture future = channel.writeAndFlush(msg)
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
        if (future.cause() != null) {
            throw new EPSCommonException("Message sent failed. Session: " + this, future.cause());
        }
        
        if (!isSendSuccess) {
            throw new SendMessageFailedException(
                    "Message wasn't send during 1 second." + errorMsg + " Session: " + this);
        }
        return msg;
    }
    
    @Override
    public String toString() {
        return "NettyClientSession{" +
                "Service=" + getName() +
                ", Remote address=" + channel.remoteAddress() +
                ", ChannelId=" + channel.id() +
                ", Send message timeout=" + sendMessageTimeout +
                '}';
    }
    
}