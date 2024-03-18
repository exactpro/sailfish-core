/*******************************************************************************
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

import java.util.Arrays;

import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.MetadataExtensions;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.util.ICommonSettings;
import com.exactpro.sf.services.IServiceContext;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.messages.AttributeNotFoundException;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageStructureVisitor;
import com.exactpro.sf.common.messages.MessageStructureWriter;
import com.exactpro.sf.common.messages.MsgMetaData;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.services.MessageHelper;
import com.exactpro.sf.services.itch.SOUPVisitorDecode;

import static com.exactpro.sf.common.util.HexDumper.peakBytes;
import static com.exactpro.sf.services.itch.soup.SOUPMessageHelper.PACKET_LENGTH;
import static com.exactpro.sf.services.itch.soup.SOUPMessageHelper.PACKET_TYPE;
import static com.exactpro.sf.services.itch.soup.SOUPMessageHelper.SEQUENCED_DATA_PACKET;
import static com.exactpro.sf.services.itch.soup.SOUPMessageHelper.SOUP_BIN_TCP_HEADER_NAME;
import static com.exactpro.sf.services.itch.soup.SOUPMessageHelper.UNSEQUENCED_DATA_PACKET;
import static org.apache.commons.lang3.ArrayUtils.isEmpty;

public class SOUPTcpCodec extends SOUPCodec {

	private static final Logger logger = LoggerFactory.getLogger(SOUPTcpCodec.class);

	private static final int TCP_SOUP_BIN_HEADER_SIZE = 3;

	private boolean soupBinHeaderAsSeparateMessage = false;

    @Override
    public void init(IServiceContext serviceContext, ICommonSettings settings, IMessageFactory msgFactory, IDictionaryStructure dictionary) {
        super.init(serviceContext, settings, msgFactory, dictionary);
        if (settings instanceof SoupTcpCodecSettings) {
            soupBinHeaderAsSeparateMessage = ((SoupTcpCodecSettings)settings).isParseHeaderAsSeparateMessage();
        }
    }

    @Override
	protected IMessage readHeader(IoSession session, IoBuffer in, ProtocolDecoderOutput out, String fromService) {
		if (in.remaining() < TCP_SOUP_BIN_HEADER_SIZE) {
			return null;
		}

		int startPosition = in.position();

		int length = in.getUnsignedShort();
		Character messageType = (char) in.getUnsigned();

		logger.debug("readSoupBinTCP [MsgType = {}, MsgLengh={}]", messageType, length);

		// Check that all messages received
		in.position(startPosition);
		if (in.remaining() < length + 2) {
			return null;
		}

		// set limit
		in.limit(startPosition + length + 2);

		byte[] soupHeaderRawData = peakBytes(in, TCP_SOUP_BIN_HEADER_SIZE);
		byte[] rawData = peakBytes(in, length + 2); // + 'Length' field length
        boolean dropMessage = false;
		IMessageStructure msgStructure = adminMsgTypeToMsgStruct.get(messageType.toString());
		IMessage message = null;

        if (msgStructure == null) {
			logger.error("Unknown adminMessageType = [{}]", messageType);
			logger.error("{}", rawData);

			message = msgFactory.createMessage("UnknownMessage");
			message.addField("RawMessage", Arrays.toString(rawData));
		} else {
			message = msgFactory.createMessage(msgStructure.getName(), msgStructure.getNamespace());

			logger.trace("Message for decoding {} [ Name = {}; position = {}; remaining = {} ]", message.getName(),
					in.position(), in.remaining());

			IMessageStructureVisitor msgStructVisitor = new SOUPVisitorDecode(in, byteOrder, message, msgFactory, visitorSettings);
            MessageStructureWriter.WRITER.traverse(msgStructVisitor, msgStructure);

			// Message '+' with variable length... Ignore var-length field (in traverse). Skip message till the end:
			if (isDebugPacket(message)) {
				in.position(startPosition);
				in.skip(length + 2); // + 'Length' field length
			}

            dropMessage = codecMessageFilter != null && codecMessageFilter.dropMessage(session, msgStructure, message);
		}

		MsgMetaData metaData = message.getMetaData();
		metaData.setAdmin(true);
//		metaData.setEnvironment(serviceName.getEnvironment()); // Fill in AbstractMitchClient
//		metaData.setToService(serviceName.getServiceName()); // Fill in AbstractMitchClient
		metaData.setFromService(fromService);

        if (soupBinHeaderAsSeparateMessage) {
            IMessage soupHeader = msgFactory.createMessage(SOUP_BIN_TCP_HEADER_NAME);
            soupHeader.addField(PACKET_LENGTH, length);
            soupHeader.addField(PACKET_TYPE, messageType);
            soupHeader.getMetaData().setRawMessage(soupHeaderRawData);
            out.write(soupHeader);
            byte[] rawDataWithoutSoupHeader = getRawDataWithoutSoupHeader(in, startPosition);
            metaData.setRawMessage(rawDataWithoutSoupHeader);
            if (UNSEQUENCED_DATA_PACKET.equals(message.getName())
                    || SEQUENCED_DATA_PACKET.equals(message.getName())
                    || isEmpty(rawDataWithoutSoupHeader)) {
                return message;
            }
        } else {
            if (UNSEQUENCED_DATA_PACKET.equals(message.getName())
                    || SEQUENCED_DATA_PACKET.equals(message.getName())) {
                metaData.setRawMessage(soupHeaderRawData);
            } else {
                metaData.setRawMessage(rawData);
            }
        }

        if (!dropMessage) {
            out.write(message);
        }

		return message;
	}

	@Override
	protected boolean hasDataPacketHeader() {
		return false;
	}

    @Override
    protected boolean hasSoupMessageHeader(IMessageStructure messageStructure) {
        try {
            return !SOUPMessageHelper.isAdmin(messageStructure);
        } catch (AttributeNotFoundException e) {
            throw new EPSCommonException(String.format("[%s] attribute is not exist in the [%s] message",
                    MessageHelper.ATTRIBUTE_IS_ADMIN, messageStructure.getName()), e);
        }
    }

	@Override
	protected Long getSequenceNumber(IMessage header) {
		return null;
	}

	private byte[] getRawDataWithoutSoupHeader(IoBuffer in, int startPosition) {
        int currentPosition = in.position();
        int payloadBegins = startPosition + TCP_SOUP_BIN_HEADER_SIZE;
        in.position(payloadBegins);
        int decodedLength = currentPosition - payloadBegins;
        byte[] decodedRawData = decodedLength > 0 ? peakBytes(in, decodedLength) : new byte[0];
        in.position(currentPosition);
        return decodedRawData;
    }

    @Override
    protected IMessage updateMessage(IMessage batchMessage) {
        IMessage message = super.updateMessage(batchMessage);
        MsgMetaData metadata = message.getMetaData();

        if (metadata.getRawMessage().length == TCP_SOUP_BIN_HEADER_SIZE) { // for messages without body
            return message;
        }

        Integer subsequence = MetadataExtensions.getSubsequence(metadata);
        if (subsequence != null && subsequence == 1) {
            //parseHeaderAsSeparateMessage = false decode output: sequensedDataPacket(subsequence = 1) -> message(subsequence = 2),
            //parseHeaderAsSeparateMessage = true decode output: header(subsequence = 1) -> message(subsequence = 2)
            MetadataExtensions.setSubsequence(metadata, 2);
        }
        return message;
    }

}
