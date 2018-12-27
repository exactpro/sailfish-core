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

import static org.junit.Assert.fail;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.hssf.util.HSSFColor.HSSFColorPredefined;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Test;

public class ExcelMatrixWriterTest {

    private String tmpDir = System.getProperty("java.io.tmpdir");
    private String fileName = tmpDir + File.separator + "test.xls";
    private String fileNameX = tmpDir + File.separator + "test.xlsx";
    private File tmpFile = new File(fileName);
    private File tmpFileX = new File(fileNameX);

    @Test
    public void testExcelMatrixWriterStringBoolean() {
        try (ExcelMatrixWriter e = new ExcelMatrixWriter(fileName, false)) {

        } catch (IOException ex) {
            fail(ex.getMessage());
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void testExcelMatrixWriterStringBooleanXlsx() {
        try (ExcelMatrixWriter e = new ExcelMatrixWriter(fileNameX, true)) {

        } catch (IOException ex) {
            fail(ex.getMessage());
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void testExcelMatrixWriterOutputStreamBoolean() {
        try (ExcelMatrixWriter e = new ExcelMatrixWriter(new FileOutputStream(tmpFile), false)) {

        } catch (IOException ex) {
            fail(ex.getMessage());
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void testExcelMatrixWriterOutputStreamBooleanXlsx() {
        try (ExcelMatrixWriter e = new ExcelMatrixWriter(new FileOutputStream(tmpFileX), true)) {

        } catch (IOException ex) {
            fail(ex.getMessage());
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void testExcelMatrixWriterStringBooleanString() {
        try (ExcelMatrixWriter e = new ExcelMatrixWriter(fileName, false, "someName")) {

        } catch (IOException ex) {
            fail(ex.getMessage());
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void testExcelMatrixWriterStringBooleanStringXlsx() {
        try (ExcelMatrixWriter e = new ExcelMatrixWriter(fileNameX, true, "someName")) {

        } catch (IOException ex) {
            fail(ex.getMessage());
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void testExcelMatrixWriterOutputStreamBooleanString() {
        try (ExcelMatrixWriter e = new ExcelMatrixWriter(new FileOutputStream(tmpFile), false, "someName")) {

        } catch (IOException ex) {
            fail(ex.getMessage());
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void testExcelMatrixWriterOutputStreamBooleanStringXlsx() {
        try (ExcelMatrixWriter e = new ExcelMatrixWriter(new FileOutputStream(tmpFileX), true, "someName")) {

        } catch (IOException ex) {
            fail(ex.getMessage());
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void testWriteCells() {
        HSSFWorkbook fakeWorkbook = new HSSFWorkbook();
        CellStyle style = fakeWorkbook.createCellStyle();
        HSSFColor myColor = HSSFColor.HSSFColorPredefined.RED.getColor();

        try (ExcelMatrixWriter e = new ExcelMatrixWriter(new FileOutputStream(tmpFile), false, "someName")) {
            SimpleCell[] cells = new SimpleCell[10];
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    byte[] forString = new byte[32];
                    new Random().nextBytes(forString);
                    style.setFillForegroundColor(myColor.getIndex());
                    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                    cells[j] = new SimpleCell(new String(forString), style);
                }
                e.writeCells(cells);
            }
        } catch (IOException ex) {
            fail(ex.getMessage());
        } catch (Exception ex) {
            fail(ex.getMessage());
        } finally {
            try {
                fakeWorkbook.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        try {
            short bg = WorkbookFactory.create(tmpFile).getSheet("someName").getRow(0).getCell(0).getCellStyle()
                    .getFillForegroundColor();
            if (bg != HSSFColorPredefined.RED.getIndex()) {
                fail("wrong color");
            }
        } catch (IOException e1) {
            fail(e1.getMessage());
        }
    }

    @Test
    public void testWriteCellsXlsx() {
        XSSFWorkbook fakeWorkbook = new XSSFWorkbook();
        CellStyle style = fakeWorkbook.createCellStyle();
        XSSFColor myColor = new XSSFColor(Color.RED, fakeWorkbook.getStylesSource().getIndexedColors());

        try (ExcelMatrixWriter e = new ExcelMatrixWriter(new FileOutputStream(tmpFileX), true, "someName")) {
            SimpleCell[] cells = new SimpleCell[10];
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    byte[] forString = new byte[32];
                    new Random().nextBytes(forString);
                    style.setFillForegroundColor(myColor.getIndexed());
                    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

                    cells[j] = new SimpleCell(new String(forString), style);
                }
                e.writeCells(cells);
            }
        } catch (IOException ex) {
            fail(ex.getMessage());
        } catch (Exception ex) {
            fail(ex.getMessage());
        } finally {
            try {
                fakeWorkbook.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        // FIXME
        // TODO Current version apache poi does not read cellStyles for *.xlsx
        // files.
        /* System.out.print(sc.getCellStyle().getFillForegroundColorColor()); */
        /*
         * try { short bg =
         * WorkbookFactory.create(tmpFileX).getSheet("someName").getRow(0).
         * getCell(0).getCellStyle() .getFillForegroundColor(); if (bg != new
         * XSSFColor(Color.RED).getIndexed()) { fail("wrong color"); } } catch
         * (InvalidFormatException e1) { fail(e1.getMessage()); } catch
         * (IOException e1) { fail(e1.getMessage()); }
         */
    }

    @Test
    public void testWrite() {
        try (ExcelMatrixWriter e = new ExcelMatrixWriter(new FileOutputStream(tmpFile), false, "someName")) {
            String[] cells = new String[10];
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    byte[] forString = new byte[32];
                    new Random().nextBytes(forString);
                    cells[j] = new String(forString);
                }
                e.write(cells);
            }
        } catch (IOException ex) {
            fail(ex.getMessage());
        } catch (Exception ex) {
            fail(ex.getMessage());
        }

        try {
            WorkbookFactory.create(tmpFile);
        } catch (IOException e1) {
            fail(e1.getMessage());
        }
    }

    @Test
    public void testWriteXlsx() {
        try (ExcelMatrixWriter e = new ExcelMatrixWriter(new FileOutputStream(tmpFileX), true, "someName")) {
            String[] cells = new String[10];
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    byte[] forString = new byte[32];
                    new Random().nextBytes(forString);
                    cells[j] = new String(forString);
                }
                e.write(cells);
            }
        } catch (IOException ex) {
            fail(ex.getMessage());
        } catch (Exception ex) {
            fail(ex.getMessage());
        }

        try {
            WorkbookFactory.create(tmpFileX);
        } catch (IOException e1) {
            fail(e1.getMessage());
        }
    }

    @Test
    public void testClose() {
        try (ExcelMatrixWriter e = new ExcelMatrixWriter(fileName, false)) {
            String[] cells = new String[10];
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    byte[] forString = new byte[32];
                    new Random().nextBytes(forString);
                    cells[j] = new String(forString);
                }
                e.write(cells);
            }
        } catch (IOException ex) {
            fail(ex.getMessage());
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void testCloseXlsx() {
        // If you close the writer and try to do it again, you get NPE (only for
        // xlsx)
        try {
            ExcelMatrixWriter e = new ExcelMatrixWriter(fileNameX, true);

            String[] cells = new String[10];
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    byte[] forString = new byte[32];
                    new Random().nextBytes(forString);
                    cells[j] = new String(forString);
                }
                e.write(cells);
            }

            e.close();
        } catch (IOException ex) {
            fail(ex.getMessage());
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void testFlush() {
        assert (true);
    }
}
