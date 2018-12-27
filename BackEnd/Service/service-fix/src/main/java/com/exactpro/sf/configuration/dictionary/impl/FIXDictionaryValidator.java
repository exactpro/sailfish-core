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
package com.exactpro.sf.configuration.dictionary.impl;

import static com.exactpro.sf.services.fix.FixMessageHelper.ATTRIBUTE_ENTITY_TYPE;
import static com.exactpro.sf.services.fix.FixMessageHelper.COMPONENT_ENTITY;
import static com.exactpro.sf.services.fix.FixMessageHelper.DAY_OF_MONTH;
import static com.exactpro.sf.services.fix.FixMessageHelper.GROUP_ENTITY;
import static com.exactpro.sf.services.fix.FixMessageHelper.HEADER;
import static com.exactpro.sf.services.fix.FixMessageHelper.HEADER_ENTITY;
import static com.exactpro.sf.services.fix.FixMessageHelper.HEARTBEAT_MESSAGE;
import static com.exactpro.sf.services.fix.FixMessageHelper.LANGUAGE;
import static com.exactpro.sf.services.fix.FixMessageHelper.LOGON_MESSAGE;
import static com.exactpro.sf.services.fix.FixMessageHelper.LOGOUT_MESSAGE;
import static com.exactpro.sf.services.fix.FixMessageHelper.MESSAGE_ENTITY;
import static com.exactpro.sf.services.fix.FixMessageHelper.MESSAGE_TYPE_ATTR_NAME;
import static com.exactpro.sf.services.fix.FixMessageHelper.MONTH_YEAR;
import static com.exactpro.sf.services.fix.FixMessageHelper.MSG_TYPE_FIELD;
import static com.exactpro.sf.services.fix.FixMessageHelper.MULTIPLECHARVALUE;
import static com.exactpro.sf.services.fix.FixMessageHelper.MULTIPLESTRINGVALUE;
import static com.exactpro.sf.services.fix.FixMessageHelper.REJECT_MESSAGE;
import static com.exactpro.sf.services.fix.FixMessageHelper.RESENDREQUEST_MESSAGE;
import static com.exactpro.sf.services.fix.FixMessageHelper.SEQUENCERESET_MESSAGE;
import static com.exactpro.sf.services.fix.FixMessageHelper.TESTREQUEST_MESSAGE;
import static com.exactpro.sf.services.fix.FixMessageHelper.TRAILER;
import static com.exactpro.sf.services.fix.FixMessageHelper.TRAILER_ENTITY;
import static com.exactpro.sf.services.fix.FixMessageHelper.TZTIMEONLY;
import static com.exactpro.sf.services.fix.FixMessageHelper.TZTIMESTAMP;
import static com.exactpro.sf.services.fix.FixMessageHelper.XMLDATA;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.lang3.ObjectUtils;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.exactpro.sf.common.impl.messages.xml.configuration.JavaType;
import com.exactpro.sf.common.messages.structures.IAttributeStructure;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.configuration.dictionary.DictionaryValidationError;
import com.exactpro.sf.configuration.dictionary.DictionaryValidationErrorLevel;
import com.exactpro.sf.configuration.dictionary.DictionaryValidationErrorType;
import com.exactpro.sf.configuration.dictionary.ValidationHelper;
import com.exactpro.sf.configuration.dictionary.interfaces.IDictionaryValidator;
import com.exactpro.sf.services.fix.FixMessageHelper;
import com.exactpro.sf.services.fix.QFJDictionaryAdapter;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

import quickfix.FieldType;

public class FIXDictionaryValidator extends AbstractDictionaryValidator {
    private static final long serialVersionUID = 1839126797364844485L;

    private final Map<String, EntityValidator> entityNamesToEntityValidators;

    private final SetMultimap<Class<?>, String> javaTypeToFixTypes = HashMultimap.create();

    private static final String DATE = "DATE";
    private static final String LONG = "LONG";


