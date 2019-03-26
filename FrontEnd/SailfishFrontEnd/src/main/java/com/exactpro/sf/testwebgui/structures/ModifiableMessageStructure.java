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

import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.map.ListOrderedMap;

import com.exactpro.sf.common.impl.messages.xml.configuration.JavaType;
import com.exactpro.sf.common.messages.structures.IAttributeStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.messages.structures.StructureType;

public class ModifiableMessageStructure extends ModifiableFieldStructure implements IMessageStructure {
    protected ListOrderedMap<String, ModifiableFieldStructure> fields;

	public ModifiableMessageStructure() {
        this.fields = new ListOrderedMap<>();
	}

	public ModifiableMessageStructure(String id, String name, String namespace, String description, Map<String, ModifiableAttributeStructure> attributes,
			boolean isRequired, boolean isCollection, ModifiableMessageStructure reference) {
		this(id, name, namespace, description, null, attributes, isRequired, isCollection, reference);
	}

	public ModifiableMessageStructure(String id, String name, String namespace, String description,
            Map<String, ModifiableFieldStructure> fields, Map<String, ModifiableAttributeStructure> attributes) {
		this(id, name, namespace, description, fields, attributes, false, false, null);
	}

	private ModifiableMessageStructure(String id, String name, String namespace, String description,
            Map<String, ModifiableFieldStructure> fields, Map<String, ModifiableAttributeStructure> attributes,
			boolean isRequired, boolean isCollection, ModifiableMessageStructure reference) {
		super(id, name, namespace, description, attributes, null, null, isRequired, isCollection, false, null);

		if (fields != null) {
            this.fields = ListOrderedMap.listOrderedMap(fields);
		} else {
            this.fields = new ListOrderedMap<>();
		}

		this.reference = reference;
	}

    @SuppressWarnings("unchecked")
	@Override
    public Map<String, IFieldStructure> getFields() {
		if (this.reference != null) {
			return ((ModifiableMessageStructure)this.reference).getFields();
		}

        return (Map<String, IFieldStructure>)(Map<String, ?>)fields;
	}

	@Override
	public Map<String, IAttributeStructure> getValues() {
		throw new UnsupportedOperationException("Messages don't have values. Message: " + getName());
	}

	@Override
	public List<ModifiableAttributeStructure> getImplValues() {
		throw new UnsupportedOperationException("Messages don't have values. Message: " + getName());
	}

	@Override
	public void addValue(ModifiableAttributeStructure val) {
		throw new UnsupportedOperationException("Messages don't have values. Message: " + getName());
	}

	@Override
	public void addValues(List<ModifiableAttributeStructure> vals) {
		throw new UnsupportedOperationException("Messages don't have values. Message: " + getName());
	}

	@Override
    public void removeValue(String name) {
		throw new UnsupportedOperationException("Messages don't have values. Message: " + getName());
	}

	@Override
	public JavaType getJavaType() {
		throw new UnsupportedOperationException("Messages don't have a java type. Message: " + getName());
	}

	@Override
	public void setJavaType(JavaType type) {
		throw new UnsupportedOperationException("Messages don't have a java type. Message: " + getName());
	}

	@Override
	public boolean isRequired() {
		if (this.reference != null) {
			return super.isRequired();
		}

		throw new UnsupportedOperationException("Messages don't have a 'required' parameter. Message: " + getName());
	}
	
    @Override
    public boolean isComplex() {
        return true;
    }

    @Override
    public boolean isEnum() {
        return false;
    }
    
    @Override
    public boolean isImplEnum() {
        return false;
    }
    
    @Override
    public boolean isSimple() {
        return false;
    }
    
    @Override
    public boolean isImplSimple() {
        return false;
    }
    
    @Override
    public StructureType getStructureType() {
        return StructureType.COMPLEX;
    }
    
	@Override
	public void setRequired(boolean req) {
		if (this.reference != null) {
			super.setRequired(req);
			return;
		}

		throw new UnsupportedOperationException("Messages don't have a 'required' parameter. Message: " + getName());
	}

	@Override
	public boolean isCollection() {
		if (this.reference != null) {
			return super.isCollection();
		}

		throw new UnsupportedOperationException("Messages don't have a 'collection' parameter. Message: " + getName());
	}

	@Override
	public void setCollection(boolean col) {
		if (this.reference != null) {
			super.setCollection(col);
			return;
		}

		throw new UnsupportedOperationException("Messages don't have a 'collection' parameter. Message: " + getName());
	}

    @Override
    public boolean isServiceName() {
        throw new UnsupportedOperationException("Messages don't have a 'serviceName' parameter. Message: " + getName());
    }

    @Override
    public void setServiceName(boolean serviceName) {
        throw new UnsupportedOperationException("Messages don't have a 'serviceName' parameter. Message: " + getName());
    }

	@Override
    public <T> T getDefaultValue() {
		throw new UnsupportedOperationException("Messages don't have a default value. Message: " + getName());
	}

	@Override
	public String getImplDefaultValue() {
		throw new UnsupportedOperationException("Messages don't have a default value. Message: " + getName());
	}

	@Override
	public void setDefaultValue(String defaultValue) {
		throw new UnsupportedOperationException("Messages don't have a default value. Message: " + getName());
	}

	/** IMPL **/

	public void addField(ModifiableFieldStructure field) {
        this.fields.put(field.getName(), field);
	}

    public void addField(int index, ModifiableFieldStructure field) {
        this.fields.put(index, field.getName(), field);
    }

	public void removeField(ModifiableFieldStructure field) {
        this.fields.remove(field.getName());
    }

    public ListOrderedMap<String, ModifiableFieldStructure> getImplFields() {
		return fields;
	}
}
