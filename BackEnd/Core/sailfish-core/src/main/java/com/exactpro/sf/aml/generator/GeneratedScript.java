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
package com.exactpro.sf.aml.generator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GeneratedScript {

	private File mainFile;
	private List<File> files;

	public GeneratedScript()
	{
		this.files = new ArrayList<File>();
	}

	public File getMainFile() {
		return mainFile;
	}

	public void setMainFile(File file) {
		this.mainFile = file;
	}

	public void addFile(File file) {
		this.files.add(file);
	}

	public List<File> getFilesList() {
		return this.files;
	}

}
