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
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.exactpro.sf.common.impl.messages.xml.configuration.JavaType;
import com.exactpro.sf.common.messages.structures.IAttributeStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.StructureType;
import com.exactpro.sf.common.messages.structures.StructureUtils;
import com.exactpro.sf.common.util.EPSCommonException;

/**
 * This structure should be immutable
 */
public class FieldStructure implements IFieldStructure {

	protected final String name;
	protected final String namespace;
	protected final String description;
	protected final String referenceName;
	protected final StructureType structureType;
	protected final Map<String, IAttributeStructure> attributes;

	private final Object defaultValue;
	private final boolean required;
	private final boolean collection;
	private final boolean serviceName;
	private final JavaType javaType;
	private final Map<String, IAttributeStructure> values;

    public FieldStructure(String name, String namespace, JavaType javaType, boolean isCollection, StructureType structureType) throws EPSCommonException {
        this(name, namespace, null, null, null, null, javaType, false, isCollection, false, null, structureType);
    }
	
	public FieldStructure(String name, String namespace, String description, String referenceName,
			  Map<String, IAttributeStructure> attributes, Map<String, IAttributeStructure> values,
			  JavaType javaType, boolean isRequired, boolean isCollection, boolean isServiceName, String defaultValue) throws EPSCommonException {

		this(name, namespace, description, referenceName, attributes, values, javaType,
				isRequired, isCollection, isServiceName, defaultValue, null);
	}

	protected FieldStructure(String name, String namespace, String description, String referenceName,
			  Map<String, IAttributeStructure> attributes, Map<String, IAttributeStructure> values, JavaType javaType, boolean isRequired,
			  boolean isCollection, boolean isServiceName, String defaultValue, StructureType structureType) throws EPSCommonException {

		this.name = name;
		this.namespace = namespace;
		this.description = description;
		this.javaType = javaType;
		this.required = isRequired;
		this.collection = isCollection;
		this.serviceName = isServiceName;
		this.defaultValue = StructureUtils.castValueToJavaType(defaultValue, javaType);

		StructureType tempStructureType = structureType != null ? structureType : null;

		if (attributes != null) {
			this.attributes = Collections.unmodifiableMap(attributes);
		} else {
			this.attributes = Collections.emptyMap();
		}

		if (values != null) {
			this.values = Collections.unmodifiableMap(values);
		} else {
			this.values = Collections.emptyMap();
		}

		if (tempStructureType == null && !this.values.isEmpty()) {
			tempStructureType = StructureType.ENUM;
		}

		if (tempStructureType == null) {
			tempStructureType = StructureType.SIMPLE;
		}

		this.structureType = tempStructureType;

		this.referenceName = referenceName;
	}

	@Override
	public Object getAttributeValueByName(String name) {
		if (this.attributes == null) return null;
		IAttributeStructure attr = this.attributes.get(name);
		return attr == null ? null : attr.getCastValue();
	}

	@Override
	public Object getAttributeValueIgnoreCase(String name) {

		if (this.attributes == null) return null;

		for (String attrName : attributes.keySet()) {
			if (attrName.equalsIgnoreCase(name)) {
				return attributes.get(attrName).getCastValue();
			}
		}

		return null;
	}

	@Override
	public Set<String> getAttributeNames() {
		if (this.attributes == null) return null;
		return this.attributes.keySet();
	}

	@Override
	public boolean isComplex() {
		return StructureType.COMPLEX.equals(this.structureType);
	}

	@Override
	public boolean isEnum() {
		return StructureType.ENUM.equals(this.structureType);
	}

	@Override
	public boolean isSimple() {
		return StructureType.SIMPLE.equals(this.structureType);
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
		return attributes;
	}

	@Override
	public Map<String, IAttributeStructure> getValues() {
		return values;
	}

	@Override
	public StructureType getStructureType() {
		return structureType;
	}

	@Override
	public JavaType getJavaType() {
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
		return referenceName;
	}

	@Override
	public IFieldStructure getField(String name) {
		throw new UnsupportedOperationException("Field '" + this.name + "' don't have another fields");
	}

	@Override
	public List<String> getFieldNames() {
		throw new UnsupportedOperationException("Field '" + this.name + "' don't have another fields");
	}

	@Override
	public List<IFieldStructure> getFields() {
        throw new UnsupportedOperationException("Field '" + this.name + "' don't have another fields");
	}
}