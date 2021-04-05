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
package com.exactpro.sf.services.websocket.handlers

import com.exactpro.sf.common.messages.IMessage
import com.exactpro.sf.common.util.EPSCommonException
import com.exactpro.sf.messages.websocket.RawMessage
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageEncoder

/**
 * Packs [IMessage] for raw message into [RawMessage] bean, so it would pass through handlers
 * defined in [com.exactpro.sf.services.websocket.WebSocketClient.fillEncodeLayer] and be later
 * unpacked via [RawMessageUnpacker] to be handled by [HandlerEncode]
 */
class RawMessagePacker : MessageToMessageEncoder<IMessage>() {
    override fun encode(context: ChannelHandlerContext, msg: IMessage, out: MutableList<Any>) {
        out += if (msg.name == RawMessage.MESSAGE_NAME) {
            RawMessage(msg).apply {
                if (!isSetText && !isSetBytes) {
                    throw EPSCommonException("${msg.name} should contain $TEXT_FIELD or $BYTES_FIELD field")
                }

                msg.metaData.apply {
                    rawMessage = if (isSetBytes) bytes.toByteArray() else text.toByteArray(Charsets.UTF_8)
                }
            }
        } else {
            msg
        }
    }

    companion object {
        const val TEXT_FIELD = "Text"
        const val BYTES_FIELD = "Bytes"
    }
}