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
package com.exactpro.sf.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.NotImplementedException;

import com.exactpro.sf.common.codecs.AbstractCodec;
import com.exactpro.sf.common.messages.AttributeNotFoundException;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.MessageNotFoundException;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;

public abstract class MessageHelper {

    public static final String FIELD_MESSAGE_TYPE = "MessageType";
    public static final String ATTRIBUTE_IS_ADMIN = "IsAdmin";
    public static final String ATTRIBUTE_MESSAGE_TYPE = FIELD_MESSAGE_TYPE;
    public static final String ATTRIBUTE_DESCRIPTION_PREFIX = "DescriptionPrefix";

    private volatile IMessageFactory messageFactory;
    private volatile IDictionaryStructure dictionaryStructure;
    private volatile String namespace;

    public void init(IMessageFactory messageFactory, IDictionaryStructure dictionaryStructure) {
        this.messageFactory = messageFactory;
        this.dictionaryStructure = dictionaryStructure;
        this.namespace = this.dictionaryStructure.getNamespace();
    }

    public IMessage prepareMessageToEncode(IMessage message, Map<String, String> params) {
        return message;
    }

    public IMessage prepareMessageToEncode(Map<String, Object> message, Map<String, String> params) {
        throw new NotImplementedException("");
    }

    public boolean isAdmin(IMessage message) throws MessageNotFoundException, AttributeNotFoundException {
        IMessageStructure messageStructure = this.dictionaryStructure.getMessageStructure(message.getName());
        if (messageStructure != null) {
            Object isAdminAttribute = messageStructure.getAttributeValueByName(ATTRIBUTE_IS_ADMIN);
            if (isAdminAttribute instanceof Boolean) {
                return (Boolean)isAdminAttribute;
            }
            throw new AttributeNotFoundException("Message " + messageStructure.getName() + " does not contains boolean attribute name " + ATTRIBUTE_IS_ADMIN + " namespace " + message.getNamespace());
        }

        return true;
    }

    public AbstractCodec getCodec(IServiceContext serviceContext) {
        throw new NotImplementedException("");
    }

    public IMessageFactory getMessageFactory() {
        return messageFactory;
    }

    public IDictionaryStructure getDictionaryStructure() {
        return dictionaryStructure;
    }

    public String getNamespace() {
        return namespace;
    }

    public static HashMap<String, Object> convert(IMessage message) {
        if (message == null) {
            throw new NullPointerException();
        }

        HashMap<String, Object> result = new HashMap<>();

        for (String fieldName : message.getFieldNames()) {
            Object field = message.getField(fieldName);
            result.put(fieldName, convertValue(field));
        }

        return result;
    }

    private static Object convertValue(Object field) {
        if (field instanceof IMessage) {
            return convert((IMessage)field);
        } else if (field instanceof List<?>) {
            List<Object> list = new ArrayList<>();
            for (Object subField : (List<?>)field) {
                list.add(convertValue(subField));
            }
            return list;
        }
        return field;
    }
}
