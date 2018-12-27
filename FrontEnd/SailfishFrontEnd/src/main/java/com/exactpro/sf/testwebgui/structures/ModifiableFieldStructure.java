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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import com.exactpro.sf.common.impl.messages.xml.configuration.JavaType;
import com.exactpro.sf.common.messages.structures.IAttributeStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.StructureType;
import com.exactpro.sf.common.util.EPSCommonException;

public class ModifiableFieldStructure implements IFieldStructure {

	protected String 									id;
	protected String 			  						name;
	protected String 			  						namespace;
	protected String 			  						description;
	protected Map<String, ModifiableAttributeStructure> attributes;
	protected Object						 			reference;

	private   Map<String, ModifiableAttributeStructure> values;
	private   JavaType 			 	 					javaType;
	private   boolean 			 	 					required;
	private   boolean 			 	 					collection;
	private   boolean                                   serviceName;
	private   String 				 	 				defaultValue;

	public ModifiableFieldStructure() {
		this.required = false;
		this.collection = false;
		this.serviceName = false;
		this.attributes = new LinkedHashMap<>();
		this.values = new LinkedHashMap<>();
	}

	public ModifiableFieldStructure(String id, String name, String namespace, String description,
			  Map<String, ModifiableAttributeStructure> attributes, Map<String, ModifiableAttributeStructure> values,
			  JavaType javaType, boolean isRequired, boolean isCollection, boolean isServiceName, String defaultValue) throws EPSCommonException {

		this.id = id;
		this.name = name;
		this.namespace = namespace;
		this.description = description;
		this.javaType = javaType;
		this.required = isRequired;
		this.collection = isCollection;
		this.serviceName = isServiceName;
		this.defaultValue = defaultValue;

		this.attributes = attributes == null ? new LinkedHashMap<String, ModifiableAttributeStructure>() : attributes;
		this.values = values == null ? new LinkedHashMap<String, ModifiableAttributeStructure>() : values;
	}

	@Override
	public Object getAttributeValueByName(String name) {

        IAttributeStructure attr = getAttributes().get(name);
		return (attr == null) ? null : attr.getCastValue();
	}

	@Override
	public Object getAttributeValueIgnoreCase(String name) {
        Map<String, IAttributeStructure> fullAttributes = getAttributes();
	    
        for (String attrName: fullAttributes.keySet()) {
			if (attrName.equalsIgnoreCase(name)) {
                return fullAttributes.get(attrName).getCastValue();
			}
		}

		return null;
	}

	@Override
	public Set<String> getAttributeNames() {
        return getAttributes().keySet();
	}

	@Override
	public boolean isComplex() {
        return false;
	}

	public boolean isMessage() {
		return isComplex() && this.reference == null;
	}

	public boolean isSubMessage() {
		return isComplex() && this.reference != null;
	}

	@Override
	public boolean isEnum() {
        IFieldStructure parent = getImplReference();
        return isImplEnum() ||
                (parent != null && parent.isEnum());
	}

