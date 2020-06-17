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
package com.exactpro.sf.common.messages

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonToken.START_OBJECT
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.jsontype.TypeSerializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer

class MetadataSerializer : StdSerializer<IMetadata>(IMetadata::class.java) {
    override fun serialize(value: IMetadata, gen: JsonGenerator, provider: SerializerProvider) {
        gen.writeStartObject()
        serialize(value, gen)
        gen.writeEndObject()
    }

    override fun serializeWithType(value: IMetadata, gen: JsonGenerator, serializers: SerializerProvider, typeSer: TypeSerializer) {
        val typeId = typeSer.typeId(value, START_OBJECT)
        typeSer.writeTypePrefix(gen, typeId)
        serialize(value, gen)
        typeSer.writeTypeSuffix(gen, typeId)
    }

    private fun serialize(value: IMetadata, gen: JsonGenerator) {
        MetadataProperty.values().forEach { (propertyName, _) ->
            if (value.contains(propertyName)) {
                gen.writeObjectField(propertyName, value[propertyName])
            }
        }
    }
}