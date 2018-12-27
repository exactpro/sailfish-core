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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
        Mockito.when(this.messageStructure.getFields()).thenReturn(Arrays.asList(new IFieldStructure[] { this.fieldStructure }));

        this.dictionaryStructure = Mockito.mock(IDictionaryStructure.class);
        Mockito.when(this.dictionaryStructure.getMessageStructures()).thenReturn(Arrays.asList(new IMessageStructure[] { this.messageStructure }));
        Mockito.when(this.dictionaryStructure.getFieldStructures()).thenReturn(Arrays.asList(new IFieldStructure[] { this.fieldStructure }));

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
                if (message != null) {
                    list.add(parentMessageFieldError);
                } else {
                    list.add(parentFieldError);
                }
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
                if (message != null) {
                    list.add(childMessageFieldError);
                } else {
                    list.add(childFieldError);
                }
                return list;
            }
        };
    }

    @Test
    public void testCallTrace() {
        Assert.assertArrayEquals(new Object[] { parentMessageFieldError, childMessageFieldError },
                childDictionaryValidator.validate(this.messageStructure, this.fieldStructure).toArray());

        Assert.assertArrayEquals(
                new Object[] { parentMessageOldError, parentMessageNewError, parentMessageFieldError, childMessageFieldError, childMessageOldError, childMessageNewError },
                childDictionaryValidator.validate(this.dictionaryStructure, this.messageStructure, true).toArray());
        Assert.assertArrayEquals(
                new Object[] { parentMessageOldError, parentMessageNewError, childMessageOldError, childMessageNewError },
                childDictionaryValidator.validate(this.dictionaryStructure, this.messageStructure, false).toArray());

        Assert.assertArrayEquals(
                new Object[] { parentDictionaryError, childDictionaryError },
                childDictionaryValidator.validate(this.dictionaryStructure, false, false).toArray());
        Assert.assertArrayEquals(
                new Object[] { parentDictionaryError, childDictionaryError },
                childDictionaryValidator.validate(this.dictionaryStructure, false, true).toArray());
        Assert.assertArrayEquals(
                new Object[] { parentDictionaryError, childDictionaryError },
                childDictionaryValidator.validate(this.dictionaryStructure, false, null).toArray());
        Assert.assertArrayEquals(
                new Object[] { parentDictionaryError, parentMessageOldError, parentMessageNewError, parentMessageFieldError, childMessageFieldError, childMessageOldError,
                        childMessageNewError, childDictionaryError },
                childDictionaryValidator.validate(this.dictionaryStructure, true, false).toArray());
        Assert.assertArrayEquals(
                new Object[] { parentDictionaryError, parentFieldError, childFieldError, childDictionaryError },
                childDictionaryValidator.validate(this.dictionaryStructure, true, true).toArray());
        Assert.assertArrayEquals(
                new Object[] { parentDictionaryError, parentFieldError, childFieldError, parentMessageOldError, parentMessageNewError, parentMessageFieldError,
                        childMessageFieldError, childMessageOldError, childMessageNewError, childDictionaryError },
                childDictionaryValidator.validate(this.dictionaryStructure, true, null).toArray());
    }

}
