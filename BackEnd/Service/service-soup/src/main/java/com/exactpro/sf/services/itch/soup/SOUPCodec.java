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
package com.exactpro.sf.services.itch.soup;

import com.exactpro.sf.common.codecs.AbstractCodec;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.IMessageStructureVisitor;
import com.exactpro.sf.common.messages.MessageStructureReader;
import com.exactpro.sf.common.messages.MessageStructureReaderHandlerImpl;
import com.exactpro.sf.common.messages.MessageStructureWriter;
import com.exactpro.sf.common.messages.MsgMetaData;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.common.util.HexDumper;
import com.exactpro.sf.common.util.ICommonSettings;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.connectivity.mina.net.IoBufferWithAddress;
import com.exactpro.sf.externalapi.codec.IExternalCodecContext;
import com.exactpro.sf.externalapi.codec.IExternalCodecContext.Role;
import com.exactpro.sf.messages.service.ErrorMessage;
import com.exactpro.sf.services.IServiceContext;
import com.exactpro.sf.services.MessageHelper;
import com.exactpro.sf.services.codecs.CodecMessageFilter;
import com.exactpro.sf.services.codecs.ICodecSettings;
import com.exactpro.sf.services.itch.DefaultPreprocessor;
import com.exactpro.sf.services.itch.IITCHPreprocessor;
import com.exactpro.sf.services.itch.ITCHCodecSettings;
import com.exactpro.sf.services.itch.ITCHMessageHelper;
import com.exactpro.sf.services.itch.ITCHVisitorBase;
import com.exactpro.sf.services.itch.SOUPVisitorDecode;
import com.exactpro.sf.services.itch.SOUPVisitorEncode;
import com.exactpro.sf.services.itch.SOUPVisitorSettings;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Table;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.CharacterCodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.exactpro.sf.common.messages.structures.StructureUtils.getAttributeValue;
import static com.exactpro.sf.services.itch.soup.SOUPMessageHelper.ADMIN_MESSAGE_TYPE_ATTR;
import static com.exactpro.sf.services.itch.soup.SOUPMessageHelper.MESSAGE_LENGTH_FIELD_NAME;
import static com.exactpro.sf.services.itch.soup.SOUPMessageHelper.MOLD_UDP_MESSAGE_HEADER_NAME;
import static com.exactpro.sf.services.itch.soup.SOUPMessageHelper.PACKET_LENGTH;
import static com.exactpro.sf.services.itch.soup.SOUPMessageHelper.PACKET_TYPE;
import static com.exactpro.sf.services.itch.soup.SOUPMessageHelper.ROUTE_ATTRIBUTE_INCOMING;
import static com.exactpro.sf.services.itch.soup.SOUPMessageHelper.ROUTE_ATTRIBUTE_OUTGOING;
import static com.exactpro.sf.services.itch.soup.SOUPMessageHelper.SEQUENCED_DATA_PACKET;
import static com.exactpro.sf.services.itch.soup.SOUPMessageHelper.SEQUENCED_HEADER_MESSAGE;
import static com.exactpro.sf.services.itch.soup.SOUPMessageHelper.UNSEQUENCED_DATA_PACKET;
import static com.exactpro.sf.services.itch.soup.SOUPMessageHelper.UNSEQUENCED_HEADER_MESSAGE;

public class SOUPCodec extends AbstractCodec {
	/*
	 * Notes:
	 * On multicast channel:
	 * * all messages are prepended by DataPacketHeader (one 'Length' field)
	 * * messages grouped by PacketHeader(UnitHeader)
	 *
	 * On TCP recovery channel:
	 * * messages wrapped to TcpSoupBin
	 * * no DataPacketHeader
	 * * no UnitHeader
	 */

	private static final Logger logger = LoggerFactory.getLogger(SOUPCodec.class);

    public static final SailfishURI SOUP_PREPROCESSORS_MAPPING_FILE_URI = SailfishURI.unsafeParse("soup_preprocessors");

	protected static final int DEFAULT_BUFFER_SIZE = 64;

	private static final int HEADER_SIZE = 20;
	private static final int SOUP_MESSAGE_HEADER_SIZE = 3;
	private static final int PACKET_TYPE_SIZE = 1;
	protected static final int MESSAGE_LENGTH_SIZE = 2;
    protected static final int END_OF_SESSION_COUNT_MARKER = 0xFFFF;

