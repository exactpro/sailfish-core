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
package com.exactpro.sf.storage.entities;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;

import com.exactpro.sf.configuration.suri.SailfishURIUtils;
import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.scriptrunner.languagemanager.LanguageManager;
import com.exactpro.sf.storage.StorageException;

public class StoredMatrix {

	private Long id;

	private String name;

	// relative (to MATRIX folder)
	private String filePath;

	private String scriptSettindsPath;

	private String description;

	private Date date;

	private String creator;

	private String language;

	private List<StoredScriptRun> scriptRuns;

	private SimpleDateFormat dateFormat;

	private String link;

	private String provider;

	public StoredMatrix() {
		this.date = new Date();
		this.creator = "Unknown creator";
		this.scriptRuns = new ArrayList<StoredScriptRun>();
		this.dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
		this.language = LanguageManager.AUTO.getName();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public List<StoredScriptRun> getScriptRuns() {
		return scriptRuns;
	}

	public void setScriptRuns(List<StoredScriptRun> scriptRuns) {
		this.scriptRuns = scriptRuns;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public InputStream readStream(IWorkspaceDispatcher workspaceDispatcher) {
		try {
		    File target = workspaceDispatcher.getFile(FolderType.MATRIX, this.filePath);
			return new FileInputStream(target);
		} catch (Exception e) {
			throw new StorageException("Can't open stored matrix", e);
		}
	}

	public void writeStream(IWorkspaceDispatcher workspaceDispatcher, InputStream stream) {
		try {
		    File result = workspaceDispatcher.createFile(FolderType.MATRIX, true, filePath);

			try (BufferedOutputStream writer = new BufferedOutputStream(new FileOutputStream(result))) {
    			int ch;
    			while ( (ch = stream.read()) != -1 ) {
    				writer.write(ch);
    			}
			}
		} catch(IOException e) {
			throw new StorageException("Could not save [" + name +"] matrix");
		}
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}


	public String getScriptSettindsPath() {
		return scriptSettindsPath;
	}

	public void setScriptSettindsPath(String scriptSettindsPath) {
		this.scriptSettindsPath = scriptSettindsPath;
	}

	public String getCreator() {
		return creator;
	}

	public void setCreator(String creator) {
		this.creator = creator;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this).
			append("id", id).
			append("name", name).
			append("path", filePath).
			append("description", description).
			toString();
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = SailfishURIUtils.sanitize(language);
	}

	public String stringDate() {
		return dateFormat.format(date);
	}

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = SailfishURIUtils.sanitize(provider);
    }
}
