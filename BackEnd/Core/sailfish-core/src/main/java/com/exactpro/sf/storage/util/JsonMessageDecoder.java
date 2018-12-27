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

import static com.exactpro.sf.storage.util.JsonMessageConverter.JSON_MESSAGE;
import static com.exactpro.sf.storage.util.JsonMessageConverter.JSON_MESSAGE_DICTIONARY_URI;
import static com.exactpro.sf.storage.util.JsonMessageConverter.JSON_MESSAGE_DIRTY;
import static com.exactpro.sf.storage.util.JsonMessageConverter.JSON_MESSAGE_ID;
import static com.exactpro.sf.storage.util.JsonMessageConverter.JSON_MESSAGE_NAME;
import static com.exactpro.sf.storage.util.JsonMessageConverter.JSON_MESSAGE_NAMESPACE;
import static com.exactpro.sf.storage.util.JsonMessageConverter.JSON_MESSAGE_PROTOCOL;
import static com.exactpro.sf.storage.util.JsonMessageConverter.JSON_MESSAGE_RR;
import static com.exactpro.sf.storage.util.JsonMessageConverter.JSON_MESSAGE_TIMESTAMP;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import com.exactpro.sf.common.impl.messages.xml.configuration.JavaType;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.StructureUtils;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.IDictionaryManager;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.suri.SailfishURIException;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

public abstract class JsonMessageDecoder <T> {

    protected final IDictionaryManager dictionaryManager;

    public JsonMessageDecoder(IDictionaryManager dictionaryManager) {
        this.dictionaryManager = dictionaryManager;
    }
    
    /**
     * Parse JSON to IMessage
     * @param json - JSON
     * @return
     */
    public T decode(String json, boolean compact) {
        try {
            JsonFactory factory = new JsonFactory();
            JsonParser parser = factory.createParser(json);
            
            parser.nextToken();
            return parse(parser, compact);
        } catch (RuntimeException | IOException | SailfishURIException e) {
            throw new EPSCommonException(e);
        }
    }
    
    private T parse(JsonParser parser, boolean compact) throws JsonParseException, IOException, SailfishURIException {
        try {
            checkToken(parser, JsonToken.START_OBJECT, parser.getCurrentToken());
            getValue(parser, JSON_MESSAGE_ID, JavaType.JAVA_LANG_LONG);
            getValue(parser, JSON_MESSAGE_TIMESTAMP, JavaType.JAVA_LANG_LONG);
            String name = getValue(parser, JSON_MESSAGE_NAME, JavaType.JAVA_LANG_STRING);
            String namespace = getValue(parser, JSON_MESSAGE_NAMESPACE, JavaType.JAVA_LANG_STRING);
            String dictionaryURIValue = JsonMessageDecoder.<String>getValue(parser, JSON_MESSAGE_DICTIONARY_URI, JavaType.JAVA_LANG_STRING);
            SailfishURI dictionaryURI = StringUtils.isNotEmpty(dictionaryURIValue)
                    ? SailfishURI.parse(dictionaryURIValue)
                    : null;
            String protocol = getValue(parser, JSON_MESSAGE_PROTOCOL, JavaType.JAVA_LANG_STRING);
            Map<String, String> optionalTokens = new HashMap();

            while(parser.getCurrentName() != JSON_MESSAGE) {
                JsonToken token = parser.getCurrentToken();
                optionalTokens.put(parser.getCurrentName(), parser.getValueAsString());
                parser.nextToken();
            }

            checkFieldName(parser, parser.getCurrentToken(), JSON_MESSAGE);
            parser.nextToken();
            boolean dirty = BooleanUtils.toBoolean(optionalTokens.get(JSON_MESSAGE_DIRTY));
            T message = parse(parser, protocol, dictionaryURI, namespace, name, compact, dirty);
            if (message instanceof IMessage) {
                ((IMessage)message).getMetaData().setRejectReason(optionalTokens.get(JSON_MESSAGE_RR));
                ((IMessage)message).getMetaData().setDirty(dirty);
            }

    //        IMessage message = messageFactory.createMessage(name, namespace);
    //        MsgMetaData metaData = new MsgMetaData(namespace, name, new Date(timestamp));
    //        metaData.setProtocol(protocol);
            //TODO: set new MsgMetaData
            
            return message;
        } catch (RuntimeException e) {
            throw new JsonParseException(e.getMessage(), parser.getCurrentLocation(), e); 
        }
    }
    
