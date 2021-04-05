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

import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

/**
 * @author sergey.smirnov
 *
 */
public class HandlerDecode extends MessageToMessageDecoder<WebSocketFrame> {

    
    public static final String CLOSE_FRAME = "CloseFrame";
    private static final Logger logger = LoggerFactory.getLogger(HandlerDecode.class);
    private final IMessageFactory msFactory;

    /**
     * 
     */
    public HandlerDecode(IMessageFactory msFactory) {
        this.msFactory = Objects.requireNonNull(msFactory, "'Message factory' parameter");
    }
    
    /* (non-Javadoc)
     * @see io.netty.handler.codec.MessageToMessageDecoder#decode(io.netty.channel.ChannelHandlerContext, java.lang.Object, java.util.List)
     */
    @Override
    protected void decode(ChannelHandlerContext handlerContext, WebSocketFrame msg, List<Object> out) throws Exception {
        
        if (msg instanceof TextWebSocketFrame || msg instanceof BinaryWebSocketFrame) {
            logger.debug("Data frame received");
            //produce bytes to real decoder
            out.add(msg.content().retain());
        } else if (msg instanceof PongWebSocketFrame) {
            logger.debug("Pong frame received");
        } else if (msg instanceof CloseWebSocketFrame) {
            logger.debug("Closing frame received");
            IMessage logoutMsg = msFactory.createMessage(CLOSE_FRAME);
            out.add(logoutMsg);
        } else if (msg instanceof PingWebSocketFrame) {
            logger.debug("Ping frame recived, sending pong frame");
            handlerContext.writeAndFlush(new PongWebSocketFrame());
        }
    }
}
