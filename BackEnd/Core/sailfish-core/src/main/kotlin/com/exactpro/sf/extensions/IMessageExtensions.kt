/*******************************************************************************
 * Copyright 2009-2020 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.extensions

import com.exactpro.sf.common.messages.IMessage

operator fun IMessage.set(fieldName: String, fieldValue: Any?) = addField(fieldName, fieldValue)
operator fun <T> IMessage.get(fieldName: String): T = getField(fieldName)

/**
 * Checks if field is present in message: null or not null. IMessage has isFieldSet but it only checks if message has not null field
 * fieldName - field name to check
 * @return whenever message has null/not null field in message.
 */
fun IMessage.isFieldPresent(fieldName: String): Boolean = fieldNames.contains(fieldName)