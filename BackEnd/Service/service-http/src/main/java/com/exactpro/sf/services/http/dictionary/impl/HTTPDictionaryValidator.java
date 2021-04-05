/******************************************************************************
 * Copyright 2009-2021 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.services.http.dictionary.impl;

import static com.exactpro.sf.common.messages.structures.StructureUtils.getAttributeValue;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.configuration.dictionary.DictionaryValidationError;
import com.exactpro.sf.configuration.dictionary.DictionaryValidationErrorLevel;
import com.exactpro.sf.configuration.dictionary.DictionaryValidationErrorType;
import com.exactpro.sf.configuration.dictionary.impl.AbstractDictionaryValidator;
import com.exactpro.sf.configuration.dictionary.interfaces.IDictionaryValidator;
import com.exactpro.sf.services.http.HTTPMessageHelper;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * @author oleg.smirnov
 *
 */
public class HTTPDictionaryValidator extends AbstractDictionaryValidator {

    /**
     * 
     */
    private static final long serialVersionUID = 8137605668930620502L;


    private final Pattern pattern = Pattern.compile("\\{(\\w)+\\}");

    /**
     * 
     */
    public HTTPDictionaryValidator() {
    }

    /**
     * 
     */
    public HTTPDictionaryValidator(IDictionaryValidator parent) {
        super(parent);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.exactpro.sf.configuration.dictionary.impl.
     * AbstractDictionaryValidator#validate(com.exactpro.sf.common.messages.
     * structures.IDictionaryStructure, boolean, java.lang.Boolean)
     */
    @Override
    public List<DictionaryValidationError> validate(IDictionaryStructure dictionary, boolean full, Boolean fieldsOnly) {
        List<DictionaryValidationError> errors = super.validate(dictionary, full, fieldsOnly);
        Multimap<Integer, String> errorsMap = HashMultimap.create();
        for(IMessageStructure messageStructure : dictionary.getMessages().values()) {
            Integer errorCode = getAttributeValue(messageStructure, HTTPMessageHelper.ERROR_CODE_ATTRIBUTE);
            if (errorCode != null) {
                errorsMap.put(errorCode, messageStructure.getName());
            }
        }
        for (Integer errorCode : errorsMap.keySet()) {
            Collection<String> mappedMessages = errorsMap.get(errorCode);
            if (mappedMessages.size() > 1) {
                for (String messageName : mappedMessages) {
                    errors.add(new DictionaryValidationError(messageName, null,
                            String.format("Code %d already mapped to [%s]", errorCode, StringUtils.join(mappedMessages, ",")),
                            DictionaryValidationErrorLevel.MESSAGE, DictionaryValidationErrorType.ERR_ATTRIBUTES));
                }
            }
        }
        return errors;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.exactpro.sf.configuration.dictionary.impl.
     * AbstractDictionaryValidator#validate(com.exactpro.sf.common.messages.
     * structures.IDictionaryStructure,
     * com.exactpro.sf.common.messages.structures.IMessageStructure, boolean)
     */
    @Override
    public List<DictionaryValidationError> validate(IDictionaryStructure dictionary, IMessageStructure message,
            boolean full) {
        List<DictionaryValidationError> errors = super.validate(dictionary, message, full);
        if (message != null) {
            checkMessageAttributes(dictionary, message, errors);
        }
        return errors;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.exactpro.sf.configuration.dictionary.impl.
     * AbstractDictionaryValidator#validate(com.exactpro.sf.common.messages.
     * structures.IMessageStructure,
     * com.exactpro.sf.common.messages.structures.IFieldStructure)
     */
    @Override
    public List<DictionaryValidationError> validate(IMessageStructure message, IFieldStructure field) {
        List<DictionaryValidationError> errors = super.validate(message, field);
        if (HTTPMessageHelper.REQUEST_URI_ATTRIBUTE.equals(field.getName())) {
            String attrValue = getAttributeValue(message, HTTPMessageHelper.REQUEST_URI_ATTRIBUTE);
            if (attrValue != null) {
                if (field.isComplex()) {
                    checkURI(attrValue, message, field, errors);
                } else {
                    errors.add(new DictionaryValidationError(message.getName(), field.getName(),
                            String.format("Field %s must be complex", HTTPMessageHelper.REQUEST_URI_ATTRIBUTE),
                            DictionaryValidationErrorLevel.FIELD, DictionaryValidationErrorType.ERR_FIELD_TYPE));
                }
            } else {
                errors.add(new DictionaryValidationError(message.getName(), null,
                        String.format("Message has field %s but doesn't contains attribute %s", field.getName(),
                                HTTPMessageHelper.REQUEST_URI_ATTRIBUTE),
                        DictionaryValidationErrorLevel.MESSAGE, DictionaryValidationErrorType.ERR_ATTRIBUTES));
            }
        }
        return errors;
    }