    private static final Set<Role> SENDER_ROLE = ImmutableSet.of(Role.SENDER);
    private static final Set<Role> RECEIVER_ROLE = ImmutableSet.of(Role.RECEIVER);
    private static final Set<Role> BOTH_ROLES = ImmutableSet.of(Role.SENDER, Role.RECEIVER);

	protected static final ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;

    protected final Table<String, Role, IMessageStructure> msgTypeToMsgStruct = HashBasedTable.create();
	protected final Map<String, IMessageStructure> adminMsgTypeToMsgStruct = new HashMap<>();

	protected IDictionaryStructure dictionaryStructure;
    @Nullable
    private IMessageStructure packetHeaderStructure;
	protected IMessageFactory msgFactory;
    protected IITCHPreprocessor preprocessor;
    protected String unseqDataPacketType;
    protected String seqDataPacketType;
    protected CodecMessageFilter codecMessageFilter;
    private boolean parseMessageLengthAaSeparateMessage;
    protected SOUPVisitorSettings visitorSettings;

	@Override
	public void init(IServiceContext serviceContext, ICommonSettings settings, IMessageFactory msgFactory, IDictionaryStructure dictionary) {
        super.init(serviceContext, settings, msgFactory, dictionary);
        Objects.requireNonNull(serviceContext, "serviceContext cannot be null");
        Objects.requireNonNull(settings, "settings cannot be null");
        Objects.requireNonNull(msgFactory, "msgFactory cannot be null");
        Objects.requireNonNull(dictionary, "dictionary cannot be null");

        this.dictionaryStructure = dictionary;
		this.msgFactory = msgFactory;

        if (hasDataPacketHeader()) {
            this.packetHeaderStructure = Objects.requireNonNull(dictionaryStructure.getMessages().get(SOUPMessageHelper.PACKET_HEADER_MESSAGE),
                    () -> String.format("Dictionary %s does not have a %s defined", dictionary.getNamespace(), SOUPMessageHelper.PACKET_HEADER_MESSAGE));
        } else {
            this.packetHeaderStructure = null;
        }

        msgTypeToMsgStruct.clear();
        for(IMessageStructure msgStruct : dictionary.getMessages().values()) {
            String route = getAttributeValue(msgStruct, SOUPMessageHelper.ROUTE_ATTRIBUTE);
            Set<Role> roles = BOTH_ROLES;

            if (ROUTE_ATTRIBUTE_INCOMING.equalsIgnoreCase(route)) {
                roles = RECEIVER_ROLE;
            } else if (ROUTE_ATTRIBUTE_OUTGOING.equalsIgnoreCase(route)) {
                roles = SENDER_ROLE;
            }

            String msgType = getAttributeValue(msgStruct, "MessageType");

            if (msgType != null) {
                for (Role role : roles) {
                    if (msgTypeToMsgStruct.put(msgType, role, msgStruct) != null) {
                        throw new EPSCommonException(String.format("MessageType/Role attribute combo should be unique. MessageType: %s, Role: %s", msgStruct.getName(), role));
                    }
                }
            }

            if (roles.equals(SENDER_ROLE)) {
                continue;
            }

            String adminMsgType = getAttributeValue(msgStruct, ADMIN_MESSAGE_TYPE_ATTR);

			if (adminMsgType != null) {
				if (!adminMsgTypeToMsgStruct.containsKey(msgType)) {
					adminMsgTypeToMsgStruct.put(adminMsgType, msgStruct);
				} else {
					throw new EPSCommonException(
							"AdminMessageType attribute should be unique. AdminMessageName:" + msgStruct.getName());
				}
			}
		}

        ICodecSettings itchCodecSettings = (ICodecSettings) settings;
        setUpPreprocessor(serviceContext, itchCodecSettings, SOUP_PREPROCESSORS_MAPPING_FILE_URI, getClass().getClassLoader());
        this.codecMessageFilter = new CodecMessageFilter(itchCodecSettings.getFilterValues());
        codecMessageFilter.init(dictionary);

        this.unseqDataPacketType = extractAdminType(dictionary, UNSEQUENCED_DATA_PACKET);
        this.seqDataPacketType = extractAdminType(dictionary, SEQUENCED_DATA_PACKET);
        this.parseMessageLengthAaSeparateMessage = isMessageLengthAsSeparateMessage(settings);
        this.visitorSettings = new SOUPVisitorSettings();
        if(settings instanceof ITCHCodecSettings) {
            this.visitorSettings.setTrimLeftPaddingEnabled(
                    ((ITCHCodecSettings) settings).isTrimLeftPaddingEnabled()
            );
        }
    }

