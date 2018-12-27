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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;


public class ExcelMatrixWriter implements IMatrixWriter {

    private final Workbook workbook;
    private final Sheet sheet;
    private OutputStream fileStream;
    private int row = 0;
    private int column = 0;
    
    private Map<CellStyle, CellStyle> stylesCashe;

    public ExcelMatrixWriter(String fileName, boolean useXlsx) throws IOException {

        this(new FileOutputStream(fileName), useXlsx, "Matrix");
    }

    public ExcelMatrixWriter(OutputStream outputStream, boolean useXlsx) throws IOException {

        this(outputStream, useXlsx, "Matrix");
    }

    public ExcelMatrixWriter(String fileName, boolean useXlsx, String sheetName) throws IOException {

        this(new FileOutputStream(fileName), useXlsx, sheetName);
    }

    public ExcelMatrixWriter(OutputStream outputStream, boolean useXlsx, String sheetName) throws IOException {

        fileStream = outputStream;

        this.workbook = useXlsx ? new XSSFWorkbook() : new HSSFWorkbook();

        sheet = workbook.createSheet(sheetName);
        
        stylesCashe = new HashMap<>();
    }

    @Override
    public void writeCells(SimpleCell[] cells) {

        column = 0;

        sheet.createRow(row);

        for (SimpleCell sc : cells) {
            
            sheet.getRow(row).createCell(column);

            Cell cell = sheet.getRow(row).getCell(column);

            cell.setCellValue(sc != null ? sc.getValue(): null);

            if (sc != null && sc.getCellStyle() != null) {
                CellStyle style = null;
                
                if (stylesCashe.containsKey(sc.getCellStyle())) {
                    style = stylesCashe.get(sc.getCellStyle());
                } else {
                    style = workbook.createCellStyle();
                    style.cloneStyleFrom(sc.getCellStyle());
                    stylesCashe.put(sc.getCellStyle(), style);
                }
                
	            cell.setCellStyle(style);
            }
            column++;
        }
        row++;
    }

    @Override
    public void write(String[] strings) {

        column = 0;

        sheet.createRow(row);

        for (String s : strings) {

            sheet.getRow(row).createCell(column);

            Cell cell = sheet.getRow(row).getCell(column);

            cell.setCellValue(s);
            column++;
        }
        row++;
    }

    @Override
    public void close() throws Exception {
        workbook.write(fileStream);
        // fileStream.close();
        workbook.close();
    }

    @Override
    public void flush() {

    }
}
