/******************************************************************************
 * Copyright 2009-2020 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.testwebgui.structures;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import com.exactpro.sf.common.messages.structures.IAttributeStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;

public class TestModifiableJsonYamlDictionaryStructureLoader {

    private static final Path BASE_DIR = Paths.get((System.getProperty("basedir") == null) ? "." : System.getProperty("basedir")).toAbsolutePath().normalize();
    private static final String MESSAGE_INHERITANCE_FILE_PATH = BASE_DIR + File.separator + "src" + File.separator + "test" + File.separator + "resources" + File.separator + "message-inheritance.json";

    @Test
    public void testMessageInheritance() throws Exception {
        try (InputStream jsonInputStream = new FileInputStream(MESSAGE_INHERITANCE_FILE_PATH)) {

            ModifiableJsonYamlDictionaryStructureLoader jsonLoader = new ModifiableJsonYamlDictionaryStructureLoader();
            ModifiableDictionaryStructure dictionaryStructureFromJson = jsonLoader.load(jsonInputStream);

            Map<String, IMessageStructure> messages = dictionaryStructureFromJson.getMessages();

            Assert.assertEquals(newHashSet("Parent", "ChildA", "ChildB"), messages.keySet());

            // Parent

            ModifiableMessageStructure message = (ModifiableMessageStructure)messages.get("Parent");

            Assert.assertTrue(message.isMessage());
            Assert.assertFalse(message.isSubMessage());

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

            message = (ModifiableMessageStructure)messages.get("ChildA");

            Assert.assertTrue(message.isMessage());
            Assert.assertFalse(message.isSubMessage());

            attributes = message.getAttributes();
            fields = message.getFields();

            Assert.assertEquals(newHashSet("AttributeA", "AttributeB"), attributes.keySet());
            Assert.assertEquals(singleton("AttributeB"), message.getImplAttributes()
                    .stream()
                    .map(ModifiableAttributeStructure::getName)
                    .collect(Collectors.toSet()));

            Assert.assertEquals(asList("FieldA", "FieldB"), newArrayList(fields.keySet()));
            Assert.assertEquals(singleton("FieldB"), message.getImplFields().keySet());

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

            message = (ModifiableMessageStructure)messages.get("ChildB");

            Assert.assertTrue(message.isMessage());
            Assert.assertFalse(message.isSubMessage());

            attributes = message.getAttributes();
            fields = message.getFields();

            Assert.assertEquals(newHashSet("AttributeA", "AttributeB", "AttributeC"), attributes.keySet());
            Assert.assertEquals(newHashSet("AttributeA", "AttributeC"), message.getImplAttributes()
                    .stream()
                    .map(ModifiableAttributeStructure::getName)
                    .collect(Collectors.toSet()));

            Assert.assertEquals(asList("FieldA", "FieldB", "FieldC", "FieldD"), newArrayList(fields.keySet()));
            Assert.assertEquals(asList("FieldC", "FieldA", "FieldD"), message.getImplFields().keyList());

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
            ModifiableMessageStructure fieldD = (ModifiableMessageStructure)fields.get("FieldD");

            Assert.assertEquals("FieldA", fieldA.getName());
            Assert.assertEquals("Integer", fieldA.getReferenceName());
            Assert.assertEquals("FieldB", fieldB.getName());
            Assert.assertEquals("Integer", fieldB.getReferenceName());
            Assert.assertEquals("FieldC", fieldC.getName());
            Assert.assertEquals("String", fieldC.getReferenceName());
            Assert.assertEquals("FieldD", fieldD.getName());
            Assert.assertEquals("Parent", fieldD.getReferenceName());
            Assert.assertTrue(fieldD.isComplex());
            Assert.assertFalse(fieldD.isMessage());
            Assert.assertTrue(fieldD.isSubMessage());
            Assert.assertEquals(singleton("FieldA"), fieldD.getFields().keySet());
            Assert.assertTrue(fieldD.getImplFields().isEmpty());
        }
    }
}