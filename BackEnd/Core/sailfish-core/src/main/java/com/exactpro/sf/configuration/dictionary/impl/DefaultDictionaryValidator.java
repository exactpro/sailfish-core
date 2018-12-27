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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.SourceVersion;

import org.apache.commons.lang3.StringUtils;

import com.exactpro.sf.common.messages.structures.IAttributeStructure;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.configuration.dictionary.DictionaryValidationError;
import com.exactpro.sf.configuration.dictionary.DictionaryValidationErrorLevel;
import com.exactpro.sf.configuration.dictionary.DictionaryValidationErrorType;
import com.exactpro.sf.configuration.dictionary.ValidationHelper;
import com.exactpro.sf.configuration.dictionary.interfaces.IDictionaryValidator;

@SuppressWarnings("serial")
public class DefaultDictionaryValidator extends AbstractDictionaryValidator {

    private static final Set<String> prohibitedNames = new HashSet<>();

    static {
        prohibitedNames.add("_");
    }

    public DefaultDictionaryValidator() {
		super();
	}

	public DefaultDictionaryValidator(IDictionaryValidator parentValidator) {
		super(parentValidator);
	}

	@Override
	public List<DictionaryValidationError> validate(IDictionaryStructure dictionary, boolean full, Boolean fieldsOnly) {
		List<DictionaryValidationError> errors =  super.validate(dictionary, full, fieldsOnly);

        checkDictionaryNamespace(errors, dictionary);

		if (fieldsOnly == null || fieldsOnly) {
			ValidationHelper.checkDuplicates(null, dictionary.getFieldStructures(), errors, false, DictionaryValidationErrorLevel.DICTIONARY);
		}

		if (fieldsOnly == null || !fieldsOnly) {
			ValidationHelper.checkDuplicates(null, dictionary.getMessageStructures(), errors, true, DictionaryValidationErrorLevel.DICTIONARY);
		}

		return errors;
	}

	@Override
	public List<DictionaryValidationError> validate(IMessageStructure message, IFieldStructure field) {
		List<DictionaryValidationError> errors = super.validate(message, field);

        if (field.isEnum()) {
            checkFieldName(errors, message, field);
        }

		if (!field.isComplex()) {

			// Check default value type
			boolean isError = ValidationHelper.checkTypeError(errors, message, field, field.getJavaType(),
					field.getDefaultValue(), DictionaryValidationErrorType.ERR_DEFAULT_VALUE);

			if (!isError) {
				checkContainsError(errors, message, field, field.getDefaultValue(), field.getValues(),
						DictionaryValidationErrorType.ERR_DEFAULT_VALUE);
			}

			// Check values types
			if(field.getValues() != null) {
				for (IAttributeStructure value : field.getValues().values()) {
                    ValidationHelper.checkTypeError(errors, null, field, field.getJavaType(), value.getValue(),
                            DictionaryValidationErrorType.ERR_VALUES);
                    checkValueName(errors, message, field, value);
				}
			}
		}

		// Check attributes types
		for (IAttributeStructure attr : field.getAttributes().values()) {
            ValidationHelper.checkTypeError(errors, message, field, attr.getType(), attr.getValue(),
                    DictionaryValidationErrorType.ERR_ATTRIBUTES);
            checkAttributeName(errors, message, field, attr);
		}

		return errors;
	}

	@Override
    public List<DictionaryValidationError> validate(IDictionaryStructure dictionary, IMessageStructure message, boolean full) {
        List<DictionaryValidationError> errors = super.validate(dictionary, message, full);

        checkMessageName(errors, message);

		// Check duplicates
		ValidationHelper.checkDuplicates(message, message.getFields(), errors, false, DictionaryValidationErrorLevel.MESSAGE);

		// Check attributes types
		for (IAttributeStructure attr : message.getAttributes().values()) {
			ValidationHelper.checkTypeError(errors, message, null, attr.getType(), attr.getValue(), DictionaryValidationErrorType.ERR_ATTRIBUTES);
            checkAttributeName(errors, message, null, attr);
		}

		return errors;
	}

