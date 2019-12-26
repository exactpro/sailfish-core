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
package com.exactpro.sf.common.impl.messages.xml.configuration;

import com.exactpro.sf.common.impl.messages.all.configuration.IDictionary;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Json class for adapter for {@link Dictionary}.
 */
public class XMLDictionary implements IDictionary {

    private static final long serialVersionUID = 3858329210982328592L;

    protected final String name;
    protected final String description;
    protected final List<XMLAttribute> attributes;
    protected final List<XMLField> fields;
    protected final List<XMLMessage> messages;

    /**
     * Create adapter and reference for field or message
     * @param field {@link Field} or {@link Message}
     * @param listOfField {@link List}<{@link XMLField}>
     * @param listOfMessage {@link List}<{@link XMLMessage}>
     * @param fieldMap {@link Map}<{@link String}, {@link XMLField}
     * @return Adapter for object <b>field</b>.
     */
    private XMLField createBeanWithReference(Field field, List<XMLField> listOfField, List<XMLMessage> listOfMessage, Map<String, XMLField> fieldMap) {
        if (field == null) {
            return null;
        }

        XMLField fieldAdapter = field.getId() != null ? fieldMap.get(field.getId()) : null;

        if (fieldAdapter != null) {
            return fieldAdapter;
        }

        if (field instanceof Message) {
            fieldAdapter = new XMLMessage((Message) field);

            if (listOfMessage != null) {
                listOfMessage.add((XMLMessage) fieldAdapter);
            }
        } else {
            fieldAdapter = new XMLField(field);

            if (listOfField != null) {
                listOfField.add(fieldAdapter);
            }
        }

        if (fieldAdapter.getId() != null) {
            fieldMap.put(fieldAdapter.getId(), fieldAdapter);
        }

        Field reference = (Field) field.getReference();
        if (reference != null) {
            fieldAdapter.setReference(createBeanWithReference(reference, fields, messages, fieldMap));
        }

        return fieldAdapter;
    }

    public XMLDictionary(Dictionary dictionary) {
        this.name = dictionary.name;
        this.description = dictionary.description;

        if (dictionary.getAttributes() != null) {
            attributes = new ArrayList<>(dictionary.getAttributes().size());
            for (Attribute attribute : dictionary.getAttributes()) {
                attributes.add(new XMLAttribute(attribute));
            }
        } else {
            attributes = Collections.emptyList();
        }

        Map<String, XMLField> fieldMap = new HashMap<>();

        if (dictionary.getFields() != null && dictionary.getFields() != null) {
            fields = new ArrayList<>(dictionary.getFields().size());
            for (Field field : dictionary.getFields()) {
               createBeanWithReference(field, fields, null, fieldMap);
            }
        } else {
            fields = Collections.emptyList();
        }

        if (dictionary.getMessages() != null && dictionary.getMessages() != null) {
            messages = new ArrayList<>(dictionary.getMessages().size());
            for (Message message : dictionary.getMessages()) {
                XMLMessage messageAdapter = (XMLMessage) createBeanWithReference(message, fields, messages, fieldMap);

                for (Field field : message.getFields()) {
                    createBeanWithReference(field, messageAdapter.getFields(), null, fieldMap);
                }
            }
        } else {
            messages = Collections.emptyList();
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public List<XMLAttribute> getAttributes() {
        return attributes;
    }

    @Override
    public List<XMLField> getFields() {
        return fields;
    }

    @Override
    public List<XMLMessage> getMessages() {
        return messages;
    }
}
