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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.FileInputStream;

import org.junit.Test;

public class ExcelMatrixReaderTest {

    @Test
    public void testExcelMatrixReaderStringBoolean() {
        try (ExcelMatrixReader e = new ExcelMatrixReader("src/test/resources/aml/iomatrix/XLSwithstaticdata.xls", false)) {

        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void testExcelMatrixReaderStringBooleanXlsx() {
        try (ExcelMatrixReader e = new ExcelMatrixReader("src/test/resources/aml/iomatrix/XLSXwithstaticdata.xlsx", true)) {

        } catch (Exception ex) {
            ex.printStackTrace();
            fail(ex.getMessage());

        }
    }

    @Test
    public void testExcelMatrixReaderInputStreamBoolean() {
        try (ExcelMatrixReader e = new ExcelMatrixReader(
                new FileInputStream("src/test/resources/aml/iomatrix/XLSwithstaticdata.xls"), false)) {

        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void testExcelMatrixReaderInputStreamBooleanXlsx() {
        try (ExcelMatrixReader e = new ExcelMatrixReader(
                new FileInputStream("src/test/resources/aml/iomatrix/XLSXwithstaticdata.xlsx"), true)) {

        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void testExcelMatrixReaderStringBooleanInt() {
        try (ExcelMatrixReader e = new ExcelMatrixReader("src/test/resources/aml/iomatrix/XLSwithstaticdata.xls", false, 0)) {

        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void testExcelMatrixReaderStringBooleanIntXlsx() {
        try (ExcelMatrixReader e = new ExcelMatrixReader("src/test/resources/aml/iomatrix/XLSXwithstaticdata.xlsx", true, 0)) {

        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void testExcelMatrixReaderInputStreamBooleanInt() {
        try (ExcelMatrixReader e = new ExcelMatrixReader(
                new FileInputStream("src/test/resources/aml/iomatrix/XLSwithstaticdata.xls"), false, 0)) {

        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void testExcelMatrixReaderInputStreamBooleanIntXlsx() {
        try (ExcelMatrixReader e = new ExcelMatrixReader(
                new FileInputStream("src/test/resources/aml/iomatrix/XLSXwithstaticdata.xlsx"), true, 0)) {

        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    private String readSheet(ExcelMatrixReader reader) {
        StringBuilder sb1 = new StringBuilder();
        while (reader.hasNext()) {
            for (String sc : reader.read()) {
                sb1.append(sc);
            }
        }
        return sb1.toString();
    }
    @Test
    public void testReadFormulaWithLinkedSheet() throws Exception {
        // Test xls format
        try (ExcelMatrixReader xls1 = new ExcelMatrixReader(new FileInputStream("src/test/resources/aml/iomatrix/XLSwithstaticdata.xls"), false, 1);
                ExcelMatrixReader xls2 = new ExcelMatrixReader(new FileInputStream("src/test/resources/aml/iomatrix/XLSwithformuladata.xls"), false,
                        1)) {
            // Reading with static data
            String xls1Content = readSheet(xls1);

            // reading with formula data
            String xls2Content = readSheet(xls2);

            assertEquals("Formula wasn't read", xls1Content, xls2Content);


            // Test xlsx format
            ExcelMatrixReader xlsx1 = new ExcelMatrixReader(new FileInputStream("src/test/resources/aml/iomatrix/XLSXwithstaticdata.xlsx"), true, 1);
            ExcelMatrixReader xlsx2 = new ExcelMatrixReader(new FileInputStream("src/test/resources/aml/iomatrix/XLSXwithformuladata.xlsx"), true, 1);

            // Reading with static data
            String xlsx1Content = readSheet(xlsx1);

            // reading with formula data
            String xlsx2Content = readSheet(xlsx2);
            assertEquals("Formula wasn't read", xlsx1Content, xlsx2Content);
        }
    }

    @Test
    public void testReadingAndClosingXLS() throws Exception {
        // Test xls format
        try (ExcelMatrixReader xls1 = new ExcelMatrixReader("src/test/resources/aml/iomatrix/XLSwithstaticdata.xls", false);
                ExcelMatrixReader xls2 = new ExcelMatrixReader("src/test/resources/aml/iomatrix/XLSwithformuladata.xls", false)) {

            // Test reading
            StringBuilder sb1 = new StringBuilder();
            while (xls1.hasNext()) {
                for (SimpleCell sc : xls1.readCells()) {
                    sb1.append(sc.getValue());
                    if (sc.getCellStyle().getFillBackgroundColor() == 64) {
                        fail("Reading must be colored");
                    }
                }
            }
            if (sb1.length() == 0) {
                fail("File wasn't read");
            }

            // Test reading with formula
            String xls2Content = readSheet(xls2);

            assertEquals("Formula wasn't read", sb1.toString(), xls2Content);
        }
    }

    @Test
    public void testReadingAndClosingXLSX() throws Exception {
        // Test xlsx format
        try (ExcelMatrixReader xlsx1 = new ExcelMatrixReader("src/test/resources/aml/iomatrix/XLSXwithstaticdata.xlsx", true);
                ExcelMatrixReader xlsx2 = new ExcelMatrixReader("src/test/resources/aml/iomatrix/XLSXwithformuladata.xlsx", true)) {

            // Test reading
            StringBuilder sb1 = new StringBuilder();
            while (xlsx1.hasNext()) {
                for (SimpleCell sc : xlsx1.readCells()) {
                    sb1.append(sc.getValue());
                    // FIXME
                    // TODO Current version apache poi does not read cellStyles
                    // for *.xlsx files.
                    /*
                     * System.out.print(sc.getCellStyle().
                     * getFillForegroundColorColor()); if
                     * (sc.getCellStyle().getFillBackgroundColor() == 64){ fail(
                     * "Reading must be colored"); }
                     */
                }
            }
            if (sb1.length() == 0) {
                fail("File wasn't read");
            }

            // Test reading with formula
            String xlsx2Content = readSheet(xlsx2);
            assertEquals("Formula wasn't read", sb1.toString(), xlsx2Content);
        }
    }
}
