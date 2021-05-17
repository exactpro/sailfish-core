/*******************************************************************************
 * Copyright 2009-2021 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.common.messages.structures.loaders;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.exactpro.sf.common.impl.messages.json.configuration.FieldMessageDeserializer;
import com.exactpro.sf.common.impl.messages.json.configuration.JsonField;
import com.exactpro.sf.common.impl.messages.json.configuration.JsonMessage;
import com.exactpro.sf.common.impl.messages.json.configuration.JsonYamlDictionary;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.common.util.SingleKeyHashMap;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature;

/**
 * Java class for load {@link IDictionaryStructure} from JSON/YAML format.
 */
public class JsonYamlDictionaryStructureLoader extends AbstractDictionaryStructureLoader {

    private final ObjectMapper objectMapper;

    public JsonYamlDictionaryStructureLoader(boolean aggregateAttributes) {
        super(aggregateAttributes);
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator
                .builder()
                .allowIfBaseType(JsonYamlDictionary.class)
                .build();
        objectMapper = new ObjectMapper(
                new YAMLFactory()
                        .disable(Feature.USE_NATIVE_OBJECT_ID)
                        .disable(Feature.USE_NATIVE_TYPE_ID)
        )
                .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
                .activateDefaultTyping(ptv);

        //For embedded messages
        SimpleModule module = new SimpleModule();
        module.addDeserializer(JsonField.class, new FieldMessageDeserializer());
        objectMapper.registerModule(module);
    }

    public JsonYamlDictionaryStructureLoader() {
        this(true);
    }

    private void setReferenceForJsonFieldList(List<JsonField> list, Map<String, JsonField> nameToFieldOrMessage) {
        for (JsonField field : list) {
            String refName = field.getReferenceName();
            if (!StringUtils.isEmpty(refName)) {
                JsonField mapRef = nameToFieldOrMessage.get(refName);
                if (mapRef != null) {
                    field.setReference(mapRef);
                } else {
                    logger.warn("Field [" + field.getName() + "] have reference to unknown field [" + refName + "]");
                }
            }
            if (field instanceof JsonMessage) {
                setReferenceForJsonFieldList(((JsonMessage)field).getFields(), nameToFieldOrMessage);
            }
        }
    }

    @Override
    public JsonYamlDictionary getDictionary(InputStream inputStream) {
        try {
            JsonYamlDictionary dictionary = objectMapper.readerFor(JsonYamlDictionary.class).readValue(inputStream);

            Map<String, JsonField> nameToFieldOrMessage = new SingleKeyHashMap<>();

            try {
                for (JsonField field : dictionary.getFields()) {
                    nameToFieldOrMessage.put(field.getId(), field);
                }
            } catch (IllegalArgumentException e) {
                throw new EPSCommonException("Duplicate fields in dictionary with name: " + dictionary.getName() , e);
            }

            for (JsonMessage message : dictionary.getMessages()){
                try {
                    nameToFieldOrMessage.put(message.getId(), message);
                } catch (IllegalArgumentException e) {
                    throw new EPSCommonException("Duplicate message and field in dictionary with name: " + dictionary.getName(), e);
                }
            }

            setReferenceForJsonFieldList(dictionary.getFields(), nameToFieldOrMessage);

            for (JsonMessage message : dictionary.getMessages()) {
                String refMessageName = message.getReferenceName();
                if (!StringUtils.isEmpty(refMessageName)) {
                    JsonField refObject = nameToFieldOrMessage.get(refMessageName);

                    if (refObject != null) {
                        message.setReference(refObject);
                    }
                }

                setReferenceForJsonFieldList(message.getFields(), nameToFieldOrMessage);
            }

            return dictionary;
        } catch (IOException e) {
            throw new EPSCommonException("Wrong input format", e);
        }
    }
}
