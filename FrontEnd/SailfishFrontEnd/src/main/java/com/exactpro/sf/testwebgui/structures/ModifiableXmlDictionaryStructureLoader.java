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

import com.exactpro.sf.common.impl.messages.xml.configuration.Attribute;
import com.exactpro.sf.common.impl.messages.xml.configuration.Dictionary;
import com.exactpro.sf.common.impl.messages.xml.configuration.Field;
import com.exactpro.sf.common.impl.messages.xml.configuration.JavaType;
import com.exactpro.sf.common.impl.messages.xml.configuration.Message;
import com.exactpro.sf.common.messages.structures.IAttributeStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.messages.structures.ModifiableStructureBuilder;
import com.exactpro.sf.common.messages.structures.StructureBuilder;
import com.exactpro.sf.common.messages.structures.loaders.XmlDictionaryStructureLoader;
import com.exactpro.sf.common.util.EPSCommonException;

public class ModifiableXmlDictionaryStructureLoader extends XmlDictionaryStructureLoader {

	// Maps Structure/Reference Name
	private Map<ModifiableFieldStructure, String> fieldReferencesMap;
	private Dictionary dic;

	public ModifiableXmlDictionaryStructureLoader() {
		super(false);
	}

	@Override
	public ModifiableDictionaryStructure load(InputStream input) throws EPSCommonException {

		this.fieldReferencesMap = new HashMap<>();

		dic = loadXmlEntity(input);
		
		ModifiableDictionaryStructure result = (ModifiableDictionaryStructure)convert(dic);

		for (ModifiableFieldStructure struct : fieldReferencesMap.keySet()) {
			struct.setReference(result.getFieldStructure(fieldReferencesMap.get(struct)));
		}

		this.fieldReferencesMap = null;

		return result;
	}

	/**
	 *  Re-save dictionary
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
	    if (args.length > 0) {
    	    ModifiableXmlDictionaryStructureLoader loader = new ModifiableXmlDictionaryStructureLoader();
    	    
    	    for (String file : args) {
    	        Path path = Paths.get(file);
    	        try {
        	        if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
        	            ModifiableDictionaryStructure dictionaryStructure = null;
                        try (InputStream inputStream = Files.newInputStream(path, LinkOption.NOFOLLOW_LINKS)) {
                            dictionaryStructure = loader.load(inputStream);
                        }

                        XmlDictionaryStructureWriter.write(dictionaryStructure, path.toFile());
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
	protected ModifiableFieldStructure createFieldStructure(Field field, boolean isTemplate, String id, String name,
			String namespace, String description, JavaType javaType, Boolean isRequired, Boolean isCollection,
			Boolean isServiceName, String defaultValue) {

		ModifiableFieldStructure struct = new ModifiableFieldStructure(id, name, namespace, description,
				null, null, javaType, isRequired, isCollection, isServiceName, defaultValue);

		if (field.getReference() != null) {
			if (!(field.getReference() instanceof Message)) {
				fieldReferencesMap.put(struct, ((Field)field.getReference()).getName());
			}
		}

		struct.setAttributes(getAttributes(field, false, null));
		if (isTemplate) {
			struct.setValues(getAttributes(field, true, javaType));
		}

		return struct;
	}

	@SuppressWarnings("unchecked")
	private Map<String, ModifiableAttributeStructure> getAttributes(Field field, boolean isValues, JavaType javaType) {
	    List<Attribute> attributes = new ArrayList<>();

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
	protected ModifiableMessageStructure createMessageStructure(Message message, String id, String name, String namespace,
			String description, List<IFieldStructure> fields, Map<String, IAttributeStructure> attributes) {

		return new ModifiableMessageStructure(id, name, namespace, description,
				(List<ModifiableFieldStructure>)(List<?>)fields,
				(Map<String, ModifiableAttributeStructure>)(Map<String, ?>)attributes);
	}

	@Override
	protected ModifiableMessageStructure createMessageStructure(Message message, Field field, String id, String name, String namespace,
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
        
        for (Message m : dic.getMessages().getMessages()) {
            messageStructures.put(m.getName(), msgStructures.remove(m.getName()));
        }
	    
		return new ModifiableDictionaryStructure(namespace, description,
                (Map<String, ModifiableAttributeStructure>)(Map<?,?>)dictionaryAttributes,
				(Map<String, ModifiableMessageStructure>)(Map<?,?>)messageStructures,
				(Map<String, ModifiableFieldStructure>)(Map<?,?>)fieldStructures);
	}

	@Override
	protected IAttributeStructure createAttributeStructure(String name, String value, Object castValue, JavaType type) {
		return new ModifiableAttributeStructure(name, value, castValue, type);
	}
	
	@Override
	protected <K,V> Map<K, V> initMap() {
	    return new LinkedHashMap<>();
	}
	
	@Override
	protected StructureBuilder initStructureBuilder(String namespace) {
	    return new ModifiableStructureBuilder(namespace);
	}
}