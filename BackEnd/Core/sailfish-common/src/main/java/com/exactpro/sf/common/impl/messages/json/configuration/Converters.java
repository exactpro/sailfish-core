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

import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.common.util.SingleKeyHashMap;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.Converter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

/**
 * Java class for different converters for different classes in JSON/YAML formats.
 */
public final class Converters {

    /**
     * <p>Java class for convert {@link Map}<{@link String}, {@link JsonAttribute}> to {@link List} of {@link JsonAttribute}
     *
     * <p>Help to make following JSON/YAML structure to objects collection, and set names to objects
     * <pre>
     *     {
     *         ...
     *         "attributes":{
     *            "name1":{...},
     *            "name2":{...},
     *            ...
     *         }
     *         ...
     *     }
     * </pre>
     */
    public static class AttributesDeserializeConverter implements Converter<Map<String, JsonAttribute>, List<JsonAttribute>> {

        @Override
        public List<JsonAttribute> convert(Map<String, JsonAttribute> value) {
            List<JsonAttribute> list = new ArrayList<>(value.size());
            for (Map.Entry<String, JsonAttribute> pair : value.entrySet()) {
                if (StringUtils.isEmpty(pair.getKey())) {
                    throw new EPSCommonException("Empty name for attribute/value");
                }
                pair.getValue().setName(pair.getKey());
                list.add(pair.getValue());
            }
            return list;
        }

        @Override
        public JavaType getInputType(TypeFactory typeFactory) {
            return typeFactory.constructMapType(SingleKeyHashMap.class, String.class, JsonAttribute.class);
        }

        @Override
        public JavaType getOutputType(TypeFactory typeFactory) {
            return typeFactory.constructCollectionLikeType(LinkedList.class, JsonAttribute.class);
        }
    }

    /**
     * <p>Java class for convert {@link Map}<{@link String}, {@link JsonField}> to {@link List} of {@link JsonField}
     *
     * <p>Help to make following JSON/YAML structure to objects collection, and set names to objects
     * <pre>
     *     {
     *         ...
     *         "fields":{
     *            "name1":{...},
     *            "name2":{...},
     *            ...
     *         }
     *         ...
     *     }
     * </pre>
     */
    public static class FieldsDeserializeConverter implements Converter<Map<String, JsonField>, List<JsonField>> {

        @Override
        public List<JsonField> convert(Map<String, JsonField> value) {
            List<JsonField> linkedList = new ArrayList<>(value.size());
            for (Map.Entry<String, JsonField> pair : value.entrySet()) {
                if (pair.getKey().length() < 1)
                    throw new EPSCommonException("Empty name for field");
                pair.getValue().setName(pair.getKey());
                linkedList.add(pair.getValue());
            }
            return linkedList;
        }

        @Override
        public JavaType getInputType(TypeFactory typeFactory) {
            return typeFactory.constructMapType(SingleKeyHashMap.class, String.class, JsonField.class);
        }

        @Override
        public JavaType getOutputType(TypeFactory typeFactory) {
            return typeFactory.constructCollectionLikeType(LinkedList.class, JsonField.class);
        }
    }

    /**
     * <p>Java class for convert {@link Map}<{@link String}, {@link JsonMessage}> to {@link List} of {@link JsonMessage}
     *
     * <p>Help to make following JSON/YAML structure to objects collection, and set names to objects
     * <pre>
     *     {
     *         ...
     *         "messages":{
     *            "name1":{...},
     *            "name2":{...},
     *            ...
     *         }
     *         ...
     *     }
     * </pre>
     */
    public static class MessagesDeserializeConverter implements Converter<Map<String, JsonMessage>, List<JsonMessage>> {

        @Override
        public List<JsonMessage> convert(Map<String, JsonMessage> value) {
            List<JsonMessage> linkedList = new ArrayList<>(value.size());
            for (Map.Entry<String, JsonMessage> pair : value.entrySet()) {
                if (pair.getKey().length() < 1)
                    throw new EPSCommonException("Empty name for message");
                pair.getValue().setName(pair.getKey());
                linkedList.add(pair.getValue());
            }
            return linkedList;
        }

        @Override
        public JavaType getInputType(TypeFactory typeFactory) {
            return typeFactory.constructMapType(SingleKeyHashMap.class, String.class, JsonMessage.class);
        }

        @Override
        public JavaType getOutputType(TypeFactory typeFactory) {
            return typeFactory.constructCollectionLikeType(LinkedList.class, JsonMessage.class);
        }
    }


