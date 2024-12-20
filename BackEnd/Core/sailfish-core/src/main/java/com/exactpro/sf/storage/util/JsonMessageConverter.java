/*
 * Copyright 2009-2024 Exactpro (Exactpro Systems Limited)
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
 */
package com.exactpro.sf.storage.util;

import com.exactpro.sf.common.messages.IHumanMessage;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMetadata;
import com.exactpro.sf.common.messages.structures.IAttributeStructure;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.comparison.ComparisonUtil;
import com.exactpro.sf.configuration.IDictionaryManager;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.collect.Iterables;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAccessor;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.exactpro.sf.common.messages.MetadataExtensions.getDictionaryUri;
import static com.exactpro.sf.common.messages.MetadataExtensions.getId;
import static com.exactpro.sf.common.messages.MetadataExtensions.getName;
import static com.exactpro.sf.common.messages.MetadataExtensions.getNamespace;
import static com.exactpro.sf.common.messages.MetadataExtensions.getProtocol;
import static com.exactpro.sf.common.messages.MetadataExtensions.getRejectReason;
import static com.exactpro.sf.common.messages.MetadataExtensions.getTimestamp;
import static com.exactpro.sf.common.messages.MetadataExtensions.isAdmin;
import static com.exactpro.sf.common.messages.MetadataExtensions.isDirty;

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
    public static final String JSON_MESSAGE_ADMIN = "admin";

    public static String formatTemporal(TemporalAccessor accessor) {
        return ComparisonUtil.formatTemporal(accessor);
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
        IMessageStructure structure = dictionary != null ? dictionary.getMessages().get(message.getName()) : null;
        JsonFactory factory = new JsonFactory();

        try(StringWriter writer = new StringWriter(4096);
                JsonGenerator generator = factory.createGenerator(writer)) {
            IMetadata metaData = message.getMetaData();

            generator.writeStartObject();
            generator.writeNumberField(JSON_MESSAGE_ID, getId(metaData));
            generator.writeNumberField(JSON_MESSAGE_TIMESTAMP, getTimestamp(metaData).getTime());
            generator.writeStringField(JSON_MESSAGE_NAME, getName(metaData));
            generator.writeStringField(JSON_MESSAGE_NAMESPACE, getNamespace(metaData));
            generator.writeStringField(JSON_MESSAGE_DICTIONARY_URI, Objects.toString(getDictionaryUri(metaData), StringUtils.EMPTY));
            generator.writeStringField(JSON_MESSAGE_PROTOCOL, StringUtils.defaultString(getProtocol(metaData)));
            generator.writeBooleanField(JSON_MESSAGE_DIRTY, isDirty(metaData));
            generator.writeBooleanField(JSON_MESSAGE_ADMIN, isAdmin(metaData));
            generator.writeStringField(JSON_MESSAGE_RR, getRejectReason(metaData));
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
     * @param dictionaryManager - dictionary manager
     * @param compact - if true use light strategy otherwise use full strategy for parsing
     * @return - deserialized {@link IMessage}
     */
    public static IMessage fromJson(String json, IDictionaryManager dictionaryManager, boolean compact) {
        return new JsonIMessageDecoder(dictionaryManager).decode(json, compact);
    }


    /**
     * Parse JSON to Human readable message
     * @param json - Sailfish JSON format
     * @param dictionaryManager - dictionary manager
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
            IFieldStructure subStructure = structure != null && structure.isComplex() ? structure.getFields().get(fieldName) : null;
            handleCompactValue(message.getField(fieldName), subStructure, generator);
        }

        generator.writeEndObject();
    }

    private static void convert(IMessage message, IFieldStructure structure, JsonGenerator generator) throws IOException {
        Set<String> fieldNames = getFieldNames(message, structure);

        generator.writeStartObject();

        for(String fieldName : fieldNames) {
            if (!message.hasField(fieldName)) {
                continue;
            }

            IFieldStructure subStructure = structure != null && structure.isComplex() ? structure.getFields().get(fieldName) : null;
            Object value = message.getField(fieldName);
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
            fieldNames.addAll(structure.getFields().keySet());
        }

        fieldNames.addAll(message.getFieldNames());

        return fieldNames;
    }

    private static Set<String> getFieldNamesRetain(IMessage message, IFieldStructure structure) {
        if(structure != null && structure.isComplex()) {
            Set<String> fieldNames = new LinkedHashSet<>(structure.getFields().keySet());
            fieldNames.retainAll(message.getFieldNames());
            return fieldNames;
        }
        return message.getFieldNames();
    }

    private static String getValueClass(Object value, IFieldStructure structure) {
        if(value instanceof List<?>) {
            value = Iterables.get((Iterable<?>)value, 0, null);

            if(value != null || structure != null) {
                return List.class.getSimpleName() + "<" + getValueClass(value, structure) + ">";
            } else {
                return List.class.getSimpleName() + "<String>";
            }
        } else if(value instanceof IMessage) {
            return IMessage.class.getSimpleName();
        } else {
            if(structure != null) {
                if(structure.isComplex()) {
                    return IMessage.class.getSimpleName();
                } else {
                    return StringUtils.substringAfterLast(structure.getJavaType().value(), ".");
                }
            } else {
                return ClassUtils.getSimpleName(value, null);
            }
        }
    }
}
