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

import com.exactpro.sf.common.impl.messages.xml.configuration.JavaType;
import com.exactpro.sf.common.messages.structures.IAttributeStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.configuration.dictionary.DictionaryValidationError;
import com.exactpro.sf.configuration.dictionary.DictionaryValidationErrorLevel;
import com.exactpro.sf.configuration.dictionary.DictionaryValidationErrorType;
import com.exactpro.sf.configuration.dictionary.ValidationHelper;
import com.exactpro.sf.services.fix.FixMessageHelper;
import com.exactpro.sf.services.fix.QFJDictionaryAdapter;

public class GroupEntityValidator extends EntityValidator {
    @Override
    public void validateEntity(List<DictionaryValidationError> errors, IMessageStructure message) {
        checkAttributes(errors, message);
    }

    private void checkAttributes(List<DictionaryValidationError> errors, IMessageStructure message) {

        ValidationHelper.checkMessageAttributeType(errors, message,
                QFJDictionaryAdapter.ATTRIBUTE_FIX_TYPE, JavaType.JAVA_LANG_STRING, null);

        boolean hasTagAttribute = ValidationHelper.checkMessageAttributeType(errors, message,
                FixMessageHelper.ATTRIBUTE_TAG, JavaType.JAVA_LANG_INTEGER, null);

        if (dictionary != null && hasTagAttribute) {
            IAttributeStructure tagAttribute = message.getAttributes().get(FixMessageHelper.ATTRIBUTE_TAG);
            Object value  = tagAttribute.getCastValue();
            if (!(value instanceof Integer)) {
                errors.add(new DictionaryValidationError(message.getName(), null,
                        String.format("Value for 'tag' attribute is missing or have incorrect type for [%s] group", message.getName()),
                        DictionaryValidationErrorLevel.MESSAGE, DictionaryValidationErrorType.ERR_ATTRIBUTES));
                return;
            }
            Integer castValue = (Integer) value;
            boolean existence = false;
            for (IFieldStructure field : dictionary.getFieldStructures()) {
                Object tag = field.getAttributeValueByName(FixMessageHelper.ATTRIBUTE_TAG);
                if (castValue.equals(tag)) {
                    existence = true;
                    break;
                }
            }
            if (!existence) {
                errors.add(new DictionaryValidationError(message.getName(), null,
                    String.format("Field [%s] is missing in <fields> section for [%s] group", castValue, message.getName()),
                    DictionaryValidationErrorLevel.MESSAGE, DictionaryValidationErrorType.ERR_ATTRIBUTES));
            }
        }
    }

}
