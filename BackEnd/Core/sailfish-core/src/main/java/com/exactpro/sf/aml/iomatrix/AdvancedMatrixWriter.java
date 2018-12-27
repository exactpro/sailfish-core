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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.aml.generator.matrix.Column;
import com.exactpro.sf.aml.generator.matrix.JavaStatement;

public class AdvancedMatrixWriter implements AutoCloseable {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final static String SYSTEM_FIELD_PREFIX = Column.getSystemPrefix();
    private final static String ACTION_FIELD = Column.Action.getName();
    private final static String DEFINE_HEADER = JavaStatement.DEFINE_HEADER.getValue();
    private final IMatrixWriter writer;
    private final List<SimpleCell> header;
    private final Map<String, Integer> convertedHeaders;

    private boolean isHeaderWrited = false;
    private int rowNumber = 1;

    public static IMatrixWriter getWriter(File matrixPath) throws IOException {
        MatrixFileTypes matrixType = MatrixFileTypes.detectFileType(matrixPath.getName());
        switch (matrixType) {
        case XLS:
            return new ExcelMatrixWriter(matrixPath.getAbsolutePath(), false);
        case XLSX:
            return new ExcelMatrixWriter(matrixPath.getAbsolutePath(), true);
        case CSV:
            return new CSVMatrixWriter(matrixPath.getAbsolutePath());
        default:
            throw new IllegalStateException("Unknown matrix type: " + matrixType);
        }
    }

    public static IMatrixWriter getWriter(File matrixPath, String encoding) throws IOException {
        MatrixFileTypes matrixType = MatrixFileTypes.detectFileType(matrixPath.getName());
        switch (matrixType) {
        case XLS:
            return new ExcelMatrixWriter(matrixPath.getAbsolutePath(), false);
        case XLSX:
            return new ExcelMatrixWriter(matrixPath.getAbsolutePath(), true);
        case CSV:
            return new CSVMatrixWriter(matrixPath.getAbsolutePath(), encoding);
        default:
            throw new IllegalStateException("Unknown matrix type: " + matrixType);
        }
    }

    public AdvancedMatrixWriter(File file, List<SimpleCell> mainHeaders) throws IOException {
        writer = getWriter(file);
        header = new ArrayList<>(mainHeaders.size());
        header.addAll(mainHeaders);
        convertedHeaders = new HashMap<String, Integer>(header.size());
        checkHeaders(header);
        rowNumber++;
    }

    @Override
    public void close() throws Exception {
        this.writer.close();
    }
    
    public void writeCells(Map<String, SimpleCell> line) throws IOException {

        if (!isHeaderWrited) {
            isHeaderWrited = true;
            writeHeaders();
        }

        int unknownFieldsCount = 0;

        for (Map.Entry<String, SimpleCell> cell : line.entrySet()) {
            if (!convertedHeaders.containsKey(cell.getKey())) {
                unknownFieldsCount++;
            }
        }
        if (unknownFieldsCount > 0) {
            logger.warn("Detected {} unknown fields.", unknownFieldsCount);
        }

        SimpleCell[] cells = new SimpleCell[header.size() + unknownFieldsCount];
        int additionalIndx = header.size();

        for (Map.Entry<String, SimpleCell> entry : line.entrySet()) {
            if (!convertedHeaders.containsKey(entry.getKey())) {
                convertedHeaders.put(entry.getKey(), additionalIndx);
                cells[additionalIndx++] = entry.getValue();
                header.add(new SimpleCell(entry.getKey()));
                continue;
            }
            int position = convertedHeaders.get(entry.getKey());
            cells[position] = entry.getValue();
        }

        if (unknownFieldsCount != 0) {
            writeDefineHeaderCommand(header);
        }

        writer.writeCells(cells);

        rowNumber++;
    }

    public void writeDefineHeader(List<SimpleCell> header) throws IOException {

        writeDefineHeader(header, false);
    }
    
    public void writeDefineHeader(List<SimpleCell> header, boolean onlyRedefineInternalHeader) throws IOException {
        
        SimpleCell[] tmp = header.toArray(new SimpleCell[] {});
        redefineHeaders(tmp);
        if (!onlyRedefineInternalHeader) {
            writeDefineHeaderCommand(this.header);
        } 
    }

    private void writeDefineHeaderCommand(List<SimpleCell> header) throws IOException {
        Map<String, SimpleCell> newHeader = new HashMap<>();

        for (SimpleCell sc : header) {
            newHeader.put(sc.getValue(), sc);
        }
        newHeader.put(ACTION_FIELD, new SimpleCell(DEFINE_HEADER));
        newHeader.remove(""); //if new header smaller then current, then after it's redefining we got empty cells, because we attempt write this.header. This leads to writing DefineHeader command again.
        writeCells(newHeader);
    }

    private void writeHeaders() throws IOException {
        writer.writeCells(header.toArray(new SimpleCell[] {}));
    }

    private void redefineHeaders(SimpleCell[] readedSimpleCells) throws IOException {

        int length = readedSimpleCells.length;
        int totalLength = Math.max(readedSimpleCells.length, header.size());

        if (header.size() < totalLength) {
            int count = totalLength - header.size();
            for (int n = 0; n < count; n++) {
                header.add(new SimpleCell());
            }
        }

        for (int i = 0; i < totalLength; i++) {
            SimpleCell cell = i < length ? readedSimpleCells[i] : new SimpleCell();
            // Insert system field
            if (i < header.size() && header.get(i).getValue().startsWith(SYSTEM_FIELD_PREFIX)) {
                // Keep system fields from previous header
                if (!header.get(i).getValue().equals(cell.getValue())) {
                    throw new EPSCommonException("Redefining error. Try redefine system field " + header.get(i).getValue() + " to "+ cell.getValue());
                }
            } else {
                header.set(i, cell);
            }
        }

        checkHeaders(header);
    }

    private boolean checkHeaders(List<SimpleCell> headers) throws IOException {

        SimpleCell defaultCell = new SimpleCell();
        int counter = -1;

        convertedHeaders.clear();

        for (SimpleCell item : headers) {
            counter++;
            // Skip commented fields

            if (item.getValue().startsWith("~")) {
                convertedHeaders.put(headers.get(counter).getValue(), counter);
                continue;
            }

            if (!item.equals(defaultCell) && convertedHeaders.put(headers.get(counter).getValue(), counter) != null) {

                List<Integer> positions = new ArrayList<>();
                for (int j = 0; j < headers.size(); j++) {
                    if (headers.get(j).getValue().equals(item.getValue())) {
                        positions.add(j + 1);
                    }
                }

                StringBuilder letterPositions = new StringBuilder();

                for (int pos : positions) {
                    letterPositions.append(TableUtils.columnAdress(pos));
                    letterPositions.append(rowNumber);
                    letterPositions.append(", ");
                }

                throw new IOException("Invalid matrix structure. Detected duplicated fields at "
                        + letterPositions.toString() + " positions. Field name is " + headers.get(counter).getValue());
            }            
        }

        return true;
    }
}
