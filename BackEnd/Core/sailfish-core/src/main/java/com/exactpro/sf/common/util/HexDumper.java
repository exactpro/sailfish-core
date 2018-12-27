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

import org.apache.mina.core.buffer.IoBuffer;

import java.util.ArrayList;
import java.util.List;

public class HexDumper {

    private static final int BYTES_COLUMN_WIDTH = 16*2+8;
    private final List<String> addresses;
    private final List<String> bytes;
    private final List<String> printableStrings;

    public HexDumper(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("byte array is null");
        }

        this.addresses = new ArrayList<>();
        this.bytes = new ArrayList<>();
        this.printableStrings = new ArrayList<>();

        int index = 0;
        int length = data.length;

        while (index < length) {
            StringBuilder addressColumn = new StringBuilder();
            StringBuilder bytesColumn = new StringBuilder();
            StringBuilder printableColumn = new StringBuilder();

            addressColumn.append(Integer.toHexString(index));

            while (addressColumn.length() < 8) {
                addressColumn.insert(0, "0");
            }

            int rowIndex = 0;
            while (rowIndex < 16 && index < length) {
                byte b = data[index];
                String xs = Integer.toHexString(b);
                if (xs.length() < 2) {
                    bytesColumn.append(0);
                    bytesColumn.append(xs);
                } else {
                    // remove extra ffffff characters if any
                    bytesColumn.append(xs.substring(xs.length()-2));
                }
                if (rowIndex % 2 == 1) {
                    bytesColumn.append(" ");
                }
                if (b < 32 || b > 126) {
                    printableColumn.append(".");
                } else {
                    printableColumn.append((char)b);
                }
                index++;
                rowIndex++;
            }

            while (bytesColumn.length() < BYTES_COLUMN_WIDTH) {
                bytesColumn.append(" ");
            }

            this.addresses.add(addressColumn.toString());
            this.bytes.add(bytesColumn.toString());
            this.printableStrings.add(printableColumn.toString());
        }
    }

	public String getHexdump() {
        StringBuilder result = new StringBuilder();

        for (int i=0; i<this.addresses.size(); i++) {
            result.append(this.addresses.get(i)).append(": ")
                    .append(this.bytes.get(i)).append(" ")
                    .append(this.printableStrings.get(i)).append("\r\n");
        }

        return result.toString();
    }

    public String getPrintableString() {
        return String.join("", printableStrings);
    }

    public String getBytes() {
        return String.join("", bytes);
    }

    public static String getHexdump(byte[] bytes) {
        return new HexDumper(bytes).getHexdump();
    }

	public static byte[] peakBytes(IoBuffer in, int lengthLimit) {
		if (lengthLimit == 0) {
			throw new IllegalArgumentException("lengthLimit: " + lengthLimit + " (expected: 1+)");
		}

		boolean truncate = in.remaining() > lengthLimit;
		int size;
		if (truncate) {
			size = lengthLimit;
		} else {
			size = in.remaining();
		}

		if (size == 0) {
			return new byte[0];
		}

		int mark = in.position();

		byte[] array = new byte[size];

		in.get(array);
		in.position(mark);

		return array;
	}

    /**
     * Dumps an {@link IoBuffer} to a hex formatted string.
     *
     * @param in the buffer to dump
     * @param lengthLimit the limit at which hex dumping will stop
     * @return a hex formatted string representation of the <i>in</i> {@link IoBuffer}.
     */
    public static String getHexdump(IoBuffer in, int lengthLimit) {
        boolean truncate = in.remaining() > lengthLimit;
        int size = truncate ? lengthLimit : in.remaining();

        if (size == 0) {
            return "empty";
        }

        byte[] data = new byte[size];
        int mark = in.position();
        in.get(data, 0, size);
        in.position(mark);

        String result = new HexDumper(data).getBytes();

        return truncate ? result + "..." : result;
    }
}
