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

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.services.MockProtocolDecoderOutput;
import com.exactpro.sf.util.TestITCHHelper;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.DummySession;
import org.apache.mina.core.session.IoSession;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;

public class TestITCHVisitorNegative extends TestITCHHelper {

    private static final Logger logger = LoggerFactory.getLogger(TestITCHVisitorNegative.class);
    private static ITCHCodec codec = null;
    private static ITCHCodec codecValid = null;
    private static ITCHCodec codecLength = null;
	
	@BeforeClass
	public static void setUpClass(){
		try{
			logger.info("Start negative tests of ITCH visitor");
			codec = getCodecWithInvalidDictionary();
			codecValid=getCodecWithAdditionalDictionary();
			codecLength=getCodecWithInvalidLengthDictionary();
		}catch(IOException e){
			logger.error(e.getMessage(),e);
			Assert.fail(e.getMessage());
		}
	}
	
	@AfterClass
	public static void tearDownClass(){
		logger.info("Finish negative tests of ITCH visitor");
	}
	
	/**
	 * Try to encode message with incorrect Type attribute: Type=invalid
	 */
	@Test
	public void testInvalidTypeEncode(){
		try{
			IMessage message = getMessageHelper().getMessageFactory().createMessage("testType", "ITCH");
			message.addField("invalid", 1);
			message=getMessageHelper().prepareMessageToEncode(message, null);
	        testNegativeEncode(message, codec);
		}catch(Exception e){
			e.printStackTrace(System.err);
			logger.error(e.getMessage(),e);
			Assert.fail(e.getMessage());
		}  
	}
	
	/**
	 * Try to decode message with incorrect Type attribute: Type=invalid
	 */
	@Test
	public void testInvalidTypeDecode(){
		byte[] array = new byte[]
        		{58, 0, 1, 48, 0, 0, 0, 0, 
        			46, 13, 102, 102, 115, 116, 49, 32, 
        			32, 32, 49, 48, 58, 52, 57, 58, 
        			48, 48, 77, 111, 110, 32, 74, 117, 
        			108, 32, 48, 52, 32, 49, 52, 58, 
        			48, 50, 58, 51, 48, 32, 77, 83, 
        			75, 32, 50, 48, 49, 54, 0, 0, 
        			0, 0, 0, 0, 0, 0, 0, 0,};   
   		try{
   			MockProtocolDecoderOutput decoderOutput = new MockProtocolDecoderOutput();
			IoSession decodeSession = new DummySession();
			IoBuffer toDecode = IoBuffer.wrap( array );
			codec.doDecode( decodeSession, toDecode, decoderOutput );
		}catch(EPSCommonException e){
			Assert.assertEquals("Unknown type = [invalid]. in field name = [invalid]. in MessageStructure Name = [testAlphaNotrim]", e.getMessage());
		}catch(Exception e){
			logger.error(e.getMessage(),e);
			e.printStackTrace(System.err);
			Assert.fail(e.getMessage());
		}
	}
	