    {
        entityNamesToEntityValidators = new HashMap<>();
        entityNamesToEntityValidators.put(MESSAGE_ENTITY, new MessageEntityValidator());
        entityNamesToEntityValidators.put(COMPONENT_ENTITY, new ComponentEntityValidator());
        entityNamesToEntityValidators.put(GROUP_ENTITY, new GroupEntityValidator());
        entityNamesToEntityValidators.put(HEADER_ENTITY, new HeaderEntityValidator());
        entityNamesToEntityValidators.put(TRAILER_ENTITY, new TrailerEntityValidator());

        javaTypeToFixTypes.put(Double.class, FieldType.Price.getName());
        javaTypeToFixTypes.put(Double.class, FieldType.Amt.getName());
        javaTypeToFixTypes.put(Double.class, FieldType.Qty.getName());
        javaTypeToFixTypes.put(Double.class, FieldType.Float.getName());
        javaTypeToFixTypes.put(Double.class, FieldType.PriceOffset.getName());
        javaTypeToFixTypes.put(Double.class, FieldType.Percentage.getName());

        javaTypeToFixTypes.putAll(BigDecimal.class, javaTypeToFixTypes.get(Double.class));

        javaTypeToFixTypes.put(Integer.class, FieldType.Int.getName());
        javaTypeToFixTypes.put(Integer.class, FieldType.DayOfMonth.getName());
        javaTypeToFixTypes.put(Integer.class, FieldType.NumInGroup.getName());
        javaTypeToFixTypes.put(Integer.class, FieldType.SeqNum.getName());
        javaTypeToFixTypes.put(Integer.class, FieldType.Length.getName());

        javaTypeToFixTypes.put(Long.class, LONG);

        javaTypeToFixTypes.put(LocalDateTime.class, FieldType.UtcTimeStamp.getName());
        javaTypeToFixTypes.put(LocalDateTime.class, FieldType.UtcDate.getName());
        javaTypeToFixTypes.put(LocalDateTime.class, FieldType.UtcTimeOnly.getName());
        javaTypeToFixTypes.put(LocalDateTime.class, FieldType.UtcDateOnly.getName());
        javaTypeToFixTypes.put(LocalDateTime.class, FieldType.Time.getName());
        javaTypeToFixTypes.put(LocalDateTime.class, DATE);
        javaTypeToFixTypes.put(LocalDateTime.class, FieldType.UtcTimeStampSecondPresicion.getName());

        javaTypeToFixTypes.put(LocalTime.class, FieldType.UtcTimeOnly.getName());

        javaTypeToFixTypes.put(LocalDate.class, FieldType.UtcDateOnly.getName());

        javaTypeToFixTypes.put(Boolean.class, FieldType.Boolean.getName());

        javaTypeToFixTypes.put(String.class, FieldType.Unknown.getName());
        javaTypeToFixTypes.put(String.class, FieldType.String.getName());
        javaTypeToFixTypes.put(String.class, FieldType.Char.getName());
        javaTypeToFixTypes.put(String.class, FieldType.Currency.getName());
        javaTypeToFixTypes.put(String.class, FieldType.MultipleValueString.getName());
        javaTypeToFixTypes.put(String.class, FieldType.Exchange.getName());
        javaTypeToFixTypes.put(String.class, FieldType.LocalMktDate.getName());
        javaTypeToFixTypes.put(String.class, FieldType.Data.getName());
        javaTypeToFixTypes.put(String.class, FieldType.MonthYear.getName());
        javaTypeToFixTypes.put(String.class, FieldType.Time.getName());
        javaTypeToFixTypes.put(String.class, FieldType.Country.getName());
        javaTypeToFixTypes.put(String.class, MULTIPLECHARVALUE);
        javaTypeToFixTypes.put(String.class, MULTIPLESTRINGVALUE);
        javaTypeToFixTypes.put(String.class, MONTH_YEAR);
        javaTypeToFixTypes.put(String.class, TZTIMEONLY);
        javaTypeToFixTypes.put(String.class, TZTIMESTAMP);
        javaTypeToFixTypes.put(String.class, DAY_OF_MONTH);
        javaTypeToFixTypes.put(String.class, LANGUAGE);
        javaTypeToFixTypes.put(String.class, XMLDATA);

        javaTypeToFixTypes.put(Character.class, FieldType.Char.getName());
    }

