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
package com.exactpro.sf.testwebgui.structures;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.exactpro.sf.common.messages.structures.IAttributeStructure;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;

public class ModifiableDictionaryStructure implements IDictionaryStructure {
	
	private String namespace;
	private String description;
	private final Map<String, ModifiableAttributeStructure> attributes;
	private final Map<String, ModifiableMessageStructure> msgStructures;
	private final Map<String, ModifiableFieldStructure> fieldStructures;
	
	public ModifiableDictionaryStructure(String namespace, String description, Map<String, ModifiableAttributeStructure> attributes,
			Map<String, ModifiableMessageStructure> msgStructures, Map<String, ModifiableFieldStructure> fieldStructures) {
		
		this.namespace = namespace;
		this.description = description;
		this.msgStructures = msgStructures;
		this.fieldStructures = fieldStructures;
		this.attributes = attributes;
	}

	public ModifiableDictionaryStructure(String namespace, String description) {
	    
	    this();
	    this.namespace = namespace;
	    this.description = description;
	}
	
	public ModifiableDictionaryStructure() {
	    this.msgStructures = new LinkedHashMap<>();
        this.fieldStructures = new LinkedHashMap<>();
        this.attributes = new LinkedHashMap<>();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, IAttributeStructure> getAttributes() {
        return (Map<String, IAttributeStructure>) (Map<String, ?>) attributes;
    }

	@Override
    public Object getAttributeValueByName(String name) {
        ModifiableAttributeStructure attr = attributes.get(name);
        return (attr == null) ? null : attr.getCastValue();
    }

    @Override
	public ModifiableMessageStructure getMessageStructure(String name) {
		return msgStructures.get(name);
	}
	
	@Override
	public ModifiableFieldStructure getFieldStructure(String name) {
		return this.fieldStructures.get(name);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<IMessageStructure> getMessageStructures() {
		return (List<IMessageStructure>)(List<?>)new ArrayList<>(this.msgStructures.values());
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<IFieldStructure> getFieldStructures() {
		return (List<IFieldStructure>)(List<?>)new ArrayList<>(this.fieldStructures.values());
	}
	
	@Override
	public String getNamespace() {
		return this.namespace;
	}
	
	@Override
	public String getDescription() {
		return description;
	}
	
	/** IMPL **/

	public List<ModifiableMessageStructure> getImplMessageStructures() {
		return new ArrayList<>(this.msgStructures.values());
	}
	
	public List<ModifiableFieldStructure> getImplFieldStructures() {
		return new ArrayList<>(this.fieldStructures.values());
	}
	
	public void addMessageStructure(ModifiableMessageStructure structure) {
		this.msgStructures.put(structure.getName(), structure);
	}
	
	public void addFieldStructure(ModifiableFieldStructure structure) {
		this.fieldStructures.put(structure.getName(), structure);
	}
	
	public void removeMessageStructure(String name) {
		this.msgStructures.remove(name);
	}
	
	public void removeFieldStructure(String name) {
		this.fieldStructures.remove(name);
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}