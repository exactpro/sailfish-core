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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.exactpro.sf.aml.AMLException;

public class AdvancedMatrixReaderTest {
    private final File csv = new File("src/test/resources/aml/iomatrix/TestDefineHeader.csv");
    private final String PATH = "src/test/resources/aml/iomatrix/";
    private final String XLSX_MATRIX = "Execl_2007_2013.xlsx";
    private final String XLS_MATRIX = "Execl_97_2003.xls";
    private final String CSV_MATRIX = "Csv.csv";
    private final String YAML_MATRIX = "SystemColumns.yaml";
    private final String ERROR_YAML_MATRIX = "ErrorArrayInBlock.yaml";
    private final String JSON_MATRIX = "JSON.json";
    private final String YAML_MATRIX_WITH_NULLS = "MatrixWithNulls.yaml";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

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
        try (AdvancedMatrixReader reader = new AdvancedMatrixReader(file)) {
            int numberLine = 2;
            Assert.assertEquals(asListWithNumberLine(el("#1",1), el("#2", 1), el("#3",1),
                    el("#4", 1), el("#action", 1), el("6", 1), el("7", 1),
                    el("8", 1), el("9", 1), el("#10",1)),
                                reader.getHeader());
            Assert.assertEquals(asMap(numberLine++,"20","20", "#new field", "#new field", "40", "40", "#action", "DefineHeader"),
                                reader.readCells());
            Assert.assertEquals(asListWithNumberLine(el("#1", 1), el("#2", 1), el("#3", 1),
                    el("#4", 1), el("#action", 1), el("20", 2), el("", 2),
                    el("", 2), el("40", 2), el("#10", 1), el("", 2),
                    el("", 2), el("", 2), el("", 2), el("", 2),
                    el("", 2), el("", 2), el("", 2), el("", 2),
                    el("", 2), el("", 2), el("", 2), el("", 2),
                    el("", 2), el("", 2), el("#new field", 2)),
                                reader.getHeader());
            Assert.assertEquals(asMap(numberLine++,"#1", "1", "#2", "2", "#new field", "new value", "#4", "4", "#3","3"),
                                reader.readCells());
            Assert.assertEquals(asMap(numberLine++,"#1", "1", "#10", "300", "250", "250", "#2", "2", "150", "150", "#additional field", "#additional field", "200", "200", "#action", "DefineHeader", "100", "100", "#4", "4", "#3", "3"),
                                reader.readCells());
            Assert.assertEquals(asListWithNumberLine(el("#1",1), el("#2", 1), el("#3",1),
                    el("#4", 1), el("#action", 1), el("100", 4), el("150", 4),
                    el("200", 4), el("250", 4), el("#10", 1), el("", 4),
                    el("", 4), el("", 4), el("", 4), el("", 4),
                    el("", 4), el("", 4), el("", 4), el("", 4),
                    el("", 4), el("", 4), el("", 4), el("", 4),
                    el("", 4), el("#additional field", 4),  el("#new field", 2)),
                                reader.getHeader());
            Assert.assertEquals(asMap(numberLine++,"#additional field", "value", "#new field", "old value", "#action", "test case end"),
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
    public void testSystemColumnsYaml() throws Exception {
        try (AdvancedMatrixReader reader = new AdvancedMatrixReader(new File(PATH + YAML_MATRIX))) {

            Assert.assertEquals(asMapWithNumberLine(el("#action", "block start",17), el("#description", "Descr", 18),
                                                    el("#reference", "block", 19), el("field", "abc", 20)),
                    reader.readCells());
            Assert.assertEquals(asMapWithNumberLine(el("#reference", "implicit_ref2", 25), el("sub4", "send4", 26)),
                    reader.readCells());
            Assert.assertEquals(asMapWithNumberLine(el("#reference", "implicit_ref3", 27), el("sub99", "send99", 28)),
                    reader.readCells());
            Assert.assertEquals(asMapWithNumberLine(el("#reference", "implicit_ref4", 29), el("sub000", "send000", 30),
                                                    el("sub111", "send111", 31)),
                    reader.readCells());
            Assert.assertEquals(asMapWithNumberLine(el("#reference", "implicit_ref5", 27), el("sub00", "[implicit_ref4]", 29)),
                    reader.readCells());
            Assert.assertEquals(asMapWithNumberLine(el("#reference", "implicit_ref6", 23), el("#action", "send2",24),
                                                    el("sub3", "[implicit_ref2]", 25), el("sub88", "[implicit_ref3,implicit_ref5]", 27)),
                    reader.readCells());
            Assert.assertEquals(asMapWithNumberLine(el("#reference", "implicit_ref7", 32), el("sub5", "send5", 33)),
                    reader.readCells());
            Assert.assertEquals(asMapWithNumberLine(el("#reference", "sub", 21), el("#action", "send",22),
                                                    el("sub2", "[implicit_ref6]", 23),  el("sub5", "[implicit_ref7]", 32),
                                                    el("sub77", "[send771,send772]",34)),
                    reader.readCells());
            Assert.assertEquals(asMapWithNumberLine(el("#reference", "sub7", 37), el("sub7", "send7",38)),
                    reader.readCells());
            Assert.assertEquals(asMapWithNumberLine(el("#reference", "sub4", 39), el("field", "send4",40)),
                    reader.readCells());
            Assert.assertEquals(asMapWithNumberLine(el("#action", "Block end", 17)),
                    reader.readCells());

            Assert.assertEquals(asMapWithNumberLine(el("#action", "test case start", 41), el("#id", "test_id", 42)),
                    reader.readCells());
            Assert.assertEquals(asMapWithNumberLine(el("#action", "include block", 46), el("#reference", "call", 43),
                                                    el("#template", "block", 45),el("#id", "123", 44)),
                    reader.readCells());
            Assert.assertEquals(asMapWithNumberLine(el("#action", "Test case end", 41)),
                    reader.readCells());
        }
    }

    @Test
    public void testYamlMatrixWithNulls() throws Exception {
        try (AdvancedMatrixReader reader = new AdvancedMatrixReader(new File(PATH + YAML_MATRIX_WITH_NULLS))) {

            Assert.assertEquals(asMapWithNumberLine(el("#action", "test case start", 17)),
                    reader.readCells());
            Assert.assertEquals(asMapWithNumberLine(el("#reference", "BL1", 19),
                    el("#action", "send", 20),
                    el("Side", "BUY", 21)),
                    reader.readCells());
            Assert.assertEquals(asMapWithNumberLine(el("#reference", "BL2", 22)),
                    reader.readCells());
            Assert.assertEquals(asMapWithNumberLine(el("#action", "Test case end", 17)),
                    reader.readCells());
        }
    }

    @Test
    public void testNumberLineJson() throws Exception {
        try (AdvancedMatrixReader reader = new AdvancedMatrixReader(new File(PATH + JSON_MATRIX))) {

            Assert.assertEquals(asMapWithNumberLine(el("#action", "block start",3), el("#description", "Descr", 3),
                                                    el("#reference", "block", 3), el("field", "abc", 4)),
                    reader.readCells());
            Assert.assertEquals(asMapWithNumberLine(el("#reference", "implicit_ref2", 9), el("sub4", "send4", 10)),
                    reader.readCells());
            Assert.assertEquals(asMapWithNumberLine(el("#reference", "implicit_ref3", 12), el("sub99", "send99", 14)),
                    reader.readCells());
            Assert.assertEquals(asMapWithNumberLine(el("#reference", "implicit_ref4", 17), el("sub000", "send000", 18),
                                                    el("sub111", "send111", 19)),
                    reader.readCells());
            Assert.assertEquals(asMapWithNumberLine(el("#reference", "implicit_ref5", 12), el("sub00", "[implicit_ref4]", 17)),
                    reader.readCells());
            Assert.assertEquals(asMapWithNumberLine(el("#reference", "implicit_ref6", 7), el("#action", "send2",8),
                                                    el("sub3", "[implicit_ref2]", 9), el("sub88", "[implicit_ref3,implicit_ref5]", 12)),
                    reader.readCells());
            Assert.assertEquals(asMapWithNumberLine(el("#reference", "implicit_ref7", 24), el("sub5", "send5", 25)),
                    reader.readCells());
            Assert.assertEquals(asMapWithNumberLine(el("#reference", "sub", 5), el("#action", "send",6),
                                                    el("sub2", "[implicit_ref6]", 7),  el("sub5", "[implicit_ref7]", 24),
                                                    el("sub77", "[send771,send772]",27)),
                    reader.readCells());
            Assert.assertEquals(asMapWithNumberLine(el("#reference", "sub7", 32), el("sub7", "send7",33)),
                    reader.readCells());
            Assert.assertEquals(asMapWithNumberLine(el("#reference", "sub4", 35), el("field", "send4",36)),
                    reader.readCells());
            Assert.assertEquals(asMapWithNumberLine(el("#action", "Block end", 3)),
                    reader.readCells());
            Assert.assertEquals(asMapWithNumberLine(el("#action", "test case start", 41), el("#id", "test_id", 42)),
                    reader.readCells());
            Assert.assertEquals(asMapWithNumberLine(el("#action", "include block", 46), el("#reference", "call", 43),
                                                    el("#template", "block", 45), el("#id", "123", 44)),
                    reader.readCells());
            Assert.assertEquals(asMapWithNumberLine(el("#action", "Test case end", 41)),
                    reader.readCells());

        }
    }

    @Test
    public void testNegativErrorArrayInBlock() throws Exception {

        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Invalid value type array field found in block block start, number line 5");

        new AdvancedMatrixReader(new File(PATH + ERROR_YAML_MATRIX));
    }

    @Test
    @Ignore
    public void someTest() throws Exception{
        try (AdvancedMatrixReader reader = new AdvancedMatrixReader(new File("/home/sergey.smirnov/coding/TMP/validTests1AFTERSAVE.csv"))) {
            while (reader.hasNext()) {
                for (Entry<String, SimpleCell> cell:reader.readCells().entrySet()) {
                    if (!cell.getValue().getValue().isEmpty()) {
                        System.out.print(cell.getKey() + " = " + cell.getValue() + ", ");
                    }
                }
                System.out.println();
            }
        }
    }

    private List<SimpleCell> noNumberLine(List<SimpleCell> simpleCellList) {
        List<SimpleCell> newSimpleCellList = new ArrayList<>();
        for (SimpleCell simpleCell: simpleCellList) {
            newSimpleCellList.add(new SimpleCell(simpleCell.getValue(),0));
        }
        return newSimpleCellList;
    }

    private Map<String, SimpleCell> asMap(int numberLine, String... values) {
        if(Objects.requireNonNull(values, "values is null").length % 2 > 0) {
            throw new IllegalArgumentException("values has odd number of elements");
        }

        Map<String, SimpleCell> map = new HashMap<>();

        for(int i = 0; i < values.length; i += 2) {
            map.put(values[i], new SimpleCell(values[i + 1], numberLine));
        }

        return map;
    }

    private Map<String, SimpleCell> asMapWithNumberLine(ReadElement... values) {

        Map<String, SimpleCell> map = new HashMap<>();

        for(ReadElement readElement: values) {
            map.put(readElement.key, new SimpleCell(readElement.value, readElement.numberLine));
        }

        return map;
    }

    private List<SimpleCell> asList(String... values) {
        Objects.requireNonNull(values, "values is null");

        List<SimpleCell> list = new ArrayList<>();

        for(String value : values) {
            list.add(new SimpleCell(value));
        }

        return list;
    }

    private List<SimpleCell> asListWithNumberLine(ReadElement... values) {
        Objects.requireNonNull(values, "values is null");

        List<SimpleCell> list = new ArrayList<>();

        for(ReadElement readElement : values) {
            list.add(new SimpleCell(readElement.value, readElement.numberLine));
        }

        return list;
    }

    private ReadElement el(String value, int numberLine){
        return new ReadElement(value, numberLine);
    }

    private ReadElement el(String key, String value, int numberLine){
        return new ReadElement(key, value, numberLine);
    }

    private class ReadElement{
        final String key;
        final String value;
        final int numberLine;

        public ReadElement(String value, int numberLine){
            this("",value,numberLine);
        }

        public ReadElement(String key, String value, int numberLine){
            this.key = key;
            this.numberLine = numberLine;
            this.value = value;
        }
    }
}


