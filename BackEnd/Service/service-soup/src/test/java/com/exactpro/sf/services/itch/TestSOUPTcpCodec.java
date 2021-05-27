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
package com.exactpro.sf.services.itch;

import com.exactpro.sf.common.impl.messages.DefaultMessageFactory;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.comparison.ComparatorSettings;
import com.exactpro.sf.comparison.ComparisonResult;
import com.exactpro.sf.comparison.MessageComparator;
import com.exactpro.sf.configuration.factory.SOUPMessageFactory;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.extensions.IMessageExtensionsKt;
import com.exactpro.sf.scriptrunner.StatusType;
import com.exactpro.sf.services.MessageHelper;
import com.exactpro.sf.services.MockProtocolDecoderOutput;
import com.exactpro.sf.services.MockProtocolEncoderOutput;
import com.exactpro.sf.services.itch.soup.SOUPCodec;
import com.exactpro.sf.services.itch.soup.SOUPMessageHelper;
import com.exactpro.sf.services.itch.soup.SOUPTcpCodec;
import com.exactpro.sf.services.itch.soup.SOUPTcpMessageHelper;
import com.exactpro.sf.services.itch.soup.SoupTcpCodecSettings;
import com.exactpro.sf.util.AbstractTest;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.DummySession;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.AbstractProtocolDecoderOutput;
import org.apache.mina.filter.codec.AbstractProtocolEncoderOutput;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;

import static com.exactpro.sf.services.itch.soup.SOUPMessageHelper.MESSAGE_TYPE_FIELD;
import static com.exactpro.sf.services.itch.soup.SOUPMessageHelper.PACKET_LENGTH;
import static com.exactpro.sf.services.itch.soup.SOUPMessageHelper.PACKET_TYPE;
import static com.exactpro.sf.services.itch.soup.SOUPMessageHelper.SEQUENCED_DATA_PACKET;
import static com.exactpro.sf.services.itch.soup.SOUPMessageHelper.SEQUENCED_HEADER_MESSAGE;
import static com.exactpro.sf.services.itch.soup.SOUPMessageHelper.SOUP_BIN_TCP_HEADER_NAME;

public class TestSOUPTcpCodec extends AbstractTest {

    private final MessageHelper messageHelper = new SOUPTcpMessageHelper();
    private IMessageFactory msgFactory;
    private IDictionaryStructure dictionary;

    @Before
	public void init() {
        dictionary = serviceContext.getDictionaryManager().createMessageDictionary("cfg/dictionaries/soup_test.xml");
        msgFactory = new SOUPMessageFactory();
        msgFactory.init(SailfishURI.unsafeParse(dictionary.getNamespace()), dictionary);
		messageHelper.init(msgFactory, dictionary);
		messageHelper.getCodec(serviceContext);
	}

	@Test
	public void testEncode() throws Exception {
		IoSession session = new DummySession();
		SOUPTcpCodec codec = (SOUPTcpCodec) messageHelper.getCodec(serviceContext);
		ProtocolEncoderOutput output = new MockProtocolEncoderOutput();

		// Encode
		IMessageFactory msgFactory = DefaultMessageFactory.getFactory();

		IMessage msg = msgFactory.createMessage("LoginRequestPacket", "namespace");
		// msg.addField("PacketLength", AUTO);
		// msg.addField("PacketType", AUTO);
		msg.addField("Member", "ABCDEFG");
		msg.addField("UserName", "USER");
		msg.addField("Password", "Password");
		msg.addField("Ticket", 1L);
		msg.addField("RequestedSequenceNumber", 1L);
		msg.addField("Version", "1.2.3");

		msg = messageHelper.prepareMessageToEncode(msg, null);
		session.write(msg);
		codec.encode(session, msg, output);
		Queue<Object> msgQueue = ((AbstractProtocolEncoderOutput) output).getMessageQueue();

		Object lastMessage = msgQueue.element();
		Assert.assertNotNull(lastMessage);
		byte[] asd = ((IoBuffer) lastMessage).array();
		int limit = ((IoBuffer) lastMessage).limit();
		byte[] bytes = Arrays.copyOf(asd, limit);

		System.err.println(bytes);
	}

