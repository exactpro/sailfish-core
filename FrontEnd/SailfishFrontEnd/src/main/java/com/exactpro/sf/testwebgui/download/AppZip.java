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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppZip {

	private static final Logger logger = LoggerFactory.getLogger(AppZip.class);

	private List<String> fileList = new ArrayList<String>();
	private List<String> absoluteList = new ArrayList<String>();

	private String sourcePath = null;

	private String currentFolder;

	public void zipIt(String zipFile) {

		byte[] buffer = new byte[1024];


		try (	FileOutputStream fos = new FileOutputStream(zipFile);
				ZipOutputStream zos = new ZipOutputStream(fos);
				) {


			logger.debug("Output to Zip : {}", zipFile);

			int i = 0;
			for (String file : this.fileList) {

				logger.debug("File Added : {}", file);
				ZipEntry ze = new ZipEntry(file);
				zos.putNextEntry(ze);

				try (FileInputStream in = new FileInputStream(this.absoluteList.get(i++))) {
					int len;
					while ((len = in.read(buffer)) > 0) {
						zos.write(buffer, 0, len);
					}
				}
			}

			zos.closeEntry();

			logger.debug("Zipping was done");

		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	public void generateFileList(File node) {
		generateFileList(node, true);
	}

	private void generateFileList(File node, boolean first) {

		if (node.isFile()) {

			if (null == this.sourcePath) {
				String file = node.getAbsoluteFile().toString();
				this.sourcePath = file.substring(0, file.lastIndexOf(File.separator));
			}

			if (null == this.currentFolder) {
				this.fileList.add(generateZipEntry(node.getAbsoluteFile().toString()));
			} else {
				this.fileList.add(this.currentFolder+File.separator+generateZipEntry(node.getAbsoluteFile().toString()));
			}
			this.absoluteList.add(node.getAbsoluteFile().toString());

		}

		if (node.isDirectory()) {

			if (first) {
				this.sourcePath = node.getAbsolutePath().toString();
				if (this.sourcePath.endsWith(File.separator)) {
                    this.sourcePath = this.sourcePath.substring(0, this.sourcePath.length());
                }
				this.currentFolder = this.sourcePath.substring(this.sourcePath.lastIndexOf(File.separator)+1);
			}

			String[] subNote = node.list();
			for (String filename : subNote) {
				generateFileList(new File(node, filename), false);
			}

		}

		if (first) {
			this.sourcePath = null;
			this.currentFolder = null;
		}

	}

	private String generateZipEntry(String file) {
		if (null == this.sourcePath) {
			return file.substring(file.lastIndexOf(File.separator)+1);
		}

    	return file.substring(this.sourcePath.length()+1, file.length());
    }

}
