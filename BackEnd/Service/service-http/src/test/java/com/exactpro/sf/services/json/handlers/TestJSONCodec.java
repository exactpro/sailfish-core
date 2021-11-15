/******************************************************************************
 * Copyright 2009-2021 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.services.json.handlers;

import static java.lang.String.join;
import static java.lang.System.lineSeparator;
import static java.util.Arrays.asList;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.intellij.lang.annotations.Language;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.impl.messages.DefaultMessageFactory;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.MsgMetaData;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.services.http.HTTPClientSettings;
import com.exactpro.sf.services.http.HTTPMessageHelper;
import com.exactpro.sf.services.json.JSONDecoder;
import com.exactpro.sf.services.json.JSONEncoder;
import com.exactpro.sf.services.json.JSONVisitorUtility;
import com.exactpro.sf.services.json.JsonSettings;
import com.exactpro.sf.util.AbstractTest;
import com.exactpro.sf.util.DateTimeUtility;
import com.exactpro.sf.util.NettyTestUtility;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.http.HttpHeaders.Names;

public class TestJSONCodec extends AbstractTest {
    private static final Logger logger = LoggerFactory.getLogger(TestJSONCodec.class);

    private static HTTPClientSettings settings;
    private static IMessageFactory factory;
    private static IDictionaryStructure dictionary;

    @BeforeClass
    public static void init() {
        settings = new HTTPClientSettings();
        factory = DefaultMessageFactory.getFactory();
        dictionary = serviceContext.getDictionaryManager().createMessageDictionary("cfg/dictionaries/http_soap_test.xml");
    }

    @Test
    public void testEncodeDecode() {
        IMessage message = factory.createMessage("TestMessage", "SOAP");
        message.addField("Integer", 777_777);
        message.addField("String", "Seven, Yee!");
        message.addField("Boolean", true);
        message.addField("LocalDateTime", DateTimeUtility.nowLocalDateTime());
        message.addField("IntegerArray", asList(777_777, 666_666));
        message.addField("StringArray", asList("Seven,", " Yee!"));
        message.addField("BooleanArray", asList(true, false));

        IMessage inner = factory.createMessage("TestInnerMessage", "SOAP");
        inner.addField("InnerInteger", 666_777);
        inner.addField("InnerString", "Six, not seven!");
        inner.addField("InnerBoolean", false);
        inner.addField("InnerLocalDateTime", DateTimeUtility.nowLocalDateTime());

        IMessage inner2 = factory.createMessage("TestInnerMessage", "SOAP");
        inner2.addField("InnerInteger", 500);
        inner2.addField("InnerString", "just five");
        inner2.addField("InnerBoolean", true);
        inner2.addField("InnerLocalDateTime", DateTimeUtility.nowLocalDateTime());

        message.addField("Message", inner);
        message.addField("MessageArray", asList(inner, inner2));


        JSONEncoder encoder = new JSONEncoder();
        JSONDecoder decoder = new JSONDecoder();
        encoder.init(settings, factory, dictionary, "TestClient");
        decoder.init(settings, factory, dictionary, "TestClient");

        ChannelHandler encodeSender = new IMessageMessageToByteEncoder();

        try {
            IMessage decoded = NettyTestUtility.encodeDecode(message,
                new ChannelHandler[] { encodeSender, encoder },
                new ChannelHandler[] {  new ByteToMessageHandler(), decoder},
                logger
            );
            decoded.removeField(HTTPMessageHelper.HTTPHEADER);
            AbstractTest.equals(message, decoded);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testDecodePositive() {
        testDecodeMessage("2017-08-30T08:20:12.502Z");
        testDecodeMessage("2017-08-30T08:20:12.502012Z");
        testDecodeMessage("2017-08-30T08:20:12.502012414Z");
    }

    @Test
    public void testDecodeNegative() {
        try {
            testDecodeMessage("2017-08-30T08:20:12.502");
            Assert.fail();
        } catch (DateTimeParseException e) {
            Assert.assertEquals("Text '2017-08-30T08:20:12.502' could not be parsed at index 23", e.getMessage());
        } catch (Exception e) {
            Assert.fail("Must be thrown [java.time.format.DateTimeParseException: Text '2017-08-30T08:20:12.502' could not be parsed at index 23]");
        }
    }

    @Test
    public void TestDateTimeFormatter() {

        String one = "2017-03-11T12:11:10Z";
        String two = "2017-03-11T12:11:10.320Z";
        String three = "2017-03-11T12:11:10.320445Z";
        String four = "2017-03-11T12:11:10.320000Z";
        String five = "2017-03-11T12:11:10.320";

        LocalDateTime.parse(one, JSONVisitorUtility.FORMATTER);
        LocalDateTime.parse(two, JSONVisitorUtility.FORMATTER);
        LocalDateTime.parse(three, JSONVisitorUtility.FORMATTER);
        LocalDateTime.parse(four, JSONVisitorUtility.FORMATTER);
        try {
            LocalDateTime.parse(five, JSONVisitorUtility.FORMATTER);
            Assert.fail("Expected DateTimeParseException exception " +
                    "'Text '2017-03-11T12:11:10.320' could not be parsed at index 23'");
        } catch (DateTimeParseException expected) {}

    }

    @Test
    public void testUnexpectedFieldsInMessage() {
        @Language("JSON")
        String json = "{\n"
                + "  \"unexpectedMessageField\": \"someValue\",\n"
                + "  \"Integer\": 1,\n"
                + "  \"Message\": {\n"
                + "    \"unexpectedFieldInSubMessage\": \"someValue\",\n"
                + "    \"InnerInteger\": 2\n"
                + "  },\n"
                + "  \"MessageArray\": [{\n"
                + "    \"unexpectedFieldInCollection\": \"someValue\",\n"
                + "    \"InnerInteger\": 3\n"
                + "  }]\n"
                + "}";

        IMessage message = decode(json, "TestMessage");
        MsgMetaData metaData = message.getMetaData();

        Assert.assertEquals(1, (int)message.<Integer>getField("Integer"));
        Assert.assertEquals(2, (int)message.<IMessage>getField("Message").<Integer>getField("InnerInteger"));
        Assert.assertEquals(3, (int)message.<List<IMessage>>getField("MessageArray").get(0).<Integer>getField("InnerInteger"));

        Assert.assertTrue(metaData.isRejected());

        Assert.assertEquals(
                join(
                        lineSeparator(),
                        "Unexpected fields in message 'Message': [unexpectedFieldInSubMessage]",
                        "Unexpected fields in message 'MessageArray': [unexpectedFieldInCollection]",
                        "Unexpected fields in message 'TestMessage': [unexpectedMessageField]"
                ),
                metaData.getRejectReason()
        );
    }

    @Test
    public void testEncodeDecodeArrayAsMessage() throws IOException {
        @Language("JSON")
        String json = "[1,[\"String1\"],[[\"String2\"],[\"String3\"]]]";

        IMessage actual = decode(json, "FromArrayMessage");
        IMessage expected = factory.createMessage("FromArrayMessage", "SOAP");

        IMessage subMessage = factory.createMessage("FromArraySubMessage", "SOAP");
        IMessage collectionMessage1 = factory.createMessage("FromArraySubMessage", "SOAP");
        IMessage collectionMessage2 = factory.createMessage("FromArraySubMessage", "SOAP");

        subMessage.addField("String", "String1");
        collectionMessage1.addField("String", "String2");
        collectionMessage2.addField("String", "String3");

        expected.addField("Number", 1);
        expected.addField("Message", subMessage);
        expected.addField("MessageCollection", asList(collectionMessage1, collectionMessage2));

        actual.removeField(HTTPMessageHelper.HTTPHEADER);

        AbstractTest.equals(expected, actual);
        Assert.assertEquals(json, encode(actual));
    }

    @Test
    public void testEncodingDecimalsAsStrings() throws IOException {
        @Language("JSON")
        String json = "{\"BigDecimal\":\"100000\"}";
        IMessage expected = factory.createMessage("TestMessage", "SOAP");
        expected.addField("BigDecimal", new BigDecimal("1E+5"));
        Assert.assertEquals(json, encode(expected, true));
    }

    //region Decoding simple value. TODO: Make a single parameterized test in JUnit 5
    @Test
    public void testDecodingSimpleRootValue() {
        String json = "42";
        IMessage result = decode(json, "SimpleRootValue");
        Assert.assertEquals(42, result.<Object>getField("Simple"));
    }

    @Test
    public void testDecodingSimpleRootValueWithStubAndUri() {
        String json = "42";
        IMessage result = decode(json, "SimpleRootValueWithStubAndUri");
        Assert.assertEquals(42, result.<Object>getField("Simple"));
    }
    //endregion

    //region Encoding simple value. TODO: Make a single parameterized test in JUnit 5
    @Test
    public void testEncodingSimpleRootValue() throws IOException {
        IMessage message = factory.createMessage("SimpleRootValue", "test");
        message.addField("Simple", 42);
        String result = encode(message);
        Assert.assertEquals("Unexpected encoded result", "42", result);
    }

    @Test
    public void testEncodingSimpleRootValueWithStubAndUri() throws IOException {
        IMessage message = factory.createMessage("SimpleRootValueWithStubAndUri", "test");
        message.addField("Simple", 42);
        String result = encode(message);
        Assert.assertEquals("Unexpected encoded result", "42", result);
    }
    //endregion

    private IMessage decode(String json, String messageName) {
        ByteBuf buffer = Unpooled.wrappedBuffer(json.getBytes(StandardCharsets.UTF_8));
        JSONDecoder decoder = new JSONDecoder(true);
        decoder.init(settings, factory, dictionary, "TestClient");
        return NettyTestUtility.decode(buffer, logger, new ByteToMessageHandler(messageName), decoder);
    }

    private String encode(IMessage message) throws IOException {
        return encode(message, false);
    }

    private String encode(IMessage message, boolean treatSimpleValuesAsStrings) throws IOException {
        JSONEncoder encoder = new JSONEncoder(new JsonSettings().setTreatSimpleValuesAsStrings(treatSimpleValuesAsStrings));
        encoder.init(settings, factory, dictionary, "TestClient");
        ByteBuf buf = NettyTestUtility.encode(message, logger, new IMessageMessageToByteEncoder(), encoder);
        return buf.toString(StandardCharsets.UTF_8);
    }

    private void testDecodeMessage(String localDateTime) {
        String message = "{\"Integer\":777777," +
                "\"Boolean\":true," +
                "\"String\":\"Seven, Yee!\"," +
                "\"Message\":{\"InnerInteger\":666777," +
                "\"InnerBoolean\":false," +
                "\"InnerString\":\"Six, not seven!\"," +
                "\"InnerLocalDateTime\":\"{}\"" +
                "}," +
                "\"LocalDateTime\":\"{}\"," +
                "\"IntegerArray\":[777777,666666]," +
                "\"BooleanArray\":[true,false]," +
                "\"StringArray\":[\"Seven,\",\" Yee!\"]," +
                "\"MessageArray\":[{" +
                "\"InnerInteger\":666777," +
                "\"InnerBoolean\":false," +
                "\"InnerString\":\"Six, not seven!\"," +
                "\"InnerLocalDateTime\":\"{}\"" +
                "}]}\n";
        message = message.replaceAll("\\{\\}", localDateTime);
        ByteBuf buffer = Unpooled.copiedBuffer(message.getBytes(StandardCharsets.US_ASCII));

        IMessage expected = factory.createMessage("TestMessage", "SOAP");
        expected.addField("Integer", 777_777);
        expected.addField("String", "Seven, Yee!");
        expected.addField("Boolean", true);
        expected.addField("LocalDateTime", LocalDateTime.parse(localDateTime, JSONVisitorUtility.FORMATTER));
        expected.addField("IntegerArray", asList(777_777, 666_666));
        expected.addField("StringArray", asList("Seven,", " Yee!"));
        expected.addField("BooleanArray", asList(true, false));

        IMessage inner = factory.createMessage("TestInnerMessage", "SOAP");
        inner.addField("InnerInteger", 666_777);
        inner.addField("InnerString", "Six, not seven!");
        inner.addField("InnerBoolean", false);
        inner.addField("InnerLocalDateTime", LocalDateTime.parse(localDateTime, JSONVisitorUtility.FORMATTER));

        expected.addField("Message", inner);
        expected.addField("MessageArray", asList(inner));

        try {
            JSONDecoder decoder = new JSONDecoder();
            decoder.init(settings, factory, dictionary, "TestClient");

            IMessage decoded = NettyTestUtility.decode(buffer, logger, new ByteToMessageHandler(), decoder);
            decoded.removeField(HTTPMessageHelper.HTTPHEADER);

            AbstractTest.equals(expected, decoded);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }
    }

    private static class IMessageMessageToByteEncoder extends MessageToByteEncoder<IMessage> {
        @Override
        protected void encode(ChannelHandlerContext handlerContext, IMessage msg, ByteBuf out) throws Exception {
            out.writeBytes(msg.getMetaData().getRawMessage());
            System.out.println(new String(msg.getMetaData().getRawMessage()));
        }
    }

    private class ByteToMessageHandler extends ByteToMessageDecoder {
        private final String messageName;

        public ByteToMessageHandler() {
            this("TestMessage");
        }

        public ByteToMessageHandler(String messageName) {
            this.messageName = messageName;
        }

        @Override
        protected void decode(ChannelHandlerContext handlerContext, ByteBuf in, List<Object> out) throws Exception {
            IMessage message = factory.createMessage(messageName, "SOAP");
            IMessage header = factory.createMessage(HTTPMessageHelper.HTTPHEADER, "SOAP");
            header.addField(Names.CONTENT_ENCODING, "IDENTITY");
            message.addField(HTTPMessageHelper.HTTPHEADER, header);

            byte[] data = new byte[in.readableBytes()];
            in.readBytes(data);
            message.getMetaData().setRawMessage(data);
            out.add(message);
        }
    }

}
