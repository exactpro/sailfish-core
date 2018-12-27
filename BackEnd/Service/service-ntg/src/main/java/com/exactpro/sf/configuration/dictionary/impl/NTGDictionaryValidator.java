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

import static com.exactpro.sf.services.ntg.NTGMessageHelper.ATTRIBUTE_MESSAGE_TYPE;
import static com.exactpro.sf.services.ntg.NTGMessageHelper.FIELD_MESSAGE_LENGTH;
import static com.exactpro.sf.services.ntg.NTGMessageHelper.FIELD_START_OF_MESSAGE;
import static com.exactpro.sf.services.ntg.NTGMessageHelper.MESSAGE_HEADER;
import static com.exactpro.sf.services.ntg.NTGMessageHelper.MESSAGE_HEARTBEAT;
import static com.exactpro.sf.services.ntg.NTGMessageHelper.MESSAGE_LOGON;
import static com.exactpro.sf.services.ntg.NTGMessageHelper.MESSAGE_REJECT;
import static com.exactpro.sf.services.ntg.NTGMessageHelper.MESSAGE_TYPE;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
import com.exactpro.sf.services.MessageHelper;
import com.exactpro.sf.services.ntg.NTGProtocolAttribute;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

public class NTGDictionaryValidator extends AbstractDictionaryValidator {

    private static final long serialVersionUID = 373835755082737945L;

    protected static final int lengthByte = 1;
    protected static final int lengthShort = 2;
    protected static final int lengthInt = 4;
    protected static final int lengthFloat = 4;
    protected static final int lengthDouble = 8;
    protected static final int lengthBigDecimal = 8;
    protected static final int lengthLong = 8;

    public NTGDictionaryValidator() {
        super();
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

        checkRequiredAttributes(errors, message);

        return errors;
    }

    @Override
    public List<DictionaryValidationError> validate(IMessageStructure message, IFieldStructure field) {
        List<DictionaryValidationError> errors = super.validate(message, field);

        if((message == null && !field.isEnum())
                || (message != null && !field.isComplex())) {

            boolean containLength = ValidationHelper.checkRequiredFieldAttribute(errors, message, field,
                    NTGProtocolAttribute.Length.toString());
            boolean containOffset = ValidationHelper.checkRequiredFieldAttribute(errors, message, field,
                    NTGProtocolAttribute.Offset.toString());

            if (containLength && containOffset) {
                checkFormat(errors, message, field);
                checkFieldOffsets(errors, message, field);
            }
        }

        return errors;
    }

    private void checkFieldOffsets(List<DictionaryValidationError> errors, IMessageStructure message, IFieldStructure structure) {

        long sum = 0;
        for (IFieldStructure field : message.getFields()) {

            if (structure.getName().equals(field.getName())) {
                int offset = (int) field.getAttributeValueByName("Offset");
                if (sum != offset) {

                    String errorText = String.format("Offset attribute is incorrect. actual - %s %s - expected", offset, sum);
                    DictionaryValidationError error = new DictionaryValidationError(message.getName(), field.getName(), errorText, DictionaryValidationErrorLevel.FIELD,
                            DictionaryValidationErrorType.ERR_ATTRIBUTES);
                    errors.add(error);
                }
                break;
            }

            sum += (int) field.getAttributeValueByName("Length");
        }
    }

    private void checkRequiredAttributes(List<DictionaryValidationError> errors, IMessageStructure message) {

        if (message.getName().equals(MESSAGE_HEADER)) {
            ValidationHelper.checkRequiredField(errors, message, FIELD_MESSAGE_LENGTH);
            ValidationHelper.checkRequiredField(errors, message, MESSAGE_TYPE);
            ValidationHelper.checkRequiredField(errors, message, FIELD_START_OF_MESSAGE);
        }

        if(!message.getAttributes().isEmpty()) {
            if (message.getName().equals(MESSAGE_HEADER)) {
                ValidationHelper.checkRequiredAttribute(errors, message, NTGProtocolAttribute.Length.toString());
                ValidationHelper.checkRequiredAttribute(errors, message, NTGProtocolAttribute.Offset.toString());
    
            } else {
                ValidationHelper.checkRequiredAttribute(errors, message, MESSAGE_TYPE);
                ValidationHelper.checkRequiredAttribute(errors, message, MessageHelper.ATTRIBUTE_IS_ADMIN);
            }
        }
    }

    private void checkFormat(List<DictionaryValidationError> errors, IMessageStructure message,
                             IFieldStructure field) {
        JavaType javaType = field.getJavaType();
        Integer length = (Integer) field.getAttributeValueByName(NTGProtocolAttribute.Length.toString());

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
                        new Integer[] {
                                lengthByte, lengthShort, lengthInt
                        });
                break;

