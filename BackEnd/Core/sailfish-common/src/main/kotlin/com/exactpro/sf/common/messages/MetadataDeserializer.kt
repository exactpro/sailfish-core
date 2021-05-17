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
package com.exactpro.sf.common.messages

import com.exactpro.sf.common.messages.impl.Metadata
import com.exactpro.sf.common.services.ServiceInfo
import com.exactpro.sf.common.services.ServiceName
import com.exactpro.sf.configuration.suri.SailfishURI
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping.NON_FINAL
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import java.util.Date

open class MetadataDeserializer : StdDeserializer<IMetadata>(IMetadata::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): IMetadata {
        val node = p.readValueAsTree<JsonNode>().unwrapTyped()
        val metadata = Metadata()

        check(node.isObject) { "node should be an object" }

        MetadataProperty.values().forEach { (propertyName, propertyClass) ->
            try {
                node[propertyName]?.parse(propertyClass)?.run { metadata[propertyName] = this }
            } catch (e: Exception) {
                throw Exception("Failed to decode property: $propertyName", e)
            }
        }

        return metadata
    }

    companion object {
        private val PTV = BasicPolymorphicTypeValidator
            .builder()
            .allowIfBaseType(Long::class.java)
            .allowIfBaseType(Date::class.java)
            .allowIfBaseType(String::class.java)
            .allowIfBaseType(Boolean::class.java)
            .allowIfBaseType(ByteArray::class.java)
            .allowIfBaseType(ServiceInfo::class.java)
            .allowIfBaseType(ServiceName::class.java)
            .allowIfBaseType(SailfishURI::class.java)
            .build()
        private val TYPED_MAPPER = ObjectMapper().activateDefaultTyping(PTV, NON_FINAL)
        private val UNTYPED_MAPPER = ObjectMapper()

        private fun JsonNode.unwrapTyped(): JsonNode {
            if (isArray) {
                check(size() == 2) { "unsupported array length" }
                check(this[0].isTextual) { "first element should be a class name" }
                return this[1]
            }

            return this
        }

        fun <T> JsonNode.parse(clazz: Class<T>): T = runCatching {
            TYPED_MAPPER.convertValue(this, clazz)
        }.recoverCatching {
            UNTYPED_MAPPER.convertValue(unwrapTyped(), clazz)
        }.getOrThrow()
    }
}