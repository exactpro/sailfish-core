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

package com.exactpro.sf.services.itch.soup

import com.exactpro.sf.common.codecs.AbstractCodec
import com.exactpro.sf.common.impl.messages.DefaultMessageFactory
import com.exactpro.sf.common.messages.IMessageFactory
import com.exactpro.sf.common.messages.IMetadata
import com.exactpro.sf.common.messages.structures.IDictionaryStructure
import com.exactpro.sf.common.messages.structures.loaders.XmlDictionaryStructureLoader
import com.exactpro.sf.common.services.ServiceName
import com.exactpro.sf.services.IServiceContext
import com.exactpro.sf.services.itch.ITCHCodecSettings
import org.apache.mina.core.buffer.IoBuffer
import org.apache.mina.core.filterchain.IoFilter
import org.apache.mina.core.filterchain.IoFilter.NextFilter
import org.apache.mina.core.future.WriteFuture
import org.apache.mina.core.session.DummySession
import org.apache.mina.core.session.IoSession
import org.apache.mina.core.write.WriteRequest
import org.apache.mina.filter.codec.ProtocolCodecFactory
import org.apache.mina.filter.codec.ProtocolCodecFilter
import org.apache.mina.filter.codec.ProtocolDecoder
import org.apache.mina.filter.codec.ProtocolEncoder
import org.junit.Assert
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.times
import java.io.FileInputStream
import kotlin.test.assertNotNull

class TestInternalSession {
    private fun createCodec(messageFactory: IMessageFactory, dictionaryStructure: IDictionaryStructure): AbstractCodec {
        return SOUPTcpCodec().apply {
            init(Mockito.mock(IServiceContext::class.java), ITCHCodecSettings(), messageFactory, dictionaryStructure)
        }
    }

    @Test
    fun testSendsRaw() {
        val dummySession = DummySession()
        val msgFactory = DefaultMessageFactory.getFactory()
        val dictionary = FileInputStream("src/test/workspace/cfg/dictionaries/soup_test.xml")
            .use(XmlDictionaryStructureLoader()::load)

        val codec = createCodec(msgFactory, dictionary)
        val sink = Mockito.mock(IoFilter::class.java)
        Mockito.`when`(sink.filterWrite(any(), any(), any())).then {
            val next = it.arguments[0] as NextFilter
            val session = it.arguments[1] as IoSession
            val future = it.arguments[2] as WriteRequest
            next.filterWrite(session, future)
        }
        dummySession.filterChain.apply {

            addLast("sink", sink)
            addLast("codec", ProtocolCodecFilter(object : ProtocolCodecFactory {

                override fun getEncoder(session: IoSession): ProtocolEncoder = codec
                override fun getDecoder(session: IoSession): ProtocolDecoder = codec
            }))
        }

        val messageHelper = SOUPTcpMessageHelper().apply {
            init(msgFactory, dictionary)
        }
        val session = SOUPTcpClient.InternalSession(ServiceName("test", "service"), dummySession, 2, 1000L, messageHelper)

        val raw = byteArrayOf( // TCP Soup BIN:
            0x00, 21,  // Length
            0x55,  // PacketType = 'S' - SequencedMessage
            // Body
            0x44,  // MessageType = 'D' - OrderDeleted
            // Message Payload
            0x00, 0x00, 0x00, 0x03,  // Timestamp = 3
            0x00, 0x04,  // TradeDate = 4
            0x00, 0x00, 0x00, 0x05,  // TradeableInstrumentId = 5
            0x53,  // Side = 'S' = Sell
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x06 // OrderID = 6
        )

        session.sendRaw(raw, IMetadata.EMPTY)
        val argumentCaptor = ArgumentCaptor.forClass(WriteRequest::class.java)
        Mockito.verify(sink, times(2)).filterWrite(any(), any(), argumentCaptor.capture())
        assertNotNull(argumentCaptor.value)

        val buffer = argumentCaptor.allValues.first().message as IoBuffer

        Assert.assertArrayEquals(raw, ByteArray(buffer.limit()).also { buffer.get(it) })
    }
}