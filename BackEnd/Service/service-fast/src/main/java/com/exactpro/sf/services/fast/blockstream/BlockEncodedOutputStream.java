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
package com.exactpro.sf.services.fast.blockstream;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openfast.IntegerValue;
import org.openfast.template.type.codec.TypeCodec;

public class BlockEncodedOutputStream extends OutputStream {
	private final Logger logger = LoggerFactory.getLogger(BlockEncodedOutputStream.class);
	private final OutputStream stream;

	public BlockEncodedOutputStream(OutputStream stream) {
		this.stream = stream;
	}

	@Override
	public void write(byte[] msg) throws IOException {
		logger.trace("Encoding message with size:{}", msg.length);
		byte[] blockHeader = TypeCodec.UINT.encode(new IntegerValue(msg.length));
		byte[] result = Arrays.copyOf(blockHeader, blockHeader.length + msg.length);
		System.arraycopy(msg, 0, result, blockHeader.length, msg.length);

		stream.write(result);
	}

	@Override
	public void close() throws IOException {
		stream.close();
	}

	@Override
	public boolean equals(Object obj) {
		return stream.equals(obj);
	}

	@Override
	public void flush() throws IOException {
		stream.flush();
	}

	@Override
	public int hashCode() {
		return stream.hashCode();
	}

	@Override
	public String toString() {
		return stream.toString();
	}

	@Override
	public void write(byte[] arg0, int arg1, int arg2) throws IOException {
		stream.write(arg0, arg1, arg2);
	}

	@Override
	public void write(int arg0) throws IOException {
		stream.write(arg0);
	}

}
