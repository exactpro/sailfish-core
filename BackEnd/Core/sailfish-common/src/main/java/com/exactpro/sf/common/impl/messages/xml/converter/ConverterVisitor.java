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
package com.exactpro.sf.common.impl.messages.xml.converter;

import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.exactpro.sf.common.impl.messages.xml.configuration.Attribute;
import com.exactpro.sf.common.impl.messages.xml.configuration.Dictionary;
import com.exactpro.sf.common.impl.messages.xml.configuration.Field;
import com.exactpro.sf.common.impl.messages.xml.configuration.JavaType;
import com.exactpro.sf.common.impl.messages.xml.configuration.Message;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageStructureVisitor;
import com.exactpro.sf.common.messages.MessageStructureWriter;
import com.exactpro.sf.common.messages.structures.IFieldStructure;

public class ConverterVisitor implements IMessageStructureVisitor {

	private static Logger logger = LoggerFactory.getLogger(ConverterVisitor.class);

	private static int COUNTER = 0;

	synchronized public static int getId() {
		return COUNTER++;
	}

	private Message xmlMessage;
	private Dictionary dictionary;

	public ConverterVisitor(Message xmlMessage, Dictionary dictionary) {
		this.xmlMessage = xmlMessage;
		this.dictionary = dictionary;
	}

	public void visit(String fieldName, IFieldStructure fldStruct, Class<?> clazz) {

		logger.debug("field = {}", fieldName);

		Field newField = new Field();
		// we know that only one inheritance level is available for simple
		// fields
		newField.setType(JavaType.fromValue(clazz.getCanonicalName()));

		newField.setName(fieldName);

		if (fldStruct.getDefaultValue() != null) {
			newField.setDefaultvalue(fldStruct.getDefaultValue().toString());
		}

		newField.setIsCollection(fldStruct.isCollection());

		for (String name : fldStruct.getAttributes().keySet()) {
			Attribute attribute = new Attribute();
			attribute.setName(name);
			Object val = fldStruct.getAttributeValueByName(name);
			attribute.setValue(val == null ? null : val.toString());
			attribute.setType(val == null ? null : JavaType.fromValue(val.getClass().getName()));
			newField.getAttributes().add(attribute);
		}

		if (fldStruct.isEnum()) {
			Field enumXmlField;

			enumXmlField = XmlDictionaryUtils.getDictionaryField(dictionary,
					fldStruct.getName());

			if (enumXmlField == null) {
				enumXmlField = new Field();
				enumXmlField.setName(fldStruct.getName());
				enumXmlField.setDescription(fldStruct.getDescription());
				enumXmlField.setId(fldStruct.getName());
				enumXmlField.setIsCollection(false);
				enumXmlField.setType(fldStruct.getJavaType());

				logger.debug("a enum field is named {}", fldStruct.getName());
				
				for (String value : fldStruct.getValues().keySet()) {
					
					Object valObj = fldStruct.getValues().get(value).getCastValue();
					
					Attribute valueAttribute = new Attribute();
					valueAttribute.setName(value);
					valueAttribute.setValue(valObj.toString());

                    logger.debug("a type of an enumeration attribute is {}, value is {}",
                                valObj.getClass(), valObj);
					
					valueAttribute.setType(JavaType.fromValue(valObj.getClass().getName()));
					
					enumXmlField.getValues().add(valueAttribute);
				}
				
				dictionary.getFields().getFields().add(enumXmlField);
			}
			newField.setReference(enumXmlField);
		}

		xmlMessage.getFields().add(newField);

	}

	@Override
	public void visit(String fieldName, Boolean value, IFieldStructure fldStruct, boolean isDefault) {
		visit(fieldName, fldStruct, Boolean.class);
	}

	@Override
	public void visitBooleanCollection(String fieldName, List<Boolean> value,
			IFieldStructure fldStruct, boolean isDefault) {
		visit(fieldName, fldStruct, Boolean[].class);
	}

	@Override
	public void visit(String fieldName, Byte value, IFieldStructure fldStruct, boolean isDefault) {
		visit(fieldName, fldStruct, Byte.class);
	}

	@Override
	public void visitByteCollection(String fieldName, List<Byte> value, IFieldStructure fldStruct, boolean isDefault) {
		visit(fieldName, fldStruct, Byte[].class);
	}

	@Override
	public void visit(String fieldName, Character value,
			IFieldStructure fldStruct, boolean isDefault) {
		visit(fieldName, fldStruct, Character.class);
	}

	@Override
	public void visitCharCollection(String fieldName, List<Character> value, IFieldStructure fldStruct, boolean isDefault) {
		visit(fieldName, fldStruct, Character[].class);
	}

	@Override
	public void visit(String fieldName, LocalDateTime value, IFieldStructure fldStruct, boolean isDefault) {
		visit(fieldName, fldStruct, LocalDateTime.class);
	}

	@Override
	public void visitDateTimeCollection(String fieldName, List<LocalDateTime> value, IFieldStructure fldStruct, boolean isDefault) {
		visit(fieldName, fldStruct, LocalDateTime[].class);
	}

    @Override
    public void visit(String fieldName, LocalDate value, IFieldStructure fldStruct, boolean isDefault) {
        visit(fieldName, fldStruct, LocalDate.class);
    }

