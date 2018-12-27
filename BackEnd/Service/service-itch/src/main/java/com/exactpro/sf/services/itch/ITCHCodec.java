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
package com.exactpro.sf.services.itch;

import com.exactpro.sf.common.codecs.AbstractCodec;
import com.exactpro.sf.common.impl.messages.DefaultMessageFactory;
import com.exactpro.sf.common.messages.*;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.common.util.HexDumper;
import com.exactpro.sf.common.util.ICommonSettings;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.connectivity.mina.net.IoBufferWithAddress;
import com.exactpro.sf.services.IServiceContext;
import com.exactpro.sf.services.MessageHelper;
import com.exactpro.sf.services.codecs.CodecMessageFilter;
import com.exactpro.sf.services.mina.MINAUtil;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteOrder;
import java.util.*;

public class ITCHCodec extends AbstractCodec {
    private static final SailfishURI ITCH_PREPROCESSORS_MAPPING_FILE_URI = SailfishURI.unsafeParse("itch_preprocessors");

    private static final Logger logger = LoggerFactory.getLogger(ITCHCodec.class);

	private static final int DEFAULT_BUFFER_SIZE = 64;

	private static final int HEADER_SIZE = 8;

	private final Map<Short, IMessageStructure> msgTypeToMsgStruct = new HashMap<>();
	private final MessageStructureReader msgStructReader = new MessageStructureReader();
	private final MessageStructureWriter msgStructWriter = new MessageStructureWriter();;
	private final ByteOrder byteOrder = ByteOrder.LITTLE_ENDIAN;

	private IDictionaryStructure msgDictionary;
	private IMessageFactory msgFactory;
	private int msgLengthFieldSize = 1;
    private CodecMessageFilter codecMessageFilter;
    private long lastDecode;
    private IITCHPreprocessor preprocessor;

    public ITCHCodec() {
		this.lastDecode = System.currentTimeMillis();
	}

	@Override
	public void init(IServiceContext serviceContext, ICommonSettings settings, IMessageFactory msgFactory, IDictionaryStructure dictionary)
	{
		this.msgDictionary = Objects.requireNonNull(dictionary, "'Dictionary' parameter cannot be null");
		this.msgFactory = msgFactory;

		if (settings != null) {
            ITCHCodecSettings msettings = (ITCHCodecSettings) settings;
			this.msgLengthFieldSize = msettings.getMsgLength();
			this.codecMessageFilter = new CodecMessageFilter(msettings.getFilterValues());
            this.preprocessor = DefaultPreprocessor.loadPreprocessor(serviceContext,
                    msettings.getDictionaryURI(), ITCH_PREPROCESSORS_MAPPING_FILE_URI,
                        this.getClass().getClassLoader());
		}
        if(codecMessageFilter != null) {
            codecMessageFilter.init(dictionary);
        }

		this.msgTypeToMsgStruct.clear();

		for (IMessageStructure msgStruct : dictionary.getMessageStructures()) {
            Short msgType = (Short) msgStruct.getAttributeValueByName(ITCHMessageHelper.ATTRIBUTE_MESSAGE_TYPE);
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
			short messageType;
			int messageLength;

			if (i == 0) {
                messageType = ITCHMessageHelper.UNITHEADERMSGTYPE;
				messageLength = length;
			} else {
				int currentPos = in.position();
				messageLength = (msgLengthFieldSize == 2) ? in.getUnsignedShort() : in.getUnsigned();
				messageType = in.getUnsigned();
				in.position(currentPos);
			}

			byte[] rawMessage = HexDumper.peakBytes(in, messageLength);

			logger.debug("MsgType = [{}]", messageType);

			IMessageStructure msgStructure = msgTypeToMsgStruct.get(messageType);

			if (msgStructure == null) {
				throw new EPSCommonException("Unknown messageType = [" + messageType + "]");
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

			logger.debug("Message for decoding {} [ Name = {}; position = {}; remaining = {} ]", message.getName(), in.position(), in.remaining());

            IMessageStructureVisitor msgStructVisitor = new ITCHVisitorDecode(in, byteOrder, message, msgFactory);

			msgStructWriter.traverse(msgStructVisitor, msgStructure);

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
				boolean isAdmin = (Boolean)msgStructure.getAttributeValueByName(MessageHelper.ATTRIBUTE_IS_ADMIN);

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

				metaData.setRawMessage(rawMessage);

				messages.add(message);

				logger.debug("Message decoded [ Name = {} ]", message.getName());
			}
		}

		if (!(someMsgDropped && messages.size() == 1 && (System.currentTimeMillis() - this.lastDecode < 2000))) { //Skip single "UnitHeader"
            IMessage message = DefaultMessageFactory.getFactory().createMessage(ITCHMessageHelper.MESSAGELIST_NAME, ITCHMessageHelper.MESSAGELIST_NAMESPACE);

            message.addField(ITCHMessageHelper.SUBMESSAGES_FIELD_NAME, messages);

			out.write(message);

			this.lastDecode = System.currentTimeMillis();
		}
		return true;
	}



