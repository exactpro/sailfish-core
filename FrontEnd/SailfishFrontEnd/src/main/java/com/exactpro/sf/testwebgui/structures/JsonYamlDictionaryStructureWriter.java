/*******************************************************************************
 * Copyright 2009-2019 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.testwebgui.structures;

import com.exactpro.sf.common.impl.messages.json.JsonYamlDictionaryWriter;
import com.exactpro.sf.common.impl.messages.json.configuration.JsonAttribute;
import com.exactpro.sf.common.impl.messages.json.configuration.JsonField;
import com.exactpro.sf.common.impl.messages.json.configuration.JsonMessage;
import com.exactpro.sf.common.impl.messages.json.configuration.JsonYamlDictionary;
import com.exactpro.sf.common.messages.structures.IAttributeStructure;
import com.exactpro.sf.common.util.EPSCommonException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JsonYamlDictionaryStructureWriter {

    public static void write(ModifiableDictionaryStructure dictionaryStructure, OutputStream output, boolean asYaml) {

        JsonYamlDictionary dictionary = convertStructureToJson(dictionaryStructure);

        JsonYamlDictionaryWriter.write(dictionary, output, asYaml);
    }

    public static void write(ModifiableDictionaryStructure dictionaryStructure, File file, boolean asYaml) throws IOException {

        try (OutputStream output = new FileOutputStream(file)) {
            write(dictionaryStructure, output, asYaml);
        } catch (FileNotFoundException e) {
            throw new EPSCommonException("Failed to write dictionary: " + dictionaryStructure.getNamespace(), e);
        }
    }

    private static JsonYamlDictionary convertStructureToJson(ModifiableDictionaryStructure dictionaryStructure) {

        JsonYamlDictionary dictionary = new JsonYamlDictionary(dictionaryStructure.getNamespace());
        dictionary.setDescription(dictionaryStructure.getDescription());

        List<JsonAttribute> attributes = new ArrayList<>();
        if (dictionaryStructure.getAttributes() != null) {
            for (IAttributeStructure attributeStructure : dictionaryStructure.getAttributes().values()) {
                attributes.add(createAttributeFromStructure(attributeStructure));
            }
        }
        dictionary.setAttributes(attributes);

        List<JsonField> fields = new ArrayList<>();
        List<JsonMessage> messages = new ArrayList<>();

        Map<ModifiableFieldStructure, JsonField> structureFieldMap = new LinkedHashMap<>();
        Map<JsonField, ModifiableFieldStructure> referenceMap = new LinkedHashMap<>();

        for (ModifiableFieldStructure fieldStructure : dictionaryStructure.getImplFieldStructures()) {
            JsonField jsonField = createFieldFromStructure(fieldStructure);

            addAttributes(fieldStructure, jsonField);
            addValues(fieldStructure, jsonField);

            structureFieldMap.put(fieldStructure, jsonField);

            if (fieldStructure.getReference() != null) {
                referenceMap.put(jsonField, fieldStructure.getImplReference());
            }

            fields.add(jsonField);
        }

        for (JsonField referencedJsonField : referenceMap.keySet()) {
            referencedJsonField.setReference(structureFieldMap.get(referenceMap.get(referencedJsonField)));
        }

        referenceMap.clear();

        Map<ModifiableMessageStructure, JsonMessage> structureMessageMap = new LinkedHashMap<>();

        for (ModifiableMessageStructure messageStructure : dictionaryStructure.getImplMessageStructures()) {

            JsonMessage jsonMessage = new JsonMessage();

            jsonMessage.setName(messageStructure.getName());
            jsonMessage.setDescription(messageStructure.getDescription());

            addAttributes(messageStructure, jsonMessage);

            structureMessageMap.put(messageStructure, jsonMessage);

            for (ModifiableFieldStructure fieldStructure : messageStructure.getImplFields().values()) {

                JsonField jsonField = createFieldFromStructure(fieldStructure);

                addAttributes(fieldStructure, jsonField);

                if (fieldStructure.getReference() != null) {

                    if (fieldStructure.getImplReference().isImplSimple() || fieldStructure.getImplReference().isImplEnum()) {
                        jsonField.setReference(structureFieldMap.get(fieldStructure.getImplReference()));
                        jsonField.setReferenceName(jsonField.getReference().getName());
                    } else {
                        referenceMap.put(jsonField, fieldStructure.getImplReference());
                    }
                }

                jsonMessage.getFields().add(jsonField);
            }

            messages.add(jsonMessage);
        }

        for (JsonField referencedJsonField : referenceMap.keySet()) {
            referencedJsonField.setReference(structureMessageMap.get(referenceMap.get(referencedJsonField)));
            referencedJsonField.setReferenceName(referencedJsonField.getReference().getName());
        }


        dictionary.setFields(fields);
        dictionary.setMessages(messages);

        return dictionary;
    }

    private static JsonField createFieldFromStructure(ModifiableFieldStructure fieldStructure) {

        JsonField jsonField = new JsonField();

        jsonField.setName(fieldStructure.getName());
        jsonField.setDescription(fieldStructure.getDescription());
        jsonField.setType(fieldStructure.getImplJavaType());

        if (fieldStructure.isCollection()) {
            jsonField.setIsCollection(fieldStructure.isCollection());
        }

        if (fieldStructure.isRequired()) {
            jsonField.setRequired(fieldStructure.isRequired());
        }

        if (!fieldStructure.isSubMessage()) {
            jsonField.setDefaultValue(fieldStructure.getImplDefaultValue());
            if (fieldStructure.getReference() != null) {
                //type contains in reference
            } else {
                jsonField.setType(fieldStructure.getImplJavaType());
            }
        }

        return jsonField;
    }

    private static void addValues(ModifiableFieldStructure fieldStructure, JsonField jsonField) {
        for (IAttributeStructure valueStructure : fieldStructure.getImplValues()) {

            JsonAttribute jsonAttribute = new JsonAttribute();
            jsonAttribute.setName(valueStructure.getName());
            jsonAttribute.setValue(valueStructure.getValue());

            jsonField.getValues().add(jsonAttribute);
        }
    }

    private static void addAttributes(ModifiableFieldStructure fieldStructure, JsonField jsonField) {
        for (IAttributeStructure attributeStructure : fieldStructure.getImplAttributes()) {

            jsonField.getAttributes().add(createAttributeFromStructure(attributeStructure));
        }
    }

    private static JsonAttribute createAttributeFromStructure(IAttributeStructure attributeStructure) {
        JsonAttribute attribute = new JsonAttribute();

        attribute.setName(attributeStructure.getName());
        attribute.setValue(attributeStructure.getValue());
        attribute.setType(attributeStructure.getType());

        return attribute;

    }
}
