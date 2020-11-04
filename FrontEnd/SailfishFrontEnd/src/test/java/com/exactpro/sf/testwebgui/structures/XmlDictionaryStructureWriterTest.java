/*******************************************************************************
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

import com.exactpro.sf.common.impl.messages.xml.configuration.JavaType;
import com.exactpro.sf.common.messages.structures.IAttributeStructure;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static com.google.common.collect.Sets.newHashSet;

public class XmlDictionaryStructureWriterTest {

    private static final Path BASE_DIR = Paths.get((System.getProperty("basedir") == null) ? "." : System.getProperty("basedir")).toAbsolutePath().normalize();
    private static final String FILE_PATH = BASE_DIR + File.separator + "src" + File.separator + "test" + File.separator + "resources" + File.separator + "dictionary_attributes.xml";

    private static final String FIRST_ATTRIBUTE_NAME = "String";
    private static final String SECOND_ATTRIBUTE_NAME = "Integer";
    private static final String THIRD_ATTRIBUTE_NAME = "Boolean";

    @Test
    public void testDictionaryAttributesWriting() throws IOException {

        File dictionary = new File(FILE_PATH);

        ModifiableDictionaryStructure dictionaryStructure = getDictionaryStructure(dictionary);

        ModifiableDictionaryStructure dictionaryStructureAfterWriting;
        File tempFile = null;
        try {

            tempFile = File.createTempFile("dictionary", ".xml");

            XmlDictionaryStructureWriter.write(dictionaryStructure, tempFile);

            dictionaryStructureAfterWriting = getDictionaryStructure(tempFile);
        } finally {

            if (tempFile != null) {
                tempFile.delete();
            }
        }
        Map<String, IAttributeStructure> attributes = dictionaryStructureAfterWriting.getAttributes();

        Assert.assertEquals(newHashSet(FIRST_ATTRIBUTE_NAME, SECOND_ATTRIBUTE_NAME, THIRD_ATTRIBUTE_NAME), attributes.keySet());

        Assert.assertEquals(attributes.get(FIRST_ATTRIBUTE_NAME).getType(), JavaType.JAVA_LANG_STRING);
        Assert.assertEquals(attributes.get(FIRST_ATTRIBUTE_NAME).getValue(), "AttributeValue");

        Assert.assertEquals(attributes.get(SECOND_ATTRIBUTE_NAME).getType(), JavaType.JAVA_LANG_INTEGER);
        Assert.assertEquals(Integer.valueOf(attributes.get(SECOND_ATTRIBUTE_NAME).getValue()), Integer.valueOf(5112020));

        Assert.assertEquals(attributes.get(THIRD_ATTRIBUTE_NAME).getType(), JavaType.JAVA_LANG_BOOLEAN);
        Assert.assertEquals(Boolean.valueOf(attributes.get(THIRD_ATTRIBUTE_NAME).getValue()), true);
    }

    private ModifiableDictionaryStructure getDictionaryStructure(File dictionary) throws IOException {
        ModifiableXmlDictionaryStructureLoader loader = new ModifiableXmlDictionaryStructureLoader();
        try (InputStream inputStream = new FileInputStream(dictionary)) {
            return loader.load(inputStream);
        }
    }

}