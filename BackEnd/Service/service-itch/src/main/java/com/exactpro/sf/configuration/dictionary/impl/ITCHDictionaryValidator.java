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
import com.exactpro.sf.services.itch.ITCHMessageHelper;
import com.exactpro.sf.services.itch.ITCHVisitorBase;
import org.apache.commons.lang3.ArrayUtils;

import java.util.*;

public class ITCHDictionaryValidator extends AbstractDictionaryValidator {

    private static final long serialVersionUID = 14671967127441418L;

    public ITCHDictionaryValidator() {
        super();
    }

    public ITCHDictionaryValidator(IDictionaryValidator parentValidator) {
        super(parentValidator);
    }

    @Override
    public List<DictionaryValidationError> validate(IDictionaryStructure dictionary, boolean full, Boolean fieldsOnly) {
        List<DictionaryValidationError> errors = super.validate(dictionary, full, fieldsOnly);

        ValidationHelper.checkRequiredMessageExistence(errors, dictionary, ITCHMessageHelper.MESSAGE_UNIT_HEADER_NAME);

        checkMessageTypes(errors, dictionary);

        return errors;
    }

    @Override
    public List<DictionaryValidationError> validate(IMessageStructure message, IFieldStructure field) {
        List<DictionaryValidationError> errors = super.validate(message, field);

        checkRequiredAttributes(errors, message, field);

        return errors;
    }

    private void checkMessageTypeAttribute(List<DictionaryValidationError> errors, IMessageStructure message, IDictionaryStructure dictionary) {

        if (message.getName().equals(ITCHMessageHelper.MESSAGE_UNIT_HEADER_NAME)) {
            checkRequiredField(errors, message, ITCHMessageHelper.FIELD_MARKET_DATA_GROUP_NAME);
            checkRequiredField(errors, message, ITCHMessageHelper.FIELD_SEQUENCE_NUMBER_NAME);
        }
        
        if(!message.getAttributes().isEmpty()) {
            if (!message.getAttributes().containsKey(ITCHMessageHelper.ATTRIBUTE_MESSAGE_TYPE)
                    && !message.getName().equals(ITCHMessageHelper.MESSAGE_UNIT_HEADER_NAME)
                    && !message.getName().equals(ITCHMessageHelper.MESSAGELIST_NAME)) {
                errors.add(new DictionaryValidationError(message.getName(), null,
                        "Message  <strong>\"" + message.getName() + "\"</strong> doesn't contain MessageType attribute",
                        DictionaryValidationErrorLevel.MESSAGE, DictionaryValidationErrorType.ERR_ATTRIBUTES));
            } else {
                //TODO uncomment this when editing dictionaries was moved to QA
//                if (!message.getName().equals(ITCHMessageHelper.MESSAGELIST_NAME)) {
//                    IFieldStructure lenField = message.getField(ITCHMessageHelper.FIELD_LENGTH_NAME);
//                    if (lenField != null && lenField.getDefaultValue() != null) {
//                        errors.add(new DictionaryValidationError(message.getName(), ITCHMessageHelper.FIELD_LENGTH_NAME,
//                                String.format(
//                                        "Error %s contains not null <strong>\"Length\"</strong> attribute. Codec automatically calculates this field.",
//                                        lenField.getName(), lenField.getDefaultValue()),
//                                DictionaryValidationErrorLevel.FIELD, DictionaryValidationErrorType.ERR_DEFAULT_VALUE));
//                    }
//
//                }
            }
        } else {
            //it's submessage
            if (!isSubMessage(dictionary, message)) {
                errors.add(new DictionaryValidationError(message.getName(), null,
                        "Message  <strong>\"" + message.getName() + "\"</strong> doesn't contain attributes",
                        DictionaryValidationErrorLevel.MESSAGE, DictionaryValidationErrorType.ERR_ATTRIBUTES));
            }
        }
    }

    private boolean isSubMessage(IDictionaryStructure dictionary, IMessageStructure subMessage) {

        for (IMessageStructure message : dictionary.getMessageStructures()) {
            for (IFieldStructure field : message.getFields()) {
                if (field.getReferenceName() != null && field.getReferenceName().equals(subMessage.getName()))
                    return true;
            }
        }
        return false;
    }

    private void checkRequiredField(List<DictionaryValidationError> errors, IMessageStructure message,
                                    String fieldName) {
        if(message.getField(fieldName) == null) {
            errors.add(new DictionaryValidationError(message.getName(), null,
                    "Message  <strong>\"" + message.getName() + "\"</strong> doesn't contain" + fieldName +" field",
                    DictionaryValidationErrorLevel.MESSAGE, DictionaryValidationErrorType.ERR_REQUIRED_FIELD));
        }
    }
    
