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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * This class represent a variable used in code.
 * @author dmitry.guriev
 *
 */
public class Variable {

	private String name;
	private Class<?> type;
	
	public Variable(String name, Class<?> type) {
		this.name = name;
		this.type = type;
	}

	/**
	 * Get variable name.
	 * @return variable name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Get variable type.
	 * @return variable type
	 */
	public Class<?> getType() {
		return type;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null)
			return false;
		if (o instanceof Variable) {
			Variable that = (Variable)o;
			List<Object> arr1 = new ArrayList<Object>();
			List<Object> arr2 = new ArrayList<Object>();
			arr1.add(this.name);
			arr2.add(that.name);
			arr1.add(this.type);
			arr2.add(that.type);
			return arr1.equals(arr2);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return (this.name+this.type).hashCode();
	}
	
	@Override
	public String toString() {
		return new ToStringBuilder(this).
				append("name", name).
				append("type", type).
				toString();
	}
	
}
