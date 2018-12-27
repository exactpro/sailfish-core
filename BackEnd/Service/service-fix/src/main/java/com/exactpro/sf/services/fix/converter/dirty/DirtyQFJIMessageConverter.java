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
package com.exactpro.sf.services.fix.converter.dirty;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAccessor;

import com.exactpro.sf.actions.DirtyFixUtil;
import com.exactpro.sf.common.impl.messages.xml.configuration.JavaType;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.MessageUtil;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.services.fix.FixFieldConverter;
import com.exactpro.sf.services.fix.QFJDictionaryAdapter;
import com.exactpro.sf.services.fix.converter.MessageConvertException;
import com.exactpro.sf.services.fix.converter.QFJIMessageConverter;
import com.exactpro.sf.services.fix.converter.dirty.struct.Field;
import com.exactpro.sf.services.fix.converter.dirty.struct.FieldList;
import com.exactpro.sf.services.fix.converter.dirty.struct.RawMessage;
import com.exactpro.sf.util.DateTimeUtility;

import quickfix.FieldType;
import quickfix.Message;
import quickfix.field.converter.BooleanConverter;
import quickfix.field.converter.UtcDateOnlyConverter;
import quickfix.field.converter.UtcTimeOnlyConverter;
import quickfix.field.converter.UtcTimestampConverter;

public class DirtyQFJIMessageConverter extends QFJIMessageConverter {
    private List<String> headerFields;
    private List<String> trailerFields;
    private FixFieldConverter fieldConverter;

    public DirtyQFJIMessageConverter(IDictionaryStructure dictionary, IMessageFactory factory, boolean verifyTags, boolean includeMilliseconds, boolean skipTags) {
        this(dictionary, factory, verifyTags, includeMilliseconds, false, skipTags);
    }

    public DirtyQFJIMessageConverter(IDictionaryStructure dictionary, IMessageFactory factory,
            boolean verifyTags, boolean includeMilliseconds,
            boolean includeMicroseconds, boolean skipTags) {
        this(dictionary, factory, verifyTags, includeMilliseconds, includeMicroseconds, skipTags, false);
    }
    
    public DirtyQFJIMessageConverter(IDictionaryStructure dictionary, IMessageFactory factory,
                                     boolean verifyTags, boolean includeMilliseconds,
                                     boolean includeMicroseconds, boolean skipTags, boolean orderingFields) {
        super(dictionary, factory, verifyTags, includeMilliseconds,  includeMicroseconds, skipTags, orderingFields);
        init();
    }

    private void init() {
        IMessageStructure header = dictionary.getMessageStructure(FieldConst.HEADER);
        headerFields = getFieldOrder(header);

        IMessageStructure trailer = dictionary.getMessageStructure(FieldConst.TRAILER);
        trailerFields = getFieldOrder(trailer);

        fieldConverter = new FixFieldConverter();
        fieldConverter.init(dictionary, dictionary.getNamespace());
    }

    public IMessage convertDirty(Message message, Boolean inlineHeaderAndTrailer) throws MessageConvertException {
        return convertDirty(message, null, null, inlineHeaderAndTrailer, false);
    }

    public IMessage convertDirty(Message message, Boolean verifyTagsOverride, Boolean skipTagsOverride,
                                 Boolean inlineHeaderAndTrailer) throws MessageConvertException {
        return convertDirty(message, verifyTagsOverride, skipTagsOverride, inlineHeaderAndTrailer, false);
    }

    public IMessage convertDirty(Message message, Boolean verifyTagsOverride, Boolean skipTagsOverride,
                                 Boolean inlineHeaderAndTrailer, boolean ignoreFieldType) throws MessageConvertException {
        if(message == null) {
            return null;
        }

        IMessage resultMessage = super.convert(message, verifyTagsOverride, skipTagsOverride, ignoreFieldType);

        if(inlineHeaderAndTrailer) {
            inlineMessage(resultMessage, FieldConst.HEADER);
            inlineMessage(resultMessage, FieldConst.TRAILER);
        }

        return resultMessage;
    }

    private void inlineMessage(IMessage parentMessage, String childName) {
        IMessage childMessage = parentMessage.getField(childName);

        for(String fieldName : childMessage.getFieldNames()) {
            parentMessage.addField(fieldName, childMessage.getField(fieldName));
        }

        parentMessage.removeField(childName);
    }

