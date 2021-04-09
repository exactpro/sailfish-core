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

import static com.exactpro.sf.common.messages.structures.StructureUtils.getAttributeValue;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.actions.DirtyFixUtil;
import com.exactpro.sf.common.impl.messages.xml.configuration.JavaType;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.MessageUtil;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.services.fix.FixFieldConverter;
import com.exactpro.sf.services.fix.QFJDictionaryAdapter;
import com.exactpro.sf.services.fix.converter.FieldConvertException;
import com.exactpro.sf.services.fix.converter.MessageConvertException;
import com.exactpro.sf.services.fix.converter.QFJIMessageConverter;
import com.exactpro.sf.services.fix.converter.QFJIMessageConverterSettings;
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
    private static final Logger logger = LoggerFactory.getLogger(DirtyQFJIMessageConverter.class);

    private final List<String> headerFields;
    private final List<String> trailerFields;
    private final FixFieldConverter fieldConverter;
    private final boolean verifyFields;

    public DirtyQFJIMessageConverter(DirtyQFJIMessageConverterSettings settings) {
        super(settings);

        verifyFields = settings.isVerifyFields();

        IMessageStructure header = dictionary.getMessages().get(FieldConst.HEADER);
        headerFields = getFieldOrder(header);

        IMessageStructure trailer = dictionary.getMessages().get(FieldConst.TRAILER);
        trailerFields = getFieldOrder(trailer);

        fieldConverter = new FixFieldConverter();
        fieldConverter.init(dictionary, dictionary.getNamespace());
    }

    public DirtyQFJIMessageConverter(QFJIMessageConverterSettings settings) {
        this(settings.toDirtySettings());
    }

    @Nullable
    public IMessage convertDirty(@Nullable Message message, Boolean inlineHeaderAndTrailer) throws MessageConvertException {
        return convertDirty(message, null, null, inlineHeaderAndTrailer, false);
    }

    @Nullable
    public IMessage convertDirty(@Nullable Message message, Boolean verifyTagsOverride, Boolean skipTagsOverride,
                                 Boolean inlineHeaderAndTrailer) throws MessageConvertException {
        return convertDirty(message, verifyTagsOverride, skipTagsOverride, inlineHeaderAndTrailer, false);
    }

    @Nullable
    public IMessage convertDirty(@Nullable Message message, Boolean verifyTagsOverride, Boolean skipTagsOverride,
                                 Boolean inlineHeaderAndTrailer, boolean ignoreFieldType) throws MessageConvertException {
        if(message == null) {
            return null;
        }

        IMessage resultMessage = convert(message, verifyTagsOverride, skipTagsOverride, ignoreFieldType);

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

    @Nullable
    public RawMessage convertDirty(IMessage message, String messageName, boolean fillHeader, String beginString, int msgSeqNum, String senderCompID, String targetCompID) throws MessageConvertException {
        if(message == null) {
            return null;
        }

        if(verifyFields && isBlank(messageName)) {
            IMessageStructure msgStructure = dictionary.getMessages().get(message.getName());
            if (msgStructure == null) {
                throw new MessageConvertException("Either the 'messageName' parameter must be set or the '" + message.getName() + "' must exist in the dictionary if field verification is enabled");
            }
            messageName = msgStructure.getName();
        }

        IMessage oldMessage = message;
        message = fieldConverter.convertFields(message, factory, false);

        extractMessage(message, FieldConst.HEADER, headerFields);
        extractMessage(message, FieldConst.TRAILER, trailerFields);

        replaceDirtyFields(message);

        RawMessage resultMessage = new RawMessage();
        IMessageStructure messageStructure = dictionary.getMessages().get(messageName);

        if(verifyFields && messageStructure == null) {
            throw new MessageConvertException("Dictionary " + dictionary.getNamespace() + " doesn't conatin message type " + messageName);
        }

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

    @Nullable
    public RawMessage convertDirty(IMessage message, String messageName) throws MessageConvertException {
        return convertDirty(message, messageName, false, null, 0, null, null);
    }

    @Nullable
    public RawMessage convertDirty(IMessage message) throws MessageConvertException {
        return convertDirty(message, null);
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
    private void traverseDirtyIMessage(IMessage message, FieldList resultMessage, @Nullable IFieldStructure rootStructure) throws MessageConvertException {
        if(message.isFieldSet(FieldConst.FIELD_ORDER)) {
            Object fieldValue = message.getField(FieldConst.FIELD_ORDER);

            if(fieldValue instanceof List<?>) {
                if(ensureListType((List<?>)fieldValue, String.class)) {
                    resultMessage.setOrder(parseFieldOrder((List<String>)fieldValue));
                } else {
                    throw new MessageConvertException("Value of " + FieldConst.FIELD_ORDER + " field in message " + message.getName() + " is not a list of strings, value '"
                            + fieldValue + "' class '" + ClassUtils.getName(fieldValue) + '\'');
                }
            } else {
                throw new MessageConvertException("Value of " + FieldConst.FIELD_ORDER + " field in message " + message.getName() + " is not a list, value '"
                        + fieldValue + "' class '" + ClassUtils.getName(fieldValue) + '\'');
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
                throw new MessageConvertException("Value of " + FieldConst.FIELD_GROUP_DELIMITER + " field in message " + message.getName() + " is not a string, value '"
                        + fieldValue + "' class '" + ClassUtils.getName(fieldValue) + '\'');
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
                throw new MessageConvertException("Value of " + FieldConst.GROUP_COUNTERS + " field in message " + message.getName() + " is neither a Map nor a IMessage, value '"
                        + fieldValue + "' class '" + ClassUtils.getName(fieldValue) + '\'');
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
                try {
                    FieldList component = new FieldList();
                    resultMessage.addField(new Field(fieldName, component));
                    traverseDirtyIMessage((IMessage)fieldValue, component, fieldStructure);
                    continue;
                } catch (FieldConvertException e) {
                    if (rootStructure != null) {
                        throw new FieldConvertException(rootStructure.getName() + '.' + e.getMessage(), e);
                    }
                    throw e;
                }
            }

            if(fieldValue instanceof List<?>) {
                if(!fieldName.matches("\\d+")) {
                    throw new MessageConvertException("Unknown the field: " + fieldName + " in message " + message.getName() + ", value '"
                            + fieldValue + "' class '" + ClassUtils.getName(fieldValue) + '\'');
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
                        try {
                            FieldList group = new FieldList();
                            traverseDirtyIMessage(iGroup, group, fieldStructure);
                            groups.add(group);
                        } catch (FieldConvertException e) {
                            if (rootStructure != null) {
                                throw new FieldConvertException(rootStructure.getName() + '.' + e.getMessage(), e);
                            }
                            throw e;
                        }
                    }

                    resultMessage.addField(new Field(fieldName, groupCounter, groups));
                } else {
                    throw new MessageConvertException("Unknown list type in the field: " + fieldName + " in message " + message.getName() + ", value " + ((List<?>)fieldValue).stream()
                            .map(ClassUtils::getName)
                            .collect(Collectors.joining()));
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
                            boolean includeNanos = includeNanoseconds;

                            Object fixType = getAttributeValue(fieldStructure, QFJDictionaryAdapter.ATTRIBUTE_FIX_TYPE);
                            if (FieldType.UtcTimeStampSecondPresicion.getName().equals(fixType)) {
                                includeMillis = includeMicros = includeNanos = false;
                            }

                            resultMessage.addField(new Field(fieldName, UtcTimestampConverter.convert(timestamp, includeMillis, includeMicros, includeNanos)));
                            break;
                        case JAVA_TIME_LOCAL_TIME:
                            resultMessage.addField(new Field(fieldName, UtcTimeOnlyConverter.convert(timestamp, includeMilliseconds, includeMicroseconds, includeNanoseconds)));
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
                            resultMessage.addField(new Field(fieldName, UtcTimeOnlyConverter.convert(timestamp, includeMilliseconds, includeMicroseconds, includeNanoseconds)));
                        } else {
                            resultMessage.addField(new Field(fieldName, UtcTimestampConverter.convert(timestamp, includeMilliseconds, includeMicroseconds, includeNanoseconds)));
                        }
                    }
                }  else if (fieldValue instanceof Boolean) {
                    resultMessage.addField(new Field(fieldName, BooleanConverter.convert((Boolean)fieldValue)));
                } else {
                    resultMessage.addField(new Field(fieldName, fieldValue.toString()));
                }
            } else {
                throw new MessageConvertException("Unknown the field: " + fieldName + " in message " + message.getName());
            }
        }
    }

    private List<String> getFieldOrder(IFieldStructure messageStructure) {
        if(messageStructure == null || !messageStructure.isComplex()) {
            return Collections.emptyList();
        }

        List<String> fieldOrder = new ArrayList<>();

        for(IFieldStructure fieldStructure : messageStructure.getFields().values()) {
            Integer fieldTag = getAttributeValue(fieldStructure, ATTRIBUTE_TAG);
            fieldOrder.add(fieldTag != null ? fieldTag.toString() : fieldStructure.getName());
        }

        // to ensure that comparator will place them at the start and the end accordingly
        fieldOrder.remove(FieldConst.HEADER);
        fieldOrder.remove(FieldConst.TRAILER);

        return fieldOrder;
    }

    private IFieldStructure getFieldStructure(String name, IFieldStructure messageStructure) throws MessageConvertException {
        if(messageStructure == null) {
            return FieldConst.HEADER.equals(name) || FieldConst.TRAILER.equals(name) ? dictionary.getMessages().get(name) : null;
        }

        if (!messageStructure.isComplex()) {
            return null;
        }

        IFieldStructure fieldStructure = messageStructure.getFields().get(name);

        if(fieldStructure == null) {
            for(IFieldStructure field : messageStructure.getFields().values()) {
                Integer tag = getAttributeValue(field, ATTRIBUTE_TAG);

                if(tag != null && name.equals(tag.toString())) {
                    return field;
                }
            }
        }

        if(verifyFields && fieldStructure == null) {
            throw new FieldConvertException(messageStructure.getName() + " doesn't conatin field or tag " + name);
        }

        return fieldStructure;
    }

    private List<String> parseFieldOrder(List<String> fieldOrder) throws MessageConvertException {
        List<String> newFieldOrder = new ArrayList<>(fieldOrder.size());

        for(String fieldName : fieldOrder) {
            String fieldTag = fieldConverter.convertToTag(fieldName);

            if(fieldTag == null) {
                IMessageStructure messageStructure = dictionary.getMessages().get(fieldName);

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

        String msgType = messageStructure != null ? getAttributeValue(messageStructure, ATTRIBUTE_MESSAGE_TYPE) : null;
        replaceIfNotExist(header, FieldConst.MSG_TYPE, msgType);

        replaceIfNotExist(header, FieldConst.SENDER_COMP_ID, senderCompID);
        replaceIfNotExist(header, FieldConst.TARGET_COMP_ID, targetCompID);

        String sendingTimeValue =  UtcTimestampConverter.convert(
                DateTimeUtility.toTimestamp(DateTimeUtility.nowLocalDateTime()),
                includeMilliseconds, includeMicroseconds, includeNanoseconds);

        replaceIfNotExist(header, FieldConst.SENDING_TIME, sendingTimeValue);
    }

    /**
     * Workaround for component stored in HashMap as list of IMessage
     * @param fieldName
     * @param fieldValue
     * @param fieldStructure
     * @return
     */
    private Object extractComponent(String fieldName, Object fieldValue, @Nullable IFieldStructure fieldStructure) {
        if(fieldValue instanceof List<?> && fieldStructure != null && fieldStructure.isComplex() && !fieldStructure.isCollection()) {
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
                    throw new MessageConvertException("Unknown the field: " + fieldName + " in message " + message.getName());
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
                throw new MessageConvertException(field + " field is not a string in message " + message.getName() + ", value " + fieldValue + ", type " + ClassUtils.getName(fieldValue));
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
