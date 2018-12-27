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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.configuration.dictionary.DictionaryValidationError;
import com.exactpro.sf.configuration.dictionary.interfaces.IDictionaryValidator;

@SuppressWarnings("serial")
public abstract class AbstractDictionaryValidator implements IDictionaryValidator, Serializable {

    protected static final Logger logger = LoggerFactory.getLogger(AbstractDictionaryValidator.class);

    private final IDictionaryValidator parentValidator; // All IDictionaryValidator implementations should implement the decorator pattern

    public AbstractDictionaryValidator() {
        this.parentValidator = null;
    }

    public AbstractDictionaryValidator(IDictionaryValidator parentValidator) {
        this.parentValidator = parentValidator;
    }

    @Override
    public List<DictionaryValidationError> validate(IDictionaryStructure dictionary, boolean full, Boolean fieldsOnly) {

        List<DictionaryValidationError> errors = new ArrayList<>();

        if (this.parentValidator != null) {
            errors.addAll(this.parentValidator.validate(dictionary, false, null));
        }

        if (full) {

            if (fieldsOnly == null || fieldsOnly) {
                checkDictionaryFields(dictionary.getFieldStructures(), errors);
            }

            if (fieldsOnly == null || !fieldsOnly) {
                checkMessages(dictionary, errors, full);
            }
        }

        return errors;
    }

    @Override
    public List<DictionaryValidationError> validate(IDictionaryStructure dictionary, IMessageStructure message, boolean full) {
        List<DictionaryValidationError> errors = new ArrayList<>();

        if (this.parentValidator != null) {
            errors.addAll(this.parentValidator.validate(dictionary, message, false));
        }
        
        if (full) {
            checkMessageFields(message, errors);
        }

        return errors;
    }

    @Override
    public List<DictionaryValidationError> validate(IMessageStructure message, IFieldStructure field) {

        List<DictionaryValidationError> errors = new ArrayList<>();

        if (this.parentValidator != null) {
            errors.addAll(this.parentValidator.validate(message, field));
        }

        return errors;
    }

    protected void checkMessages(IDictionaryStructure dictionary, List<DictionaryValidationError> errors, boolean full) {

        for (IMessageStructure message : dictionary.getMessageStructures()) {

            errors.addAll(this.validate(dictionary, message, full));
        }
    }

    protected void checkDictionaryFields(List<IFieldStructure> fields, List<DictionaryValidationError> errors) {

        for (IFieldStructure field : fields) {

            errors.addAll(this.validate(null, field));
        }
    }

    protected void checkMessageFields(IMessageStructure message, List<DictionaryValidationError> errors) {

        for (IFieldStructure field : message.getFields()) {

            errors.addAll(this.validate(message, field));
        }
    }
}