    protected void setUpPreprocessor(IServiceContext serviceContext, ICodecSettings itchCodecSettings, SailfishURI soupPreprocessorsMappingFileUri, ClassLoader classLoader) {
        this.preprocessor = DefaultPreprocessor.loadPreprocessor(serviceContext,
                itchCodecSettings.getDictionaryURI(), SOUP_PREPROCESSORS_MAPPING_FILE_URI, getClass().getClassLoader());
    }

    private boolean realDecode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) {
		if (logger.isDebugEnabled()) {
			logger.debug("doDecode: [limit:{}; remaining:{}]; Buffer:{}", in.limit(), in.remaining(), HexDumper.getHexdump(in, in.remaining()));
			logger.debug("\n{}", HexDumper.getHexdump(in, in.remaining()));
		}

		in = in.order(byteOrder);

		String packetAddress;
		if (in instanceof IoBufferWithAddress) {
			packetAddress = ((IoBufferWithAddress) in).getAddress();
		} else {
			try {
				packetAddress = session.getRemoteAddress().toString();
			} catch (Exception e) {
				packetAddress = "unknown (exception catched)";
			}
		}

        IMessage header = readHeader(session, in, out, packetAddress);
        if (header == null) {
            return false;
        }

        readMessages(session, in, out, packetAddress, header);

        return true;
	}

    private void readMessages(IoSession session, IoBuffer in, ProtocolDecoderOutput out, String packetAddress, IMessage header) {
        Long sequenceNumber = getSequenceNumber(header);
        // When we read header we set the limit to the expected message length.
        // If we work with UDP the limit will be automatically set because UDP must not be fragment.
        // So, at the end we should have the position equal to limit.
        // If it is not true we should produce an error message to the out with remaining bytes.
        int endPosition = in.limit();
        try {
            while (in.hasRemaining()) {
                readMessage(session, in, out, header, sequenceNumber, packetAddress);
                if (sequenceNumber != null) {
                    sequenceNumber++;
                }
            }
        } finally {
            remainingBytesAsErrorMessage(in, out, endPosition);
        }
    }

    private void remainingBytesAsErrorMessage(IoBuffer in, ProtocolDecoderOutput out, int endPosition) {
        int curPosition = in.position();
        if (curPosition != endPosition) {
            int remaining = endPosition - curPosition;
            logger.error("Remaining bytes left: {}", remaining);
            addErrorMessage(in, curPosition, out, remaining + " remaining byte(s) left. "
                    + "Previous message has incorrect structure or the buffer contains unexpected bytes at the end");
            in.position(endPosition);
        }
    }

    protected Long getSequenceNumber(IMessage header) {
		return header.getField("PHSequence");
	}

	protected void readMessage(IoSession session, IoBuffer in, ProtocolDecoderOutput out, IMessage header, Long sequenceNumber, String packetAddress) {
		int startPosition = in.position();
		int length = 0;
		if (hasDataPacketHeader()) {
			length = in.getUnsignedShort(); // 'Length' field
		}

		readMessageWithoutHeader(session, in, out, header, sequenceNumber, packetAddress, startPosition, length);
	}

    protected void readMessageWithoutHeader(IoSession session, IoBuffer in, ProtocolDecoderOutput out, IMessage header, Long sequenceNumber, String packetAddress, int startPosition, int length) {
        // read message type
        String messageType = Character.toString((char) in.getUnsigned());

        in.position(startPosition);
        if (hasDataPacketHeader()) {
            in.skip(MESSAGE_LENGTH_SIZE); // 'Length' field
        }

        logger.debug("MsgType = [{}]", messageType);

        Role role = session.containsAttribute(IExternalCodecContext.class) ? ((IExternalCodecContext)session.getAttribute(IExternalCodecContext.class)).getRole() : Role.RECEIVER;
        IMessageStructure msgStructure = msgTypeToMsgStruct.get(messageType, role);

        int declaredLimit = in.limit();
        int limit = 0;
        if (length != 0) {
            declaredLimit = startPosition + length;
            if (hasDataPacketHeader()) {
                declaredLimit += MESSAGE_LENGTH_SIZE;
            }
            limit = in.limit();
            in.limit(declaredLimit);
        }
        if (msgStructure == null) {
            String errStr = "Unknown messageType = [" + messageType + ']';
            addErrorMessage(in, startPosition, out, errStr);
            in.position(declaredLimit); // consider that all left bytes are incorrect
            return;
        }

        IMessage message = msgFactory.createMessage(msgStructure.getName(), msgStructure.getNamespace());

        // substitute header:
        substituteHeader(header, message, sequenceNumber);

        logger.trace("Message for decoding [ Name = {}; position = {}; remaining = {} ]", message.getName(),
                in.position(), in.remaining());



        try {
            IMessageStructureVisitor msgStructVisitor = new SOUPVisitorDecode(in, byteOrder, message, msgFactory, visitorSettings);
            MessageStructureWriter.WRITER.traverse(msgStructVisitor, msgStructure);
        } catch(EPSCommonException e) {
            message.getMetaData().setRejectReason(String.format("%s: %s", e.getMessage(), ExceptionUtils.getRootCauseMessage(e)));
            // we use limit in case the message length was not set
            // because we won't be able to decode the rest of the payload anyway
            // so we skip the whole payload to be able to continue the decoding
            // the limit was set in SOUPCodec#readHeader method

            int lengthRaw = declaredLimit - startPosition;
            message.getMetaData().setRawMessage(rawData(in, startPosition, lengthRaw));
            in.position(declaredLimit);
        }

        if (length != 0) {
            if (in.hasRemaining()) {
                int realLength = in.position() - startPosition;
                if (hasDataPacketHeader()) {
                    realLength -= MESSAGE_LENGTH_SIZE;
                }
                message.getMetaData().setRejectReason(
                        String.format("Declared message length {%s} isn't equal to real one {%s}", length, realLength));
                in.position(declaredLimit);
            }
            in.limit(limit);
        }
        if (hasSoupMessageHeader(msgStructure) && header != null
                && SEQUENCED_DATA_PACKET.equalsIgnoreCase(header.getName())) {
            message.addField(SEQUENCED_HEADER_MESSAGE, header);
        }

        if (preprocessor != null) {
            preprocessor.process(message, session, msgStructure);
        }

        if (codecMessageFilter != null && codecMessageFilter.dropMessage(session, msgStructure, message)) {
            return;
        }

        startPosition = addMessageLengthAsSeparateMessage(in, msgStructure.getNamespace(), length, out, startPosition);

        boolean isAdmin = getAttributeValue(msgStructure, MessageHelper.ATTRIBUTE_IS_ADMIN);

        prepareMetadata(message, isAdmin, packetAddress, in, startPosition);

        out.write(message);

        logger.debug("Message decoded [ Name = {} ]", message.getName());
    }

    protected int addMessageLengthAsSeparateMessage(IoBuffer in, String namespaceName, int length, ProtocolDecoderOutput out, int startPosition) {
        if (parseMessageLengthAaSeparateMessage) {
            IMessage lengthMessage;
            if (namespaceName == null) {
                lengthMessage = msgFactory.createMessage(MOLD_UDP_MESSAGE_HEADER_NAME);
            } else {
                lengthMessage = msgFactory.createMessage(MOLD_UDP_MESSAGE_HEADER_NAME, namespaceName);
            }
            lengthMessage.addField(MESSAGE_LENGTH_FIELD_NAME, length);
            lengthMessage.getMetaData().setRawMessage(rawData(in, startPosition, MESSAGE_LENGTH_SIZE));
            out.write(lengthMessage);
            return startPosition + MESSAGE_LENGTH_SIZE;
        }

        return startPosition;
    }

    protected void prepareMetadata(IMessage message, boolean isAdmin, String packetAddress, IoBuffer in, int startPosition) {
        MsgMetaData metaData = message.getMetaData();
        metaData.setAdmin(isAdmin);
        //		metaData.setEnvironment(serviceName); // Fill in AbstractMitchClient
        //		metaData.setToService(serviceName); // Fill in AbstractMitchClient
        metaData.setFromService(packetAddress);
        metaData.setRawMessage(rawData(in, startPosition, in.position() - startPosition));
    }

    protected void substituteHeader(IMessage header, IMessage message, Long sequenceNumber) {
        if (header != null) {
            for (String field : header.getFieldNames()) {
                message.addField(field, header.getField(field));
            }
        }

        if (sequenceNumber != null) {
            message.addField(ITCHMessageHelper.FAKE_FIELD_MESSAGE_SEQUENCE_NUMBER, sequenceNumber);
        }
    }

    protected byte[] rawData(IoBuffer in, int startPosition, int length) {
	    int endPosition = in.position();
        in.position(startPosition);
        byte[] result = HexDumper.peakBytes(in, length);
        in.position(endPosition);
        return result;
    }
    
    protected void addErrorMessage(IoBuffer in, int startPosition, ProtocolDecoderOutput out, String errorString) {
        ErrorMessage errorMessage = new ErrorMessage(msgFactory);
        errorMessage.setCause(errorString);
        IMessage msg = errorMessage.getMessage();
        msg.getMetaData().setRejectReason(errorString);
        msg.getMetaData().setRawMessage(rawData(in, startPosition, in.remaining()));
        out.write(msg);
    }
    
    protected boolean hasDataPacketHeader() {
		return true;
    }

    protected boolean hasSoupMessageHeader(IMessageStructure messageStructure) {
        return false;
    }

	private String readString(IoBuffer buffer, int length) {
		byte[] array = new byte[length];

		buffer.get(array);

		try {
            return ITCHVisitorBase.decoder.get().decode(ByteBuffer.wrap(array)).toString().trim();
		} catch (CharacterCodingException e) {
			throw new EPSCommonException(e);
		}
	}

	@Override
	public boolean doDecodeInternal(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws Exception {
		int position = in.position();
		int limit = in.limit();
		try {
			boolean isDecoded = realDecode(session, in, out);
			if (!isDecoded) {
				in.position(position);
			}
			return isDecoded;
		} finally {
			in.limit(limit);
		}
	}

	@Override
	public void encode(IoSession session, Object inMessage, ProtocolEncoderOutput out) throws Exception {
		if (!(inMessage instanceof IMessage)) {
			throw new IllegalArgumentException(
					"Message parameter is not instance of " + IMessage.class.getCanonicalName());
		}

        if (dictionaryStructure == null) {
            throw new NullPointerException("Encode error: msgDictionary is not defined");
        }

		IoBuffer buffer = IoBuffer.allocate(DEFAULT_BUFFER_SIZE, false);

		IMessage message = (IMessage) inMessage;

		String msgName = message.getName();
		String msgNamespace = message.getNamespace();

        IMessageStructure msgStructure = dictionaryStructure.getMessages().get(msgName);
		if (msgStructure == null) {
			throw new EPSCommonException("Could not find MessageStructure for messageName=[" + msgName + "] Namespace=[" + msgNamespace + "]");
		}

		skipPackageHeaderBytes(buffer, message, msgStructure);

        int startpos = buffer.position();

        fillCollectionSize(msgStructure,message);

        encodeMessageWithoutPackageHeader(buffer, msgStructure, message);

		int endpos = buffer.position();
		buffer.position(startpos);

        boolean isAdmin = ObjectUtils.defaultIfNull(getAttributeValue(msgStructure, "IsAdmin"), false);
        message.getMetaData().setAdmin(isAdmin);

        int messageLength = endpos - startpos;
        encodePackageHeader(session, buffer, message, msgStructure, messageLength);

        buffer.position(0);
        buffer.limit(endpos);

        // we need to set all raw data to the message (header + body)
        byte[] rawMessage = new byte[buffer.limit()];
        buffer.get(rawMessage);
        message.getMetaData().setRawMessage(rawMessage);

        out.write(buffer.flip());
    }

    protected void encodeMessageWithoutPackageHeader(IoBuffer buffer, IMessageStructure msgStructure, IMessage message) {
        IMessageStructureVisitor msgStructVisitor = getEncodeVisitor(buffer, byteOrder);
        MessageStructureReader.READER.traverse(msgStructVisitor, msgStructure, message, MessageStructureReaderHandlerImpl.instance());
    }

    protected void encodePackageHeader(IoSession session, IoBuffer buffer, IMessage message, IMessageStructure msgStructure, int messageLength) {
        if (hasSoupMessageHeader(msgStructure)) {
            addPacketHeader(buffer, message, messageLength);
        }
    }

    protected void skipPackageHeaderBytes(IoBuffer buffer, IMessage message, IMessageStructure msgStructure) {
        if (hasSoupMessageHeader(msgStructure)) {
            buffer.position(SOUP_MESSAGE_HEADER_SIZE);
        }
    }

    protected IMessageStructureVisitor getEncodeVisitor(IoBuffer buffer, ByteOrder byteOrder) {
        return new SOUPVisitorEncode(buffer, byteOrder);
    }

    private void fillCollectionSize(IMessageStructure msgStructure, IMessage message) {
	    msgStructure.getFields().forEach( (fieldName,fieldStructure) -> {
	        if(fieldStructure.isCollection()) {
                String sizeFieldName = getAttributeValue(fieldStructure,SOUPMessageHelper.SIZE_FIELD);
                if( sizeFieldName != null) {
                    if (!message.isFieldSet(sizeFieldName)) {
                        List<?> list = message.getField(fieldName);
                        if(list == null){
                            message.addField(sizeFieldName,0);
                        }else{
                            message.addField(sizeFieldName,list.size());
                        }
                    }
                }
            }
	    });
    }

    /**If this method return null:
      * <ul>
      *     <li> not enough data in buffer
      *     <li> and readMessage() will not called
      * </ul>
      * If this method return IMessage (!= null):
      * <ul>
      *     <li> Some header was read and stored in DB
      *     <li> and this method set limit() on buffer just after end of payload
      *     <li> and readMessages() will called until buffer has data
      * </ul>
      */
	protected IMessage readHeader(IoSession session, IoBuffer in, ProtocolDecoderOutput out, String fromService) {
        if (in.remaining() < HEADER_SIZE) {
            addErrorMessage(in, in.position(), out, "There are less bytes left in the buffer than the size of the header." +
                    " Remaining: " + in.remaining() + ", header size: " + HEADER_SIZE);
            return null;
        }
        int startPosition = in.position();
        String protocolSession = readString(in, 10);
		long protocolSequence = in.getLong(); // FIXME: unsigned
		int messageCount = in.getUnsignedShort();

		int messagesPosition = in.position();

		// Data Header:
        int accumulatedLength = 0;
        if (needToAccumulateContentLength(messageCount)) {
            for (int i = 0; i < messageCount; i++) {
                if (in.remaining() < MESSAGE_LENGTH_SIZE) {
                    addErrorMessage(in, in.position(), out, "There are less bytes left in the buffer than is necessary to decode the message length."
                            + "Need " + MESSAGE_LENGTH_SIZE + " byte(s), have: " + in.remaining() + " byte(s)");
                    return null;
                }
                int length = in.getUnsignedShort();
                if (in.remaining() < length) {
                    addErrorMessage(in, in.position(), out, "There are less bytes left in the buffer than message length." +
                            " Remaining: " + in.remaining() + ", message length: " + length);
                    return null;
                }
                in.skip(length);
                accumulatedLength += length + MESSAGE_LENGTH_SIZE;  // + 'Length' field length
            }
        }

		// we have all data... we ready to parse... set limits
		in.position(messagesPosition);
		in.limit(messagesPosition + accumulatedLength);
        
        IMessage packetHeader = msgFactory.createMessage(
                packetHeaderStructure == null ? SOUPMessageHelper.PACKET_HEADER_MESSAGE : packetHeaderStructure.getName(), dictionaryStructure.getNamespace());
        packetHeader.addField("PHSession", protocolSession);
        packetHeader.addField("PHSequence", protocolSequence);
        packetHeader.addField("PHCount", messageCount);
        
        MsgMetaData metaData = packetHeader.getMetaData();
        metaData.setAdmin(true);
        //		metaData.setEnvironment(serviceName); // Fill in AbstractMitchClient
        //		metaData.setToService(serviceName); // Fill in AbstractMitchClient
        metaData.setFromService(fromService);
        metaData.setRawMessage(rawData(in, startPosition, HEADER_SIZE));

        if (codecMessageFilter == null || packetHeaderStructure == null || !codecMessageFilter.dropMessage(session, packetHeaderStructure, packetHeader)) {
            out.write(packetHeader);
        }

		return packetHeader;
	}

    protected boolean needToAccumulateContentLength(int messageCount) {
        return messageCount != END_OF_SESSION_COUNT_MARKER;
    }

    protected boolean isDebugPacket(IMessage message) {
        return SOUPMessageHelper.DEBUG_PACKET_NAME.equals(message.getName())
                || SOUPMessageHelper.UNDOCUMENTED_ERROR_MESSAGE.equals(message.getName());
    }

    protected String extractAdminType(IDictionaryStructure dictionary, String adminMessageName) {
        IMessageStructure adminMessage = dictionary.getMessages().get(adminMessageName);
        if (adminMessage == null) {
            throw new EPSCommonException(String.format("[%s] is not present in the [%s] dictionary",
                    adminMessageName, dictionary.getNamespace()));
        }
        String attributeValue = getAttributeValue(adminMessage, ADMIN_MESSAGE_TYPE_ATTR);
        if (attributeValue == null) {
            throw new EPSCommonException(String.format("[%s] is not present in the [%s] message",
                    ADMIN_MESSAGE_TYPE_ATTR, adminMessageName));
        }if (attributeValue.length() > 1) {
            throw new EPSCommonException(String.format("[%s] value of [%s] attribute in the [%s] message must be one char",
                    attributeValue, ADMIN_MESSAGE_TYPE_ATTR, adminMessageName));
        }
        return attributeValue;
    }

    protected void addPacketHeader(IoBuffer buffer, IMessage message, int messageLength) {
        boolean isSequenced = message.isFieldSet(SEQUENCED_HEADER_MESSAGE);
        boolean isUnsequenced = message.isFieldSet(UNSEQUENCED_HEADER_MESSAGE);

        if (isSequenced && isUnsequenced) {
            throw new EPSCommonException("The message contains " + SEQUENCED_HEADER_MESSAGE + " and " + UNSEQUENCED_HEADER_MESSAGE + ", although it should only contain one of them.");
        }

        String dataPacketType = unseqDataPacketType;
        String headerFieldName = UNSEQUENCED_HEADER_MESSAGE;
        String headerMessageName = UNSEQUENCED_DATA_PACKET;

        if(isSequenced) {
            dataPacketType = seqDataPacketType;
            headerFieldName = SEQUENCED_HEADER_MESSAGE;
            headerMessageName = SEQUENCED_DATA_PACKET;
        }

        int packetLength = messageLength + PACKET_TYPE_SIZE;
        buffer.position(0);
        buffer.putShort((short)packetLength);
        buffer.putUnsigned(dataPacketType.charAt(0));
        IMessage packetHeader = message.getField(headerFieldName);
        if (packetHeader == null) {
            packetHeader = msgFactory.createMessage(
                    headerMessageName, dictionaryStructure.getNamespace());
            packetHeader.addField(PACKET_LENGTH, packetLength);
            packetHeader.addField(PACKET_TYPE, dataPacketType);
            message.addField(headerFieldName, packetHeader);
        } else {
            if (!packetHeader.isFieldSet(PACKET_LENGTH)) {
                packetHeader.addField(PACKET_LENGTH, packetLength);
            }
            if (!packetHeader.isFieldSet(PACKET_TYPE)) {
                packetHeader.addField(PACKET_TYPE, dataPacketType);
            }
        }
    }

    private boolean isMessageLengthAsSeparateMessage(ICommonSettings settings) {
	    return settings instanceof MoldUdpCodecSettings
                && ((MoldUdpCodecSettings)settings).isParseMessageLengthAsSeparateMessage();
    }
}
