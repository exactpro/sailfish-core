/*******************************************************************************
 *   Copyright 2009-2019 Exactpro (Exactpro Systems Limited)
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 ******************************************************************************/

package com.exactpro.sf.services.fast;

import org.openfast.ByteUtil;
import org.openfast.util.RecordingInputStream;

import java.io.IOException;
import java.io.InputStream;

public class ResizableRecordingInputStream extends RecordingInputStream {
    private static final int SIZE = 1024;
    private byte[] buffer = new byte[SIZE];
    private int index = 0;
    private InputStream in;

    public ResizableRecordingInputStream(InputStream inputStream) {
        super(inputStream);
        in = inputStream;
    }

    @Override
    public int read() throws IOException {
        int read = in.read();

        if (index == buffer.length) {
            resize(2 * buffer.length);
        }

        buffer[index++] = (byte) read;
        return read;
    }

    public String toString() {
        return ByteUtil.convertByteArrayToBitString(buffer, index);
    }

    @Override
    public byte[] getBuffer() {
        byte[] b = new byte[index];
        System.arraycopy(buffer, 0, b, 0, index);
        return b;
    }

    @Override
    public void clear() {
        index = 0;
        buffer = new byte[SIZE];
    }


    private void resize(int capacity) {
        byte[] copy = new byte[capacity];
        System.arraycopy(buffer, 0, copy, 0, buffer.length);
        buffer = copy;
    }

}