    public T parse(JsonParser parser, String protocol, SailfishURI dictionaryURI, String namespace, String name, boolean compact, boolean dirty) throws JsonParseException, IOException {
        IDictionaryStructure dictionaryStructure =
                dictionaryManager != null && dictionaryURI != null
                ? dictionaryManager.getDictionary(dictionaryURI)
                : null;
        IFieldStructure fieldStructure = dictionaryStructure != null ? dictionaryStructure.getMessageStructure(name) : null;
        if (compact) {
            return getMessageCompact(parser, fieldStructure, name, dirty);
        } else {
            return getMessageFull(parser, fieldStructure, name, dirty);
        }
    }
    
    protected abstract T createMessage(String messageName);
    
    protected abstract void handleField(T message, String fieldName, FieldInfo fieldInfo);
    
    private T getMessageCompact(JsonParser parser, IFieldStructure fieldStructure, String messageName, boolean dirty) throws JsonParseException, IOException {
        try {
            checkToken(parser, JsonToken.START_OBJECT, parser.getCurrentToken());
            T message = createMessage(messageName);
            while (JsonToken.END_OBJECT != parser.nextToken()) {
                String fieldName = parser.getCurrentName();
                IFieldStructure subFieldStructure = fieldStructure != null ? fieldStructure.getField(fieldName) : null;
                parser.nextToken();
                FieldInfo fieldInfo = getValueCompact(parser, subFieldStructure, dirty);
                handleField(message, fieldName, fieldInfo);
            }
            return message;
        } catch (JsonParseException e) {
            String message = String.format("Parsing %s failed", messageName);
            throw new JsonParseException(null, message, e);
        }
    }
    
    private T getMessageFull(JsonParser parser, IFieldStructure fieldStructure, String messageName, boolean dirty) throws JsonParseException, IOException {
        try {
            checkToken(parser, JsonToken.START_OBJECT, parser.getCurrentToken());
            T message = createMessage(messageName);
            while (JsonToken.FIELD_NAME == parser.nextToken()) {
                String fieldName = parser.getCurrentName();
                IFieldStructure subFieldStructure = fieldStructure != null ? fieldStructure.getField(fieldName) : null;
                parser.nextToken();
                FieldInfo fieldInfo = getValueFull(parser, subFieldStructure, dirty);
                handleField(message, fieldName, fieldInfo);
            }
            checkToken(parser, JsonToken.END_OBJECT, parser.getCurrentToken());
            return message;
        } catch (JsonParseException e) {
            String message = String.format("Parsing %s failed", messageName);
            throw new JsonParseException(null, message, e);
        }
    }
    
    private FieldInfo getValueCompact(JsonParser parser, IFieldStructure fieldStructure, boolean dirty) throws JsonParseException, IOException {
        String currentName = parser.getCurrentName();
        try {
            boolean isMessage = parser.getCurrentToken() == JsonToken.START_OBJECT;
            boolean isColection = parser.getCurrentToken() == JsonToken.START_ARRAY;
            if (isColection) {
                JsonToken currentToken = parser.nextToken();
                parser.getCurrentName();
                isMessage = parser.getCurrentToken() == JsonToken.START_OBJECT;
                List<Object> list = new ArrayList<>();
                while (JsonToken.END_ARRAY != currentToken) {
                    list.add(getValueCompact(parser, fieldStructure, dirty).getValue());
                    currentToken = parser.nextToken();
                    currentName = parser.getCurrentName();
                }
                Type type = isMessage ?
                        Type.IMESSAGE :
                        !dirty && (fieldStructure != null && !fieldStructure.isComplex()) ? Type.parse(fieldStructure.getJavaType()) : Type.STRING;

                return new FieldInfo(list, type, isColection, null, fieldStructure);
            } else {
                if (isMessage) {
                    String name = fieldStructure != null ? fieldStructure.getName() : JSON_MESSAGE_NAME;
                    Object message = getMessageCompact(parser, fieldStructure, name, dirty);
                    return new FieldInfo(message, Type.IMESSAGE, false, null, fieldStructure);
                } else {
                    JavaType javaType = !dirty && fieldStructure != null ? fieldStructure.getJavaType() : JavaType.JAVA_LANG_STRING;
                    Object value = parseValueCompact(parser, fieldStructure, javaType, dirty);
                    return new FieldInfo(value, Type.parse(javaType), false, null, fieldStructure);
                }
            }
        } catch (Exception e) {
            String message = String.format("Parsing %s failed", currentName);
            throw new JsonParseException(null, message, e);
        }
    }
    
