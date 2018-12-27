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
package com.exactpro.sf.testwebgui.dictionaries;

import java.io.Serializable;

import com.exactpro.sf.testwebgui.BeanUtil;
import com.exactpro.sf.testwebgui.structures.ModifiableAttributeStructure;
import com.exactpro.sf.testwebgui.structures.ModifiableFieldStructure;

@SuppressWarnings("serial")
public class AttributeModel implements Serializable {
	
	private ModifiableAttributeStructure actual; // Copy for gui editor

	private ModifiableAttributeStructure original; // Reference to attribute of owner

	private ModifiableFieldStructure owner;

	private boolean inheritedAttribute = false;

	private boolean overrides = false;

	private boolean attribute; // attribute/value

	public static ModifiableAttributeStructure cloneAttribute(ModifiableAttributeStructure toClone) {

		ModifiableAttributeStructure result = new ModifiableAttributeStructure();

		result.setName(toClone.getName());
		result.setType(toClone.getType());
		result.setValue(toClone.getValue());
		result.setCastValue(toClone.getCastValue());

		return result;
	}

	public AttributeModel(ModifiableAttributeStructure original, ModifiableFieldStructure owner, boolean attribute) {
		this.owner = owner;
		this.original = original;
		this.attribute = attribute;
		this.actual = cloneAttribute(original);
	}

	public ModifiableFieldStructure getOwner() {
		return owner;
	}

	public void setOwner(ModifiableFieldStructure owner) {
		this.owner = owner;
	}
	
	public String getActualType() {
		if (this.actual == null || this.actual.getType() == null) return null;
        return BeanUtil.getJavaTypeLabel(this.actual.getType());
	}
	
	public void setActualType(String str) {
        this.actual.setType(DictionaryEditorModel.fromTypeLabel(str));
	}

	public ModifiableAttributeStructure getActual() {
		return actual;
	}

	public void setActual(ModifiableAttributeStructure actual) {
		this.actual = actual;
	}

	public ModifiableAttributeStructure getOriginal() {
		return original;
	}

	public void setOriginal(ModifiableAttributeStructure original) {
		this.original = original;
	}

	public boolean isInheritedAttribute() {
		return inheritedAttribute;
	}

	public void setInheritedAttribute(boolean inheritedAttribute) {
		this.inheritedAttribute = inheritedAttribute;
	}

	public boolean isOverrides() {
		return overrides;
	}

	public void setOverrides(boolean overrides) {
		this.overrides = overrides;
	}

	public boolean isAttribute() {
		return attribute;
	}
}