    public RawMessage convertDirty(IMessage message, String messageName, boolean fillHeader, String beginString, int msgSeqNum, String senderCompID, String targetCompID) throws MessageConvertException {
        if(message == null) {
            return null;
        }

        IMessage oldMessage = message;
        message = fieldConverter.convertFields(message, factory, false);

        extractMessage(message, FieldConst.HEADER, headerFields);
        extractMessage(message, FieldConst.TRAILER, trailerFields);

        replaceDirtyFields(message);

        RawMessage resultMessage = new RawMessage();
        IMessageStructure messageStructure = dictionary.getMessageStructure(messageName);

        if(fillHeader) {
            fillHeader(message, messageStructure, beginString, msgSeqNum, senderCompID, targetCompID);
        }

        traverseDirtyIMessage(message, resultMessage, messageStructure);

        IMessage header = message.getField(FieldConst.HEADER);

        // this check is needed in case of excluded field
        if(!header.isFieldSet(FieldConst.BODY_LENGTH)) {
            resultMessage.calculateBodyLength();
        }

        IMessage trailer = message.getField(FieldConst.TRAILER);

        // this check is needed in case of excluded field
        if(!trailer.isFieldSet(FieldConst.CHECKSUM)) {
            resultMessage.calculateCheckSum();
        }

        resultMessage.ensureOrder();

        copyComponent(resultMessage, FieldConst.HEADER, oldMessage, FieldConst.HEADER);
        copyComponent(resultMessage, FieldConst.TRAILER, oldMessage, FieldConst.TRAILER);

        return resultMessage;
    }

    public RawMessage convertDirty(IMessage message, String messageName) throws MessageConvertException {
        return convertDirty(message, messageName, false, null, 0, null, null);
    }

    private void extractMessage(IMessage parentMessage, String childName, List<String> childFields) {
        IMessage childMessage = extractComponent(parentMessage.getField(childName));

        if(childMessage == null) {
            childMessage = factory.createMessage(childName, dictionary.getNamespace());

            for(String fieldName : childFields) {
                if(parentMessage.isFieldSet(fieldName)) {
                    childMessage.addField(fieldName, parentMessage.getField(fieldName));
                    parentMessage.removeField(fieldName);
                }
            }
        }

        parentMessage.addField(childName, childMessage);
    }

