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
package com.exactpro.sf.common.impl.messages;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.exactpro.sf.common.impl.messages.xml.configuration.JavaType;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.messages.structures.loaders.IDictionaryStructureLoader;
import com.exactpro.sf.common.messages.structures.loaders.XmlDictionaryStructureLoader;
import com.exactpro.sf.common.messages.structures.loaders.XsdDictionaryStructureLoader;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.util.EPSTestCase;

public class TestDictionaryStructureLoader extends EPSTestCase {
	
    @Rule
    public final ExpectedException exception = ExpectedException.none();
    
	@Test
	public void testCreateXsdDictionary() throws Exception {

		IDictionaryStructureLoader loader = new XsdDictionaryStructureLoader();
		
		String dictionaryFile = getBaseDir() + File.separator + "src"
				+ File.separator + "test" + File.separator + "resources" + File.separator + "example.xsd";

		IDictionaryStructure dictionary;
		
		try (InputStream in = new FileInputStream(dictionaryFile)) {
			dictionary = loader.load(in);
    	}

		System.out.println(dictionary.getNamespace());
		Assert.assertEquals(dictionary.getNamespace(), "Example");

		Assert.assertEquals(dictionary.getFieldStructures().size(), 2);
		Assert.assertEquals(dictionary.getMessageStructures().size(), 6);
		
		List<String> fields = new ArrayList<>();
		fields.add("ReceivedSequenceNumberType");
		fields.add("MessageType");
		
		List<String> messages = new ArrayList<>();
		messages.add("BroadcastMessageHeaderType");
		messages.add("BroadcastHeaderType");
		messages.add("Alphas_Betas");
		messages.add("Alphas_BetasType");
		messages.add("LoginRequest");
		messages.add("Alphas_Betas1");

        List<String> attributes = new ArrayList<>();
        attributes.add("DictionaryAttr");
		
		System.out.println("Fields:");

		for (IFieldStructure fieldStructure : dictionary.getFieldStructures()) {
			System.out.println("\t" + fieldStructure.getName());
			Assert.assertTrue(fields.contains(fieldStructure.getName()));
		}

		System.out.println("Messages:");

		for (IMessageStructure msgStruct : dictionary.getMessageStructures()) {
			System.out.println("\t" + msgStruct.getName());
			Assert.assertTrue(messages.contains(msgStruct.getName()));
		}

        System.out.println("Attributes:");
        for (String attrName : dictionary.getAttributes().keySet()) {
            System.out.println("\t" + attrName);
            Assert.assertTrue(attributes.contains(attrName));
        }
		
		try (InputStream in = new FileInputStream(dictionaryFile)) {
            Assert.assertEquals(dictionary.getNamespace(), loader.extractNamespace(in));
        }
	}
	
	@Test
	public void testCreateXmlDictionary() throws Exception {

		IDictionaryStructureLoader loader = new XmlDictionaryStructureLoader();
		
		String dictionaryFile = getBaseDir() + File.separator + "src"
				+ File.separator + "test" + File.separator + "resources" + File.separator + "native.xml";
		
		IDictionaryStructure dictionary;
		
		try (InputStream in = new FileInputStream(dictionaryFile)) {
			dictionary = loader.load(in);
    	}

		System.out.println(dictionary.getNamespace());
		Assert.assertEquals("native1", dictionary.getNamespace());

		Assert.assertEquals(8, dictionary.getFieldStructures().size());
		Assert.assertEquals(3, dictionary.getMessageStructures().size());
		
		System.out.println("Fields:");

		for (IFieldStructure fieldStructure : dictionary.getFieldStructures()) {
			System.out.println("\t" + fieldStructure.getName());
		}

		System.out.println("Messages:");

		for (IMessageStructure msgStruct : dictionary.getMessageStructures()) {
			System.out.println("\t" + msgStruct.getName());
		}

        List<String> attributes = new ArrayList<>();
        attributes.add("DictionaryAttr");

        System.out.println("Attributes:");
        for (String attrName : dictionary.getAttributes().keySet()) {
            System.out.println("\t" + attrName);
            Assert.assertTrue(attributes.contains(attrName));
        }
		
		try (InputStream in = new FileInputStream(dictionaryFile)) {
            Assert.assertEquals(dictionary.getNamespace(), loader.extractNamespace(in));
        }
	}
	
    @Test
    public void testDisabledTypeOverriding() throws Exception {

        IDictionaryStructureLoader loader = new XmlDictionaryStructureLoader();

        String dictionaryFile = getBaseDir() + File.separator + "src" + File.separator + "test" + File.separator + "resources" + File.separator
                + "native.xml";

        IDictionaryStructure dictionary;

        try (InputStream in = new FileInputStream(dictionaryFile)) {
            dictionary = loader.load(in);
        }

        System.out.println(dictionary.getNamespace());
        Assert.assertEquals(dictionary.getNamespace(), "native1");

        Assert.assertEquals(JavaType.JAVA_LANG_INTEGER, dictionary.getMessageStructure("MessageHeader").getField("enum").getJavaType());
        Assert.assertEquals(JavaType.JAVA_LANG_INTEGER, dictionary.getMessageStructure("MessageHeader").getField("MessageLength").getJavaType());
        Assert.assertEquals(JavaType.JAVA_LANG_INTEGER, dictionary.getFieldStructure("enum").getJavaType());
        Assert.assertEquals(true, dictionary.getFieldStructure("enum").isEnum());
        Assert.assertEquals(2, dictionary.getFieldStructure("enum").getValues().size());
        
    }
    
    @Test
    public void testDuplicatedValuesInField() throws Exception {
        
        IDictionaryStructureLoader loader = new XmlDictionaryStructureLoader();
        
        String dictionaryFile = getBaseDir() + File.separator + "src" + File.separator + "test" + File.separator + "resources" + File.separator
                + "sameValuesName.xml";
        
        exception.expect(EPSCommonException.class);
        exception.expectMessage("Duplicated values at test, attribute name is normal");
        try (InputStream in = new FileInputStream(dictionaryFile)) {
            loader.load(in);
        }
        
    }

    @Test
    public void testMessageFieldWithoutType() throws Exception {
        IDictionaryStructureLoader loader = new XmlDictionaryStructureLoader();

        String dictionaryFile = getBaseDir() + File.separator + "src" + File.separator + "test" + File.separator + "resources" + File.separator
                + "noTypeInMessageField.xml";

        try (InputStream in = new FileInputStream(dictionaryFile)) {
            loader.load(in);
            Assert.fail("Dictionary was loaded");
        } catch (EPSCommonException ex) {
            Assert.assertNotNull("No wrapped exception", ex.getCause());

            String expectedException = "Field [FieldWithoutType] in message [Message] has neither a type nor a reference";
            Assert.assertEquals("Unexpected exception", expectedException, ex.getCause().getMessage());
        }
    }
}