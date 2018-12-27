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
package com.exactpro.sf.storage.util;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

import com.exactpro.sf.common.messages.IHumanMessage;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.MsgMetaData;
import com.exactpro.sf.common.messages.structures.IAttributeStructure;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.IDictionaryManager;
import com.exactpro.sf.util.DateTimeUtility;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.collect.Iterables;

public class JsonMessageConverter {

    public static final String JSON_MESSAGE = "message";
    public static final String JSON_MESSAGE_DICTIONARY_URI = "dictionaryURI";
    public static final String JSON_MESSAGE_PROTOCOL = "protocol";
    public static final String JSON_MESSAGE_DIRTY = "dirty";
    public static final String JSON_MESSAGE_NAMESPACE = "namespace";
    public static final String JSON_MESSAGE_NAME = "name";
    public static final String JSON_MESSAGE_TIMESTAMP = "timestamp";
    public static final String JSON_MESSAGE_ID = "id";
    public static final String JSON_MESSAGE_RR = "rejectReason";

    private static final Map<Class<?>, DateTimeFormatter> CLASS_TO_FORMATTER = new HashMap<>();

    static {
        CLASS_TO_FORMATTER.put(LocalDate.class, DateTimeFormatter.ISO_DATE);
        CLASS_TO_FORMATTER.put(LocalTime.class, DateTimeFormatter.ISO_TIME);
        CLASS_TO_FORMATTER.put(LocalDateTime.class, DateTimeFormatter.ISO_DATE_TIME);
    }

    public static String formatTemporal(TemporalAccessor accessor) {
        ZonedDateTime zonedDateTime = DateTimeUtility.toZonedDateTime(accessor);
        return CLASS_TO_FORMATTER.get(accessor.getClass()).format(zonedDateTime);
    }

    public static String toJson(IMessage message) {
        return toJson(message, true);
    }

    public static String toJson(IMessage message, boolean compact) {
        return toJson(message, null, compact);
    }

    public static String toJson(IMessage message, IDictionaryStructure dictionary) {
        return toJson(message, dictionary, true);
    }

    public static String toJson(IMessage message, IDictionaryStructure dictionary, boolean compact) {
        IMessageStructure structure = dictionary != null ? dictionary.getMessageStructure(message.getName()) : null;
        JsonFactory factory = new JsonFactory();

        try(StringWriter writer = new StringWriter(4096);
                JsonGenerator generator = factory.createGenerator(writer)) {
            MsgMetaData metaData = message.getMetaData();

            generator.writeStartObject();
            generator.writeNumberField(JSON_MESSAGE_ID, metaData.getId());
            generator.writeNumberField(JSON_MESSAGE_TIMESTAMP, metaData.getMsgTimestamp().getTime());
            generator.writeStringField(JSON_MESSAGE_NAME, metaData.getMsgName());
            generator.writeStringField(JSON_MESSAGE_NAMESPACE, metaData.getMsgNamespace());
            generator.writeStringField(JSON_MESSAGE_DICTIONARY_URI, Objects.toString(metaData.getDictionaryURI(), StringUtils.EMPTY));
            generator.writeStringField(JSON_MESSAGE_PROTOCOL, StringUtils.defaultString(metaData.getProtocol()));
            generator.writeStringField(JSON_MESSAGE_DIRTY, Objects.toString(metaData.isDirty(), StringUtils.EMPTY));
            generator.writeStringField(JSON_MESSAGE_RR, metaData.getRejectReason());
            generator.writeFieldName(JSON_MESSAGE);

            if(compact) {
                convertCompact(message, structure, generator);
            } else {
                convert(message, structure, generator);
            }

            generator.writeEndObject();
            generator.flush();

            return writer.toString();
        } catch(IOException e) {
            throw new EPSCommonException(e);
        }
    }

    public static IMessage fromJson(String json) {
        return fromJson(json, null, true);
    }
    
    public static IMessage fromJson(String json, boolean compact) {
        return fromJson(json, null, compact);
    }
    
    /**
     * Parse JSON to IMessage
     * @param json - JSON
     * @param dictionaryManager
     * @param compact - if true use light strategy otherwise use full strategy for parsing
     * @return
     */
    public static IMessage fromJson(String json, IDictionaryManager dictionaryManager, boolean compact) {
        return new JsonIMessageDecoder(dictionaryManager).decode(json, compact);
    }


    /**
     * Parse JSON to Human readable message
     * @param json - Sailfish JSON format
     * @param dictionaryManager
     * @param compact - if true use light strategy otherwise use full strategy for parsing
     * @return String
     */
    public static IHumanMessage fromJsonToHuman(String json, IDictionaryManager dictionaryManager, boolean compact) {
        return new JsonHumanDecoder(dictionaryManager)
                .decode(json, compact);
    }

