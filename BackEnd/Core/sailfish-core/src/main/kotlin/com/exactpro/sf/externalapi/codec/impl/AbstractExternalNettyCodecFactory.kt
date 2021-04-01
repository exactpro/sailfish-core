/*******************************************************************************
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
package com.exactpro.sf.externalapi.codec.impl

import com.exactpro.sf.common.messages.IMessage
import com.exactpro.sf.externalapi.codec.IExternalCodec
import com.exactpro.sf.externalapi.codec.IExternalCodecSettings
import com.exactpro.sf.services.netty.internal.NettyEmbeddedPipeline
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandler
import io.netty.channel.ChannelOutboundHandler
import io.netty.handler.codec.MessageToByteEncoder

abstract class AbstractExternalNettyCodecFactory : AbstractExternalCodecFactory() {
    protected abstract fun getEncodeHandlers(settings: IExternalCodecSettings): List<ChannelOutboundHandler>
    protected abstract fun getDecodeHandlers(settings: IExternalCodecSettings): List<ChannelInboundHandler>

    override fun createCodec(settings: IExternalCodecSettings): IExternalCodec {
        validateDictionaries(settings)
        return ExternalNettyCodec(NettyEmbeddedPipeline(getEncodeHandlers(settings), getDecodeHandlers(settings)))
    }

    protected class IMessageToByteBufEncoder : MessageToByteEncoder<IMessage>() {
        override fun encode(context: ChannelHandlerContext, msg: IMessage, out: ByteBuf) {
            out.writeBytes(msg.metaData.rawMessage)
        }
    }
}