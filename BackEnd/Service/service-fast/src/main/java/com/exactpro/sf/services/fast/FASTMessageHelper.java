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
package com.exactpro.sf.services.fast;

import java.util.HashMap;
import java.util.Map;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.services.MessageHelper;

/**
 * @author nikita.smirnov
 *
 */
public class FASTMessageHelper extends MessageHelper {

    public static final String MESSAGE_TYPE_ATTR_NAME = "MessageType";
    public static final String MESSAGE_TYPE_FIELD = MESSAGE_TYPE_ATTR_NAME;
    public static final String TEMPLATE_ID_ATTR_NAME = "templateId";
    public static final String TEMPLATE_ID_FIELD = TEMPLATE_ID_ATTR_NAME;
    public static final String TEMPLATE_ATTRIBYTE = "Template";
    public static final String DEFAULT_TEMPLATE_PATH = "fast/templates";

    private final Map<String, Object> messageTypes = new HashMap<>();
    private final Map<String, Object> templateIds = new HashMap<>();

    @Override
    public void init(IMessageFactory messageFactory, IDictionaryStructure dictionaryStructure) {
        super.init(messageFactory, dictionaryStructure);

        messageTypes.clear();
        templateIds.clear();

        if (dictionaryStructure != null) {
            for (IMessageStructure messageStructure : dictionaryStructure.getMessageStructures()) {
                Object messageType = getAttributeValueByName(messageStructure, MESSAGE_TYPE_FIELD, MESSAGE_TYPE_ATTR_NAME);
                if (messageType != null) {
                    messageTypes.put(messageStructure.getName(), messageType);
                }
                Object templateId = getAttributeValueByName(messageStructure, TEMPLATE_ID_FIELD, TEMPLATE_ID_ATTR_NAME);
                if (templateId != null) {
                    templateIds.put(messageStructure.getName(), templateId);
                }
            }
        }
    }

    @Override
    public IMessage prepareMessageToEncode(IMessage message, Map<String, String> params) {
        addCachedValue(message, MESSAGE_TYPE_FIELD, this.messageTypes);
        addCachedValue(message, TEMPLATE_ID_FIELD, this.templateIds);
        return message;
    }

    public static String getTemplatePath(String templateName) {
        return DEFAULT_TEMPLATE_PATH + "/" + templateName;
    }

    private Object getAttributeValueByName(IMessageStructure structure, String fieldName, String attributeName) {
        if (structure != null) {
            if (structure.getFieldNames().contains(fieldName)) {
                return structure.getAttributeValueByName(attributeName);
            }
        }
        return null;
    }

    private void addCachedValue(IMessage message, String fieldName, Map<String, Object> cache) {
        if (!message.isFieldSet(fieldName)) {
            Object value = cache.get(message.getName());
            if (value != null) {
                message.addField(fieldName, value);
            }
        }
    }

}
