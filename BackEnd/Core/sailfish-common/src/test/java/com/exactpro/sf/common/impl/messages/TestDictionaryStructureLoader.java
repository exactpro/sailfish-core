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

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.exactpro.sf.common.impl.messages.xml.configuration.JavaType;
import com.exactpro.sf.common.messages.structures.IAttributeStructure;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.messages.structures.loaders.IDictionaryStructureLoader;
import com.exactpro.sf.common.messages.structures.loaders.XmlDictionaryStructureLoader;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.util.EPSTestCase;

public class TestDictionaryStructureLoader extends EPSTestCase {
	
    @Rule
    public final ExpectedException exception = ExpectedException.none();
    private final String path = getBaseDir() + File.separator + "src" + File.separator + "test" + File.separator + "resources" + File.separator;

    @Test
	public void testCreateXmlDictionary() throws Exception {

		IDictionaryStructureLoader loader = new XmlDictionaryStructureLoader();

        String dictionaryFile = path + "native.xml";
		
		IDictionaryStructure dictionary;
		
		try (InputStream in = new FileInputStream(dictionaryFile)) {
			dictionary = loader.load(in);
    	}

		System.out.println(dictionary.getNamespace());
		Assert.assertEquals("native1", dictionary.getNamespace());

        Assert.assertEquals(8, dictionary.getFields().size());
        Assert.assertEquals(3, dictionary.getMessages().size());
		
		System.out.println("Fields:");

        for(IFieldStructure fieldStructure : dictionary.getFields().values()) {
			System.out.println("\t" + fieldStructure.getName());
		}

		System.out.println("Messages:");

        for(IMessageStructure msgStruct : dictionary.getMessages().values()) {
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

        String dictionaryFile = path + "native.xml";

        IDictionaryStructure dictionary;

        try (InputStream in = new FileInputStream(dictionaryFile)) {
            dictionary = loader.load(in);
        }

        System.out.println(dictionary.getNamespace());
        Assert.assertEquals(dictionary.getNamespace(), "native1");

        Assert.assertEquals(JavaType.JAVA_LANG_INTEGER, dictionary.getMessages().get("MessageHeader").getFields().get("enum").getJavaType());
        Assert.assertEquals(JavaType.JAVA_LANG_INTEGER, dictionary.getMessages().get("MessageHeader").getFields().get("MessageLength").getJavaType());
        Assert.assertEquals(JavaType.JAVA_LANG_INTEGER, dictionary.getFields().get("enum").getJavaType());
        Assert.assertEquals(true, dictionary.getFields().get("enum").isEnum());
        Assert.assertEquals(2, dictionary.getFields().get("enum").getValues().size());
        
    }
    
    @Test
    public void testDuplicatedValuesInField() throws Exception {
        
        IDictionaryStructureLoader loader = new XmlDictionaryStructureLoader();

        String dictionaryFile = path + "sameValuesName.xml";
        
        exception.expect(EPSCommonException.class);
        exception.expectMessage("Duplicated values at test, attribute name is normal");
        try (InputStream in = new FileInputStream(dictionaryFile)) {
            loader.load(in);
        }
        
    }

    @Test
    public void testMessageFieldWithoutType() throws Exception {
        IDictionaryStructureLoader loader = new XmlDictionaryStructureLoader();

        String dictionaryFile = path + "noTypeInMessageField.xml";

        try (InputStream in = new FileInputStream(dictionaryFile)) {
            loader.load(in);
            Assert.fail("Dictionary was loaded");
        } catch (EPSCommonException ex) {
            Assert.assertNotNull("No wrapped exception", ex.getCause());

            String expectedException = "Field [FieldWithoutType] in message [Message] has neither a type nor a reference";
            Assert.assertEquals("Unexpected exception", expectedException, ex.getCause().getMessage());
        }
    }

    @Test
    public void testMessageSelfReference() throws Exception {
        exception.expect(EPSCommonException.class);
        exception.expectMessage("Recursion at message id: 'MessageId' has been detected!");

        try (InputStream stream = new FileInputStream(path + "message-self-reference.xml")) {
            new XmlDictionaryStructureLoader().load(stream);
        }
    }

    @Test
    public void testMessageReferenceToField() throws Exception {
        exception.expect(EPSCommonException.class);
        exception.expectMessage("Message 'Message' has field 'Field' as a reference");

        try (InputStream stream = new FileInputStream(path + "message-reference-to-field.xml")) {
            new XmlDictionaryStructureLoader().load(stream);
        }
    }

    @Test
    public void testMessageCircularReferenceInInheritance() throws Exception {
        exception.expect(EPSCommonException.class);
        exception.expectMessage("Recursion at message id: 'Message1Id' has been detected");

        try (InputStream stream = new FileInputStream(path + "message-circular-reference-in-inheritance.xml")) {
            new XmlDictionaryStructureLoader().load(stream);
        }
    }

    @Test
    public void testMessageCircularReferenceThroughInheritance() throws Exception {
        exception.expect(EPSCommonException.class);
        exception.expectMessage("Message 'Message2', problem with content");

        try (InputStream stream = new FileInputStream(path + "message-circular-reference-through-inheritance.xml")) {
            new XmlDictionaryStructureLoader().load(stream);
        }
    }

    @Test
    public void testMessageInheritance() throws Exception {
        try (InputStream stream = new FileInputStream(path + "message-inheritance.xml")) {
            Map<String, IMessageStructure> messages = new XmlDictionaryStructureLoader().load(stream).getMessages();

            Assert.assertEquals(newHashSet("Parent", "ChildA", "ChildB"), messages.keySet());

            // Parent

            IMessageStructure message = messages.get("Parent");
            Map<String, IAttributeStructure> attributes = message.getAttributes();
            Map<String, IFieldStructure> fields = message.getFields();

            Assert.assertEquals(singleton("AttributeA"), attributes.keySet());
            Assert.assertEquals(singleton("FieldA"), fields.keySet());

            IAttributeStructure attributeA = attributes.get("AttributeA");

            Assert.assertEquals("AttributeA", attributeA.getName());
            Assert.assertEquals("ValueA", attributeA.getValue());

            IFieldStructure fieldA = fields.get("FieldA");

            Assert.assertEquals("FieldA", fieldA.getName());
            Assert.assertEquals("String", fieldA.getReferenceName());

            // Child A <- Parent

            message = messages.get("ChildA");
            attributes = message.getAttributes();
            fields = message.getFields();

            Assert.assertEquals(newHashSet("AttributeA", "AttributeB"), attributes.keySet());
            Assert.assertEquals(asList("FieldA", "FieldB"), newArrayList(fields.keySet()));

            attributeA = attributes.get("AttributeA");
            IAttributeStructure attributeB = attributes.get("AttributeB");

            Assert.assertEquals("AttributeA", attributeA.getName());
            Assert.assertEquals("ValueA", attributeA.getCastValue());
            Assert.assertEquals("AttributeB", attributeB.getName());
            Assert.assertEquals("ValueB", attributeB.getCastValue());

            fieldA = fields.get("FieldA");
            IFieldStructure fieldB = fields.get("FieldB");

            Assert.assertEquals("FieldA", fieldA.getName());
            Assert.assertEquals("String", fieldA.getReferenceName());
            Assert.assertEquals("FieldB", fieldB.getName());
            Assert.assertEquals("Integer", fieldB.getReferenceName());

            // Child B <- Child A <- Parent

            message = messages.get("ChildB");
            attributes = message.getAttributes();
            fields = message.getFields();

            Assert.assertEquals(newHashSet("AttributeA", "AttributeB", "AttributeC"), attributes.keySet());
            Assert.assertEquals(asList("FieldA", "FieldB", "FieldC"), newArrayList(fields.keySet()));

            attributeA = attributes.get("AttributeA");
            attributeB = attributes.get("AttributeB");
            IAttributeStructure attributeC = attributes.get("AttributeC");

            Assert.assertEquals("AttributeA", attributeA.getName());
            Assert.assertEquals(1L, (long)attributeA.getCastValue());
            Assert.assertEquals("AttributeB", attributeB.getName());
            Assert.assertEquals("ValueB", attributeB.getCastValue());
            Assert.assertEquals("AttributeC", attributeC.getName());
            Assert.assertEquals("ValueC", attributeC.getCastValue());

            fieldA = fields.get("FieldA");
            fieldB = fields.get("FieldB");
            IFieldStructure fieldC = fields.get("FieldC");

            Assert.assertEquals("FieldA", fieldA.getName());
            Assert.assertEquals("Integer", fieldA.getReferenceName());
            Assert.assertEquals("FieldB", fieldB.getName());
            Assert.assertEquals("Integer", fieldB.getReferenceName());
            Assert.assertEquals("FieldC", fieldC.getName());
            Assert.assertEquals("String", fieldC.getReferenceName());
        }
    }
}