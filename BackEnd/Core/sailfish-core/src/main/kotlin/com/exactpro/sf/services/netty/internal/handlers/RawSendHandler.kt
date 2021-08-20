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

package com.exactpro.sf.services.netty.internal.handlers

import com.exactpro.sf.common.messages.IMessage
import com.exactpro.sf.common.messages.merge
import com.exactpro.sf.services.netty.internal.NettyEmbeddedPipeline
import com.exactpro.sf.services.netty.internal.RawDataHolder
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageEncoder

/**
 * Allows to send raw data via the service. It calls the passed [embeddedPipeline] to decode the raw data
 * and filters the result using [acceptMessage] function.
 * All decoded messages go further in the actual pipeline
 */
class RawSendHandler @JvmOverloads constructor(
    private val embeddedPipeline: NettyEmbeddedPipeline?,
    private val acceptMessage: (IMessage) -> Boolean = { true }
) : MessageToMessageEncoder<RawDataHolder>(RawDataHolder::class.java) {
    override fun encode(context: ChannelHandlerContext, msg: RawDataHolder, out: MutableList<Any>) {
        requireNotNull(embeddedPipeline) {
            "Raw data sending is not enabled"
        }
        embeddedPipeline.decode(msg).forEach {
            if (acceptMessage(it)) {
                out += it.apply {
                    metaData.merge(msg.metadata)
                }
            }
        }
    }
}