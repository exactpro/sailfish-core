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

import com.exactpro.sf.common.impl.messages.xml.configuration.JavaType;
import com.exactpro.sf.common.messages.structures.IAttributeStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.messages.structures.StructureType;

/**
 * This structure should be immutable
 */
public class MessageStructure extends FieldStructure implements IMessageStructure {
    private final Map<String, IFieldStructure> fields;

    private final IMessageStructure reference;

	public MessageStructure(String name, String namespace, boolean isCollection, IMessageStructure reference) {
	    this(name, namespace, null, null, false, isCollection, null, reference);
	}

	public MessageStructure(String name, String namespace, String description, boolean isRequired, boolean isCollection,
			Map<String, IAttributeStructure> attributes, IMessageStructure reference) {
		this(name, namespace, description, null, isRequired, isCollection, attributes, reference);
	}

    public MessageStructure(String name, String namespace, String description, Map<String, IFieldStructure> fields,
			Map<String, IAttributeStructure> attributes) {
		this(name, namespace, description, fields, false, false, attributes, null);
	}

    private MessageStructure(String name, String namespace, String description, Map<String, IFieldStructure> fields,
			boolean isRequired, boolean isCollection, Map<String, IAttributeStructure> attributes, IMessageStructure reference) {
		super(name, namespace, description, reference != null ? reference.getName() : null, attributes,
				null, null, isRequired, isCollection, false, null, StructureType.COMPLEX);
        this.fields = fields != null ? Collections.unmodifiableMap(fields) : Collections.emptyMap();
		this.reference = reference;
	}

	@Override
    public Map<String, IFieldStructure> getFields() {
        return reference != null ? reference.getFields() : fields;
    }

	@Override
	public Map<String, IAttributeStructure> getValues() {
		throw new UnsupportedOperationException("Messages don't have values. Message: " + getName());
	}

	@Override
	public JavaType getJavaType() {
		throw new UnsupportedOperationException("Messages don't have a java type. Message: " + getName());
	}

	@Override
	public boolean isRequired() {
        if(reference != null) {
			return super.isRequired();
		}

		throw new UnsupportedOperationException("Messages don't have a 'required' parameter. Message: " + getName());
	}

	@Override
	public boolean isCollection() {
        if(reference != null) {
			return super.isCollection();
		}

		throw new UnsupportedOperationException("Messages don't have a 'collection' parameter. Message: " + getName());
	}

    @Override
    public boolean isServiceName() {
        throw new UnsupportedOperationException("Messages don't have a 'serviceName' parameter. Message: " + getName());
    }

	@Override
    public <T> T getDefaultValue() {
		throw new UnsupportedOperationException("Messages don't have a default value. Message: " + getName());
	}
}
