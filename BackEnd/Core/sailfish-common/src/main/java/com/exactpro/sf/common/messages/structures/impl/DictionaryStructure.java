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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
	private final Map<String, IMessageStructure> msgStructures;
	private final Map<String, IFieldStructure> fieldStructures;
	private final Map<String, IAttributeStructure> attributes;

	private final List<IMessageStructure> msgStructureList;
	private final List<IFieldStructure> fieldStructureList;
	
	public DictionaryStructure(String namespace, String description, Map<String, IAttributeStructure> attributes,
			Map<String, IMessageStructure> msgStructures, Map<String, IFieldStructure> fieldStructures) {
		
		this.namespace = namespace;
		this.description = description;
		
		if (msgStructures != null) {
			this.msgStructures = Collections.unmodifiableMap(msgStructures);
			this.msgStructureList = Collections.unmodifiableList(new ArrayList<>(this.msgStructures.values()));
		} else {
			this.msgStructures = Collections.emptyMap();
			this.msgStructureList = Collections.emptyList();
		}
		
		if (fieldStructures != null) {
			this.fieldStructures = Collections.unmodifiableMap(fieldStructures);
			this.fieldStructureList = Collections.unmodifiableList(new ArrayList<>(this.fieldStructures.values()));
		} else {
			this.fieldStructures = Collections.emptyMap();
			this.fieldStructureList = Collections.emptyList();
		}

		if (attributes != null) {
			this.attributes = Collections.unmodifiableMap(attributes);
		} else {
			this.attributes = Collections.emptyMap();
		}
	}

	@Override
	public Map<String, IAttributeStructure> getAttributes() {
		return attributes;
	}

	@Override
    public Object getAttributeValueByName(String name) {
        IAttributeStructure attr = attributes.get(name);
        return (attr == null) ? null : attr.getCastValue();
    }

    @Override
	public IMessageStructure getMessageStructure(String name) {
		return msgStructures.get(name);
	}
	
	@Override
	public IFieldStructure getFieldStructure(String name) {
		return this.fieldStructures.get(name);
	}
	
	@Override
	public List<IMessageStructure> getMessageStructures() {
		return this.msgStructureList;
	}
	
	@Override
	public List<IFieldStructure> getFieldStructures() {
		return this.fieldStructureList;
	}

	@Override
	public String getNamespace() {
		return this.namespace;
	}
	
	@Override
	public String getDescription() {
		return description;
	}
}