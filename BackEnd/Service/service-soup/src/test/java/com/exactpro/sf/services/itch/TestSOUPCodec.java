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
package com.exactpro.sf.services.itch;

import com.exactpro.sf.common.impl.messages.DefaultMessageFactory;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.services.MessageHelper;
import com.exactpro.sf.services.MockProtocolDecoderOutput;
import com.exactpro.sf.services.MockProtocolEncoderOutput;
import com.exactpro.sf.services.itch.soup.SOUPCodec;
import com.exactpro.sf.services.itch.soup.SOUPMessageHelper;
import com.exactpro.sf.services.itch.soup.SOUPTcpCodec;
import com.exactpro.sf.util.AbstractTest;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.DummySession;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.AbstractProtocolEncoderOutput;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;

import static com.exactpro.sf.services.itch.soup.SOUPMessageHelper.MESSAGE_TYPE_FIELD;

public class TestSOUPCodec extends AbstractTest {

	private final MessageHelper messageHelper = new SOUPMessageHelper();

	@Before
	public void init() {
		IMessageFactory msgFactory = DefaultMessageFactory.getFactory();
		IDictionaryStructure dictionary =  serviceContext.getDictionaryManager().createMessageDictionary("cfg/dictionaries/soup_test.xml");
		messageHelper.init(msgFactory, dictionary);
		messageHelper.getCodec(serviceContext);
	}

	@Test
	public void testDecode() throws Exception {
		byte[] raw = {
				// Packet Header:
				0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x42, // Session = 'AAAAAAAAAB'
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, // Sequence = 1
				0x00, 0x01, // Count = 1
				// Data Message Header:
				0x00, 0x14, // Length: 20
				0x44, // MessageType = 'D' - OrderDeleted
				// Message Payload
				0x00, 0x00, 0x00, 0x03, // Timestamp = 3
				0x00, 0x04, // TradeDate = 4
				0x00, 0x00, 0x00, 0x05, // TradeableInstrumentId = 5
				0x53, // Side = 'S' = Sell
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x06 // OrderID = 6

		};
		SOUPCodec codec = (SOUPCodec) messageHelper.getCodec(serviceContext);

		// Decode
		IoBuffer toDecode = IoBuffer.wrap(raw);
		IoSession decodeSession = new DummySession();

		MockProtocolDecoderOutput decoderOutput = new MockProtocolDecoderOutput();
		codec.decode(decodeSession, toDecode, decoderOutput);

		// PacketHeader
		IMessage actual = (IMessage) decoderOutput.getMessageQueue().poll();
        Assert.assertEquals(SOUPMessageHelper.PACKET_HEADER_MESSAGE, actual.getName());
		Assert.assertEquals("AAAAAAAAAB", actual.getField("PHSession"));
        Assert.assertEquals((Long)1L, actual.getField("PHSequence"));
        Assert.assertEquals((Integer)1, actual.getField("PHCount"));

		// OrderDeleted
		actual = (IMessage) decoderOutput.getMessageQueue().poll();
		Assert.assertEquals("OrderDeleted", actual.getName());
		Assert.assertEquals("AAAAAAAAAB", actual.getField("PHSession"));
        Assert.assertEquals((Long)1L, actual.getField("PHSequence"));
        Assert.assertEquals((Integer)1, actual.getField("PHCount"));

		Assert.assertEquals("D", actual.getField(MESSAGE_TYPE_FIELD));
        Assert.assertEquals((Long)3L, actual.getField("Timestamp"));
        Assert.assertEquals((Integer)4, actual.getField("TradeDate"));
        Assert.assertEquals((Long)5L, actual.getField("TradeableInstrumentId"));
		Assert.assertEquals("S", actual.getField("Side"));
        Assert.assertEquals((Long)6L, actual.getField("OrderId"));

		// no more messages
		Assert.assertTrue(decoderOutput.getMessageQueue().isEmpty());
	}


	@Test
    public void testEncode() throws Exception {
        IoSession session = new DummySession();
        SOUPCodec codec = (SOUPCodec) messageHelper.getCodec(serviceContext);
		AbstractProtocolEncoderOutput output = new MockProtocolEncoderOutput();

        List<String> testList = new ArrayList<>();
        testList.add("1111");
        testList.add("2222");
        testList.add("3333");
        testList.add("4444");

        // Encode
        IMessageFactory msgFactory = DefaultMessageFactory.getFactory();

        IMessage msg = msgFactory.createMessage("MarketByPrice", "namespace");
        msg.addField("PHSession", "AAAAAAAAAB");
        msg.addField("PHSequence", 1L);
        msg.addField("PHCount", 1);
        msg.addField(MESSAGE_TYPE_FIELD, 6);
        msg.addField("Length", 20);

        msg.addField("ArrayOfItems", testList);

        msg = messageHelper.prepareMessageToEncode(msg, null);
        codec.encode(session, msg, output);
        Queue<Object> msgQueue = output.getMessageQueue();

		IoBuffer lastMessage = (IoBuffer) msgQueue.element();
        Assert.assertNotNull(lastMessage);
        byte[] bytes = new byte[lastMessage.limit()];
        lastMessage.get(bytes);

        byte[] raw = {
                0x36,

                0x00, 0x04,

                0x31, 0x31, 0x31, 0x31, 0x32, 0x32, 0x32, 0x32,
                0x33, 0x33, 0x33, 0x33, 0x34, 0x34, 0x34, 0x34
        };

        Assert.assertArrayEquals(raw,bytes);
	}

