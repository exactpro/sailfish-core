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
package com.exactpro.sf.common.codecs;

import java.util.Objects;
import java.util.Queue;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.AbstractProtocolDecoderOutput;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.util.EvolutionBatch;
import com.exactpro.sf.common.util.ICommonSettings;
import com.exactpro.sf.common.util.IEvolutionSettings;
import com.exactpro.sf.services.IServiceContext;
import com.exactpro.sf.services.MockProtocolDecoderOutput;

public abstract class AbstractCodec extends CumulativeProtocolDecoder implements ProtocolEncoder {
    private static final Logger logger = LoggerFactory.getLogger(AbstractCodec.class);
    private boolean evolutionSupport;
    private IMessageFactory messageFactory;

    public void init(IServiceContext serviceContext, ICommonSettings settings, IMessageFactory msgFactory, IDictionaryStructure dictionary) {
        this.messageFactory = Objects.requireNonNull(msgFactory, "'Msg factory' parameter");
        if (settings instanceof IEvolutionSettings) {
            IEvolutionSettings evolutionSettings = (IEvolutionSettings)settings;
            evolutionSupport = evolutionSettings.isEvolutionSupportEnabled();
        }
    }

    @Override
    protected boolean doDecode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws Exception {
        if (!evolutionSupport) {
            return doDecodeInternal(session, in, out);
        }

        AbstractProtocolDecoderOutput mockOutput = new MockProtocolDecoderOutput();
        boolean dataDecoded = doDecodeInternal(session, in, mockOutput);
        if (!dataDecoded) {
            return false;
        }
        Queue<Object> messageQueue = mockOutput.getMessageQueue();
        if (!messageQueue.isEmpty()) {
            int outputSize = messageQueue.size();
            EvolutionBatch batchMessage = new EvolutionBatch(outputSize);
            for (Object obj : messageQueue) {
                if (obj instanceof IMessage) {
                    IMessage message = (IMessage)obj;
                    addToBatch(batchMessage, message);
                } else {
                    out.write(obj); // if it is not IMessage probably there is no IMessage in out. And the EB will be empty
                }
            }
            int batchSize = batchMessage.size();
            if (outputSize != batchSize) {
                logger.warn("Some object from output are not messages. Output objects: {}; Messages: {}", outputSize, batchSize);
            }
            if (!batchMessage.isEmpty()) {
                out.write(updateBatchMessage(batchMessage.toMessage(messageFactory)));
            }
        }
        return true;
    }

    /**
     * Should be overridden if protocol requires specific structure for messages in output.
     *
     * Note: The method will be called only during the decoding and only if evolution support is enabled.
     *
     * @param batchMessage batch message to be stored
     * @return the updated view of {@code batchMessage} if protocol requires the specific structure of messages in output.
     *         By default, returns batch message without any change
     */
    protected IMessage updateBatchMessage(IMessage batchMessage) {
        return batchMessage;
    }

    /**
     * This method should be used if codec produces the specific message structure to the output
     * that requires extracting the original messages from it.
     *
     * Note: The method will be called only during the decoding and only if evolution support is enabled.
     * @param batchMessage current batch
     * @param message the decoded message
     */
    protected void addToBatch(EvolutionBatch batchMessage, IMessage message) {
        batchMessage.addMessage(message);
    }

    /**
     * Duplicate for {@link #doDecode(IoSession, IoBuffer, ProtocolDecoderOutput)} to allow custom logic for Evolution project.<br/>
     *
     * This method will be invoked in the {@link #doDecode(IoSession, IoBuffer, ProtocolDecoderOutput)}.
     * It should have the same behavior as should the original method. Read the Javadoc for the original method.
     * @see CumulativeProtocolDecoder#doDecode(IoSession, IoBuffer, ProtocolDecoderOutput)
     */
    protected boolean doDecodeInternal(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws Exception {
        throw new UnsupportedOperationException("'doDecodeInternal' is not implemented");
    }
}