	@Test
	public void testDecode() throws Exception {
        byte[] raw = {
				// TCP Soup BIN:
				0x00, 21, // Length
				0x53, // PacketType = 'S' - SequencedMessage
				// Body
				0x44, // MessageType = 'D' - OrderDeleted
				// Message Payload
				0x00, 0x00, 0x00, 0x03, // Timestamp = 3
				0x00, 0x04, // TradeDate = 4
				0x00, 0x00, 0x00, 0x05, // TradeableInstrumentId = 5
				0x53, // Side = 'S' = Sell
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x06 // OrderID = 6

		};
		SOUPTcpCodec codec = (SOUPTcpCodec) messageHelper.getCodec(serviceContext);

		// Decode
		IoBuffer toDecode = IoBuffer.wrap(raw);
		IoSession decodeSession = new DummySession();

		MockProtocolDecoderOutput decoderOutput = new MockProtocolDecoderOutput();
		codec.decode(decodeSession, toDecode, decoderOutput);

		// SequencedMessage
		IMessage actual = (IMessage) decoderOutput.getMessageQueue().poll();
		Assert.assertEquals("SequencedDataPacket", actual.getName());
        Assert.assertEquals((Integer)21, actual.getField("PacketLength"));
		Assert.assertEquals("S", actual.getField("PacketType"));

		// OrderDeleted
		actual = (IMessage) decoderOutput.getMessageQueue().poll();
		Assert.assertEquals("OrderDeleted", actual.getName());
		Assert.assertEquals("D", actual.getField(MESSAGE_TYPE_FIELD));
        Assert.assertEquals((Long)3L, actual.getField("Timestamp"));
        Assert.assertEquals((Integer)4, actual.getField("TradeDate"));
        Assert.assertEquals((Long)5L, actual.getField("TradeableInstrumentId"));
		Assert.assertEquals("S", actual.getField("Side"));
        Assert.assertEquals((Long)6L, actual.getField("OrderId"));
	}

