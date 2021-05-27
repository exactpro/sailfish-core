/******************************************************************************
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
package com.exactpro.sf.services.itch

import com.exactpro.sf.configuration.dictionary.DictionaryValidationError
import com.exactpro.sf.configuration.dictionary.impl.SOUPDictionaryValidator
import com.exactpro.sf.util.AbstractTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
class TestSOUPDictionaryValidator : AbstractTest() {
    @Test
    fun test() {
        val dictionary = serviceContext.dictionaryManager.createMessageDictionary("cfg/dictionaries/soup_dictionary_validator_test.xml")
        val errors = SOUPDictionaryValidator().validate(dictionary, true, null)

        assertEquals(7, errors.size)
        assertTrue(errors.containsError("UnknownRoute", "Unknown route: Unknown"))
        assertTrue(errors.containsError("OutgoingTypeC", "Collision by type (C) and route (Outgoing) with following messages: TypeC"))
        assertTrue(errors.containsError("TypeC", "Collision by type (C) and route (Incoming/Outgoing) with following messages: OutgoingTypeC"))
        assertTrue(errors.containsError("IncomingTypeA1", "Collision by type (A) and route (Incoming) with following messages: IncomingTypeA2"))
        assertTrue(errors.containsError("IncomingTypeA2", "Collision by type (A) and route (Incoming) with following messages: IncomingTypeA1"))
        assertTrue(errors.containsError("TypeB1", "Collision by type (B) and route (Incoming/Outgoing) with following messages: TypeB2"))
        assertTrue(errors.containsError("TypeB2", "Collision by type (B) and route (Incoming/Outgoing) with following messages: TypeB1"))
    }

    private fun List<DictionaryValidationError>.containsError(message: String, error: String) = find { it.message == message && it.error == error } != null
}