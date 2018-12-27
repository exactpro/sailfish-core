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
import java.io.Reader;
import java.util.Arrays;
import java.util.NoSuchElementException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.ArrayUtils;

import com.csvreader.CsvReader;
import com.exactpro.sf.util.UnicodeReader;

public class CSVMatrixReader implements IMatrixReader {

    private final CsvReader reader;
    private boolean hasNext;

    public CSVMatrixReader(String fileName) throws IOException {
        this(fileName, determineCSVDelimiter(new FileInputStream(fileName), true));
    }

    public CSVMatrixReader(String fileName, char delimiter) throws IOException {

        this(new FileInputStream(fileName), delimiter, "UTF-8");
    }

    public CSVMatrixReader(String fileName, String encoding) throws IOException {

        this(new FileInputStream(fileName), determineCSVDelimiter(new FileInputStream(fileName), true), encoding);
    }

    public CSVMatrixReader(String fileName, char delimiter, String encoding) throws IOException {

        this(new FileInputStream(fileName), delimiter, encoding);
    }

    public CSVMatrixReader(InputStream inputStream) throws IOException {

        this(inputStream = wrappedStream(inputStream), determineCSVDelimiter(inputStream, false), "UTF-8");
    }

    public CSVMatrixReader(InputStream inputStream, String encoding) throws IOException {

        this(inputStream = wrappedStream(inputStream), determineCSVDelimiter(inputStream, false), encoding);
    }

    public CSVMatrixReader(InputStream inputStream, char delimiter) throws IOException {

        this(inputStream, delimiter, "UTF-8");
    }

    public CSVMatrixReader(InputStream inputStream, char delimiter, String encoding) throws IOException {

        Reader rdr = new UnicodeReader(inputStream, encoding);

        reader = new CsvReader(rdr);
        reader.setDelimiter(delimiter);
        this.reader.setSkipEmptyRecords(false);
        this.reader.setTrimWhitespace(true);
        readRecord();
    }

    @Override
    public void close() throws Exception {
        reader.close();
    }

    @Override
    public SimpleCell[] readCells() throws IOException, NoSuchElementException {

        String[] cells = read();
        SimpleCell[] simpleCells = new SimpleCell[cells.length];

        int counter = 0;
        for (String s : cells) {
            simpleCells[counter++] = new SimpleCell(s);
        }

        return simpleCells;
    }

    @Override
    public String[] read() throws IOException, NoSuchElementException {

        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        String[] values = reader.getValues();
        readRecord();

        // drop all values after last non-empty cell
        // otherwise a lot of empty (but styled) cells will be returned
        int lastNonEmptyCellIdx = ArrayUtils.INDEX_NOT_FOUND;

        for (int i = values.length - 1; i >= 0; i--) {
            if (StringUtils.isNotBlank(values[i])) {
                lastNonEmptyCellIdx = i;
                break;
            }
        }

        if(lastNonEmptyCellIdx == ArrayUtils.INDEX_NOT_FOUND) {
            return ArrayUtils.EMPTY_STRING_ARRAY;
        }

        if(values.length != lastNonEmptyCellIdx + 1) {
            values = Arrays.copyOf(values, lastNonEmptyCellIdx + 1);
        }

        for (int i=0; i<values.length; i++) {
            values[i] = values[i].trim();
        }

        return values;
    }

    private void readRecord() throws IOException {
        hasNext = reader.readRecord();
    }

    @Override
    public boolean hasNext() {
        return hasNext;
    }

    /**
     * Search first char "," ";" or "\t" and use it as CSV delimiter.
     *
     * @param is input stream for the csv file
     * @return actual delimiter
     * @throws IOException
     */
    public static char determineCSVDelimiter(InputStream is, boolean needClose) throws IOException {
        if (is.markSupported())
            is.mark(0);

        byte[] bytes = new byte[1024];
        try {
            int n = 0;
            while ((n = is.read(bytes)) != -1) {
                CSVDelimiter result = determineCSVDelimiter(bytes, n);
                if (result != null) {
                    if (needClose) {
                        is.close();
                    }
                    return result.getCharValue();
                }
            }
        } finally {
            if (is.markSupported()) {
                is.reset();
            }
        }

        if (needClose) {
            is.close();
        }
        return CSVDelimiter.COMMA.getCharValue();
    }

    /**
     * Search csv delimiter in byte array
     * @param byte array
     * @param size of byte array
     * @return csv delimiter
     */
    public static CSVDelimiter determineCSVDelimiter(byte[] bytes, int size) {

        for (int i = 0; i < size; i++) {
            byte b = bytes[i];
            CSVDelimiter delimiter = CSVDelimiter.valueOf((char) b);
            if (delimiter != null) {
                return delimiter;
            }
        }
        return null;
    }

    private static InputStream wrappedStream(InputStream in) {
        if (in.markSupported()) {
            return in;
        } else {
            return new BufferedInputStream(in);
        }
    }
}
