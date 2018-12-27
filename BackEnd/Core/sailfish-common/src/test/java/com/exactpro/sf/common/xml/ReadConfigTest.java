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
package com.exactpro.sf.common.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.exactpro.sf.common.impl.messages.xml.XMLTransmitter;
import com.exactpro.sf.common.impl.messages.xml.configuration.Attribute;
import com.exactpro.sf.common.impl.messages.xml.configuration.Dictionary;
import com.exactpro.sf.common.impl.messages.xml.configuration.Field;
import com.exactpro.sf.common.impl.messages.xml.configuration.JavaType;
import com.exactpro.sf.common.impl.messages.xml.configuration.Message;
import com.exactpro.sf.common.messages.DefaultMessageStructureVisitor;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.MessageStructureWriter;
import com.exactpro.sf.common.messages.structures.IAttributeStructure;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.messages.structures.loaders.IDictionaryStructureLoader;
import com.exactpro.sf.common.messages.structures.loaders.XmlDictionaryStructureLoader;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.util.EPSTestCase;

public class ReadConfigTest extends EPSTestCase {

	private String PATH_PREFIX = getBaseDir() + File.separator + "src" + File.separator + "test" + File.separator + "resources" + File.separator ;
	
	@Test
	public void test0() throws Exception {

		Dictionary dict = new Dictionary();
		dict.setName("NAME");

		Field field = new Field();
		field.setDescription("description");
		field.setIsCollection(false);
		field.setName("name");
		field.setId("idshnik");
		field.setType(JavaType.JAVA_LANG_BYTE);
		Attribute tmp = new Attribute();
		tmp.setName("FIRST");
		tmp.setType(JavaType.JAVA_LANG_BYTE);
		tmp.setValue("1");
		field.getValues().add(tmp);
		tmp = new Attribute();
		tmp.setName("SECOND");
		tmp.setType(JavaType.JAVA_LANG_BYTE);
		tmp.setValue("2");
		field.getValues().add(tmp);
		tmp = new Attribute();
		tmp.setName("THIRD");
		tmp.setType(JavaType.JAVA_LANG_BYTE);
		tmp.setValue("3");
		field.getValues().add(tmp);
		dict.setFields(new Dictionary.Fields());
		dict.getFields().getFields().add(field);

		Message message = new Message();
		message.setName("message_name");

		Field mfield = new Field();
		mfield.setName("f_name");
		mfield.setReference(field);
		message.getFields().add(mfield);
		dict.setMessages(new Dictionary.Messages());
		dict.getMessages().getMessages().add(message);

		XMLTransmitter
				.getTransmitter()
				.marshal(
						dict,
						new File(PATH_PREFIX, "simple.xml"),
						new File(
								getClass()
										.getResource(XmlDictionaryStructureLoader.DEFAULT_SCHEMA_VALIDATOR)
										.toURI()));

	}

	@Test
	public void test1() throws Exception {
		IDictionaryStructureLoader loader = new XmlDictionaryStructureLoader();

		File xmlFile = new File(PATH_PREFIX, "native.xml");

		IDictionaryStructure dictionary;
		
		try (InputStream in = new FileInputStream(xmlFile)) {
			dictionary = loader.load(in);
    	}

		MessageStructureWriter wtraverser = new MessageStructureWriter();

		TestVisitor visitor = new TestVisitor();

		for (IMessageStructure imstructure : dictionary.getMessageStructures()) {

			System.out.println(" message = " + imstructure.getName());
			wtraverser.traverse(visitor, imstructure);

		}
		
		IMessageStructure heartbeat = dictionary.getMessageStructure("Heartbeat");
		IFieldStructure messageHeaderField = heartbeat.getField("MessageHeader");
		Assert.assertArrayEquals(
				new String[] { "inclusion", "length" }, 
				messageHeaderField.getAttributeNames().toArray(new String[0]) 
				);
		
		IMessageStructure messageHeader = dictionary.getMessageStructure("MessageHeader");
		IFieldStructure messageTypeFieldStructure = messageHeader.getField("MessageType");
		
		IFieldStructure messageTypeFieldType = dictionary.getFieldStructure("MessageType");
		
		Assert.assertTrue(messageTypeFieldStructure.isEnum());
		Assert.assertTrue(messageTypeFieldType.isEnum());
		
		Assert.assertArrayEquals(extractAlias(messageTypeFieldType.getValues()), extractAlias(messageTypeFieldStructure.getValues()));
		
		Assert.assertEquals(20, messageTypeFieldStructure.getAttributeValueByName("Index"));
		
		IMessageStructure missedMessageRequest = dictionary.getMessageStructure("MissedMessageRequest");
		IFieldStructure appID = missedMessageRequest.getField("AppID");
		
		Assert.assertEquals((short)2, appID.getDefaultValue());
	}

	@Test
    public void swiftTest() throws Exception {
		IDictionaryStructureLoader loader = new XmlDictionaryStructureLoader();
		File xmlFile = new File(PATH_PREFIX, "swift.xml");
		
		IDictionaryStructure adopted;
		
		try (InputStream in = new FileInputStream(xmlFile)) {
			adopted = loader.load(in);
    	}

		IMessageStructure structure = adopted.getMessageStructure("RepetitiveMandatorySubsequenceA1Linkages");
		Assert.assertNotSame(0, structure.getFields().size());
		IFieldStructure tmp = null;
		for(IFieldStructure fieldStructure : structure.getFields()){
			if("20C".equals(fieldStructure.getName())){
				tmp = fieldStructure;
			}
		}
		Assert.assertNotNull(tmp);

		for (String attributeName : tmp.getAttributes().keySet()) {
			System.out.println(tmp.getAttributeValueByName(attributeName));
		}
    }

