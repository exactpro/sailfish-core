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
package com.exactpro.sf.services.ntg;

import com.exactpro.sf.common.impl.messages.DefaultMessageFactory;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.util.AbstractTest;
import junit.framework.Assert;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.DummySession;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.AbstractProtocolEncoderOutput;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Queue;

public class TestNTGCodecNegative extends AbstractTest {

    private static final Logger logger = LoggerFactory.getLogger(TestNTGCodecNegative.class);

	@BeforeClass
	public static void setUpClass(){
        logger.info("Start negative tests of NTGCodec");
	}

	@AfterClass
	public static void tearDownClass(){
        logger.info("Finish negative tests of NTGCodec");
	}
	/**
	 * Negative test encode Heartbeat message with StartOfMessage=3 and decode it after
	 */
	@Test
	public void testWrongMessage(){
        IMessage message = DefaultMessageFactory.getFactory().createMessage("Heartbeat", "NTG");
        IMessage messageHeader = DefaultMessageFactory.getFactory().createMessage("MessageHeader", "NTG");
		messageHeader.addField("StartOfMessage", 3);
		messageHeader.addField("MessageLength", 9);
		messageHeader.addField("MessageType", "2");
		message.addField("MessageHeader", messageHeader);
		try{
            IDictionaryStructure dictionary = TestNTGHelper.getDictionary();
			IMessageStructure msgStruct = dictionary.getMessageStructure(message.getName());
			Assert.assertNotNull("Message structure is null.", msgStruct);
            NTGCodec encodeCodec = new NTGCodec();
			encodeCodec.init(serviceContext, null, DefaultMessageFactory.getFactory(), dictionary);
            ProtocolEncoderOutput output = (new TestNTGHelper()).new MockProtocolEncoderOutput();
	 		encodeCodec.encode(new DummySession(), message, output);

			Queue<Object> msgQueue = ((AbstractProtocolEncoderOutput) output).getMessageQueue();

			Assert.assertNotNull("Message queue from AbstractProtocolEncoderOutput.", msgQueue);
			Assert.assertTrue("Message queue size must be equal 1.", 1 == msgQueue.size());

			Object lastMessage = msgQueue.element();

			if (lastMessage instanceof IoBuffer) {
                NTGCodec decodeCodec = new NTGCodec();
				decodeCodec.init(serviceContext, null, DefaultMessageFactory.getFactory(), dictionary);
				IoBuffer toDecode = IoBuffer.wrap( new byte[0] );
				toDecode.setAutoExpand(true);
				toDecode.put(((IoBuffer)lastMessage).array());
				toDecode.compact();
				toDecode.order(ByteOrder.LITTLE_ENDIAN);

				IoSession decodeSession = new DummySession();
				decodeCodec.decodable(decodeSession, toDecode);
				Assert.fail("Exception hasn't been thrown");
			}
		}catch(Exception e){
			Assert.assertEquals("Unexpected start of message: 3", e.getMessage());
		}

	}

	/**
     * Negative test of incorrect NTGCodec initialization with null MessageFactory or null DictionaryStructure
	 */
	@Test
	public void testInvalidInitialization(){
        NTGCodec encodeCodec = new NTGCodec();

		try{
            encodeCodec.init(serviceContext, null, null, TestNTGHelper.getDictionary());
			Assert.fail("There is no exception was threw");
		}catch(IOException e){
			logger.error(e.getMessage(),e);
			Assert.fail(e.getMessage());
		}catch(IllegalArgumentException e){
			Assert.assertEquals("Parameter [msgFactory] could not be null", e.getMessage());
		}
		try{
			encodeCodec.init(serviceContext, null, DefaultMessageFactory.getFactory(), null);
			Assert.fail("There is no exception was threw");
		}catch(IllegalArgumentException e){
			Assert.assertEquals("Parameter [dictionary] could not be null", e.getMessage());
		}
	}

	/**
     * Negative test NTGCodec initialization with dictionary, which contains message with duplicate MessageType
	 * @throws Exception
	 */
	@Test
	public void testDublicateMessageTypeValue()throws Exception{
		try{
            IDictionaryStructure dictionary = TestNTGHelper.getDictionaryWithDublicateMessages();
            NTGCodec encodeCodec = new NTGCodec();
			encodeCodec.init(serviceContext, null, DefaultMessageFactory.getFactory(), dictionary);
			Assert.fail("There is no exception was threw");
		}catch(Exception e){
			Assert.assertTrue(e.getMessage(), e.getMessage().startsWith("[MessageType] attribute must be unique for input / output sets."));
		}
	}

	/**
     * Negative test NTGCodec initialization with dictionary, contains Logon message with missed Length attribute
	 */
	@Test
	public void testInvalidFieldLenght(){
		try{
            IDictionaryStructure dictionary = TestNTGHelper.getDictionaryWithMissedLength();
            NTGCodec encodeCodec = new NTGCodec();
			encodeCodec.init(serviceContext, null, DefaultMessageFactory.getFactory(), dictionary);
			Assert.fail("There is no exception was threw");
		}catch(Exception e){
			Assert.assertEquals("Attribute [Length] missed in definition field Username in message Logon",
					e.getMessage());
		}
	}

	/**
	 * Negative test of encode null message
	 */
	@Test
	public void testNullObjectEncode(){
	    try{
            NTGCodec encodeCodec = new NTGCodec();
            encodeCodec.init(serviceContext, null, DefaultMessageFactory.getFactory(), TestNTGHelper.getDictionary());
            ProtocolEncoderOutput output = (new TestNTGHelper()).new MockProtocolEncoderOutput();
			encodeCodec.encode(new DummySession(), null, output);
			Assert.fail("There is no exception was threw");
	    }catch(IllegalArgumentException e){
            Assert.assertEquals("Message parameter is not instance of com.exactpro.sf.common.messages.IMessage", e.getMessage());
	    }catch(Exception e){
	    	logger.error(e.getMessage(),e);
	    	Assert.fail(e.getMessage());
	    }
	}

	/**
	 * Negative test of encode message with null MessageStructure
	 */
	@Test
	public void testNullMessageStructureEncode(){
	    try{
            NTGCodec encodeCodec = new NTGCodec();
			IMessage message = DefaultMessageFactory.getFactory().createMessage("invalid", "invalid");
            encodeCodec.init(serviceContext, null, DefaultMessageFactory.getFactory(), TestNTGHelper.getDictionary());
            ProtocolEncoderOutput output = (new TestNTGHelper()).new MockProtocolEncoderOutput();
			encodeCodec.encode(new DummySession(), message, output);
			Assert.fail("There is no exception was threw");
	    }catch(NullPointerException e){
	    	Assert.assertEquals("MsgStructure is null. Namespace=invalid, MsgName=invalid", e.getMessage());
	    }catch(Exception e){
	    	logger.error(e.getMessage(),e);
	    	Assert.fail(e.getMessage());
	    }
	}

}