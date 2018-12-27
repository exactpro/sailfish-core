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
import com.exactpro.sf.common.impl.messages.MapMessage;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.util.ErrorUtil;
import com.exactpro.sf.services.MockProtocolDecoderOutput;
import com.exactpro.sf.storage.util.JsonMessageConverter;
import com.exactpro.sf.util.AbstractTest;
import com.exactpro.sf.util.DateTimeUtility;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.DummySession;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.AbstractProtocolDecoderOutput;
import org.apache.mina.filter.codec.AbstractProtocolEncoderOutput;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.ByteOrder;
import java.util.Queue;

public class TestNTGCodecPositive extends AbstractTest {

    private static final Logger logger = LoggerFactory.getLogger(TestNTGCodecPositive.class);

	@BeforeClass
	public static void setUpClass(){
        logger.info("Start positive tests of NTGCodec");
	}

	@AfterClass
	public static void tearDownClass(){
        logger.info("Finish positive tests of NTGCodec");
	}
	/**
	 * Test encode and decode heartbeat message and comparing result message and original message
	 */
	@Test
	public void testEncodeDecode_Heartbeat()  {
		try{
            IMessage message = TestNTGHelper.getHeartbeat();
            testRoundTrip(message, TestNTGHelper.getDictionary());
		}catch(Exception e){
			logger.error(e.getMessage(),e);
			Assert.fail(e.getMessage());
		}

	}

	/**
	 * Test encode and decode NewOrder message and comparing result message and original message
	 */
	@Test
	public void testEncodeDecode_NewOrder()  {
		try{
            IMessage message = TestNTGHelper.getNewOrder();
            testRoundTrip(message, TestNTGHelper.getDictionary());
		}catch(Exception e){
			logger.error(e.getMessage(),e);
			Assert.fail(e.getMessage());
		}
	}

	/**
	 * Test encode and decode Logon message and comparing result message and original message
	 */
	@Test
	public void testEncodeDecode_Logon()  {
		try{
            IMessage message = TestNTGHelper.getLogon();
            testRoundTrip(message, TestNTGHelper.getDictionary());
		}catch(Exception e){
			logger.error(e.getMessage(),e);
			Assert.fail(e.getMessage());
		}
	}

	/**
	 * Test encode and decode ExecutionReport message and comparing result message and original message
	 */
	@Test
	public void testEncodeDecode_ExecutionReport() {
		IMessage execReport = DefaultMessageFactory.getFactory()
                .createMessage("ExecutionReport", TestNTGHelper.nameSpace);

        String execID = NTGUtility.getRandomString(12);
        String orderID = NTGUtility.getRandomString(12);
		int leavesQty = 1000;
		int displayQty = 900;

		execReport.addField("AppID", 123);
		execReport.addField("SequenceNo", 12345);
		execReport.addField("ExecID", execID);
		execReport.addField("OrderID", orderID);
        execReport.addField("ExecType", "0"); // com.exactpro.sf.messages.ntg.components.ExecType.New
		execReport.addField("ExecRefID", execID);
        execReport.addField("OrdStatus", 0); // com.exactpro.sf.messages.ntg.components.OrderStatus.New
		execReport.addField("LeavesQty", leavesQty);
		execReport.addField("ExecutedQty", 0);
        execReport.addField("ClOrdID", NTGUtility.getNewClOrdID());
		execReport.addField("DisplayQty", displayQty);
		execReport.addField("CommonSymbol", "VODl");
        execReport.addField("Side", 2);//com.exactpro.sf.messages.ntg.components.Side.Sell
        execReport.addField("TargetBook", 1); // com.exactpro.sf.messages.ntg.components.TargetBook.IntegratedOrderBook
		execReport.addField("Counterparty", "Counterpa");
		// execReport.addField("TradeLiquidityIndicator", "C");
		execReport.addField("TradeMatchID", 1L);
        String s = NTGUtility.getTransactTime();
		execReport.addField("TransactTime", s);
		try{
            testRoundTrip(TestNTGHelper.getMessageHelper(false).prepareMessageToEncode(execReport, null),
                    TestNTGHelper.getDictionary());
		}catch(IOException e){
			logger.error(e.getMessage(),e);
			Assert.fail(e.getMessage());
		}

	}

