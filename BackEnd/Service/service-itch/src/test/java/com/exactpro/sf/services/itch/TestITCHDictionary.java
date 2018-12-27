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
import java.util.ArrayList;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.util.TestITCHHelper;

import junit.framework.Assert;

public class TestITCHDictionary extends TestITCHHelper {

    private static final Logger logger = LoggerFactory.getLogger(TestITCHDictionary.class);

	@BeforeClass
	public static void setUpClass(){
		logger.info("Start tests of ITCH dictionaries");
	}

	@AfterClass
	public static void tearDownClass(){
		logger.info("Finish tests of ITCH dictionaries");
	}

	/**
     * Test fields and messages in dictionary itch.xml
	 */
	@Test
	public void testDictionary(){
		IDictionaryStructure dictionary=null;
		try {
			dictionary = getDictionary();
		} catch(IOException e){
			logger.error(e.getMessage(),e);
			Assert.fail(e.getMessage());
		}

		Assert.assertEquals("ITCH", dictionary.getNamespace());

		ArrayList<String> fieldNames = new ArrayList<>();
        fieldNames.add(ITCHMessageHelper.FAKE_FIELD_UH_MARKET_DATA_GROUP);
        fieldNames.add(ITCHMessageHelper.FAKE_FIELD_UH_SEQUENCE_NUMBER);
		fieldNames.add("IndexStatus");
		fieldNames.add("Side");
		fieldNames.add("EventCodeType");
		fieldNames.add("AllowedBookType");
        fieldNames.add(ITCHMessageHelper.FAKE_FIELD_MESSAGE_TIME);
        fieldNames.add(ITCHMessageHelper.FAKE_FIELD_MESSAGE_SEQUENCE_NUMBER);
		fieldNames.add("MessageType");
		fieldNames.add("SourceVenue");

		testFieldNames(dictionary, fieldNames);

		ArrayList<String> messageNames = new ArrayList<>();
		messageNames.add("OrderExecuted");
		messageNames.add("SystemEvent");
		messageNames.add("UnitHeader");
		messageNames.add("LoginRequest");
		messageNames.add("SecurityLimits");
		messageNames.add("SecurityClassTickMatrix");
		messageNames.add("AddOrderOneByteLength");
		messageNames.add("AddOrder");
		messageNames.add("Time");
        messageNames.add("LowLatencyIndicesUpdate");
		messageNames.add("AddOrderShortMBP");
		messageNames.add("AddOrderShort");
		messageNames.add("TicksGroup");
		messageNames.add("SymbolDirectory");

		testMessageNames(dictionary, messageNames);
	}

	/**
     * Test fields and messages in dictionary itch_additional.xml
	 */
	@Test
	public void testAdditionalDictionary(){
		IDictionaryStructure dictionary=null;
		try {
			dictionary = getAdditionalDictionary();
		} catch(IOException e){
			logger.error(e.getMessage(),e);
			Assert.fail(e.getMessage());
		}

		Assert.assertEquals("ITCH", dictionary.getNamespace());

		ArrayList<String> fieldNames = new ArrayList<>();
        fieldNames.add(ITCHMessageHelper.FAKE_FIELD_UH_MARKET_DATA_GROUP);
        fieldNames.add(ITCHMessageHelper.FAKE_FIELD_UH_SEQUENCE_NUMBER);
        fieldNames.add(ITCHMessageHelper.FAKE_FIELD_MESSAGE_SEQUENCE_NUMBER);
		fieldNames.add("MessageType");

		testFieldNames(dictionary, fieldNames);

		ArrayList<String> messageNames = new ArrayList<>();
		messageNames.add("UnitHeader");
		messageNames.add("testDate");
		messageNames.add("testDouble");
		messageNames.add("testByte");
		messageNames.add("testString");
		messageNames.add("testInteger");
		messageNames.add("testShort");
		messageNames.add("testIMessage");
		messageNames.add("testMessage");
		messageNames.add("testBigDecimal");
		messageNames.add("testFloat");
		messageNames.add("testLong");
		messageNames.add("testAlphaNotrim");

		testMessageNames(dictionary, messageNames);
	}

	/**
     * Test fields and messages in dictionary itch_DublicateMessageTypeValue.xml
	 */
	@Test
	public void testDublicateMessageDictionary(){
		IDictionaryStructure dictionary=null;
		try {
			dictionary = getDictionaryWithDublicateMessages();
		} catch(IOException e){
			logger.error(e.getMessage(),e);
			Assert.fail(e.getMessage());
		}

		Assert.assertEquals("ITCH", dictionary.getNamespace());
		ArrayList<String> messageNames = new ArrayList<>();
		messageNames.add("LoginRequest");
		messageNames.add("UnitHeader");

		testMessageNames(dictionary, messageNames);

		short messageType=256;
		for ( IMessageStructure msgStruct : dictionary.getMessageStructures() )	{
			Assert.assertEquals(messageType,(short)msgStruct.getAttributeValueByName("MessageType"));
		}
	}

	/**
     * Test fields and messages in dictionary itch_invalid.xml
	 */
	@Test
	public void testInvalidDictionary(){
		IDictionaryStructure dictionary=null;
		try {
			dictionary = getInvalidDictionary();
		} catch(IOException e){
			logger.error(e.getMessage(),e);
			Assert.fail(e.getMessage());
		}

		Assert.assertEquals("ITCH", dictionary.getNamespace());

		ArrayList<String> messageNames = new ArrayList<>();
		messageNames.add("UnitHeader");
		messageNames.add("testDate");
		messageNames.add("testDouble");
		messageNames.add("testByte");
		messageNames.add("testString");
		messageNames.add("testInteger");
		messageNames.add("testShort");
		messageNames.add("testIMessage");
		messageNames.add("testMessage");
		messageNames.add("testBigDecimal");
		messageNames.add("testFloat");
		messageNames.add("testLong");
		messageNames.add("testAlphaNotrim");
		messageNames.add("testType");

		testMessageNames(dictionary, messageNames);
	}
}
