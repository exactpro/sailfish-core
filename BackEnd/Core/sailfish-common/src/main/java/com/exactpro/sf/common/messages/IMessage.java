/*
 * Copyright 2009-2024 Exactpro (Exactpro Systems Limited)
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
 */
package com.exactpro.sf.common.messages;

import org.jetbrains.annotations.Nullable;

import java.util.Set;


/**
 * @author Max
 *
 */
public interface IMessage
{
	/**
	 * @return name of the message. It should be unique inside namespace
	 */
	String getName();

	/**
	 * @return namespace of the message. The uniqueness of message is defined by values
	 * of its name and namespace
	 */
	String getNamespace();

	MsgMetaData getMetaData();

	/**
	 * Adds field and value to this message. Null values aren't added
	 * @param name - name of the field.
	 * @param value - value to be linked with the name.
	 */
	void addField(String name, @Nullable Object value);

	Object removeField(String name);

	<T> T getField(String name);

	FieldMetaData getFieldMetaData(String name);

	/**
	 * Checks if field is present in message and not equal to null. {@link  IMessage#hasField} can be used to check the field is set.
	 * @param name - field name.
	 * @return whenever field is present in this message and it is not null.
	 */
	boolean isFieldSet(String name);

	/**
	 * Checks if field is set in message.
	 * @param name - field name.
	 * @return whenever field is set in this message.
	 */
	boolean hasField(String name);

	Set<String> getFieldNames();

    int getFieldCount();

	IFieldInfo getFieldInfo(String name);

	IMessage cloneMessage();

	boolean compare(IMessage message);
}
