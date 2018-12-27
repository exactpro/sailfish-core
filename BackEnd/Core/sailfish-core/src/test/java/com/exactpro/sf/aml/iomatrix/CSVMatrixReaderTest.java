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

import static org.junit.Assert.*;

import java.io.FileInputStream;
import java.util.Arrays;

import org.junit.Test;

import com.csvreader.CsvReader;

import junit.framework.Assert;

public class CSVMatrixReaderTest {

    @Test
    public void testCSVMatrixReaderString() {
        try (CSVMatrixReader e = new CSVMatrixReader("src/test/resources/aml/iomatrix/testcsv.csv")) {

        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void testCSVMatrixReaderStringChar() {
        try (CSVMatrixReader e = new CSVMatrixReader("src/test/resources/aml/iomatrix/testcsv.csv", ',')) {

        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void testCSVMatrixReaderInputStream() {
        try (CSVMatrixReader e = new CSVMatrixReader(
                new FileInputStream("src/test/resources/aml/iomatrix/testcsv.csv"))) {

        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void testCSVMatrixReaderInputStreamChar() {
        try (CSVMatrixReader e = new CSVMatrixReader(new FileInputStream("src/test/resources/aml/iomatrix/testcsv.csv"),
                ',')) {

        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void testClose() {
        try (CSVMatrixReader e = new CSVMatrixReader("src/test/resources/aml/iomatrix/testcsv.csv")) {
            e.close();
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void testReadCells() {
        try (CSVMatrixReader e = new CSVMatrixReader("src/test/resources/aml/iomatrix/testcsv.csv")) {
            StringBuilder sb = new StringBuilder();
            while (e.hasNext()) {
                sb.append(Arrays.toString(e.readCells()));
            }
            if (sb.length() == 0) {
                fail("File not readed");
            }
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void testRead() {
        try (CSVMatrixReader e = new CSVMatrixReader("src/test/resources/aml/iomatrix/testcsv.csv")) {
            StringBuilder sb = new StringBuilder();
            while (e.hasNext()) {
                for (SimpleCell sc : e.readCells()) {
                    sb.append(sc.getValue());
                }
            }
            if (sb.length() == 0) {
                fail("File not readed");
            }
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }
    
    @Test
    public void get() {
        try {
            CsvReader e = new CsvReader("src/test/resources/aml/iomatrix/testcsv.csv");
            e.readRecord();
            String val = e.get(0);
            Assert.assertEquals("1494574053", val);
            String val2 = e.get(100500);
            Assert.assertEquals("", val2);
            String val3 = e.get("SOME_COLUMN");
            Assert.assertEquals("", val3);
            
            System.out.println("finished");
            
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void testReadFromNoMarkSupportInputStream() {
        try (CSVMatrixReader reader = new CSVMatrixReader(
                new FileInputStream("src/test/resources/aml/iomatrix/testcsv.csv"))) {
            String[] expectedCells = new String[]{"1494574053", "1000199004", "975277106", "-900841273", "-1205771794",
                    "-436980907","-544962948","-1060190917","1082171287","284431925"};
            String[] actualCells = reader.read();
            assertArrayEquals("Wrong reading line", expectedCells, actualCells);
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }
}