    public FIXDictionaryValidator() {
        super();
    }

    public FIXDictionaryValidator(IDictionaryValidator parentValidator) {
        super(parentValidator);
    }

    @Override
    public List<DictionaryValidationError> validate(IDictionaryStructure dictionary, boolean full, Boolean fieldsOnly) {
        entityNamesToEntityValidators.get(GROUP_ENTITY).setDictionary(dictionary);

        List<DictionaryValidationError> errors = super.validate(dictionary, full, fieldsOnly);
        checkMessageTypes(errors, dictionary);
        checkRequiredMessages(errors, dictionary);
        checkTagDuplicates(errors, dictionary);

        entityNamesToEntityValidators.get(GROUP_ENTITY).setDictionary(null);
        return errors;
    }

    @Override
    public List<DictionaryValidationError> validate(IDictionaryStructure dictionary, IMessageStructure message, boolean full) {
        List<DictionaryValidationError> errors = super.validate(dictionary, message, full);

        validateEntity(errors, message);

        return errors;
    }

    @Override
    public List<DictionaryValidationError> validate(IMessageStructure message, IFieldStructure field) {
        List<DictionaryValidationError> errors = super.validate(message, field);

        if(message == null && !field.isComplex()) {

            if(field.getAttributes().isEmpty()) {
                errors.add(new DictionaryValidationError(null, field.getName(),
                        "Field  <strong>\"" + field.getName() + "\"</strong> doesn't contain attributes",
                        DictionaryValidationErrorLevel.FIELD, DictionaryValidationErrorType.ERR_ATTRIBUTES));
            } else {
                checkTag(errors, null, field);
                checkFixType(errors, null, field);
            }
        }

        return errors;
    }

    private void checkTagDuplicates(List<DictionaryValidationError> errors, IDictionaryStructure dictionary) {
        Multimap<Integer, FieldHolder> tags = ArrayListMultimap.create();

        Set<String> references = dictionary.getMessageStructures().stream()
                .flatMap(message ->
                        message.getFields().stream()
                                .peek(field -> addTagToMap(tags, message, field))
                                .map(IFieldStructure::getReferenceName)
                                .filter(Objects::nonNull))
                .collect(Collectors.toSet());

        dictionary.getFieldStructures().stream()
                .filter(field -> !references.contains(field.getName()))
                .forEach(field -> addTagToMap(tags, null, field));

        Map<Integer, Set<String>> duplicatedTags = new HashMap<>();

        tags.asMap().forEach((tag, fieldHolders) -> {
            Set<String> fieldsWithTag = fieldHolders.stream()
                    .map(FieldHolder::getFieldName)
                    .collect(Collectors.toSet());
            if (fieldsWithTag.size() > 1) {
                duplicatedTags.put(tag, fieldsWithTag);
            }
        });

        errors.addAll(duplicatedTags.keySet().stream()
                .flatMap(tag -> tags.get(tag).stream()
                        .filter(pair -> duplicatedTags.get(tag).contains(pair.getFieldName()))
                        .map(pair -> new DictionaryValidationError(pair.getMessageName(), pair.getFieldName(), errorMessageForDuplicatedTag(tag, pair.getMessageName(), pair.getFieldName()),
                                DictionaryValidationErrorLevel.FIELD, DictionaryValidationErrorType.ERR_ATTRIBUTES)))
                .collect(Collectors.toList()));
    }

    private String errorMessageForDuplicatedTag(Integer tag, String messageName, String fieldName) {
        String msg = String.format("Duplicated tag %d in filed %s", tag, fieldName);
        if (messageName != null) {
            msg += " in message " + messageName;
        }
        return msg;
    }

    private void addTagToMap(Multimap<Integer, FieldHolder> tags, IMessageStructure message, IFieldStructure field) {
        Integer tag = (Integer) field.getAttributeValueByName(FixMessageHelper.ATTRIBUTE_TAG);
        if (tag != null) {
            tags.put(tag, new FieldHolder(message == null ? null : message.getName(), field.getName()));
        }
    }

