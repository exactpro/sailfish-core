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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.exactpro.sf.common.messages.structures.DictionaryComparator;

public class TestJsonYamlDictionaryStructureWriter {

    private static final Path BASE_DIR = Paths.get((System.getProperty("basedir") == null) ? "." : System.getProperty("basedir")).toAbsolutePath().normalize();
    private static final String TEST_FILES_DIRECTORY_PATH = BASE_DIR + File.separator + "src" + File.separator + "test" + File.separator + "resources";

    private static final File MESSAGE_INHERITANCE_FILE = new File(TEST_FILES_DIRECTORY_PATH, "message-inheritance.json");
    private static final File WRITER_TEST_DICTIONARY_FILE = new File(TEST_FILES_DIRECTORY_PATH, "writer-test-dictionary.json");
    private static final File MESSAGE_EMBEDDED_FILE = new File(TEST_FILES_DIRECTORY_PATH, "message-embedded.json");

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testWriterWithoutDifference() throws IOException {

        ModifiableDictionaryStructure modifiableDictionaryStructure = getDictionaryStructure(MESSAGE_INHERITANCE_FILE);

        File tempFile = folder.newFile("jsonDictionary.json");

        JsonYamlDictionaryStructureWriter.write(modifiableDictionaryStructure, tempFile, false);

        ModifiableDictionaryStructure dictionaryStructureFromJson = getDictionaryStructure(tempFile);

        DifferenceListener listener = new DifferenceListener();
        DictionaryComparator comparator = new DictionaryComparator();

        comparator.compare(listener,
                modifiableDictionaryStructure, dictionaryStructureFromJson, true, false, true);

        Assert.assertTrue(listener.getDifferences().isEmpty());
    }

    @Test
    public void testWriterWithDifference() throws IOException {

        ModifiableDictionaryStructure modifiableDictionaryStructure = getDictionaryStructure(WRITER_TEST_DICTIONARY_FILE);

        File tempFile = folder.newFile("jsonDictionary.json");

        JsonYamlDictionaryStructureWriter.write(modifiableDictionaryStructure, tempFile, false);

        ModifiableDictionaryStructure dictionaryStructureFromJson = getDictionaryStructure(tempFile);

        modifiableDictionaryStructure.setNamespace("SetDifferentNamespace");
        modifiableDictionaryStructure.setDescription("SetDifferentDescription");

        DifferenceListener listener = new DifferenceListener();
        DictionaryComparator comparator = new DictionaryComparator();

        comparator.compare(listener,
                modifiableDictionaryStructure, dictionaryStructureFromJson, true, false, true);

        Assert.assertEquals(listener.getDifferences().size(), 2);
        Assert.assertEquals(listener.getDifferences().get(0), "[String : SetDifferentNamespace] [String : TestAML] - Namespace");
        Assert.assertEquals(listener.getDifferences().get(1), "[String : SetDifferentDescription] [null] - Description");

    }

    @Test
    public void testEmbeddedMessage() throws IOException {

        ModifiableDictionaryStructure dictionaryStructureBeforeWriting = getDictionaryStructure(MESSAGE_EMBEDDED_FILE);

        File tempFile = folder.newFile("jsonDictionary.json");

        JsonYamlDictionaryStructureWriter.write(dictionaryStructureBeforeWriting, tempFile, false);

        ModifiableDictionaryStructure dictionaryStructureAfterWriting = getDictionaryStructure(tempFile);

        DifferenceListener listener = new DifferenceListener();
        DictionaryComparator comparator = new DictionaryComparator();

        comparator.compare(listener,
                dictionaryStructureBeforeWriting, dictionaryStructureAfterWriting, true, false, true);

        Assert.assertTrue(listener.getDifferences().isEmpty());
    }

    private ModifiableDictionaryStructure getDictionaryStructure(File dictionary) throws IOException {
        try (InputStream inputStream = new FileInputStream(dictionary)) {
            ModifiableJsonYamlDictionaryStructureLoader loader = new ModifiableJsonYamlDictionaryStructureLoader();
            return loader.load(inputStream);
        }
    }
}