    @SuppressWarnings({ "unchecked", "deprecation" })
    private void traverseDirtyIMessage(IMessage message, FieldList resultMessage, IFieldStructure rootStructure) throws MessageConvertException {
        if(message.isFieldSet(FieldConst.FIELD_ORDER)) {
            Object fieldValue = message.getField(FieldConst.FIELD_ORDER);

            if(fieldValue instanceof List<?>) {
                if(ensureListType((List<?>)fieldValue, String.class)) {
                    resultMessage.setOrder(parseFieldOrder((List<String>)fieldValue));
                } else {
                    throw new MessageConvertException(message, FieldConst.FIELD_ORDER + " field is not a list of strings");
                }
            } else {
                throw new MessageConvertException(message, FieldConst.FIELD_ORDER + " field is not a list");
            }
        } else if (message.isFieldSet(FieldConst.FIELD_GROUP_DELIMITER)) {
            Object fieldValue = message.getField(FieldConst.FIELD_GROUP_DELIMITER);

            if (fieldValue instanceof String) {
                List<String> originOrder = getFieldOrder(rootStructure);
                List<String> firstElement = parseFieldOrder(Arrays.asList((String)fieldValue));

                originOrder.removeAll(firstElement);
                firstElement.addAll(originOrder);
                resultMessage.setOrder(firstElement);
            } else {
                throw new MessageConvertException(message, FieldConst.FIELD_GROUP_DELIMITER + " field is not a string");
            }
        } else {
            resultMessage.setOrder(getFieldOrder(rootStructure));
        }

        message.removeField(FieldConst.FIELD_ORDER);
        message.removeField(FieldConst.FIELD_GROUP_DELIMITER);

        convertDoubleTag(message, DirtyFixUtil.DUPLICATE_TAG);
        convertDoubleTag(message, DirtyFixUtil.DOUBLE_TAG);

        Map<String, Object> groupCounters = new HashMap<>();

        if(message.isFieldSet(FieldConst.GROUP_COUNTERS)) {
            Object fieldValue = message.removeField(FieldConst.GROUP_COUNTERS);

            if(fieldValue instanceof Map<?, ?>) {
                groupCounters = parseGroupCounters((Map<?, ?>)fieldValue);
            } else if(fieldValue instanceof IMessage) {
                groupCounters = parseGroupCounters(MessageUtil.convertToHashMap((IMessage) fieldValue));
            } else {
                throw new MessageConvertException(message, "GroupCounters field is neither a Map nor a IMessage");
            }
        }

        for(String fieldName : message.getFieldNames()) {
            Object fieldValue = message.getField(fieldName);
            IFieldStructure fieldStructure = getFieldStructure(fieldName, rootStructure);

            if(fieldValue == null || fieldValue == FieldConst.EXCLUDED_FIELD) { // Compare reference between field value and EXCLUDE_FIELD
                continue;
            }

            fieldValue = extractComponent(fieldName, fieldValue, fieldStructure);

            if(fieldValue instanceof IMessage) {
                FieldList component = new FieldList();
                resultMessage.addField(new Field(fieldName, component));
                traverseDirtyIMessage((IMessage)fieldValue, component, fieldStructure);

                continue;
            }

            if(fieldValue instanceof List<?>) {
                if(!fieldName.matches("\\d+")) {
                    throw new MessageConvertException(message, "Unknown field: " + fieldName);
                }

                if(ensureListType((List<?>)fieldValue, String.class)) {
                    List<String> multipleValues = (List<String>)fieldValue;

                    for(int i = 0; i < multipleValues.size(); i++) {
                        String value = multipleValues.get(i);
                        resultMessage.addField(new Field(fieldName, value, i));
                    }
                } else if(ensureListType((List<?>)fieldValue, IMessage.class)) {
                    List<IMessage> iGroups = (List<IMessage>)fieldValue;
                    List<FieldList> groups = new ArrayList<>(iGroups.size());
                    Field groupCounter = null;

                    if(groupCounters.containsKey(fieldName)) {
                        Object value = groupCounters.get(fieldName);

                        if(value != FieldConst.EXCLUDED_FIELD) {
                            groupCounter = new Field(fieldName, value.toString());
                        }
                    } else {
                        groupCounter = new Field(fieldName, String.valueOf(iGroups.size()));
                    }

                    for(IMessage iGroup : iGroups) {
                        FieldList group = new FieldList();
                        traverseDirtyIMessage(iGroup, group, fieldStructure);
                        groups.add(group);
                    }

                    resultMessage.addField(new Field(fieldName, groupCounter, groups));
                } else {
                    throw new MessageConvertException(message, "Unknown list type in field: " + fieldName);
                }

                continue;
            }

            if(fieldName.matches("\\d+")) {
                if (fieldValue instanceof TemporalAccessor) {
                    TemporalAccessor temporalAccessor = (TemporalAccessor) fieldValue;
                    Timestamp timestamp = DateTimeUtility.toTimestamp(temporalAccessor);

                    if (fieldStructure != null && fieldStructure.isSimple()) {
                        JavaType type = fieldStructure.getJavaType();

                        switch (type) {
                        case JAVA_TIME_LOCAL_DATE_TIME:
                            boolean includeMillis = includeMilliseconds;
                            boolean includeMicros = includeMicroseconds;

                            Object fixType = fieldStructure.getAttributeValueByName(QFJDictionaryAdapter.ATTRIBUTE_FIX_TYPE);
                            if (FieldType.UtcTimeStampSecondPresicion.getName().equals(fixType)) {
                                includeMillis = includeMicros = false;
                            }

                            resultMessage.addField(new Field(fieldName, UtcTimestampConverter.convert(timestamp, includeMillis, includeMicros)));
                            break;
                        case JAVA_TIME_LOCAL_TIME:
                            resultMessage.addField(new Field(fieldName, UtcTimeOnlyConverter.convert(timestamp, this.includeMilliseconds, this.includeMicroseconds)));
                            break;
                        case JAVA_TIME_LOCAL_DATE:
                            resultMessage.addField(new Field(fieldName, UtcDateOnlyConverter.convert(timestamp)));
                            break;
                        default:
                            resultMessage.addField(new Field(fieldName, fieldValue.toString()));
                        }
                    } else {
                        if (fieldValue instanceof LocalDate) {
                            resultMessage.addField(new Field(fieldName, UtcDateOnlyConverter.convert(timestamp)));
                        } else if (fieldValue instanceof LocalTime) {
                            resultMessage.addField(new Field(fieldName, UtcTimeOnlyConverter.convert(timestamp, this.includeMilliseconds, this.includeMicroseconds)));
                        } else {
                            resultMessage.addField(new Field(fieldName, UtcTimestampConverter.convert(timestamp, this.includeMilliseconds, this.includeMicroseconds)));
                        }
                    }
                }  else if (fieldValue instanceof Boolean) {
                    resultMessage.addField(new Field(fieldName, BooleanConverter.convert((Boolean)fieldValue)));
                } else {
                    resultMessage.addField(new Field(fieldName, fieldValue.toString()));
                }
            } else {
                throw new MessageConvertException(message, "Unknown field: " + fieldName);
            }
        }
    }

