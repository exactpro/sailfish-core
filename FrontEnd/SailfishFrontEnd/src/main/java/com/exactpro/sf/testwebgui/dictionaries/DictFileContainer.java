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
package com.exactpro.sf.testwebgui.dictionaries;

import java.io.Serializable;

import com.exactpro.sf.configuration.suri.SailfishURI;

@SuppressWarnings("serial")
public class DictFileContainer implements Serializable {

	private SailfishURI uri;
	private String namespace;
	private String fileName;

	public DictFileContainer(SailfishURI uri, String fileName, String namespace){
		this.uri = uri;
		this.fileName = fileName;
		this.namespace = namespace;
	}

	/**
	 * relative (to DICTIONARIES folder) path
	 */
	public String getFileName() {
		return fileName;
	}

	/**
	 * relative (to DICTIONARIES folder) path
	 */
	public void setFileName(String file) {
		this.fileName = file;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public SailfishURI getURI() {
		return uri;
	}

	public void setURI(SailfishURI uri) {
		this.uri = uri;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fileName == null) ? 0 : fileName.hashCode());
		result = prime * result + ((namespace == null) ? 0 : namespace.hashCode());
		result = prime * result + ((uri == null) ? 0 : uri.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DictFileContainer other = (DictFileContainer) obj;
		if (fileName == null) {
			if (other.fileName != null)
				return false;
		} else if (!fileName.equals(other.fileName))
			return false;
		if (namespace == null) {
			if (other.namespace != null)
				return false;
		} else if (!namespace.equals(other.namespace))
			return false;
		if (uri == null) {
			if (other.uri != null)
				return false;
		} else if (!uri.equals(other.uri))
			return false;
		return true;
	}
}