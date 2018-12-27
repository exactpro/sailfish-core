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
package com.exactpro.sf.common.messages.structures;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.exactpro.sf.common.util.EPSCommonException;

public class StructureBuilder {
	
	private final Map<String, IMessageStructure> msgStructures;
	private final Map<String, IFieldStructure> fieldStructures;
	
	private String namespace;
	
	public StructureBuilder(String namespace) {
		this(namespace, new HashMap<String, IMessageStructure>(), new HashMap<String, IFieldStructure>());
	}
	
	protected StructureBuilder(String namespace, Map<String, IMessageStructure> msg, Map<String, IFieldStructure> field) {
	    this.namespace = namespace;
        this.msgStructures = msg;
        this.fieldStructures = field;
	}
	
	public void addMessageStructure(IMessageStructure msgStructure) {
		
		String name = msgStructure.getName();

		if (name == null) {
			throw new IllegalArgumentException("name parameter is null");
		}

		if (this.msgStructures.containsKey(name)) {
			throw new EPSCommonException("There is another message with \"" + name + "\" name in \"" + this.namespace + "\"");
		}

		this.msgStructures.put(name, msgStructure);
	}
	
	public void addFieldStructure(IFieldStructure fieldStructure) {
		
		String name = fieldStructure.getName();
		
		if (name == null) {
			throw new IllegalArgumentException("name parameter is null");
		}
		
		if (this.fieldStructures.containsKey(name)) {
			throw new EPSCommonException("There is another field with \"" + name + "\" name in \"" + this.namespace + "\"");
		}
		
		this.fieldStructures.put(name, fieldStructure);
	}
	
	public IMessageStructure getMessageStructure(String name) {
		return msgStructures.get(name);
	}
	
	public IFieldStructure getFieldStructure(String name) {
		return this.fieldStructures.get(name);
	}
	
	public List<IMessageStructure> getMsgStructures() {
		return new ArrayList<>(msgStructures.values());
	}

	public Map<String, IMessageStructure> getMsgStructureMap() {
		return msgStructures;
	}
	
	public List<IFieldStructure> getFieldStructures() {
		return new ArrayList<>(fieldStructures.values());
	}

	public Map<String, IFieldStructure> getFieldStructureMap() {
		return fieldStructures;
	}

	public String getNamespace() {
		return namespace;
	}
}
