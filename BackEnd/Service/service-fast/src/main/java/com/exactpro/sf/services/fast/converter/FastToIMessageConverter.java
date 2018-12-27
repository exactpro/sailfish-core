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

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.ArrayList;

import org.openfast.FieldValue;
import org.openfast.GroupValue;
import org.openfast.Message;
import org.openfast.SequenceValue;
import org.openfast.template.Field;
import org.openfast.template.Group;
import org.openfast.template.MessageTemplate;
import org.openfast.template.Scalar;
import org.openfast.template.Sequence;
import org.openfast.template.type.StringType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;

public class FastToIMessageConverter {
	private final static Logger logger = LoggerFactory.getLogger(FastToIMessageConverter.class);
	private final IMessageFactory messageFactory;
	private final String namespace;

	public FastToIMessageConverter(
			IMessageFactory messageFactory,
			String namespace) {
		this.namespace = namespace;
		this.messageFactory = messageFactory;
	}

	public IMessage convert(Message fastMsg) throws ConverterException {

		MessageTemplate template = fastMsg.getTemplate();
		String msgName = getIMessageName(template.getName());
		IMessage message;
		try {
			message = messageFactory.createMessage(msgName, namespace);
		} catch (Exception e) {
			logger.warn("Can not create fast IMessage for template id {}", msgName);
			throw new ConverterException(
					"Can not create fast IMessage for template id " + msgName, e);
		}

		convertFields(fastMsg, message, msgName);

		return message;
	}

	private void convertFields(GroupValue fastMsg, IMessage message, String msgName)
	throws ConverterException {
		Group template = fastMsg.getGroup();
		for (Field fastFld : template.getFields()) {
			convertField(fastFld, fastMsg, message, msgName);
		}
	}

	private void convertField(
			Field fastFld,
			GroupValue fastMsg,
			IMessage message, String msgName)
	throws ConverterException {
		String iMessageFieldName = getIMessageName(fastFld.getName());

		//FIXME: Have not found any better way to get value of the QNamed field
		Group grp = fastMsg.getGroup();
		int index = grp.getFieldIndex(fastFld);

		if (!fastMsg.isDefined(index)) {
			return;
		}

		FieldValue fieldValue = fastMsg.getValue(index);
		if (fieldValue == null) {
//			message.addField(iMessageFieldName, null);
			return;
		}

		if (fastFld.getTypeName().equals("scalar")) {
			setScalarValue((Scalar) fastFld, fastMsg, message, iMessageFieldName );
		} else if (fastFld.getTypeName().equals("sequence")) {
			SequenceValue sqsValue = fastMsg.getSequence(index);
			convertSequence(sqsValue, iMessageFieldName, message, msgName);
		} else if (fastFld.getTypeName().equals("group")) {
			GroupValue grpValue = fastMsg.getGroup(index);
			convertGroup(grpValue, iMessageFieldName, message, msgName);
		} else if (fastFld.getTypeName().equals("decimal")) {
			BigDecimal bdVal = fastMsg.getBigDecimal(index);
			message.addField(iMessageFieldName, bdVal);
		} else {
			logger.error("Can not convert field with type {}", fastFld.getTypeName());
			throw new ConverterException(
					"Can not convert field with type " + fastFld.getTypeName());
		}
	}

	private void convertGroup(GroupValue grpValue, String iMessageFieldName,
			IMessage message, String msgName) throws ConverterException {
		IMessage innerMessage;
		String msgType = msgName + "_" + iMessageFieldName;
		try {
			innerMessage = messageFactory.createMessage(msgType, namespace);
		} catch (Exception e) {
			logger.error("Can not create message for id:{}", msgType, e);
			throw new ConverterException("Can not create message for id:" + msgType, e);
		}

		convertFields(grpValue, innerMessage, msgType);

		message.addField(iMessageFieldName, innerMessage);
	}

	private void convertSequence(SequenceValue sqsValue,
			String iMessageFieldName, IMessage message, String msgName) throws ConverterException {

		Sequence sqs = sqsValue.getSequence();

		String lengthName = sqs.getLength().getName();
		if (lengthName == null || lengthName.equals("")) {
			lengthName = "length";
		}
		lengthName = getIMessageName(lengthName);
		lengthName = getIMessageName(sqsValue.getSequence().getName()) + "_" + lengthName;
		message.addField(lengthName, sqsValue.getLength());
		String msgType = msgName + "_" + iMessageFieldName;

		java.util.ArrayList<IMessage> collectionMessages = new ArrayList<IMessage>();
		for(GroupValue groupVal:sqsValue.getValues()) {
			IMessage innerMessage;
			try {
				innerMessage = messageFactory.createMessage(msgType, namespace);
			} catch (Exception e) {
				logger.error("Can not create message for id:{}", msgType, e);
				throw new ConverterException("Can not create message for id:" + msgType, e);
			}
			convertFields(groupVal, innerMessage, msgType);
			collectionMessages.add(innerMessage);
		}
		message.addField(iMessageFieldName, collectionMessages);
	}

	private void setScalarValue(Scalar fastFld, GroupValue fastMsg,
			IMessage message, String iMessageFieldName) throws ConverterException {

		//FIXME: Have not found any better way getting value of QNamed field
		Group grp = fastMsg.getGroup();
		int index = grp.getFieldIndex(fastFld);

		if (fastFld.getType().getName().equals("int8")) {
			Byte byteVal = fastMsg.getByte(index);
			if (byteVal != null) {
				message.addField(iMessageFieldName, byteVal);
			}
		} else if (fastFld.getType().getName().equals("int32")) {
			Integer intVal = fastMsg.getInt(index);
			if (intVal != null) {
				message.addField(iMessageFieldName, intVal);
			}
		} else if (fastFld.getType().getName().equals("uInt32")) {
			Long longVal = fastMsg.getLong(index);
			if (longVal != null) {
				message.addField(iMessageFieldName, longVal);
			}
		} else if (fastFld.getType().getName().equals("int64")) {
			Long longVal = fastMsg.getLong(index);
			if (longVal != null) {
				message.addField(iMessageFieldName, longVal);
			}
		} else if (fastFld.getType().getName().equals("uInt64")) {
			BigDecimal bdVal = fastMsg.getBigDecimal(index);
			if (bdVal != null) {
				message.addField(iMessageFieldName, bdVal);
			}
		} else if (fastFld.getType() instanceof StringType ) {
			String sVal = fastMsg.getString(index);
			if (sVal != null) {
				message.addField(iMessageFieldName, sVal);
			}
		} else if (fastFld.getType().getName().equals("decimal")) {

			BigDecimal bdVal = fastMsg.getBigDecimal(index);
			if (bdVal != null) {
				message.addField(iMessageFieldName, bdVal);
			}
		} else if (fastFld.getType().getName().equals("byteVector")) {
			byte[] bvVal = fastMsg.getBytes(index);
			if (bvVal != null) {
			//FIXME: in most cases a separate field contains encoding of the message

//			message.addField(iMessageFieldName, bvVal);
				try {
					message.addField(iMessageFieldName, new String(bvVal, "ascii"));
				} catch (UnsupportedEncodingException e) {
					logger.debug("can not convert bytearray to string with ascii encoding", e);
				}
			}
		} else {
			throw new ConverterException("Can not convert field of type: " +
					fastFld.getType().getName());
		}
	}

	private String getIMessageName(String name) {
		return name.replaceAll("[_ -]", "");
	}
}
