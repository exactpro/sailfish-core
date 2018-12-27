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

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.DummySession;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import java.time.LocalDateTime;
import java.time.Month;

import com.exactpro.sf.common.codecs.AbstractCodec;
import com.exactpro.sf.common.impl.messages.DefaultMessageFactory;
import com.exactpro.sf.common.impl.messages.MapMessage;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.util.AbstractTest;

/**
 * @author nikita.smirnov
 *
 */
public class TestInternalJsonCodec extends AbstractTest {

    private AbstractCodec codec;
    private IoSession session;

    @Before
    public void init() {
        this.codec = new InternalJsonCodec();
        this.codec.init(serviceContext, null, DefaultMessageFactory.getFactory(), null);

        this.session = new DummySession();
    }


    @Test
    public void testEncode() throws Exception {
        final IMessage msg = new MapMessage("namespace", "name");
        msg.addField("MessageType", 123);
        msg.addField("Qty", 123.123);
        msg.addField("ClOrdID", "");
        msg.addField("fieldDate", LocalDateTime.of(2010, Month.JANUARY, 1, 11, 22, 33, 444_555_666));

        final ProtocolDecoderOutput decoderOutput = Mockito.mock(ProtocolDecoderOutput.class);
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                IMessage decoded = invocation.getArgument(0);

                Assert.assertEquals(msg.getFieldNames(), decoded.getFieldNames());
                for (String column : decoded.getFieldNames()) {
                    Assert.assertEquals(column, msg.<Object>getField(column), decoded.<Object>getField(column));
                }
                return null;
            }
        }).when(decoderOutput).write(Mockito.anyObject());

        final ProtocolEncoderOutput encoderOutput = Mockito.mock(ProtocolEncoderOutput.class);
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(org.mockito.invocation.InvocationOnMock invocation) throws Throwable {
                codec.decode(session, invocation.getArgument(0), decoderOutput);
                return null;
            };
        } ).when(encoderOutput).write(Mockito.anyObject());

        codec.encode(this.session, msg, encoderOutput);
    }

    @Test
    public void testDecoder() throws Exception {
        IoBuffer buffer = IoBuffer.allocate(1024);

        ProtocolDecoderOutput decoderOutput = Mockito.mock(ProtocolDecoderOutput.class);
        Mockito.doAnswer(new Answer<Void>() {

            private String[] responses = new String[] { "Cause=Unexpected end-of-input within/between Object entries",
                    "Qty=123.123|ClOrdID=|MessageType=123", "Qty=456.456|ClOrdID=ORD34|MessageType=100",
                    "Qty=777.777|ClOrdID=WithDate|fieldDate=2010-01-01T11:22:33.444555666|MessageType=123"
            };
            private int index = 0;

            @Override
            public Void answer(org.mockito.invocation.InvocationOnMock invocation) throws Throwable {
                String input = invocation.<IMessage>getArgument(0).toString();
                Assert.assertTrue("index: " + index + " expect: " + this.responses[index] + " actual: " + input, input.startsWith(this.responses[index]));
                index++;
                return null;
            };
        }).when(decoderOutput).write(Mockito.anyObject());

        String data = "{\"Qty\":123.123,\"ClOrdID\"";
        buffer.putLong(data.length());
        buffer.putLong(1L);
        buffer.put(data.getBytes());

        data = "{\"Qty\":123.123,\"ClOrdID\":\"\",\"MessageType\":123}";
        buffer.putLong(data.length());
        buffer.putLong(2L);
        buffer.put(data.getBytes());

        data = "{\"Qty\":456.456,\"ClOrdID\":\"ORD34\",\"MessageType\":100}";
        buffer.putLong(data.length());
        buffer.putLong(3L);
        buffer.put(data.getBytes());

        data = "{\"Qty\":777.777,\"ClOrdID\":\"WithDate\",\"fieldDate\":[\"java.time.LocalDateTime\",\"2010-01-01T11:22:33.444555666\"],\"MessageType\":123}";
        buffer.putLong(data.length());
        buffer.putLong(4L);
        buffer.put(data.getBytes());
        buffer.flip();

        codec.decode(this.session, buffer, decoderOutput);
    }
}
