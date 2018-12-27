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
package com.exactpro.sf.services.ntg;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.exactpro.sf.common.codecs.AbstractCodec;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.MessageStructureWriter;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.util.GenericConverter;
import com.exactpro.sf.services.IServiceContext;
import com.exactpro.sf.services.MessageHelper;
import com.exactpro.sf.services.ntg.exceptions.UndefinedMessageException;

public class NTGMessageHelper extends MessageHelper {

    public static String MESSAGE_HEADER = "MessageHeader";
    public static String FIELD_DIRTY_MSG_TYPE = "NTGMsgType";
    public static String FIELD_DIRTY_START = "DirtyStart";
    public static String FIELD_DIRTY_MSG_LEN = "DirtyMsgLen";

	public static final String FIELD_MESSAGE_LENGTH = "MessageLength";
	public static final String FIELD_START_OF_MESSAGE = "StartOfMessage";
	public static final String MESSAGE_TYPE = "MessageType";

	public static final String MESSAGE_LOGON = "Logon";
	public static final String MESSAGE_HEARTBEAT = "Heartbeat";
	public static final String MESSAGE_REJECT = "Reject";
	public static final String ATTRIBUTE_MESSAGE_TYPE = "MessageType";
    
	private AbstractCodec codec;
    private Map<String, NTGMessageMetadata> metadata;

    private final class NTGMessageMetadata {
		public final Object messageType;
		public final Integer messageSize;

        public NTGMessageMetadata(Object messageType, Integer messageSize) {
			this.messageType = messageType;
			this.messageSize = messageSize;
		}

	}

    private class NTGSizeCalculator extends SimpleMessageStructureVisitor {

		private int messageSize = 0;

		@Override
		public void visit(String fieldName, Object value, IFieldStructure fldStruct, boolean isDefault) {
            int length = (Integer) fldStruct.getAttributeValueByName(NTGProtocolAttribute.Length.toString());
            int offset = (Integer) fldStruct.getAttributeValueByName(NTGProtocolAttribute.Offset.toString());

			if (messageSize != offset)
				throw new IllegalStateException();

			messageSize = offset + length;
		}

		@Override
		public void visitCollection(String fieldName, List<?> value, IFieldStructure fldStruct, boolean isDefault) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void visitMessage(String fieldName, IMessage message, IFieldStructure complexField,
				IFieldStructure fldStruct, boolean isDefault) {

            NTGSizeCalculator visitor = new NTGSizeCalculator();
			MessageStructureWriter messageStructureWriter = new MessageStructureWriter();
			messageStructureWriter.traverse(visitor, complexField.getFields());

            Integer length = (Integer) fldStruct.getAttributeValueByName(NTGProtocolAttribute.Length.toString());
			if (length != null && visitor.getMessageSize() != length) {
				throw new IllegalStateException();
			}
			
			messageSize += visitor.getMessageSize();
		}

		@Override
		public void visitMessageCollection(String fieldName, List<IMessage> message, IFieldStructure complexField, boolean isDefault) {
			throw new UnsupportedOperationException();
		}

		public int getMessageSize() {
			return messageSize;
		}

	}

    @Override
    public void init(IMessageFactory messageFactory, IDictionaryStructure messageDictionary) {
		super.init(messageFactory, messageDictionary);

        metadata = new HashMap<String, NTGMessageMetadata>();

		List<IMessageStructure> messages = messageDictionary.getMessageStructures();
		for (IMessageStructure structure : messages) {
            // part of validation is done in NTGVisitorBase
            // part of message header creation is done in NTGVisitorBase
			
		    Byte messageTypeCode = (Byte)structure.getAttributeValueByName(ATTRIBUTE_MESSAGE_TYPE); //Fields block can not contain MessageType 
			if (messageTypeCode != null) { // skip headers...
				// calculate message sizes
				Integer messageSize = null;
				MessageStructureWriter writer = new MessageStructureWriter();
                NTGSizeCalculator visitor = new NTGSizeCalculator();
				writer.traverse(visitor, structure);
				messageSize = visitor.getMessageSize() - 3; // Don't include 'StartOfMessage' and 'MessageLength'

				String messageType = GenericConverter.convertByteArrayToString( 1, new byte[] { messageTypeCode } );
				metadata.put(
						structure.getName(),
                        new NTGMessageMetadata(messageType, messageSize));
			}
		}
	}

	@Override
	public synchronized AbstractCodec getCodec(IServiceContext serviceContext) {
		if (codec == null) {
            codec = new NTGCodec();
			codec.init(serviceContext, null, getMessageFactory(), getDictionaryStructure());
		}
		return codec;
	}

	@Override
	public IMessage prepareMessageToEncode(IMessage message, Map<String, String> params) {
		// AML3 - header can be filled like repeating group.
		IMessage header = (IMessage) message.getField("MessageHeader");
        NTGMessageMetadata m = getMetadata(message.getName());

		if (header != null) {
			if (!header.isFieldSet("MessageLength")) {
				// special case: handle AML2 actions that doesn't sets
				// MessageLength field
				header.addField("MessageLength", m.messageSize);
			}
			return message;
		}

		// fill header
		header = getMessageFactory().createMessage("MessageHeader", getNamespace());
		header.addField("StartOfMessage", 2); // FIXME: better ?? default value?
		header.addField("MessageLength", m.messageSize);
		header.addField("MessageType", m.messageType);

		if (params != null) {
            // AML2 - header filled by actions. Dirty AML2 uses "#nativemsgtype", "#dirtystart", "#dirtymsglen" to override header's fields
		    String value = params.get(FIELD_DIRTY_MSG_LEN);
		    if (value != null) {
                header.addField("MessageLength", Integer.valueOf(value));
		    }
		    
		    value = params.get(FIELD_DIRTY_MSG_TYPE);
            if (value != null) {
                header.addField("MessageType", value);
            }
            
            value = params.get(FIELD_DIRTY_START);
            if (value != null) {
                header.addField("StartOfMessage", Integer.valueOf(value));
            }
		}
		
		// add header to message
		message.addField("MessageHeader", header);

		return message;
	}

    private NTGMessageMetadata getMetadata(String messageName) {
        NTGMessageMetadata m = metadata.get(messageName);
		if (m == null) {
			throw new UndefinedMessageException(String.format("namespace=[%s] name=[%s]", getNamespace(), messageName));
		}
		return m;
	}
}