	/**
	 * Test decode from bytes OrderReject message

	 */
	@Test
	public void testDecodeOrderRejectFromBytes(){
		int[] ints = new int[]{
				0x02, 0x45, 0x00, 0x39, 0x01, 0xf2, 0xdd, 0x00, 0x00, 0x6f, 0x62, 0x75, 0x79, 0x2d, 0x31, 0x32,
				0x37, 0x34, 0x37, 0x31, 0x34, 0x34, 0x38, 0x37, 0x33, 0x37, 0x35, 0x00, 0x61, 0x4f, 0x31, 0x31,
				0x41, 0x6c, 0x41, 0x5f, 0x34, 0x00, 0x37, 0x31, 0x34, 0x34, 0x38, 0x37, 0x33, 0x37, 0x35, 0x00,
				0x61, 0x63, 0xb1, 0x04, 0x00, 0x00, 0x67, 0x8b, 0xfa, 0x4b, 0x18, 0xd8, 0x05, 0x00, 0x00, 0x00,
				0x00, 0x00, 0x00, 0x00, 0x00};
		try{
			decodeBytes(ints);
		}catch(Exception e){
			logger.error(e.getMessage(),e);
			Assert.fail(e.getMessage());
		}

	}

	/**
	 * Test decode from bytes ExecutionReport message
	 */
	@Test
	public void testDecodeExecutionReportFromBytes()
	{
		int[] ints = new int[]{
				0x02, 0x97, 0x00, 0x38, 0x01, 0xd3, 0xdb, 0x04, 0x00, 0x45, 0x30, 0x31, 0x78, 0x4d, 0x63, 0x47,
				0x36, 0x55, 0x46, 0x36, 0x4f, 0x31, 0x32, 0x37, 0x36, 0x36, 0x38, 0x35, 0x37, 0x38, 0x36, 0x33,
				0x39, 0x35, 0x30, 0x30, 0x30, 0x30, 0x30, 0x31, 0x33, 0x4f, 0x31, 0x31, 0x43, 0x53, 0x6e, 0x5f,
				0x32, 0x30, 0x32, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x46, 0x00,
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00,
				0x00, 0x37, 0x17, 0x89, 0x00, 0x00, 0x00, 0x00, 0x40, 0x9a, 0x1e, 0x02, 0x60, 0x8c, 0xda, 0x01,
				0x00, 0x60, 0x8c, 0xda, 0x01, 0x41, 0x42, 0x42, 0x4e, 0x7a, 0x00, 0x01, 0xca, 0x00, 0x00, 0x00,
				0x45, 0xca, 0x2e, 0x00, 0x45, 0x75, 0x72, 0x6f, 0x43, 0x43, 0x50, 0x00, 0x00, 0x00, 0x00, 0x52,
				0xa9, 0x75, 0x4a, 0x00, 0x5c, 0xdd, 0x58, 0x02, 0x80, 0x01, 0x19, 0x4c, 0x18, 0x3d, 0x0a, 0x00,
				0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};

		try{
			decodeBytes(ints);
		}catch(Exception e){
			logger.error(e.getMessage(),e);
			Assert.fail(e.getMessage());
		}

		ints = new int[]{
				0x02, 0x97, 0x00, 0x38, 0x01, 0xF5, 0x58, 0x03, 0x00, 0x45, 0x30, 0x31, 0x76, 0x6B, 0x72, 0x58,
				0x30, 0x76, 0x39, 0x44, 0x59, 0x34, 0x32, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x32, 0x00,
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x4F, 0x31, 0x31, 0x43, 0x49, 0x55, 0x5F,
				0x31, 0x31, 0x32, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x46, 0x00,
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00,
				0xFF, 0x15, 0xD6, 0x36, 0x00, 0x00, 0x00, 0x00, 0xE0, 0x22, 0x02, 0x00, 0x30, 0x75, 0x00, 0x00,
				0x00, 0x30, 0x75, 0x00, 0x00, 0x41, 0x43, 0x41, 0x70, 0x00, 0x00, 0x01, 0x70, 0x00, 0x00, 0x00,
				0x85, 0x2A, 0x2E, 0x00, 0x45, 0x75, 0x72, 0x6F, 0x43, 0x43, 0x50, 0x00, 0x00, 0x00, 0x00, 0x52,
				0xAB, 0xF2, 0xEE, 0x00, 0x8C, 0xDB, 0x58, 0x02, 0xCF, 0xF3, 0x15, 0x4C, 0x28, 0x4C, 0x0E, 0x00,
				0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};

		try{
			decodeBytes(ints);
		}catch(Exception e){
			logger.error(e.getMessage(),e);
			Assert.fail(e.getMessage());
		}

	}

