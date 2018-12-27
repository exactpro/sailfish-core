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

import com.exactpro.sf.common.impl.messages.DefaultMessageFactory;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.services.MockProtocolDecoderOutput;
import com.exactpro.sf.services.MockProtocolEncoderOutput;
import com.exactpro.sf.storage.util.JsonMessageConverter;
import com.exactpro.sf.util.TestITCHHelper;
import junit.framework.Assert;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.DummySession;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.nio.ByteOrder;

public class TestITCHCodecNegative extends TestITCHHelper {

    private static final Logger logger = LoggerFactory.getLogger(TestITCHCodecNegative.class);

	@BeforeClass
	public static void setUpClass(){
		logger.info("Start negative tests of ITCH codec");
	}

	@AfterClass
	public static void tearDownClass(){
		logger.info("Finish negative tests of ITCH codec");
	}

	/**
     * Try to initialized ITCHCodec without dictionary
	 */
	@Test
	public void invalidInitialization(){
		try{
            ITCHCodec codec = new ITCHCodec();
			codec.init(serviceContext, null, getMessageHelper().getMessageFactory(), null);
        	Assert.fail("There is no exception was threw");
		}catch(NullPointerException e){
			Assert.assertEquals("'Dictionary' parameter cannot be null", e.getMessage());
		}
	}

	/**
	 * Try to initialized codec with dictionary contain duplicate MessageType
	 */
	@Test
	public void testDublicateMessageTypeValue(){
		try{
			IDictionaryStructure dictionary = getDictionaryWithDublicateMessages();
            ITCHCodec codec = new ITCHCodec();
			codec.init(serviceContext, null, getMessageHelper().getMessageFactory(), dictionary);
			Assert.fail("There is no exception was threw");
		}catch(EPSCommonException e){
			Assert.assertTrue(e.getMessage(), e.getMessage().startsWith("MessageType attribute should be unique. MessageName:"));
		}catch(Exception e){
			logger.error(e.getMessage(),e);
			Assert.fail(e.getMessage());
		}
	}

	/**
	 * Try to decode from short empty byte[] and try to decode from byte[], shorter then UnitHeader
	 */
	@Test
	public void testDecodeFromInvalidBytes(){
		try{
            ITCHCodec codec = (ITCHCodec) getMessageHelper().getCodec(serviceContext);
			byte[] b = new byte[1];

			IoBuffer toDecode = IoBuffer.wrap( b );
			toDecode.order(ByteOrder.LITTLE_ENDIAN);
			toDecode.position(0);
			IoSession decodeSession = new DummySession();
			MockProtocolDecoderOutput decoderOutput = new MockProtocolDecoderOutput();
			boolean decodableResult = codec.doDecode( decodeSession, toDecode, decoderOutput );
			Assert.assertFalse("Empty byte[] can not be decode",decodableResult);

			b = new byte[3];
			b[0]=0x2E;
			b[1]=0x00;
			b[2]=0x01;
			toDecode = IoBuffer.wrap( b );
			toDecode.order(ByteOrder.LITTLE_ENDIAN);
			toDecode.position(0);
			decodeSession = new DummySession();
			decoderOutput = new MockProtocolDecoderOutput();
			decodableResult = codec.doDecode( decodeSession, toDecode, decoderOutput );
			Assert.assertFalse("Empty byte[] can not be decode",decodableResult);
		}catch(Exception e){
			logger.error(e.getMessage(),e);
			Assert.fail(e.getMessage());
		}
	}

	/**
	 * Try to encode and decode AddOrder message with invalid MessageType
	 */
	@Test
	public void testInvalidMessageType(){
		short messageType=0;
		try{
			IMessage message=getMessageCreator().getAddOrder();
			message.addField("MessageType", messageType);
			message=getMessageHelper().prepareMessageToEncode(message, null);
			IMessage decodedMessage=decode(encode(message,null),null);
			Assert.fail("There is no exception was threw. Result message after encode/decode:"+
                    JsonMessageConverter.toJson(decodedMessage, getMessageHelper().getDictionaryStructure()));
		}catch(EPSCommonException e){
			Assert.assertEquals("Unknown messageType = [" + messageType + "]", e.getMessage());
		}catch(Exception e){
			logger.error(e.getMessage(),e);
			Assert.fail(e.getMessage());
		}
	}

	/**
	 * Try to encode BigDecimal value as a message
	 */
	@Test
	public void testEncodeInvalidInstanceMessage(){
		try{
			BigDecimal message = new BigDecimal(99998);
            ITCHCodec codec = new ITCHCodec();
			IoSession session = new DummySession();
			ProtocolEncoderOutput output = new MockProtocolEncoderOutput();
			session.write(message);
            codec.init(serviceContext, null, getMessageHelper().getMessageFactory(), getMessageHelper().getDictionaryStructure());
			codec.encode(session, message, output);
			Assert.fail("There is no exception was threw");
		}catch(IllegalArgumentException e){
            Assert.assertEquals("Message parameter is not instance of com.exactpro.sf.common.messages.IMessage", e.getMessage());
		}catch(Exception e){
			logger.error(e.getMessage(),e);
			Assert.fail(e.getMessage());
		}
	}

	/**
	 * Try to encode message without UnitHeader
	 */
	@Test
	public void testUnpreparedMessage(){
		try{
			IMessage message= getMessageCreator().getAddOrder();
			encode(message,null);
			Assert.fail("There is no exception was threw");
		}catch(NullPointerException e){
            Assert.assertEquals("MessagesList didn't contain field: " + ITCHMessageHelper.SUBMESSAGES_FIELD_NAME, e.getMessage());
		}catch(Exception e){
			logger.error(e.getMessage(),e);
			Assert.fail(e.getMessage());
		}
	}

	/**
	 * Try to encode prepared AddOrder message with uninitialized codec
	 */
	@Test
	public void testUninitiliazedCodecEncode(){
		try{
			IMessage message= getMessageCreator().getAddOrder();
			message=getMessageHelper().prepareMessageToEncode(message, null);
            ITCHCodec codec = new ITCHCodec();
			IoSession session = new DummySession();
			ProtocolEncoderOutput output = new MockProtocolEncoderOutput();
			session.write(message);
            codec.encode(session, message, output);
			Assert.fail("There is no exception was threw");
		}catch(NullPointerException e){
			Assert.assertEquals("ITCH Encode: msgDictionary is not defined", e.getMessage());
		}catch(Exception e){
			logger.error(e.getMessage(),e);
			Assert.fail(e.getMessage());
		}
	}

	/**
	 * Try to encode message with invalid name and namespace. MessageStructure is null for its message
	 */
	@Test
	public void testNullMessageStructureEncode(){
		String test="invalid";
		try{
            ITCHCodec codec = new ITCHCodec();
			IMessage message = DefaultMessageFactory.getFactory().createMessage(test, test);
			message=getMessageHelper().prepareMessageToEncode(message, null);
			IoSession session = new DummySession();
			ProtocolEncoderOutput output = new MockProtocolEncoderOutput();
			session.write(message);
            codec.init(serviceContext, null, getMessageHelper().getMessageFactory(), getMessageHelper().getDictionaryStructure());
			codec.encode(session, message, output);
			Assert.fail("There is no exception was threw");
	    }catch(EPSCommonException e){
	    	Assert.assertEquals("Could not find IMessageStructure for messageName=[" + test + "] Namespace=[" + test + "]", e.getMessage());
	    }catch(Exception e){
	    	logger.error(e.getMessage(),e);
	    	Assert.fail(e.getMessage());
	    }
	}
}
