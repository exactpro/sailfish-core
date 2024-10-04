/******************************************************************************
 * Copyright 2009-2022 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.aml.iomatrix;

import com.exactpro.sf.util.AbstractTest;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ConvertMatrixTest extends AbstractTest {

    private static final String LETTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private static final File MAIN_DIR = new File(BASE_DIR, "src/test/resources/aml/iomatrix");
    private static final File TEST_FILE = new File(MAIN_DIR, "simple_matrix.csv");

    private final List<File> testFiles = new ArrayList<>();

    @Before
    public void createTestFiles() {
        testFiles.add(TEST_FILE);
        for(MatrixFileTypes type: MatrixFileTypes.values()) {
            if(type != MatrixFileTypes.UNKNOWN) {
                testFiles.add(new File(MAIN_DIR, randomFileName(type)));
            }
        }
    }

    @Test
    public void test() throws Exception {
        for(int i = 0; i < testFiles.size() - 1; i++) {
            File readerFile = testFiles.get(i);
            File writerFile = testFiles.get(i + 1);
            ConvertMatrix.convertMatrices(readerFile, writerFile);
            compareFiles(testFiles.get(i), testFiles.get(i + 1));
        }

        compareFiles(TEST_FILE, testFiles.get(testFiles.size() - 1));
    }

    private void compareFiles(File file0, File file1) throws Exception {
        List<List<String>> srcLines = readFile(file0);
        List<List<String>> destLines = readFile(file1);

        Assert.assertEquals(srcLines.size(), destLines.size());

        for(int i = 0; i < srcLines.size(); i++) {
            Assert.assertEquals(srcLines.get(i), destLines.get(i));
        }
    }

    @After
    public void deleteTestFiles() throws IOException {
        for(File file: testFiles) {
            if(!file.equals(TEST_FILE)) {
                Files.deleteIfExists(file.toPath());
            }
        }
        testFiles.clear();
    }

    private static List<List<String>> readFile(File file) throws Exception {
        List<List<String>> lines = new ArrayList<>();
        int headerSize = -1;
        try(IMatrixReader reader = AdvancedMatrixReader.getReader(file)) {
            while(reader.hasNext()) {
                List<String> cells = Arrays.stream(reader.read()).collect(Collectors.toList());
                if(headerSize < 0) {
                    headerSize = cells.size();
                } else {
                    while(cells.size() < headerSize) {
                        cells.add("");
                    }
                }
                Collections.sort(cells);
                lines.add(cells);
            }
        }
        return lines;
    }

    private static String randomFileName(MatrixFileTypes type) {
        String suffix = RandomStringUtils.random(16, LETTERS);
        return String.format("output-file-%s.%s", suffix, type.name().toLowerCase());
    }

}
