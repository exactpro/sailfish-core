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

import com.exactpro.sf.common.impl.messages.xml.configuration.JavaType;
import com.exactpro.sf.common.messages.structures.IAttributeStructure;

/**
 * This structure should be immutable
 */
public class AttributeStructure implements IAttributeStructure {
	private final String name;
	
	private final String value;
	
	private final Object castValue;
	
	private final JavaType type;
	
	public AttributeStructure(String name, String value, Object castValue, JavaType type) {
		this.name = name;
		this.value = value;
		this.castValue = castValue;
		this.type = type;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getValue() {
		return value;
	}

	@Override
    public <T> T getCastValue() {
        return (T)castValue;
	}

	@Override
	public JavaType getType() {
		return type;
	}
}
