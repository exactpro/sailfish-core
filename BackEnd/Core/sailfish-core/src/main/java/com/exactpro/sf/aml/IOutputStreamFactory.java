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
package com.exactpro.sf.aml;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.io.output.NullOutputStream;

public interface IOutputStreamFactory {

	
	OutputStream createOutputStream(String streamName) throws IOException; /* file name */
	OutputStream createOutputStream(File file) throws IOException;
	
	public static class DefaultOutputStreamFactory implements IOutputStreamFactory {

		@Override
		public OutputStream createOutputStream(String fileName) throws IOException {
			return new FileOutputStream(fileName);
		}

		@Override
		public OutputStream createOutputStream(File file) throws IOException {
			return new FileOutputStream(file);
		}
		
	}
	
	public static class NullOutputStreamFactory implements IOutputStreamFactory {

		@Override
		public OutputStream createOutputStream(String streamName) {
			return new NullOutputStream();
		}

		@Override
		public OutputStream createOutputStream(File file) {
			return new NullOutputStream();
		}
		
	}

	
}