	@Test
	public void swift518Test() throws Exception {
		IDictionaryStructureLoader loader = new XmlDictionaryStructureLoader();
		File xmlFile = new File(PATH_PREFIX, "swift.xml");

		IDictionaryStructure dictionary;
		
		try (InputStream in = new FileInputStream(xmlFile)) {
			dictionary = loader.load(in);
    	}
		
		IMessageStructure structure = dictionary.getMessageStructure("518");
		Assert.assertNotSame(0, structure.getFields().size());
		System.out.println(structure.getFields().size());

		int messageCount518 = 0;

		for (IFieldStructure fieldStructure0 :  structure.getFields()) { 
			System.out.println(fieldStructure0.getName() + " " + fieldStructure0.getStructureType());
			if (fieldStructure0.isComplex()) {
				messageCount518++;
				if( fieldStructure0.getName().equals("SMandatorySequenceBConfirmationDetails") ){
					Assert.assertEquals(3, fieldStructure0.getFields().size());
					for (IFieldStructure fieldStructure1 : fieldStructure0.getFields()) {
						System.out.println(fieldStructure1.getName() + " " + fieldStructure1.getStructureType());
						if (fieldStructure1.isComplex()) {
							for (IFieldStructure fieldStructure2 : fieldStructure1.getFields()) {
								System.out.println(fieldStructure2.getName() + " " + fieldStructure2.getStructureType());
							}
						}
					}
				}
			}
		}
		Assert.assertEquals(messageCount518, 4);
	}

    @Test
    public void recursionTest() throws Exception {
        IDictionaryStructureLoader loader = new XmlDictionaryStructureLoader();
        File xmlFile = new File(PATH_PREFIX, "recursion.xml");

        try (InputStream in = new FileInputStream(xmlFile)) {
            try {
            loader.load(in);
            } catch (EPSCommonException e) {
                Assert.assertEquals("Message 'message_name', problem with content", e.getMessage());
                Assert.assertEquals("Recursion at message id: 'self' has been detected!", e.getCause().getMessage());
                return;
            }
        }
        Assert.fail("Incorrect structure has been sucess loaded.");
    }

    @Test
    public void duplicateNameTest() throws Exception {
        IDictionaryStructureLoader loader = new XmlDictionaryStructureLoader();
        File xmlFile = new File(PATH_PREFIX, "sameNames.xml");

        try (InputStream in = new FileInputStream(xmlFile)) {
            try {
                loader.load(in);
            } catch (EPSCommonException e) {
                Assert.assertTrue(e.getMessage().contains("message_name") && e.getMessage().contains("Messages with same names has been detected"));
                return;
            }
        }
        Assert.fail("Incorrect structure has been sucess loaded.");
    }

	public void simpleTest() throws Exception {
		IDictionaryStructureLoader loader = new XmlDictionaryStructureLoader();
        File xmlFile = new File(PATH_PREFIX, "simple.xml");

        try (InputStream in = new FileInputStream(xmlFile)) {
        	loader.load(in);
    	}
    }

	private static class TestVisitor extends DefaultMessageStructureVisitor {

		private void visitField(String fieldName, String type, IFieldStructure fldStruct, String value) {
			System.out.println(fieldName + "  " + type + " " + value);
			for (String aname : fldStruct.getAttributes().keySet()) {
				System.out.println(aname + " " + fldStruct.getAttributeValueByName(aname));
			}
		}

		@Override
		public void visit(String fieldName, Byte value,
				IFieldStructure fldStruct, boolean isDefault) {
			visitField(fieldName, "Byte", fldStruct, "" + value);
		}

		@Override
		public void visit(String fieldName, Character value,
				IFieldStructure fldStruct, boolean isDefault) {
			visitField(fieldName, "Character", fldStruct, "" + value);
		}

		@Override
		public void visit(String fieldName, Double value,
				IFieldStructure fldStruct, boolean isDefault) {
			visitField(fieldName, "Double", fldStruct, "" + value);
		}

		@Override
		public void visit(String fieldName, Float value,
				IFieldStructure fldStruct, boolean isDefault) {
			visitField(fieldName, "Float", fldStruct, "" + value);
		}

		@Override
		public void visit(String fieldName, IMessage message, IFieldStructure fldType, boolean isDefault) {
			System.out.println("message " + fieldName);
		}

		@Override
		public void visit(String fieldName, Integer value,
				IFieldStructure fldStruct, boolean isDefault) {
			visitField(fieldName, "Integer", fldStruct, "" + value);
		}

		@Override
		public void visit(String fieldName, Long value,
				IFieldStructure fldStruct, boolean isDefault) {
			visitField(fieldName, "Long", fldStruct, "" + value);
		}

		@Override
		public void visit(String fieldName, Short value,
				IFieldStructure fldStruct, boolean isDefault) {
			visitField(fieldName, "Short", fldStruct, "" + value);
		}

		@Override
		public void visit(String fieldName, String value,
				IFieldStructure fldStruct, boolean isDefault) {
			visitField(fieldName, "String", fldStruct, "" + value);
		}

	}

	private String[] extractAlias(Map<String, IAttributeStructure> values) {
	    List<String> result = new ArrayList<String>();
	    result.addAll(values.keySet());
	    
	    Collections.sort(result);
	    
	    return result.toArray(new String[0]);
	}
}
