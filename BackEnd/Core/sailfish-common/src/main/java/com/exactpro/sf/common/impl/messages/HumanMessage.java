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
package com.exactpro.sf.common.impl.messages;

import java.util.List;

import com.exactpro.sf.common.messages.IHumanMessage;
import com.exactpro.sf.common.messages.MessageUtil;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.util.EnumUtils;

public class HumanMessage implements IHumanMessage {

    protected static final String FIELD_DELIMITER = "; ";
    protected static final char FIELD_VALUE_DELIMITER = '=';
    protected static final char COLLECTION_END = ']';
    protected static final char COLLECTION_START = '[';
    protected static final char SUB_MESSAGE_END = '}';
    protected static final char SUB_MESSAGE_START = '{';

    protected final StringBuilder builder;

    public HumanMessage() {
        this.builder = new StringBuilder();
    }

    @Override
    public void addField(IFieldStructure fieldStructure, String fieldName, Object value) {
        if (fieldName == null) {
            throw new IllegalArgumentException("[fieldName] could not be null");
        }
        if(builder.length() != 0) {
            builder.append(FIELD_DELIMITER);
        }

        appendName(fieldName, fieldStructure);
        builder.append(FIELD_VALUE_DELIMITER);
        if (value instanceof List<?>) {
            List<?> list = (List<?>) value;
            builder.append(COLLECTION_START);
            if (!list.isEmpty()) {
                appendValue(fieldStructure, list.get(0));
                for (int i = 1; i < list.size(); i++) {
                    builder.append(FIELD_DELIMITER);
                    appendValue(fieldStructure, list.get(i));
                }
            }
            builder.append(COLLECTION_END);
        } else {
            appendValue(fieldStructure, value);
        }
    }

    protected void appendName(String name, IFieldStructure fieldStructure) {
        builder.append(name);
    }

    protected void appendValue(IFieldStructure fieldStructure, Object value) {
        if (value instanceof HumanMessage) {
            builder.append(SUB_MESSAGE_START)
                .append(((HumanMessage)value).builder)
                .append(SUB_MESSAGE_END);
        } else if (fieldStructure != null && fieldStructure.isEnum()) {
            String converted = EnumUtils.getAlias(fieldStructure, value);
            builder.append(converted == null ? value : converted);
        }
        else {
            builder.append(value);
        }
    }

    @Override
    public String toString() {
        return MessageUtil.escapeCharacter(builder);
    }
}
