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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import javax.faces.event.ActionListener;

import com.exactpro.sf.util.DateTimeUtility;
import org.primefaces.component.filedownload.FileDownloadActionListener;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.format.DateTimeFormatter;

public class FileAdapter implements Serializable
{
	private static final long serialVersionUID = -841841214852788979L;

	private static final Logger logger = LoggerFactory.getLogger(FileAdapter.class);
	private static final DateTimeFormatter formatter = DateTimeUtility.createFormatter("yyyy-MM-dd HH:mm:ss");

	private final File file; // canonicalPath
	private final String relativePath; // path relative to ROOT;
	private String size;
	private String lastModification;
	private Long rawLastModification;
	private Long rawSize;
	private String name;
	private boolean directory;

	public FileAdapter(File file, String relativePath) {
		this.file = file;
		this.relativePath = relativePath;

		setDirectory(file.isDirectory());

		String sizeParam = (directory) ? "-" : humanReadableByteCount(file.length(), false);
		setSize(sizeParam);
		setLastModification(formatter.format(DateTimeUtility.toLocalDateTime(file.lastModified())));
		setRawLastModification(file.lastModified());
		setRawSize(file.length());
		setName(file.getName());

	}

	private String humanReadableByteCount(long bytes, boolean si)
	{
		int unit = si ? 1000 : 1024;
	    if (bytes < unit) return bytes + " B";
	    int exp = (int) (Math.log(bytes) / Math.log(unit));
	    String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
	    return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
	}

	public ActionListener exportFile()
	{
		return new FileDownloadActionListener();
	}

	public StreamedContent getStrContent()
	{
		if(!file.exists())
			return null;

		try {
			InputStream stream = new BufferedInputStream(new FileInputStream(file));
			return new DefaultStreamedContent(stream, "*/*", file.getName());
		}
		catch(IOException e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}

	public long getFolderSize(File directory) {
		long length = 0;
		for (File file : directory.listFiles()) {
			if (file.isFile())
				length += file.length();
			else
				length += getFolderSize(file);
		}
		return length;
	}

	public void updateFolderSize() {
		Long length = getFolderSize(file);
		this.rawSize = length;
		this.size = humanReadableByteCount(length, false);
	}

	public void setLastModification(String lastModification) {
		this.lastModification = lastModification;
	}

	public String getLastModification() {
		return lastModification;
	}

	public void setSize(String size) {
		this.size = size;
	}

	public String getSize() {
		return size;
	}

	public void setRawSize(Long rawSize) {
		this.rawSize = rawSize;
	}

	public Long getRawSize() {
		return rawSize;
	}

	public void setRawLastModification(Long rawLastModification) {
		this.rawLastModification = rawLastModification;
	}

	public Long getRawLastModification() {
		return rawLastModification;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setDirectory(boolean directory) {
		this.directory = directory;
	}

	public boolean isDirectory() {
		return directory;
	}

	public File getFile() {
		return file;
	}

	public String getRelativePath() {
		return relativePath;
	}

	@Override
	public boolean equals(Object obj) {
	    if (obj instanceof FileAdapter) {
	        return this.file.equals(((FileAdapter)obj).file);
	    }
	    return false;
	}
}
