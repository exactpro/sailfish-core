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
package com.exactpro.sf.common.messages.structures;

import java.util.Map;

import com.exactpro.sf.common.impl.messages.xml.configuration.JavaType;

public interface IFieldStructure {
	/**
	 * Returns {@code true} if, and only if, instance contains fields and doesn't contain values.
	 */
	boolean isComplex();

	/**
     * Returns {@code true} if, and only if, instance doesn't contain fields and contains values.
     */
	boolean isEnum();

	/**
     * Returns {@code true} if, and only if, instance doesn't contain fields and values.
     */
	boolean isSimple();

	/**
     * Returns own name.
     */
	String getName();

	/**
     * Returns own description.
     */
	String getDescription();

	/**
     * Returns fields from own collection.
     */
    Map<String, IFieldStructure> getFields();

	/**
     * Returns attributes from own and inherit collection.
     */
	Map<String, IAttributeStructure> getAttributes();

	/**
     * Returns values from own and inherit collection.
     */
	Map<String, IAttributeStructure> getValues();

	/**
     * Returns own {@link StructureType}.
     */
	StructureType getStructureType();

	/**
     * Returns own on inherit {@link StructureType}.
     */
	JavaType getJavaType();

	/**
     * Returns own is required flag.
     */
	boolean isRequired();

	/**
     * Returns own is collection flag.
     */
	boolean isCollection();

	/**
     * Returns own is service name flag.
     */
	boolean isServiceName();

	/**
     * Returns own or inherit default value.
     */
    <T> T getDefaultValue();

	/**
     * Returns dictionary's namespace.
     */
	String getNamespace();

	/**
     * Returns name of parent field structure.
     */
	String getReferenceName();
}