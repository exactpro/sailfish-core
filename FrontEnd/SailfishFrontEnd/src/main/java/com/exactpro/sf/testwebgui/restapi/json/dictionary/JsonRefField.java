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
package com.exactpro.sf.testwebgui.restapi.json.dictionary;

import java.util.Map;

import com.exactpro.sf.common.messages.IFieldInfo.FieldType;
import com.exactpro.sf.common.messages.structures.IAttributeStructure;
import com.fasterxml.jackson.annotation.JsonProperty;

public class JsonRefField extends JsonField {

	@JsonProperty
	private final String ref;

	public JsonRefField(String name, String description, Map<String, IAttributeStructure> attributes, Object defaultValue,
			boolean isRequired, boolean isCollection, int index, String ref) {
		super(name, description, attributes, defaultValue, isRequired, isCollection, index);
		this.ref = ref;
	}

	public String getRef() {
		return ref;
	}

	@JsonProperty("type")
	public FieldType getFieldType() {
		return FieldType.SUBMESSAGE;
	}
}
