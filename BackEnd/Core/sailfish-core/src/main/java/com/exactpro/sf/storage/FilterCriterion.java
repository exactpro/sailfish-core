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
package com.exactpro.sf.storage;

public class FilterCriterion 
{
	public enum Operation
	{
		EQUALS,
		NOT_EQUALS,
		GREATER,
		LESSER,
		LIKE
	}
	
	private final String name;
	private final String value;
	private final Operation oper;
	
	public FilterCriterion(String name, String value, Operation oper) 
	{
		this.name = name;
		this.value = value;
		this.oper = oper;
	}
	
	
	public String getName() {
		return name;
	}
	

	public String getValue() {
		return value;
	}
	

	public Operation getOper() {
		return oper;
	}

}
