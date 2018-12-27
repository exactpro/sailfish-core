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
package com.exactpro.sf.common.messages.structures.loaders;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXParseException;

import com.exactpro.sf.common.impl.messages.xml.XMLTransmitter;
import com.exactpro.sf.common.impl.messages.xml.configuration.Attribute;
import com.exactpro.sf.common.impl.messages.xml.configuration.Dictionary;
import com.exactpro.sf.common.impl.messages.xml.configuration.Field;
import com.exactpro.sf.common.impl.messages.xml.configuration.JavaType;
import com.exactpro.sf.common.impl.messages.xml.configuration.Message;
import com.exactpro.sf.common.messages.structures.IAttributeStructure;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.messages.structures.StructureBuilder;
import com.exactpro.sf.common.messages.structures.StructureUtils;
import com.exactpro.sf.common.messages.structures.impl.AttributeStructure;
import com.exactpro.sf.common.messages.structures.impl.DictionaryStructure;
import com.exactpro.sf.common.messages.structures.impl.FieldStructure;
import com.exactpro.sf.common.messages.structures.impl.MessageStructure;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.common.util.StringUtil;

public class XmlDictionaryStructureLoader implements IDictionaryStructureLoader {

	private static final Logger logger = LoggerFactory.getLogger(XmlDictionaryStructureLoader.class);

	public static final String DEFAULT_SCHEMA_VALIDATOR = "/xsd/dictionary.xsd";

	protected final boolean aggregateAttributes;

    private final Set<String> pendingMessages = new HashSet<>();

	public XmlDictionaryStructureLoader() {
		this(true);
	}

	protected XmlDictionaryStructureLoader(boolean aggregate) {
		this.aggregateAttributes = aggregate;
	}

	public Dictionary loadXmlEntity(InputStream input) throws EPSCommonException {
		InputStream schemaStream = getClass().getResourceAsStream(DEFAULT_SCHEMA_VALIDATOR);

		XMLTransmitter transmitter = XMLTransmitter.getTransmitter();

		ClassLoader currentThreadClassLoader = Thread.currentThread().getContextClassLoader();
		try {
		    Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

			Dictionary dict = transmitter.unmarshal(Dictionary.class, input, schemaStream, null);

			return dict;

		} catch (JAXBException jabEx) {

			SAXParseException saxParseException = null;

			if (null != jabEx.getCause() && (jabEx.getCause() instanceof SAXParseException)) {
				saxParseException = ((SAXParseException) jabEx.getCause());
			} else if (null != jabEx.getLinkedException()
					&& (jabEx.getLinkedException() instanceof SAXParseException)) {
				saxParseException = ((SAXParseException) jabEx.getLinkedException());
			}

			if (null != saxParseException) {

				final String strError = String.format("%4$sThis xml stream structure could not be unmarshaled due to SAXParser error %4$s[%1$s].%4$sError was found on the line [%2$d], column [%3$d].%4$s",
													  saxParseException.getMessage(), saxParseException.getLineNumber(),
													  saxParseException.getColumnNumber(), StringUtil.EOL);

				throw new EPSCommonException(strError, jabEx);

			} else {
				throw new EPSCommonException("This xml stream structure could not be unmarshaled due to SAXParser error.", jabEx);
			}

		} catch (Exception e) {
			throw new EPSCommonException("This xml stream structure could not be unmarshaled.", e);
		} finally {
		    Thread.currentThread().setContextClassLoader(currentThreadClassLoader);
		}
	}

	@Override
	public IDictionaryStructure load(InputStream input) throws EPSCommonException {

		return convert(loadXmlEntity(input));

	}

	@Override
	public String extractNamespace(InputStream input) throws EPSCommonException {
	    return loadXmlEntity(input).getName();
	}

	
	protected StructureBuilder initStructureBuilder(String namespace) {
	    return new StructureBuilder(namespace);
	}
	
	protected <K,V> Map<K,V> initMap() {
	    return new HashMap<>();
	}
	
