/*******************************************************************************
 * Copyright 2009-2019 Exactpro (Exactpro Systems Limited)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 ******************************************************************************/

package com.exactpro.sf.aml.iomatrix;

import com.exactpro.sf.common.util.EPSCommonException;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.fasterxml.jackson.core.JsonGenerator.Feature.STRICT_DUPLICATE_DETECTION;

public class JSONMatrixParser {

    private static final ObjectReader JSON_READER;
    private static final ObjectReader YAML_READER;

    static {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(CustomValue.class, new CustomValueDeserializer());
        module.addKeyDeserializer(KeyValue.class, new CustomValueKeyDeserializer());
        ObjectMapper jsonMapper = new ObjectMapper().registerModule(module).enable(STRICT_DUPLICATE_DETECTION);
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory()).registerModule(module).enable(STRICT_DUPLICATE_DETECTION);
        JSON_READER = jsonMapper.readerFor(CustomValue.class);
        YAML_READER = yamlMapper.readerFor(CustomValue.class);
    }
    public static CustomValue readValue(File file, MatrixFileTypes type) throws IOException {
        switch (type) {
            case JSON:
                return JSON_READER.readValue(file);
            case YAML:
                return YAML_READER.readValue(file);
            default:
                throw new EPSCommonException("Unsupported matrix type " + type);
        }
    }
}

final class KeyValue extends CustomValue {

    private final String key;


    public final String getKey() {
        return this.key;
    }

    public KeyValue(int line, int column, String key) {
        super(line, column);
        this.key = key;
    }
}

final class ObjectValue extends CustomValue {

    public ObjectValue(int line, int column, Map<KeyValue, CustomValue> objectValue) {
        super(line, column);
        this.setObjectValue(objectValue);
    }
}

final class SimpleValue extends CustomValue {

    public SimpleValue(int line, int column, Object simpleValue) {
        super(line, column);
        this.setSimpleValue(simpleValue);
    }
}

final class ArrayValue extends CustomValue {

    public ArrayValue(int line, int column, List<CustomValue> arrayValue) {
        super(line, column);
        this.setArrayValue(arrayValue);
    }
}

final class NullValue extends CustomValue {
    public NullValue(int line, int column) {
        super(line, column);
    }
}

final class CustomValueKeyDeserializer extends KeyDeserializer {
    @Override
    public Object deserializeKey(String key, DeserializationContext ctxt) throws IOException {
        JsonLocation tokenLocation = ctxt.getParser().getCurrentLocation();
        return new KeyValue(tokenLocation.getLineNr(), tokenLocation.getColumnNr(), key);
    }
}

final class CustomValueDeserializer extends StdDeserializer<CustomValue> {

    private static final TypeReference<Map<KeyValue, CustomValue>> TYPE_REF_OBJECT = new TypeReference<Map<KeyValue, CustomValue>>() {};
    private static final TypeReference<List<CustomValue>> TYPE_REF_ARRAY = new TypeReference<List<CustomValue>>() {};

    protected CustomValueDeserializer() {
        super(CustomValue.class);
    }

    @Override
    public CustomValue deserialize(JsonParser p, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        int lineNr = p.getCurrentLocation().getLineNr();
        int columnNr = p.getCurrentLocation().getColumnNr();
        switch (p.currentToken()) {
            case START_OBJECT :
                Map<KeyValue, CustomValue> maps = p.readValueAs(new TypeReference<Map<KeyValue, CustomValue>>() {});
                return new ObjectValue(lineNr, columnNr, maps);
            case START_ARRAY :
                List<CustomValue> customValues = p.readValueAs(new TypeReference<List<CustomValue>>() {});
                return new ArrayValue(lineNr, columnNr, customValues);
            default :
                Object value = p.readValueAs(Object.class);
                return new SimpleValue(lineNr, columnNr, value);
        }
    }

    @Override
    public CustomValue getNullValue(DeserializationContext ctxt){
        JsonLocation currentLocation = ctxt.getParser().getCurrentLocation();
        int lineNr = currentLocation.getLineNr();
        int column = currentLocation.getColumnNr();
        return new NullValue(lineNr, column);
    }
}
