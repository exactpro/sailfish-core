/*******************************************************************************
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

import java.io.InputStream;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.exactpro.sf.common.messages.structures.DictionaryComparator;
import com.exactpro.sf.common.messages.structures.DictionaryComparator.DictionaryPath;
import com.exactpro.sf.common.messages.structures.DictionaryComparator.DistinctionType;
import com.exactpro.sf.common.messages.structures.DictionaryComparator.EntityCheckType;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.messages.structures.loaders.XmlDictionaryStructureLoader;
import com.exactpro.sf.configuration.dictionary.DictionaryValidationError;
import com.exactpro.sf.configuration.dictionary.DictionaryValidationErrorLevel;
import com.exactpro.sf.configuration.dictionary.DictionaryValidationErrorType;
import com.exactpro.sf.configuration.dictionary.interfaces.IDictionaryValidator;

public class PrototypeDictionaryValidator extends AbstractDictionaryValidator {

    private static final long serialVersionUID = -485508906826769301L;
    
    private final IDictionaryStructure prototypeDictionary;
    private final DictionaryComparator comparator = new DictionaryComparator(false);
    private final boolean compareOrderInMessage;

    public PrototypeDictionaryValidator(InputStream prototypeDictionary) {
        this(prototypeDictionary, null);
    }

    public PrototypeDictionaryValidator(InputStream prototypeDictionary, boolean compareOrderInMessage) {
        this(prototypeDictionary, null, compareOrderInMessage);
    }

    public PrototypeDictionaryValidator(InputStream prototypeDictionary, IDictionaryValidator parent) {
        this(prototypeDictionary, parent, false);
    }

    public PrototypeDictionaryValidator(InputStream prototypeDictionary, IDictionaryValidator parent, boolean compareOrderInMessage) {
        super(parent);
        this.prototypeDictionary = new XmlDictionaryStructureLoader().load(prototypeDictionary);
        this.compareOrderInMessage = compareOrderInMessage;
    }

    @Override
    public List<DictionaryValidationError> validate(IDictionaryStructure dictionary, boolean full, Boolean fieldsOnly) {
        List<DictionaryValidationError> errors = super.validate(dictionary, full, fieldsOnly);
        comparator.compare(
                (DistinctionType distinctionType, Object first, Object second, DictionaryPath dictionaryPath) ->
                        catchDifference(errors, DictionaryValidationErrorLevel.DICTIONARY, distinctionType, first, second, dictionaryPath),
                prototypeDictionary, dictionary, false, true, false);

        return errors;
    }

    @Override
    public List<DictionaryValidationError> validate(IDictionaryStructure dictionary, IMessageStructure message, boolean full) {
        List<DictionaryValidationError> errors = super.validate(dictionary, message, full);

        IMessageStructure prototypeMessageStructure = prototypeDictionary.getMessageStructure(message.getName());
        if (prototypeMessageStructure != null) {
            comparator.compare(
                    (DistinctionType distinctionType, Object first, Object second, DictionaryPath dictionaryPath) ->
                            catchDifference(errors, null, distinctionType, first, second, dictionaryPath),
                    prototypeMessageStructure, message, new DictionaryPath(dictionary.getNamespace()).setMessage(message.getName()),
                    compareOrderInMessage, true, true, EntityCheckType.MESSAGE);
        }

        return errors;
    }

    @Override
    public List<DictionaryValidationError> validate(IMessageStructure message, IFieldStructure field) {
        List<DictionaryValidationError> errors = super.validate(message, field);

        DictionaryPath path = new DictionaryPath("");
        IFieldStructure prototypeField = null;
        if (message == null) {
            prototypeField = prototypeDictionary.getFieldStructure(field.getName());
        } else {
            IMessageStructure prototypeMessage = prototypeDictionary.getMessageStructure(message.getName());
            if (prototypeMessage != null) {
                path.setMessage(prototypeMessage.getName());
                prototypeField = prototypeMessage.getField(field.getName());
            }
        }

        if (prototypeField != null) {
            comparator.compare(
                    (DistinctionType distinctionType, Object first, Object second, DictionaryPath dictionaryPath) ->
                            catchDifference(errors, DictionaryValidationErrorLevel.FIELD, distinctionType, first, second, dictionaryPath),
                    prototypeField, field, path.setField(prototypeField.getName()),
                    false, true, true, EntityCheckType.FIELD);
        }

        return errors;
    }

    private void catchDifference(List<DictionaryValidationError> errors, DictionaryValidationErrorLevel level, DistinctionType distinctionType, Object prototype, Object actual, DictionaryPath dictionaryPath) {
        if (distinctionType == DistinctionType.Namespace
                || distinctionType == DistinctionType.Description) {
            return;
        }

        DictionaryValidationErrorType type = getErrorType(distinctionType);

        if (level == null) {
            level = getErrorLevel(dictionaryPath);
        }
        
        String field = dictionaryPath.getField();

        if (DictionaryValidationErrorType.ERR_REQUIRED_FIELD == type) {
            field = null;
        }

        String errorMsg = createErrorMsg(distinctionType, level, prototype, actual, dictionaryPath);

        errors.add(new DictionaryValidationError(dictionaryPath.getMessage(), field, errorMsg, level, type));
    }

    private String createErrorMsg(DistinctionType type, DictionaryValidationErrorLevel level, Object prototype, Object actual, DictionaryPath path) {
        switch (level) {
            case DICTIONARY:
                return createDictionaryErrorMsg(path);
            case FIELD:
            case MESSAGE:
                return createCommonErrorMsg(type, prototype, actual, path);
            default:
                return null;
        }
    }

    private String createCommonErrorMsg(DistinctionType type, Object prototype, Object actual, DictionaryPath path) {
        String errorMsg = null;
        switch (type) {
            case AttributeValue:
                if (actual == null) {
                    errorMsg = String.format("Attribute <strong>%s</strong> was missed", path.getAttribute());
                } else {
                    errorMsg = String.format("Attribute <strong>%s</strong> should be <strong>%s</strong> but was <strong>%s</strong>", path.getAttribute(), prototype, actual);
                }
                break;
            case EnumValue:
                if (actual == null) {
                    errorMsg = String.format("Value <strong>%s</strong> was missed", path.getValue());
                } else {
                    errorMsg = String.format("Value <strong>%s</strong> should be <strong>%s</strong> but was <strong>%s</strong>", path.getValue(), prototype, actual);
                }
                break;
            case JavaType:
            case IsCollection:
            case IsRequired:
            case IsServiceName:
            case IsComplex:
            case IsEnum:
            case DefaultValue:
                errorMsg = String.format("<strong>%s</strong> should be <strong>%s</strong> but was <strong>%s</strong>", type, prototype, actual);
                break;
            case Existing:
                errorMsg = "Missed required field <strong>" + path.getField() + "</strong>";
                break;
            case FieldOrder:
                errorMsg = String.format("At this position message should have field <strong>%s</strong> but it was <strong>%s</strong>",
                        prototype, actual);
            default:
                //do nothing
                break;
        }
        return errorMsg;
    }

    private String createDictionaryErrorMsg(DictionaryPath path) {
        StringBuilder builder = new StringBuilder("Missed required ");
        if (path.getMessage() != null) {
            builder.append("message")
                    .append(StringUtils.SPACE)
                    .append(path.getMessage());
        } else {
            builder.append("field")
                    .append(StringUtils.SPACE)
                    .append(path.getField());
        }
        return builder.toString();
    }

    private DictionaryValidationErrorType getErrorType(DistinctionType distinctionType) {
        switch (distinctionType) {
            case Existing:
                return DictionaryValidationErrorType.ERR_REQUIRED_FIELD;
            case DefaultValue:
                return DictionaryValidationErrorType.ERR_DEFAULT_VALUE;
            case JavaType:
            case IsCollection:
            case IsRequired:
            case IsServiceName:
            case IsComplex:
            case IsEnum:
                return DictionaryValidationErrorType.ERR_FIELD_TYPE;
            case AttributeValue:
                return DictionaryValidationErrorType.ERR_ATTRIBUTES;
            case EnumValue:
                return DictionaryValidationErrorType.ERR_VALUES;
            case FieldOrder:
                return DictionaryValidationErrorType.ERR_NAME;
            default:
                return null;
        }
    }

    private DictionaryValidationErrorLevel getErrorLevel(DictionaryPath dictionaryPath) {
        if (dictionaryPath.getMessage() != null && dictionaryPath.getField() == null) {
            return DictionaryValidationErrorLevel.MESSAGE;
        } else if (dictionaryPath.getField() != null) {
            return DictionaryValidationErrorLevel.FIELD;
        } else {
            return DictionaryValidationErrorLevel.DICTIONARY;
        }
    }
}