	public IDictionaryStructure convert(Dictionary dictionary) throws EPSCommonException {
		
	    StructureBuilder builder = initStructureBuilder(dictionary.getName());
		
		for (Field field : dictionary.getFields().getFields()) {

			if (getFieldType(field, true) == null) {
				throw new EPSCommonException("A field " + field.getName()
		                + " with an id " + field.getId()
		                + " has neither a type nor a reference");
			}

			if (field.getName() == null) {
			    throw new EPSCommonException("Field id='" + field.getId() + "' name='" + field.getName() + "' type='"+ field.getType() +"' does not contain name");
			}

			if (isComplex(field)) {
				throw new EPSCommonException("It is impossible to keep messages in fields");
			} else {

				JavaType javaType = getFieldType(field, this.aggregateAttributes);
				String defVal = getDefaultValue(field, this.aggregateAttributes);

				IFieldStructure fieldStructure = createFieldStructure(
						field, true,
						field.getId(),
						field.getName(),
						dictionary.getName(),
						field.getDescription(),
						javaType,
						field.isRequired(),
						field.isIsCollection(),
						field.isIsServiceName(),
						defVal
				);

				builder.addFieldStructure(fieldStructure);
			}
		}

		List<Message> messages = dictionary.getMessages().getMessages(); 
		int i = 0;
		int size = messages.size();

		while (builder.getMsgStructures().size() != size) {

		    //Check index need if dictionary contains messages with same names
		    //Submessages load while loads message contains it
            if (i >= size) {
                
                Set<String> dup = new HashSet<>();
                StringBuilder duplicatedNames = new StringBuilder();
		        
                for (Message m : messages) {
                
                    String messageName = m.getName();
                    
                    if (!dup.add(messageName)) {
                        duplicatedNames.append(messageName);
                        duplicatedNames.append("; ");
                    }
                }
                
                throw new EPSCommonException(
                        "Messages with same names has been detected! Check names of your messages. Message names: " + duplicatedNames.toString());
            }

			Message msg = messages.get(i++);

			if (builder.getMessageStructure(msg.getName()) == null) {
				convertMessage(dictionary.getName(), builder, msg);
			}

            pendingMessages.clear();
		}

		Map<String, IAttributeStructure> dictAttributes = getDictionaryAttributes(dictionary);

		return createDictionaryStructure(dictionary.getName(), dictionary.getDescription(), dictAttributes,
			builder.getMsgStructureMap(), builder.getFieldStructureMap());
	}

	private void convertMessage(String namespace, StructureBuilder builder, Message message) {

        if (!pendingMessages.add(message.getId())) {
            throw new EPSCommonException(String.format("Recursion at message id: '%s' has been detected!", message.getId()));
        }

        try {
    		IMessageStructure messageStructure = createMessageStructure(
    				message,
    				message.getId(),
    				message.getName(),
    				namespace,
    				message.getDescription(),
    				createFieldStructures(builder, message, namespace),
    				getAttributes(message, false, null)
    		);
    
    		builder.addMessageStructure(messageStructure);
        } catch (RuntimeException e) {
            throw new EPSCommonException("Message '" + message.getName() + "', problem with content", e);
        }
	}

	protected IFieldStructure createFieldStructure(Field field, boolean isTemplate, String id, String name, String namespace,
			String description, JavaType javaType, Boolean isRequired, Boolean isCollection, Boolean isServiceName, String defaultValue) {

		String referenceName = field.getReference() != null ? ((Field)field.getReference()).getName() : null;

		try {
    		Map<String, IAttributeStructure> attributes = getAttributes(field, false, null);
    		Map<String, IAttributeStructure> values = (!isTemplate && referenceName == null) ? null : getAttributes(field, true, javaType);
    		
    		return new FieldStructure(name, namespace, description, referenceName, attributes, values,
    		        javaType, isRequired, isCollection, isServiceName, defaultValue);
		} catch (EPSCommonException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new EPSCommonException("Field '" + name + "', problem with attributes or values", e);
		}
	}

	protected IMessageStructure createMessageStructure(Message message, String id, String name, String namespace, String description,
			List<IFieldStructure> fields, Map<String, IAttributeStructure> attributes) {

		return new MessageStructure(name, namespace, description, fields, attributes);
	}

	protected IMessageStructure createMessageStructure(Message message, Field field, String id, String name, String namespace,
			String description, Boolean isRequired, Boolean isCollection, IMessageStructure reference) {

	    try {
    		Map<String, IAttributeStructure> attributes = getAttributes(field, false, null);
    
    		return new MessageStructure(name, namespace, description, isRequired, isCollection, attributes, reference);
	    } catch (RuntimeException e) {
            throw new EPSCommonException("Message '" + name + "', problem with attributes", e);
        }
	}

	protected IDictionaryStructure createDictionaryStructure(String namespace, String description, Map<String, IAttributeStructure> dictAttributes,
			Map<String, IMessageStructure> msgStructures, Map<String, IFieldStructure> fieldStructures) {


		return new DictionaryStructure(namespace, description, dictAttributes, msgStructures, fieldStructures);
	}

	protected IAttributeStructure createAttributeStructure(String name, String value, Object castValue, JavaType type) {
		return new AttributeStructure(name, value, castValue, type);
	}

	private String getDefaultValue(Field field, boolean search) {
	    if (!search) {
	        return field.getDefaultvalue();
	    }	        
	        
		if (field.getDefaultvalue() != null) {
			return field.getDefaultvalue();
		}

		if (field.getReference() != null) {
			return getDefaultValue((Field)field.getReference(), search);
		}

		return null;
	}

