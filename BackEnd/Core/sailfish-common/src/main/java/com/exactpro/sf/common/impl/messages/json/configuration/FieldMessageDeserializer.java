/*******************************************************************************
 * Copyright 2009-2019 Exactpro (Exactpro Systems Limited)
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

package com.exactpro.sf.common.impl.messages.json.configuration;

import java.io.IOException;

import com.exactpro.sf.common.util.EPSCommonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

public class FieldMessageDeserializer extends JsonDeserializer<JsonField> {

    private static final ObjectReader INNER_READER = new ObjectMapper().enable(Feature.STRICT_DUPLICATE_DETECTION).readerFor(JsonField.class);

    @Override
    public JsonField deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        JsonNode obj = p.readValueAsTree();

        if (obj == null) {
            return null;
        }

        Object fields = obj.get("fields");
        if (fields != null) {
            return p.getCodec().treeToValue(obj, JsonMessage.class);
        } else {
            if (p.getCodec() == INNER_READER) {
                throw new EPSCommonException("This deserializer using like root. Please create separate module with addDeserializer().");
            }
            return INNER_READER.readValue(obj);
        }
    }
}
