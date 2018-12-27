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
package com.exactpro.sf.configuration.dictionary;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.impl.messages.xml.configuration.JavaType;
import com.exactpro.sf.common.messages.structures.IAttributeStructure;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;

public class ValidationHelper {
	
	private static final Logger logger = LoggerFactory.getLogger(ValidationHelper.class);

	public static void checkDuplicates(IMessageStructure message, List<? extends IFieldStructure> fields, 
			List<DictionaryValidationError> errors, boolean isMessage, DictionaryValidationErrorLevel level) {
		
		Set<String> names = new HashSet<>();
		
		for (IFieldStructure field : fields) {
			if (names.contains(field.getName())) {
				
				if (isMessage) {
					errors.add(new DictionaryValidationError(field.getName(), null, 
							   "Duplicated message <strong>\"" + field.getName() + "\"</strong>", level, DictionaryValidationErrorType.ERR_DUPLICATE_NAME));
					
					logger.error("[{}] Duplicated message", field.getName());
					
				} else {
					
					errors.add(new DictionaryValidationError(message == null ? null : message.getName(), field.getName(), 
							   "Duplicated field <strong>\"" + field.getName() + "\"</strong>", level, DictionaryValidationErrorType.ERR_DUPLICATE_NAME));
					
					logger.error("[{}{}] Duplicated field", (message != null ? message.getName() + "/" : ""), field.getName());
				}
				
			}
			
			names.add(field.getName());
		}
	}
	
	public static boolean checkTypeError(List<DictionaryValidationError> errors, 
								   IMessageStructure  message, 
								   IFieldStructure    field, 
								   JavaType type, 
								   Object   value, 
								   DictionaryValidationErrorType errType) {
					
		if (!isTypeApplicable(field, type, value)) {
		
			errors.add(new DictionaryValidationError(message == null ? null : message.getName(), field == null? null:field.getName(), 
			"Value <strong>\"" + value + "\"</strong> is not applicable for " + getJavaTypeLabel(type) + " type",
			DictionaryValidationErrorLevel.FIELD, errType));
		
			return true;
		}
		
		return false;
	}

	public static void checkRequiredField(List<DictionaryValidationError> errors, IMessageStructure message,
												  String fieldName) {
		if(message.getField(fieldName) == null) {
			errors.add(new DictionaryValidationError(message.getName(), null,
					"Message  <strong>\"" + message.getName() + "\"</strong> doesn't contain " + fieldName +" field",
					DictionaryValidationErrorLevel.MESSAGE, DictionaryValidationErrorType.ERR_REQUIRED_FIELD));
		}
	}

	public static void checkRequiredAttribute(List<DictionaryValidationError> errors, IMessageStructure message,
												  String attributeName) {
		if(!message.getAttributes().containsKey(attributeName)) {
			errors.add(new DictionaryValidationError(message.getName(), null,"Message  <strong>\""
					+ message.getName() + "\"</strong> doesn't contain " + attributeName +" attribute",
					DictionaryValidationErrorLevel.MESSAGE, DictionaryValidationErrorType.ERR_ATTRIBUTES));
		}
	}

	public static boolean checkRequiredFieldAttribute(List<DictionaryValidationError> errors, IMessageStructure message,
												   IFieldStructure field, String attributeName) {

		String messageName = message == null ? null : message.getName();
		String fieldName = field.getName();

		if(field.getAttributes().isEmpty()) {
			errors.add(new DictionaryValidationError(messageName, fieldName, "Attributes is null",
					DictionaryValidationErrorLevel.FIELD, DictionaryValidationErrorType.ERR_ATTRIBUTES));

			return false;

		} else if(!field.getAttributes().containsKey(attributeName)) {
			errors.add(new DictionaryValidationError(messageName, fieldName, "Field  <strong>\""
					+ fieldName + "\"</strong> doesn't contain  " + attributeName +" attribute in "
					+ messageName + " message" ,
					DictionaryValidationErrorLevel.FIELD, DictionaryValidationErrorType.ERR_ATTRIBUTES));

			return false;
		}
		return true;
	}

	public static boolean checkRequiredFieldAttributes(List<DictionaryValidationError> errors, IMessageStructure message,
												   IFieldStructure field, String... attributes) {
		String messageName = message == null ? null : message.getName();
		String fieldName = field.getName();

		if(field.getAttributes().isEmpty()) {
			errors.add(new DictionaryValidationError(messageName, fieldName, "Attributes is null",
					DictionaryValidationErrorLevel.FIELD, DictionaryValidationErrorType.ERR_ATTRIBUTES));

			return false;
		} else {
			for(String attributeName : attributes) {
				if(!field.getAttributes().containsKey(attributeName)) {
					errors.add(new DictionaryValidationError(messageName, fieldName, "Field  <strong>\""
							+ fieldName + "\"</strong> doesn't contain  " + attributeName +" attribute in "
							+ messageName + " message" ,
							DictionaryValidationErrorLevel.FIELD, DictionaryValidationErrorType.ERR_ATTRIBUTES));

					return false;
				}
			}
		}
		return true;
	}

	public static void checkRequiredMessageExistence(List<DictionaryValidationError> errors,
											   IDictionaryStructure dictionary, String messageName) {
		if(dictionary.getMessageStructure(messageName) == null) {
			errors.add(new DictionaryValidationError(null, null,
					"Message  <strong>\"" + messageName + "\"</strong> is missing in dictionary",
					DictionaryValidationErrorLevel.DICTIONARY, DictionaryValidationErrorType.ERR_REQUIRED_FIELD));
		}

	}
	