    private List<String> getFieldOrder(IFieldStructure messageStructure) {
        if(messageStructure == null || !messageStructure.isComplex()) {
            return Collections.emptyList();
        }

        List<String> fieldOrder = new ArrayList<>();

        for(IFieldStructure fieldStructure : messageStructure.getFields()) {
            Integer fieldTag = (Integer)fieldStructure.getAttributeValueByName(ATTRIBUTE_TAG);

            if(fieldTag != null) {
                fieldOrder.add(fieldTag.toString());
            } else {
                fieldOrder.add(fieldStructure.getName());
            }
        }

        // to ensure that comparator will place them at the start and the end accordingly
        fieldOrder.remove(FieldConst.HEADER);
        fieldOrder.remove(FieldConst.TRAILER);

        return fieldOrder;
    }

    private IFieldStructure getFieldStructure(String name, IFieldStructure messageStructure) {
        if(messageStructure == null) {
            if (FieldConst.HEADER.equals(name) || FieldConst.TRAILER.equals(name)) {
                return this.dictionary.getMessageStructure(name);
            }
            return null;
        }

        if (!messageStructure.isComplex()) {
            return null;
        }

        IFieldStructure fieldStructure = messageStructure.getField(name);

        if(fieldStructure == null) {
            for(IFieldStructure field : messageStructure.getFields()) {
                Integer tag = (Integer)field.getAttributeValueByName(ATTRIBUTE_TAG);

                if(tag != null && name.equals(tag.toString())) {
                    return field;
                }
            }
        }

        return fieldStructure;
    }

    private List<String> parseFieldOrder(List<String> fieldOrder) throws MessageConvertException {
        List<String> newFieldOrder = new ArrayList<>(fieldOrder.size());

        for(String fieldName : fieldOrder) {
            String fieldTag = fieldConverter.convertToTag(fieldName);

            if(fieldTag == null) {
                IMessageStructure messageStructure = dictionary.getMessageStructure(fieldName);

                if(messageStructure == null) {
                    throw new MessageConvertException("Unknown field in field order: " + fieldName);
                }

                newFieldOrder.add(fieldName);
            } else {
                newFieldOrder.add(fieldTag);
            }
        }

        return newFieldOrder;
    }

    private Map<String, Object> parseGroupCounters(Map<?, ?> map) throws MessageConvertException {
        Map<String, Object> counters = new HashMap<>();

        for(Entry<?, ?> e : map.entrySet()) {
            String name = fieldConverter.convertToTag(e.getKey().toString());
            Object value = e.getValue();

            if(name == null) {
                throw new MessageConvertException("Unknown group counter field in GroupCounters: " + e.getKey());
            }

            counters.put(name, value);
        }

        return counters;
    }

    private boolean ensureListType(List<?> list, Class<?> type) {
        for(Object e : list) {
            if(e != null && !type.isAssignableFrom(e.getClass())) {
                return false;
            }
        }

        return true;
    }

    private void fillHeader(IMessage message, IMessageStructure messageStructure, String beginString, int msgSeqNum, String senderCompID, String targetCompID) {
        IMessage header = message.getField(FieldConst.HEADER);

        replaceIfNotExist(header, FieldConst.BEGIN_STRING, beginString);
        replaceIfNotExist(header, FieldConst.MSG_SEQ_NUM, msgSeqNum);

        String msgType = messageStructure != null ? (String)messageStructure.getAttributeValueByName(ATTRIBUTE_MESSAGE_TYPE) : null;
        replaceIfNotExist(header, FieldConst.MSG_TYPE, msgType);

        replaceIfNotExist(header, FieldConst.SENDER_COMP_ID, senderCompID);
        replaceIfNotExist(header, FieldConst.TARGET_COMP_ID, targetCompID);

        String sendingTimeValue =  UtcTimestampConverter.convert(
                DateTimeUtility.toTimestamp(DateTimeUtility.nowLocalDateTime()),
                this.includeMilliseconds, this.includeMicroseconds);

        replaceIfNotExist(header, FieldConst.SENDING_TIME, sendingTimeValue);
    }

