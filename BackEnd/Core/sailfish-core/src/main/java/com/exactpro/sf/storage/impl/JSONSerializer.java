/******************************************************************************
 * Copyright 2009-2018 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.storage.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import com.exactpro.sf.storage.ISerializer;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonInclude.Value;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

public class JSONSerializer<T> implements ISerializer<T> {
    protected final ObjectMapper mapper = new ObjectMapper().setPropertyInclusion(Value.construct(Include.NON_NULL, Include.NON_NULL));
    private final ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
    private final Class<T> clazz;

    public JSONSerializer(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public T deserialize(File input) throws Exception {
        try(InputStream stream = new FileInputStream(input)) {
            return deserialize(stream);
        }
    }

    @Override
    public T deserialize(InputStream input) throws Exception {
        return mapper.readValue(input, clazz);
    }

    @Override
    public void serialize(T object, File output) throws Exception {
        try(OutputStream stream = new FileOutputStream(output)) {
            serialize(object, stream);
        }
    }

    @Override
    public void serialize(T object, OutputStream output) throws Exception {
        writer.writeValue(output, object);
    }

    public static <T> JSONSerializer<T> of(Class<T> clazz) {
        return new JSONSerializer<>(clazz);
    }
}
