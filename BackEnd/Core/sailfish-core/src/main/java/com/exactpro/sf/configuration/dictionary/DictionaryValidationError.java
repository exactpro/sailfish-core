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
package com.exactpro.sf.configuration.dictionary;

import java.io.Serializable;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

@SuppressWarnings("serial")
public class DictionaryValidationError implements Serializable {

	private String message;

	private String field;

	private String error;

	private DictionaryValidationErrorLevel level;

	private DictionaryValidationErrorType type;

	public DictionaryValidationError(String message, String field, String error, DictionaryValidationErrorLevel level, DictionaryValidationErrorType type) {
		this.message = message;
		this.field   = field;
		this.error   = error;
		this.level   = level;
		this.type    = type;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getField() {
		return field;
	}

	public void setField(String field) {
		this.field = field;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

	public DictionaryValidationErrorLevel getLevel() {
		return level;
	}

	public void setLevel(DictionaryValidationErrorLevel level) {
		this.level = level;
	}

	public DictionaryValidationErrorType getType() {
		return type;
	}

	public void setType(DictionaryValidationErrorType type) {
		this.type = type;
	}

	@Override
	public String toString() {
	    ToStringBuilder builder = new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE);

	    builder.append("message", message);
	    builder.append("field", field);
	    builder.append("error", error);
	    builder.append("level", level);
	    builder.append("type", type);

	    return builder.toString();
	}
}
