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

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.ByteOrder;
import java.util.List;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.DummySession;
import org.apache.mina.core.session.IoSession;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDate;
import java.time.LocalTime;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.services.MockProtocolDecoderOutput;
import com.exactpro.sf.util.TestITCHHelper;

public class TestITCHVisitorPositive extends TestITCHHelper {

    private static final Logger logger = LoggerFactory.getLogger(TestITCHVisitorPositive.class);
    private static ITCHCodec codec = null;

	@BeforeClass
	public static void setUpClass(){
		logger.info("Start positive tests of ITCH visitor");
		try{
			codec = getCodecWithAdditionalDictionary();
		}catch(IOException e){
			logger.error(e.getMessage(),e);
			Assert.fail(e.getMessage());
		}
	}

	@AfterClass
	public static void tearDownClass(){
		logger.info("Finish positive tests of ITCH visitor");
	}

	/**
     * Test methods visit(Integer value...) in ITCHVisitorEncode and ITCHVisitorDecode with all possible type:
	 * UInt16, Int8, Int16, Int32 and STUB
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testIntegerType(){
		try{
			IMessage message = getMessageCreator().getTestInteger();

	        IMessage decodedMessage=decode(encode(message,codec),codec);

            List<IMessage> result = (List<IMessage>) decodedMessage.getField(ITCHMessageHelper.SUBMESSAGES_FIELD_NAME);
            List<IMessage> original = (List<IMessage>) message.getField(ITCHMessageHelper.SUBMESSAGES_FIELD_NAME);

		    Assert.assertTrue("UnitHeader messages must be equal. Original message:"+original.get(0)+"; \n"
		    		+ "Result message:"+result.get(0),result.get(0).compare(original.get(0)));
		    compareFieldsValues(result.get(1),original.get(1),"uInt16",Integer.class);
		    compareFieldsValues(result.get(1),original.get(1),"int8",Integer.class);
		    compareFieldsValues(result.get(1),original.get(1),"int16",Integer.class);
		    compareFieldsValues(result.get(1),original.get(1),"int32",Integer.class);
		    compareFieldsValues(result.get(1),original.get(1),"STUB",Integer.class);
		}catch(Exception e){
			e.printStackTrace(System.err);
			logger.error(e.getMessage(),e);
			Assert.fail(e.getMessage());
		}

	}

	/**
     * Test methods visit(Long value...) in ITCHVisitorEncode and ITCHVisitorDecode with all possible type:
	 * UInt32, UInt64, Int16, Int32,  Int64 and STUB
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testLongType(){
		try{
			IMessage message = getMessageCreator().getTestLong();

	        IMessage decodedMessage=decode(encode(message,codec),codec);

            List<IMessage> result = (List<IMessage>) decodedMessage.getField(ITCHMessageHelper.SUBMESSAGES_FIELD_NAME);
            List<IMessage> original = (List<IMessage>) message.getField(ITCHMessageHelper.SUBMESSAGES_FIELD_NAME);
		    Assert.assertTrue("UnitHeader messages must be equal. Original message:"+original.get(0)+"; \n"
		    		+ "Result message:"+result.get(0),result.get(0).compare(original.get(0)));
		    compareFieldsValues(result.get(1),original.get(1),"uInt32",Long.class);
		    compareFieldsValues(result.get(1),original.get(1),"uInt64",Long.class);
		    compareFieldsValues(result.get(1),original.get(1),"int16",Long.class);
		    compareFieldsValues(result.get(1),original.get(1),"int32",Long.class);
		    compareFieldsValues(result.get(1),original.get(1),"int64",Long.class);
		    compareFieldsValues(result.get(1),original.get(1),"STUB",Long.class);
		}catch(Exception e){
			e.printStackTrace(System.err);
			logger.error(e.getMessage(),e);
			Assert.fail(e.getMessage());
		}
	}

	/**
     * Test methods visit(Short value...) in ITCHVisitorEncode and ITCHVisitorDecode with all possible type:
	 * UInt8, Byte, Int8, Int16 and STUB
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testShortType(){
		try{
			IMessage message = getMessageCreator().getTestShort();

	        IMessage decodedMessage=decode(encode(message,codec),codec);

            List<IMessage> result = (List<IMessage>) decodedMessage.getField(ITCHMessageHelper.SUBMESSAGES_FIELD_NAME);
            List<IMessage> original = (List<IMessage>) message.getField(ITCHMessageHelper.SUBMESSAGES_FIELD_NAME);
		    Assert.assertTrue("UnitHeader messages must be equal. Original message:"+original.get(0)+"; \n"
		    		+ "Result message:"+result.get(0),result.get(0).compare(original.get(0)));
		    compareFieldsValues(result.get(1),original.get(1),"uInt8",Short.class);
		    compareFieldsValues(result.get(1),original.get(1),"byte",Short.class);
		    compareFieldsValues(result.get(1),original.get(1),"int8",Short.class);
		    compareFieldsValues(result.get(1),original.get(1),"int16",Short.class);
		    compareFieldsValues(result.get(1),original.get(1),"STUB",Short.class);
		}catch(Exception e){
			logger.error(e.getMessage(),e);
			e.printStackTrace(System.err);
			Assert.fail(e.getMessage());
		}
	}

	/**
     * Test methods visit(Byte value...) in ITCHVisitorEncode and ITCHVisitorDecode with all possible type:
	 * Byte and Int8
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testByteType(){
		try{
			IMessage message = getMessageCreator().getTestByte();

	        IMessage decodedMessage=decode(encode(message,codec),codec);

            List<IMessage> result = (List<IMessage>) decodedMessage.getField(ITCHMessageHelper.SUBMESSAGES_FIELD_NAME);
            List<IMessage> original = (List<IMessage>) message.getField(ITCHMessageHelper.SUBMESSAGES_FIELD_NAME);
		    Assert.assertTrue("UnitHeader messages must be equal. Original message:"+original.get(0)+"; \n"
		    		+ "Result message:"+result.get(0),result.get(0).compare(original.get(0)));
		    compareFieldsValues(result.get(1),original.get(1),"byte",Byte.class);
		    compareFieldsValues(result.get(1),original.get(1),"int8",Byte.class);
		}catch(Exception e){
			logger.error(e.getMessage(),e);
			e.printStackTrace(System.err);
			Assert.fail(e.getMessage());
		}
	}

	/**
     * Test methods visit(String value...) in ITCHVisitorEncode and ITCHVisitorDecode with all possible type:
	 * Alpha, Time, Date and STUB
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testStringType(){
		try{
			IMessage message = getMessageCreator().getTestString();

	        IMessage decodedMessage=decode(encode(message,codec),codec);

            List<IMessage> result = (List<IMessage>) decodedMessage.getField(ITCHMessageHelper.SUBMESSAGES_FIELD_NAME);
            List<IMessage> original = (List<IMessage>) message.getField(ITCHMessageHelper.SUBMESSAGES_FIELD_NAME);
		    Assert.assertTrue("UnitHeader messages must be equal. Original message:"+original.get(0)+"; \n"
		    		+ "Result message:"+result.get(0),result.get(0).compare(original.get(0)));
		    compareFieldsValues(result.get(1),original.get(1),"Alpha",String.class);
		    compareFieldsValues(result.get(1),original.get(1),"Time",String.class);
		    compareFieldsValues(result.get(1),original.get(1),"Date",String.class);
		    compareFieldsValues(result.get(1),original.get(1),"STUB",String.class);
		}catch(Exception e){
			logger.error(e.getMessage(),e);
			e.printStackTrace(System.err);
			Assert.fail(e.getMessage());
		}
	}

	/**
     * Test methods visit(Float value...) in ITCHVisitorEncode and ITCHVisitorDecode with all possible type:
	 * Price
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testFloatType(){
		try{
			IMessage message = getMessageCreator().getTestFloat();

	        IMessage decodedMessage=decode(encode(message,codec),codec);

            List<IMessage> result = (List<IMessage>) decodedMessage.getField(ITCHMessageHelper.SUBMESSAGES_FIELD_NAME);
            List<IMessage> original = (List<IMessage>) message.getField(ITCHMessageHelper.SUBMESSAGES_FIELD_NAME);
		    Assert.assertTrue("UnitHeader messages must be equal. Original message:"+original.get(0)+"; \n"
		    		+ "Result message:"+result.get(0),result.get(0).compare(original.get(0)));
		    compareFieldsValues(result.get(1),original.get(1),"Price",Float.class);
		}catch(Exception e){
			logger.error(e.getMessage(),e);
			e.printStackTrace(System.err);
			Assert.fail(e.getMessage());
		}
	}

	/**
     * Test methods visit(Double value...) in ITCHVisitorEncode and ITCHVisitorDecode with all possible type and positive value:
	 * Price, Size, Price4 and Size4
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testDoubleType(){
		try{
			IMessage message = getMessageCreator().getTestDouble();
			IMessage decodedMessage=decode(encode(message,codec),codec);

            List<IMessage> result = (List<IMessage>) decodedMessage.getField(ITCHMessageHelper.SUBMESSAGES_FIELD_NAME);
            List<IMessage> original = (List<IMessage>) message.getField(ITCHMessageHelper.SUBMESSAGES_FIELD_NAME);
		    Assert.assertTrue("UnitHeader messages must be equal. Original message:"+original.get(0)+"; \n"
		    		+ "Result message:"+result.get(0),result.get(0).compare(original.get(0)));
		    compareFieldsValues(result.get(1),original.get(1),"Price",Double.class);
		    compareFieldsValues(result.get(1),original.get(1),"Size",Double.class);
		    compareFieldsValues(result.get(1),original.get(1),"Price4",Double.class);
		    compareFieldsValues(result.get(1),original.get(1),"Size4",Double.class);
		    compareFieldsValues(result.get(1),original.get(1),"UInt16",Double.class);
		}catch(Exception e){
			logger.error(e.getMessage(),e);
			e.printStackTrace(System.err);
			Assert.fail(e.getMessage());
		}
	}

	/**
     * Test methods visit(Double value...) in ITCHVisitorEncode and ITCHVisitorDecode with all possible type and negative value:
	 * Price, Size, Price4 and Size4
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testDoubleTypeWithNegativeValue(){
		try{
			double neg=-3.1;
			IMessage message = getMessageHelper().getMessageFactory().createMessage("testDouble", "ITCH");
			message.addField("Price", neg);
			message.addField("Size", (double)0);
			message.addField("Price4", neg);
			message.addField("Size4", (double)0);
			message.addField("UInt16", (double)0);
			message=getMessageHelper().prepareMessageToEncode(message, null);
			IMessage decodedMessage=decode(encode(message,codec),codec);

            List<IMessage> result = (List<IMessage>) decodedMessage.getField(ITCHMessageHelper.SUBMESSAGES_FIELD_NAME);
            Assert.assertEquals((Double)(-9.223372036544775E10), result.get(1).getField("Price"));
            Assert.assertEquals((Double)(-9.223372036854745E14), result.get(1).getField("Price4"));
		}catch(Exception e){
			logger.error(e.getMessage(),e);
			e.printStackTrace(System.err);
			Assert.fail(e.getMessage());
		}


	}

	/**
     * Test methods visit(BigDecimal value...) in ITCHVisitorEncode and ITCHVisitorDecode with all possible type and positive value:
	 * UInt64,Price,Size and UDT
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testBigDecimalType(){
		try{
			IMessage message = getMessageCreator().getTestBigDecimal();

	        IMessage decodedMessage=decode(encode(message,codec),codec);
            List<IMessage> result = (List<IMessage>) decodedMessage.getField(ITCHMessageHelper.SUBMESSAGES_FIELD_NAME);
            List<IMessage> original = (List<IMessage>) message.getField(ITCHMessageHelper.SUBMESSAGES_FIELD_NAME);
		    Assert.assertTrue("UnitHeader messages must be equal. Original message:"+original.get(0)+"; \n"
		    		+ "Result message:"+result.get(0),result.get(0).compare(original.get(0)));
		    compareFieldsValues(result.get(1),original.get(1),"UInt64",BigDecimal.class);
		    compareFieldsValues(result.get(1),original.get(1),"Int32",BigDecimal.class);
		    compareFieldsValues(result.get(1),original.get(1),"UInt32",BigDecimal.class);
		    compareFieldsValues(result.get(1),original.get(1),"Price",BigDecimal.class);
		    compareFieldsValues(result.get(1),original.get(1),"Size",BigDecimal.class);
		    compareFieldsValues(result.get(1),original.get(1),"UDT",BigDecimal.class);
		}catch(Exception e){
			logger.error(e.getMessage(),e);
			e.printStackTrace(System.err);
			Assert.fail(e.getMessage());
		}
	}

	/**
     * Test methods visit(BigDecimal value...) in ITCHVisitorEncode and ITCHVisitorDecode with all possible type and negative value:
	 * UInt64,Price,Size and UDT
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testBigDecimalTypeWithNegativeValue(){
		try{
			BigDecimal negativeValue=new BigDecimal(-1);
			IMessage message = getMessageHelper().getMessageFactory().createMessage("testBigDecimal", "ITCH");
			message.addField("UInt64", new BigDecimal(0));
			message.addField("Int32", new BigDecimal(0));
			message.addField("UInt32", new BigDecimal(0));
			message.addField("Price", negativeValue);
			message.addField("Size", new BigDecimal(0));
			message.addField("UDT", new BigDecimal(0));
			message=getMessageHelper().prepareMessageToEncode(message, null);

	        IMessage decodedMessage=decode(encode(message,codec),codec);
            List<IMessage> result = (List<IMessage>) decodedMessage.getField(ITCHMessageHelper.SUBMESSAGES_FIELD_NAME);

	        long val = (long) (negativeValue.doubleValue() * 100000000);
			long mask = 0x7FFFFFFFFFFFFFFFL;
			val = val & mask;
			val = val * -1L;
			BigDecimal valBD = new BigDecimal(val);
			valBD=valBD.divide(new BigDecimal(100000000L));

			Assert.assertEquals(valBD,result.get(1).getField("Price"));
		}catch(Exception e){
			logger.error(e.getMessage(),e);
			e.printStackTrace(System.err);
			Assert.fail(e.getMessage());
		}
	}

	/**
     * Test methods visit(IMessage value...) in ITCHVisitorEncode and ITCHVisitorDecode
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testIMessageType(){
		try{
			IMessage testMessage = getMessageHelper().getMessageFactory().createMessage("testMessage", "ITCH");
			IMessage message = getMessageHelper().getMessageFactory().createMessage("testIMessage", "ITCH");
			message.addField("testMessage", testMessage);
			message=getMessageHelper().prepareMessageToEncode(message, null);
			IMessage decodedMessage=decode(encode(message,codec),codec);
            List<IMessage> result = (List<IMessage>) decodedMessage.getField(ITCHMessageHelper.SUBMESSAGES_FIELD_NAME);
            List<IMessage> original = (List<IMessage>) message.getField(ITCHMessageHelper.SUBMESSAGES_FIELD_NAME);
		    Assert.assertTrue("UnitHeader messages must be equal. Original message:"+original.get(0)+"; \n"
		    		+ "Result message:"+result.get(0),result.get(0).compare(original.get(0)));
		}catch(Exception e){
			logger.error(e.getMessage(),e);
			e.printStackTrace(System.err);
			Assert.fail(e.getMessage());
		}
	}

	/**
	 * Test decode message testString from bytes and check Alpha_notrim
	 */
	@Test
	public void testStringAlphaNotrimDecode(){
		byte[] array = new byte[]
        		{58, 0, 1, 48, 0, 0, 0, 0,
        			46, 13, 102, 102, 115, 116, 49, 32,
        			32, 32, 49, 48, 58, 52, 57, 58,
        			48, 48, 77, 111, 110, 32, 74, 117,
        			108, 32, 48, 52, 32, 49, 52, 58,
        			48, 50, 58, 51, 48, 32, 77, 83,
        			75, 32, 50, 48, 49, 54, 0, 0,
        			0, 0, 0, 0, 0, 0, 0, 0,};
   		IoBuffer toDecode = IoBuffer.wrap( array );
		toDecode.order(ByteOrder.LITTLE_ENDIAN);
		toDecode.position(0);

		IoSession decodeSession = new DummySession();
		MockProtocolDecoderOutput decoderOutput = new MockProtocolDecoderOutput();
		try{
			boolean decodableResult = codec.doDecode( decodeSession, toDecode, decoderOutput );
			Assert.assertTrue( "Decoding error.", decodableResult);
			IMessage message=(IMessage) decoderOutput.getMessageQueue().element();
			@SuppressWarnings("unchecked")
            List<IMessage> result = (List<IMessage>) message.getField(ITCHMessageHelper.SUBMESSAGES_FIELD_NAME);
			Assert.assertEquals(2, result.size());
			Assert.assertEquals(1, (int)Integer.valueOf(result.get(1).getField("Alpha_notrim").toString().trim()));
		}catch(Exception e){
			e.printStackTrace(System.err);
			logger.error(e.getMessage(),e);
			Assert.fail(e.getMessage());
		}
	}

	/**
     * Test methods visit(Date value...) in ITCHVisitorEncode and ITCHVisitorDecode with all possible type:
	 * Date, Time, Days and Stub
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testDateType(){
		try{
			IMessage message = getMessageCreator().getTestDate();

	        IMessage decodedMessage=decode(encode(message,codec),codec);

            List<IMessage> result = (List<IMessage>) decodedMessage.getField(ITCHMessageHelper.SUBMESSAGES_FIELD_NAME);
            List<IMessage> original = (List<IMessage>) message.getField(ITCHMessageHelper.SUBMESSAGES_FIELD_NAME);
		    Assert.assertTrue("UnitHeader messages must be equal. Original message:"+original.get(0)+"; \n"
		    		+ "Result message:"+result.get(0),result.get(0).compare(original.get(0)));
            compareFieldsValues(result.get(1), original.get(1), "Date", LocalDate.class);
            compareFieldsValues(result.get(1), original.get(1), "Time", LocalTime.class);
            compareFieldsValues(result.get(1), original.get(1), "Days", LocalDate.class);
		}catch(Exception e){
			logger.error(e.getMessage(),e);
			e.printStackTrace(System.err);
			Assert.fail(e.getMessage());
		}
	}

}
