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
package com.exactpro.sf.aml.generator.matrix;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.exactpro.sf.aml.IField;
/**
 * This class represent a single cell value from matrix
 *
 * @author dmitry.guriev
 *
 */
@SuppressWarnings("serial")
public class Value implements IField, Cloneable, Serializable {

	private boolean isReference = false;
	private boolean isJava = false;
	private String fieldName;
	private String value;
	private final String origValue;
	private boolean check = true;
	private int index = 0;
	private List<RefParameter> parameters;

	public Value(String value) {
		setValue(value);
		this.origValue = value;
		this.parameters = new LinkedList<RefParameter>();
	}

	public Value(String origValue, String newValue) {
		setValue(newValue);
		this.origValue = origValue;
		this.parameters = new LinkedList<RefParameter>();
	}

	/**
	 * Return <code>true</code> if value referred to another message.
	 * <code>false</code> otherwise.
	 */
	public boolean isReference() {
		return isReference;
	}

	/**
	 * Set reference flag. <code>true</code> if value referred to another
	 * message. <code>false</code> otherwise.
	 *
	 * @param b
	 */
	public void setReference(boolean b) {
		this.isReference = b;
	}

	/**
	 * Return <code>true</code> if value is a piece of java code.
	 * <code>false</code> otherwise.
	 */
	public boolean isJava()
	{
		return isJava;
	}

	/**
	 * Set java flag. <code>true</code> if value is a piece of java
	 * code. <code>false</code> otherwise.
	 *
	 * @param isJava
	 */
	public void setJava(boolean isJava)
	{
		this.isJava = isJava;
	}

	/**
	 * Get string representation if a field value.
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Set string representation of a value.
	 *
	 * @param value
	 *            string representation of a value
	 */
	public void setValue(String value) {
		this.value = value;
	}

	public String getOrigValue() {
		return origValue;
	}

	/**
	 * Set status status of value.<br>
	 * <code>true</code> if required to precompile value, <code>false</code>
	 * otherwise.
	 *
	 * @param check
	 *            <code>true</code> if required to precompile value,
	 *            <code>false</code> otherwise
	 */
	public void setCheck(boolean check) {
		this.check = check;
	}

	/**
	 * Return <code>true</code> if value need to be checked by precompilation.
	 */
	public boolean isCheck() {
		return this.check;
	}

	public void setFieldName(String name) {
		this.fieldName = name;
	}

	public String getFieldName() {
		return this.fieldName;
	}

	public int nextIndex() {
		return this.index++;
	}

	public void addParameter(RefParameter p) {
		this.parameters.add(p);
	}

	public List<RefParameter> getParameters() {
		return parameters;
	}

	public Value clone() {
	    return new Value(origValue, value);
	}

	@Override
	public String toString(){
		return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).
				append("fieldName", fieldName).
				append("value", value).
				append("origValue", origValue).
				append("isReference", isReference).
				append("check", check).
				append("index", index).
				append("parameters", parameters).
				toString();
	}

}