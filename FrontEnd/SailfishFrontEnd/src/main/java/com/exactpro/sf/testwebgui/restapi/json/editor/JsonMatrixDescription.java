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
package com.exactpro.sf.testwebgui.restapi.json.editor;

import java.util.Date;

import com.exactpro.sf.configuration.suri.SailfishURI;

public class JsonMatrixDescription {

	protected Long id;
	protected String name;
	protected Date date;
	protected SailfishURI languageURI;

	public JsonMatrixDescription() {
		super();
	}

	public JsonMatrixDescription(Long id, String name, Date date, SailfishURI languageURI) {
		super();
		this.id = id;
		this.name = name;
		this.date = date;
		this.languageURI = languageURI;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public SailfishURI getLanguageURI() {
		return languageURI;
	}

	public void setLanguageURI(SailfishURI languageURI) {
		this.languageURI = languageURI;
	}

}
