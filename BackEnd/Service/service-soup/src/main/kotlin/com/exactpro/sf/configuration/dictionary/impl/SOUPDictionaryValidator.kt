/*******************************************************************************
 * Copyright 2009-2021 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.configuration.dictionary.impl

import com.exactpro.sf.common.messages.structures.IDictionaryStructure
import com.exactpro.sf.common.messages.structures.IMessageStructure
import com.exactpro.sf.common.messages.structures.StructureUtils.getAttributeValue
import com.exactpro.sf.configuration.dictionary.DictionaryValidationError
import com.exactpro.sf.configuration.dictionary.DictionaryValidationErrorLevel.MESSAGE
import com.exactpro.sf.configuration.dictionary.DictionaryValidationErrorType.ERR_ATTRIBUTES
import com.exactpro.sf.configuration.dictionary.ValidationHelper
import com.exactpro.sf.configuration.dictionary.interfaces.IDictionaryValidator
import com.exactpro.sf.extensions.messageType
import com.exactpro.sf.externalapi.codec.IExternalCodecContext.Role
import com.exactpro.sf.externalapi.codec.IExternalCodecContext.Role.RECEIVER
import com.exactpro.sf.externalapi.codec.IExternalCodecContext.Role.SENDER
import com.exactpro.sf.services.itch.soup.SOUPMessageHelper.PACKET_HEADER_MESSAGE
import com.exactpro.sf.services.itch.soup.SOUPMessageHelper.ROUTE_ATTRIBUTE
import com.exactpro.sf.services.itch.soup.SOUPMessageHelper.ROUTE_ATTRIBUTE_INCOMING
import com.exactpro.sf.services.itch.soup.SOUPMessageHelper.ROUTE_ATTRIBUTE_OUTGOING

open class SOUPDictionaryValidator constructor(
    private val checkPacketHeader: Boolean,
    parent: IDictionaryValidator? = null
) : AbstractDictionaryValidator(parent) {
    constructor() : this(true)
    constructor(dictionaryValidator: IDictionaryValidator) : this(true, dictionaryValidator)

    override fun validate(dictionary: IDictionaryStructure, full: Boolean, fieldsOnly: Boolean?): List<DictionaryValidationError> {
        val errors = super.validate(dictionary, full, fieldsOnly)
        if (checkPacketHeader) {
            ValidationHelper.checkRequiredMessageExistence(errors, dictionary, PACKET_HEADER_MESSAGE)
        }
        return errors
    }

    override fun validate(dictionary: IDictionaryStructure, message: IMessageStructure, full: Boolean): MutableList<DictionaryValidationError> {
        val errors = super.validate(dictionary, message, full)
        val name = message.name
        val type = message.messageType ?: return errors
        val route = message.route ?: "$ROUTE_ATTRIBUTE_INCOMING/$ROUTE_ATTRIBUTE_OUTGOING"
        val roles = message.roles

        if (roles.isEmpty()) {
            return errors.apply {
                add(DictionaryValidationError(
                    name,
                    null,
                    "Unknown route: $route",
                    MESSAGE,
                    ERR_ATTRIBUTES
                ))
            }
        }

        val collisions = dictionary.messages
            .values
            .asSequence()
            .filter { it.name != name && it.messageType == type && it.roles.any(roles::contains) }
            .joinToString { it.name }

        if (collisions.isNotEmpty()) {
            errors += DictionaryValidationError(
                name,
                null,
                "Collision by type ($type) and route ($route) with following messages: $collisions",
                MESSAGE,
                ERR_ATTRIBUTES
            )
        }

        return errors
    }

    private val IMessageStructure.roles: Set<Role>
        get() = when {
            route.equals(ROUTE_ATTRIBUTE_INCOMING, true) -> setOf(RECEIVER)
            route.equals(ROUTE_ATTRIBUTE_OUTGOING, true) -> setOf(SENDER)
            route == null -> setOf(SENDER, RECEIVER)
            else -> setOf()
        }

    private val IMessageStructure.route: String?
        get() = getAttributeValue<String>(this, ROUTE_ATTRIBUTE)
}