	/**
	 * Test decode from bytes CancelReject message
	 */
	@Test
	public void testDecodeCancelRejectFromBytes()
	{
		int[] ints = new int[]{
				0x02, 0x3c, 0x00, 0x39, 0x01, 0x83, 0x0f, 0x03, 0x00, 0x31, 0x33, 0x39, 0x34, 0x34, 0x35, 0x31,
				0x30, 0x30, 0x30, 0x34, 0x30, 0x32, 0x30, 0x30, 0x30, 0x30, 0x30, 0x31, 0x35, 0x4e, 0x4f, 0x4e,
				0x45, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xd0, 0x07, 0x00, 0x00, 0xea, 0xad, 0x1d,
				0x53, 0x60, 0xea, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};

		try{
			decodeBytes(ints);
		}catch(Exception e){
			logger.error(e.getMessage(),e);
			Assert.fail(e.getMessage());
		}

	}


	/**
	 * Test encode and decode test message with Float type of variable.
	 * Message defined in special dictionary.
	 */
	@Test
	public void testEncodeDecodeMessageWithFloatType() {
        IMessage message = DefaultMessageFactory.getFactory().createMessage("TestFloat", "NTG");
        IMessage messageHeader = DefaultMessageFactory.getFactory().createMessage("MessageHeader", "NTG");
		messageHeader.addField("StartOfMessage", 2);
		messageHeader.addField("MessageLength", 5);
		messageHeader.addField("MessageType", "5");
		message.addField("MessageHeader", messageHeader);
		message.addField("testFloat", 3.14f);
		try{
            testRoundTrip(message, TestNTGHelper.getDictionaryWithDifferentTypesMessages());
		}catch(Exception e){
			logger.error(e.getMessage(),e);
			Assert.fail(e.getMessage());
		}
	}

	/**
	 * Test encode and decode test message with Double type of variable.
	 * Message defined in special dictionary.
	 */
	@Test
	public void testEncodeDecodeMessageWithDoubleType() {
        IMessage message = DefaultMessageFactory.getFactory().createMessage("TestDouble", "NTG");
        IMessage messageHeader = DefaultMessageFactory.getFactory().createMessage("MessageHeader", "NTG");
		messageHeader.addField("StartOfMessage", 2);
		messageHeader.addField("MessageLength", 5);
		messageHeader.addField("MessageType", "6");
		message.addField("MessageHeader", messageHeader);
		message.addField("testDouble", 3.14d);
		try{
            testRoundTrip(message, TestNTGHelper.getDictionaryWithDifferentTypesMessages());
		}catch(Exception e){
			logger.error(e.getMessage(),e);
			Assert.fail(e.getMessage());
		}
	}

	/**
	 * Test encode and decode test message with Date type of variable.
	 * Message defined in special dictionary.
	 */
	@Test
	public void testEncodeDecodeMessageWithDateType() {
        IMessage message = DefaultMessageFactory.getFactory().createMessage("TestDate", "NTG");
        IMessage messageHeader = DefaultMessageFactory.getFactory().createMessage("MessageHeader", "NTG");
		messageHeader.addField("StartOfMessage", 2);
		messageHeader.addField("MessageLength", 9);
		messageHeader.addField("MessageType", "4");
		message.addField("MessageHeader", messageHeader);
		message.addField("testDate", DateTimeUtility.nowLocalDateTime());
		try{
            testRoundTrip(message, TestNTGHelper.getDictionaryWithDifferentTypesMessages());
		}catch(Exception e){
			logger.error(e.getMessage(),e);
			Assert.fail(e.getMessage());
		}
	}

	/**
	 * Test encode and decode test message with BigDecimal type of variable and with attribute Type=Uint64.
	 * Message defined in special dictionary.
	 */

	@Test
	public void testEncodeDecodeMessageWithBigDecimalUIntType() {
        IMessage message = DefaultMessageFactory.getFactory().createMessage("TestBigDecimal", "NTG");
        IMessage messageHeader = DefaultMessageFactory.getFactory().createMessage("MessageHeader", "NTG");
		messageHeader.addField("StartOfMessage", 2);
		messageHeader.addField("MessageLength", 9);
		messageHeader.addField("MessageType", "3");
		message.addField("MessageHeader", messageHeader);
		message.addField("testBigDecimal", new BigDecimal(10));
		try{
            testRoundTrip(message, TestNTGHelper.getDictionaryWithDifferentTypesMessages());
		}catch(Exception e){
			logger.error(e.getMessage(),e);
			Assert.fail(e.getMessage());
		}
	}