	@Override
	public boolean isSimple() {
        return !isEnum();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public Map<String, IAttributeStructure> getAttributes() {
        return getAttributeMapping(this.attributes, parent -> true, IFieldStructure::getAttributes);
	}

	@Override
	public Map<String, IAttributeStructure> getValues() {
        return getAttributeMapping(this.values, IFieldStructure::isEnum, IFieldStructure::getValues);
	}

	@Override
	public StructureType getStructureType() {
        return isEnum() ? StructureType.ENUM : StructureType.SIMPLE;
	}

	@Override
	public JavaType getJavaType() {
        if (javaType == null && this.reference != null) {
            return getImplReference().getJavaType();
        }
		return javaType;
	}

	@Override
	public boolean isRequired() {
		return required;
	}

	@Override
	public boolean isCollection() {
		return collection;
	}

	@Override
    public boolean isServiceName() {
        return serviceName;
    }

    @Override
	public Object getDefaultValue() {
		return defaultValue;
	}

	@Override
	public String getNamespace() {
		return namespace;
	}

	@Override
	public String getReferenceName() {
	    return reference == null ? null : ((ModifiableFieldStructure) reference).getName();
	}

	@Override
	public ModifiableFieldStructure getField(String name) {
		throw new UnsupportedOperationException("Fields don't have another fields");
	}

	@Override
	public List<String> getFieldNames() {
		throw new UnsupportedOperationException("Fields don't have another fields");
	}

	@Override
	public List<IFieldStructure> getFields() {
		throw new UnsupportedOperationException("Fields don't have another fields");
	}

	/** IMPL **/

    public boolean isImplEnum() {
        return !this.values.isEmpty();
    }
	
    public boolean isImplSimple() {
        return this.values.isEmpty();
    }
    
    public JavaType getImplJavaType() {
        return javaType;
    }
    
	public List<ModifiableAttributeStructure> getImplAttributes() {
		return new ArrayList<>(attributes.values());
	}

	public int getImplAttributesSize() {
        return attributes.size();
    }
	
	public List<ModifiableAttributeStructure> getImplValues() {
		return new ArrayList<>(values.values());
	}

	public int getImplValuesSize() {
        return values.size();
    }
	
	public void addAttribute(ModifiableAttributeStructure attr) {
		this.attributes.put(attr.getName(), attr);
	}

	public void addValue(ModifiableAttributeStructure val) {
		this.values.put(val.getName(), val);
	}

	public void addAttributes(List<ModifiableAttributeStructure> attrs) {
		Map<String, ModifiableAttributeStructure> toAdd = new LinkedHashMap<>();
		for (ModifiableAttributeStructure attr : attrs) {
			toAdd.put(attr.getName(), attr);
		}
		this.attributes.putAll(toAdd);
	}

	public void addValues(List<ModifiableAttributeStructure> vals) {
		Map<String, ModifiableAttributeStructure> toAdd = new LinkedHashMap<>();
		for (ModifiableAttributeStructure val : vals) {
			toAdd.put(val.getName(), val);
		}
		this.attributes.putAll(toAdd);
	}

	public void removeAttribute(String name) {
		this.attributes.remove(name);
	}

	public void removeValue(String name) {
		this.values.remove(name);
	}

	public void setAttributes(Map<String, ModifiableAttributeStructure> attributes) {
		if (attributes == null) return;
		this.attributes = attributes;
	}

	public void setValues(Map<String, ModifiableAttributeStructure> values) {
		if (values == null) return;
		this.values = values;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Object getReference() {
		return this.reference;
	}

	public ModifiableFieldStructure getImplReference() {
		return (ModifiableFieldStructure)this.reference;
	}

	public void setReference(Object reference) {
		this.reference = reference;
	}

	public void setJavaType(JavaType javaType) {
		this.javaType = javaType;
	}

	public void setRequired(boolean required) {
		this.required = required;
	}

	public void setCollection(boolean collection) {
		this.collection = collection;
	}

	public void setServiceName(boolean serviceName) {
        this.serviceName = serviceName;
    }

    public String getImplDefaultValue() {
		return this.defaultValue;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	//Fix problem with searching getter / setter in the inherited class
	public void setDefaultValue(Object defaultValue) {
	    setDefaultValue((String)defaultValue);
    }

    @SuppressWarnings("unchecked")
    private Map<String, IAttributeStructure> getAttributeMapping(Map<String, ModifiableAttributeStructure> attributes,
            Predicate<IFieldStructure> parentPredicate,
            Function<IFieldStructure, Map<String, IAttributeStructure>> parentMapper) {
        Map<String, IAttributeStructure> result = (Map<String, IAttributeStructure>)(Map<String, ?>)attributes;
        IFieldStructure parent = getImplReference();
        if (parent != null && parentPredicate.test(parent)) {
            result = new HashMap<>();
            result.putAll(parentMapper.apply(parent));
            result.putAll((Map<String, IAttributeStructure>)(Map<String, ?>)attributes);
        }
        return Collections.unmodifiableMap(result);
    }
}