    private void checkRequiredAttributes(List<DictionaryValidationError> errors, IMessageStructure message,
                                         IFieldStructure field) {
        try {
            if((message == null && !field.isEnum() ) || (message != null && !field.isComplex())) {

                Map<String, IAttributeStructure> attributes = field.getAttributes();

                if(attributes.isEmpty()) {
                    try {
                        if(field.getReferenceName() == null) {
                            errors.add(new DictionaryValidationError(message == null ? null : message.getName(), field.getName(),
                                    "Field " + field.getName() + " doesn't contain <strong>\"Length\"</strong> and <strong>"
                                            + ITCHVisitorBase.TYPE_ATTRIBUTE + "</strong> attributes",
                                    DictionaryValidationErrorLevel.FIELD, DictionaryValidationErrorType.ERR_FIELD_TYPE));
                        }
                    } catch (Exception e) {
                        errors.add(new DictionaryValidationError(message == null ? null : message.getName(), field.getName(),
                                "Error " + field.getName() + " doesn't contain <strong>\"Length\"</strong> and <strong>"
                                        + ITCHVisitorBase.TYPE_ATTRIBUTE + "</strong> attributes",
                                DictionaryValidationErrorLevel.FIELD, DictionaryValidationErrorType.ERR_FIELD_TYPE));
                    }

                    return;
                }

                boolean isTypeAndLengthExist = ValidationHelper.checkRequiredFieldAttributes(errors, message, field,
                        ITCHVisitorBase.TYPE_ATTRIBUTE,
                        ITCHVisitorBase.LENGTH_ATTRIBUTE);

                if(!isTypeAndLengthExist) {
                    
                    return;
                }

                ITCHVisitorBase.ProtocolType type =
                        ITCHVisitorBase.ProtocolType.getEnum((String) field.getAttributeValueByName(ITCHVisitorBase.TYPE_ATTRIBUTE));
                Integer length = (Integer) field.getAttributeValueByName(ITCHVisitorBase.LENGTH_ATTRIBUTE);

                JavaType javaType = field.getJavaType();
                switch (type) {
                    case ALPHA:
                        checkJavaType(errors, message, field, JavaType.JAVA_LANG_STRING, javaType);

                        break;

                    case ALPHA_NOTRIM:
                        checkJavaType(errors, message, field, JavaType.JAVA_LANG_STRING, javaType);

                        break;

                    case DATE:
                        checkJavaTypeArray(errors, message, field, new JavaType[]{
                                JavaType.JAVA_LANG_STRING,
                                JavaType.JAVA_TIME_LOCAL_DATE_TIME}, javaType);

                        if (length != 8) {
                            addProtocolTypeError(errors, message, field, type, 8, length);
                        }

                        break;

                    case DAYS:
                        checkJavaType(errors, message, field, JavaType.JAVA_TIME_LOCAL_DATE_TIME, javaType);

                        if (length != 2) {
                            addProtocolTypeError(errors, message, field, type, 2, length);
                        }

                        break;

                    case TIME:
                        checkJavaTypeArray(errors, message, field, new JavaType[]{
                                JavaType.JAVA_LANG_STRING,
                                JavaType.JAVA_TIME_LOCAL_DATE_TIME}, javaType);
                        if (!ArrayUtils.contains(new int[]{8, 6, 14}, length)) {
                            addProtocolTypeError(errors, message, field, type, 8, length);
                        }

                        break;

                    case PRICE:
                        checkJavaTypeArray(errors, message, field, new JavaType[]{
                                JavaType.JAVA_LANG_FLOAT,
                                JavaType.JAVA_LANG_DOUBLE,
                                JavaType.JAVA_MATH_BIG_DECIMAL}, javaType);

                        if (javaType.equals(JavaType.JAVA_LANG_FLOAT)) {
                            if (length != 4) {
                                addProtocolTypeError(errors, message, field, type, 4, length);
                            }
                        } else if (javaType.equals(JavaType.JAVA_LANG_DOUBLE)
                                || javaType.equals(JavaType.JAVA_MATH_BIG_DECIMAL)) {
                            if (length != 8) {
                                addProtocolTypeError(errors, message, field, type, 8, length);
                            }
                        }

                        break;

                    case PRICE4:
                        checkJavaType(errors, message, field, JavaType.JAVA_LANG_DOUBLE, javaType);

                        if (length != 8) {
                            addProtocolTypeError(errors, message, field, type, 8, length);
                        }

                        break;

                    case SIZE:
                        checkJavaTypeArray(errors, message, field, new JavaType[]{
                                JavaType.JAVA_LANG_DOUBLE,
                                JavaType.JAVA_MATH_BIG_DECIMAL}, javaType);

                        if (length != 8) {
                            addProtocolTypeError(errors, message, field, type, 8, length);
                        }

                        break;

                    case SIZE4:
                        checkJavaType(errors, message, field, JavaType.JAVA_LANG_DOUBLE, javaType);

                        if (length != 8) {
                            addProtocolTypeError(errors, message, field, type, 8, length);
                        }

                        break;

                    case STUB:
                        if (length != 0) {
                            addProtocolTypeError(errors, message, field, type, 0, length);
                        }

                        break;

                    case BYTE:
                        checkJavaTypeArray(errors, message, field, new JavaType[]{
                                JavaType.JAVA_LANG_BYTE,
                                JavaType.JAVA_LANG_SHORT}, javaType);

                        if (length != 1) {
                            addProtocolTypeError(errors, message, field, type, 1, length);
                        }

                        break;

                    case UINT8:
                        checkJavaType(errors, message, field, JavaType.JAVA_LANG_SHORT, javaType);

                        if (length != 1) {
                            addProtocolTypeError(errors, message, field, type, 1, length);
                        }

                        break;

                    case UINT16:
                        checkJavaTypeArray(errors, message, field, new JavaType[]{
                                JavaType.JAVA_LANG_INTEGER,
                                JavaType.JAVA_LANG_SHORT}, javaType);

                        if (length != 2) {
                            addProtocolTypeError(errors, message, field, type, 2, length);
                        }

                        break;

                    case UINT32:
                        checkJavaTypeArray(errors, message, field, new JavaType[]{
                                JavaType.JAVA_LANG_LONG,
                                JavaType.JAVA_MATH_BIG_DECIMAL}, javaType);

                        if (length != 4) {
                            addProtocolTypeError(errors, message, field, type, 4, length);
                        }

                        break;

                    case UINT64:
                        checkJavaTypeArray(errors, message, field, new JavaType[]{
                                JavaType.JAVA_LANG_LONG,
                                JavaType.JAVA_MATH_BIG_DECIMAL}, javaType);

                        if (length != 8) {
                            addProtocolTypeError(errors, message, field, type, 8, length);
                        }

                        break;

                    case UDT:
                        checkJavaTypeArray(errors, message, field, new JavaType[] { JavaType.JAVA_MATH_BIG_DECIMAL, JavaType.JAVA_TIME_LOCAL_DATE_TIME}, javaType);

                        if (length != 8) {
                            addProtocolTypeError(errors, message, field, type, 8, length);
                        }

                        break;
                    case INT8:
                        checkJavaTypeArray(errors, message, field, new JavaType[]{
                                JavaType.JAVA_LANG_INTEGER,
                                JavaType.JAVA_LANG_SHORT,
                                JavaType.JAVA_LANG_BYTE}, javaType);

                        if (length != 1) {
                            addProtocolTypeError(errors, message, field, type, 1, length);
                        }

                        break;

                    case INT16:
                        checkJavaTypeArray(errors, message, field, new JavaType[]{
                                JavaType.JAVA_LANG_INTEGER,
                                JavaType.JAVA_LANG_SHORT}, javaType);

                        if (length != 2) {
                            addProtocolTypeError(errors, message, field, type, 2, length);
                        }

                        break;

                    case INT32:
                        checkJavaTypeArray(errors, message, field, new JavaType[]{
                                JavaType.JAVA_LANG_INTEGER,
                                JavaType.JAVA_LANG_LONG,
                                JavaType.JAVA_MATH_BIG_DECIMAL}, javaType);

                        if (length != 4) {
                            addProtocolTypeError(errors, message, field, type, 4, length);
                        }

                        break;

                    case INT64:
                        checkJavaTypeArray(errors, message, field, new JavaType[]{
                                JavaType.JAVA_LANG_LONG,
                                JavaType.JAVA_MATH_BIG_DECIMAL}, javaType);

                        if (length != 8) {
                            addProtocolTypeError(errors, message, field, type, 8, length);
                        }

                        break;
                }
            }
        } catch (Exception e) {
            errors.add(new DictionaryValidationError(message == null ? null : message.getName(), field.getName(),
                    e.getMessage(), DictionaryValidationErrorLevel.FIELD, DictionaryValidationErrorType.ERR_FIELD_TYPE));
        }

    }