    /**
     * 
     * @param dictionary
     * @param message
     * @param errors
     */
    private void checkMessageAttributes(IDictionaryStructure dictionary, IMessageStructure message,
            List<DictionaryValidationError> errors) {
        Object attrValue = getAttributeValue(message, HTTPMessageHelper.REQUEST_RESPONSE_ATTRIBUTE);
        if (attrValue != null) {
            IMessageStructure response = dictionary.getMessages().get(attrValue.toString());
            if (response == null) {
                errors.add(new DictionaryValidationError(message.getName(), null,
                        String.format("Message have attribute %s with unknown message name %s",
                                HTTPMessageHelper.REQUEST_RESPONSE_ATTRIBUTE, attrValue),
                        DictionaryValidationErrorLevel.MESSAGE, DictionaryValidationErrorType.ERR_ATTRIBUTES));
            }
        }
        attrValue = getAttributeValue(message, HTTPMessageHelper.REQUEST_METHOD_ATTRIBUTE);
        if (attrValue != null) {
            if (HttpMethod.fromValue(attrValue.toString()) == HttpMethod.UNKNOWN) {
                StringBuilder errorMsg = new StringBuilder(
                        String.format("Message have attribute %s with unknown HTTP Method %s. Available: ",
                                HTTPMessageHelper.REQUEST_METHOD_ATTRIBUTE, attrValue));
                for (HttpMethod method : HttpMethod.values()) {
                    if (method != HttpMethod.UNKNOWN) {
                        errorMsg.append(method.name() + ", ");
                    }
                }
                errors.add(new DictionaryValidationError(message.getName(), null,
                        errorMsg.toString().substring(0, errorMsg.length() - 2),
                        DictionaryValidationErrorLevel.MESSAGE, DictionaryValidationErrorType.ERR_ATTRIBUTES));
            }
        }

        attrValue = getAttributeValue(message, HTTPMessageHelper.REQUEST_URI_ATTRIBUTE);
        if (attrValue != null) {
            checkURI(attrValue.toString(), message, message.getFields().get(HTTPMessageHelper.REQUEST_URI_ATTRIBUTE), errors);
        }
    }

    /**
     * 
     * @param uri
     * @param message
     * @param uriField
     * @param errors
     */
    private void checkURI(String uri, IMessageStructure message, IFieldStructure uriField,
            List<DictionaryValidationError> errors) {
        Matcher matcher = pattern.matcher(uri);
        if (matcher.find()) {
            if (uriField == null) {
                errors.add(new DictionaryValidationError(message.getName(), null,
                        String.format("Message doesn't have field %s with parameters for attribute %s",
                                HTTPMessageHelper.REQUEST_URI_ATTRIBUTE, HTTPMessageHelper.REQUEST_URI_ATTRIBUTE),
                        DictionaryValidationErrorLevel.MESSAGE, DictionaryValidationErrorType.ERR_ATTRIBUTES));
                return;
            }
            if (!uriField.isComplex()) {
                errors.add(new DictionaryValidationError(message.getName(), uriField.getName(),
                        String.format("Field %s must be complex", HTTPMessageHelper.REQUEST_URI_ATTRIBUTE),
                        DictionaryValidationErrorLevel.FIELD, DictionaryValidationErrorType.ERR_FIELD_TYPE));
                return;
            }
            String paramName;
            Set<String> fieldsName = uriField.getFields().keySet();
            do {
                paramName = uri.substring(matcher.start() + 1, matcher.end() - 1);
                if (!fieldsName.contains(paramName)) {
                    errors.add(new DictionaryValidationError(message.getName(), uriField.getName(),
                            String.format("Message doesn't have field %s for parameter in URI", paramName),
                            DictionaryValidationErrorLevel.MESSAGE, DictionaryValidationErrorType.ERR_REQUIRED_FIELD));
                }
            } while (matcher.find());
        }
    }

    enum HttpMethod {
        GET, HEAD, POST, PUT, PATCH, DELETE, TRACE, CONNECT,
        // for unknown HTTP Method
        UNKNOWN;

        public static HttpMethod fromValue(String method) {
            for (HttpMethod m : values()) {
                if (m.name().equals(method)) {
                    return m;
                }
            }
            return UNKNOWN;
        }
    }
}
