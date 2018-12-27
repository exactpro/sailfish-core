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
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import com.exactpro.sf.aml.AMLException;
import com.exactpro.sf.aml.generator.matrix.Column;
import com.exactpro.sf.aml.generator.matrix.JavaStatement;

public class AdvancedMatrixReader implements AutoCloseable {

    private final static String ACTION_FIELD = Column.Action.getName();
    private final static String DEFINE_HEADER = JavaStatement.DEFINE_HEADER.getValue();
    private final static String SYSTEM_FIELD_PREFIX = Column.getSystemPrefix();

    private final IMatrixReader reader;
    private List<SimpleCell> header;
    private final MatrixFileTypes fileType;
    private boolean positionFound = false;
    private int actionPosition = -1;
    private int rowNumber = 1;

    public AdvancedMatrixReader(File file) throws IOException, AMLException {
        reader = getReader(file);
        fileType = MatrixFileTypes.detectFileType(file.getName());
        fillHeaders();
    }

    public AdvancedMatrixReader(File file, String encoding) throws IOException, AMLException {
    	reader = getReader(file, encoding);
        fileType = MatrixFileTypes.detectFileType(file.getName());
        fillHeaders();
    }

	private void fillHeaders() throws IOException, AMLException {

		header = new ArrayList<>();
        List<SimpleCell> cells = null;
        try {
        	cells = Arrays.asList(reader.readCells());
        } catch (NoSuchElementException e) {
        	// The matrix is empty
        }

        if (cells != null) {
	        header.addAll(cells);
	        checkHeaders(header);
	        rowNumber++;
        }
	}

    public Map<String, SimpleCell> readCells() throws IOException, AMLException {

        SimpleCell[] readedCells = reader.readCells();

        if (actionPosition > -1) {
            if (readedCells.length > actionPosition && readedCells[actionPosition].getValue().equals(DEFINE_HEADER)) {
                redefineHeaders(readedCells);
            }
        }

        Map<String, SimpleCell> result = new LinkedHashMap<>();

        for (int i = 0; i < Math.min(header.size(), readedCells.length); i++) {
            SimpleCell value = readedCells[i];

            if(!value.getValue().isEmpty()) {
                result.put(header.get(i).getValue(), value);
            }
        }
        // Key-value pair with key "" inserts into result map, if "DefineHeader"
        // command overrides longer header than original. Delete it's.
        result.remove("");

        rowNumber++;

        return result;
    }

    public static IMatrixReader getReader(File matrixPath) throws IOException {
        MatrixFileTypes matrixType = MatrixFileTypes.detectFileType(matrixPath.getName());
        switch (matrixType) {
        case XLS:
            return new ExcelMatrixReader(matrixPath.getAbsolutePath(), false);
        case XLSX:
            return new ExcelMatrixReader(matrixPath.getAbsolutePath(), true);
        case CSV:
            return new CSVMatrixReader(matrixPath.getAbsolutePath());
        default:
            throw new IllegalStateException("Unknown matrix type: " + matrixType);
        }
    }

    public static IMatrixReader getReader(File matrixPath, String encoding) throws IOException {
        MatrixFileTypes matrixType = MatrixFileTypes.detectFileType(matrixPath.getName());
        switch (matrixType) {
        case XLS:
            return new ExcelMatrixReader(matrixPath.getAbsolutePath(), false);
        case XLSX:
            return new ExcelMatrixReader(matrixPath.getAbsolutePath(), true);
        case CSV:
            return new CSVMatrixReader(matrixPath.getAbsolutePath(), encoding);
        default:
            throw new IllegalStateException("Unknown matrix type: " + matrixType);
        }
    }

    public MatrixFileTypes getFileType() {
        return fileType;
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public List<SimpleCell> getHeader() {
        return Collections.unmodifiableList(header);
    }

    public boolean hasNext() {
        return reader.hasNext();
    }

    @Override
    public void close() throws Exception {
        this.reader.close();
    }

    private void redefineHeaders(SimpleCell[] readedSimpleCells) throws AMLException {

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
            } else {
                header.set(i, cell);
            }
        }

        checkHeaders(header);
    }

    private boolean checkHeaders(List<SimpleCell> headers) throws AMLException {

        Set<String> tmp = new HashSet<>(headers.size());

        int i = 0;
        for (SimpleCell item : headers) {
            String value = item.getValue();

            if(value.isEmpty()) {
                i++;
                continue;
            }

            if (!positionFound && value.equalsIgnoreCase(ACTION_FIELD)) {
                actionPosition = i;
            }

            i++;
            // Skip commented fields
            if (value.startsWith("~")) {
                continue;
            }

            if (!tmp.add(value)) {

                List<Integer> positions = new ArrayList<>();
                for (int j = 0; j < headers.size(); j++) {
                    if (headers.get(j).getValue().equals(value)) {
                        positions.add(j + 1);
                    }
                }

                StringBuilder letterPositions = new StringBuilder();

                for (int pos : positions) {
                    letterPositions.append(TableUtils.columnAdress(pos));
                    letterPositions.append(rowNumber);
                    letterPositions.append(", ");
                }
                throw new AMLException("Invalid matrix structure. Detected duplicated fields at "
                        + letterPositions.toString() + " positions. Field name is " + header.get(i - 1).getValue());
            }
        }

        positionFound = true;
        return true;
    }
}