	public static boolean checkErrorExistByText(List<DictionaryValidationError> list, String text) {
		
		for (DictionaryValidationError error : list) {
			if (error.getError().equals(text)) {
				return true;
			}
		}
		
		return false;
	}

	public static boolean checkMessageAttributeType(List<DictionaryValidationError> errors, IMessageStructure message,
													String attributeName, JavaType expectedJavaType,
													Object[] possibleValues) {

		IAttributeStructure messageTypeStructure = message.getAttributes().get(attributeName);

		return ValidationHelper.checkAttributeType(errors, messageTypeStructure, message, null,
											attributeName, expectedJavaType, possibleValues);
	}

	public static boolean checkFieldAttributeType(List<DictionaryValidationError> errors, IFieldStructure field,
													String attributeName, JavaType expectedJavaType,
													Object[] possibleValues) {

		IAttributeStructure messageTypeStructure = field.getAttributes().get(attributeName);

		return ValidationHelper.checkAttributeType(errors, messageTypeStructure, null, field,
											attributeName, expectedJavaType, possibleValues);
	}

	public static boolean checkAttributeType(List<DictionaryValidationError> errors,
											 IAttributeStructure messageTypeStructure,
											 IMessageStructure message, IFieldStructure field,
											 String attributeName, JavaType expectedJavaType,
											 Object[] possibleValues) {

		String messageName = message == null ? null : message.getName();
		String fieldName = field == null ? null : field.getName();
		String type = message == null ? "Field" : "Message";
		String name = message == null ? fieldName : messageName;

		if (messageTypeStructure == null) {
			errors.add(new DictionaryValidationError(messageName, fieldName,
					type + "  <strong>\"" + name
							+ "\"</strong> doesn't contain " + attributeName +  " attribute",
					DictionaryValidationErrorLevel.MESSAGE, DictionaryValidationErrorType.ERR_ATTRIBUTES));

			return false;
		}

		if(!messageTypeStructure.getType().equals(expectedJavaType)) {
			errors.add(new DictionaryValidationError(messageName, fieldName,
					type + "  <strong>\"" + name
							+ "\"</strong> contain " + attributeName +  " attribute"
							+ "incorrect type <strong>\"" + messageTypeStructure.getType() + "\"</strong>",
					DictionaryValidationErrorLevel.MESSAGE, DictionaryValidationErrorType.ERR_ATTRIBUTES));

			return false;
		}

		if(possibleValues != null) {
			for(Object possibleValue : possibleValues) {
				if(possibleValue.equals(messageTypeStructure.getValue())) {
					return true;
				}
			}

			errors.add(new DictionaryValidationError(messageName, fieldName,
					type + "  <strong>\"" + name
							+ "\"</strong> contain " + attributeName +  " attribute "
							+ "with incorrect value <strong>\"" + messageTypeStructure.getValue() + "\"</strong>",
					DictionaryValidationErrorLevel.MESSAGE, DictionaryValidationErrorType.ERR_ATTRIBUTES));

			return false;
		}

		return true;
	}
	
	private static boolean isTypeApplicable(IFieldStructure field, JavaType type, Object objValue) {
		
		String value = null;
		
		try {
			
			if (objValue == null) return true;
			
			if (type == null) return true;
		
			if (!(objValue instanceof String)) {
				return true;
			}
			
			value = (String) objValue;
			
			if (value.isEmpty()) return true;
			
			switch (type) {
			
				case JAVA_LANG_BOOLEAN : 
					
					if (null == boolValueOf(value)) throw new ClassCastException();
					break;
			
				case JAVA_LANG_BYTE :
					
					if (null == Byte.valueOf(value)) throw new ClassCastException();
					break;	
			
				case JAVA_LANG_SHORT :
				
					if (null == Short.valueOf(value)) throw new ClassCastException();
					break;
					
				case JAVA_LANG_INTEGER :
					
					if (null == Integer.valueOf(value)) throw new ClassCastException();
					break;
					
				case JAVA_LANG_LONG :
					
					if (null == Long.valueOf(value)) throw new ClassCastException();
					break;
					
				case JAVA_LANG_FLOAT :
					
					if (null == Float.valueOf(value)) throw new ClassCastException();
					break;
					
				case JAVA_LANG_DOUBLE :
					
					if (null == Double.valueOf(value)) throw new ClassCastException();
					break;
			
				case JAVA_LANG_CHARACTER :
					
					if (value.length() > 1) throw new ClassCastException();
					break;
					
				case JAVA_MATH_BIG_DECIMAL :
					
					if (null == BigDecimal.valueOf(Double.valueOf(value))) throw new ClassCastException();
					break;
					
				default: break;
			}
			
			return true;
		
		} catch (Exception e) {
			
			logger.error("[{}] Value {} is not applicable for {} type", field == null ? null : field.getName() , value, getJavaTypeLabel(type), e);
			
			return false;
			
		}
	}
	
	private static Boolean boolValueOf(String value) {
		
	    if ("Y".equals(value)) return true;
	    
        if ("N".equals(value)) return false;
		
		if (Boolean.valueOf(value)) return true;
		
		if (!value.equalsIgnoreCase("false")) return null;
		
		return false;
	}
	
	private static String getJavaTypeLabel(JavaType type) {
		int index = type.value().lastIndexOf(".") + 1;
		String result = type.value();
		if (index != 0) {
			result = type.value().substring(index);
		}
		return result;
	}
}
