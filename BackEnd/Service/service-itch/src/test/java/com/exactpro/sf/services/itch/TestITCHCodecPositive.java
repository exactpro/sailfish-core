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
package com.exactpro.sf.services.itch;

import com.exactpro.sf.common.impl.messages.MapMessage;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.services.MockProtocolDecoderOutput;
import com.exactpro.sf.util.DateTimeUtility;
import com.exactpro.sf.util.TestITCHHelper;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.DummySession;
import org.apache.mina.core.session.IoSession;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestITCHCodecPositive extends TestITCHHelper {

    private static final Logger logger = LoggerFactory.getLogger(TestITCHCodecPositive.class);

	@BeforeClass
	public static void setUpClass(){
		logger.info("Start positive tests of ITCH codec");
	}

	@AfterClass
	public static void tearDownClass(){
		logger.info("Finish positive tests of ITCH codec");
	}

	/**
     * Encode and decode AddOrder message with ITCHPreprocessor. Encode and decode OrderExecuted after and compare
	 * AddOrder's InstrumntID field from original message and OrderExecuted's FakeInstrumentID field from result message
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testITCHPreprocessor(){
		try{
			IMessage messageAddOrder = getMessageCreator().getAddOrder();

            ITCHCodec codec = new ITCHCodec();
            ITCHCodecSettings settings = new ITCHCodecSettings();
            settings = new ITCHCodecSettings();
            settings.setMsgLength(1);
            settings.setDictionaryURI(SailfishURI.unsafeParse("ITCH"));
            codec.init(serviceContext, settings, getMessageHelper().getMessageFactory(), getMessageHelper().getDictionaryStructure());

            messageAddOrder = getMessageHelper().prepareMessageToEncode(messageAddOrder, null);
			IMessage decodedMessage= decode(encode(messageAddOrder,null),codec);

			IMessage orderExecuted = getMessageCreator().getOrderExecuted();
			orderExecuted = getMessageHelper().prepareMessageToEncode(orderExecuted, null);
			decodedMessage=decode(encode(orderExecuted,null),codec);
			Assert.assertTrue( "Object must be instance of IMessage.", (decodedMessage instanceof IMessage) );
		    Assert.assertTrue( "Object must be instance of MapMessage.", (decodedMessage instanceof MapMessage) );

            List<IMessage> listResult = (List<IMessage>) decodedMessage.getField(ITCHMessageHelper.SUBMESSAGES_FIELD_NAME);
            List<IMessage> original = (List<IMessage>) messageAddOrder.getField(ITCHMessageHelper.SUBMESSAGES_FIELD_NAME);
		    Assert.assertEquals(2, listResult.size());
		    Assert.assertEquals(2, original.size());
            Assert.assertEquals(original.get(1).<Object>getField("InstrumentID"), listResult.get(1).<Object>getField("FakeInstrumentID"));
	    }catch(Exception e){
	    	logger.error(e.getMessage(),e);
	    	Assert.fail(e.getMessage());
	    }
	}


	/**
	 * Try to encode and decode UnitHeader message. Compare original and result message.
	 */
	@Test
	public void testEncodeDecodeUnitHeader(){
		IMessage messageHeader = getUnitHeader((short)0);
		try{
	    	IMessage decodedMessage=decode(encode(messageHeader,null),null);
		    Assert.assertTrue( "Object must be instance of IMessage.", (decodedMessage instanceof IMessage) );
		    Assert.assertTrue( "Object must be instance of MapMessage.", (decodedMessage instanceof MapMessage) );
		    @SuppressWarnings("unchecked")
            IMessage resultUnitHeader = ((List<IMessage>) decodedMessage.getField(ITCHMessageHelper.SUBMESSAGES_FIELD_NAME)).get(0);
		    Assert.assertTrue("UnitHeader messages must be equal. Original message:"+messageHeader+"; \n"
		    		+ "Result message:"+resultUnitHeader, resultUnitHeader.compare(messageHeader));
	    }catch(Exception e){
	    	logger.error(e.getMessage(),e);
	    	Assert.fail(e.getMessage());
	    }
	}

	/**
	 * Try to encode and decode list of messages: UnitHeader, Time, AddOrderOneByteLength. Compare result and check
	 * UnitHeader, Seconds in Time message and Nanosecond in AddOrderOneByteLength message.
	 * Message create without prepareMessageToEncode(...) method.
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testEncodeDecodeListMessages(){
		IMessage messageHeader = getUnitHeader((short)2);
		long seconds = 60 * 4 + 8;
		long nanoseconds=10L;
	    IMessage messageTime = getMessageCreator().getTime(seconds);
	    IMessage messageAddOrder = getMessageCreator().getAddOrderOneByteLength(nanoseconds);
	    List<IMessage> list = new ArrayList<>();
	    list.add(messageHeader);
	    list.add(messageTime);
	    list.add(messageAddOrder);
	    IMessage messageList = getMessageCreator().getMessageList(list);
	    try{
	    	IMessage decodedMessage=decode(encode(messageList,null),null);
		    Assert.assertTrue( "Object must be instance of IMessage.", (decodedMessage instanceof IMessage) );
		    Assert.assertTrue( "Object must be instance of MapMessage.", (decodedMessage instanceof MapMessage) );

            List<IMessage> listResult = (List<IMessage>) decodedMessage.getField(ITCHMessageHelper.SUBMESSAGES_FIELD_NAME);
            List<IMessage> original = (List<IMessage>) messageList.getField(ITCHMessageHelper.SUBMESSAGES_FIELD_NAME);
		    Assert.assertEquals(3, listResult.size());
		    Assert.assertEquals(3, original.size());
		    Assert.assertTrue("UnitHeader messages must be equal. Original message:"+original.get(0)+"; \n"
		    		+ "Result message:"+listResult.get(0), listResult.get(0).compare(original.get(0)));
		    Assert.assertTrue("Field \"Seconds\" can not be null.",listResult.get(1).getField("Seconds")!=null);
		    Assert.assertTrue("Field \"Seconds\" must be equal. Original message:"+original.get(1)+"; \n"
		    		+ "Result message:"+listResult.get(1), original.get(1).getField("Seconds").
		    			equals(listResult.get(1).getField("Seconds")));
		    Assert.assertTrue("Field \"Nanosecond\" can not be null.",listResult.get(2).getField("Nanosecond")!=null);
		    Assert.assertTrue("Field \"Nanosecond\" must be equal. Original message:"+original.get(2)+"; \n"
		    		+ "Result message:"+listResult.get(2), original.get(2).getField("Nanosecond").
		    			equals(listResult.get(2).getField("Nanosecond")));
	    }catch(Exception e){
	    	logger.error(e.getMessage(),e);
	    	Assert.fail(e.getMessage());
	    }
	}

	/**
	 * Try to encode and decode message SecurityClassTickMatrix with 2 TicksGroup's messages. Compare result and check
	 * UnitHeader and both TicksGroup's messages. Message create with prepareMessageToEncode(...) method.
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testEncodeDecodeSecurityClassTickMatrix(){
		List<IMessage> groups = new ArrayList<>();
		IMessage group = getMessageCreator().getTicksGroup(0.0,0.0,0.0);
		groups.add(group);
		group = getMessageCreator().getTicksGroup(1.0,1.0,1.0);
		groups.add(group);
        IMessage message = getMessageCreator().getSecurityClassTickMatrix(groups);
		IMessage messageList = getMessageHelper().prepareMessageToEncode(message, null);
		try{
			IMessage decodedMessage=decode(encode(messageList,null),null);
			Assert.assertTrue( "Object must be instance of IMessage.", (decodedMessage instanceof IMessage) );
		    Assert.assertTrue( "Object must be instance of MapMessage.", (decodedMessage instanceof MapMessage) );

            List<IMessage> listResult = (List<IMessage>) decodedMessage.getField(ITCHMessageHelper.SUBMESSAGES_FIELD_NAME);
            List<IMessage> original = (List<IMessage>) messageList.getField(ITCHMessageHelper.SUBMESSAGES_FIELD_NAME);
		    Assert.assertEquals(2, listResult.size());
		    Assert.assertEquals(2, original.size());
		    Assert.assertTrue("UnitHeader messages must be equal. Original message:"+original.get(0)+"; \n"
		    		+ "Result message:"+listResult.get(0), listResult.get(0).compare(original.get(0)));
		    List<IMessage> ticksGroupOriginal=(List<IMessage>)original.get(1).getField("TicksGroup");
		    List<IMessage> ticksGroupResult=(List<IMessage>)listResult.get(1).getField("TicksGroup");
		    Assert.assertTrue("First TicksGroup messages must be equal. Original message:"+ticksGroupOriginal.get(0)+"; \n"
		    		+ "Result message:"+ticksGroupResult.get(0), ticksGroupOriginal.get(0).compare(ticksGroupResult.get(0)));
		    Assert.assertTrue("Second TicksGroup messages must be equal. Original message:"+ticksGroupOriginal.get(1)+"; \n"
		    		+ "Result message:"+ticksGroupResult.get(1), ticksGroupOriginal.get(1).compare(ticksGroupResult.get(1)));
		}catch(Exception e){
			logger.error(e.getMessage(),e);
			Assert.fail(e.getMessage());
		}
	}

	/**
	 * Try to encode LoginRequest and print result bytes into console.
	 */
	@Ignore@Test
	public void testEncodeMessageLoginRequest(){
		try{
			IMessage list = getMessageHelper().prepareMessageToEncode(getMessageCreator().getLoginRequest(), null);
			Object lastMessage = encode(list,null);
	        byte[] asd = ((IoBuffer)lastMessage).array();
	        int limit = ((IoBuffer)lastMessage).limit();
	        byte[] bytes = Arrays.copyOf(asd, limit );
	        for(int i=0;i<bytes.length;i++)
	        	System.out.println(bytes[i]);
		}catch(Exception e){
			logger.error(e.getMessage(),e);
			Assert.fail(e.getMessage());
		}
	}

	/**
	 * Try to encode SymbolDirectory and print result bytes into console.
	 */
	@Ignore@Test
	public void testEncodeMessageSymbolDirectory(){
		try{
			IMessage addOrder = getMessageCreator().getAddOrder();
			IMessage list = getMessageHelper().prepareMessageToEncode(addOrder, null);
			Object lastMessage = encode(list,null);
			byte[] asd = ((IoBuffer)lastMessage).array();
			int limit = ((IoBuffer)lastMessage).limit();
			byte[] bytes = Arrays.copyOf(asd, limit );
			for(int i=0;i<bytes.length;i++)
	        	System.out.println(bytes[i]);
		}catch(Exception e){
			logger.error(e.getMessage(),e);
			Assert.fail(e.getMessage());
		}
	}

	/**
	 * Try to decode AddOrderOneByteLength from byte[] and print result message into console.
	 */
	@Ignore@Test
	public void testDecodeMessageAddOrderOneByteLength(){
        ITCHCodec codec = (ITCHCodec) getMessageHelper().getCodec(serviceContext);
        int[] array = new int[]
             {0x2E, 0x00, 0x01, 0x31, 0x10, 0x05, 0x00, 0x00, 0x2E, 0x41, 0xE0, 0x98, 0xC3, 0x22, 0x0E, 0x00,
              0x00, 0x40, 0xC4, 0xD4, 0x57, 0x01, 0x42, 0xE8, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x3D,
              0xE6, 0x46, 0x00, 0x00, 0x00, 0x00, 0x4E, 0x72, 0x53, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
              0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
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
		try{
			boolean decodableResult = codec.doDecode( decodeSession, toDecode, decoderOutput );
			System.out.println((IMessage) decoderOutput.getMessageQueue().element());
			Assert.assertTrue( "Decoding error.", decodableResult);
		}catch(Exception e){
			logger.error(e.getMessage(),e);
			Assert.fail(e.getMessage());
		}
	}

	/**
	 * Try to decode SymbolDirectory from byte[] and print result message into console.
	 */
	@Ignore@Test
	public void testDecodeMessageSymbolDirectory(){
        ITCHCodec codec = (ITCHCodec) getMessageHelper().getCodec(serviceContext);
		int[] array = new int[]{
				0x64, 0x00, // Size
				0x01,       // Count
				0x31,       // MD Group
				0x00, 0x00, 0x00, 0x00, // SeqNumber
				0x5C, 0x52, 0xF0, 0x9D, 0xE6, 0x2B, 0xA4, 0x42, 0x0F, 0x00, 0x00, 0x00, 0x20, 0x49, 0x54, 0x31, 0x30, 0x30, 0x31, 0x30, 0x30, 0x20, 0x20, 0x20, 0x20, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x54, 0x41, 0x48, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x45, 0x55, 0x52, 0x00, 0x20, 0x20, 0x20, 0x20, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x00, 0x00, 0x00, 0x00, 0x00, 0x64, 0x00, 0x01, 0x31, 0x00, 0x00, 0x00, 0x00, 0x5C, 0x52, 0xF8, 0xE9, 0xE7, 0x2B, 0xA9, 0x42, 0x0F, 0x00, 0x00, 0x00, 0x20, 0x49, 0x54, 0x30, 0x30, 0x30, 0x30, 0x30, 0x36, 0x32, 0x30, 0x37, 0x32, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x54, 0x41, 0x48, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x45, 0x55, 0x52, 0x00, 0x20, 0x20, 0x20, 0x20, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x47, 0x45, 0x4E, 0x45, 0x52, 0x41, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x00, 0x00, 0x00, 0x00, 0x00};
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
		try{
			boolean decodableResult = codec.doDecode( decodeSession, toDecode, decoderOutput );
			System.out.println((IMessage) decoderOutput.getMessageQueue().element());
			Assert.assertTrue( "Decoding error.", decodableResult);
			System.out.println("position = "+toDecode.position());
		}catch(Exception e){
			logger.error(e.getMessage(),e);
			Assert.fail(e.getMessage());
		}

	}

	@Test
    public void testBigTime() throws Exception{
        IMessage messageHeader = getUnitHeader((short)2);
        IMessage messageTime = getMessageCreator().getTime(Integer.MAX_VALUE + 1l);
        IMessage messageAddOrder = getMessageCreator().getAddOrderOneByteLength(Integer.MAX_VALUE + 2l);
        List<IMessage> list = new ArrayList<>();
        list.add(messageHeader);
        list.add(messageTime);
        list.add(messageAddOrder);
        IMessage messageList = getMessageCreator().getMessageList(list);
        IMessage decodedMessage=decode(encode(messageList,null),null);
        Assert.assertTrue( "Object must be instance of IMessage.", (decodedMessage instanceof IMessage) );

        List<IMessage> listResult = decodedMessage.<List<IMessage>>getField(ITCHMessageHelper.SUBMESSAGES_FIELD_NAME);
        Assert.assertEquals(3, listResult.size());
        Assert.assertEquals(DateTimeUtility.toLocalDateTime((Integer.MAX_VALUE + 1l) * 1000).plusNanos(Integer.MAX_VALUE + 2l), listResult.get(2).getField(ITCHMessageHelper.FAKE_FIELD_MESSAGE_TIME));
    }

	/*
	@Ignore@Ignore@Test
	public void testFakeMessageTime() throws Exception {
	    long second = 60 * 4 + 8;
	    long nanoseconds = 1_123_456_789L;

	    ITCHCodec codec = (ITCHCodec) this.messageHelper.getCodec();
        ProtocolEncoderOutput output = new MockProtocolEncoderOutput();

        IMessage messageHeader = msgFactory.createMessage("UnitHeader", namespace);
        messageHeader.addField("Length", 52);
        messageHeader.addField("MessageCount", (short) 2);
        messageHeader.addField("MarketDataGroup", "M");
        messageHeader.addField("SequenceNumber", 1L);

        IMessage messageTime = msgFactory.createMessage("Time", namespace);
        messageTime.addField(ITCHMessageHelper.FIELD_SECONDS, Long.valueOf(second));

        IMessage messageAddOrder = msgFactory.createMessage("AddOrderOneByteLength", namespace);
        messageAddOrder.addField(ITCHMessageHelper.FIELD_NANOSECOND, nanoseconds);
        messageAddOrder.addField("OrderID", new BigDecimal(10));
        messageAddOrder.addField("Side", (short)66);
        messageAddOrder.addField("Quantity", new BigDecimal(10));
        messageAddOrder.addField("InstrumentID", 10L);
        messageAddOrder.addField("Reserved1", (short)10);
        messageAddOrder.addField("Reserved2", (short)10);
        messageAddOrder.addField("Price", 10d);
        messageAddOrder.addField("Flags", (short)10);
        messageAddOrder.addField("ImpliedPrice", 10d);

        IMessage messageList = msgFactory.createMessage(ITCHMessageHelper.MESSAGELIST_NAME, namespace);

        List<IMessage> list = new ArrayList<>();
        list.add(messageHeader);
        list.add(messageTime);
        list.add(messageAddOrder);

        messageList.addField(ITCHMessageHelper.SUBMESSAGES_FIELD_NAME, list);

        session.write(messageList);
        codec.encode(session, messageList, output);
        Queue<Object> msgQueue = ((AbstractProtocolEncoderOutput)output).getMessageQueue();
        Object lastMessage = msgQueue.element();
        Assert.assertNotNull(lastMessage);

        MockProtocolDecoderOutput decoderOutput = new MockProtocolDecoderOutput();
        IoSession decodeSession = new DummySession();
        IoBuffer toDecode = IoBuffer.wrap( ((IoBuffer)lastMessage).array() );

        Assert.assertTrue(codec.doDecode( decodeSession, toDecode, decoderOutput ));
        IMessage result = (IMessage) decoderOutput.getMessageQueue().element();
        @SuppressWarnings("unchecked")
        List<IMessage> listResult = (List<IMessage>) result.getField(ITCHMessageHelper.SUBMESSAGES_FIELD_NAME);

        Calendar calendar = new GregorianCalendar();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        long timeInMilliseconds = calendar.getTimeInMillis() + second * 1_000L + nanoseconds / 1_000_000L;

        System.out.println(listResult.get(2));
        System.out.println(listResult.get(2).getField("MessageTime"));
        System.out.println(new DateFormatted(timeInMilliseconds));

        Assert.assertEquals(timeInMilliseconds, ((Date)listResult.get(2).getField("MessageTime")).getTime());
    }

	// AddOrder
	// ,0x2E ,0x00 ,0x01 ,0x31 ,0x10 ,0x05 ,0x00 ,0x00 ,0x26 ,0x41 ,0xE0 ,0x98 ,0xC3 ,0x22 ,0x0E ,0x00 ,0x00 ,0x40 ,0xC4 ,0xD4 ,0x57 ,0x01 ,0x42 ,0xE8 ,0x03 ,0x00 ,0x00 ,0x00 ,0x00 ,0x00 ,0x00 ,0x3D ,0xE6 ,0x46 ,0x00 ,0x00 ,0x00 ,0x00 ,0x4E ,0x72 ,0x53 ,0x00 ,0x00 ,0x00 ,0x00 ,0x00

	@Ignore@Ignore@Test
	public void testDecodeFloatField() throws Exception
	{
		ITCHCodec codec = (ITCHCodec) messageHelper.getCodec();
		int[] array = new int[]
             {0x64, 0x00, 0x01, 0x41, 0x00, 0x00, 0x00, 0x00, 0x5C, 0x52, 0x78, 0x02, 0x73, 0x08, 0x46, 0x18,
              0x00, 0x00, 0x00, 0x00, 0x20, 0x4E, 0x4F, 0x30, 0x30, 0x30, 0x33, 0x37, 0x33, 0x33, 0x38, 0x30,
              0x30, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x4F, 0x42, 0x58,
              0x20, 0x20, 0x20, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x4E, 0x4F, 0x4B, 0x00, 0x20, 0x20, 0x20,
              0x20, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20,
              0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x00,
              0x00, 0x00, 0x00, 0x00, 0x64, 0x00, 0x01, 0x41, 0x00, 0x00, 0x00, 0x00, 0x5C, 0x52, 0x38, 0xD1,
              0x75, 0x08, 0x4C, 0x18, 0x00, 0x00, 0x00, 0x00, 0x20, 0x4E, 0x4F, 0x30, 0x30, 0x31, 0x30, 0x31,
              0x39, 0x39, 0x31, 0x35, 0x31, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
              0x00, 0x4F, 0x42, 0x58, 0x20, 0x20, 0x20, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x4E, 0x4F, 0x4B,
              0x00, 0x20, 0x20, 0x20, 0x20, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x20, 0x20, 0x20,
              0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20,
              0x20, 0x20, 0x20, 0x00, 0x00, 0x00, 0x00, 0x00, 0x64, 0x00, 0x01, 0x41, 0x00, 0x00, 0x00, 0x00,
              0x5C, 0x52, 0x80, 0xB3, 0x78, 0x08, 0x60, 0x18, 0x00, 0x00, 0x00, 0x00, 0x20, 0x4E, 0x4F, 0x30,
              0x30, 0x30, 0x33, 0x30, 0x32, 0x38, 0x39, 0x30, 0x34, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
              0x00, 0x00, 0x00, 0x00, 0x00, 0x4F, 0x42, 0x58, 0x20, 0x20, 0x20, 0x00, 0x00, 0x00, 0x00, 0x00,
              0x00, 0x4E, 0x4F, 0x4B, 0x00, 0x20, 0x20, 0x20, 0x20, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
              0x00, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20,
              0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x00, 0x00, 0x00, 0x00, 0x00, 0x64, 0x00, 0x01, 0x41,
              0x00, 0x00, 0x00, 0x00, 0x5C, 0x52, 0x10, 0x8A, 0x7B, 0x08, 0x90, 0x18, 0x00, 0x00, 0x00, 0x00,
              0x20, 0x4E, 0x4F, 0x30, 0x30, 0x30, 0x33, 0x30, 0x35, 0x33, 0x36, 0x30, 0x35, 0x00, 0x00, 0x00,
              0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x4F, 0x42, 0x58, 0x20, 0x20, 0x20, 0x00,
              0x00, 0x00, 0x00, 0x00, 0x00, 0x4E, 0x4F, 0x4B, 0x00, 0x20, 0x20, 0x20, 0x20, 0x00, 0x00, 0x00,
              0x00, 0x00, 0x00, 0x00, 0x00, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20,
              0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x00, 0x00, 0x00, 0x00, 0x00,
              0x64, 0x00, 0x01, 0x41, 0x00, 0x00, 0x00, 0x00, 0x5C, 0x52, 0x88, 0x64, 0x7E, 0x08, 0x23, 0xFC,
              0x00, 0x00, 0x00, 0x00, 0x20, 0x4E, 0x4F, 0x30, 0x30, 0x31, 0x30, 0x32, 0x33, 0x39, 0x34, 0x33,
              0x37, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x4F, 0x42, 0x58,
              0x20, 0x20, 0x20, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x4E, 0x4F, 0x4B, 0x00, 0x20, 0x20, 0x20,
              0x20, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20,
              0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x00,
              0x00, 0x00, 0x00, 0x00, 0x20, 0x00, 0x01, 0x41, 0x00, 0x00, 0x00, 0x00, 0x18, 0x83, 0x00, 0x00,
              0x00, 0x00, 0x4F, 0x42, 0x58, 0x20, 0x20, 0x20, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02,
              0x01, 0x00, 0x00, 0x00};
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
        Assert.assertTrue( "Decoding error.", decodableResult);
        //System.out.println(decoderOutput.getMessageQueue().element());

		IMessage im = (IMessage)decoderOutput.getMessageQueue().element();
		System.out.println(((List)im.getField("IncludedMessages")).get(1));
		SimpleTreeEntity entity1 = new SimpleTreeEntity(IMessage.class.getCanonicalName(), ((List)im.getField("IncludedMessages")).get(1));

		IMessage sd = new MapMessage("itch", "SymbolDirectory");
		// only this field should be compared
		sd.addField("DynamicCircuitBreakerTolerances", 0.0f);

		System.out.println(sd);

		SimpleTreeEntity entity2 = new SimpleTreeEntity(IMessage.class.getCanonicalName(), sd);

		CompareSettings compSettings = new CompareSettings();
		//compSettings.setDoublePrecision(0.0);

		ComparisonResult table = TreeComparer.compare(entity1, entity2, compSettings);
		System.out.println(table);
		System.out.println(ComparisonUtil.toTable(table).toString());

		int count = ComparisonUtil.getResultCount(table, StatusType.FAILED);
		count += ComparisonUtil.getResultCount(table, StatusType.CONDITIONALLY_FAILED);
		Assert.assertTrue("Some comparison failed", count == 0);
		count = ComparisonUtil.getResultCount(table, StatusType.PASSED);
		count += ComparisonUtil.getResultCount(table, StatusType.CONDITIONALLY_PASSED);
		Assert.assertTrue("Some comparison do not passed", count == 1);
	}




	@Ignore@Test
	public void testDoubleTypeVisitorLogic() {
		byte[] array = new byte[] {0x00, 0x00, 0x00, 0x00, 0x05, (byte)0xf5, (byte)0xe1, 0x00};

		long ioLong = IoBuffer.wrap(array).getLong();
		long byteLong = ByteBuffer.wrap(array).getLong();
		BigInteger bi = new BigInteger(array);

		Assert.assertEquals(100000000, ioLong);
		Assert.assertEquals(100000000, byteLong);
		Assert.assertEquals(new BigInteger("100000000"), bi);
	}

	*/




}
