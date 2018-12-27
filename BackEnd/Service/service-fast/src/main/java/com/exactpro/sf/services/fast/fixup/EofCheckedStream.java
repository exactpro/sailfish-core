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
package com.exactpro.sf.services.fast.fixup;

import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EofCheckedStream extends InputStream {
	private final Logger logger = LoggerFactory.getLogger(EofCheckedStream.class);

	private InputStream is;

	public EofCheckedStream(InputStream is) {
		this.is = is;
	}

	@Override
	public int available() throws IOException {
		return is.available();
	}

	@Override
	public void close() throws IOException {
		is.close();
	}

	@Override
	public synchronized void mark(int readlimit) {
		is.mark(readlimit);
	}

	@Override
	public boolean markSupported() {
		return is.markSupported();
	}

	@Override
	public int read() throws IOException {
		int retVal = is.read();
		if (retVal == -1) {
			logger.trace("Endof stream encountered whie reading a byte from input stream");
			throw new EofIOException("Endof stream encountered whie reading a byte from input stream");
		}
		return retVal;
	}

	@Override
	public int read(byte[] arg0, int arg1, int arg2) throws IOException {
		int retVal = is.read(arg0, arg1, arg2);
		if (retVal == -1) {
			logger.trace("Endof stream encountered whie reading a byte from input stream");
			throw new EofIOException("Endof stream encountered whie reading a byte from input stream");
		}
		return retVal;
	}

	@Override
	public int read(byte[] b) throws IOException {
		int retVal = is.read(b);
		if (retVal == -1) {
			logger.trace("Endof stream encountered whie reading a byte from input stream");
			throw new EofIOException("Endof stream encountered whie reading a byte from input stream");
		}
		return retVal;
	}

	@Override
	public void reset() throws IOException {
		is.reset();
	}

	@Override
	public long skip(long arg0) throws IOException {
		return is.skip(arg0);
	}

}
