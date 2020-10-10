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

package com.exactpro.sf.services.netty.handlers

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageDecoder

/**
 * This class should be use to produce a mark that means that the all input bytes processed.
 * If any messages we received before [Delimiter] they should be packed to a single [com.exactpro.sf.common.util.EvolutionBatch].
 *
 * This handler provides compatibility with Evolution project and should be used
 * only if [com.exactpro.sf.common.util.IEvolutionSettings.isEvolutionSupportEnabled] returns true.
 */
class DecodedMessagesDelimiterHandler : MessageToMessageDecoder<ByteBuf>() {
    override fun decode(context: ChannelHandlerContext, msg: ByteBuf, out: MutableList<Any>) {
        out += msg.retain()
        out += Delimiter
    }

}

/**
 * If this object is received all incoming messages were handled
 */
object Delimiter