	private List<IFieldStructure> createFieldStructures(StructureBuilder builder, Message message, String namespace) {

		List<IFieldStructure> result = new ArrayList<>();

		for (Field field : message.getFields()) {

			IFieldStructure fieldStructure;

			if (field.getReference() != null && field.getReference() instanceof Message) {

				IMessageStructure struct = builder.getMessageStructure(((Message)field.getReference()).getName());

				if (struct == null) {
					convertMessage(namespace, builder, (Message)field.getReference());
					struct = builder.getMessageStructure(((Message)field.getReference()).getName());
				}

				fieldStructure = createMessageStructure((Message)field.getReference(), field, field.getId(),
						field.getName(), namespace, field.getDescription(), field.isRequired(), field.isIsCollection(), struct);

			} else {

				if (getFieldType(field, true) == null) {
					throw new EPSCommonException(
							String.format("Field [%s] in message [%s] has neither a type nor a reference",
									field.getName(), message.getName()));
				}

				JavaType javaType = getFieldType(field, this.aggregateAttributes);
				String defVal = getDefaultValue(field, this.aggregateAttributes);

				fieldStructure = createFieldStructure(
						field, false,
						field.getId(),
						field.getName(),
						namespace,
						field.getDescription(),
						javaType,
						field.isRequired(),
						field.isIsCollection(),
						field.isIsServiceName(),
						defVal
				);
			}

			result.add(fieldStructure);
		}

		return result;
	}
	
	private JavaType getFieldType(Field field, boolean search) {
	    if (!search) {
	        return field.getType();
	    }
	    
		if (field.getReference() != null) {
			return getFieldType((Field) field.getReference(), search);
		} else {
			return field.getType();
		}		
	}

	private boolean isComplex(Field field) {
		Object reference = field.getReference();

		boolean value = (field instanceof Message);

		if (!value) {
			if (reference != null) {
				value = isComplex((Field) field.getReference());
			}
		}

		return value;
	}

	private Map<String, IAttributeStructure> getAttributes(Field field, boolean isValues, JavaType javaType) {
	    List<Attribute> attributes = new ArrayList<>();

	    if (isValues) {
	    	collectFieldValues(field, attributes);
	    } else {
	    	collectFieldAttributes(field, attributes);
	    }

        Map<String, IAttributeStructure> result = collectAttributeStructures(attributes, javaType, isValues);

	    return result.isEmpty() ? null : result;
	}

	private Map<String, IAttributeStructure> getDictionaryAttributes(Dictionary dictionary) {

        List<Attribute> filteredAttr = new ArrayList<>();
        for (Attribute attr : dictionary.getAttributes()) {
            replaceOrAdd(filteredAttr, attr);
        }
        Map<String, IAttributeStructure> result = collectAttributeStructures(filteredAttr, null, false);
        return result.isEmpty() ? null : result;
    }
	
    protected Map<String, IAttributeStructure> collectAttributeStructures(List<Attribute> attributes, JavaType javaType, boolean isValues) {
        Map<String, IAttributeStructure> result = initMap();
	    for (Attribute attribute : attributes) {
            Object value = getAttributeValue(attribute, javaType);

            result.put(attribute.getName(),
                    createAttributeStructure(attribute.getName(), attribute.getValue(), value, isValues ? javaType : attribute.getType()));
        }
        return result;
    }

	protected void collectFieldAttributes(Field field, Collection<Attribute> attributes) {
		collectFieldAttributesOrValues(field, attributes, field.getAttributes(), false);
    }

	protected void collectFieldValues(Field field, Collection<Attribute> values) {
		collectFieldAttributesOrValues(field, values, field.getValues(), true);
	}

	private void collectFieldAttributesOrValues(Field field, Collection<Attribute> attrOrVal, List<Attribute> attrOrValList, boolean isValue) {

		if (this.aggregateAttributes) {
			Object reference = field.getReference();
	        if (reference != null) {
	        	collectFieldAttributesOrValues((Field) reference, attrOrVal, isValue ? ((Field)reference).getValues() : ((Field)reference).getAttributes(), isValue);
	        }
		}

        for (Attribute attribute : attrOrValList) {
            
            boolean corruptValues = replaceOrAdd(attrOrVal, attribute);
            
            if (isValue && corruptValues) {
                throw new EPSCommonException(String.format("Duplicated values at %s, attribute name is %s", field.getName(), attribute.getName()));
            }
        }
    }

	protected static boolean replaceOrAdd(Collection<Attribute> attributes, Attribute attribute) {
	    boolean removed = false;
	    Iterator<Attribute> iterator = attributes.iterator();
	    while (iterator.hasNext()) {
	        Attribute element = iterator.next();
	        if (attribute.getName().equals(element.getName())) {
	            iterator.remove();
	            removed = true;
	            break;
            }
	    }

	    attributes.add(attribute);
	    return removed;
    }

	protected static Object getAttributeValue(Attribute attribute, JavaType javaType) throws EPSCommonException, NullPointerException {

		javaType = defaultIfNull(defaultIfNull(javaType, attribute.getType()), JavaType.JAVA_LANG_STRING);

		if (javaType == JavaType.JAVA_LANG_STRING) {
			return attribute.getValue();
		} else if (StringUtils.isEmpty(attribute.getValue())) {
			return null;
		}

		return StructureUtils.castValueToJavaType(attribute.getValue(), javaType);
	}
}
