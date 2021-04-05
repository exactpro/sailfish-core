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

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.util.EPSCommonException;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

/**
 * @author sergey.smirnov
 *
 */
public class HandlerEncode extends MessageToMessageEncoder<IMessage> {

    private final FrameType frameType;

    /**
     * 
     */
    public HandlerEncode() {
        this(FrameType.BINARY);
    }

    /**
     * 
     */
    public HandlerEncode(FrameType frameType) {
        this.frameType = Objects.requireNonNull(frameType, "'Frame type' parameter");
    }

    /*
     * (non-Javadoc)
     * 
     * @see io.netty.handler.codec.MessageToMessageEncoder#encode(io.netty.channel.
     * ChannelHandlerContext, java.lang.Object, java.util.List)
     */
    @Override
    protected void encode(ChannelHandlerContext handlerContext, IMessage msg, List<Object> out) throws Exception {

        byte[] content = msg.getMetaData().getRawMessage();

        WebSocketFrame textFrame = getWebSocketFrame(Unpooled.wrappedBuffer(content), frameType);

        out.add(textFrame);
    }

    private WebSocketFrame getWebSocketFrame(ByteBuf content, FrameType frameType) {
        switch (frameType) {
        case BINARY:
            return new BinaryWebSocketFrame(content);
        case TEXT:
            return new TextWebSocketFrame(content);
        default:
            throw new EPSCommonException("Unknown frame type: " + frameType);
        }
    }

    public enum FrameType {
        TEXT, BINARY
    }
}
