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

package com.exactpro.sf.common.codecs;

import java.util.Queue;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.DummySession;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.AbstractProtocolDecoderOutput;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.junit.Assert;
import org.junit.Test;

import com.exactpro.sf.common.impl.messages.DefaultMessageFactory;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.util.EvolutionBatch;
import com.exactpro.sf.common.util.IEvolutionSettings;
import com.exactpro.sf.services.MockProtocolDecoderOutput;

public class TestAbstractCodec {

    @Test
    public void testNormalMode() throws Exception {
        TestCodec testCodec = new TestCodec(true, false);
        testCodec.init(null, createSettings(false), DefaultMessageFactory.getFactory(), null);
        AbstractProtocolDecoderOutput out = new MockProtocolDecoderOutput();
        byte[] originalData = { 1, 2, 3 };
        testCodec.decode(new DummySession(), IoBuffer.wrap(originalData), out);
        Queue<Object> messageQueue = out.getMessageQueue();
        Assert.assertEquals(originalData.length, messageQueue.size());
    }

    @Test
    public void testEvolutionModeNotAMessageInOut() throws Exception {
        TestCodec testCodec = new TestCodec(true, true);
        testCodec.init(null, createSettings(true), DefaultMessageFactory.getFactory(), null);
        AbstractProtocolDecoderOutput out = new MockProtocolDecoderOutput();
        byte[] originalData = { 1, 2, 3 };
        IoBuffer wrap = IoBuffer.wrap(originalData);
        testCodec.decode(new DummySession(), wrap, out);
        Queue<Object> messageQueue = out.getMessageQueue();
        Assert.assertEquals(1, messageQueue.size());
        Assert.assertEquals(wrap, messageQueue.poll());
    }

    @Test
    public void testEvolutionModeNoBatchIfFalseInDecode() throws Exception {
        TestCodec testCodec = new TestCodec(false, false);
        testCodec.init(null, createSettings(true), DefaultMessageFactory.getFactory(), null);
        AbstractProtocolDecoderOutput out = new MockProtocolDecoderOutput();
        testCodec.decode(new DummySession(), IoBuffer.wrap(new byte[] { 1, 2, 3 }), out);
        Queue<Object> messageQueue = out.getMessageQueue();
        Assert.assertEquals(0, messageQueue.size());
    }

    @Test
    public void testEvolutionModeOnlyBatchGoesFurther() throws Exception {
        TestCodec testCodec = new TestCodec(true, false);
        testCodec.init(null, createSettings(true), DefaultMessageFactory.getFactory(), null);
        AbstractProtocolDecoderOutput out = new MockProtocolDecoderOutput();
        testCodec.decode(new DummySession(), IoBuffer.wrap(new byte[] { 1, 2, 3 }), out);
        Queue<Object> messageQueue = out.getMessageQueue();
        Assert.assertEquals(1, messageQueue.size());
        IMessage message = (IMessage)messageQueue.poll();
        Assert.assertNotNull(message);
        Assert.assertEquals(EvolutionBatch.MESSAGE_NAME, message.getName());
        EvolutionBatch batch = new EvolutionBatch(message);

        Assert.assertEquals(3, batch.size());
    }

    public static IEvolutionSettings createSettings(boolean enabled) {
        return new IEvolutionSettings() {
            @Override
            public boolean isEvolutionSupportEnabled() {
                return enabled;
            }

            @Override
            public void load(HierarchicalConfiguration<ImmutableNode> config) {
                throw new UnsupportedOperationException("load method is not implemented");
            }
        };
    }

    private static final class TestCodec extends AbstractCodec {
        private final boolean decoded;
        private final boolean pushBuffer;

        private TestCodec(boolean decoded, boolean pushBuffer) {
            this.decoded = decoded;
            this.pushBuffer = pushBuffer;
        }

        @Override
        protected boolean doDecodeInternal(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws Exception {
            int position = in.position();
            if (pushBuffer) {
                out.write(
                        IoBuffer.allocate(in.remaining())
                                .put(in)
                );
            } else {
                int count = 0;
                while (count < in.remaining()) {
                    IMessage message = DefaultMessageFactory.getFactory().createMessage("Test", "test");
                    byte[] bytes = new byte[1];
                    in.get(bytes);
                    message.getMetaData().setRawMessage(bytes);
                    out.write(message);
                }
            }
            if (!decoded) {
                in.position(position);
            }
            return decoded;
        }

        @Override
        public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception {
            throw new UnsupportedOperationException("encode method is not implemented");
        }
    }
}