    private void checkMessageTypes(List<DictionaryValidationError> errors, IDictionaryStructure dictionary) {
        Set<Object> messageTypes = new HashSet<>();

        for(IMessageStructure message : dictionary.getMessageStructures()) {
            String entity_type = (String) message.getAttributeValueByName(ATTRIBUTE_ENTITY_TYPE);

            if(entity_type != null && entity_type.equals(MESSAGE_ENTITY)) {
                Object messageTypeAttribute = message.getAttributeValueByName(MESSAGE_TYPE_ATTR_NAME);

                if(messageTypeAttribute != null) {

                    if(!messageTypes.add(messageTypeAttribute)) {
                        errors.add(new DictionaryValidationError(message.getName(), null,
                                MESSAGE_TYPE_ATTR_NAME + " attribute is not unique in <strong>\""
                                        + message.getName() + "\"</strong> message",
                                DictionaryValidationErrorLevel.MESSAGE, DictionaryValidationErrorType.ERR_ATTRIBUTES));
                    }

                    IFieldStructure messageTypeEnum =
                            dictionary.getFieldStructure(MSG_TYPE_FIELD);

                    Map<String, IAttributeStructure> messageTypeValues = messageTypeEnum.getValues();

                    for(IAttributeStructure messageTypeValue : messageTypeValues.values()) {
                        if(messageTypeValue.getValue().equals(messageTypeAttribute.toString())) {
                            return;
                        }
                    }

                    errors.add(new DictionaryValidationError(message.getName(), null,
                            MESSAGE_TYPE_ATTR_NAME
                                    + " attribute value is not exist in enum. Value [" + messageTypeAttribute + "]",
                            DictionaryValidationErrorLevel.MESSAGE, DictionaryValidationErrorType.ERR_ATTRIBUTES));

                }
            }
        }

    }

    private void checkRequiredMessages(List<DictionaryValidationError> errors, IDictionaryStructure dictionary) {
        ValidationHelper.checkRequiredMessageExistence(errors, dictionary, HEARTBEAT_MESSAGE);
        ValidationHelper.checkRequiredMessageExistence(errors, dictionary, TESTREQUEST_MESSAGE);
        ValidationHelper.checkRequiredMessageExistence(errors, dictionary, LOGON_MESSAGE);
        ValidationHelper.checkRequiredMessageExistence(errors, dictionary, LOGOUT_MESSAGE);
        ValidationHelper.checkRequiredMessageExistence(errors, dictionary, REJECT_MESSAGE);
        ValidationHelper.checkRequiredMessageExistence(errors, dictionary, RESENDREQUEST_MESSAGE);
        ValidationHelper.checkRequiredMessageExistence(errors, dictionary, SEQUENCERESET_MESSAGE);
        ValidationHelper.checkRequiredMessageExistence(errors, dictionary, HEADER);
        ValidationHelper.checkRequiredMessageExistence(errors, dictionary, TRAILER);
    }

    private void validateEntity(List<DictionaryValidationError> errors, IMessageStructure message) {
        if(message.getAttributes().isEmpty()) {
            errors.add(new DictionaryValidationError(message.getName(), null,
                    "Message  <strong>\"" + message.getName() + "\"</strong> doesn't contain attributes",
                    DictionaryValidationErrorLevel.MESSAGE, DictionaryValidationErrorType.ERR_ATTRIBUTES));
        } else {

            String entityType = (String) message.getAttributeValueByName(ATTRIBUTE_ENTITY_TYPE);

            if(entityType == null) {
                errors.add(new DictionaryValidationError(message.getName(), null, "Message  <strong>\""
                        + message.getName() + "\"</strong> doesn't contain <strong>\""
                        + ATTRIBUTE_ENTITY_TYPE +"\"</strong> attribute",
                        DictionaryValidationErrorLevel.MESSAGE, DictionaryValidationErrorType.ERR_ATTRIBUTES));
                return;
            }

            EntityValidator entityValidator = entityNamesToEntityValidators.get(entityType);

            if(entityValidator == null) {
                errors.add(new DictionaryValidationError(message.getName(), null, "Message  <strong>\""
                        + message.getName() + "\"</strong> contains incorrect value <strong>\""
                        + entityType + "\"</strong> for <strong>\"" + ATTRIBUTE_ENTITY_TYPE
                        + "\"</strong> attribute",
                        DictionaryValidationErrorLevel.MESSAGE, DictionaryValidationErrorType.ERR_ATTRIBUTES));
                return;
            }

            entityValidator.validateEntity(errors, message);
        }
    }