    private static void convertCompact(IMessage message, IFieldStructure structure, JsonGenerator generator) throws IOException {
        Set<String> fieldNames = getFieldNamesRetain(message, structure);
        generator.writeStartObject();

        for(String fieldName : fieldNames) {
            generator.writeFieldName(fieldName);
            IFieldStructure subStructure = structure != null && structure.isComplex() ? structure.getField(fieldName) : null;
            handleCompactValue(message.getField(fieldName), subStructure, generator);
        }

        generator.writeEndObject();
    }

    private static void convert(IMessage message, IFieldStructure structure, JsonGenerator generator) throws IOException {
        Set<String> fieldNames = getFieldNames(message, structure);

        generator.writeStartObject();

        for(String fieldName : fieldNames) {
            IFieldStructure subStructure = structure != null && structure.isComplex() ? structure.getField(fieldName) : null;
            Object value = message.getField(fieldName);

            if(value == null) {
                continue;
            }

            generator.writeFieldName(fieldName);
            generator.writeStartObject();
            generator.writeStringField("type", getValueClass(value, subStructure));
            handleValue(value, subStructure, generator);
            generator.writeEndObject();
        }

        generator.writeEndObject();
    }

    private static void handleCompactValue(Object value, IFieldStructure structure, JsonGenerator generator) throws IOException {
        if(value instanceof List<?>) {
            generator.writeStartArray();

            for(Object element : (List<?>)value) {
                handleCompactValue(element, structure, generator);
            }

            generator.writeEndArray();
        } else if(value instanceof IMessage) {
            convertCompact((IMessage)value, structure, generator);
        } else if(value instanceof LocalDate || value instanceof LocalTime || value instanceof LocalDateTime) {
            generator.writeString(formatTemporal((TemporalAccessor)value));
        } else {
            generator.writeObject(Objects.toString(value, null));
        }
    }

    private static void handleValue(Object value, IFieldStructure structure, JsonGenerator generator) throws IOException {
        generator.writeFieldName("value");

        if(value == null) {
            generator.writeNull();
            return;
        }

        if(value instanceof List<?>) {
            generator.writeStartArray();

            for(Object element : (List<?>)value) {
                if(element == null) {
                    continue;
                }

                generator.writeStartObject();
                generator.writeStringField("type", getValueClass(element, structure));
                handleValue(element, structure, generator);
                generator.writeEndObject();
            }

            generator.writeEndArray();
        } else if(value instanceof IMessage) {
            convert((IMessage)value, structure, generator);
        } else if(value instanceof LocalDate || value instanceof LocalTime || value instanceof LocalDateTime) {
            generator.writeString(formatTemporal((TemporalAccessor)value));
        } else if(value instanceof Character) {
            generator.writeString(value.toString());
        } else {
            generator.writeObject(value);
        }

        if(structure != null && structure.isEnum()) {
            String alias = null;

            for(IAttributeStructure attribute : structure.getValues().values()) {
                if(attribute.getCastValue().equals(value)) {
                    alias = attribute.getName();
                    break;
                }
            }

            if(alias != null) {
                generator.writeStringField("alias", alias);
            }
        }
    }

    private static Set<String> getFieldNames(IMessage message, IFieldStructure structure) {
        Set<String> fieldNames = new LinkedHashSet<>();

        if(structure != null && structure.isComplex()) {
            fieldNames.addAll(structure.getFieldNames());
        }

        fieldNames.addAll(message.getFieldNames());

        return fieldNames;
    }

    private static Set<String> getFieldNamesRetain(IMessage message, IFieldStructure structure) {
        if(structure != null && structure.isComplex()) {
            Set<String> fieldNames = new LinkedHashSet<>(structure.getFieldNames());
            fieldNames.retainAll(message.getFieldNames());
            return fieldNames;
        }
        return message.getFieldNames();
    }

    private static String getValueClass(Object value, IFieldStructure structure) {
        String clazz = null;

        if(value instanceof List<?>) {
            value = Iterables.get((Iterable<?>)value, 0, null);

            if(value != null || structure != null) {
                clazz = List.class.getSimpleName() + "<" + getValueClass(value, structure) + ">";
            } else {
                clazz = List.class.getSimpleName() + "<String>";
            }
        } else if(value instanceof IMessage) {
            clazz = IMessage.class.getSimpleName();
        } else {
            if(structure != null) {
                if(structure.isComplex()) {
                    clazz = IMessage.class.getSimpleName();
                } else {
                    clazz = StringUtils.substringAfterLast(structure.getJavaType().value(), ".");
                }
            } else {
                clazz = ClassUtils.getSimpleName(value, null);
            }
        }

        return clazz;
    }
}
