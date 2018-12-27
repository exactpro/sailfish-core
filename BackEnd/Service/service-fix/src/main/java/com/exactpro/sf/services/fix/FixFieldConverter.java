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
package com.exactpro.sf.services.fix;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.services.tcpip.IFieldConverter;

public class FixFieldConverter implements IFieldConverter {
    private final Map<String, String> fieldToTag;
    private final Map<String, String> tagToField;


    public FixFieldConverter() {
        this.fieldToTag = new HashMap<>();
        this.tagToField = new HashMap<>();
    }

    @Override
    public IMessage convertFields(IMessage message, IMessageFactory messageFactory, boolean toHumanReadable) {
        IMessage result = messageFactory.createMessage(message.getName(), message.getNamespace());

        String targetName = null;
        Object targetValue = null;
        for (String fieldName : message.getFieldNames()) {
            if (fieldName.matches("\\d+") && toHumanReadable) {
                targetName = this.tagToField.get(fieldName);
            } else if (!fieldName.matches("\\d+") && !toHumanReadable) {
                targetName = this.fieldToTag.get(fieldName);
            } else {
                targetName = fieldName;
            }

            targetName = targetName != null ? targetName : fieldName;

            targetValue = message.getField(fieldName);

            if (targetValue instanceof List) {
                List<Object> list = new ArrayList<>();

                for (Object element : (List<?>) targetValue) {
                    if (element instanceof IMessage) {
                        list.add(convertFields((IMessage) element, messageFactory, toHumanReadable));
                    } else {
                        list.add(element);
                    }
                }

                targetValue = list;
            } else if (targetValue instanceof IMessage) {
                targetValue = convertFields((IMessage) targetValue, messageFactory, toHumanReadable);
            }

            result.addField(targetName, targetValue);
        }

        return result;
    }

    @Override
    public Object convertValue(Object value, boolean toHumanReadable) {
        return toHumanReadable ? convertToName((String)value) : convertToTag((String)value);
    }

    @Override
    public void init(IDictionaryStructure dictionary, String namespace) {
        this.fieldToTag.clear();
        this.tagToField.clear();

        for (IFieldStructure fieldType : dictionary.getFieldStructures()) {
            String tag = fieldType.getAttributeValueByName(FixMessageHelper.ATTRIBUTE_TAG).toString();

            this.tagToField.put(tag, fieldType.getName());
            this.fieldToTag.put(fieldType.getName(), tag);
        }
    }

    public String convertToTag(String value) {
        if(value.matches("\\d+")) {
            return value;
        }

        return fieldToTag.get(value);
    }

    public String convertToName(String value) {
        if(value.matches("\\d+")) {
            return tagToField.get(value);
        }

        return value;
    }
}