	@Override
	public boolean doDecode(IoSession session, IoBuffer in,
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
	    for (IFieldStructure field:message.getFields()) {

	        if (field.isComplex()) {
	            sum+=getMessageLength(field);
	        } else {
                sum += (int) field.getAttributeValueByName(ITCHVisitorBase.LENGTH_ATTRIBUTE);
	        }
	    }

	    return sum;
	}

	private void processMessagesLength(List<?> messages) {
	    int sumforHead = 0;
	    for (Object message:messages) {
	        IMessage m = (IMessage) message;
	        IFieldStructure structure = this.msgDictionary.getMessageStructure(m.getName());
	        if (structure == null) {
                throw new EPSCommonException("Could not find IMessageStructure for messageName=[" + m.getName() + "] Namespace=[" + m.getNamespace() + "]");
            }
	        Integer length = getMessageLength(structure);
	        sumforHead+=length;

	        if (m.getField("Length") == null) {
	            Number l;
	            switch (structure.getField("Length").getJavaType()) {
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
	                throw new EPSCommonException(String.format("Unknown type %s of length field", structure.getField("Length").getJavaType()));
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

		if (fieldValue == null && !messagesList.getName().equals("UnitHeader")) {
            throw new NullPointerException("MessagesList didn't contain field: " + ITCHMessageHelper.SUBMESSAGES_FIELD_NAME);
		}
        if (messagesList.getName().equals("UnitHeader")) {
            fieldValue = new ArrayList<IMessage>(1);
            ((List<IMessage>)fieldValue).add(messagesList);
        }


		List<?> messages = (List<?>) fieldValue;

		IoBuffer buffer = IoBuffer.allocate(DEFAULT_BUFFER_SIZE, false);
        ITCHVisitorEncode msgStructVisitor = new ITCHVisitorEncode(buffer, byteOrder);



		if (this.msgDictionary == null) {
            throw new NullPointerException("ITCH Encode: msgDictionary is not defined");
		}

		processMessagesLength(messages);

		for (Object objMessage : messages) {
			IMessage message = (IMessage) objMessage;

			String msgName = message.getName();
			String msgNamespace = message.getNamespace();

			IMessageStructure msgStructure = this.msgDictionary.getMessageStructure(msgName);

			if (msgStructure == null) {
				throw new EPSCommonException("Could not find IMessageStructure for messageName=[" + msgName + "] Namespace=[" + msgNamespace + "]");
			}

			int startpos = buffer.position();

			msgStructReader.traverse(msgStructVisitor, msgStructure, message, MessageStructureReaderHandlerImpl.instance());

			int endpos = buffer.position();
			byte[] rawMessage = new byte[endpos - startpos];

			buffer.position(startpos);
			buffer.get(rawMessage);

			boolean isAdmin = (Boolean)msgStructure.getAttributeValueByName("IsAdmin");

			message.getMetaData().setAdmin(isAdmin);
			message.getMetaData().setRawMessage(rawMessage);
		}

		buffer.flip();

		out.write( buffer );
	}
}