    @Override
    public void visitDateCollection(String fieldName, List<LocalDate> value, IFieldStructure fldStruct, boolean isDefault) {
        visit(fieldName, fldStruct, LocalDate[].class);
    }

    @Override
    public void visit(String fieldName, LocalTime value, IFieldStructure fldStruct, boolean isDefault) {
        visit(fieldName, fldStruct, LocalTime.class);
    }

    @Override
    public void visitTimeCollection(String fieldName, List<LocalTime> value, IFieldStructure fldStruct, boolean isDefault) {
        visit(fieldName, fldStruct, LocalTime[].class);
    }

	@Override
	public void visit(String fieldName, Double value, IFieldStructure fldStruct, boolean isDefault) {
		visit(fieldName, fldStruct, Double.class);
	}

	@Override
	public void visitDoubleCollection(String fieldName, List<Double> value,
			IFieldStructure fldStruct, boolean isDefault) {
		visit(fieldName, fldStruct, Double[].class);
	}

	@Override
	public void visit(String fieldName, Float value, IFieldStructure fldStruct, boolean isDefault) {
		visit(fieldName, fldStruct, Float.class);
	}

	@Override
	public void visitFloatCollection(String fieldName, List<Float> value, IFieldStructure fldStruct, boolean isDefault) {
		visit(fieldName, fldStruct, Float[].class);
	}

	@Override
	public void visit(String fieldName, Integer value, IFieldStructure fldStruct, boolean isDefault) {
		visit(fieldName, fldStruct, Integer.class);
	}

	@Override
	public void visitIntCollection(String fieldName, List<Integer> value, IFieldStructure fldStruct, boolean isDefault) {
		visit(fieldName, fldStruct, int[].class);
	}

	@Override
	public void visit(String fieldName, Long value, IFieldStructure fldStruct, boolean isDefault) {
		visit(fieldName, fldStruct, Long.class);
	}

	@Override
	public void visitLongCollection(String fieldName, List<Long> value, IFieldStructure fldStruct, boolean isDefault) {
		visit(fieldName, fldStruct, long[].class);
	}

	@Override
	public void visit(String fieldName, BigDecimal value,
			IFieldStructure fldStruct, boolean isDefault) {
		visit(fieldName, fldStruct, BigDecimal.class);
	}

	@Override
	public void visitBigDecimalCollection(String fieldName, List<BigDecimal> value,
			IFieldStructure fldStruct, boolean isDefault) {
		visit(fieldName, fldStruct, BigDecimal[].class);
	}

	@Override
	public void visit(String fieldName, Short value, IFieldStructure fldStruct, boolean isDefault) {
		visit(fieldName, fldStruct, Short.class);
	}

	@Override
	public void visitShortCollection(String fieldName, List<Short> value, IFieldStructure fldStruct, boolean isDefault) {
		visit(fieldName, fldStruct, Short[].class);
	}

	@Override
	public void visit(String fieldName, String value, IFieldStructure fldStruct, boolean isDefault) {
		visit(fieldName, fldStruct, String.class);
	}

	@Override
	public void visitStringCollection(String fieldName, List<String> value,
			IFieldStructure fldStruct, boolean isDefault) {
		visit(fieldName, fldStruct, String[].class);
	}
	
	private void visitMessage(String fieldName, IFieldStructure complexField, boolean isCollection) {

		logger.debug("message = {}", fieldName);

		Message refferedMessage = null;

		for (Message message : dictionary.getMessages().getMessages()) {
			if (complexField.getReferenceName().equals(message.getName())) {
				logger.debug("{} message was found", fieldName);
				refferedMessage = message;
				break;
			}
		}
		Message messageField = new Message();

		if (refferedMessage == null) {
			refferedMessage = new Message();
			MessageStructureWriter writer = new MessageStructureWriter();
			refferedMessage.setName(complexField.getReferenceName());
			refferedMessage.setId(complexField.getReferenceName());
			ConverterVisitor visitor = new ConverterVisitor(refferedMessage,
					dictionary);
			dictionary.getMessages().getMessages().add(refferedMessage);
			writer.traverse(visitor, complexField.getFields());
		}

		messageField.setReference(refferedMessage);
		messageField.setName(fieldName);
		messageField.setIsCollection(isCollection);


        logger.debug("the filed structure names {}", complexField.getReferenceName());

		for (String name : complexField.getAttributes().keySet()) {
			Attribute attribute = new Attribute();
			attribute.setName(name);
			Object val = complexField.getAttributeValueByName(name);
			attribute.setValue(val == null ? null : val.toString());

            if(val != null) {
                logger.debug("an attribute type of the attribute {} = {}", name, val.getClass().getName());
            }

			attribute.setType(val == null ? null : JavaType.fromValue(val.getClass().getName()));
			messageField.getAttributes().add(attribute);
		}

		xmlMessage.getFields().add(messageField);
	}
	
	@Override
	public void visit(String fieldName, IMessage message, IFieldStructure complexField, boolean isDefault) {
		visitMessage(fieldName, complexField, false);
	}

	@Override
	public void visitMessageCollection(String fieldName, List<IMessage> message, IFieldStructure complexField, boolean isDefault) {
		visitMessage(fieldName, complexField, true);
	}
}