    /**
     * <p>Java class for convert {@link List} of {@link JsonAttribute} to {@link Map}<{@link String}, {@link JsonAttribute}>.
     *
     * <p>Help to make collection of the object to following JSON/YAML structure.
     * <pre>
     *     {
     *         ...
     *         "attributes":{
     *            "name1":{...},
     *            "name2":{...},
     *            ...
     *         }
     *         ...
     *     }
     * </pre>
     * <p>
     * or
     *
     * <pre>
     *     {
     *         ...
     *         "values":{
     *             "name1":{...},
     *             "name2":{...},
     *             ...
     *         }
     *         ...
     *     }
     * </pre>
     */
    public static class AttributesSerializeConverter implements Converter<List<JsonAttribute>, Map<String, JsonAttribute>> {

        @Override
        public Map<String, JsonAttribute> convert(List<JsonAttribute> value) {
            Map<String, JsonAttribute> map = new LinkedHashMap<>();
            for (JsonAttribute attribute : value) {
                map.put(attribute.getName(), attribute);
            }
            return map;
        }

        @Override
        public JavaType getInputType(TypeFactory typeFactory) {
            return typeFactory.constructCollectionLikeType(List.class, JsonAttribute.class);
        }

        @Override
        public JavaType getOutputType(TypeFactory typeFactory) {
            return typeFactory.constructMapType(HashMap.class, String.class, JsonAttribute.class);
        }
    }

    /**
     * <p>Java class for convert {@link List} of {@link JsonField} to {@link Map}<{@link String}, {@link JsonField}>.
     *
     * <p>Help to make collection of the object to following JSON/YAML structure.
     * <pre>
     *     {
     *         ...
     *         "fields":{
     *            "name1":{...},
     *            "name2":{...},
     *            ...
     *         }
     *         ...
     *     }
     * </pre>
     */
    public static class FieldsSerializeConverter implements Converter<List<JsonField>, Map<String, JsonField>> {

        @Override
        public Map<String, JsonField> convert(List<JsonField> value) {
            Map<String, JsonField> map = new LinkedHashMap<>();
            for (JsonField field : value) {
                map.put(field.getName(), field);
            }
            return map;
        }

        @Override
        public JavaType getInputType(TypeFactory typeFactory) {
            return typeFactory.constructCollectionLikeType(List.class, JsonField.class);
        }

        @Override
        public JavaType getOutputType(TypeFactory typeFactory) {
            return typeFactory.constructMapType(HashMap.class, String.class, JsonField.class);
        }
    }

    /**
     * <p>Java class for convert {@link List} of {@link JsonMessage} to {@link Map}<{@link String}, {@link JsonMessage}>.
     *
     * <p>Help to make collection of the object to following JSON/YAML structure.
     * <pre>
     *     {
     *         ...
     *         "messages":{
     *            "name1":{...},
     *            "name2":{...},
     *            ...
     *         }
     *         ...
     *     }
     * </pre>
     */
    public static class MessagesSerializeConverter implements Converter<List<JsonMessage>, Map<String, JsonMessage>> {

        @Override
        public Map<String, JsonMessage> convert(List<JsonMessage> value) {
            Map<String, JsonMessage> map = new LinkedHashMap<>();
            for (JsonMessage message : value) {
                map.put(message.getName(), message);
            }
            return map;
        }

        @Override
        public JavaType getInputType(TypeFactory typeFactory) {
            return typeFactory.constructCollectionLikeType(List.class, JsonMessage.class);
        }

        @Override
        public JavaType getOutputType(TypeFactory typeFactory) {
            return typeFactory.constructMapType(HashMap.class, String.class, JsonMessage.class);
        }
    }

    /**
     * <p>Java class for convert {@link String} to {@link com.exactpro.sf.common.impl.messages.xml.configuration.JavaType}.
     */
    public static class JavaTypeDeserializeConverter implements Converter<String, com.exactpro.sf.common.impl.messages.xml.configuration.JavaType> {

        @Override
        public com.exactpro.sf.common.impl.messages.xml.configuration.JavaType convert(String value) {
            return com.exactpro.sf.common.impl.messages.xml.configuration.JavaType.fromValue(value);
        }

        @Override
        public JavaType getInputType(TypeFactory typeFactory) {
            return typeFactory.constructType(String.class);
        }

        @Override
        public JavaType getOutputType(TypeFactory typeFactory) {
            return typeFactory.constructType(com.exactpro.sf.common.impl.messages.xml.configuration.JavaType.class);
        }
    }

    /**
     * <p>Java class for convert {@link com.exactpro.sf.common.impl.messages.xml.configuration.JavaType} to {@link String}.
     */
    public static class JavaTypeSerializeConverter implements Converter<com.exactpro.sf.common.impl.messages.xml.configuration.JavaType, String> {

        @Override
        public String convert(com.exactpro.sf.common.impl.messages.xml.configuration.JavaType value) {
            return value.value();
        }

        @Override
        public JavaType getInputType(TypeFactory typeFactory) {
            return typeFactory.constructType(com.exactpro.sf.common.impl.messages.xml.configuration.JavaType.class);
        }

        @Override
        public JavaType getOutputType(TypeFactory typeFactory) {
            return typeFactory.constructType(String.class);
        }
    }

}
