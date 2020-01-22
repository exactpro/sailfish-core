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
package com.exactpro.sf.services.fast.converter;

import static com.exactpro.sf.common.messages.structures.StructureUtils.getAttributeValue;
import static com.exactpro.sf.services.fast.converter.FastToIMessageConverter.getFieldTimeUnit;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.exactpro.sf.util.DateTimeUtility;
import com.google.common.base.Enums;
import org.jetbrains.annotations.NotNull;
import org.openfast.GroupValue;
import org.openfast.Message;
import org.openfast.SequenceValue;
import org.openfast.template.Group;
import org.openfast.template.MessageTemplate;
import org.openfast.template.Sequence;
import org.openfast.template.TemplateRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.util.EPSCommonException;

public class IMessageToFastConverter {
    private static final Logger logger = LoggerFactory.getLogger(IMessageToFastConverter.class);

	private final IDictionaryStructure dictionary;
	private final TemplateRegistry templateRegistry;

	private final HashMap<IMessageStructure, Map<String, IFieldStructure> > msgFielsMap = new HashMap<IMessageStructure, Map<String, IFieldStructure>>();

	public IMessageToFastConverter(IDictionaryStructure dictionary, TemplateRegistry registry) {
		this.dictionary = dictionary;
		this.templateRegistry = registry;

		hashMessageFields();
	}

	private void hashMessageFields() {
        for(IMessageStructure msgStructure : dictionary.getMessages().values()) {
            msgFielsMap.put(msgStructure, msgStructure.getFields());
		}
	}

	public Message convert(IMessage iMsg) throws ConverterException {

        IMessageStructure structure = dictionary.getMessages().get(iMsg.getName());

		if (structure == null) {
			throw new EPSCommonException("Can not find message " + iMsg.getName() +
					" in the namespace " + iMsg.getNamespace());
		}

        String templateId = getAttributeValue(structure, "templateId");

		MessageTemplate template = templateRegistry.get(Integer.valueOf(templateId));
		Message fastMsg = new Message(template);

		convertFields(fastMsg, iMsg);

		return fastMsg;
	}

	private void convertFields(GroupValue fastMsg, 	IMessage message)
	throws ConverterException {
        IMessageStructure structure = dictionary.getMessages().get(message.getName());
		if (structure == null) {
			logger.error("Can get description from dictionary for {}:{}", message.getName(), message.getNamespace());
			throw new ConverterException("Can get description from dictionary for " +
					message.getName() + ":" + message.getNamespace());
		}
		Group template = fastMsg.getGroup();
		for (String fieldName : message.getFieldNames()) {
			convertField(template, fastMsg, message, fieldName, structure);
		}
	}

	private void convertField(Group template, GroupValue fastMsg, IMessage message,
			String fieldName, IMessageStructure structure) throws ConverterException {
        IFieldStructure messageFieldDescr = msgFielsMap.get(structure).get(fieldName);
        String fastFieldName = getAttributeValue(messageFieldDescr, "fastName");

		if (fastFieldName == null) {
            Boolean isLength = getAttributeValue(messageFieldDescr, "isLength");
			if (isLength != null && isLength) {
				return;
			}
			throw new ConverterException("Failed to get fast field " +
					"name for field :" + fieldName + " message=" + message);
		}
		Object value = message.getField(fieldName);
		setFastField(template, fastMsg, fastFieldName, value, message, fieldName);
	}

