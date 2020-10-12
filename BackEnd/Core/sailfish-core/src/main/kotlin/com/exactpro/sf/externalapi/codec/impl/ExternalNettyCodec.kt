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
import com.exactpro.sf.common.util.HexDumper
import com.exactpro.sf.externalapi.codec.IExternalCodec
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelInboundHandler
import io.netty.channel.ChannelOutboundHandler
import io.netty.channel.embedded.EmbeddedChannel
import org.slf4j.LoggerFactory

class ExternalNettyCodec(
    encodeHandlers: List<ChannelOutboundHandler>,
    decodeHandlers: List<ChannelInboundHandler>
) : IExternalCodec {
    private val logger = LoggerFactory.getLogger("${this::class.java.canonicalName}@${this.hashCode().toString(16)}")

    private val encodeChannel: EmbeddedChannel = createChannel(encodeHandlers)
    private val decodeChannel: EmbeddedChannel = createChannel(decodeHandlers)

    override fun encode(message: IMessage): ByteArray = encodeChannel.runCatching {
        check(writeOutbound(message)) { "Encoding did not produce any results" }
        check(outboundMessages().size == 1) { "Expected 1 result, but got: ${outboundMessages().size}" }
        readOutbound<ByteBuf>().run { ByteArray(readableBytes()).apply { readBytes(this) } }
    }.getOrElse {
        encodeChannel.releaseOutbound()
        throw EncodeException("Failed to encode message: $message", it)
    }

    override fun decode(data: ByteArray): List<IMessage> = decodeChannel.runCatching {
        check(writeInbound(Unpooled.wrappedBuffer(data))) { "Decoding did not produce any results" }
        generateSequence { readInbound<IMessage>() }.toList()
    }.getOrElse {
        decodeChannel.releaseInbound()
        throw DecodeException("Failed to decode data:${System.lineSeparator()}${HexDumper.getHexdump(data)}", it)
    }

    private fun createChannel(handlers: List<ChannelHandler>): EmbeddedChannel = EmbeddedChannel().apply {
        handlers.forEach { pipeline().addLast(it) }
    }

    override fun close() {
        runCatching(encodeChannel.close()::sync).onFailure { logger.error("Failed to close encode channel", it) }
        runCatching(decodeChannel.close()::sync).onFailure { logger.error("Failed to close decode channel", it) }
    }
}