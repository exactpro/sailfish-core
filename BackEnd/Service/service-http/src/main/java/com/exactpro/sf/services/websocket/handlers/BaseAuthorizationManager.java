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
package com.exactpro.sf.services.websocket.handlers;

import java.net.URI;
import java.util.Objects;

import com.exactpro.sf.services.http.HTTPClientSettings;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;

/**
 * @author oleg.smirnov
 *
 */
public class BaseAuthorizationManager extends ChannelDuplexHandler {

    protected final IHandshaker handshaker;
    protected final HTTPClientSettings settings;

    /**
     *
     */
    public BaseAuthorizationManager(IHandshaker handshaker, HTTPClientSettings settings) {
        this.handshaker = Objects.requireNonNull(handshaker, "'Handshaker' parameter");
        this.settings = Objects.requireNonNull(settings, "'Settings' parameter");
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * io.netty.channel.ChannelInboundHandlerAdapter#channelActive(io.netty.channel.
     * ChannelHandlerContext)
     */
    @Override
    public void channelActive(ChannelHandlerContext handlerContext) throws Exception {
        URI uri = new URI(settings.getURI());
        handshaker.startHandshake(uri, WebSocketVersion.V13, handshakeHttpHeaders(), handlerContext.channel());
        handlerContext.fireChannelActive();
    }

    protected HttpHeaders handshakeHttpHeaders(){
        return new DefaultHttpHeaders();
    }

}
