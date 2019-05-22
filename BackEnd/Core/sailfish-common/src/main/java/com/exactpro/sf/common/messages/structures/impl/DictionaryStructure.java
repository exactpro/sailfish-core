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
package com.exactpro.sf.common.messages.structures.impl;

import java.util.Collections;
import java.util.Map;

import com.exactpro.sf.common.messages.structures.IAttributeStructure;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;

/**
 * This structure should be immutable
 */
public class DictionaryStructure implements IDictionaryStructure {
	private final String namespace;
	private final String description;
    private final Map<String, IMessageStructure> messages;
    private final Map<String, IFieldStructure> fields;
	private final Map<String, IAttributeStructure> attributes;
	
	public DictionaryStructure(String namespace, String description, Map<String, IAttributeStructure> attributes,
            Map<String, IMessageStructure> messages, Map<String, IFieldStructure> fields) {
		this.namespace = namespace;
		this.description = description;
        this.messages = messages != null ? Collections.unmodifiableMap(messages) : Collections.emptyMap();
        this.fields = fields != null ? Collections.unmodifiableMap(fields) : Collections.emptyMap();
        this.attributes = attributes != null ? Collections.unmodifiableMap(attributes) : Collections.emptyMap();
	}

	@Override
	public Map<String, IAttributeStructure> getAttributes() {
		return attributes;
	}

	@Override
    public Map<String, IMessageStructure> getMessages() {
        return messages;
	}
	
	@Override
    public Map<String, IFieldStructure> getFields() {
        return fields;
	}

	@Override
	public String getNamespace() {
        return namespace;
	}
	
	@Override
	public String getDescription() {
		return description;
	}
}