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

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageStructureVisitor;
import com.exactpro.sf.common.messages.MessageStructureWriter;
import com.exactpro.sf.common.messages.MsgMetaData;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.util.HexDumper;
import com.exactpro.sf.services.itch.ITCHVisitorDecode;

public class SOUPUnicastUdpCodec extends SOUPCodec {

	private static final Logger logger = LoggerFactory.getLogger(SOUPUnicastUdpCodec.class);

	private static final String MESSAGE_NAME = "PacketHeader";

	@Override
	protected boolean hasDataPacketHeader() {
		return true;
	}

	@Override
	protected Long getSequenceNumber(IMessage header) {
		return null;
	}

	@Override
	protected IMessage readHeader(IoSession session, IoBuffer in, ProtocolDecoderOutput out, String fromService) {
		// UDP can't be fragmented?

		logger.debug("Read {} (as header)", MESSAGE_NAME);

		byte[] rawData = HexDumper.peakBytes(in, in.remaining());

        IMessageStructure msgStructure = dictionaryStructure.getMessages().get(MESSAGE_NAME);
        if (msgStructure == null) {
            throw new IllegalStateException("Dictionary " + dictionaryStructure.getNamespace() + "doesn't contains message " + MESSAGE_NAME);
        }

        IMessage message = msgFactory.createMessage(msgStructure.getName(), msgStructure.getNamespace());

        logger.trace("Message for decoding [ Name = {}; position = {}; remaining = {} ]", message.getName(),
                in.position(), in.remaining());

        IMessageStructureVisitor msgStructVisitor = new ITCHVisitorDecode(in, byteOrder, message, msgFactory, visitorSettings);
        MessageStructureWriter.WRITER.traverse(msgStructVisitor, msgStructure);

        MsgMetaData metaData = message.getMetaData();
        metaData.setAdmin(true);
        //		metaData.setEnvironment(serviceName.getEnvironment()); // Fill in AbstractMitchClient
        //		metaData.setToService(serviceName.getServiceName()); // Fill in AbstractMitchClient
        metaData.setFromService(fromService);
        metaData.setRawMessage(rawData);

        if (codecMessageFilter == null || !codecMessageFilter.dropMessage(session, msgStructure, message)) {
            out.write(message);
        }

		return message;
	}

}
