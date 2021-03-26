/*******************************************************************************
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

package com.exactpro.sf.services.netty.internal

import com.exactpro.sf.common.messages.IMessage
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelInboundHandler
import io.netty.channel.ChannelOutboundHandler
import io.netty.channel.embedded.EmbeddedChannel
import org.slf4j.LoggerFactory

open class NettyEmbeddedPipeline(
    encodeHandlers: List<ChannelOutboundHandler>,
    decodeHandlers: List<ChannelInboundHandler>
) : AutoCloseable {
    protected val LOGGER = LoggerFactory.getLogger("${this::class.java.canonicalName}@${this.hashCode().toString(16)}")

    private val encodeChannel: EmbeddedChannel = createChannel(encodeHandlers)
    private val decodeChannel: EmbeddedChannel = createChannel(decodeHandlers)

    /**
     * Writes [message] to the [encodeChannel]
     */
    open fun encode(message: IMessage): ByteArray = encodeInternal { message }

    /**
     * Writes bytes from [rawDataHolder] to the [decodeChannel]
     */
    open fun decode(rawDataHolder: RawDataHolder): List<IMessage> = decodeInternal { Unpooled.wrappedBuffer(rawDataHolder.bytes) }

    protected fun <T> encodeInternal(outbound: () -> T): ByteArray = encodeChannel.runCatching {
        check(writeOutbound(outbound())) { "Encoding did not produce any results" }
        check(outboundMessages().size == 1) { "Expected 1 result, but got: ${outboundMessages().size}" }
        readOutbound<ByteBuf>().run { ByteArray(readableBytes()).apply { readBytes(this) } }
    }.getOrElse {
        encodeChannel.releaseOutbound()
        throw it
    }

    protected fun <T> decodeInternal(limit: Int = -1, inbound: () -> T): List<IMessage> = decodeChannel.runCatching {
        check(writeInbound(inbound())) { "Decoding did not produce any results" }
        generateSequence { readInbound<IMessage>() }.toList().also {
            if (limit > 0) {
                check(it.size <= limit) { "More than $limit message(s) was(were) read" }
            }
        }
    }.getOrElse {
        decodeChannel.releaseInbound()
        throw it
    }

    private fun createChannel(handlers: List<ChannelHandler>): EmbeddedChannel = EmbeddedChannel().apply {
        handlers.forEach { pipeline().addLast(it) }
    }

    override fun close() {
        runCatching(encodeChannel.close()::sync).onFailure { LOGGER.error("Failed to close encode channel", it) }
        runCatching(decodeChannel.close()::sync).onFailure { LOGGER.error("Failed to close decode channel", it) }
    }
}