    private void checkTag(List<DictionaryValidationError> errors, IMessageStructure message,
                          IFieldStructure field) {

        if(ValidationHelper.checkFieldAttributeType(errors, field, FixMessageHelper.ATTRIBUTE_TAG,
                JavaType.JAVA_LANG_INTEGER, null)) {

            Integer tag = (Integer) field.getAttributeValueByName(FixMessageHelper.ATTRIBUTE_TAG);

            if (tag == null) {
                errors.add(new DictionaryValidationError(message == null ? null : message.getName(), field.getName(),
                        "Field  <strong>\"" + field.getName() + "\"</strong> doesn't contain <strong>\""
                                + FixMessageHelper.ATTRIBUTE_TAG + "\"</strong> attribute",
                        DictionaryValidationErrorLevel.FIELD, DictionaryValidationErrorType.ERR_ATTRIBUTES));
            }
        }
    }

    private void checkFixType(List<DictionaryValidationError> errors, IMessageStructure message,
                              IFieldStructure field) {
        String fixType = (String) field.getAttributeValueByName(QFJDictionaryAdapter.ATTRIBUTE_FIX_TYPE);

        if(fixType == null) {
            errors.add(new DictionaryValidationError(message == null ? null : message.getName(), field.getName(),
                    "Field  <strong>\"" + field.getName() + "\"</strong> doesn't contain <strong>\""
                            + QFJDictionaryAdapter.ATTRIBUTE_FIX_TYPE + "\"</strong> attribute",
                    DictionaryValidationErrorLevel.FIELD, DictionaryValidationErrorType.ERR_ATTRIBUTES));
        } else {
            checkJavaType(errors, message, field, fixType, field.getJavaType().value());
        }
    }

    private void checkJavaType(List<DictionaryValidationError> errors, IMessageStructure message,
                               IFieldStructure field, String fixType, String javaType) {
        try {
            Class<?> javaTypeClass = Class.forName(javaType);

            if(!javaTypeToFixTypes.get(javaTypeClass).contains(fixType)) {
                errors.add(new DictionaryValidationError(message == null ? null : message.getName(), field.getName(),
                        "Field  <strong>\"" + field.getName() + "\"</strong>  contains  incorrect value <strong>\""
                                + fixType + "\"</strong> for <strong>\""
                                + QFJDictionaryAdapter.ATTRIBUTE_FIX_TYPE + "\"</strong> attribute",
                        DictionaryValidationErrorLevel.FIELD, DictionaryValidationErrorType.ERR_ATTRIBUTES));
            }
        } catch (ClassNotFoundException e) {
            errors.add(new DictionaryValidationError(message == null ? null : message.getName(), field.getName(),
                    "Field  <strong>\"" + field.getName() + "\"</strong>  contains  incorrect java type for <strong>\""
                            + QFJDictionaryAdapter.ATTRIBUTE_FIX_TYPE + "\"</strong> attribute",
                    DictionaryValidationErrorLevel.FIELD, DictionaryValidationErrorType.ERR_ATTRIBUTES));
        }
    }

    private static class FieldHolder {
        private String messageName;
        private String fieldName;

        public FieldHolder(String messageName, String fieldName) {
            this.messageName = messageName;
            this.fieldName = fieldName;
        }

        public String getMessageName() {
            return messageName;
        }

        public String getFieldName() {
            return fieldName;
        }
    }
}
