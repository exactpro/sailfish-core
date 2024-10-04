/******************************************************************************
 * Copyright 2009-2020 Exactpro (Exactpro Systems Limited)
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

import com.exactpro.sf.common.impl.messages.xml.configuration.JavaType;
import com.exactpro.sf.common.messages.structures.IAttributeStructure;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.messages.structures.StructureUtils;
import com.exactpro.sf.configuration.dictionary.DictionaryValidationError;
import com.exactpro.sf.configuration.dictionary.DictionaryValidationErrorLevel;
import com.exactpro.sf.configuration.dictionary.DictionaryValidationErrorType;
import com.exactpro.sf.configuration.dictionary.ValidationHelper;
import com.exactpro.sf.configuration.dictionary.interfaces.IDictionaryValidator;
import com.exactpro.sf.services.MessageHelper;
import com.exactpro.sf.services.ntg.NTGFieldFormat;
import com.exactpro.sf.services.ntg.NTGProtocolAttribute;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.exactpro.sf.common.messages.structures.StructureUtils.getAttributeValue;
import static com.exactpro.sf.services.MessageHelper.FIELD_MESSAGE_TYPE;
import static com.exactpro.sf.services.ntg.NTGMessageHelper.ATTRIBUTE_MESSAGE_TYPE;
import static com.exactpro.sf.services.ntg.NTGMessageHelper.FIELD_MESSAGE_LENGTH;
import static com.exactpro.sf.services.ntg.NTGMessageHelper.FIELD_START_OF_MESSAGE;
import static com.exactpro.sf.services.ntg.NTGMessageHelper.MESSAGE_HEADER;
import static com.exactpro.sf.services.ntg.NTGMessageHelper.MESSAGE_HEARTBEAT;
import static com.exactpro.sf.services.ntg.NTGMessageHelper.MESSAGE_LOGON;
import static com.exactpro.sf.services.ntg.NTGMessageHelper.MESSAGE_REJECT;
import static com.exactpro.sf.services.ntg.NTGMessageHelper.MESSAGE_TYPE;

public class NTGDictionaryValidator extends AbstractDictionaryValidator {

    private static final long serialVersionUID = 373835755082737945L;

    protected static final int lengthByte = 1;
    protected static final int lengthShort = 2;
    protected static final int lengthInt = 4;
    protected static final int lengthFloat = 4;
    protected static final int lengthDouble = 8;
    protected static final int lengthBigDecimal = 8;
    protected static final int lengthLong = 8;
    protected static final int[] lengthLocalDateTime = {24};

    private static final String UINT8 = "UInt8";
    private static final String UINT16 = "UInt16";
    private static final String UINT32 = "UInt32";
    private static final String UINT64 = "Uint64";

    private static final ImmutableBiMap<JavaType, String> POSSIBLE_ATTRIBUTE_TYPE_FOR_FIELD_TYPE = ImmutableBiMap.<JavaType, String>builder()
            .put(JavaType.JAVA_LANG_SHORT, UINT8)
            .put(JavaType.JAVA_LANG_INTEGER, UINT16)
            .put(JavaType.JAVA_LANG_LONG, UINT32)
            .put(JavaType.JAVA_MATH_BIG_DECIMAL, UINT64)
            .build();

    private final Multimap<JavaType, String> typeAllowedFormats = ImmutableSetMultimap.<JavaType, String>builder()
            .putAll(JavaType.JAVA_LANG_STRING, NTGFieldFormat.A.name(), NTGFieldFormat.D.name())
            .build();

    public NTGDictionaryValidator() {
    }

    public NTGDictionaryValidator(IDictionaryValidator parentValidator) {
        super(parentValidator);
    }

    @Override
    public List<DictionaryValidationError> validate(IDictionaryStructure dictionary, boolean full, Boolean fieldsOnly) {
        List<DictionaryValidationError> errors = super.validate(dictionary, full, fieldsOnly);

        ValidationHelper.checkRequiredMessageExistence(errors, dictionary, MESSAGE_LOGON);
        ValidationHelper.checkRequiredMessageExistence(errors, dictionary, MESSAGE_HEARTBEAT);
        ValidationHelper.checkRequiredMessageExistence(errors, dictionary, MESSAGE_REJECT);
        ValidationHelper.checkRequiredMessageExistence(errors, dictionary, MESSAGE_HEADER);

        checkMessageTypes(errors, dictionary);

        return errors;
    }

    @Override
    public List<DictionaryValidationError> validate(IDictionaryStructure dictionary, IMessageStructure message, boolean full) {
        List<DictionaryValidationError> errors = super.validate(dictionary, message, full);

        checkRequiredFields(errors, message);
        checkRequiredAttributes(errors, message);

        return errors;
    }

    @Override
    public List<DictionaryValidationError> validate(IMessageStructure message, IFieldStructure field) {
        List<DictionaryValidationError> errors = super.validate(message, field);

        if (message != null && !field.isComplex()) {

            boolean containLength = ValidationHelper.checkRequiredFieldAttribute(errors, message, field,
                    NTGProtocolAttribute.Length.toString());
            boolean containOffset = ValidationHelper.checkRequiredFieldAttribute(errors, message, field,
                    NTGProtocolAttribute.Offset.toString());

            if (containLength && containOffset) {
                checkLength(errors, message, field);
                checkFieldOffsets(errors, message, field);
            }

            if (containLength) {
                checkCompatibilityLengthAndAttributeType(errors, message, field);
            }

            if (typeAllowedFormats.containsKey(field.getJavaType())) {
                checkFormatAttribute(errors, message, field, typeAllowedFormats.get(field.getJavaType()));
            }
        }

        return errors;
    }


    private void checkFormatAttribute(List<DictionaryValidationError> errors, IMessageStructure message, IFieldStructure field, Collection<String> allowedFormats) {
        if (ValidationHelper.checkRequiredFieldAttribute(errors, message, field, NTGProtocolAttribute.Format.name())) {
            String format = getAttributeValue(field, NTGProtocolAttribute.Format.name());
            if (!allowedFormats.contains(format)) {
                errors.add(
                        new DictionaryValidationError(message == null ? null : message.getName(), field.getName(),
                                String.format("Attribute <strong>%s</strong> must have one of the next values [%s] for field with type [%s] but has [%s]", NTGProtocolAttribute.Format,
                                        String.join(",", allowedFormats), field.getJavaType().value(), format),
                                DictionaryValidationErrorLevel.FIELD, DictionaryValidationErrorType.ERR_ATTRIBUTES)
                );
            }
        }
    }

    private void checkFieldOffsets(List<DictionaryValidationError> errors, IMessageStructure message, IFieldStructure structure) {

        long sum = 0;
        for (IFieldStructure field : message.getFields().values()) {

            if (structure.getName().equals(field.getName())) {
                int offset = getAttributeValue(field, NTGProtocolAttribute.Offset.name());
                if (sum != offset) {

                    String errorText = String.format("Offset attribute is incorrect. actual - %s; expected - %s", offset, sum);
                    DictionaryValidationError error = new DictionaryValidationError(message.getName(), field.getName(), errorText, DictionaryValidationErrorLevel.FIELD,
                            DictionaryValidationErrorType.ERR_ATTRIBUTES);
                    errors.add(error);
                }
                break;
            }

            sum += StructureUtils.<Integer>getAttributeValue(field, NTGProtocolAttribute.Length.name());
        }
    }

    private void checkRequiredAttributes(List<DictionaryValidationError> errors, IMessageStructure message) {
        if (!message.getAttributes().isEmpty()) {
            if (message.getName().equals(MESSAGE_HEADER)) {
                ValidationHelper.checkRequiredAttribute(errors, message, NTGProtocolAttribute.Length.toString());
                ValidationHelper.checkRequiredAttribute(errors, message, NTGProtocolAttribute.Offset.toString());

            } else {
                ValidationHelper.checkRequiredAttribute(errors, message, ATTRIBUTE_MESSAGE_TYPE);
                ValidationHelper.checkRequiredAttribute(errors, message, MessageHelper.ATTRIBUTE_IS_ADMIN);
            }
        }
    }

    private void checkRequiredFields(List<DictionaryValidationError> errors, IMessageStructure message) {
        if (message.getName().equals(MESSAGE_HEADER)) {
            ValidationHelper.checkRequiredField(errors, message, FIELD_MESSAGE_LENGTH);
            ValidationHelper.checkRequiredField(errors, message, MESSAGE_TYPE);
            ValidationHelper.checkRequiredField(errors, message, FIELD_START_OF_MESSAGE);
        }
    }

    private void checkLength(List<DictionaryValidationError> errors, IMessageStructure message,
                             IFieldStructure field) {
        JavaType javaType = field.getJavaType();
        Integer length = getAttributeValue(field, NTGProtocolAttribute.Length.toString());

        switch (javaType) {
            case JAVA_LANG_BOOLEAN:
                addNoImplementationInVisitorsError(errors, message, field, javaType);
                break;

            case JAVA_LANG_BYTE:
                if (length != lengthByte) {
                    addProtocolTypeError(errors, message, field, lengthByte, length);
                }
                break;

            case JAVA_LANG_CHARACTER:
                addNoImplementationInVisitorsError(errors, message, field, javaType);
                break;

            case JAVA_LANG_DOUBLE:
                if (length != lengthDouble) {
                    addProtocolTypeError(errors, message, field, lengthDouble, length);
                }
                break;

            case JAVA_LANG_FLOAT:
                if (length != lengthFloat) {
                    addProtocolTypeError(errors, message, field, lengthFloat, length);
                }
                break;

            case JAVA_LANG_INTEGER:
                checkLengthFromPossibleValues(errors, message, field, length,
                        new Integer[]{
                                lengthByte, lengthShort, lengthInt
                        });
                break;

            case JAVA_LANG_LONG:
                checkLengthFromPossibleValues(errors, message, field, length,
                        new Integer[]{
                                lengthInt, lengthLong
                        });
                break;

            case JAVA_LANG_SHORT:
                addNoImplementationInVisitorsError(errors, message, field, javaType);
                break;

            case JAVA_LANG_STRING:
                break;

            case JAVA_MATH_BIG_DECIMAL:
                if (length != lengthBigDecimal) {
                    addProtocolTypeError(errors, message, field, lengthBigDecimal, length);
                }
                break;

            case JAVA_TIME_LOCAL_DATE_TIME:
                if (!isCorrectLength(length, lengthLocalDateTime)) {
                    addProtocolTypeError(errors, message, field, lengthLocalDateTime, length);
                }
                break;

            default:
                errors.add(new DictionaryValidationError(message == null ? null : message.getName(), field.getName(),
                        "Incorrect JavaType:<strong>" + javaType + "</strong>",
                        DictionaryValidationErrorLevel.FIELD, DictionaryValidationErrorType.ERR_FIELD_TYPE));
                break;
        }
    }

    private boolean isCorrectLength(int length, int[] lengthLocalDateTime) {
        return ArrayUtils.contains(lengthLocalDateTime, length);
    }

    private void addProtocolTypeError(List<DictionaryValidationError> errors, IMessageStructure message,
                                      IFieldStructure field, int expectedLength, int actualLength) {
        errors.add(new DictionaryValidationError(message == null ? null : message.getName(), field.getName(),
                "Attribute <strong>\"Length\"</strong> has incorrect value = ["
                        + actualLength + "]. Must be [" + expectedLength + "]",
                DictionaryValidationErrorLevel.FIELD, DictionaryValidationErrorType.ERR_ATTRIBUTES));

    }

    private void addProtocolTypeError(List<DictionaryValidationError> errors, IMessageStructure message,
                                      IFieldStructure field, int[] expectedLength, int actualLength) {
        errors.add(new DictionaryValidationError(message == null ? null : message.getName(), field.getName(),
                "Attribute <strong>\"Length\"</strong> has incorrect value = ["
                        + actualLength + "]. Must be [" + Arrays.toString(expectedLength) + "]",
                DictionaryValidationErrorLevel.FIELD, DictionaryValidationErrorType.ERR_ATTRIBUTES));

    }

    private void checkLengthFromPossibleValues(List<DictionaryValidationError> errors, IMessageStructure message,
                                               IFieldStructure field, Integer actualLength, Integer[] possibleValues) {

        for (Integer possibleValue : possibleValues) {
            if (possibleValue.equals(actualLength)) {
                return;
            }
        }

        errors.add(new DictionaryValidationError(message == null ? null : message.getName(), field.getName(),
                "Attribute <strong>\"Length\"</strong> has incorrect value = ["
                        + actualLength + "]. Must be one of " + Arrays.toString(possibleValues),
                DictionaryValidationErrorLevel.FIELD, DictionaryValidationErrorType.ERR_ATTRIBUTES));

    }

    private void addNoImplementationInVisitorsError(List<DictionaryValidationError> errors, IMessageStructure message,
                                                    IFieldStructure field, JavaType javaType) {
        errors.add(new DictionaryValidationError(message == null ? null : message.getName(), field.getName(),
                "There is no implementation in visitors for " + javaType + " type",
                DictionaryValidationErrorLevel.FIELD, DictionaryValidationErrorType.ERR_ATTRIBUTES));

    }

    private void checkMessageTypes(List<DictionaryValidationError> errors, IDictionaryStructure dictionary) {
        SetMultimap<Object, Boolean> test = HashMultimap.create();

        IFieldStructure messageTypeEnum = dictionary.getFields().get(FIELD_MESSAGE_TYPE);
        if (messageTypeEnum == null) {
            errors.add(new DictionaryValidationError(null, null, "Dictionary doesn't contain <strong>" + FIELD_MESSAGE_TYPE + "</strong> field",
                    DictionaryValidationErrorLevel.DICTIONARY, DictionaryValidationErrorType.ERR_REQUIRED_FIELD));
            return;
        }
        Map<String, IAttributeStructure> messageTypeValues = messageTypeEnum.getValues();

        for (IMessageStructure message : dictionary.getMessages().values()) {

            if (message.getAttributes().containsKey(ATTRIBUTE_MESSAGE_TYPE)) {
                Object messageTypeAttribute = getAttributeValue(message, ATTRIBUTE_MESSAGE_TYPE);
                Boolean isIncoming = getAttributeValue(message, "IsInput");
                if (messageTypeAttribute != null) {

                    if (!test.put(messageTypeAttribute, isIncoming)) {
                        errors.add(new DictionaryValidationError(message.getName(), null,
                                ATTRIBUTE_MESSAGE_TYPE + " attribute is not unique in <strong>\""
                                        + message.getName() + "\"</strong> message",
                                DictionaryValidationErrorLevel.MESSAGE, DictionaryValidationErrorType.ERR_ATTRIBUTES));
                    }

                    Byte byteValue = Byte.parseByte(messageTypeAttribute.toString());

                    if (byteValue < 0) { // for messageType with UByte value;
                        continue;
                    }

                    boolean valueMatched = messageTypeValues.values().stream()
                            .map(attr -> (byte) attr.getValue().charAt(0))
                            .anyMatch(byteValue::equals);
                    if (valueMatched) {
                        continue;
                    }

                    if (!message.getName().equals(MESSAGE_HEARTBEAT)) {

                        errors.add(new DictionaryValidationError(message.getName(), null,
                                ATTRIBUTE_MESSAGE_TYPE
                                        + " attribute value is not exist in enum. Value [" + messageTypeAttribute + "]",
                                DictionaryValidationErrorLevel.MESSAGE, DictionaryValidationErrorType.ERR_ATTRIBUTES));
                    }
                }
            }

        }
    }

    private void checkCompatibilityLengthAndAttributeType(List<DictionaryValidationError> errors, IMessageStructure message, IFieldStructure field) {

        String attributeType = getAttributeValue(field, NTGProtocolAttribute.Type.toString());

        if (attributeType == null) {
            return;
        }

        Integer length = getAttributeValue(field, NTGProtocolAttribute.Length.toString());

        switch (attributeType) {
            case UINT8:
                checkAttributeTypeSatisfyFieldType(errors, message, field, attributeType);

                if (length == lengthByte) {
                    return;
                }
                break;

            case UINT16:
                checkAttributeTypeSatisfyFieldType(errors, message, field, attributeType);

                if (length == lengthShort) {
                    return;
                }
                break;

            case UINT32:
                checkAttributeTypeSatisfyFieldType(errors, message, field, attributeType);

                if (length == lengthInt) {
                    return;
                }
                break;

            case UINT64:
                checkAttributeTypeSatisfyFieldType(errors, message, field, attributeType);

                if (length == lengthLong) {
                    return;
                }
                break;

            default:
                return;

        }
        errors.add(
                new DictionaryValidationError(message == null ? null : message.getName(), field.getName(),
                        String.format("Attribute <strong>\"Type\"</strong> value [%s] has length different from the attribute <strong>\"Length\"</strong> value = [%s]",
                                attributeType, length),
                        DictionaryValidationErrorLevel.FIELD, DictionaryValidationErrorType.ERR_ATTRIBUTES));
    }

    private void checkAttributeTypeSatisfyFieldType(List<DictionaryValidationError> errors, IMessageStructure message,
            IFieldStructure field, String attributeType) {

        JavaType fieldType = field.getJavaType();
        String errorMessage = null;
        if (fieldType == JavaType.JAVA_LANG_INTEGER) {
            if (!attributeType.equals(UINT8) && !attributeType.equals(UINT16)) {
                errorMessage = String.format("Use [%s, %s] for Type attribute with current field's type or change field's type to [%s] to use with current Type attribute value",
                        UINT8, UINT16, POSSIBLE_ATTRIBUTE_TYPE_FOR_FIELD_TYPE.inverse().get(attributeType));
            }
        } else {
            String possibleType = POSSIBLE_ATTRIBUTE_TYPE_FOR_FIELD_TYPE.get(fieldType);
            if (!Objects.equals(possibleType, attributeType)) {

                if (fieldType == JavaType.JAVA_LANG_BYTE) {
                    String fieldTypeInStringFormat = field.getJavaType().toString();
                    errorMessage = String.format("Field's type [%s] cannot be matched to any unsigned type. Use [%s] for field's type with current Type attribute",
                            fieldTypeInStringFormat, POSSIBLE_ATTRIBUTE_TYPE_FOR_FIELD_TYPE.inverse().get(attributeType));

                } else {
                    errorMessage = String.format("Use [%s] for Type attribute with current field's type or change field's type to [%s] to use with current Type attribute value",
                            POSSIBLE_ATTRIBUTE_TYPE_FOR_FIELD_TYPE.get(fieldType), POSSIBLE_ATTRIBUTE_TYPE_FOR_FIELD_TYPE.inverse().get(attributeType));
                }
            }
        }

        if (errorMessage != null) {
            errors.add(
                    new DictionaryValidationError(message == null ? null : message.getName(), field.getName(),
                            errorMessage,
                            DictionaryValidationErrorLevel.FIELD, DictionaryValidationErrorType.ERR_ATTRIBUTES));
        }
    }
}