	/**
	 * 	Check the presence of value in values table
	 */
	private void checkContainsError(List<DictionaryValidationError> errors,
									IMessageStructure message,
									IFieldStructure   field,
									Object  value,
									Map<String, IAttributeStructure> values,
									DictionaryValidationErrorType errType) {

		if (value == null) return;
		if (value instanceof String && StringUtils.isEmpty((String)value)) return;
		if (values == null || values.isEmpty()) return;

		boolean found = false;

		for (IAttributeStructure attr : values.values()) {

			if (value.toString().equals(attr.getValue())) {
				found = true;
			}
		}

		if (!found) {

			errors.add(new DictionaryValidationError(message == null ? null : message.getName(), field.getName(),
					"Value <strong>\"" + value + "\"</strong> wasn't found in values table",
					DictionaryValidationErrorLevel.FIELD, errType));

			logger.error("[{}{}] Value \"{}\" wasn't found in values table", (message != null ? message.getName() + "/" : ""), field.getName(), value);
		}
	}

    private void checkFieldName(List<DictionaryValidationError> errors, IMessageStructure message, IFieldStructure field) {
        checkStructureName(errors, message, field, field.getName(), Structure.Field);
    }

    private void checkMessageName(List<DictionaryValidationError> errors, IMessageStructure message) {
        checkStructureName(errors, message, null, message.getName(), Structure.Message);
    }

    private void checkAttributeName(List<DictionaryValidationError> errors, IMessageStructure message, IFieldStructure field, IAttributeStructure attr) {
        checkStructureName(errors, message, field, attr.getName(), Structure.Attribute);
    }

    private void checkValueName(List<DictionaryValidationError> errors, IMessageStructure message, IFieldStructure field, IAttributeStructure value) {
        checkStructureName(errors, message, field, value.getName(), Structure.Value);
    }

    private void checkDictionaryNamespace(List<DictionaryValidationError> errors, IDictionaryStructure dictionary) {
        checkStructureName(errors, null, null, dictionary.getNamespace(), Structure.Dictionary);
    }

    private void checkStructureName(List<DictionaryValidationError> errors, IMessageStructure message, IFieldStructure field, String structureName, Structure structure) {
        if (structure.isProhibited(structureName)) {
            DictionaryValidationErrorLevel level;
            if (field != null) {
                level = DictionaryValidationErrorLevel.FIELD;
            } else if (message != null) {
                level = DictionaryValidationErrorLevel.MESSAGE;
            } else {
                level = DictionaryValidationErrorLevel.DICTIONARY;
            }

            DictionaryValidationErrorType type;
            switch (structure) {
                case Attribute:
                    type = DictionaryValidationErrorType.ERR_ATTRIBUTES;
                    break;
                case Value:
                    type = DictionaryValidationErrorType.ERR_VALUES;
                    break;
                default:
                    type = DictionaryValidationErrorType.ERR_NAME;
                    break;
            }

            errors.add(new DictionaryValidationError(message != null ? message.getName() : null,
                    field != null ? field.getName() : null,
                    String.format("Prohibited name <strong>%s</strong> for %s. Name can't be Java keyword or have value that presents in this list [%s]",
                            structureName, structure, StringUtils.join(structure.getProhibitedNames(), ",")),
                    level, type));
        }
    }

    private enum Structure {
        Message,
        Field,
        Dictionary,
        Attribute,
        Value {
            Set<String> prohibit = new HashSet<>(prohibitedNames);
            {
                prohibit.add("Missed");
                prohibit.add("Present");
            }

            @Override
            boolean isProhibited(String name) {
                return super.isProhibited(name)
                        || prohibit.contains(name);
            }

            @Override
            Set<String> getProhibitedNames() {
                return prohibit;
            }
        };

        boolean isProhibited(String name) {
            return prohibitedNames.contains(name) || SourceVersion.isKeyword(name) || !SourceVersion.isIdentifier(name);
        }

        Set<String> getProhibitedNames() {
            return prohibitedNames;
        }
    }
}
