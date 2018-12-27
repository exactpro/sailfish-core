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
package com.exactpro.sf.aml.iomatrix;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.exactpro.sf.aml.AMLException;

public class AdvancedMatrixReaderTest {
    private File csv = new File("src/test/resources/aml/iomatrix/TestDefineHeader.csv");
    private final String PATH = "src/test/resources/aml/iomatrix/";
    private final String XLSX_MATRIX = "Execl_2007_2013.xlsx";
    private final String XLS_MATRIX = "Execl_97_2003.xls";
    private final String CSV_MATRIX = "Csv.csv";


    @Test
    public void testCompareReaders() throws IOException, AMLException {
        AdvancedMatrixReader xlsxReader = new AdvancedMatrixReader(new File(PATH + XLSX_MATRIX));
        Assert.assertEquals(MatrixFileTypes.XLSX, xlsxReader.getFileType());
        AdvancedMatrixReader xlsReader = new AdvancedMatrixReader(new File(PATH + XLS_MATRIX));
        Assert.assertEquals(MatrixFileTypes.XLS, xlsReader.getFileType());
        AdvancedMatrixReader csvReader = new AdvancedMatrixReader(new File(PATH + CSV_MATRIX));
        Assert.assertEquals(MatrixFileTypes.CSV, csvReader.getFileType());

        Map<String, AdvancedMatrixReader> mapReaders = new HashMap<>();
        mapReaders.put("xlsxReader", xlsxReader);
        mapReaders.put("xlsReader", xlsReader);
        mapReaders.put("csvReader", csvReader);

        for (Entry<String, AdvancedMatrixReader> element : mapReaders.entrySet()) {
            AdvancedMatrixReader reader = element.getValue();
            String name = element.getKey();
            List<Map<String, SimpleCell>> lines = new ArrayList<>();

            while(reader.hasNext()) {
                lines.add(reader.readCells());
            }

            Assert.assertEquals(name, 3, lines.size());
            Assert.assertEquals(name, 0, lines.get(0).size());
            Assert.assertEquals(name, 0, lines.get(1).size());

            Map<String, SimpleCell> line = lines.get(2);

            Assert.assertEquals(name, 3, line.size());
            Assert.assertTrue(name, Arrays.asList("H2", "H4", "H6").containsAll(line.keySet()));
            Assert.assertEquals(name, "R3C2", line.get("H2").getValue());
            Assert.assertEquals(name, "R3C4", line.get("H4").getValue());
            Assert.assertEquals(name, "R3C6", line.get("H6").getValue());
        }
    }

    @Test
    //@Ignore
    public void testDefineHeaderCsv() throws Exception {
        checkMap(csv);
    }

    private void checkMap(File file) throws Exception {
        try (AdvancedMatrixReader reader = new AdvancedMatrixReader(file);) {

            Assert.assertEquals(asList("#1", "#2", "#3", "#4", "#action", "6", "7", "8", "9", "#10"),
                                reader.getHeader());
            Assert.assertEquals(asMap("20","20", "#new field", "#new field", "40", "40", "#action", "DefineHeader"),
                                reader.readCells());
            Assert.assertEquals(asList("#1", "#2", "#3", "#4", "#action", "20", "", "", "40", "#10", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "#new field"),
                                reader.getHeader());
            Assert.assertEquals(asMap("#1", "1", "#2", "2", "#new field", "new value", "#4", "4", "#3","3"),
                                reader.readCells());
            Assert.assertEquals(asMap("#1", "1", "#10", "300", "250", "250", "#2", "2", "150", "150", "#additional field", "#additional field", "200", "200", "#action", "DefineHeader", "100", "100", "#4", "4", "#3", "3"),
                                reader.readCells());
            Assert.assertEquals(asList("#1", "#2", "#3", "#4", "#action", "100", "150", "200", "250", "#10", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "#additional field", "#new field"),
                                reader.getHeader());
            Assert.assertEquals(asMap("#additional field", "value", "#new field", "old value", "#action", "test case end"),
                                reader.readCells());

            boolean error = false;
            try {
                reader.readCells();
            } catch (AMLException e) {
            	error = true;
                Assert.assertEquals("Invalid matrix structure. Detected duplicated fields at K6, Z6,  positions. Field name is #new field", e.getMessage());
            }
            Assert.assertTrue("DefineHeader command added already contains \"#new field\" field.", error);
        }
    }

    @Test
    @Ignore
    public void someTest() throws Exception{
        try (AdvancedMatrixReader reader = new AdvancedMatrixReader(new File("/home/sergey.smirnov/coding/TMP/validTests1AFTERSAVE.csv"))) {
            while (reader.hasNext()) {
                for (Map.Entry<String, SimpleCell> cell:reader.readCells().entrySet()) {
                    if (!cell.getValue().getValue().isEmpty()) {
                        System.out.print(cell.getKey() + " = " + cell.getValue() + ", ");
                    }
                }
                System.out.println();
            }
        }
    }

    private Map<String, SimpleCell> asMap(String... values) {
        if(Objects.requireNonNull(values, "values is null").length % 2 > 0) {
            throw new IllegalArgumentException("values has odd number of elements");
        }

        Map<String, SimpleCell> map = new HashMap<>();

        for(int i = 0; i < values.length; i += 2) {
            map.put(values[i], new SimpleCell(values[i + 1]));
        }

        return map;
    }

    private List<SimpleCell> asList(String...values) {
        Objects.requireNonNull(values, "values is null");

        List<SimpleCell> list = new ArrayList<>();

        for(String value : values) {
            list.add(new SimpleCell(value));
        }

        return list;
    }
}