	private void setFastField(Group template, GroupValue fastMsg, String fastFieldName,
			Object value, IMessage message, String fieldName)
	throws ConverterException {
		
		if (value instanceof Byte) {
            byte[] byteValue = { (Byte)value };
			fastMsg.setByteVector(fastFieldName, byteValue);
		} else if (value instanceof Integer) {
			Integer intValue = (Integer) value;
			fastMsg.setInteger(fastFieldName, intValue);
		} else if (value instanceof Long) {
			Long longValue = (Long) value;
			fastMsg.setLong(fastFieldName, longValue);
		} else if (value instanceof BigDecimal) {
			BigDecimal bdValue = (BigDecimal) value;
			fastMsg.setDecimal(fastFieldName, bdValue);
		} else if (value instanceof String) {
			String strValue = (String) value;
			fastMsg.setString(fastFieldName, strValue);
		} else if (value instanceof LocalDateTime) {
			LocalDateTime localDateTimeValue = (LocalDateTime) value;
			String unitAttrValue = getFieldTimeUnit(dictionary, message.getName(),fieldName);
			//FAST 1.2. A unit may not exist, then the millisecond is used as the default unit.
			FastToIMessageConverter.TIMESTAMP_UNIT timeUnit = unitAttrValue != null ? Enums.getIfPresent(FastToIMessageConverter.TIMESTAMP_UNIT.class, unitAttrValue)
					.or(FastToIMessageConverter.TIMESTAMP_UNIT.millisecond) : FastToIMessageConverter.TIMESTAMP_UNIT.millisecond;
			if (timeUnit == null) {
				throw new EPSCommonException("Incorrect time unit = " + unitAttrValue + " for " + fieldName + " field");
			}
			BigDecimal bdValue = BigDecimal.valueOf(dateTimeToTimestamp(localDateTimeValue, timeUnit));
			fastMsg.setDecimal(fastFieldName, bdValue);
		} else if (value instanceof Boolean) {
			Boolean boolValue = (Boolean) value;
			long longValue = boolValue ? 1 : 0;
			fastMsg.setLong(fastFieldName, longValue);
		} else if (value instanceof Collection<?>) {
			Collection<?> colValue = (Collection<?>) value;
			Sequence sequence = template.getSequence(fastFieldName);
			SequenceValue sqsValue = new SequenceValue(sequence);
			convertCollection(sqsValue, colValue);
			fastMsg.setFieldValue(sequence, sqsValue);
		} else if (value instanceof IMessage) {
			IMessage msgValue = (IMessage) value;
			Group group = template;
			Group innerGrp = group.getGroup(fastFieldName);
			GroupValue groupVal = new GroupValue(innerGrp);
			convertFields(groupVal, msgValue);
			fastMsg.setFieldValue(innerGrp, groupVal);
		} else if (value instanceof byte[]) {
			byte[] byteVal = (byte[]) value;
			fastMsg.setByteVector(fastFieldName, byteVal);
		} else {
			throw new ConverterException("Can not convert value of unknown type");
		}
	}

	private void convertCollection(SequenceValue sqs, Collection<?> colValue) throws ConverterException {
		Sequence sequence = sqs.getSequence();
		Group grp = sequence.getGroup();
		for (Object o: colValue) {
			IMessage innerMsg = (IMessage) o;
			GroupValue grpValue = new GroupValue(grp);
			convertFields(grpValue, innerMsg);
			try {
				sqs.add(grpValue);
			} catch (Exception e) {
				logger.warn("Failed to add group value to collection", e);
			}
		}

	}

	private long dateTimeToTimestamp(LocalDateTime dateTime, @NotNull FastToIMessageConverter.TIMESTAMP_UNIT unit) {
		switch (unit) {
			case day:
				return TimeUnit.MILLISECONDS.toDays(DateTimeUtility.getMillisecond(dateTime));
			case second:
				return TimeUnit.MILLISECONDS.toSeconds(DateTimeUtility.getMillisecond(dateTime));
			case millisecond:
				return DateTimeUtility.getMillisecond(dateTime);
			case microsecond:
				return TimeUnit.SECONDS.toMicros(DateTimeUtility.getSecond(dateTime)) + TimeUnit.NANOSECONDS.toMicros(dateTime.getNano());
			case nanosecond:
				return TimeUnit.SECONDS.toNanos(DateTimeUtility.getSecond(dateTime)) + dateTime.getNano();

		}
		return DateTimeUtility.getMillisecond(dateTime);
	}
}