    private FieldInfo getValueFull(JsonParser parser, IFieldStructure fieldStructure, boolean dirty) throws JsonParseException, IOException {
        String currentFieldName = parser.getCurrentName();
        try {
            checkToken(parser, JsonToken.START_OBJECT, parser.getCurrentToken());
            String stringType = ObjectUtils.defaultIfNull(JsonMessageDecoder.<String>getValue(parser, "type", JavaType.JAVA_LANG_STRING), Type.STRING.value);
            boolean isColection = Type.isCollection(stringType);
            Type type = dirty ? Type.STRING : Type.parse(stringType);

            FieldInfo result = null;
            if (isColection) {
                checkFieldName(parser, parser.nextToken(), "value");
                checkToken(parser, JsonToken.START_ARRAY, parser.nextToken());
                JsonToken currentToken = parser.nextToken();
                currentFieldName = parser.getCurrentName();
                List<Object> list = new ArrayList<>();
                while (JsonToken.END_ARRAY != currentToken) {
                    list.add(getValueFull(parser, fieldStructure, dirty).getValue());
                    currentToken = parser.nextToken();
                    parser.getCurrentName();
                }
                result = new FieldInfo(list, type, isColection, null, fieldStructure);
                parser.nextToken();
            } else {
                Object value = getValue(parser, fieldStructure, "value", type, dirty);
                Map<String, String> properties = new HashMap<>();
                while (JsonToken.END_OBJECT != parser.nextToken()) {
                    checkFieldName(parser, JsonToken.FIELD_NAME, null);
                    parser.nextToken();
                    currentFieldName = parser.getCurrentName();
                    properties.put(parser.getCurrentName(), parser.getValueAsString());
                }
                result = new FieldInfo(value, type, isColection, properties, fieldStructure);
            }
            checkToken(parser, JsonToken.END_OBJECT, parser.getCurrentToken());
            return result;
        } catch (Exception e) {
            String message = String.format("Parsing %s failed", currentFieldName);
            throw new JsonParseException(null, message, e);
        }
    }
    
    @SuppressWarnings("unchecked")
    protected static <V> V getValue(JsonParser parser, String name, JavaType javaType) throws JsonParseException, IOException {
        checkFieldName(parser, parser.nextToken(), name);
        parser.nextToken();
        return (V) StructureUtils.castValueToJavaType(parser.getValueAsString(), javaType);
    }
    
    @SuppressWarnings("unchecked")
    protected <V> V getValue(JsonParser parser, IFieldStructure fieldStructure, String name, Type type, boolean dirty) throws JsonParseException, IOException {
        checkFieldName(parser, parser.nextToken(), name);
        parser.nextToken();
        return (V) parseValueFull(parser, fieldStructure, name, type, dirty);
    }

    protected Object parseValueFull(JsonParser parser, IFieldStructure fieldStructure, String name, Type type, boolean dirty) throws IOException {
        Object result = null;
        switch (type) {
            case BOOLEAN:
                result = Boolean.valueOf(parser.getBooleanValue());
                break;
            case SHORT:
                result = Short.valueOf(parser.getShortValue());
                break;
            case INTEGER:
                result = Integer.valueOf(parser.getIntValue());
                break;
            case LONG:
                result = Long.valueOf(parser.getLongValue());
                break;
            case BYTE:
                result = Byte.valueOf(parser.getByteValue());
                break;
            case FLOAT:
                result = Float.valueOf(parser.getFloatValue());
                break;
            case DOUBLE:
                result = Double.valueOf(parser.getDoubleValue());
                break;
            case NOTNULLFILTER:
            case NULLFILTER:
            case MVELFILTER:
            case REGEXMVELFILTER:
            case SIMPLEMVELFILTER:
            case KNOWNBUGFILTER:
            case STRING:
                result = parser.getValueAsString();
                break;
            case LOCALDATETIME:
                    result = LocalDateTime.parse(parser.getValueAsString(), DateTimeFormatter.ISO_DATE_TIME);
                break;
            case LOCALDATE:
                result = LocalDate.parse(parser.getValueAsString(), DateTimeFormatter.ISO_DATE);
                break;
            case LOCALTIME:
                result = LocalTime.parse(parser.getValueAsString(), DateTimeFormatter.ISO_TIME);
                break;
            case CHARACTER:
                result = Character.valueOf(parser.getValueAsString().charAt(0));
                break;
            case BIGDECIMAL:
                result = new BigDecimal(parser.getValueAsString());
                break;
            case IMESSAGE:
                result = getMessageFull(parser, fieldStructure, name, dirty);
                break;
            default:
                throw new JsonParseException("Unsupported type " + type, parser.getCurrentLocation());
        }
        return result;
    }

    protected Object parseValueCompact(JsonParser parser, IFieldStructure fieldStructure, JavaType javaType, boolean dirty) throws IOException {
        return Type.parse(javaType).parseValue(parser.getValueAsString(), dirty);
    }
    
