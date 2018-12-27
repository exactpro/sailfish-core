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

import com.exactpro.sf.common.impl.messages.xml.configuration.JavaType;
import com.exactpro.sf.common.messages.structures.IAttributeStructure;
import com.exactpro.sf.testwebgui.dictionaries.DictionaryEditorModel;

public class ModifiableAttributeStructure implements IAttributeStructure {
	
	private String name;
	
	private String value;
	
	private Object castValue;
	
	private JavaType type;
	
	public ModifiableAttributeStructure() {}
	
	public ModifiableAttributeStructure(String name, String value, JavaType type) {
		this.name = name;
		this.value = value;
		this.type = type;
	}
	
	public ModifiableAttributeStructure(String name, String value, Object castValue, JavaType type) {
		this(name, value, type);
		this.castValue = castValue;
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
	public Object getCastValue() {
		return castValue;
	}
	
	@Override
	public JavaType getType() {
		return type;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public void setCastValue(Object castValue) {
		this.castValue = castValue;
	}

	public void setType(JavaType type) {
		this.type = type;
	}

    public void setType(String type) {
        this.type = DictionaryEditorModel.fromTypeLabel(type);
    }
}