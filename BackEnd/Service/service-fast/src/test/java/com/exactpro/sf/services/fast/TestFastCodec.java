/******************************************************************************
 * Copyright 2009-2018 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.services.fast;

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.DefaultWriteFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.DummySession;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.AbstractProtocolEncoderOutput;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.exactpro.sf.center.impl.CoreVersion;
import com.exactpro.sf.common.impl.messages.DefaultMessageFactory;
import com.exactpro.sf.common.impl.messages.MapMessage;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.util.HexDumper;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.services.MockProtocolDecoderOutput;
import com.exactpro.sf.util.FASTServicePluginTest;

public class TestFastCodec extends FASTServicePluginTest {

    private IoSession session;
    private static final String TEMPLATE_TITLE = "FAST_2";
    private static final String CORE_ALIAS = new CoreVersion().getAlias();
	private static final SailfishURI DICTIONARY_URI = SailfishURI.unsafeParse(CORE_ALIAS + ":" + TEMPLATE_TITLE);

    @Before
    public void init() {
        this.session = new DummySession();
    }

    @Test
	public void testEncodeMessage() throws Exception
	{

		FASTCodec codec = getCodec(DICTIONARY_URI, TEMPLATE_TITLE);

		ProtocolEncoderOutput output = new MockProtocolEncoderOutput();

		IMessage message = new MapMessage("OEquity", "Logon");

		message.addField("Password", "tnp123");
		message.addField("Username", "MADTY0" );
		message.addField("SendingTime", "20120101-01:01:01.333");
		message.addField("ApplID", "0");
		message.addField("MessageType", "0");

		session.write(message);
		codec.encode(session, message, output);
		Queue<Object> msgQueue = ((AbstractProtocolEncoderOutput)output).getMessageQueue();
		Object lastMessage = msgQueue.element();
		byte[] asd = ((IoBuffer)lastMessage).array();
		int limit = ((IoBuffer)lastMessage).limit();
		byte[] bytes = Arrays.copyOf(asd, limit );

		session.write(lastMessage);
		System.out.println(HexDumper.getHexdump(bytes));
	}

    // AddOrder
	// ,0x2E ,0x00 ,0x01 ,0x31 ,0x10 ,0x05 ,0x00 ,0x00 ,0x26 ,0x41 ,0xE0 ,0x98 ,0xC3 ,0x22 ,0x0E ,0x00 ,0x00 ,0x40 ,0xC4 ,0xD4 ,0x57 ,0x01 ,0x42 ,0xE8 ,0x03 ,0x00 ,0x00 ,0x00 ,0x00 ,0x00 ,0x00 ,0x3D ,0xE6 ,0x46 ,0x00 ,0x00 ,0x00 ,0x00 ,0x4E ,0x72 ,0x53 ,0x00 ,0x00 ,0x00 ,0x00 ,0x00

	@Test
	public void testDecodeMessage() throws Exception
	{
		FASTCodec codec = getCodec(DICTIONARY_URI, TEMPLATE_TITLE);
		int[] array = new int[]{
				0xA8, 0xC0, 0x81, 0xB0, 0x32, 0x30, 0x31, 0x32,
				0x30, 0x31, 0X30, 0X31, 0x2D, 0x30, 0x31, 0x3A,
				0x30, 0x31, 0x3A, 0x30, 0x31, 0x2E, 0x33, 0x33,
				0xB3, 0xB0, 0x80, 0x4D, 0x41, 0x44, 0x54, 0x59,
				0xB0, 0x74, 0x6E, 0x70, 0x31, 0x32, 0xB3, 0x80,
				0x80};
		byte[] b = new byte[array.length];
		for (int i=0; i<array.length; i++)
		{
			b[i] = (byte) array[i];
		}

		IoBuffer toDecode = IoBuffer.wrap( b );
		toDecode.order(ByteOrder.LITTLE_ENDIAN);
		toDecode.position(0);

		IoSession decodeSession = new DummySession();

		MockProtocolDecoderOutput decoderOutput = new MockProtocolDecoderOutput();
		boolean decodableResult = codec.doDecode( decodeSession, toDecode, decoderOutput );
		System.out.println(decoderOutput.getMessageQueue().element());

		Assert.assertTrue( "Decoding error.", decodableResult);
	}

	@Test
    public void testDecodeFastMessage() throws Exception {

        String dictName = "FAST";
        SailfishURI dictUri = SailfishURI.unsafeParse(CORE_ALIAS + ":" + dictName);
        FASTCodec codec = getCodec(dictUri, dictName);

        byte[] sourceArray = new byte[] { (byte) 0xab, (byte) 0xc0, (byte) 0x83, 0x42, (byte) 0xd7, 0x32, 0x30, 0x31, 0x36, 0x30, 0x32, 0x31, 0x30,
                0x2d, 0x30, 0x37, 0x3a, 0x31, 0x30, 0x3a, 0x30, 0x36, 0x2e, 0x31, 0x39, (byte) 0xb3, 0x36, 0x30, 0x36, 0x36, 0x36, (byte) 0xb0,
                (byte) 0x80, (byte) 0x82, (byte) 0x82, (byte) 0xc0, (byte) 0x87, 0x54, 0x4d, (byte) 0xd0, 0x21, (byte) 0xe9, 0x21, (byte) 0xee};

        IoBuffer toDecode = IoBuffer.wrap(sourceArray);
        toDecode.order(ByteOrder.LITTLE_ENDIAN);
        toDecode.position(0);

        IoSession decodeSession = new DummySession();

        MockProtocolDecoderOutput decoderOutput = new MockProtocolDecoderOutput();
        boolean decodableResult = codec.doDecode(decodeSession, toDecode, decoderOutput);
        Assert.assertTrue("Decoding error.", decodableResult);

        IMessage message = DefaultMessageFactory.getFactory().createMessage("ApplicationMessageRequest", "fast");
        message.addField("MsgType", "BW");
        message.addField("SendingTime", "20160210-07:10:06.193");
        message.addField("ApplReqID", "606660");
        message.addField("ApplReqType", 0L);
        message.addField("NoApplIDs", 1L);
        List<IMessage> list = new ArrayList<>();
        IMessage subMessage = DefaultMessageFactory.getFactory().createMessage("ApplicationMessageRequest_IndicesRequestEntries", "fast");
        subMessage.addField("RefApplID", "TMP");
        subMessage.addField("Reserved1", 6L);
        subMessage.addField("ApplBegSeqNum", 4328L);
        subMessage.addField("ApplEndSeqNum", 4333L);

        list.add(subMessage);
        message.addField("IndicesRequestEntries", list);

        AbstractProtocolEncoderOutput output = new MockProtocolEncoderOutput();

        session.write(message);
        codec.encode(session, message, output);
        Queue<Object> msgQueue = output.getMessageQueue();
        Object lastMessage = msgQueue.element();
        byte[] asd = ((IoBuffer) lastMessage).array();
        int limit = ((IoBuffer) lastMessage).limit();
        byte[] bytes = Arrays.copyOf(asd, limit);

        session.write(lastMessage);
        Assert.assertArrayEquals("Compare source and encoded\n" + HexDumper.getHexdump(bytes), sourceArray, bytes);
    }

	@Test
	public void testDecodeSeveralMessages() throws Exception
	{
		FASTCodec codec = getCodec(DICTIONARY_URI, TEMPLATE_TITLE);
		int[] array = new int[]{
				0xA8, 0xC0, 0x81, 0xB0, 0x32, 0x30, 0x31, 0x32,
				0x30, 0x31, 0X30, 0X31, 0x2D, 0x30, 0x31, 0x3A,
				0x30, 0x31, 0x3A, 0x30, 0x31, 0x2E, 0x33, 0x33,
				0xB3, 0xB0, 0x80, 0x4D, 0x41, 0x44, 0x54, 0x59,
				0xB0, 0x74, 0x6E, 0x70, 0x31, 0x32, 0xB3, 0x80,
				0x80,

				0xA8, 0xC0, 0x81, 0xB0, 0x32, 0x30, 0x31, 0x32,
				0x30, 0x31, 0X30, 0X31, 0x2D, 0x30, 0x31, 0x3A,
				0x30, 0x31, 0x3A, 0x30, 0x31, 0x2E, 0x33, 0x33,
				0xB3, 0xB0, 0x80, 0x4D, 0x41, 0x44, 0x54, 0x59,
				0xB0, 0x74, 0x6E, 0x70, 0x31, 0x32, 0xB3, 0x80,
				0x80
		};

		byte[] b = new byte[array.length];
		for (int i=0; i<array.length; i++)
		{
			b[i] = (byte) array[i];
		}

		IoBuffer toDecode = IoBuffer.wrap( b );
		toDecode.order(ByteOrder.LITTLE_ENDIAN);
		toDecode.position(0);

		IoSession decodeSession = new DummySession();

		MockProtocolDecoderOutput decoderOutput = new MockProtocolDecoderOutput();
		boolean decodableResult = codec.doDecode( decodeSession, toDecode, decoderOutput );
		System.out.println(decoderOutput.getMessageQueue().element());
		System.out.println(decoderOutput.getMessageQueue().element());

		Assert.assertTrue( "Decoding error.", decodableResult);
		System.out.println("position = "+toDecode.position());
	}

	@Test
	public void testEncodeDecodeMessage() throws Exception
	{
		FASTCodec codec = getCodec(DICTIONARY_URI, TEMPLATE_TITLE);
		ProtocolEncoderOutput output = new MockProtocolEncoderOutput();

		IMessage message = new MapMessage("OEquity", "Logon");

        message.addField("Password", "tnp123");
        message.addField("Username", "MADTY0" );
        message.addField("SendingTime", "20120101-01:01:01.333");
        message.addField("ApplID", "0");
        message.addField("MessageType", "0");

		session.write(message);
		codec.encode(session, message, output);
		Queue<Object> msgQueue = ((AbstractProtocolEncoderOutput)output).getMessageQueue();
		Object lastMessage = msgQueue.element();
		byte[] asd = ((IoBuffer)lastMessage).array();
		int limit = ((IoBuffer)lastMessage).limit();
		byte[] bytes = Arrays.copyOf(asd, limit );

		session.write(lastMessage);
		System.out.println(HexDumper.getHexdump(bytes));
	}

	@Test
	public void testRecoveryFromBadMessage() throws Exception
	{
		FASTCodec codec = getCodec(DICTIONARY_URI, TEMPLATE_TITLE);
		int[] array1 = new int[]{
				0xA8, 0xC0, 0x81, 0xB0, 0x32, 0x30, 0x31, 0x32,
				0x30, 0x31, 0X30, 0X31, 0x2D, 0x30, 0x31, 0x3A,
				0x30, 0x31, 0x3A, 0x30, 0x31, 0x2E, 0x33, 0x33,
		};

		int[] array2 = new int[]{
				0xA8, 0xC0, 0x81, 0xB0, 0x32, 0x30, 0x31, 0x32,
				0x30, 0x31, 0X30, 0X31, 0x2D, 0x30, 0x31, 0x3A,
				0x30, 0x31, 0x3A, 0x30, 0x31, 0x2E, 0x33, 0x33,
				0xB3, 0xB0, 0x80, 0x4D, 0x41, 0x44, 0x54, 0x59,
				0xB0, 0x74, 0x6E, 0x70, 0x31, 0x32, 0xB3, 0x80,
				0x80
		};

		byte[] b = new byte[array1.length];
		for (int i=0; i<array1.length; i++)
		{
			b[i] = (byte) array1[i];
		}

		IoBuffer toDecode = IoBuffer.wrap( b );
		toDecode.order(ByteOrder.LITTLE_ENDIAN);
		toDecode.position(0);

		IoSession decodeSession = new DummySession();

		MockProtocolDecoderOutput decoderOutput = new MockProtocolDecoderOutput();
		boolean decodableResult = codec.doDecode( decodeSession, toDecode, decoderOutput );
		Assert.assertFalse( "Decoding error.", decodableResult);

		b = new byte[array2.length];
		for (int i=0; i<array2.length; i++)
		{
			b[i] = (byte) array2[i];
		}

		toDecode = IoBuffer.wrap( b );
		decodableResult = codec.doDecode( decodeSession, toDecode, decoderOutput );

		System.out.println(decoderOutput.getMessageQueue().element());

		Assert.assertTrue( "Decoding error.", decodableResult);
		System.out.println("position = "+toDecode.position());
	}

	 @Test
	 public void testWrongTemplate() {
	   	FASTClientSettings settings = new FASTClientSettings();
		IMessageFactory msgFactory = DefaultMessageFactory.getFactory();
		FASTCodec codec = new FASTCodec();
		try{
			codec.init(serviceContext, settings, msgFactory, null);
			Assert.fail("dictionary parameter cannot ne null");
		}catch(NullPointerException e){
			Assert.assertEquals("'dictionary' parameter cannot be null",e.getMessage());
		}
	 }

	class MockProtocolEncoderOutput extends AbstractProtocolEncoderOutput
	{
		@Override
		public WriteFuture flush()
		{
			return new DefaultWriteFuture(session);
		}
	}

}