    private void addProtocolTypeError(List<DictionaryValidationError> errors, IMessageStructure message, IFieldStructure field, ITCHVisitorBase.ProtocolType type,
                                      int expectedLength, int actualLength) {
        errors.add(new DictionaryValidationError(message == null ? null : message.getName(), field.getName(),
                "Attribute <strong>\"Length\"</strong> for type <strong>\"" + type
                        + "\"</strong> has incorrect value = [" + actualLength + "]. Must be [" + expectedLength + "]",
                DictionaryValidationErrorLevel.FIELD, DictionaryValidationErrorType.ERR_ATTRIBUTES));

    }

    private boolean checkJavaType(List<DictionaryValidationError> errors, IMessageStructure message, IFieldStructure field,
                                  JavaType expectedType, JavaType actualType) {

        if(!actualType.equals(expectedType)) {
            errors.add(new DictionaryValidationError(message == null ? null : message.getName(), field.getName(),
                    "Field <strong>\"" + field.getName() + "\"</strong> has incorrect type = ["
                            + actualType + "]. Must be [" + expectedType + "]",
                    DictionaryValidationErrorLevel.FIELD, DictionaryValidationErrorType.ERR_FIELD_TYPE));

            return false;
        }

        return true;
    }

    private boolean checkJavaTypeArray(List<DictionaryValidationError> errors, IMessageStructure message,
                                       IFieldStructure field, JavaType[] expectedTypeList, JavaType actualType) {
        for(JavaType currentType : expectedTypeList) {
            if(actualType.equals(currentType)) {
                return true;
            }
        }

        errors.add(new DictionaryValidationError(message == null ? null : message.getName(), field.getName(),
                "Field <strong>\"" + field.getName() + "\"</strong> has incorrect type = ["
                        + actualType + "]. Must be one of " + Arrays.toString(expectedTypeList),
                DictionaryValidationErrorLevel.FIELD, DictionaryValidationErrorType.ERR_FIELD_TYPE));

        return false;
    }

