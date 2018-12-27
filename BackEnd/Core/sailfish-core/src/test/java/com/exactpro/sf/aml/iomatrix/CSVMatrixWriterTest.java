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

import java.io.File;
import java.io.FileOutputStream;
import java.util.Random;

import org.junit.Test;

public class CSVMatrixWriterTest {

    private String tmpDir = System.getProperty("java.io.tmpdir");
    private String fileName = tmpDir + File.separator + "test.csv";
    private File tmpFile = new File(fileName);

    @Test
    public void testCSVMatrixWriterString() {

        try (CSVMatrixWriter e = new CSVMatrixWriter(fileName)) {

        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void testCSVMatrixWriterOutputStream() {

        try (CSVMatrixWriter e = new CSVMatrixWriter(new FileOutputStream(tmpFile))) {

        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void testClose() {
        try (CSVMatrixWriter e = new CSVMatrixWriter(new FileOutputStream(tmpFile))) {
            e.close();
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void testWriteCells() {
        try (CSVMatrixWriter e = new CSVMatrixWriter(new FileOutputStream(tmpFile))) {
            SimpleCell[] cells = new SimpleCell[10];
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    byte[] forString = new byte[32];
                    new Random().nextBytes(forString);
                    cells[j] = new SimpleCell(new String(forString));
                }
                e.writeCells(cells);
            }
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
        if (tmpFile.exists()) {
            if (tmpFile.length() == 0L) {
                fail("File is empty");
            }
        } else {
            fail("File not exists!");
        }
    }

    @Test
    public void testWrite() {
        try (CSVMatrixWriter e = new CSVMatrixWriter(fileName)) {
            String[] cells = new String[10];
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    byte[] forString = new byte[32];
                    new Random().nextBytes(forString);
                    cells[j] = new String(forString);
                }
                e.write(cells);
            }
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
        if (tmpFile.exists()) {
            if (tmpFile.length() == 0L) {
                fail("File is empty");
            }
        } else {
            fail("File not exists!");
        }
    }

    @Test
    public void testFlush() {
        try (CSVMatrixWriter e = new CSVMatrixWriter(fileName)) {
            String[] cells = new String[10];
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    byte[] forString = new byte[32];
                    new Random().nextBytes(forString);
                    cells[j] = new String(forString);
                }
                e.write(cells);
            }

            e.flush();

            if (tmpFile.exists()) {
                if (tmpFile.length() == 0L) {
                    fail("File is empty");
                }
            } else {
                fail("File not exists!");
            }

        } catch (Exception ex) {
            fail(ex.getMessage());
        }
    }
}
