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
package com.exactpro.sf.testwebgui.restapi.json;

import java.util.ArrayList;
import java.util.List;

public class JsonErrorMessage implements WithJsonAMLError {

	private final List<JsonAMLError> errors;

	public JsonErrorMessage(JsonAMLError error) {
		this.errors = new ArrayList<JsonAMLError>(1);
		this.errors.add(error);
	}

	public JsonErrorMessage(List<JsonAMLError> errors) {
		this.errors = errors;
	}

	@Override
	public List<JsonAMLError> getErrors() {
		return errors;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("JsonErrorMessage [errors=");
		builder.append(errors);
		builder.append("]");
		return builder.toString();
	}

}
