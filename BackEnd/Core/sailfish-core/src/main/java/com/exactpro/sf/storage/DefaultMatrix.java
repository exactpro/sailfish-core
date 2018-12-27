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
package com.exactpro.sf.storage;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Date;

import org.apache.commons.lang3.builder.ToStringBuilder;

import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.scriptrunner.languagemanager.AutoLanguageFactory;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class DefaultMatrix implements IMatrix, Serializable {

    private static final long serialVersionUID = -837199127428674128L;

    private Long id;

	private String name;

	private String description;

	private String creator;

	private SailfishURI languageURI;

	// relative (to MATRIX folder)
	private String filePath;

	private Date date;

	private String scriptSettindsPath;

	private String link;

	private SailfishURI providerURI;

	public DefaultMatrix() {
		this.date = new Date();
		this.creator = "Unknown creator";
		this.languageURI = AutoLanguageFactory.URI;
	}

	public DefaultMatrix(Long id, String name, String description, String creator, SailfishURI languageURI, String filePath, Date date, String link, SailfishURI providerURI) {
		this.id = id;
		this.name = name;
		this.date = date;
		if (creator != null) {
			this.creator = creator;
		} else {
			this.creator = "Unknown creator";
		}
		if (description != null) {
			this.description = description;
		} else {
			this.description = "";
		}
        if (link != null) {
			this.link = link;
		} else {
			this.link = "";
		}

        this.providerURI = providerURI;

        if (languageURI != null) {
			this.languageURI = languageURI;
		} else {
			this.languageURI = AutoLanguageFactory.URI;
		}
		this.filePath = filePath;
	}

	@Override
	public String getFilePath() {
		return filePath;
	}

    @Override
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public InputStream readStream(IWorkspaceDispatcher workspaceDispatcher) {
	    try {
            File target = workspaceDispatcher.getFile(FolderType.MATRIX, this.filePath);
            return new FileInputStream(target);
        } catch (Exception e) {
            throw new StorageException("Can't open stored matrix", e);
        }
	}

	@Override
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

	@Override
	public Date getDate() {
		return date;
	}

	@Override
    public void setDate(Date date) {
		this.date = date;
	}

	@Override
	public String getScriptSettindsPath() {
		return scriptSettindsPath;
	}

	public void setScriptSettindsPath(String scriptSettindsPath) {
		this.scriptSettindsPath = scriptSettindsPath;
	}

	@Override
	public String getCreator() {
		return creator;
	}

	public void setCreator(String creator) {
		this.creator = creator;
	}

	@Override
	public SailfishURI getLanguageURI() {
		return languageURI;
	}

	public void setLanguageURI(SailfishURI languageURI) {
		this.languageURI = languageURI;
	}

	@Override
	public Long getId() {
		return id;
	}

    @Override
    public String getLink() {
        return link;
    }

    @Override
    public void setLink(String link) {
        this.link = link;
    }

    @JsonIgnore
    @Override
    public boolean getReloadEnabled() {
        return link != null && !link.equals("");
    }

    @Override
    public SailfishURI getProviderURI() {
        return providerURI;
    }

    @Override
    public void setProviderURI(SailfishURI providerURI) {
        this.providerURI = providerURI;
    }

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		DefaultMatrix that = (DefaultMatrix) o;

		if (!id.equals(that.id)) return false;

		return true;
	}

	@Override
	public int hashCode() {
		return id.hashCode();
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
}
