/*
 * Copyright 2009-2024 Exactpro (Exactpro Systems Limited)
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
 */

package com.exactpro.sf.common.messages;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.junit.Test;

import com.exactpro.sf.common.impl.messages.DefaultMessageFactory;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.messages.structures.loaders.XmlDictionaryStructureLoader;

import javax.annotation.Nullable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestMessageStructureReader {
    
    final Path BASE_DIR = Paths.get((System.getProperty("basedir") == null) ? "." : System.getProperty("basedir")).toAbsolutePath().normalize();
    private static final String DICTIONARY_PATH = "src" + File.separator + "test" + File.separator + "resources" + File.separator + "testMessages.xml";

    @Test
    public void testTraverse() throws IOException {
        IDictionaryStructure dictionary = loadDictionary();

        IMessageStructure structure = dictionary.getMessages().get("TestMessage");
        IMessage message = DefaultMessageFactory.getFactory().createMessage(structure.getName(), dictionary.getNamespace());
        
        message.addField("Boolean", "true");
        message.addField("Byte", 1);
        message.addField("Short", 2);
        message.addField("Integer", "3");
        message.addField("Long", 4);
        message.addField("Float", 5.1);
        message.addField("Double", 6.2);
        message.addField("BigDecimal", "7");
        message.addField("Character", 8);
        message.addField("String", 9);
        message.addField("LocalDate", "2016-08-18");
        message.addField("LocalTime", "01:02:03.123456789");
        message.addField("LocalDateTime", "2016-08-18T01:02:03.123456789");
        message.addField("NullValue", null);
        message.addField("Collection", Arrays.asList("1", "2", "3", "4"));
        message.addField("CollectionNullValue", Arrays.asList("5", null, null, "8"));

        TestVisitor visitor = new TestVisitor();
        MessageStructureReader.READER.traverse(visitor, structure, message, MessageStructureReaderHandlerImpl.instance());

        assertFiledClassName(structure, visitor.getMap());
    }

    @Test
    public void testDefaultValueNotPutToMessage() throws IOException {
        IDictionaryStructure dictionary = loadDictionary();
        IMessageStructure messageStructure = dictionary.getMessages().get("MessageWithDefaultValues");

        int defaultValues = messageStructure.getFields().values().stream()
                .mapToInt(field -> field.getDefaultValue() == null ? 0 : 1).sum();
        assertTrue("'" + messageStructure.getName() + "' hasn't got default fields", defaultValues > 0);

        IMessage message = DefaultMessageFactory.getFactory().createMessage(messageStructure.getName(), dictionary.getNamespace());
        TestVisitor visitor = new TestVisitor();
        MessageStructureReader.READER.traverse(visitor, messageStructure, message, MessageStructureReaderHandlerImpl.instance());

        assertEquals("Result hasn't default fields", defaultValues, visitor.getMap().size());
        assertEquals("Message has unexpected fields", 0, message.getFieldCount());
        assertFiledClassName(messageStructure, visitor.getMap());
    }

    private void assertFiledClassName(IMessageStructure structure, Map<String, Object> map) {
        structure.getFields().forEach((fieldName, fldStructure) -> {
            String expectedType = fldStructure.getJavaType().value();
            if (fldStructure.isCollection()) {
                ((Collection<?>) map.get(fieldName)).forEach(element ->
                        assertClassName(fieldName, expectedType, element));
            } else {
                assertClassName(fieldName, expectedType, map.get(fieldName));
            }
        });
    }

    private void assertClassName(String fieldName, String expectedClass, @Nullable Object value) {
        if (value != null) {
            assertEquals("Incorrect type of " + fieldName + " field", expectedClass, value.getClass().getName());
        }
    }

    private IDictionaryStructure loadDictionary() throws IOException {
        try (InputStream fileInputStream = Files.newInputStream(new File(BASE_DIR.toString(), DICTIONARY_PATH).toPath())) {
            return new XmlDictionaryStructureLoader().load(fileInputStream);
        }
    }
}