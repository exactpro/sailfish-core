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
package com.exactpro.sf.common.util;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.lang3.StringUtils;

/**
 * Wrapper for create text files.
 *
 * @author dmitry.guriev
 *
 */
public class TextOutputStream implements Closeable {

    protected final static String EOL = System.getProperty("line.separator");
    protected final static String TAB = "\t";

    private OutputStream os;

    public TextOutputStream(OutputStream os) {
        this.os = os;
    }

    /**
     * Write string to OutputStream immediately.
     *
     * @param s
     * @throws IOException
     */
    public void write(String s) throws IOException {
        this.os.write(s.getBytes());
    }

    /**
     * Write string with indentation to OutputStream immediately.
     *
     * @param s
     * @throws IOException
     */
    public void write(int tabs, String s) throws IOException {
        write(StringUtils.repeat(TAB, tabs) + s);
    }

    /**
     * Write formatted string to OutputStream immediately.
     *
     * @param s
     * @throws IOException
     */
    public void write(String s, Object... args) throws IOException {
        write(String.format(s, args));
    }

    /**
     * Write formatted string with EOL and indentation to OutputStream
     * immediately.
     *
     * @param s
     * @throws IOException
     */
    public void write(int tabs, String s, Object... args) throws IOException {
        write(tabs, String.format(s, args));
    }

    /**
     * Write EOL to OutputStream immediately.
     *
     * @param s
     * @throws IOException
     */
    public void writeLine() throws IOException {
        write(EOL);
    }

    /**
     * Write string with EOL to OutputStream immediately.
     *
     * @param s
     * @throws IOException
     */
    public void writeLine(String s) throws IOException {
        write(s);
        writeLine();
    }

    /**
     * Write string with EOL and indentation to OutputStream immediately.
     *
     * @param s
     * @throws IOException
     */
    public void writeLine(int tabs, String s) throws IOException {
        write(tabs, s);
        writeLine();
    }

    /**
     * Write formatted string with EOL to OutputStream immediately.
     *
     * @param s
     * @throws IOException
     */
    public void writeLine(String s, Object... args) throws IOException {
        write(s, args);
        writeLine();
    }

    /**
     * Write formatted string with EOL and indentation to OutputStream
     * immediately.
     *
     * @param s
     * @throws IOException
     */
    public void writeLine(int tabs, String s, Object... args) throws IOException {
        write(tabs, s, args);
        writeLine();
    }

    /**
     * Close OutputStream and destroy cache.
     *
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        this.os.close();
    }
}
