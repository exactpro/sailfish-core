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
package com.exactpro.sf.testwebgui.download;

import java.io.File;
import java.util.Comparator;

public class FileNameComparator implements Comparator<FileAdapter> {

	@Override
	public int compare(FileAdapter fa1, FileAdapter fa2) {
		final File f1 = fa1.getFile();
		final File f2 = fa2.getFile();

		if (f1.isDirectory() && f2.isFile())
			return -1;
		if (f1.isFile() && f2.isDirectory())
			return 1;

		return f1.getName().compareTo(f2.getName());
	}

}
