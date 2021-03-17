/*******************************************************************************
 * Copyright 2009-2021 Exactpro (Exactpro Systems Limited)
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

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Map;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.exactpro.sf.common.impl.messages.json.configuration.JsonYamlDictionary;
import com.exactpro.sf.common.impl.messages.xml.configuration.JavaType;
import com.exactpro.sf.common.messages.structures.IAttributeStructure;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.messages.structures.StructureType;
import com.exactpro.sf.common.messages.structures.loaders.IDictionaryStructureLoader;
import com.exactpro.sf.common.messages.structures.loaders.JsonYamlDictionaryStructureLoader;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.util.EPSTestCase;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

public class TestJsonYamlDictionaryStructureLoader extends EPSTestCase {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    // Using BaseMatcher for checking causes message
    private static class CauseMatcher extends BaseMatcher<Throwable> {

        private Class<? extends Throwable> aClass;
        private String message;

        public CauseMatcher(Class<? extends Throwable> aClass, String message) {
            this.aClass = aClass;
            this.message = message;
        }

        @Override
        public boolean matches(Object item) {
            return aClass.isAssignableFrom(item.getClass()) && ((Throwable) item).getMessage().equals(message);
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("Message in exceptions cause should be \"" + message + "\"");
        }
    }

    private IDictionaryStructure loadDictionaryFrom(String resourceFileName) throws IOException {
        IDictionaryStructureLoader loader = new JsonYamlDictionaryStructureLoader();

        String dictionaryJsonFile = Paths.get(getBaseDir(), "src", "test", "resources", resourceFileName).toString();

        IDictionaryStructure dictionary;

        try (InputStream in = new FileInputStream(dictionaryJsonFile)) {
            dictionary = loader.load(in);
        }

        try (InputStream in = new FileInputStream(dictionaryJsonFile)) {
            Assert.assertEquals(dictionary.getNamespace(), loader.extractNamespace(in));
        }

        return dictionary;
    }

    private boolean testNativeDictionary(IDictionaryStructure dictionary) {
        Assert.assertEquals(dictionary.getNamespace(), "native1");
        Assert.assertEquals(dictionary.getAttributes().size(), 1);
        Assert.assertEquals(dictionary.getAttributes().get("DictionaryAttr").getValue(), "test_attr");
        Assert.assertEquals(dictionary.getFields().size(), 2);
        Assert.assertEquals(dictionary.getFields().get("MessageType").getJavaType().value(), "java.lang.Short");
        Assert.assertEquals(dictionary.getMessages().size(), 3);
        Assert.assertTrue(dictionary.getMessages().get("Heartbeat").getFields().get("MessageHeader1").getStructureType() == StructureType.COMPLEX);
        Assert.assertEquals(dictionary.getMessages().get("MissedMessageRequest").getAttributes().size(), 1);
        Assert.assertEquals(dictionary.getMessages().get("MissedMessageRequest").getFields().size(), 3);
        return true;
    }

    /**
     * Test for json and yaml dictionaries.
     * @throws Exception
     */
    @Test
    public void testCreateJsonDictionary() throws Exception {
        testNativeDictionary(loadDictionaryFrom("native.json"));
    }

    @Test
    public void testCreateYamlDictionary() throws Exception {
        testNativeDictionary(loadDictionaryFrom("native.yaml"));
    }

    @Test
    public void testCreateNoNameDictionary() throws Exception {
        exception.expect(EPSCommonException.class);
        exception.expectMessage("Wrong input format");
        exception.expectCause(new CauseMatcher(MismatchedInputException.class, "Missing required creator property 'name' (index 0)\n at [Source: (FileInputStream); line: 13, column: 1]"));

        loadDictionaryFrom("noNameDictionary.json");
    }

    @Test
    public void testRecursiveReferenceForFields() throws Exception {
        exception.expect(EPSCommonException.class);
        exception.expectMessage("Recursive reference for fields with ids: [A,B,C]");

        loadDictionaryFrom("recursiveFieldReference.json");
    }

    @Test
    public void testDisabledTypeOverriding() throws Exception {
        IDictionaryStructure dictionary = loadDictionaryFrom("disabledTypeOverriding.json");

        Assert.assertEquals(dictionary.getNamespace(), "DisabledTypeOverriding");

        Assert.assertEquals(JavaType.JAVA_LANG_INTEGER, dictionary.getFields().get("A").getJavaType());
        Assert.assertEquals(JavaType.JAVA_LANG_INTEGER, dictionary.getFields().get("B").getJavaType());
        Assert.assertTrue(dictionary.getFields().get("B").isEnum());
        Assert.assertEquals(2, dictionary.getFields().get("B").getValues().size());

        Assert.assertEquals(JavaType.JAVA_LANG_INTEGER, dictionary.getMessages().get("MessageTest").getFields().get("RefByA").getJavaType());
        Assert.assertEquals(JavaType.JAVA_LANG_INTEGER, dictionary.getMessages().get("MessageTest").getFields().get("RefByB").getJavaType());
        Assert.assertEquals(JavaType.JAVA_LANG_INTEGER, dictionary.getMessages().get("MessageTest").getFields().get("RefByBWithType").getJavaType());
    }

    @Test
    public void testDuplicatedValuesNameInField() throws Exception {
        exception.expect(EPSCommonException.class);
        exception.expectMessage("Wrong input format");
        exception.expectCause(new CauseMatcher(JsonMappingException.class, "Duplicate field 'normal'\n"
                + " at [Source: (FileInputStream); line: 10, column: 9] (through reference chain: com.exactpro.sf.common.impl.messages.json.configuration.JsonYamlDictionary[\"fields\"])"));

        loadDictionaryFrom("sameValuesName.json");

    }

    @Test
    public void testDuplicatedMessagesName() throws Exception {
        exception.expect(EPSCommonException.class);
        exception.expectMessage("Wrong input format");
        exception.expectCause(new CauseMatcher(JsonMappingException.class, "Duplicate field 'message_name'\n"
                + " at [Source: (FileInputStream); line: 11, column: 5] (through reference chain: com.exactpro.sf.common.impl.messages.json.configuration.JsonYamlDictionary[\"messages\"])"));

        loadDictionaryFrom("sameNames.json");
    }

    @Test
    public void testMessageFieldWithoutType() throws Exception {
        exception.expect(EPSCommonException.class);
        exception.expectCause(new CauseMatcher(EPSCommonException.class, "Field [FieldWithoutType] in message [Message] has neither a type nor a reference"));

        loadDictionaryFrom("noTypeInMessageField.json");
    }

    @Test
    public void testRecursion() throws Exception {

        IDictionaryStructure dictionaryStructure = loadDictionaryFrom("recursion.json");
        IFieldStructure selfMessage = dictionaryStructure.getMessages().get("self").getFields().get("s_name");
        Assert.assertTrue(selfMessage instanceof IMessageStructure);
    }

    @Test
    public void testJsonSerialize() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();


        String file = Paths.get(getBaseDir(), "src", "test", "resources", "native.json").toString();
        JsonYamlDictionary dictionary = new JsonYamlDictionaryStructureLoader().getDictionary(new FileInputStream(file));
        String jsonStr = objectMapper.writerFor(JsonYamlDictionary.class).withDefaultPrettyPrinter().writeValueAsString(dictionary);


        System.out.println(testNativeDictionary(new JsonYamlDictionaryStructureLoader().convert(new JsonYamlDictionaryStructureLoader().getDictionary(new ByteArrayInputStream(jsonStr.getBytes())))));
        System.out.println(jsonStr);
    }

    @Test
    public void testYamlSerialize() throws Exception {
        // Disabled USE_NATIVE_OBJECT_ID because YAMLFactory write reference like '*N1' on default
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.USE_NATIVE_OBJECT_ID).disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID));


        String file = Paths.get(getBaseDir(), "src", "test", "resources", "native.json").toString();
        JsonYamlDictionary dictionary = new JsonYamlDictionaryStructureLoader().getDictionary(new FileInputStream(file));
        String jsonStr = objectMapper.writerFor(JsonYamlDictionary.class).withDefaultPrettyPrinter().writeValueAsString(dictionary);


        System.out.println(testNativeDictionary(new JsonYamlDictionaryStructureLoader().convert(new JsonYamlDictionaryStructureLoader().getDictionary(new ByteArrayInputStream(jsonStr.getBytes())))));
        System.out.println(jsonStr);
    }

    @Test
    public void testMessageSelfReference() throws Exception {
        exception.expect(EPSCommonException.class);
        exception.expectMessage("Recursion at message id: 'Message' has been detected!");

        loadDictionaryFrom("message-self-reference.json");
    }

    @Test
    public void testMessageReferenceToField() throws Exception {
        exception.expect(EPSCommonException.class);
        exception.expectMessage("Message 'Message' has field 'Field' as a reference");

        loadDictionaryFrom("message-reference-to-field.json");
    }

    @Test
    public void testMessageCircularReferenceInInheritance() throws Exception {
        exception.expect(EPSCommonException.class);
        exception.expectMessage("Recursion at message id: 'Message1' has been detected");

        loadDictionaryFrom("message-circular-reference-in-inheritance.json");
    }

    @Test
    public void testMessageCircularReferenceThroughInheritance() throws Exception {
        IDictionaryStructure dictionaryStructure = loadDictionaryFrom("message-circular-reference-through-inheritance.json");
        Assert.assertTrue(dictionaryStructure.getMessages().get("Message2").getFields().get("Field1") instanceof IMessageStructure);
    }

    @Test
    public void testMessageInheritance() throws Exception {
        Map<String, IMessageStructure> messages = loadDictionaryFrom("message-inheritance.json").getMessages();

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

    private void testEmbeddedDictionary(IDictionaryStructure dictionary) {
        Assert.assertEquals(dictionary.getNamespace(), "embeddedMessage");
        Assert.assertEquals(dictionary.getAttributes().size(), 1);
        Assert.assertEquals(dictionary.getAttributes().get("DictionaryAttr").getValue(), "test_attr");
        Assert.assertEquals(dictionary.getFields().size(), 2);
        Assert.assertEquals(dictionary.getFields().get("MessageType").getJavaType().value(), "java.lang.Short");
        Assert.assertEquals(dictionary.getMessages().size(), 2);
        IFieldStructure embeddedMessage = dictionary.getMessages().get("Heartbeat").getFields().get("MessageHeader");
        Assert.assertTrue(embeddedMessage instanceof IMessageStructure);
        Assert.assertTrue(embeddedMessage.isComplex());
        Assert.assertEquals(2, embeddedMessage.getFields().size());
        Assert.assertArrayEquals(
                new String[] { "StartOfMessage", "MessageType1" },
                embeddedMessage.getFields().keySet().toArray(new String[0])
        );
        Assert.assertEquals(dictionary.getMessages().get("MissedMessageRequest").getAttributes().size(), 1);
        Assert.assertEquals(dictionary.getMessages().get("MissedMessageRequest").getFields().size(), 3);
    }

    @Test
    public void testEmbeddedMessageJson() throws IOException {
        IDictionaryStructure dictionary = loadDictionaryFrom("embeddedMessage.json");
        testEmbeddedDictionary(dictionary);
    }

    @Test
    public void testEmbeddedMessageYaml() throws IOException {
        IDictionaryStructure dictionary = loadDictionaryFrom("embeddedMessage.yaml");
        testEmbeddedDictionary(dictionary);
    }

    @Test
    public void testMessageInFields() throws Exception {
        exception.expect(EPSCommonException.class);
        exception.expectMessage("It is impossible to keep message 'Short' in fields");

        loadDictionaryFrom("messageInFields.json");
    }

    @Test
    public void testMessageCiclicReferences() throws Exception {
        IDictionaryStructure dictionary = loadDictionaryFrom("cyclicReferences.json");
        Assert.assertTrue(dictionary.getMessages().get("A").getFields().get("self") instanceof IMessageStructure);
        Assert.assertTrue(dictionary.getMessages().get("InnerMessage").getFields().get("circlied") instanceof IMessageStructure);
    }
}
