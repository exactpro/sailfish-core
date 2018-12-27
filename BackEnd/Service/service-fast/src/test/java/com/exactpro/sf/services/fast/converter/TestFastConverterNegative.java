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
import org.openfast.DecimalValue;
import org.openfast.Message;
import org.openfast.template.BasicTemplateRegistry;

import com.exactpro.sf.common.impl.messages.DefaultMessageFactory;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.util.FastConverterTest;

public class TestFastConverterNegative extends FastConverterTest {
	
	private static FastToIMessageConverter converter;
	private static BasicTemplateRegistry registry;
	private static Message fastMessage;
	private static IMessage iMessage;
	private static final String MESSAGE_NAME="testMessage";
	private static final String NAMESPACE="FAST";
	private static final String TEMPLATE_POSITIVE= "fast/templates" +File.separator+"testPositive.xml";
	private static final String TEMPLATE_NEGATIVE= "fast/templates" +File.separator+"testNegative.xml";
	private static final String TEMPLATE_WRONG_TYPE= "fast/templates" +File.separator+"testWrongType.xml";
	private static final String DICTIONARY_NAME="dictionaries"+File.separator+"TestDictionaryNegative.xml";
	private static final String DICTIONARY_EMPTY_FASTFIELD="dictionaries"+File.separator+"TestDictionaryEmptyFastFieldName.xml";
	
	@BeforeClass
	public static void setUpClass() throws IOException,ConverterException {
		registry=initRegistry(TEMPLATE_POSITIVE);
		converter = new FastToIMessageConverter(DefaultMessageFactory.getFactory(), NAMESPACE);
		fastMessage = initMessage(registry, MESSAGE_NAME);
		iMessage = converter.convert(fastMessage);		
	}
	
	@Test
	public void testConvertMessageWithNullNamespace(){
		try{
			FastToIMessageConverter converter = new FastToIMessageConverter(DefaultMessageFactory.getFactory(), null);
			converter.convert(fastMessage);
			Assert.fail("Convert with null namespace has been done.");
		}catch(Exception e){
			Assert.assertTrue(e instanceof ConverterException);
			Assert.assertEquals("Can not create fast IMessage for template id "+MESSAGE_NAME,e.getMessage());
		}
	}

	@Test
	public void testSetUpIncorrectField(){
		try{
			fastMessage.setFieldValue("wrong_field", new DecimalValue(-1));
			Assert.fail("Field \"wrong_field\" can not be set up");
		}catch(Exception e){
			Assert.assertEquals("The field wrong_field does not exist in group "+MESSAGE_NAME, e.getMessage());
		}
	}

	@Test
	public void testUploadIncorrectTemplate(){
		try{
			initRegistry(TEMPLATE_WRONG_TYPE);
			Assert.fail("Template testNegative.xml can not be upload.");
		}catch(Exception e){
			Assert.assertEquals("No parser registered for wrong", e.getMessage());
		}
	}
	
	@Test
	public void testDictionaryWrongMessageName(){
		try{
			IDictionaryStructure dictionary = initDictionary(DICTIONARY_NAME);		
			IMessageToFastConverter backConverter = new IMessageToFastConverter(dictionary, registry);
			backConverter.convert(iMessage);
			Assert.fail("Convert toFastMessage can not be done.");
		}catch(Exception e){
			Assert.assertEquals("Can not find message "+MESSAGE_NAME+" in the namespace "+NAMESPACE, e.getMessage());
		}
	}
	
	@Test
	public void testEmptyFastFieldName(){
		try{
			BasicTemplateRegistry registry = initRegistry(TEMPLATE_NEGATIVE); 
			FastToIMessageConverter converter = new FastToIMessageConverter(DefaultMessageFactory.getFactory(), "FAST");
			Message fastMessage = new Message(registry.get(MESSAGE_NAME));
			fastMessage.setString("stringx", "str");
			IMessage iMessage = converter.convert(fastMessage);
			IDictionaryStructure dictionary = initDictionary(DICTIONARY_EMPTY_FASTFIELD);		
			IMessageToFastConverter backConverter = new IMessageToFastConverter(dictionary, registry);
			backConverter.convert(iMessage);
			Assert.fail("Convert can not be done");
		}catch(Exception e){
			Assert.assertEquals("Failed to get fast field name for field :stringx message=stringx=str", e.getMessage());			
		}
	}
			
}
