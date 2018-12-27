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

import java.util.List;

import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.configuration.dictionary.DictionaryValidationError;
import com.exactpro.sf.configuration.dictionary.ValidationHelper;
import com.exactpro.sf.services.fix.FixMessageHelper;

public class HeaderEntityValidator extends EntityValidator {
    @Override
    public void validateEntity(List<DictionaryValidationError> errors, IMessageStructure message) {
        ValidationHelper.checkRequiredField(errors, message, FixMessageHelper.BEGIN_STRING_FIELD);
        ValidationHelper.checkRequiredField(errors, message, FixMessageHelper.BODY_LENGTH_FIELD);
        ValidationHelper.checkRequiredField(errors, message, FixMessageHelper.MSG_TYPE_FIELD);
        ValidationHelper.checkRequiredField(errors, message, FixMessageHelper.SENDER_COMP_ID_FIELD);
        ValidationHelper.checkRequiredField(errors, message, FixMessageHelper.TARGET_COMP_ID_FIELD);
        ValidationHelper.checkRequiredField(errors, message, FixMessageHelper.MSG_SEQ_NUM_FIELD);
    }
}

