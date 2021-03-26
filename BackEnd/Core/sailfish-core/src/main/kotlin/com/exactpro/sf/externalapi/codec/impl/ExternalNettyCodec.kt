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
import com.exactpro.sf.common.messages.IMetadata
import com.exactpro.sf.common.util.HexDumper
import com.exactpro.sf.externalapi.codec.IExternalCodec
import com.exactpro.sf.services.netty.internal.NettyEmbeddedPipeline
import com.exactpro.sf.services.netty.internal.RawDataHolder
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelInboundHandler
import io.netty.channel.ChannelOutboundHandler
import io.netty.channel.embedded.EmbeddedChannel
import org.slf4j.LoggerFactory

class ExternalNettyCodec(
    private val embeddedPipeline: NettyEmbeddedPipeline
) : IExternalCodec {

    override fun encode(message: IMessage): ByteArray = embeddedPipeline.runCatching {
        encode(message)
    }.getOrElse {
        throw EncodeException("Failed to encode message: $message", it)
    }

    override fun decode(data: ByteArray): List<IMessage> = embeddedPipeline.runCatching {
        decode(RawDataHolder(data, IMetadata.EMPTY))
    }.getOrElse {
        throw DecodeException("Failed to decode data:${System.lineSeparator()}${HexDumper.getHexdump(data)}", it)
    }

    override fun close() {
        embeddedPipeline.close()
    }
}