    protected static void skipValue(JsonParser parser) throws JsonParseException, IOException {
        checkFieldName(parser, parser.getCurrentToken(), null);
        parser.nextToken();
    }
    
    protected static void checkFieldName(JsonParser parser, JsonToken jsonToken, String name) throws IOException, JsonParseException {
        if (JsonToken.FIELD_NAME != jsonToken || (name != null && !name.equals(parser.getCurrentName()))) {
            throw new JsonParseException("Missing expected '" + name + "' field", parser.getCurrentLocation());
        }
    }
    
    protected static void checkToken(JsonParser parser, JsonToken expected, JsonToken actual) throws JsonParseException, IOException {
        if (expected != actual) {
            throw new JsonParseException("Incorrect structure, expectd: " + expected + ", actual: " + actual, parser.getCurrentLocation());
        }
    }
    
    protected static class FieldInfo {
        private final Object value;
        private final Type type;

        private final boolean isCollection;
        private final Map<String, String> properties;
        
        private final IFieldStructure fieldStructure;
        
        public FieldInfo(Object value, Type type, boolean isCollection, Map<String, String> properties, IFieldStructure fieldStructure) {
            super();
            this.value = value;
            this.type = type;
            this.isCollection = isCollection;
            this.properties = properties != null ? properties : Collections.<String, String>emptyMap();
            this.fieldStructure = fieldStructure;
        }

        public Object getValue() {
            return value;
        }

        public Type getType() {
            return type;
        }

        public boolean isCollection() {
            return isCollection;
        }

        public Map<String, String> getProperties() {
            return properties;
        }

        public IFieldStructure getFieldStructure() {
            return fieldStructure;
        }
    }
    
    protected static enum Type {
        IMESSAGE(null, "IMessage"),
        BOOLEAN(JavaType.JAVA_LANG_BOOLEAN, "Boolean"),
        SHORT(JavaType.JAVA_LANG_SHORT, "Short"),
        INTEGER(JavaType.JAVA_LANG_INTEGER, "Integer"),
        LONG(JavaType.JAVA_LANG_LONG, "Long"),
        BYTE(JavaType.JAVA_LANG_BYTE, "Byte"),
        FLOAT(JavaType.JAVA_LANG_FLOAT, "Float"),
        DOUBLE(JavaType.JAVA_LANG_DOUBLE, "Double"),
        STRING(JavaType.JAVA_LANG_STRING, "String"),
        LOCALDATETIME(JavaType.JAVA_TIME_LOCAL_DATE_TIME, "LocalDateTime") {
            @Override
            public Object parseValue(String value, boolean dirty) {
                return LocalDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME);
            }
        },
        LOCALDATE(JavaType.JAVA_TIME_LOCAL_DATE, "LocalDate") {
            @Override
            public Object parseValue(String value, boolean dirty) {
                return LocalDate.parse(value, DateTimeFormatter.ISO_DATE);
            }
        },
        LOCALTIME(JavaType.JAVA_TIME_LOCAL_TIME, "LocalTime") {
            @Override
            public Object parseValue(String value, boolean dirty) {
                return LocalTime.parse(value, DateTimeFormatter.ISO_TIME);
            }
        },
        CHARACTER(JavaType.JAVA_LANG_CHARACTER, "Character"),
        BIGDECIMAL(JavaType.JAVA_MATH_BIG_DECIMAL, "BigDecimal"),
        MVELFILTER(null, "MvelFilter"),
        NOTNULLFILTER(null, "NotNullFilter"),
        NULLFILTER(null, "NullFilter"),
        REGEXMVELFILTER(null, "RegexMvelFilter"),
        SIMPLEMVELFILTER(null, "SimpleMvelFilter"),
        KNOWNBUGFILTER(null, "KnownBugFilter");

        private final String value;
        private final JavaType javaType;

        Type(JavaType javaType, String value) {
            this.value = value;
            this.javaType = javaType;
        }

        public Object parseValue(String value, boolean dirty) {
            if (javaType == null) {
                throw new RuntimeException("Unsupported type " + this + " value " + value);
            }
            return StructureUtils.castValueToJavaType(value, javaType);
        }
        
        static Type parse(String value) {
            value = isCollection(value) ? value.substring(5, value.length() - 1) : value;
            for (Type type : Type.values()) {
                if (type.value.equals(value)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown field type: " + value);
        }
        
        static Type parse(JavaType javaType) {
            for (Type type : Type.values()) {
                if (Objects.equals(type.javaType, javaType)) {
                    return type;
                }
            }
            return null;
        }

        static boolean isCollection(String value) {
            return value.startsWith("List");
        }
    }
}
