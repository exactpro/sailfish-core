/*******************************************************************************
 * Copyright 2009-2019 Exactpro (Exactpro Systems Limited)
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
			  Map<String, ? extends IAttributeStructure> attributes, Map<String, ? extends IAttributeStructure> values,
			  JavaType javaType, boolean isRequired, boolean isCollection, boolean isServiceName, String defaultValue) throws EPSCommonException {
		this(name, namespace, description, referenceName, attributes, values, javaType,
				isRequired, isCollection, isServiceName, defaultValue, null);
	}

	protected FieldStructure(String name, String namespace, String description, String referenceName,
			  Map<String, ? extends IAttributeStructure> attributes, Map<String, ? extends IAttributeStructure> values, JavaType javaType, boolean isRequired,
			  boolean isCollection, boolean isServiceName, String defaultValue, StructureType structureType) throws EPSCommonException {
		this.name = name;
		this.namespace = namespace;
		this.description = description;
		this.javaType = javaType;
		this.required = isRequired;
		this.collection = isCollection;
		this.serviceName = isServiceName;
		this.defaultValue = StructureUtils.castValueToJavaType(defaultValue, javaType);
        this.attributes = attributes != null ? Collections.unmodifiableMap(attributes) : Collections.emptyMap();
        this.values = values != null ? Collections.unmodifiableMap(values) : Collections.emptyMap();

        if(structureType == null && !this.values.isEmpty()) {
            structureType = StructureType.ENUM;
		}

        if(structureType == null) {
            structureType = StructureType.SIMPLE;
		}

        this.structureType = structureType;
		this.referenceName = referenceName;
	}

	public FieldStructure(String name, String namespace, JavaType javaType, boolean isCollection, StructureType structureType, Map<String, IAttributeStructure> attributes, Map<String, IAttributeStructure> values) {
		this(name, namespace, null, null, attributes, values, javaType, false, isCollection, false, null, structureType);
	}

	@Override
	public boolean isComplex() {
        return structureType == StructureType.COMPLEX;
	}

	@Override
	public boolean isEnum() {
        return structureType == StructureType.ENUM;
	}

	@Override
	public boolean isSimple() {
        return structureType == StructureType.SIMPLE;
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
    public <T> T getDefaultValue() {
        return (T)defaultValue;
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
    public Map<String, IFieldStructure> getFields() {
        throw new UnsupportedOperationException("Field '" + name + "' don't have another fields");
	}
}