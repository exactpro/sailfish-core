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
package com.exactpro.sf.aml.writer;

import com.exactpro.sf.aml.generator.matrix.Column;
import com.exactpro.sf.aml.iomatrix.AdvancedMatrixReader;
import com.exactpro.sf.aml.iomatrix.IMatrixReader;
import com.exactpro.sf.aml.iomatrix.IMatrixWriter;
import com.exactpro.sf.aml.iomatrix.JSONMatrixWriter;
import com.exactpro.sf.aml.iomatrix.MatrixFileTypes;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.util.AbstractTest;
import com.google.common.collect.Sets;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JSONWriterTest extends AbstractTest {

    private static final File MAIN_DIR = new File(BASE_DIR, "src/test/resources/aml/writer");
    private static final File BAD_BLOCK_1_FILE = new File(MAIN_DIR, "jw_test_bad_block_1.csv");
    private static final File BAD_BLOCK_2_FILE = new File(MAIN_DIR, "jw_test_bad_block_2.csv");
    private static final File BAD_BLOCK_3_FILE = new File(MAIN_DIR, "jw_test_bad_block_3.csv");
    private static final File BIG_1_FILE = new File(MAIN_DIR, "jw_test_big_1.csv");
    private static final File BIG_2_FILE = new File(MAIN_DIR, "jw_test_big_2.csv");

    private static final File OUTPUT_FILE = new File(MAIN_DIR, "output.yaml");

    @Test
    public void testUnexpectedTypes() throws Exception {
        MatrixFileTypes[] badFileTypes = {
            MatrixFileTypes.CSV,
            MatrixFileTypes.XLS,
            MatrixFileTypes.XLSX,
            MatrixFileTypes.UNKNOWN
        };

        for(MatrixFileTypes badFileType: badFileTypes) {
            try {
                new JSONMatrixWriter(OUTPUT_FILE, badFileType).close();
                Assert.fail();
            } catch (EPSCommonException ignored) {}
        }
    }

    @Test
    public void testBadBlock1() throws Exception {
        Assert.assertTrue(
            testBadBlockNRunner(BAD_BLOCK_1_FILE, JSONMatrixWriter.EXCEPTION_CASE_OUT_OF_BLOCK)
        );
    }

    @Test
    public void testBadBlock2() throws Exception {
        Assert.assertTrue(
            testBadBlockNRunner(BAD_BLOCK_2_FILE, JSONMatrixWriter.EXCEPTION_UNCLOSED_BLOCK_FMT)
        );
    }

    @Test
    public void testBadBlock3() throws Exception {
        Assert.assertTrue(
            testBadBlockNRunner(BAD_BLOCK_3_FILE, JSONMatrixWriter.EXCEPTION_CASE_OUT_OF_BLOCK)
        );
    }

    public static boolean testBadBlockNRunner(File badFile, String containsMsg) throws Exception {
        try(
            IMatrixReader reader = AdvancedMatrixReader.getReader(badFile);
            IMatrixWriter writer = new JSONMatrixWriter(OUTPUT_FILE, MatrixFileTypes.YAML)
        ) {
            while(reader.hasNext()) {
                writer.write(reader.read());
            }
        } catch(EPSCommonException e) {
            return e.getMessage().contains(containsMsg);
        }
        return false;
    }

    @Test
    public void testBig1() throws Exception {
        compareConvertedMatrices(BIG_1_FILE);
    }

    @Test
    public void testBig2() throws Exception {
        compareConvertedMatrices(BIG_2_FILE);
    }

    public static void compareConvertedMatrices(File testFile) throws Exception {
        try(
            IMatrixReader dataReader = AdvancedMatrixReader.getReader(testFile);
            IMatrixWriter resultWriter = new JSONMatrixWriter(OUTPUT_FILE, MatrixFileTypes.YAML)
        ) {
            while(dataReader.hasNext()) {
                resultWriter.write(dataReader.read());
            }
        }
        try(
            IMatrixReader dataReader = AdvancedMatrixReader.getReader(testFile);
            IMatrixReader resultReader = AdvancedMatrixReader.getReader(OUTPUT_FILE)
        ) {
            List<Map<String, String>> dataLines = convertNextToMap(dataReader);
            List<Map<String, String>> resultLines = convertNextToMap(resultReader);
            Assert.assertEquals(dataLines.size(), resultLines.size());

            for(int i = 0; i < dataLines.size(); i++) {
                Map<String, String> dataLine = dataLines.get(i);
                Map<String, String> resultLine = resultLines.get(i);
                Map<String, String> diffMap = entrySetToMap(Sets.difference(
                    dataLine.entrySet(), resultLine.entrySet()
                ).immutableCopy());
                diffMap.remove(Column.Reference.getName());

                if(diffMap.isEmpty()) {
                    continue;
                }
                Assert.fail();
            }
        }
    }

    public static List<Map<String, String>> convertNextToMap(IMatrixReader reader) throws IOException {
        List<Map<String, String>> result = new ArrayList<>();
        String[] headers = reader.read();

        while(reader.hasNext()) {
            Map<String, String> map = new HashMap<>();
            String[] cols = reader.read();
            for(int i = 0; i < cols.length; i++) {
                String col = cols[i];
                if(!col.isEmpty()) {
                    map.put(headers[i], col.toLowerCase());
                }
            }
            if(!map.isEmpty()) {
                result.add(map);
            }
        }
        return result;
    }

    public static Map<String, String> entrySetToMap(Set<Map.Entry<String, String>> set) {
        Map<String, String> map = new HashMap<>();
        for(Map.Entry<String, String> e: set) {
            map.put(e.getKey(), e.getValue());
        }
        return map;
    }

    @After
    public void cleanUp() throws IOException {
        Files.deleteIfExists(OUTPUT_FILE.toPath());
    }

}
