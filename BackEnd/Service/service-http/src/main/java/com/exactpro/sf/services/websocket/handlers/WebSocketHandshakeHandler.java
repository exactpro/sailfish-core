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
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.util.EPSCommonException;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;

/**
 * @author sergey.smirnov
 *
 */
public class WebSocketHandshakeHandler extends MessageToMessageDecoder<FullHttpResponse> implements IHandshaker {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketHandshakeHandler.class);

    //TODO check working without sync() promise;
    private volatile WebSocketClientHandshaker handshaker;
    private final int maxFramePayloadLength;

    public WebSocketHandshakeHandler(int maxFramePayloadLength) {
        this.maxFramePayloadLength = Math.max(maxFramePayloadLength, 65536);
    }

    /* (non-Javadoc)
     * @see io.netty.handler.codec.MessageToMessageDecoder#decode(io.netty.channel.ChannelHandlerContext, java.lang.Object, java.util.List)
     */
    @Override
    protected void decode(ChannelHandlerContext handlerContext, FullHttpResponse msg, List<Object> out) throws Exception {
        if (handshaker != null) {
            logger.debug("Await response");
            if (!handshaker.isHandshakeComplete()) {
                try {
                    handshaker.finishHandshake(handlerContext.channel(), msg);
                    logger.info("Websocket client connected");
                } catch (WebSocketHandshakeException e) {
                    logger.error("Websocket client failed to connect", e);
                    throw new EPSCommonException(e);
                }
            } else {
                throw new IllegalStateException("Unexpected FullHttpResponse");
            }
        } else {
            out.add(msg.retain());
        }
    }

    protected WebSocketClientHandshaker createClientHandshaker(URI uri, WebSocketVersion wsVersion, HttpHeaders headers) {
        return WebSocketClientHandshakerFactory.newHandshaker(uri, wsVersion, null, false, headers, maxFramePayloadLength);
    }

    @Override
    public void startHandshake(URI uri, WebSocketVersion wsVersion, HttpHeaders headers, Channel channel) {
        this.handshaker = createClientHandshaker(uri, wsVersion, headers);
        handshaker.handshake(channel);
    }

    public boolean isHandshakeComplete() {
        return handshaker != null && handshaker.isHandshakeComplete();
    }

    public void close(Channel channel) {
        if (handshaker != null) {
            handshaker.close(channel, new CloseWebSocketFrame());
        }
    }
}
