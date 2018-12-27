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
package com.exactpro.sf.util;

import java.io.IOException;

import org.junit.Assert;
import org.slf4j.Logger;

import com.exactpro.sf.common.messages.IMessage;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;

public class NettyTestUtility {

    public static ByteBuf encode(IMessage msg, final Logger logger, ChannelHandler... handlers) throws IOException {
        EmbeddedChannel encodeChannel = getEmbeddedChannel(logger, handlers);
        encodeChannel.writeOutbound(msg);
        ByteBuf buf = (ByteBuf) encodeChannel.outboundMessages().poll();
        Assert.assertTrue(buf.isReadable());
        return buf;
    }

    public static IMessage decode(ByteBuf buf, final Logger logger, ChannelHandler... handlers) {
        EmbeddedChannel decodeChannel = getEmbeddedChannel(logger, handlers);
        decodeChannel.writeInbound(buf);
        return (IMessage) decodeChannel.inboundMessages().poll();
    }

    public static EmbeddedChannel getEmbeddedChannel(final Logger logger, ChannelHandler... handlers) {
        EmbeddedChannel channel = new EmbeddedChannel(handlers);
        channel.pipeline().addLast(
                new ChannelInboundHandlerAdapter() {
                    @Override
                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                        if (logger != null) {
                            logger.error(cause.getMessage(), cause);
                        }
                    }
                }
        );
        return channel;
    }

    public static IMessage encodeDecode(IMessage msg, ChannelHandler[] encodeHandlers,
                                          ChannelHandler[] decodeHandlers, final Logger logger)
            throws IOException {
        try {
            ByteBuf buf = encode(msg, logger, encodeHandlers);
            return decode(buf, logger, decodeHandlers);
        } catch (IOException e) {
            if (logger != null) {
                logger.error(e.getMessage(), e);
            }
            throw e;
        }
    }
}
