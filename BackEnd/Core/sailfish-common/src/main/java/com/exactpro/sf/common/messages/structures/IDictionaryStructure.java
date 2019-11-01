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

/**
 * Java interface for dictionaries
 */
public interface IDictionaryStructure {

	/**
	 * Get the dictionaries attributes.
	 * @return {@link Map}<{@link String}, {@link IAttributeStructure}>
	 */
    Map<String, IAttributeStructure> getAttributes();

	/**
	 * Get the dictionaries messages.
	 * @return {@link Map}<{@link String}, {@link IMessageStructure}>
	 */
	Map<String, IMessageStructure> getMessages();

	/**
	 * Get the dictionaries fields.
	 * @return {@link Map}<{@link String}, {@link IFieldStructure}>
	 */
	Map<String, IFieldStructure> getFields();

	/**
	 * Get the dictionaries namespace.
	 * @return {@link String}
	 */
	String getNamespace();

	/**
	 * Get the dictionaries description.
	 * @return {@link String}
	 */
	String getDescription();
}