	/**
	 * Try to encode message with overflow string variable
	 */
	@Test
	public void testOverflowStringType(){
		try{
			IMessage message = getMessageHelper().getMessageFactory().createMessage("testString", "ITCH");
			message.addField("Alpha", "overflow");
			message=getMessageHelper().prepareMessageToEncode(message, null);
			testNegativeEncode(message, codecValid);
		}catch(Exception e){
			logger.error(e.getMessage(),e);
			e.printStackTrace(System.err);
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testInvalidIMessageType(){
		try{
			IMessage message = getMessageHelper().getMessageFactory().createMessage("testIMessage", "ITCH");
			message=getMessageHelper().prepareMessageToEncode(message, null);
			try{
	        	encode(message,codecValid);
	        	Assert.fail("There is no exception was threw");
	        }catch(EPSCommonException e){
	        	Assert.assertEquals("Travers problem for FieldName = testMessage, FieldValue = null. "
	        			+ "in MessageStructure Name = [testIMessage]", e.getMessage());
	        }    	    
		}catch(Exception e){
			logger.error(e.getMessage(),e);
			e.printStackTrace(System.err);
			Assert.fail(e.getMessage());
		}
	}
	
	/**
	 * Try to encode and decode message with Integer type and invalid Type=Byte
	 */
	@Test
	public void testInvalidTypeInteger(){
		try{
			IMessage message = getMessageHelper().getMessageFactory().createMessage("testInteger", "ITCH");
			message.addField("Byte", 1);
			message=getMessageHelper().prepareMessageToEncode(message, null);
			testNegativeEncode(message, codec);
	        message=getMessageCreator().getTestInteger();
	        testNegativeDecode(message, codec, codecValid, "Byte");
		}catch(Exception e){
			e.printStackTrace(System.err);
			logger.error(e.getMessage(),e);
			Assert.fail(e.getMessage());
		}
	}
	
	/**
	 * Try to encode and decode message with Long type and invalid Type=Byte
	 */
	@Test
	public void testInvalidTypeLong(){
		try{
			IMessage message = getMessageHelper().getMessageFactory().createMessage("testLong", "ITCH");
			message.addField("Byte", (long)1);
			message=getMessageHelper().prepareMessageToEncode(message, null);
	        testNegativeEncode(message, codec);
	        message=getMessageCreator().getTestLong();
	        testNegativeDecode(message, codec, codecValid, "Byte");
		}catch(Exception e){
			e.printStackTrace(System.err);
			logger.error(e.getMessage(),e);
			Assert.fail(e.getMessage());
		}
	}
	
	/**
	 * Try to encode and decode message with Short type and invalid Type=Int64
	 */
	@Test
	public void testInvalidTypeShort(){
		try{
			IMessage message = getMessageHelper().getMessageFactory().createMessage("testShort", "ITCH");
			message.addField("Int64", (short)1);
			message=getMessageHelper().prepareMessageToEncode(message, null);
			testNegativeEncode(message, codec);
	        message=getMessageCreator().getTestShort();
	        testNegativeDecode(message, codec, codecValid, "Int64");
		}catch(Exception e){
			e.printStackTrace(System.err);
			logger.error(e.getMessage(),e);
			Assert.fail(e.getMessage());
		}
	}
	
	/**
	 * Try to encode and decode message with Byte type and invalid Type=Int64
	 */
	@Test
	public void testInvalidTypeByte(){
		try{
			IMessage message = getMessageHelper().getMessageFactory().createMessage("testByte", "ITCH");
			message.addField("Int64", (byte)1);
			message=getMessageHelper().prepareMessageToEncode(message, null);
			testNegativeEncode(message, codec);
	        message=getMessageCreator().getTestByte();
	        testNegativeDecode(message, codec, codecValid, "Int64");
		}catch(Exception e){
			e.printStackTrace(System.err);
			logger.error(e.getMessage(),e);
			Assert.fail(e.getMessage());
		}
	}
	
	/**
	 * Try to encode and decode message with String type and invalid Type=Int16
	 */
	@Test
	public void testInvalidTypeString(){
		try{
			IMessage message = getMessageHelper().getMessageFactory().createMessage("testString", "ITCH");
			message.addField("Int16", "TD");
			message=getMessageHelper().prepareMessageToEncode(message, null);
			testNegativeEncode(message, codec);
	        message=getMessageCreator().getTestString();
	        testNegativeDecode(message, codec, codecValid, "Int16");
		}catch(Exception e){
			e.printStackTrace(System.err);
			logger.error(e.getMessage(),e);
			Assert.fail(e.getMessage());
		}
	}
	
	/**
	 * Try to encode and decode message with Float type and invalid Type=Int16
	 */
	@Test
	public void testInvalidTypeFloat(){
		try{
			IMessage message = getMessageHelper().getMessageFactory().createMessage("testFloat", "ITCH");
			message.addField("Int16", (float)1.1);
			message=getMessageHelper().prepareMessageToEncode(message, null);
			testNegativeEncode(message, codec);
	        message=getMessageCreator().getTestFloat();
	        testNegativeDecode(message, codec, codecValid, "Int16");
		}catch(Exception e){
			e.printStackTrace(System.err);
			logger.error(e.getMessage(),e);
			Assert.fail(e.getMessage());
		}
	}
	
	/**
	 * Try to encode and decode message with Double type and invalid Type=Int64
	 */
	@Test
	public void testInvalidTypeDouble(){
		try{
			IMessage message = getMessageHelper().getMessageFactory().createMessage("testDouble", "ITCH");
			message.addField("Int64", (double)1.1);
			message=getMessageHelper().prepareMessageToEncode(message, null);
			testNegativeEncode(message, codec);
	        message=getMessageCreator().getTestDouble();
	        testNegativeDecode(message, codec, codecValid, "Int64");
		}catch(Exception e){
			e.printStackTrace(System.err);
			logger.error(e.getMessage(),e);
			Assert.fail(e.getMessage());
		}
	}
	
	/**
	 * Try to encode and decode message with Date type and invalid Type=Int64
	 */
	@Test
	public void testInvalidTypeDate(){
		try{
			IMessage message = getMessageHelper().getMessageFactory().createMessage("testDate", "ITCH");
			message.addField("Int64", new Date());
			message=getMessageHelper().prepareMessageToEncode(message, null);
			testNegativeEncode(message, codec);
	        message=getMessageCreator().getTestDate();
	        testNegativeDecode(message, codec, codecValid, "Int64");
		}catch(Exception e){
			e.printStackTrace(System.err);
			logger.error(e.getMessage(),e);
			Assert.fail(e.getMessage());
		}
	}
	
	/**
	 * Try to encode and decode message with BigDecimal type and invalid Type=Int8
	 */
	@Test
	public void testInvalidTypeBigDecimal(){
		try{
			IMessage message = getMessageHelper().getMessageFactory().createMessage("testBigDecimal", "ITCH");
			message.addField("Int8", new BigDecimal(1));
			message=getMessageHelper().prepareMessageToEncode(message, null);
			testNegativeEncode(message, codec);
	        message=getMessageCreator().getTestBigDecimal();
	        testNegativeDecode(message, codec, codecValid, "Int8");
		}catch(Exception e){
			e.printStackTrace(System.err);
			logger.error(e.getMessage(),e);
			Assert.fail(e.getMessage());
		}
	}
	
	/**
	 * Try to decode message with incorrect length in Integer type of value.
	 */
	@Test
	public void testIncorrectLengthIntegerType(){
		IMessage message = getMessageCreator().getTestInteger();
		try{
			decode(encode(message,codecLength),codecLength);
			Assert.fail("There is no exception was threw");
		}catch(EPSCommonException e){
			Assert.assertEquals("Read 2 bytes, but length = 1 for uInt16 field. in field name = [uInt16]."
					+ " in MessageStructure Name = [testInteger]", e.getMessage());
		}catch(Exception e){
			e.printStackTrace(System.err);
			logger.error(e.getMessage(),e);
			Assert.fail(e.getMessage());
		}
	}
	
	/**
	 * Try to decode message with incorrect length in Long type of value.
	 */
	@Test
	public void testIncorrectLengthLongType(){
		IMessage message = getMessageCreator().getTestLong();
		try{
			decode(encode(message,codecLength),codecLength);
			Assert.fail("There is no exception was threw");
		}catch(EPSCommonException e){
			Assert.assertEquals("Read 4 bytes, but length = 1 for uInt32 field. in field name = [uInt32]. "
					+ "in MessageStructure Name = [testLong]", e.getMessage());
		}catch(Exception e){
			e.printStackTrace(System.err);
			logger.error(e.getMessage(),e);
			Assert.fail(e.getMessage());
		}
	}
	
	/**
	 * Try to decode message with incorrect length in Short type of value.
	 */
	@Test
	public void testIncorrectLengthShortType(){
		IMessage message = getMessageCreator().getTestShort();
		try{
			decode(encode(message,codecLength),codecLength);
			Assert.fail("There is no exception was threw");
		}catch(EPSCommonException e){
			Assert.assertEquals("Read 2 bytes, but length = 1 for int16 field. in field name = [int16]. "
					+ "in MessageStructure Name = [testShort]", e.getMessage());
		}catch(Exception e){
			e.printStackTrace(System.err);
			logger.error(e.getMessage(),e);
			Assert.fail(e.getMessage());
		}
	}
	
	/**
	 * Try to decode message with incorrect length in Byte type of value.
	 */
	@Test
	public void testIncorrectLengthByteType(){
		IMessage message = getMessageCreator().getTestByte();
		try{
			decode(encode(message,codecLength),codecLength);
			Assert.fail("There is no exception was threw");
		}catch(EPSCommonException e){
			Assert.assertEquals("Read 1 bytes, but length = 2 for byte field. in field name = [byte]. "
					+ "in MessageStructure Name = [testByte]", e.getMessage());
		}catch(Exception e){
			e.printStackTrace(System.err);
			logger.error(e.getMessage(),e);
			Assert.fail(e.getMessage());
		}
	}
	
	/**
	 * Try to decode message with incorrect length in Float type of value.
	 */
	@Test
	public void testIncorrectLengthFloatType(){
		IMessage message = getMessageCreator().getTestFloat();
		try{
			decode(encode(message,codecLength),codecLength);
			Assert.fail("There is no exception was threw");
		}catch(EPSCommonException e){
			Assert.assertEquals("Read 4 bytes, but length = 3 for Price field. in field name = [Price]. "
					+ "in MessageStructure Name = [testFloat]", e.getMessage());
		}catch(Exception e){
			e.printStackTrace(System.err);
			logger.error(e.getMessage(),e);
			Assert.fail(e.getMessage());
		}
	}
	
	/**
	 * Try to decode message with incorrect length in Double type of value.
	 */
	@Test
	public void testIncorrectLengthDoubleType(){
		IMessage message = getMessageCreator().getTestDouble();
		try{
			decode(encode(message,codecLength),codecLength);
			Assert.fail("There is no exception was threw");
		}catch(EPSCommonException e){
			Assert.assertEquals("Read 8 bytes, but length = 3 for Price field. in field name = [Price]. "
					+ "in MessageStructure Name = [testDouble]", e.getMessage());
		}catch(Exception e){
			e.printStackTrace(System.err);
			logger.error(e.getMessage(),e);
			Assert.fail(e.getMessage());
		}
	}
	
	/**
	 * Try to decode message with incorrect length in BigDecimal type of value.
	 */
	@Test
	public void testIncorrectLengthBigDecimalType(){
		try{
			IMessage message = getMessageCreator().getTestBigDecimal();
			decode(encode(message,codecLength),codecLength);
			Assert.fail("There is no exception was threw");
		}catch(EPSCommonException e){
			Assert.assertEquals("Read 8 bytes, but length = 3 for Price field. in field name = [Price]. "
					+ "in MessageStructure Name = [testBigDecimal]", e.getMessage());
		}catch(Exception e){
			e.printStackTrace(System.err);
			logger.error(e.getMessage(),e);
			Assert.fail(e.getMessage());
		}
	}
	
	/**
	 * Try to decode message with incorrect length in Date type of value.
	 */
	@Test
	public void testIncorrectLengthDateType(){
		try{
			IMessage message = getMessageCreator().getTestDate();
			decode(encode(message,codecLength),codecLength);
			Assert.fail("There is no exception was threw");
		}catch(EPSCommonException e){
			Assert.assertEquals("Incorrect field lenth = 1 for Days field. in field name = [Days]. in MessageStructure Name = [testDate]", e.getMessage());
		}catch(Exception e){
			e.printStackTrace(System.err);
			logger.error(e.getMessage(),e);
			Assert.fail(e.getMessage());
		}
	}
}
