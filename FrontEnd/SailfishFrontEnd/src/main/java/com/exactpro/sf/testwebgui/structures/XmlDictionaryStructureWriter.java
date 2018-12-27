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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import com.exactpro.sf.common.impl.messages.xml.XmlDictionaryWriter;
import com.exactpro.sf.common.impl.messages.xml.configuration.Attribute;
import com.exactpro.sf.common.impl.messages.xml.configuration.Dictionary;
import com.exactpro.sf.common.impl.messages.xml.configuration.Dictionary.Fields;
import com.exactpro.sf.common.impl.messages.xml.configuration.Dictionary.Messages;
import com.exactpro.sf.common.impl.messages.xml.configuration.Field;
import com.exactpro.sf.common.impl.messages.xml.configuration.Message;
import com.exactpro.sf.common.messages.structures.IAttributeStructure;
import com.exactpro.sf.common.util.EPSCommonException;

public class XmlDictionaryStructureWriter {
	
	private XmlDictionaryStructureWriter() {}
	
	public static void write(ModifiableDictionaryStructure dictionaryStructure, OutputStream output) {
		Dictionary dictionary = convertStructureToXml(dictionaryStructure);
		XmlDictionaryWriter.write(dictionary, output);
	}
	
	public static void write(ModifiableDictionaryStructure dictionaryStructure, File file) throws IOException {
		try {
		    try (OutputStream output = new FileOutputStream(file)) {
		        write(dictionaryStructure, output);
		    }
		} catch (FileNotFoundException e) {
			throw new EPSCommonException("A DictionaryStructure convertation exception", e);
		}
	}
	
	private static Dictionary convertStructureToXml(ModifiableDictionaryStructure dictionaryStructure) {
		
		Dictionary dictionary = new Dictionary();
		dictionary.setDescription(dictionaryStructure.getDescription());
		dictionary.setName(dictionaryStructure.getNamespace());
		
		Fields fields = new Fields();
		Messages messages = new Messages();
		
		// Fields adding
		
		Map<ModifiableFieldStructure, Field> structureFieldMap = new LinkedHashMap<>();
		Map<Field, ModifiableFieldStructure> referenceMap = new LinkedHashMap<>();
		
		for (ModifiableFieldStructure fieldStructure : dictionaryStructure.getImplFieldStructures()) {
			
			Field field = createFieldFromStructure(fieldStructure, true);
			
			addAttributes(fieldStructure, field);
			addValues(fieldStructure, field);
			
			structureFieldMap.put(fieldStructure, field);
			
			if (fieldStructure.getReference() != null) {
				referenceMap.put(field, fieldStructure.getImplReference());
			}
			
			fields.getFields().add(field);
		}
		
		for (Field referencedField : referenceMap.keySet()) {
			referencedField.setReference(structureFieldMap.get(referenceMap.get(referencedField)));
		}
		
		referenceMap.clear();
		
		Map<ModifiableMessageStructure, Message> structureMessageMap = new LinkedHashMap<>();
		
		// Messages adding
		
		for (ModifiableMessageStructure messageStructure : dictionaryStructure.getImplMessageStructures()) {
			
			Message message = new Message();
			message.setId("M_" + messageStructure.getName());
			message.setName(messageStructure.getName());
			message.setDescription(messageStructure.getDescription());
			addAttributes(messageStructure, message);
			
			structureMessageMap.put(messageStructure, message);
			
			for (ModifiableFieldStructure fieldStructure : messageStructure.getImplFields()) {
				
				Field field = createFieldFromStructure(fieldStructure, false);
				addAttributes(fieldStructure, field);
				
				if (fieldStructure.getReference() != null) {
					
					if (fieldStructure.getImplReference().isImplSimple() || fieldStructure.getImplReference().isImplEnum()) {
						field.setReference(structureFieldMap.get(fieldStructure.getImplReference()));
					} else {
						referenceMap.put(field, fieldStructure.getImplReference());
					}
				}
				
				message.getFields().add(field);
			}
			
			messages.getMessages().add(message);
		}
		
		for (Field referencedField : referenceMap.keySet()) {
			referencedField.setReference(structureMessageMap.get(referenceMap.get(referencedField)));
		}
		
		dictionary.setFields(fields);
		dictionary.setMessages(messages);
		
		return dictionary;
	}

	private static Field createFieldFromStructure(ModifiableFieldStructure fieldStructure, boolean isTemplateField) {
		
		Field field = new Field();
		
		field.setName(fieldStructure.getName());
		if (isTemplateField) {
		    field.setId("F_" + fieldStructure.getName());
		}
		field.setDescription(fieldStructure.getDescription());
		
		if (Boolean.TRUE.equals(fieldStructure.isCollection())) {
			field.setIsCollection(fieldStructure.isCollection());
		}
		
		if (Boolean.TRUE.equals(fieldStructure.isRequired())) {
			field.setRequired(fieldStructure.isRequired());
		}
		
		if (!fieldStructure.isSubMessage()) {
			field.setDefaultvalue(fieldStructure.getImplDefaultValue());
			if (fieldStructure.getReference() != null) {
			    //type contains in reference
			} else {
			    field.setType(fieldStructure.getImplJavaType());
			}
		}
		
		return field;
	}

	private static void addValues(ModifiableFieldStructure fieldStructure, Field field) {
		for (IAttributeStructure valueStructure : fieldStructure.getImplValues()) {
			
			Attribute value = new Attribute();
			value.setName(valueStructure.getName());
			value.setValue(valueStructure.getValue());

			field.getValues().add(value);
		}
	}

	private static void addAttributes(ModifiableFieldStructure fieldStructure, Field field) {
		for (IAttributeStructure attributeStructure : fieldStructure.getImplAttributes()) {
			
			Attribute attribute = new Attribute();
			attribute.setName(attributeStructure.getName());
			attribute.setValue(attributeStructure.getValue());
			attribute.setType(attributeStructure.getType());
			
			field.getAttributes().add(attribute);
		}
	}
}