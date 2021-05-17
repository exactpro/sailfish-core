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
package com.exactpro.sf.services.json

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode

interface IJsonNodeWrapper {
    val node: JsonNode
    val isArray: Boolean
        get() = node.isArray

    fun add(node: JsonNode): Unit = throw UnsupportedOperationException()
    operator fun set(fieldName: String, value: JsonNode): Unit = throw UnsupportedOperationException()
}

class ObjectNodeWrapper(override val node: ObjectNode) : IJsonNodeWrapper {
    override fun set(fieldName: String, value: JsonNode) {
        node.set<JsonNode>(fieldName, value)
    }
}

class ArrayNodeWrapper(override val node: ArrayNode) : IJsonNodeWrapper {
    override fun add(node: JsonNode) {
        this.node.add(node)
    }
}