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
import java.util.List;

import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.testwebgui.restapi.json.JsonAMLError;
import com.exactpro.sf.testwebgui.restapi.json.WithJsonAMLError;

public class JsonMatrix extends JsonMatrixDescription implements WithJsonAMLError {

	private final List<JsonMatrixTestCase> data;

	private final List<JsonAMLError> errors;

	public JsonMatrix(Long id, String name, Date date, SailfishURI languageURI, List<JsonMatrixTestCase> data,
			List<JsonAMLError> errors) {
		super(id, name, date, languageURI);
		this.data = data;
		this.errors = errors;
	}

	public List<JsonMatrixTestCase> getData() {
		return data;
	}

	@Override
	public List<JsonAMLError> getErrors() {
		return errors;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("JsonMatrix [data=");
		builder.append(data);
		builder.append(", errors=");
		builder.append(errors);
		builder.append(", id=");
		builder.append(id);
		builder.append(", name=");
		builder.append(name);
		builder.append(", date=");
		builder.append(date);
		builder.append(", language=");
		builder.append(languageURI);
		builder.append("]");
		return builder.toString();
	}

}
