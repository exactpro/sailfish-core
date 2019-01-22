/******************************************************************************
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
package com.exactpro.sf.scriptrunner.impl.jsonreport;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class JsonInstantSerializer extends JsonSerializer<Instant> {
    private static final String JSON_DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSX";

    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern(JSON_DATE_PATTERN).withZone(ZoneOffset.UTC);

    public JsonInstantSerializer() {
        super();
    }

    @Override
    public void serialize(Instant value, JsonGenerator generator, SerializerProvider provider) throws IOException {
        generator.writeString(formatter.format(value));
    }
}