	@Test
	@Ignore
	public void testDecodeTradeReport() throws Exception {
		byte[] raw = {
				// Packet Header:
				0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x42, // Session = 'AAAAAAAAAB'
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, // Sequence = 1
				0x00, 0x01, // Count = 1
				// Data Message Header:
				0x00, 32, // Length: 41
				0x59, // MessageType = 'Y' - MarketSettlement
				// Message Payload
				0x00, 0x00, 0x00, 0x03, // Timestamp = 3
				0x00, 0x04, // TradeDate = 4
				0x00, 0x00, 0x00, 0x05, // TradeableInstrumentId = 5
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x06, // SettelmentPrice = 6
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, // Volatility = 0.000007
				(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xF7, // Delta = -0.000009
				0x41, // 'A'


		};
		SOUPCodec codec = (SOUPCodec) messageHelper.getCodec(serviceContext);

		// Decode
		IoBuffer toDecode = IoBuffer.wrap(raw);
		IoSession decodeSession = new DummySession();

		MockProtocolDecoderOutput decoderOutput = new MockProtocolDecoderOutput();
		codec.decode(decodeSession, toDecode, decoderOutput);

		// PacketHeader
		IMessage actual = (IMessage) decoderOutput.getMessageQueue().poll();
        Assert.assertEquals(ITCHMessageHelper.MESSAGE_UNIT_HEADER_NAME, actual.getName());
		Assert.assertEquals("AAAAAAAAAB", actual.getField("Session"));
        Assert.assertEquals((Long)1L, actual.getField("Sequence"));
        Assert.assertEquals((Integer)1, actual.getField("Count"));

		// OrderDeleted
		actual = (IMessage) decoderOutput.getMessageQueue().poll();
		Assert.assertEquals("MarketSettlement", actual.getName());
		Assert.assertEquals("AAAAAAAAAB", actual.getField("Session"));
        Assert.assertEquals((Long)1L, actual.getField("Sequence"));
        Assert.assertEquals((Integer)1, actual.getField("Count"));

		Assert.assertEquals("Y", actual.getField(MESSAGE_TYPE_FIELD));
        Assert.assertEquals((Long)3L, actual.getField("Timestamp"));
        Assert.assertEquals((Integer)4, actual.getField("TradeDate"));
        Assert.assertEquals((Long)5L, actual.getField("TradeableInstrumentId"));
        Assert.assertEquals((Long)6L, actual.getField("SettlementPrice"));
		Assert.assertEquals(new BigDecimal("0.000007"), actual.getField("Volatility"));
		Assert.assertEquals(new BigDecimal("-0.000009"), actual.getField("Delta"));

		// no more messages
		Assert.assertTrue(decoderOutput.getMessageQueue().isEmpty());
	}

	@Test
	public void testHeartBeat() throws Exception {
		// real heartbeat:
		// 08:27:46.840375 IP 203.4.177.2.65160 > 239.5.161.1.17510: UDP, length 20
		// 0x0000:  4510 0030 6191 4000 2011 ed0d cb04 b102  E..0a.@.........
		// 0x0010:  ef05 a101 fe88 4466 001c 4bbb 3134 3531  ......Df..K.1451
		// 0x0020:  3334 3236 3530 0000 0000 0001 63fc 0000  342650......c...

		byte[] raw = {
				// Packet Header:
				0x31, 0x34, 0x35, 0x31, 0x33, 0x34, 0x32, 0x36, 0x35, 0x30, // Session = '1451342650'
				0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x63, (byte) 0xfc, // Sequence = 91132
				0x00, 0x00, // Count = 0
		};
		SOUPCodec codec = (SOUPCodec) messageHelper.getCodec(serviceContext);

		// Decode
		IoBuffer toDecode = IoBuffer.wrap(raw);
		IoSession decodeSession = new DummySession();

		MockProtocolDecoderOutput decoderOutput = new MockProtocolDecoderOutput();
		codec.decode(decodeSession, toDecode, decoderOutput);

		// PacketHeader
		IMessage actual = (IMessage) decoderOutput.getMessageQueue().poll();
        Assert.assertNotNull("Actual message is null", actual);
        Assert.assertEquals(SOUPMessageHelper.PACKET_HEADER_MESSAGE, actual.getName());
		Assert.assertEquals("1451342650", actual.getField("PHSession"));
        Assert.assertEquals((Long)91132L, actual.getField("PHSequence"));
        Assert.assertEquals((Integer)0, actual.getField("PHCount"));

		// no more messages
		Assert.assertTrue(decoderOutput.getMessageQueue().isEmpty());
	}

    @Test
    public void testEndOfSession() throws Exception {
        byte[] raw = {
                // Packet Header:
                0x31, 0x34, 0x35, 0x31, 0x33, 0x34, 0x32, 0x36, 0x35, 0x30, // Session = '1451342650'
                0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x63, (byte) 0xfc, // Sequence = 91132
                (byte)0xFF, (byte)0xFF, // Count = 65535 (end of session)
        };
        SOUPCodec codec = (SOUPCodec) messageHelper.getCodec(serviceContext);

        // Decode
        IoBuffer toDecode = IoBuffer.wrap(raw);
        IoSession decodeSession = new DummySession();

        MockProtocolDecoderOutput decoderOutput = new MockProtocolDecoderOutput();
        codec.decode(decodeSession, toDecode, decoderOutput);

        // PacketHeader
        IMessage actual = (IMessage) decoderOutput.getMessageQueue().poll();
        Assert.assertNotNull("Actual message is null", actual);
        Assert.assertEquals(SOUPMessageHelper.PACKET_HEADER_MESSAGE, actual.getName());
        Assert.assertEquals("1451342650", actual.getField("PHSession"));
        Assert.assertEquals((Long)91132L, actual.getField("PHSequence"));
        Assert.assertEquals((Integer)65535, actual.getField("PHCount"));

        // no more messages
        Assert.assertTrue(decoderOutput.getMessageQueue().isEmpty());
    }
}
