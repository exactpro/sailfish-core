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
package com.exactpro.sf.services.tcpip;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.commons.codec.binary.Hex;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.codecs.AbstractCodec;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.MessageUtil;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.util.ICommonSettings;
import com.exactpro.sf.services.IServiceContext;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Codec implements encode / decode message between internal tools by JSON protocol
 * @author nikita.smirnov
 */
public class InternalJsonCodec extends AbstractCodec {

    private static final Logger logger = LoggerFactory.getLogger(InternalJsonCodec.class);

    private static final int HEADER_SIZE = 8 * 2;

    private final ObjectMapper objectMapper;
    private final AtomicLong sequence = new AtomicLong(1);

    private IMessageFactory msgFactory;

    public InternalJsonCodec() {
        objectMapper = new ObjectMapper().enableDefaultTyping()
                .registerModule(new JavaTimeModule());
    }

    @Override
    public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception {
        if (message instanceof IMessage) {
            Map<String, Object> map = MessageUtil.convertToHashMap((IMessage)message);
            String json = this.objectMapper.writeValueAsString(map);

            long seq = this.sequence.getAndIncrement();

            IoBuffer buffer = IoBuffer.allocate(json.length() + HEADER_SIZE);
            buffer.putLong(json.length());
            buffer.putLong(seq);
            buffer.put(json.getBytes());

            out.write(buffer.flip());

            ((IMessage) message).getMetaData().setRawMessage(buffer.array());

            if (logger.isDebugEnabled()) {
                logger.debug("encode() sequnce {} as hex [{}]", seq, buffer.getHexDump());
            }
        } else {
            out.write(message);
        }
    }

    @Override
    public void init(IServiceContext serviceContext, ICommonSettings settings, IMessageFactory msgFactory, IDictionaryStructure dictionary) {
        if (msgFactory == null) {
            throw new IllegalArgumentException("Message factory can't be null");
        }

        this.msgFactory = msgFactory;
    }

    @Override
    protected boolean doDecode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws Exception {
        boolean decoded = false;

        while (in.remaining() > HEADER_SIZE) {
            in.mark();
            long length = in.getLong();
            long seq = in.getLong();

            if (in.remaining() >= length) {
                byte[] buff = new byte[(int) length];
                in.get(buff);

                if (logger.isDebugEnabled()) {
                    logger.debug("decode() sequnce {} as hex [{}]", seq, Hex.encodeHex(buff));
                }
                decoded = true;

                in.reset();
                byte[] rawData = new byte[(int) length + HEADER_SIZE];
                in.get(rawData);

                IMessage message = null;

                try {
                    Map<?, ?> map = this.objectMapper.readValue(buff, HashMap.class);
                    message = MessageUtil.convertToIMessage(map, this.msgFactory, TCPIPMessageHelper.INCOMING_MESSAGE_NAME_AND_NAMESPACE,
                            TCPIPMessageHelper.INCOMING_MESSAGE_NAME_AND_NAMESPACE);

                } catch (Exception e) {
                    message = msgFactory.createMessage("Exception",
                            TCPIPMessageHelper.INCOMING_MESSAGE_NAME_AND_NAMESPACE);

                    message.addField("Cause", e.getMessage());
                }

                message.getMetaData().setRawMessage(rawData);
                out.write(message);
            } else {
                in.reset();
            }
        }

        return decoded;
    }

}