	/**
	 * Test encode and decode test message with BigDecimal type of variable and with attribute Type=Price.
	 * Message defined in special dictionary.
	 */
	@Test
	public void testEncodeDecodeMessageWithBigDecimalPriceType() {
        IMessage message = DefaultMessageFactory.getFactory().createMessage("TestBigDecimalPrice", "NTG");
        IMessage messageHeader = DefaultMessageFactory.getFactory().createMessage("MessageHeader", "NTG");
		messageHeader.addField("StartOfMessage", 2);
		messageHeader.addField("MessageLength", 9);
		messageHeader.addField("MessageType", "2");
		message.addField("MessageHeader", messageHeader);
		message.addField("testBigDecimalPrice", new BigDecimal(10));
		try{
            testRoundTrip(message, TestNTGHelper.getDictionaryWithDifferentTypesMessages());
		}catch(Exception e){
			logger.error(e.getMessage(),e);
			Assert.fail(e.getMessage());
		}
	}

	private void testRoundTrip(IMessage message, IDictionaryStructure dictionary) {
		try
		{
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
				AbstractProtocolDecoderOutput decoderOutput = new MockProtocolDecoderOutput();

				IoBuffer toDecode = IoBuffer.wrap( new byte[0] );
				toDecode.setAutoExpand(true);
				toDecode.put(((IoBuffer)lastMessage).array());
				//IoBuffer.wrap( Arrays.copyOf(((IoBuffer)lastMessage).array(), ((IoBuffer)lastMessage).limit() ));
				toDecode.compact();
				toDecode.order(ByteOrder.LITTLE_ENDIAN);

				IoSession decodeSession = new DummySession();
				boolean decodableResult = decodeCodec.decodable(decodeSession, toDecode);
				decoderOutput = new MockProtocolDecoderOutput();

				Assert.assertTrue("Test for decoding error.", decodableResult);

				boolean decodeResult = decodeCodec.doDecode(decodeSession, toDecode, decoderOutput);
				Assert.assertTrue("Decoding error.", decodeResult);

				Assert.assertTrue( "Message queue size must not less then 1.", 1 <= decoderOutput.getMessageQueue().size());
				Object decodedMessage = decoderOutput.getMessageQueue().element();

				Assert.assertTrue( "Object must be instance of IMessage.", (decodedMessage instanceof IMessage) );
				Assert.assertTrue( "Object must be instance of MapMessage.", (decodedMessage instanceof MapMessage) );
                Assert.assertTrue("Messages must be equal. Original message:" + JsonMessageConverter.toJson(message, dictionary) + "; "
                        + "result message:" + JsonMessageConverter.toJson((IMessage)decodedMessage, dictionary),
                        message.compare((MapMessage) decodedMessage));
			}
		}
		catch (Exception e)
		{
		    logger.error(e.getMessage(), e);
			Assert.fail(ErrorUtil.formatException(e));
		}
	}

	private void decodeBytes(int[] ints) throws Exception{
		byte[] bytes = new byte[ints.length];
		for (int i =0; i<ints.length; i++)
		{
			bytes[i] = (byte) ints[i];
		}

        NTGCodec decodeCodec2 = new NTGCodec();
        decodeCodec2.init(serviceContext, null, DefaultMessageFactory.getFactory(), TestNTGHelper.getDictionary());
		ProtocolDecoderOutput decoderOutput2 = new MockProtocolDecoderOutput();

		IoBuffer toDecode = IoBuffer.wrap( new byte[0] );
		toDecode.setAutoExpand(true);
		toDecode.put(bytes);
		//IoBuffer.wrap( Arrays.copyOf(((IoBuffer)lastMessage).array(), ((IoBuffer)lastMessage).limit() ));
		toDecode.compact();
		toDecode.order(ByteOrder.LITTLE_ENDIAN);

		IoSession decodeSession2 = new DummySession();
		boolean decodableResult = decodeCodec2.decodable( decodeSession2, toDecode );
		Assert.assertTrue( "Test for decoding error.", decodableResult);
		decoderOutput2 = new MockProtocolDecoderOutput();

		boolean decodeResult = decodeCodec2.doDecode( decodeSession2, toDecode, decoderOutput2 );
		Assert.assertTrue( "Decoding error.", decodeResult );

		Assert.assertTrue( "Message queue size must not less then 1.", 1 <= ((AbstractProtocolDecoderOutput)decoderOutput2).getMessageQueue().size() );
		Object decodedMessage2 = ((AbstractProtocolDecoderOutput)decoderOutput2).getMessageQueue().element();

		Assert.assertTrue( "Object must be instance of IMessage.", (decodedMessage2 instanceof IMessage) );
		Assert.assertTrue( "Object must be instance of MapMessage.", (decodedMessage2 instanceof MapMessage) );
	}


}