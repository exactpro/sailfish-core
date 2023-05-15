/******************************************************************************
 * Copyright 2009-2023 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.services.http.oauth;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.messages.oauth.HttpResponseMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TokenResponseChannelHandler extends ChannelDuplexHandler {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);
    private final Lock writeLock = readWriteLock.writeLock();
    private final Lock readLock = readWriteLock.readLock();
    private final OAuthHttpMessageConverter messageConverter;
    private AccessToken accessToken = null;
    private AccessTokenError accessTokenError = null;
    private Throwable exception = null;

    public TokenResponseChannelHandler(OAuthHttpMessageConverter messageConverter) {
        this.messageConverter = messageConverter;
    }

    @Override
    public void write(ChannelHandlerContext context, Object msg, ChannelPromise promise) throws Exception {
        context.write(msg, promise);
        if(msg instanceof FullHttpRequest) {
            IMessage iMessage = messageConverter.fullHttpRequestToMessage((FullHttpRequest) msg)
                    .getMessage();
            context.write(iMessage, promise);
            context.flush();
        }
    }

    @Override
    public void channelRead(@NotNull ChannelHandlerContext context, @NotNull Object msg) throws Exception {
        if (msg instanceof IMessage) {
            HttpResponseMessage response = new HttpResponseMessage((IMessage) msg);
            writeLock.lock();
            try {
                if(response.getStatus() == HttpResponseStatus.OK.code()) {
                    accessToken = OBJECT_MAPPER.readValue(response.getBody(), AccessToken.class);
                } else {
                    accessTokenError = OBJECT_MAPPER.readValue(response.getBody(), AccessTokenError.class);
                }
            } finally {
                writeLock.unlock();
            }
        }
        super.channelRead(context, msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) throws Exception {
        writeLock.lock();
        try {
            exception = cause;
        } finally {
            writeLock.unlock();
        }
    }

    public AccessToken getAccessToken() {
        readLock.lock();
        try {
            return accessToken;
        } finally {
            readLock.unlock();
        }
    }

    public AccessTokenError getAccessTokenError() {
        readLock.lock();
        try {
            return accessTokenError;
        } finally {
            readLock.unlock();
        }
    }

    public Throwable getException() {
        readLock.lock();
        try {
            return exception;
        } finally {
            readLock.unlock();
        }
    }
}
