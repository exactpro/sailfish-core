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
package com.exactpro.sf.configuration;

import static java.util.Collections.singletonMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.configuration.dictionary.DictionaryValidationError;
import com.exactpro.sf.configuration.dictionary.DictionaryValidationErrorLevel;
import com.exactpro.sf.configuration.dictionary.DictionaryValidationErrorType;
import com.exactpro.sf.configuration.dictionary.impl.AbstractDictionaryValidator;
import com.exactpro.sf.configuration.dictionary.interfaces.IDictionaryValidator;

public class TestAbstractDictionaryValidator {

    private final DictionaryValidationError parentDictionaryError = new DictionaryValidationError("parentDictionaryError", "field", "error",
            DictionaryValidationErrorLevel.DICTIONARY,
            DictionaryValidationErrorType.ERR_NAME);
    private final DictionaryValidationError parentMessageNewError = new DictionaryValidationError("parentMessageNewError", "field", "error",
            DictionaryValidationErrorLevel.MESSAGE,
            DictionaryValidationErrorType.ERR_NAME);
    private final DictionaryValidationError parentMessageOldError = new DictionaryValidationError("parentMessageOldError", "field", "error",
            DictionaryValidationErrorLevel.MESSAGE,
            DictionaryValidationErrorType.ERR_NAME);
    private final DictionaryValidationError parentMessageFieldError = new DictionaryValidationError("parentMessageFieldError", "field", "error",
            DictionaryValidationErrorLevel.FIELD,
            DictionaryValidationErrorType.ERR_NAME);
    private final DictionaryValidationError parentFieldError = new DictionaryValidationError("parentFieldError", "field", "error",
            DictionaryValidationErrorLevel.FIELD,
            DictionaryValidationErrorType.ERR_NAME);

    private final DictionaryValidationError childDictionaryError = new DictionaryValidationError("childDictionaryError", "field", "error",
            DictionaryValidationErrorLevel.DICTIONARY,
            DictionaryValidationErrorType.ERR_NAME);
    private final DictionaryValidationError childMessageNewError = new DictionaryValidationError("childMessageNewError", "field", "error",
            DictionaryValidationErrorLevel.MESSAGE,
            DictionaryValidationErrorType.ERR_NAME);
    private final DictionaryValidationError childMessageOldError = new DictionaryValidationError("childMessageOldError", "field", "error",
            DictionaryValidationErrorLevel.MESSAGE,
            DictionaryValidationErrorType.ERR_NAME);
    private final DictionaryValidationError childMessageFieldError = new DictionaryValidationError("childMessageFieldError", "field", "error",
            DictionaryValidationErrorLevel.FIELD,
            DictionaryValidationErrorType.ERR_NAME);
    private final DictionaryValidationError childFieldError = new DictionaryValidationError("childFieldError", "field", "error",
            DictionaryValidationErrorLevel.FIELD,
            DictionaryValidationErrorType.ERR_NAME);

    private final IDictionaryValidator childDictionaryValidator;

    private final IDictionaryStructure dictionaryStructure;
    private final IMessageStructure messageStructure;
    private final IFieldStructure fieldStructure;

