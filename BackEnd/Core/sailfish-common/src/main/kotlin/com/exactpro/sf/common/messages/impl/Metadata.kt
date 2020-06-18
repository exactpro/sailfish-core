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
package com.exactpro.sf.common.messages.impl

import com.exactpro.sf.common.messages.IMetadata
import com.exactpro.sf.common.messages.MetadataDeserializer
import com.exactpro.sf.common.messages.MetadataSerializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import java.math.BigDecimal
import java.util.Collections
import java.util.Objects

@JsonSerialize(using = MetadataSerializer::class)
@JsonDeserialize(using = MetadataDeserializer::class)
open class Metadata : IMetadata {
    private val map = hashMapOf<String, Any>()
    override val keys: Set<String> = Collections.unmodifiableSet(map.keys)

    override fun get(key: String): Any? = map[key]

    override fun set(key: String, value: Any) {
        check(key.isNotBlank()) { "key cannot be blank" }
        map[key] = value
    }

    override fun contains(key: String): Boolean = map.containsKey(key)

    override fun remove(key: String) {
        map.remove(key)
    }

    override fun clone(): IMetadata = Metadata().also { clone ->
        clone.map += map
    }

    override fun toString(): String {
        return "Metadata$map"
    }

    override fun hashCode(): Int {
        return map.hashCode()
    }

    override fun equals(other: Any?): Boolean = when {
        this === other -> true
        other !is IMetadata -> false
        keys != other.keys -> false
        else -> keys.all { key ->
            val thisValue = this[key]
            val otherValue = other[key]

            when {
                thisValue is BigDecimal && otherValue is BigDecimal -> thisValue.compareTo(otherValue) == 0
                else -> Objects.deepEquals(thisValue, otherValue)
            }
        }
    }
}