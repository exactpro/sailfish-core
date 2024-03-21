/******************************************************************************
 * Copyright 2009-2024 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.services.itch;

import static com.exactpro.sf.common.messages.structures.StructureUtils.getAttributeValue;

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.codecs.AbstractCodec;
import com.exactpro.sf.common.impl.messages.DefaultMessageFactory;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.IMessageStructureVisitor;
import com.exactpro.sf.common.messages.MessageStructureReader;
import com.exactpro.sf.common.messages.MessageStructureReaderHandlerImpl;
import com.exactpro.sf.common.messages.MessageStructureWriter;
import com.exactpro.sf.common.messages.MetadataExtensions;
import com.exactpro.sf.common.messages.MsgMetaData;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.messages.structures.StructureUtils;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.common.util.EvolutionBatch;
import com.exactpro.sf.common.util.HexDumper;
import com.exactpro.sf.common.util.ICommonSettings;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.connectivity.mina.net.IoBufferWithAddress;
import com.exactpro.sf.services.IServiceContext;
import com.exactpro.sf.services.MessageHelper;
import com.exactpro.sf.services.codecs.CodecMessageFilter;
import com.exactpro.sf.services.mina.MINAUtil;

public class ITCHCodec extends AbstractCodec {
    private static final SailfishURI ITCH_PREPROCESSORS_MAPPING_FILE_URI = SailfishURI.unsafeParse("itch_preprocessors");

    private static final Logger logger = LoggerFactory.getLogger(ITCHCodec.class);

	private static final int DEFAULT_BUFFER_SIZE = 64;

	private static final int HEADER_SIZE = 8;

	private final Map<Short, IMessageStructure> msgTypeToMsgStruct = new HashMap<>();
    private final ByteOrder byteOrder = ByteOrder.LITTLE_ENDIAN;

	private IDictionaryStructure msgDictionary;
	private IMessageFactory msgFactory;
	private int msgLengthFieldSize = 1;
    private CodecMessageFilter codecMessageFilter;
    private long lastDecode;
    private IITCHPreprocessor preprocessor;
    private boolean wrapMessages = true;
    private ITCHVisitorSettings itchVisitorSettings = null;

    public ITCHCodec() {
		this.lastDecode = System.currentTimeMillis();
	}

	@Override
	public void init(IServiceContext serviceContext, ICommonSettings settings, IMessageFactory msgFactory, IDictionaryStructure dictionary)
	{
        super.init(serviceContext, settings, msgFactory, dictionary);
		this.msgDictionary = Objects.requireNonNull(dictionary, "'Dictionary' parameter cannot be null");
		this.msgFactory = msgFactory;

		if (settings != null) {
            ITCHCodecSettings msettings = (ITCHCodecSettings) settings;
			this.msgLengthFieldSize = msettings.getMsgLength();
			this.codecMessageFilter = new CodecMessageFilter(msettings.getFilterValues());
            this.preprocessor = msettings.isPreprocessingEnabled() ? DefaultPreprocessor.loadPreprocessor(serviceContext,
                    msettings.getDictionaryURI(), ITCH_PREPROCESSORS_MAPPING_FILE_URI,
                    getClass().getClassLoader()) : null;
            this.wrapMessages = msettings.isWrapMessages();
            this.itchVisitorSettings = ITCHVisitorSettings.from(msettings);
		}
        if(codecMessageFilter != null) {
            codecMessageFilter.init(dictionary);
        }

        msgTypeToMsgStruct.clear();

        for(IMessageStructure msgStruct : dictionary.getMessages().values()) {
            Short msgType = getAttributeValue(msgStruct, ITCHMessageHelper.ATTRIBUTE_MESSAGE_TYPE);
            if(msgType != null) {
                if (!msgTypeToMsgStruct.containsKey(msgType)) {
                    msgTypeToMsgStruct.put(msgType, msgStruct);
                } else {
                    throw new EPSCommonException("MessageType attribute should be unique. MessageName:" + msgStruct.getName());
                }
            }
        }
	}

	private boolean realDecode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) {
		if (logger.isDebugEnabled()) {
			logger.debug("doDecode: [limit:{}; remaining:{}]; Buffer:{}", in.limit(), in.remaining(), HexDumper.getHexdump(in, in.remaining()));
			logger.debug("\n{}", MINAUtil.getHexdumpAdv(in, in.remaining()));
		}

		int startPosition = in.position();
		if (in.remaining() < 2) {
			return false;
		}

		in = in.order(byteOrder);
		int length = in.getUnsignedShort();

		if (in.remaining() < HEADER_SIZE - 2 || in.remaining() < (length - 2)) {
			logger.debug("doDecode FALSE: [limit:{}; remaining:{}", in.limit(), in.remaining());
			return false;
		}

		short messageCount = in.getUnsigned();
		in.position(startPosition);

		List<IMessage> messages = new LinkedList<>();

		logger.debug("doDecode [length = {}; Msg Count = {}]", length, messageCount);

		IMessage unitHeader = null;
		boolean someMsgDropped = false;
		for (short i = 0; i < messageCount + 1; i++) {
            int startCurMsgPosition = in.position();
			short messageType;
			int messageLength;

			if (i == 0) {
                messageType = ITCHMessageHelper.UNITHEADERMSGTYPE;
				messageLength = HEADER_SIZE;
			} else {
				int currentPos = in.position();
                //For length with msgLengthFieldSize == 2 we need 2 bytes, otherwise 1 byte, for message type 1 more byte.
                if (in.remaining() < ((msgLengthFieldSize == 2) ? 3 : 2)) {
                    logger.debug("Not enough data to decode:  remaining:{}", in.remaining());
                    return false;
                }
				messageLength = (msgLengthFieldSize == 2) ? in.getUnsignedShort() : in.getUnsigned();
				messageType = in.getUnsigned();
				in.position(currentPos);
			}

			logger.debug("MsgType = [{}]", messageType);

			IMessageStructure msgStructure = msgTypeToMsgStruct.get(messageType);

			if (msgStructure == null) {
				throw new EPSCommonException("Unknown messageType = [" + messageType + "]. Probably the previous message was not read correctly.");
			}

			IMessage message = msgFactory.createMessage(msgStructure.getName(), msgStructure.getNamespace());
			if (i == 0) {
			    unitHeader = message;
			} else {
			    long seqNum = (Long) unitHeader.getField("SequenceNumber");
                Object mktDataGrp = unitHeader.getField("MarketDataGroup");

                message.addField(ITCHMessageHelper.FAKE_FIELD_UH_SEQUENCE_NUMBER, (int) seqNum);
                message.addField(ITCHMessageHelper.FAKE_FIELD_UH_MARKET_DATA_GROUP, mktDataGrp);
                message.addField(ITCHMessageHelper.FAKE_FIELD_MESSAGE_SEQUENCE_NUMBER, (int) (seqNum + i - 1));
			}

			logger.debug("Message for decoding [ Name = {}; position = {}; remaining = {} ]", message.getName(), in.position(), in.remaining());

            IMessageStructureVisitor msgStructVisitor = new ITCHVisitorDecode(in, byteOrder, message, msgFactory, itchVisitorSettings);

            MessageStructureWriter.WRITER.traverse(msgStructVisitor, msgStructure);

            int endCurMsgPosition = in.position();
            in.position(startCurMsgPosition);
            int lengthReadByte = endCurMsgPosition - startCurMsgPosition;
            byte[] rawCurMsg = HexDumper.peakBytes(in, Math.max(lengthReadByte, messageLength));
            if (i != 0 && messageLength != lengthReadByte) {
                message.getMetaData().setRejectReason("When traverse a message, more(less) bytes were read than specified. "
                + "Expected length of message: " + messageLength + ", actual: " + lengthReadByte + " bytes were read.");
                in.position(startCurMsgPosition + messageLength);
            } else {
                in.position(endCurMsgPosition);
            }

			// RM9425, RM23982, RM25261, RM25262, RM34032
			if (preprocessor != null) {
				preprocessor.process(message, session, msgStructure);
			}

			boolean drop = false;
            if(codecMessageFilter != null) {
                drop = codecMessageFilter.dropMessage(session, msgStructure, message);
            }

			if (drop) {
				someMsgDropped = true;
			} else {
                boolean isAdmin = Objects.requireNonNull(
                        getAttributeValue(msgStructure, MessageHelper.ATTRIBUTE_IS_ADMIN),
                        () -> "Message " + msgStructure.getName() + " does not have attribute " + MessageHelper.ATTRIBUTE_IS_ADMIN
                );

				MsgMetaData metaData = message.getMetaData();
				metaData.setAdmin(isAdmin);

				//metaData.setToService(this.serviceName);

				String packetAddress;
				if (in instanceof IoBufferWithAddress) {
					packetAddress = ((IoBufferWithAddress)in).getAddress();
				} else {
					try
					{
						packetAddress = session.getRemoteAddress().toString();
					}catch (Exception e)
					{
						packetAddress = "unknown (exception catched)";
					}
				}

				metaData.setFromService(packetAddress);

				metaData.setRawMessage(rawCurMsg);

				messages.add(message);

				logger.debug("Message decoded [ Name = {} ]", message.getName());
			}
		}

        if(!(someMsgDropped && messages.size() == 1 && (System.currentTimeMillis() - lastDecode < 2000))) { //Skip single "UnitHeader"
            if (wrapMessages) {
                IMessage message = wrapToMessageList(messages);

                out.write(message);
            } else {
                messages.forEach(out::write);
            }

			this.lastDecode = System.currentTimeMillis();
		}
		return true;
	}

    @NotNull
    private IMessage wrapToMessageList(IMessage message) {
        return wrapToMessageList(Collections.singletonList(message));
    }

    @NotNull
    private IMessage wrapToMessageList(List<IMessage> messages) {
        IMessage message = DefaultMessageFactory.getFactory().createMessage(ITCHMessageHelper.MESSAGELIST_NAME, ITCHMessageHelper.MESSAGELIST_NAMESPACE);

        message.addField(ITCHMessageHelper.SUBMESSAGES_FIELD_NAME, messages);
        return message;
    }

    @Override
    protected void addToBatch(EvolutionBatch batchMessage, IMessage message) {
        for (IMessage extractedMessage : ITCHMessageHelper.extractSubmessages(message)) {
            super.addToBatch(batchMessage, extractedMessage);
        }
    }

    @Override
    protected IMessage updateBatchMessage(IMessage batchMessage) {
        return wrapToMessageList(super.updateBatchMessage(batchMessage));
    }

    @Override
    protected IMessage updateMessage(IMessage batchMessage) {
        IMessage message = super.updateMessage(batchMessage);
        MsgMetaData metadata = message.getMetaData();
        long batchSequence = metadata.getSequence();
        List<IMessage> subMessages = ITCHMessageHelper.extractSubmessages(message);
        if (subMessages != null) {
            int subSequence = 1;
            int size = subMessages.size();
            for (IMessage subMessage : subMessages) {
                MsgMetaData subMessageMetadata = subMessage.getMetaData();
                MetadataExtensions.setBatchSequence(subMessageMetadata, batchSequence);
                MetadataExtensions.setSubsequence(subMessageMetadata, subSequence);
                if (subSequence == size) {
                    MetadataExtensions.setLastInBatch(subMessageMetadata, true);
                }
                subSequence++;
            }
        }
        return message;
    }

    @Override
	public boolean doDecodeInternal(IoSession session, IoBuffer in,
                                    ProtocolDecoderOutput out) throws Exception
	{
		int position = in.position();
		boolean isDecoded = realDecode(session, in, out);
		if (!isDecoded) {
			in.position(position);
		}
		return isDecoded;
	}

	private int getMessageLength(IFieldStructure message) {
	    int sum = 0;

        for(IFieldStructure field : message.getFields().values()) {
            sum += field.isComplex() ? getMessageLength(field) : StructureUtils.<Integer>getAttributeValue(field, ITCHVisitorBase.LENGTH_ATTRIBUTE);
	    }

	    return sum;
	}

	private void processMessagesLength(List<?> messages) {
	    int sumforHead = 0;
	    for (Object message:messages) {
	        IMessage m = (IMessage) message;
            IFieldStructure structure = msgDictionary.getMessages().get(m.getName());
	        if (structure == null) {
                throw new EPSCommonException("Could not find IMessageStructure for messageName=[" + m.getName() + "] Namespace=[" + m.getNamespace() + "]");
            }
	        Integer length = getMessageLength(structure);
	        sumforHead+=length;

	        if (m.getField("Length") == null) {
	            Number l;
                switch(structure.getFields().get("Length").getJavaType()) {
	            case JAVA_LANG_BYTE:
	                l = length.byteValue();
	                break;
	            case JAVA_LANG_SHORT:
	                l = length.shortValue();
	                break;
	            case JAVA_LANG_INTEGER:
	                l = length.intValue();
	                break;
	            default:
                    throw new EPSCommonException(String.format("Unknown type %s of length field", structure.getFields().get("Length").getJavaType()));
	            }
	            m.addField("Length", l);
	        }
	    }
	    IMessage unitHeader = (IMessage) messages.get(0);
	    unitHeader.addField("Length", sumforHead);
	    unitHeader.addField("MessageCount", (short)(messages.size()-1));
	}


	@SuppressWarnings("unchecked")
	@Override
	public void encode(IoSession session, Object inMessage, ProtocolEncoderOutput out) throws Exception
	{
		if (!(inMessage instanceof IMessage)) {
			throw new IllegalArgumentException("Message parameter is not instance of " + IMessage.class.getCanonicalName() );
		}

		IMessage messagesList = (IMessage)inMessage;

        Object fieldValue = messagesList.getField(ITCHMessageHelper.SUBMESSAGES_FIELD_NAME);

        if(fieldValue == null && !"UnitHeader".equals(messagesList.getName())) {
            throw new NullPointerException("MessagesList didn't contain field: " + ITCHMessageHelper.SUBMESSAGES_FIELD_NAME);
		}
        if("UnitHeader".equals(messagesList.getName())) {
            fieldValue = new ArrayList<IMessage>(1);
            ((List<IMessage>)fieldValue).add(messagesList);
        }


		List<?> messages = (List<?>) fieldValue;

		IoBuffer buffer = IoBuffer.allocate(DEFAULT_BUFFER_SIZE, false);
        ITCHVisitorEncode msgStructVisitor = new ITCHVisitorEncode(buffer, byteOrder);

        if(msgDictionary == null) {
            throw new NullPointerException("ITCH Encode: msgDictionary is not defined");
		}

		processMessagesLength(messages);

		for (Object objMessage : messages) {
			IMessage message = (IMessage) objMessage;

			String msgName = message.getName();
			String msgNamespace = message.getNamespace();

            IMessageStructure msgStructure = msgDictionary.getMessages().get(msgName);

			if (msgStructure == null) {
				throw new EPSCommonException("Could not find IMessageStructure for messageName=[" + msgName + "] Namespace=[" + msgNamespace + "]");
			}

			int startpos = buffer.position();

            MessageStructureReader.READER.traverse(msgStructVisitor, msgStructure, message, MessageStructureReaderHandlerImpl.instance());

			int endpos = buffer.position();
			byte[] rawMessage = new byte[endpos - startpos];

			buffer.position(startpos);
			buffer.get(rawMessage);

            boolean isAdmin = getAttributeValue(msgStructure, "IsAdmin");

			message.getMetaData().setAdmin(isAdmin);
			message.getMetaData().setRawMessage(rawMessage);
		}

		buffer.flip();

		out.write( buffer );
	}
}