    /**
     * Workaround for component stored in HashMap as list of IMessage
     * @param fieldName
     * @param fieldValue
     * @param fieldStructure
     * @return
     */
    private Object extractComponent(String fieldName, Object fieldValue, IFieldStructure fieldStructure) {
        if(fieldValue instanceof List<?> && (fieldStructure != null && fieldStructure.isComplex() && !fieldStructure.isCollection())) {
            return extractComponent(fieldValue);
        }

        return fieldValue;
    }

    private IMessage extractComponent(Object obj) {
        if(obj != null && List.class.isAssignableFrom(obj.getClass())) {
            List<?> list = (List<?>)obj;

            if(list.size() == 1 && list.get(0) != null && IMessage.class.isAssignableFrom(list.get(0).getClass())) {
                return (IMessage)list.get(0);
            }
        }

        return (IMessage)obj;
    }

    /**
     * @deprecated Message content has more priority than dirty tags
     */
    @Deprecated
    private void replaceDirtyFields(IMessage message) throws MessageConvertException {
        IMessage header = message.getField(FieldConst.HEADER);

        IMessage trailer = message.getField(FieldConst.TRAILER);
        moveIfExist(message, DirtyFixUtil.DIRTY_CHECK_SUM, trailer, FieldConst.CHECKSUM);

        List<String> fieldNames = new ArrayList<>(message.getFieldNames());
        for(int i = 0; i < fieldNames.size(); i++){
            String fieldName = fieldNames.get(i);
            if(fieldName.startsWith(DirtyFixUtil.DIRTY)){
                String tag = fieldConverter.convertToTag(fieldName.substring(DirtyFixUtil.DIRTY.length()));
                if(tag == null){
                    throw new MessageConvertException(message, "Unknown field: " + fieldName);
                }
                moveIfExist(message, fieldName, header, tag);
            }
        }
    }

    /**
     * Convert double tag to array of values
     * @throws MessageConvertException
     * @deprecated message may contain array of values
     */
    @SuppressWarnings("unchecked")
    @Deprecated
    private void convertDoubleTag(IMessage message, String field) throws MessageConvertException {
        if(message.isFieldSet(field)) {
            Object fieldValue = message.removeField(field);

            if(fieldValue instanceof String) {
                String[] pairArray = ((String) fieldValue).split(";");
                for (int i = 0; i < pairArray.length; i++) {
                    String[] keyValue = pairArray[i].split("=");

                    List<Object> value = null;
                    if (message.isFieldSet(keyValue[0])) {
                        Object originValue = message.getField(keyValue[0]);
                        if (originValue instanceof List) {
                            value = (List<Object>) originValue;
                        } else {
                            value = new ArrayList<>();
                            value.add(originValue);
                            message.addField(keyValue[0], value);
                        }
                    } else {
                        value = new ArrayList<>();
                        message.addField(keyValue[0], value);
                    }
                    value.add(keyValue[1]);
                }
            } else {
                throw new MessageConvertException(message, field + " field is not a string");
            }
        }
    }

    private void replaceIfNotExist(IMessage message, String fieldName, Object newValue) {
        Object currentValue = message.getField(fieldName);

        if(currentValue == null) {
            message.addField(fieldName, newValue);
        }
    }

    private void moveIfExist(IMessage donor, String fieldSource, IMessage acceptor, String fieldTarget) {
        Object value = donor.removeField(fieldSource);

        if(value != null) {
            acceptor.addField(fieldTarget, value);
        }
    }

    private void copyComponent(RawMessage sourceMessage, String sourceFieldName, IMessage targetMessage, String targetFieldName) throws MessageConvertException {
        Field sourceField = sourceMessage.getField(sourceFieldName);

        if(sourceField == null || !sourceField.isComponent()) {
            throw new MessageConvertException("Source field is missing or not a component");
        }

        IMessage targetField = extractComponent(targetMessage.getField(targetFieldName));

        if(targetField == null) {
            targetField = factory.createMessage(targetFieldName, targetMessage.getNamespace());
            targetMessage.addField(targetFieldName, targetField);
        }

        for(Field field : sourceField.getFields().getFields()) {
            if(!field.isSimple()) {
                continue;
            }

            String fieldTag = field.getName();
            String fieldName = fieldConverter.convertToName(fieldTag);

            if(!targetField.isFieldSet(fieldTag) && !targetField.isFieldSet(fieldName)) {
                targetField.addField(StringUtils.defaultIfEmpty(fieldName, fieldTag), field.getValue());
            }
        }
    }
}
