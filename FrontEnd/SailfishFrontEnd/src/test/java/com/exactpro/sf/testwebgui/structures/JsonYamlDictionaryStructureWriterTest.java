/*******************************************************************************
 * Copyright 2009-2019 Exactpro (Exactpro Systems Limited)
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

import com.exactpro.sf.common.messages.structures.DictionaryComparator;
import com.exactpro.sf.common.messages.structures.DictionaryComparator.DistinctionType;
import com.exactpro.sf.common.messages.structures.DictionaryComparator.DictionaryPath;
import com.exactpro.sf.common.messages.structures.DictionaryComparator.IDiffListener;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.loaders.JsonYamlDictionaryStructureLoader;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class JsonYamlDictionaryStructureWriterTest {

    private static final Path BASE_DIR = Paths.get((System.getProperty("basedir") == null) ? "." : System.getProperty("basedir")).toAbsolutePath().normalize();

    private static final String FILE_PATH = BASE_DIR + File.separator + "src" + File.separator + "test" + File.separator + "resources" + File.separator + "writer-test-dictionary.xml";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testWriterWithoutDifference() throws IOException {

        ModifiableDictionaryStructure modifiableDictionaryStructure;

        try (FileInputStream inputStream = new FileInputStream(FILE_PATH)) {

            modifiableDictionaryStructure = new ModifiableXmlDictionaryStructureLoader()
                    .load(inputStream);
        }

        File tempFile = folder.newFile("jsonDictionary.json");

        JsonYamlDictionaryStructureWriter.write(modifiableDictionaryStructure, tempFile, false);

        IDictionaryStructure dictionaryStructureFromJson;
        try (InputStream inputStream = new FileInputStream(tempFile)) {

            JsonYamlDictionaryStructureLoader loader = new JsonYamlDictionaryStructureLoader();
            dictionaryStructureFromJson = loader.load(inputStream);
        }

        Listener listener = new Listener();
        DictionaryComparator comparator = new DictionaryComparator();

        comparator.compare(listener,
                modifiableDictionaryStructure, dictionaryStructureFromJson, true, false, true);

        Assert.assertEquals(listener.getDifferences().isEmpty(), true);
    }

    @Test
    public void testWriterWithDifference() throws IOException {

        ModifiableDictionaryStructure modifiableDictionaryStructure;

        try (FileInputStream inputStream = new FileInputStream(FILE_PATH)) {
            modifiableDictionaryStructure = new ModifiableXmlDictionaryStructureLoader()
                    .load(inputStream);
        }

        File tempFile = folder.newFile("jsonDictionary.json");

        JsonYamlDictionaryStructureWriter.write(modifiableDictionaryStructure, tempFile, false);


        IDictionaryStructure dictionaryStructureFromJson;
        try (InputStream inputStream = new FileInputStream(tempFile)) {

            JsonYamlDictionaryStructureLoader loader = new JsonYamlDictionaryStructureLoader();
            dictionaryStructureFromJson = loader.load(inputStream);
        }

        modifiableDictionaryStructure.setNamespace("SetDifferentNamespace");
        modifiableDictionaryStructure.setDescription("SetDifferentDescription");

        Listener listener = new Listener();
        DictionaryComparator comparator = new DictionaryComparator();

        comparator.compare(listener,
                modifiableDictionaryStructure, dictionaryStructureFromJson, true, false, true);

        Assert.assertEquals(listener.getDifferences().size(), 2);
        Assert.assertEquals(listener.getDifferences().get(0), "[String : SetDifferentNamespace] [String : TestAML] - Namespace");
        Assert.assertEquals(listener.getDifferences().get(1), "[String : SetDifferentDescription] [null] - Description");

    }

    private class Listener implements IDiffListener {

        List<String> differences = new ArrayList<>();

        @Override
        public void differnce(DistinctionType distinctionType, Object first, Object second,
                              DictionaryPath dictionaryPath) {

            StringBuilder differenceStorage = new StringBuilder();

            differenceStorage.append('[');
            if (first != null) {
                differenceStorage.append(first.getClass().getSimpleName()).append(" : ");
            }
            differenceStorage.append(first).append(']')
                    .append(' ')
                    .append('[');
            if (second != null) {
                differenceStorage.append(second.getClass().getSimpleName()).append(" : ");
            }
            differenceStorage.append(second).append(']');
            if (distinctionType != null) {
                differenceStorage.append(" - ").append(distinctionType);
            }

            differences.add(differenceStorage.toString());
        }

        List<String> getDifferences() {
            return differences;
        }
    }

}