            case JAVA_LANG_LONG:
                checkLengthFromPossibleValues(errors, message, field, length,
                        new Integer[] {
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
                if (length != lengthLong) {
                    addProtocolTypeError(errors, message, field, lengthLong, length);
                }
                break;

            default:
                errors.add(new DictionaryValidationError(message == null ? null : message.getName(), field.getName(),
                        "Incorrect JavaType:<strong>" + javaType + "</strong>",
                        DictionaryValidationErrorLevel.FIELD, DictionaryValidationErrorType.ERR_FIELD_TYPE));
                break;
        }
    }

    private void addProtocolTypeError(List<DictionaryValidationError> errors, IMessageStructure message,
                                      IFieldStructure field,int expectedLength, int actualLength) {
        errors.add(new DictionaryValidationError(message == null ? null : message.getName(), field.getName(),
                "Attribute <strong>\"Length\"</strong> has incorrect value = ["
                        + actualLength + "]. Must be [" + expectedLength + "]",
                DictionaryValidationErrorLevel.FIELD, DictionaryValidationErrorType.ERR_ATTRIBUTES));

    }

    private void checkLengthFromPossibleValues(List<DictionaryValidationError> errors, IMessageStructure message,
                                               IFieldStructure field, Integer actualLength, Integer[] possibleValues) {

        for(Integer possibleValue : possibleValues) {
            if(possibleValue.equals(actualLength)) {
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
                "There is no implementation in visitors for "  + javaType + " type",
                DictionaryValidationErrorLevel.FIELD, DictionaryValidationErrorType.ERR_ATTRIBUTES));

    }

    private void checkMessageTypes(List<DictionaryValidationError> errors, IDictionaryStructure dictionary) {
//        Set<Object> messageTypes = new HashSet<>();
        SetMultimap<Object, Boolean> test = HashMultimap.create();
        outerLoop:
        for(IMessageStructure message : dictionary.getMessageStructures()) {

            if(message.getAttributeNames().contains(ATTRIBUTE_MESSAGE_TYPE)) {
                Object messageTypeAttribute = message.getAttributeValueByName(ATTRIBUTE_MESSAGE_TYPE);
                Boolean isIncoming = (Boolean) message.getAttributeValueByName("IsInput");
                if(messageTypeAttribute != null) {

                    if(!test.put(messageTypeAttribute, isIncoming)) {
                        errors.add(new DictionaryValidationError(message.getName(), null,
                                ATTRIBUTE_MESSAGE_TYPE + " attribute is not unique in <strong>\""
                                        + message.getName() + "\"</strong> message",
                                DictionaryValidationErrorLevel.MESSAGE, DictionaryValidationErrorType.ERR_ATTRIBUTES));
                    }

                    IFieldStructure messageTypeEnum =
                            dictionary.getFieldStructure(ATTRIBUTE_MESSAGE_TYPE);

                    if (messageTypeEnum != null) {

	                    Map<String, IAttributeStructure> messageTypeValues = messageTypeEnum.getValues();


	                    Byte byteValue = Byte.parseByte(messageTypeAttribute.toString());

	                    if(byteValue < 0) { // for messageType with UByte value;
	                        continue;
	                    }

	                    for(IAttributeStructure messageTypeValue : messageTypeValues.values()) {
	                        Byte asciiValue = (byte)messageTypeValue.getValue().charAt(0);

	                        if(asciiValue.equals(byteValue)) {
	                            continue outerLoop;
	                        }
	                    }

	                    if(!message.getName().equals(MESSAGE_HEARTBEAT)) {

	                        errors.add(new DictionaryValidationError(message.getName(), null,
	                                ATTRIBUTE_MESSAGE_TYPE
	                                        + " attribute value is not exist in enum. Value [" + messageTypeAttribute + "]",
	                                DictionaryValidationErrorLevel.MESSAGE, DictionaryValidationErrorType.ERR_ATTRIBUTES));

	                    }

                    } else {

                    	String errMsg = "Dictionary doesn't contain <strong>" + ATTRIBUTE_MESSAGE_TYPE + "</strong> field";

                    	if (!ValidationHelper.checkErrorExistByText(errors, errMsg)) {
                    		errors.add(new DictionaryValidationError(null, null, errMsg,
                    				DictionaryValidationErrorLevel.DICTIONARY, DictionaryValidationErrorType.ERR_REQUIRED_FIELD));
                    	}
                    }
                }
            }

        }
    }

}
