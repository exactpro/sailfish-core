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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

import com.csvreader.CsvWriter;

public class CSVMatrixWriter implements IMatrixWriter {

    private final CsvWriter writer;

    public CSVMatrixWriter(String fileName) throws IOException {

        this(new FileOutputStream(fileName), ',');
    }

    public CSVMatrixWriter(String fileName, char delimiter) throws IOException {

        this(new FileOutputStream(fileName), delimiter);
    }

    public CSVMatrixWriter(String fileName, String encoding) throws FileNotFoundException {

        this(new FileOutputStream(fileName), ',', encoding);
    }

    public CSVMatrixWriter(OutputStream outputStream) {

        this(outputStream, ',');
    }

    public CSVMatrixWriter(OutputStream outputStream, char delimiter) {

        this(outputStream, delimiter, Charset.defaultCharset());
    }

    public CSVMatrixWriter(OutputStream outputStream, char delimiter, String encoding) {
        
        this(outputStream, delimiter, Charset.forName(encoding));
    }

    public CSVMatrixWriter(OutputStream outputStream, char delimiter, Charset charset) {

        writer = new CsvWriter(outputStream, delimiter, charset);
    }

    @Override
    public void close() throws Exception {

        writer.close();
    }

    @Override
    public void writeCells(SimpleCell[] cells) throws IOException {
        for (SimpleCell sc : cells) {
            writer.write(sc != null ? sc.getValue() : null);
        }
        writer.endRecord();
    }

    @Override
    public void write(String[] strings) throws IOException {
        writer.writeRecord(strings); // this method calls Record and endOfRecord

        // CSVWriter doesn't write anything if arguments are null or zero-length
        // array
        // Write it manually:
        if (strings == null || strings.length == 0) {
            writer.endRecord();
        }
    }

    @Override
    public void flush() {
        writer.flush();
    }

}