	@Test
	public void testDecodeLoginReject() throws Exception {
		// There was a bug in Codec: it skept all messages that shorter than 20 bytes
        byte[] raw = {
				// TCP Soup BIN:
				0x00, 0x09, // Length
				0x4a, // 'J' Login Reject Packet
				(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xfb,
				0x00, 0x00, 0x00, 0x00,

				// TCP Soup BIN:
				0x00, 0x01, // Length
				0x5a // 'Z' End of Session Packet
		};
		SOUPTcpCodec codec = (SOUPTcpCodec) messageHelper.getCodec(serviceContext);

		// Decode
		IoBuffer toDecode = IoBuffer.wrap(raw);
		IoSession decodeSession = new DummySession();

		MockProtocolDecoderOutput decoderOutput = new MockProtocolDecoderOutput();
		codec.decode(decodeSession, toDecode, decoderOutput);

		// LoginRejectPacket
		IMessage actual = (IMessage) decoderOutput.getMessageQueue().poll();
		Assert.assertEquals("LoginRejectPacket", actual.getName());
        Assert.assertEquals((Integer)9, actual.getField("PacketLength"));
		Assert.assertEquals("J", actual.getField("PacketType"));
        //noinspection UnnecessaryParentheses
        Assert.assertEquals((Integer)(-5), actual.getField("RejectReasonCode"));
        Assert.assertEquals((Integer)0, actual.getField("ErrorCode"));

		codec.decode(decodeSession, toDecode, decoderOutput);

		// EndOfSessionDataPacket
		actual = (IMessage) decoderOutput.getMessageQueue().poll();
		Assert.assertEquals("EndOfSessionDataPacket", actual.getName());
        Assert.assertEquals((Integer)1, actual.getField("PacketLength"));
		Assert.assertEquals("Z", actual.getField("PacketType"));
	}

	@Test
	@Ignore //FIXME
	public void testDecodeLoginAccept() throws Exception {
        byte[] raw = {
				// TCP Soup BIN:
				0x00, 0x09, // Length
				0x41, // 'A' LoginAcceptedPacket
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, // SequenceNumber = 1

				// TCP Soup BIN:
				0x00, 0x06, // Length
				0x53, // 'S' Sequenced Packet
				// Body
				0x54, // 'T' - Time message
				0x56, (byte)0x94, (byte) 0x96, 0x60, // Seconds

				// TCP Soup BIN:
				0x00, 0x38, // Length
				0x53, // 'S' Sequenced Packet
				// Body
				0x74, // 't' - Open, High, Low, Last Trade Adjustment
				0x12, 0x15, (byte) 0xf9, 0x70, // timestamp
			    0x00, 0x00, // trade date
			    0x00, 0x00, 0x0d, 0x51, // Tradeable Instrument Id
			    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			    0x00, 0x00, 0x00, 0x00, // Last Volume
			    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
		};
		SOUPTcpCodec codec = (SOUPTcpCodec) messageHelper.getCodec(serviceContext);

		// Decode
		IoBuffer toDecode = IoBuffer.wrap(raw);
		IoSession decodeSession = new DummySession();

		MockProtocolDecoderOutput decoderOutput = new MockProtocolDecoderOutput();
		codec.decode(decodeSession, toDecode, decoderOutput);

		// LoginRejectPacket
		IMessage actual = (IMessage) decoderOutput.getMessageQueue().poll();
		Assert.assertEquals("LoginAcceptedPacket", actual.getName());
        Assert.assertEquals((Integer)9, actual.getField("PacketLength"));
		Assert.assertEquals("A", actual.getField("PacketType"));
        Assert.assertEquals((Long)1L, actual.getField("SequenceNumber"));

		codec.decode(decodeSession, toDecode, decoderOutput);

		// SequencedMessage
		actual = (IMessage) decoderOutput.getMessageQueue().poll();
		Assert.assertEquals("SequencedDataPacket", actual.getName());
        Assert.assertEquals((Integer)6, actual.getField("PacketLength"));
		Assert.assertEquals("S", actual.getField("PacketType"));

		// TimeMessage
		actual = (IMessage) decoderOutput.getMessageQueue().poll();
		Assert.assertEquals("TimeMessage", actual.getName());
		Assert.assertEquals("T", actual.getField(MESSAGE_TYPE_FIELD));
        Assert.assertEquals((Long)1452578400L, actual.getField("Second"));

		codec.decode(decodeSession, toDecode, decoderOutput);

		// SequencedMessage
		actual = (IMessage) decoderOutput.getMessageQueue().poll();
		Assert.assertEquals("SequencedDataPacket", actual.getName());
        Assert.assertEquals((Integer)56, actual.getField("PacketLength"));
		Assert.assertEquals("S", actual.getField("PacketType"));

		// OpenHighLowLastTradeAdjustment
		actual = (IMessage) decoderOutput.getMessageQueue().poll();
		Assert.assertEquals("OpenHighLowLastTradeAdjustment", actual.getName());
		Assert.assertEquals("t", actual.getField(MESSAGE_TYPE_FIELD));
        Assert.assertEquals((Long)303430000L, actual.getField("Timestamp"));
        Assert.assertEquals((Integer)0, actual.getField("TradeDate"));
        Assert.assertEquals((Long)3409L, actual.getField("TradeableInstrumentId"));
        Assert.assertEquals((Long)0L, actual.getField("OpeningTrade"));
        Assert.assertEquals((Long)0L, actual.getField("HighestTrade"));
        Assert.assertEquals((Long)0L, actual.getField("LowestTrade"));
        Assert.assertEquals((Long)0L, actual.getField("LastVolume"));
        Assert.assertEquals((Long)0L, actual.getField("TotalTradeVolume"));
	}

	@Test
    public void TestDecodeWithSeparateHeader() throws Exception {
        MessageData messageData = new MessageData(msgFactory);
        IoBuffer raw = IoBuffer.allocate(0).setAutoExpand(true);
        raw.put(messageData.getLoginHeaderRaw());
        raw.put(messageData.getLoginBodyRaw());
        raw.put(messageData.getHearbeatRaw());
        raw.put(messageData.getOrderDeletedHeaderRaw());
        raw.put(messageData.getOrderDeletedRaw());
        raw.flip();
        Queue<Object> messageQueue = decodeWithSeparateHeader(raw);
        Assert.assertEquals(5, messageQueue.size());
        compare(messageData.getLoginAcceptedHeader(), messageQueue.poll(), messageData.getLoginHeaderRaw());
        compare(messageData.getLoginAccepted(), messageQueue.poll(), messageData.getLoginBodyRaw());
        compare(messageData.getHeartbeatHeader(), messageQueue.poll(), messageData.getHearbeatRaw());
        compare(messageData.getOrderDeletedHeader(), messageQueue.poll(), messageData.getOrderDeletedHeaderRaw());
        compare(messageData.getOrderDeleted(), messageQueue.poll(), messageData.getOrderDeletedRaw());
    }

	@Test
	public void testDecodeCombinationSymbolDirectory() throws Exception {
        byte[] raw = {
				// TCP Soup BIN:
				0x00, (byte) 0xdf,// Length
				0x53,  // 'S'
				// Body
				0x4d, // M
				0x38, 0x35, 0x30, (byte) 0xd0, // Timestamp
				0x41, (byte) 0xad, // TradeDate
				0x00, 0x00, 0x37, (byte) 0xa0, // Tradeable Instrument Id
				// SymbolName 'UDC_BB-F_J5DV'
				0x55, 0x44, 0x43, 0x5f, 0x42, 0x42, 0x2d, 0x46, 0x5f, 0x4a, 0x35, 0x44, 0x56, 0x20, 0x20, 0x20,
				0x20, 0x20,	0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20,

				// 'UDC Instrument for Trade24 Market'
				0x55, 0x44, 0x43, 0x20, 0x49, 0x6e, 0x73, 0x74, 0x72, 0x75, 0x6d, 0x65, 0x6e, 0x74, 0x20, 0x66,
				0x6f, 0x72, 0x20, 0x54, 0x72, 0x61, 0x64, 0x65, 0x32, 0x34, 0x20, 0x4d, 0x61, 0x72, 0x6b, 0x65,
                0x74, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20,
                0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20,
				// CFI Code 'MMMXXX'
				0x4d, 0x4d, 0x4d, 0x58, 0x58, 0x58,

				0x00, // Price Method
				0x00, // Price Display Decimals
				0x00, 0x0f, 0x42, 0x40, // Price Fractional Denominator
				0x00, 0x00, 0x27, 0x10, // Price Minimum Tick
				0x02, // Legs

				// LEGS:
				0x00, 0x00, 0x04, 0x01,
				0x53, // Side 'S'
				0x00, 0x00, 0x00, 0x04,
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,

				0x00, 0x00, 0x04, 0x04,
				0x53,
				0x00, 0x00, 0x00, 0x01,
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,

				0x00, 0x00, 0x00, 0x00,
				0x20,
				0x00, 0x00, 0x00, 0x00,
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,

				0x00, 0x00, 0x00, 0x00,
				0x20,
				0x00, 0x00, 0x00, 0x00,
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,

				0x00, 0x00, 0x00, 0x00,
				0x20,
				0x00, 0x00, 0x00, 0x00,
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,

				0x00, 0x00, 0x00, 0x00,
				0x20,
				0x00, 0x00, 0x00, 0x00,
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
		};
		SOUPTcpCodec codec = (SOUPTcpCodec) messageHelper.getCodec(serviceContext);

		// Decode
		IoBuffer toDecode = IoBuffer.wrap(raw);
		IoSession decodeSession = new DummySession();

		MockProtocolDecoderOutput decoderOutput = new MockProtocolDecoderOutput();
		codec.decode(decodeSession, toDecode, decoderOutput);

		// SequencedMessage
		IMessage actual = (IMessage) decoderOutput.getMessageQueue().poll();
		Assert.assertEquals("SequencedDataPacket", actual.getName());
        Assert.assertEquals((Integer)223, actual.getField("PacketLength"));
		Assert.assertEquals("S", actual.getField("PacketType"));

		// TimeMessage
		actual = (IMessage) decoderOutput.getMessageQueue().poll();
		Assert.assertEquals("CombinationSymbolDirectory", actual.getName());
		Assert.assertEquals("M", actual.getField(MESSAGE_TYPE_FIELD));
		// Missed fields
		Assert.assertEquals("UDC_BB-F_J5DV", actual.getField("SymbolName"));
		Assert.assertEquals("UDC Instrument for Trade24 Market", actual.getField("LongName"));
		Assert.assertEquals("MMMXXX", actual.getField("CFICode"));
        Assert.assertEquals((Short)(short)0, actual.getField("PriceMethod"));
        Assert.assertEquals((Short)(short)0, actual.getField("PriceDisplayDecimals"));
        Assert.assertEquals((Long)1000000L, actual.getField("PriceFractionalDenominator"));
        Assert.assertEquals((Long)10000L, actual.getField("PriceMinimumTick"));
        Assert.assertEquals((Short)(short)2, actual.getField("Legs"));

		List<IMessage> legs = actual.getField("CombinationSymbolDirectoryLeg");
		Assert.assertEquals(6, legs.size());

		IMessage leg1 = legs.get(0);
        Assert.assertEquals((Long)1025L, leg1.getField("TradeableInstrumentId"));
		Assert.assertEquals("S", leg1.getField("Side"));
        Assert.assertEquals((Long)4L, leg1.getField("Ratio"));
        Assert.assertEquals((Long)0L, leg1.getField("Price"));

		IMessage leg2 = legs.get(1);
        Assert.assertEquals((Long)1028L, leg2.getField("TradeableInstrumentId"));
		Assert.assertEquals("S", leg2.getField("Side"));
        Assert.assertEquals((Long)1L, leg2.getField("Ratio"));
        Assert.assertEquals((Long)0L, leg2.getField("Price"));

		for (int i=2; i<6; i++) {
			IMessage leg = legs.get(i);
            Assert.assertEquals((Long)0L, leg.getField("TradeableInstrumentId"));
			Assert.assertEquals("", leg.getField("Side"));
            Assert.assertEquals((Long)0L, leg.getField("Ratio"));
            Assert.assertEquals((Long)0L, leg.getField("Price"));
		}
	}

    @Test
    public void testEncodeDecode() throws Exception {
        IoSession session = new DummySession();
        SOUPTcpCodec codec = (SOUPTcpCodec) messageHelper.getCodec(serviceContext);
        AbstractProtocolEncoderOutput output = new MockProtocolEncoderOutput();
        MockProtocolDecoderOutput decoderOutput = new MockProtocolDecoderOutput();

        IMessageFactory msgFactory = DefaultMessageFactory.getFactory();

        IMessage msg = msgFactory.createMessage("TcpTestMessage", "SOUP");
        msg.addField(MESSAGE_TYPE_FIELD, "1");    // 1
        msg.addField("String", "USER");         // 10
        msg.addField("ASCII", 111L);             // 10
        msg.addField("VARIABLE_1", "null_1");   // 7
        msg.addField("SignedInteger", 1);    // 4
        msg.addField("UnsignedInteger", 2L);  // 4
        msg.addField("UnsignedLong", 3L);     // 8
        msg.addField("VARIABLE_2", "null_2");   // 7
        msg.addField("SignedLong", 4L);         // 8
        msg.addField("VARIABLE_3", "");         // 1
        msg.addField("VARIABLE_4", "null_3");   // 7
        // header fields for proper comparison
        msg.addField("PacketLength", 68);
        msg.addField("PacketType", "U");

        IMessage msg_2 = msgFactory.createMessage("TcpTestMessage", "SOUP");
        msg_2.addField(MESSAGE_TYPE_FIELD, "1");    // 1
        msg_2.addField("String", "USER_1");       // 10
        msg_2.addField("ASCII", 222L);             // 10
        msg_2.addField("VARIABLE_1", "var_1");    // 6
        msg_2.addField("SignedInteger", -1);    // 4
        msg_2.addField("UnsignedInteger", 22L);  // 4
        msg_2.addField("UnsignedLong", 33L);     // 8
        msg_2.addField("VARIABLE_2", "variable_");// 10
        msg_2.addField("SignedLong", -4L);         // 8
        msg_2.addField("VARIABLE_3", "");         // 1
        msg_2.addField("VARIABLE_4", "null_4");   // 7
        // header fields for proper comparison
        msg_2.addField("PacketLength", 70);
        msg_2.addField("PacketType", "U");

        encode(session, codec, output, msg, msg_2);
        Assert.assertEquals(2, output.getMessageQueue().size());
        decode(output.getMessageQueue(), session, codec, decoderOutput);

        Assert.assertEquals(4, decoderOutput.getMessageQueue().size());

        IMessage seqDataPacket = msgFactory.createMessage("UnsequencedDataPacket", "SOUP");
        seqDataPacket.addField("PacketLength", 68);
        seqDataPacket.addField("PacketType", "U");
        IMessage seqDataPacket_2 = msgFactory.createMessage("UnsequencedDataPacket", "SOUP");
        seqDataPacket_2.addField("PacketLength", 70);
        seqDataPacket_2.addField("PacketType", "U");

        IMessage msg_header = msg.getField(SOUPMessageHelper.UNSEQUENCED_HEADER_MESSAGE);
        Assert.assertNotNull(msg_header);
        Assert.assertEquals((int)msg_header.getField(SOUPMessageHelper.PACKET_LENGTH), 68);
        Assert.assertEquals(msg_header.getField(SOUPMessageHelper.PACKET_TYPE), "U");
        msg.removeField(SOUPMessageHelper.UNSEQUENCED_HEADER_MESSAGE);

        IMessage msg_2_header = msg_2.getField(SOUPMessageHelper.UNSEQUENCED_HEADER_MESSAGE);
        Assert.assertNotNull(msg_2_header);
        Assert.assertEquals((int)msg_2_header.getField(SOUPMessageHelper.PACKET_LENGTH), 70);
        Assert.assertEquals(msg_2_header.getField(SOUPMessageHelper.PACKET_TYPE), "U");
        msg_2.removeField(SOUPMessageHelper.UNSEQUENCED_HEADER_MESSAGE);

        compare(seqDataPacket, (IMessage) decoderOutput.getMessageQueue().poll());
        compare(msg, (IMessage) decoderOutput.getMessageQueue().poll());
        compare(seqDataPacket_2, (IMessage) decoderOutput.getMessageQueue().poll());
        compare(msg_2, (IMessage) decoderOutput.getMessageQueue().poll());
    }

    @Test
    public void testDecodeWithIncorrectDictionaryStructure() throws Exception {
        byte[] raw = {
                // TCP Soup BIN:
                0x00, 26, // Length
                0x53, // PacketType = 'S' - SequencedMessage
                // Body
                0x37, // MessageType = '7' - OrderDeleted
                // Message Payload
                0x00, 0x00, 0x00, 0x03, // Timestamp = 3
                0x00, 0x04, // TradeDate = 4
                0x00, 0x00, 0x00, 0x05, // TradeableInstrumentId = 5
                0x53, // Side = 'S' = Sell
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x06, // OrderID = 6
                0x77, 0x77, 0x77, 0x77, 0x77 // String wwwww (expected length in dict 10 but actual only 5)

        };
        SOUPTcpCodec codec = (SOUPTcpCodec) messageHelper.getCodec(serviceContext);

        // Decode
        IoBuffer toDecode = IoBuffer.wrap(raw);
        IoSession decodeSession = new DummySession();

        AbstractProtocolDecoderOutput decoderOutput = new MockProtocolDecoderOutput();
        codec.decode(decodeSession, toDecode, decoderOutput);
        Assert.assertEquals(raw.length, toDecode.position());

        Assert.assertEquals(2, decoderOutput.getMessageQueue().size());

        // SequencedMessage
        IMessage actual = (IMessage) decoderOutput.getMessageQueue().poll();
        Assert.assertEquals("SequencedDataPacket", actual.getName());
        Assert.assertEquals((Integer)26, actual.getField("PacketLength"));
        Assert.assertEquals("S", actual.getField("PacketType"));

        // OrderDeleted
        actual = (IMessage)decoderOutput.getMessageQueue().poll();
        Assert.assertTrue(actual.getMetaData().isRejected());

        // check norma fields
        Assert.assertEquals("IncorrectOrderDeleted", actual.getName());
        Assert.assertEquals("7", actual.getField(MESSAGE_TYPE_FIELD));
        Assert.assertEquals((Long)3L, actual.getField("Timestamp"));
        Assert.assertEquals((Integer)4, actual.getField("TradeDate"));
        Assert.assertEquals((Long)5L, actual.getField("TradeableInstrumentId"));
        Assert.assertEquals("S", actual.getField("Side"));
        Assert.assertEquals((Long)6L, actual.getField("OrderId"));
    }

    @Ignore("Manual testing")
    @Test
    public void testManual() throws Exception {
        IoBuffer buffer = IoBuffer.allocate(0)
                .setAutoExpand(true);
        int bufSize = 1024;
        byte[] buf = new byte[bufSize];
        try (InputStream stream = Files.newInputStream(Paths.get("file name"), StandardOpenOption.READ)) {
            int read;
            while ((read = stream.read(buf)) > 0) {
                if (read == bufSize) {
                    buffer.put(buf);
                } else {
                    buffer.put(buf, 0, read);
                }
            }
        }
        buffer.flip();

        SOUPCodec codec = (SOUPCodec) messageHelper.getCodec(serviceContext);
        IoSession decodeSession = new DummySession();
        AbstractProtocolDecoderOutput decoderOutput = new MockProtocolDecoderOutput();

        int bufPos = 0;
        try {
            // Because we need to check the buffer position after each processed segment
            while (codec.doDecodeInternal(decodeSession, buffer, decoderOutput)) {
                bufPos = buffer.position();
                while (!decoderOutput.getMessageQueue().isEmpty()) {
                    IMessage poll = (IMessage)decoderOutput.getMessageQueue().poll();
                    System.out.println("Message " + poll.getName() + ": " + poll);
                    if (poll.getMetaData().isRejected()) {
                        System.out.println(poll.getMetaData().getRejectReason());
                    }
                }
            }
        } finally {
            System.out.println("bufPos = " + bufPos);
            System.out.println("buffer.remaining() = " + buffer.remaining());
        }
    }

    private void encode(IoSession session, SOUPTcpCodec codec, ProtocolEncoderOutput output, IMessage... messages) throws Exception {
        for (IMessage message : messages) {
            // message = messageHelper.prepareMessageToEncode(message, null);
            codec.encode(session, message, output);
        }
    }

    private void decode(Queue<Object> msgQueue, IoSession session, SOUPTcpCodec codec, AbstractProtocolDecoderOutput output) throws Exception {
        IoBuffer all = (IoBuffer)msgQueue.poll();
        all.setAutoExpand(true);
        all.position(all.remaining());
        while (!msgQueue.isEmpty()) {
            IoBuffer buffer = (IoBuffer)msgQueue.poll();
            all.put(buffer);
        }
        all.position(0);
        codec.decode(session, all, output);
        Assert.assertEquals(false, all.hasRemaining());
    }

    private void compare(IMessage msg, IMessage result) {
        ComparatorSettings settings = new ComparatorSettings();

        ComparisonResult comparisonResult = MessageComparator.compare(msg, result, settings);

        Assert.assertNotNull(comparisonResult);

        checkComparisonResult(comparisonResult);
    }

    private void checkComparisonResult(ComparisonResult comparisonResult) {
        StatusType status = comparisonResult.getStatus();

        if(status == null) {
            if(comparisonResult.hasResults()) {
                for(ComparisonResult subResult : comparisonResult) {
                    checkComparisonResult(subResult);
                }
            } else {
                Assert.fail("Status of the ComparisonResult ["
                        + comparisonResult +"] is null and it doesn't have subComparisons");
            }
        } else {
            Assert.assertEquals(StatusType.PASSED, status);
        }
    }
    
    private static class MessageData {
        private final IMessageFactory msgFactory;

        private MessageData(IMessageFactory msgFactory) {
            this.msgFactory = msgFactory;
        }

        private byte[] getLoginHeaderRaw() {
            return  new byte[]{
                    0x00, 19, // Length
                    0x41, // 'A' LoginAcceptedPacket
            };
        }

        private byte[] getLoginBodyRaw() {
            return  new byte[]{
                    0x42, 0x42, 0x42, 0x42, 0x42, 0x42, 0x42, 0x42, 0x42, 0x42, // SessionID
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, // SequenceNumber = 1
            };
        }
        
        private byte[] getHearbeatRaw() {
            return  new byte[]{
                    0x00, 0x01, // Length
                    0x48, // PacketType = 'H' - ServerHeartbeatPackets
            };
        }

        private byte[] getOrderDeletedHeaderRaw() {
            return new byte[]{
                    0x00, 21, // Length
                    0x53, // PacketType = 'S' - SequencedMessage
            };
        }

        private byte[] getOrderDeletedRaw() {
            return new byte[]{
                    // Body
                    0x44, // MessageType = 'D' - OrderDeleted
                    // Message Payload
                    0x00, 0x00, 0x00, 0x03, // Timestamp = 3
                    0x00, 0x04, // TradeDate = 4
                    0x00, 0x00, 0x00, 0x05, // TradeableInstrumentId = 5
                    0x53, // Side = 'S' = Sell
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x06 // OrderID = 6
            };
        }
        
        private IMessage getLoginAcceptedHeader() {
            IMessage soupBinHeader = msgFactory.createMessage(SOUP_BIN_TCP_HEADER_NAME, "SOUP");
            soupBinHeader.addField(PACKET_LENGTH, 19);
            soupBinHeader.addField(PACKET_TYPE, 'A');
            return soupBinHeader;
        }
        
        private IMessage getHeartbeatHeader() {
            IMessage soupBinHeader = msgFactory.createMessage(SOUP_BIN_TCP_HEADER_NAME, "SOUP");
            soupBinHeader.addField(PACKET_LENGTH, 1);
            soupBinHeader.addField(PACKET_TYPE, 'H');
            return soupBinHeader;
        }
        
        private IMessage getOrderDeletedHeader() {
            IMessage soupBinHeader = msgFactory.createMessage(SOUP_BIN_TCP_HEADER_NAME, "SOUP");
            soupBinHeader.addField(PACKET_LENGTH, 21);
            soupBinHeader.addField(PACKET_TYPE, 'S');
            return soupBinHeader;
        }

        private IMessage getOrderDeleted() {
            IMessage seqHeader = msgFactory.createMessage(SEQUENCED_DATA_PACKET, "SOUP");
            seqHeader.addField(PACKET_LENGTH, 21);
            seqHeader.addField(PACKET_TYPE, "S");
            IMessage orderDeleted = msgFactory.createMessage("OrderDeleted", "SOUP");
            orderDeleted.addField(PACKET_LENGTH, 21);
            orderDeleted.addField(PACKET_TYPE, "S");
            orderDeleted.addField(MESSAGE_TYPE_FIELD, "D");
            orderDeleted.addField(SEQUENCED_HEADER_MESSAGE, seqHeader);
            orderDeleted.addField("Timestamp", 3L);
            orderDeleted.addField("TradeDate", 4);
            orderDeleted.addField("TradeableInstrumentId", 5L);
            orderDeleted.addField("Side", "S");
            orderDeleted.addField("OrderId", 6L);
            return orderDeleted;
        }
        
        private IMessage getLoginAccepted() {
            IMessage loginAccepted = msgFactory.createMessage("LoginAcceptedPacket", "SOUP");
            loginAccepted.addField(PACKET_LENGTH, 19);
            loginAccepted.addField(PACKET_TYPE, "A");
            loginAccepted.addField("SessionID", "BBBBBBBBBB");
            loginAccepted.addField("SequenceNumber", 1L);
            return loginAccepted;
        }
    }

    private Queue<Object> decodeWithSeparateHeader(IoBuffer data) throws Exception {
        IoSession decodeSession = new DummySession();
        SoupTcpCodecSettings codecsSettings = new SoupTcpCodecSettings();
        codecsSettings.setParseHeaderAsSeparateMessage(true);
        SOUPTcpCodec codec = (SOUPTcpCodec) messageHelper.getCodec(serviceContext);
        codec.init(serviceContext, codecsSettings, msgFactory, dictionary);
        MockProtocolDecoderOutput decoderOutput = new MockProtocolDecoderOutput();
        codec.decode(decodeSession, data, decoderOutput);
        return decoderOutput.getMessageQueue();
    }

    private void compare(IMessage expected, Object actual, byte[] expectedRaw) {
        Assert.assertTrue(actual instanceof IMessage);
        AbstractTest.equals( expected, (IMessage)actual);
        Assert.assertArrayEquals(expectedRaw, ((IMessage) actual).getMetaData().getRawMessage());
    }
}
