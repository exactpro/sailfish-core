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

import com.exactpro.sf.common.impl.messages.xml.configuration.JavaType;
import com.exactpro.sf.common.messages.structures.IAttributeStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.messages.structures.StructureType;

/**
 * This structure should be immutable
 */
public class MessageStructure extends FieldStructure implements IMessageStructure {

	private final List<IFieldStructure> fields;
	private final List<String> 		    fieldNames;

	private IMessageStructure reference;

	public MessageStructure(String name, String namespace, boolean isCollection, IMessageStructure reference) {
	    this(name, namespace, null, null, false, isCollection, null, reference);
	}

	public MessageStructure(String name, String namespace, String description, boolean isRequired, boolean isCollection,
			Map<String, IAttributeStructure> attributes, IMessageStructure reference) {

		this(name, namespace, description, null, isRequired, isCollection, attributes, reference);
	}

	public MessageStructure(String name, String namespace, String description, List<IFieldStructure> fields,
			Map<String, IAttributeStructure> attributes) {

		this(name, namespace, description, fields, false, false, attributes, null);
	}

	private MessageStructure(String name, String namespace, String description, List<IFieldStructure> fields,
			boolean isRequired, boolean isCollection, Map<String, IAttributeStructure> attributes, IMessageStructure reference) {

		super(name, namespace, description, reference != null ? reference.getName() : null, attributes,
				null, null, isRequired, isCollection, false, null, StructureType.COMPLEX);

		if (fields != null) {
			this.fields = Collections.unmodifiableList(fields);
		} else {
			this.fields = Collections.emptyList();
		}

		this.fieldNames = createFieldNames();
		this.reference = reference;
	}

	private List<String> createFieldNames() {
		List<String> result = new ArrayList<>();

		for (IFieldStructure field : this.fields) {
			result.add(field.getName());
		}

		return Collections.unmodifiableList(result);
	}

	@Override
	public IFieldStructure getField(String name) {

		List<IFieldStructure> curFields = this.reference != null ? this.reference.getFields() : this.fields;

		if (curFields == null) return null;

		for (IFieldStructure fieldStructure : curFields) {
			if (fieldStructure.getName().equals(name)) {
				return fieldStructure;
			}
		}

		return null;
	}

	@Override
	public List<String> getFieldNames() {

		if (this.reference != null) {
			return this.reference.getFieldNames();
		}

		return this.fieldNames;
	}

	@Override
	public List<IFieldStructure> getFields() {

		if (this.reference != null) {
			return this.reference.getFields();
		}

		return this.fields;
	}

	@Override
	public Map<String, IAttributeStructure> getValues() {
		throw new UnsupportedOperationException("Messages don't have values");
	}

	@Override
	public JavaType getJavaType() {
		throw new UnsupportedOperationException("Messages don't have a java type");
	}

	@Override
	public boolean isRequired() {

		if (this.reference != null) {
			return super.isRequired();
		}

		throw new UnsupportedOperationException("Messages don't have a 'required' parameter");
	}

	@Override
	public boolean isCollection() {

		if (this.reference != null) {
			return super.isCollection();
		}

		throw new UnsupportedOperationException("Messages don't have a 'collection' parameter");
	}

    @Override
    public boolean isServiceName() {
        throw new UnsupportedOperationException("Messages don't have a 'serviceName' parameter");
    }

	@Override
	public Object getDefaultValue() {
		throw new UnsupportedOperationException("Messages don't have a default value");
	}
}
