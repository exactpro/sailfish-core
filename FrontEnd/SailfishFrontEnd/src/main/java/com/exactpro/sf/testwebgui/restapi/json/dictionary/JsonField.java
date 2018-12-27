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

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.exactpro.sf.common.messages.structures.IAttributeStructure;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

// it flatized version of IFieldStructure
@JsonInclude(Include.NON_NULL)
public abstract class JsonField {

	@JsonProperty
	protected final String name;
	@JsonProperty
	protected final String description;
	@JsonIgnore
	protected final Map<String, IAttributeStructure> attributes;
	@JsonProperty
	protected final Object defaultValue;
	// annotation on getter
	protected final boolean isRequired;
	// annotation on getter
	protected final boolean isCollection;
	// annotation on getter
	// index of field in message/group
	protected final int index;

	JsonField(String name, String description, Map<String, IAttributeStructure> attributes, Object defaultValue, 
			  boolean isRequired, boolean isCollection, int index) {
		
		this.description = description;
		this.name = name;
		this.attributes = attributes;
		this.defaultValue = defaultValue;
		this.isRequired = isRequired;
		this.isCollection = isCollection;
		this.index = index;
	}

	public String getName() {
		return this.name;
	}

	@JsonIgnore
	public Set<String> getProtocolAttributesNames() {
		if (this.attributes != null)
			return Collections.unmodifiableSet(this.attributes.keySet());

		return null;
	}

	@JsonIgnore
	public IAttributeStructure getProtocolAttributeValue(String name) {
		if (this.attributes != null)
			return this.attributes.get(name);

		return null;
	}

	public Object getProtocolAttributeValueIgnoreCase(String name) {
		if (this.attributes == null)
			return null;

		Object result = attributes.get(name);
		if (result != null)
			return result;

		for (String attrName : attributes.keySet()) {
			if (attrName.equalsIgnoreCase(name)) {
				return attributes.get(attrName);
			}
		}

		return null;
	};
	
	public String getDescription() {
		return this.description;
	}

	public Object getDefaultValue() {
		return defaultValue;
	}

	public Map<String, IAttributeStructure> getProtocolAttributes() {
		return attributes;
	}

	@JsonProperty("req")
	public boolean isRequired() {
		return isRequired;
	}

	@JsonProperty("coll")
	public boolean isCollection() {
		return isCollection;
	}

	@JsonProperty("idx")
	public int getIndex() {
		return index;
	}
}
