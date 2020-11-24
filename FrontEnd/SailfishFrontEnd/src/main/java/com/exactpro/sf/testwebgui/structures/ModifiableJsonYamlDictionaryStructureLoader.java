/******************************************************************************
 * Copyright 2009-2020 Exactpro (Exactpro Systems Limited)
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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.exactpro.sf.common.impl.messages.all.configuration.IAttribute;
import com.exactpro.sf.common.impl.messages.all.configuration.IDictionary;
import com.exactpro.sf.common.impl.messages.all.configuration.IField;
import com.exactpro.sf.common.impl.messages.all.configuration.IMessage;
import com.exactpro.sf.common.impl.messages.json.configuration.JsonMessage;
import com.exactpro.sf.common.impl.messages.xml.configuration.JavaType;
import com.exactpro.sf.common.messages.structures.IAttributeStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.messages.structures.ModifiableStructureBuilder;
import com.exactpro.sf.common.messages.structures.StructureBuilder;
import com.exactpro.sf.common.messages.structures.loaders.JsonYamlDictionaryStructureLoader;
import com.exactpro.sf.common.util.EPSCommonException;

public class ModifiableJsonYamlDictionaryStructureLoader extends JsonYamlDictionaryStructureLoader {

    private Map<ModifiableFieldStructure, String> fieldReferencesMap;
    private IDictionary dictionary;

    public ModifiableJsonYamlDictionaryStructureLoader() {
        super(false);
    }

    @Override
    public ModifiableDictionaryStructure load(InputStream input) throws EPSCommonException {
        fieldReferencesMap = new HashMap<>();

        dictionary = getDictionary(input);
        ModifiableDictionaryStructure result = (ModifiableDictionaryStructure)convert(dictionary);

        for (ModifiableFieldStructure fieldStructure : fieldReferencesMap.keySet()) {
            fieldStructure.setReference(result.getFields().get(fieldReferencesMap.get(fieldStructure)));
        }
        return result;
    }

    /**
     *  Re-save dictionary
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        if (args.length > 0) {
            ModifiableJsonYamlDictionaryStructureLoader loader = new ModifiableJsonYamlDictionaryStructureLoader();

            for (String file : args) {
                Path path = Paths.get(file);
                try {
                    if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
                        ModifiableDictionaryStructure dictionaryStructure;
                        try (InputStream inputStream = Files.newInputStream(path, LinkOption.NOFOLLOW_LINKS)) {
                            dictionaryStructure = loader.load(inputStream);
                        }

                        JsonYamlDictionaryStructureWriter.write(dictionaryStructure, path.toFile(), false);
                    } else {
                        System.err.println("Path '" + path + "' does not exist");
                    }
                } catch (Exception e) {
                    System.err.println("Problem with path '" + path + "'");
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected ModifiableFieldStructure createFieldStructure(IField field, boolean isTemplate, String id, String name,
            String namespace, String description, JavaType javaType,
            Boolean isRequired, Boolean isCollection, Boolean isServiceName, String defaultValue) {

        ModifiableFieldStructure fieldStructure = new ModifiableFieldStructure(id, name, namespace, description,
                null, null, javaType, isRequired, isCollection, isServiceName, defaultValue);

        if (field.getReference() != null) {
            if (!(field.getReference() instanceof JsonMessage)) {
                fieldReferencesMap.put(fieldStructure, field.getReference().getName());
            }
        }

        fieldStructure.setAttributes(getAttributes(field, false, null));
        if (isTemplate) {
            fieldStructure.setValues(getAttributes(field, true, javaType));
        }

        return fieldStructure;
    }

    @SuppressWarnings("unchecked")
    protected Map<String, ModifiableAttributeStructure> getAttributes(IField field, boolean isValues, JavaType javaType) {
        List<IAttribute> attributes = new ArrayList<>();

        if (isValues) {
            collectFieldValues(field, attributes);
        } else {
            collectFieldAttributes(field, attributes);
        }

        Map<String, ModifiableAttributeStructure> result = (Map<String, ModifiableAttributeStructure>)(Map<String, ?>)collectAttributeStructures(attributes, javaType, isValues);

        return result.isEmpty() ? null : result;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected ModifiableMessageStructure createMessageStructure(IMessage message, String id, String name, String namespace,
                                                                String description, boolean isRequired, boolean isCollection,
                                                                Map<String, IFieldStructure> fields,
                                                                Map<String, ? extends IAttributeStructure> attributes, IMessageStructure reference) {

        return new ModifiableMessageStructure(id, name, namespace, description,
                (Map<String, ModifiableFieldStructure>)(Map<String, ?>)fields,
                (Map<String, ModifiableAttributeStructure>)attributes,
                isRequired, isCollection,
                (ModifiableMessageStructure)reference);
    }

    @Override
    protected ModifiableMessageStructure createMessageStructure(IMessage message, IField field, String id, String name, String namespace,
            String description, Boolean isRequired, Boolean isCollection, IMessageStructure reference) {

        Map<String, ModifiableAttributeStructure> attributes = getAttributes(field, false, null);

        return new ModifiableMessageStructure(id, name, namespace, description, attributes, isRequired, isCollection,
                (ModifiableMessageStructure)reference);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected ModifiableDictionaryStructure createDictionaryStructure(String namespace, String description, Map<String, IAttributeStructure> dictionaryAttributes,
            Map<String, IMessageStructure> msgStructures, Map<String, IFieldStructure> fieldStructures) {

        Map<String, IMessageStructure> messageStructures = new LinkedHashMap<>();

        for (IMessage message : dictionary.getMessages()) {
            messageStructures.put(message.getName(), msgStructures.remove(message.getName()));
        }

        return new ModifiableDictionaryStructure(namespace, description,
                (Map<String, ModifiableAttributeStructure>)(Map<?, ?>)dictionaryAttributes,
                (Map<String, ModifiableMessageStructure>)(Map<?, ?>)messageStructures,
                (Map<String, ModifiableFieldStructure>)(Map<?, ?>)fieldStructures);
    }

    @Override
    protected IAttributeStructure createAttributeStructure(String name, String value, Object castValue, JavaType type) {
        return new ModifiableAttributeStructure(name, value, castValue, type);
    }

    @Override
    protected <K, V> Map<K, V> initMap() {
        return new LinkedHashMap<>();
    }

    @Override
    protected StructureBuilder initStructureBuilder(String namespace) {
        return new ModifiableStructureBuilder(namespace);
    }
}
