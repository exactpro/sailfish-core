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

import com.exactpro.sf.common.messages.structures.IMessageStructure
import com.exactpro.sf.common.messages.structures.StructureUtils
import com.exactpro.sf.services.MessageHelper
import com.exactpro.sf.services.MessageHelper.ATTRIBUTE_MESSAGE_TYPE

/**
 * Returns the value that is specified in the [MessageHelper.ATTRIBUTE_MESSAGE_TYPE] attribute for that structure
 * or `null` if structure doesn't have that attribute
 */
val IMessageStructure.messageType: String?
    get() = StructureUtils.getAttributeValue(this, ATTRIBUTE_MESSAGE_TYPE)

/**
 * Returns the value that is specified in the [MessageHelper.ATTRIBUTE_MESSAGE_TYPE] attribute for that structure.
 *
 * If structure doesn't have that attribute throws [IllegalArgumentException]
 * @throws IllegalArgumentException structure doesn't have [MessageHelper.ATTRIBUTE_MESSAGE_TYPE] or it has value of `null`
 */
fun IMessageStructure.requireMessageType(): String =
    requireNotNull(messageType) { "Structure $name doesn't have $ATTRIBUTE_MESSAGE_TYPE attribute" }