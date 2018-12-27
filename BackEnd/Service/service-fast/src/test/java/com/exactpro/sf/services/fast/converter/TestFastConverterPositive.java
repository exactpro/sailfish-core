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
package com.exactpro.sf.services.fast.converter;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openfast.Message;
import org.openfast.template.BasicTemplateRegistry;

import com.exactpro.sf.common.impl.messages.DefaultMessageFactory;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.util.FastConverterTest;



public class TestFastConverterPositive extends FastConverterTest{
	
	private static FastToIMessageConverter converter;
	private static BasicTemplateRegistry registry;
	private static IMessageToFastConverter backConverter;
	private static Message fastMessage;
	private static IMessage iMessage;
	private static Message fastBackConvertMessage;
	private static final String TEMPLATE_POSITIVE= "fast/templates" +File.separator+"testPositive.xml";
	private static final String NAMESPACE="FAST";
	private static final String DICTIONARY="dictionaries"+File.separator+"TestDictionary.xml";
	private static final String MESSAGE_NAME="testMessage";
	

	@BeforeClass
	public static void setUpClass() throws IOException,ConverterException {
		registry = initRegistry(TEMPLATE_POSITIVE);
		converter = new FastToIMessageConverter(DefaultMessageFactory.getFactory(), NAMESPACE);
		IDictionaryStructure dictionary = initDictionary(DICTIONARY);
		backConverter = new IMessageToFastConverter(dictionary, registry);
		fastMessage = initMessage(registry, MESSAGE_NAME);
		iMessage = converter.convert(fastMessage);
		fastBackConvertMessage = backConverter.convert(iMessage);		
	}
	
	@Test
	public void testConvertMessageIsNotNull(){
		Assert.assertNotNull("Converted IMessage message is null", iMessage);
	}
	
	@Test
	public void testStringx(){
		Assert.assertTrue("Fix fastMessage.stringx != fastBackConvertMessage.stringx", 
				fastMessage.getString("stringx").equals(fastBackConvertMessage.getString("stringx")));
	}
	
	@Test
	public void testInt32(){
		Assert.assertTrue("Fix fastMessage.int32 != fastBackConvertMessage.int32",
				fastMessage.getInt("int32") == fastBackConvertMessage.getInt("int32"));
	}
	
	@Test
	public void testUnsignedInt32(){
		Assert.assertTrue("Fix fastMessage.uInt32 != fastBackConvertMessage.uInt32",
				fastMessage.getLong("uInt32") == fastBackConvertMessage.getLong("uInt32"));
	}

	@Test
	public void testInt64(){
		Assert.assertTrue("Fix fastMessage.int64 != fastBackConvertMessage.int64",
				fastMessage.getLong("int64") == fastBackConvertMessage.getLong("int64"));
	}
	
	@Test
	public void testUnsignedInt64(){
		Assert.assertTrue("Fix fastMessage.uInt64 != fastBackConvertMessage.uInt64",
				fastMessage.getBigDecimal("uInt64").equals(fastBackConvertMessage.getBigDecimal("uInt64")));
	}
	
	@Test
	public void testDecimal(){
		Assert.assertTrue("Fix fastMessage.decimal != fastBackConvertMessage.decimal", 
				fastMessage.getBigDecimal("decimal").equals(fastBackConvertMessage.getBigDecimal("decimal")));
	}
	
	@Test
	public void testByteVector(){
		Assert.assertTrue("Fix fastMessage.bv != fastBackConvertMessage.bv",
				this.isEqual(fastMessage.getBytes("bv"),fastBackConvertMessage.getBytes("bv")));
	}
	
	@Test
	public void testGroup(){
		Assert.assertTrue("Fix fastMessage.grp.stringx != fastBackConvertMessage.grp.stringx",
				fastMessage.getGroup("grp").getString("stringx").equals(fastBackConvertMessage.getGroup("grp").getString("stringx")));
	}
	
	@Test
	public void testSequence(){
		Assert.assertTrue("Fix msg1.grp.stringx != msg.grp.stringx",
				fastMessage.getSequence("sqs").get(0).getString("sqsv").
				equals(fastBackConvertMessage.getSequence("sqs").get(0).getString("sqsv")));
		
		Assert.assertTrue("Fix msg1.grp.stringx != msg.grp.stringx",
				fastMessage.getSequence("sqs").get(1).getString("sqsv").
				equals(fastBackConvertMessage.getSequence("sqs").get(1).getString("sqsv")));
		
		Assert.assertTrue("Fix msg1.grp.stringx != msg.grp.stringx",
				fastMessage.getSequence("seq").get(0).getString("seqv").
				equals(fastBackConvertMessage.getSequence("seq").get(0).getString("seqv")));
		
		Assert.assertTrue("Fix msg1.grp.stringx != msg.grp.stringx",
				fastMessage.getSequence("seq").get(1).getString("seqv").
				equals(fastBackConvertMessage.getSequence("seq").get(1).getString("seqv")));
	}


	@Test
	public void testConvertEmptyMessage() throws ConverterException {
		Message fastMessage = new Message(registry.get("testMessage"));
		IMessage iMesssage = converter.convert(fastMessage);
		Message newFastMessage = backConverter.convert(iMesssage);
		Assert.assertNotNull("Empty message conversion failed", newFastMessage);
	} 
}