    private void checkMessageTypes(List<DictionaryValidationError> errors, IDictionaryStructure dictionary) {
        Set<Object> messageTypes = new HashSet<>();

        for(IMessageStructure message : dictionary.getMessageStructures()) {
            Object messageTypeAttribute = message.getAttributeValueByName(ITCHMessageHelper.ATTRIBUTE_MESSAGE_TYPE);

            if(messageTypeAttribute != null) {

                if(!messageTypes.add(messageTypeAttribute)) {
                    errors.add(new DictionaryValidationError(message.getName(), null,
                            ITCHMessageHelper.ATTRIBUTE_MESSAGE_TYPE + " attribute is not unique in <strong>\""
                                    + message.getName() + "\"</strong> message",
                            DictionaryValidationErrorLevel.MESSAGE, DictionaryValidationErrorType.ERR_ATTRIBUTES));
                }

                IFieldStructure messageTypeEnum =
                        dictionary.getFieldStructure(ITCHMessageHelper.ATTRIBUTE_MESSAGE_TYPE);
                
                if (messageTypeEnum != null) {

	                Map<String, IAttributeStructure> messageTypeValues = messageTypeEnum.getValues();
	
	                boolean messageTypeExistInEnum = false;
	
                    if (messageTypeValues != null) {
                        for (IAttributeStructure messageTypeValue : messageTypeValues.values()) {
                            if (messageTypeValue.getValue().equals(messageTypeAttribute.toString())) {
                                messageTypeExistInEnum = true;
                                break;
                            }
                        }
                    }

                    if (!messageTypeExistInEnum && !message.getName().equals(ITCHMessageHelper.MESSAGE_UNIT_HEADER_NAME)
                            && !message.getName().equals(ITCHMessageHelper.MESSAGELIST_NAME)) {
	
	                    errors.add(new DictionaryValidationError(message.getName(), null,
                                ITCHMessageHelper.ATTRIBUTE_MESSAGE_TYPE
	                                    + " attribute value is not exist in enum. Value [" + messageTypeAttribute + "]",
	                            DictionaryValidationErrorLevel.MESSAGE, DictionaryValidationErrorType.ERR_ATTRIBUTES));
	                }
	                
                } else {

                    String errMsg = "Dictionary doesn't contain <strong>" + ITCHMessageHelper.ATTRIBUTE_MESSAGE_TYPE + "</strong> field";
                	
                	if (!ValidationHelper.checkErrorExistByText(errors, errMsg)) {
                		errors.add(new DictionaryValidationError(null, null, errMsg,
                				DictionaryValidationErrorLevel.DICTIONARY, DictionaryValidationErrorType.ERR_REQUIRED_FIELD));
                	}
                }
            }
            checkMessageTypeAttribute(errors, message, dictionary);
        }
    }

}
