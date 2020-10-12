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

import com.exactpro.sf.common.codecs.AbstractCodec
import com.exactpro.sf.common.messages.IMessage
import com.exactpro.sf.common.messages.IMessageFactory
import com.exactpro.sf.common.messages.structures.IDictionaryStructure
import com.exactpro.sf.common.util.HexDumper
import com.exactpro.sf.common.util.ICommonSettings
import com.exactpro.sf.externalapi.codec.IExternalCodec
import com.exactpro.sf.externalapi.codec.IExternalCodecContext
import com.exactpro.sf.externalapi.codec.IExternalCodecContext.Role
import com.exactpro.sf.services.IServiceContext
import com.exactpro.sf.services.MockProtocolDecoderOutput
import com.exactpro.sf.services.MockProtocolEncoderOutput
import org.apache.mina.core.buffer.IoBuffer
import org.apache.mina.core.session.DummySession
import org.apache.mina.core.session.IoSession

class ExternalMinaCodec(
    codecClass: Class<out AbstractCodec>,
    serviceContext: IServiceContext,
    settings: ICommonSettings,
    messageFactory: IMessageFactory,
    dictionary: IDictionaryStructure
) : IExternalCodec {
    private val codec: AbstractCodec = codecClass.newInstance().apply {
        init(serviceContext, settings, messageFactory, dictionary)
    }

    private val encodeSession = DummySession()
    private val decodeSession = DummySession()

    private val encodeOutput = MockProtocolEncoderOutput()
    private val decodeOutput = MockProtocolDecoderOutput()

    override fun encode(message: IMessage): ByteArray = encode(message, EMPTY_CONTEXT)

    override fun encode(message: IMessage, context: IExternalCodecContext): ByteArray = encodeOutput.runCatching {
        encodeSession.setContext(context)
        codec.encode(encodeSession, message, this)
        check(messageQueue.size == 1) { "Expected 1 result, but got: ${messageQueue.size}" }
        (messageQueue.poll() as IoBuffer).run { ByteArray(remaining()).apply { get(this) } }
    }.getOrElse {
        encodeOutput.messageQueue.clear()
        throw EncodeException("Failed to encode message: $message", it)
    }

    override fun decode(data: ByteArray): List<IMessage> = decode(data, EMPTY_CONTEXT)

    override fun decode(data: ByteArray, context: IExternalCodecContext): List<IMessage> = decodeOutput.runCatching {
        decodeSession.setContext(context)
        codec.decode(decodeSession, IoBuffer.wrap(data), this)
        check(messageQueue.isNotEmpty()) { "Decoding did not produce any results" }
        generateSequence(messageQueue::poll).map { it as IMessage }.toList()
    }.getOrElse {
        decodeOutput.messageQueue.clear()
        throw DecodeException("Failed to decode data:${System.lineSeparator()}${HexDumper.getHexdump(data)}", it)
    }

    private fun IoSession.setContext(context: IExternalCodecContext) = when (context) {
        EMPTY_CONTEXT -> removeAttribute(IExternalCodecContext::class.java)
        else -> setAttribute(IExternalCodecContext::class.java, context)
    }

    override fun close() {}

    companion object {
        private val EMPTY_CONTEXT = ExternalCodecContext(Role.RECEIVER)
    }
}