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

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.NoSuchElementException;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelMatrixReader implements IMatrixReader {

    private final Workbook workbook;
    private final FormulaEvaluator formulaEvaluator;
    private final Sheet sheet;
    private final Iterator<Row> rowIterator;
    private final DataFormatter dataFormatter;
    private Row currentRow;
    private Cell currentCell;
    private boolean hasNext;
    private int sheetIndex = 0;
    private int lastRowIndex = -1;

    public ExcelMatrixReader(String fileName, boolean useXlsx) throws IOException {

        this(new FileInputStream(fileName), useXlsx, 0);
    }

    public ExcelMatrixReader(InputStream inputStream, boolean useXlsx) throws IOException {

        this(inputStream, useXlsx, 0);
    }

    public ExcelMatrixReader(String fileName, boolean useXlsx, int sheetNumber) throws IOException {

        this(new FileInputStream(fileName), useXlsx, 0);
    }

    public ExcelMatrixReader(InputStream inputStream, boolean useXlsx, int sheetNumber) throws IOException {

        this.sheetIndex = sheetNumber;
        this.workbook = useXlsx ? new XSSFWorkbook(inputStream)
                : new HSSFWorkbook(new BufferedInputStream(inputStream));
        inputStream.close();
        formulaEvaluator =  workbook.getCreationHelper().createFormulaEvaluator();
        sheet = workbook.getSheetAt(sheetIndex);
        rowIterator = sheet.iterator();
        this.dataFormatter = new DataFormatter(true);
        this.dataFormatter.setDefaultNumberFormat(NumberFormat.getNumberInstance(Locale.US));

        readRecord();
    }

    @Override
    public void close() throws Exception {
        workbook.close();
    }

    @Override
    public SimpleCell[] readCells() throws NoSuchElementException {

        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        if (++lastRowIndex < currentRow.getRowNum()) {
            return new SimpleCell[0];
        }

        List<SimpleCell> cells = new ArrayList<>(Math.abs(currentRow.getLastCellNum()));

        Iterator<Cell> cellIterator = currentRow.iterator();
        int index = 0;
        boolean allEmpty = true;

        while (cellIterator.hasNext()) {
            currentCell = cellIterator.next();
            while (index++ < currentCell.getColumnIndex()) {
                cells.add(new SimpleCell(""));
            }

            String value = this.dataFormatter.formatCellValue(currentCell,formulaEvaluator).trim();

            allEmpty &= StringUtils.isEmpty(value);

            cells.add(new SimpleCell(value, currentCell.getCellStyle()));
        }

        readRecord();

        if (allEmpty) {
            cells = Collections.emptyList();
        }

        // drop all values after last non-empty cell
        // otherwise a lot of empty (but styled) cells will be returned
        ListIterator<SimpleCell> it = cells.listIterator(cells.size());
        while (it.hasPrevious()) {
            SimpleCell cell = it.previous();
            if (cell == null || cell.getValue() == null || cell.getValue().trim().isEmpty()) {
                it.remove();
            } else {
                break;
            }
        }

        return cells.toArray(new SimpleCell[cells.size()]);
    }

    @Override
    public String[] read() throws NoSuchElementException {
        SimpleCell[] cells = readCells();
        String[] values = new String[cells.length];

        int counter = 0;
        for (SimpleCell sc : cells) {
            values[counter++] = sc.getValue();
        }

        return values;
    }

    @Override
    public boolean hasNext() {
        return hasNext;
    }

    private void readRecord() {
        hasNext = rowIterator.hasNext();
        if (hasNext) {
            currentRow = rowIterator.next();
        }
    }
}