    @SuppressWarnings("serial")
    public TestAbstractDictionaryValidator() {
        this.fieldStructure = Mockito.mock(IFieldStructure.class);

        this.messageStructure = Mockito.mock(IMessageStructure.class);
        Map<String, IFieldStructure> fields = singletonMap(fieldStructure.getName(), fieldStructure);
        Mockito.when(messageStructure.getFields()).thenReturn(fields);

        this.dictionaryStructure = Mockito.mock(IDictionaryStructure.class);
        Map<String, IMessageStructure> messages = singletonMap(messageStructure.getName(), messageStructure);
        Mockito.when(dictionaryStructure.getMessages()).thenReturn(messages);
        Mockito.when(dictionaryStructure.getFields()).thenReturn(fields);

        AbstractDictionaryValidator parent = new AbstractDictionaryValidator() {

            @Override
            public List<DictionaryValidationError> validate(IDictionaryStructure dictionary, boolean full, Boolean fieldsOnly) {
                List<DictionaryValidationError> list = new ArrayList<>();
                list.addAll(super.validate(dictionary, full, fieldsOnly));
                list.add(parentDictionaryError);
                return list;
            }

            @Override
            public List<DictionaryValidationError> validate(IDictionaryStructure dictionary, IMessageStructure message, boolean full) {
                List<DictionaryValidationError> list = super.validate(dictionary, message, full);
                list.add(parentMessageOldError);
                list.add(parentMessageNewError);
                return list;
            }

            @Override
            public List<DictionaryValidationError> validate(IMessageStructure message, IFieldStructure field) {
                List<DictionaryValidationError> list = super.validate(message, field);
                list.add(message != null ? parentMessageFieldError : parentFieldError);
                return list;
            }
        };

        this.childDictionaryValidator = new AbstractDictionaryValidator(parent) {
            @Override
            public List<DictionaryValidationError> validate(IDictionaryStructure dictionary, boolean full, Boolean fieldsOnly) {
                List<DictionaryValidationError> list = new ArrayList<>();
                list.addAll(super.validate(dictionary, full, fieldsOnly));
                list.add(childDictionaryError);
                return list;
            }

            @Override
            public List<DictionaryValidationError> validate(IDictionaryStructure dictionary, IMessageStructure message, boolean full) {
                List<DictionaryValidationError> list = super.validate(dictionary, message, full);
                list.add(childMessageOldError);
                list.add(childMessageNewError);
                return list;
            }

            @Override
            public List<DictionaryValidationError> validate(IMessageStructure message, IFieldStructure field) {
                List<DictionaryValidationError> list = super.validate(message, field);
                list.add(message != null ? childMessageFieldError : childFieldError);
                return list;
            }
        };
    }

    @Test
    public void testCallTrace() {
        Assert.assertArrayEquals(new Object[] { parentMessageFieldError, childMessageFieldError },
                childDictionaryValidator.validate(messageStructure, fieldStructure).toArray());

        Assert.assertArrayEquals(
                new Object[] { parentMessageOldError, parentMessageNewError, parentMessageFieldError, childMessageFieldError, childMessageOldError, childMessageNewError },
                childDictionaryValidator.validate(dictionaryStructure, messageStructure, true).toArray());
        Assert.assertArrayEquals(
                new Object[] { parentMessageOldError, parentMessageNewError, childMessageOldError, childMessageNewError },
                childDictionaryValidator.validate(dictionaryStructure, messageStructure, false).toArray());

        Assert.assertArrayEquals(
                new Object[] { parentDictionaryError, childDictionaryError },
                childDictionaryValidator.validate(dictionaryStructure, false, false).toArray());
        Assert.assertArrayEquals(
                new Object[] { parentDictionaryError, childDictionaryError },
                childDictionaryValidator.validate(dictionaryStructure, false, true).toArray());
        Assert.assertArrayEquals(
                new Object[] { parentDictionaryError, childDictionaryError },
                childDictionaryValidator.validate(dictionaryStructure, false, null).toArray());
        Assert.assertArrayEquals(
                new Object[] { parentDictionaryError, parentMessageOldError, parentMessageNewError, parentMessageFieldError, childMessageFieldError, childMessageOldError,
                        childMessageNewError, childDictionaryError },
                childDictionaryValidator.validate(dictionaryStructure, true, false).toArray());
        Assert.assertArrayEquals(
                new Object[] { parentDictionaryError, parentFieldError, childFieldError, childDictionaryError },
                childDictionaryValidator.validate(dictionaryStructure, true, true).toArray());
        Assert.assertArrayEquals(
                new Object[] { parentDictionaryError, parentFieldError, childFieldError, parentMessageOldError, parentMessageNewError, parentMessageFieldError,
                        childMessageFieldError, childMessageOldError, childMessageNewError, childDictionaryError },
                childDictionaryValidator.validate(dictionaryStructure, true, null).toArray());
    }

}
