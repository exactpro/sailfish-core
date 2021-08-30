/*
 * Copyright 2009-2020 Exactpro (Exactpro Systems Limited)
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
 */
package com.exactpro.sf.services.fix;

import static org.junit.Assert.assertArrayEquals;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.DummySession;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.AbstractProtocolDecoderOutput;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.quickfixj.CharsetSupport;

import com.exactpro.sf.actions.DirtyFixUtil;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.MessageUtil;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.loaders.XmlDictionaryStructureLoader;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.factory.FixMessageFactory;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.messages.service.ErrorMessage;
import com.exactpro.sf.services.MessageHelper;
import com.exactpro.sf.services.MockProtocolDecoderOutput;
import com.exactpro.sf.services.tcpip.MessageParseException;
import com.exactpro.sf.services.tcpip.TCPIPSettings;
import com.exactpro.sf.util.AbstractTest;
import com.exactpro.sf.util.DateTimeUtility;
import com.google.common.collect.ImmutableList;

import quickfix.DataDictionary;
import quickfix.FieldNotFound;
import quickfix.InvalidMessage;
import quickfix.Message;
import quickfix.field.converter.UtcDateOnlyConverter;
import quickfix.field.converter.UtcTimeOnlyConverter;
import quickfix.field.converter.UtcTimestampConverter;

public class FixCodecTest extends AbstractTest {

    private final SailfishURI dictionaryURI = SailfishURI.unsafeParse("dictionary");
    private final IoSession session = new DummySession();

    private final IDictionaryStructure dictionary;
    private final IMessageFactory factory = new FixMessageFactory();
    private final MessageHelper helper = new FixMessageHelper();

    {
        try {
            dictionary = loadDictionaryFromResource("dictionary/FIX50.TEST.xml");
            factory.init(dictionaryURI, dictionary);
            helper.init(factory, dictionary);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    public void testDateTimeFieldAccuracy() throws Exception {
        IMessage msg = factory.createMessage("NewOrderSingle");
        //noinspection MagicNumber
        msg.addField("TransactTime", LocalDateTime.of(2021, 8, 27, 21, 2, 34, 123456789));
        msg = helper.prepareMessageToEncode(msg, Collections.emptyMap());

        TCPIPSettings settings = new TCPIPSettings();
        checkEncodeResult(settings, msg, "8=FIXT.1.1\u00019=26\u000135=D\u000160=20210827-21:02:34\u000110=063\u0001");

        settings.setIncludeMilliseconds(true);
        checkEncodeResult(settings, msg, "8=FIXT.1.1\u00019=30\u000135=D\u000160=20210827-21:02:34.123\u000110=254\u0001");

        settings.setIncludeMicroseconds(true);
        checkEncodeResult(settings, msg, "8=FIXT.1.1\u00019=33\u000135=D\u000160=20210827-21:02:34.123456\u000110=160\u0001");

        settings.setIncludeNanoseconds(true);
        checkEncodeResult(settings, msg, "8=FIXT.1.1\u00019=36\u000135=D\u000160=20210827-21:02:34.123456789\u000110=075\u0001");
    }

    private void checkEncodeResult(TCPIPSettings settings, IMessage msg, String expected) throws Exception {
        settings.setVerifyMessageStructure(true);
        settings.setDecodeByDictionary(true);
        settings.setDepersonalizationIncomingMessages(false);

        FIXCodec codec = createCodec(dictionary, settings);
        ProtocolEncoderOutput out = Mockito.mock(ProtocolEncoderOutput.class);
        codec.encode(null, msg, out);

        ArgumentCaptor<IoBuffer> argument = ArgumentCaptor.forClass(IoBuffer.class);
        Mockito.verify(out).write(argument.capture());
        Assert.assertEquals(expected, argument.getValue().getString(StandardCharsets.UTF_8.newDecoder()));
    }

	@Test
	@Ignore //Tests for manual testing
	public void testDecode() throws Exception {

        String s = "8=FIX.4.4|9=391|35=AE|34=7|49=Sender|50=CERT|52=20140320-11:17:28.081|56=Target|57=ECL_SETS|17=PTXKLZMJA8|30=XLON-SETS|31=90|32=100000|55=LU0533033584|60=20140320-11:17:28|64=20140325|75=20140320|487=0|552=2|54=1|37=00I0GfxNvjS3|11=1395314247932|453=1|448=DSG01FIX01|447=D|452=1|581=1|15=GBP|528=A|54=2|37=00I0GfxNvjS4|11=1395314248027|453=1|448=DSG01FIX01|447=D|452=1|581=1|15=GBP|528=A|570=N|571=PTXKLZMJA8|828=0|10=132|".replace("|", "\001");

		System.out.println(s);
		Message message = FixUtil.fromString(s);

		System.out.println(message);
	}


	@Test
	@Ignore //Tests for manual testing
	public void testFromString() throws FieldNotFound, InvalidMessage {

		FixDataDictionaryProvider dictionaryProvider = new FixDataDictionaryProvider(serviceContext.getDictionaryManager(), SailfishURI.unsafeParse("FIX_4_4"));

        String message = "8=FIX.4.4|9=625|35=AE|49=Sender|56=BNP|128=BNP|34=8|52=20140516-10:23:46.667|571=1.835002.1|487=0|856=0|570=N|423=1|55=[N/A]|48=912828VQ0|22=1|454=1|455=US912828VQ01|456=4|460=6|541=20180731|225=20130731|231=1|223=0.01375000|106=United States Treasury Note/Bond|107=T 1 3/8 07/31/18|873=20130731|874=20130731|854=1|235=WORST|236=0.62958095|32=2000000|31=12|75=20140516|60=20140516-08:16:00.300|63=0|64=20140521|573=1|552=1|54=1|37=1.835002.1|453=5|448=PartyID1|447=D|452=1|448=PartyID2|447=D|452=12|448=PartyID3|447=D|452=17|448=okelley|447=D|452=26|448=PartyID4|447=D|452=4|15=USD|12=13|13=3|479=USD|381=240000.00|159=8356.35|118=248356.35|10=246|".replace("|", "\001");

		DataDictionary dd = dictionaryProvider.getApplicationDataDictionary(null);
		Message msg = new Message();
		msg.fromString(message, dd, true);

		System.out.println(msg.toString().equals(message));
		System.out.println(message);
		System.out.println(msg);
	}

    @Test(expected = IllegalArgumentException.class)
    public void testVerifyMessageStructureSetting1() throws Exception {
        TCPIPSettings settings = new TCPIPSettings();
        settings.setVerifyMessageStructure(true);
        createCodec(dictionary, settings);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testVerifyMessageStructureSetting2() throws Exception {
        TCPIPSettings settings = new TCPIPSettings();
        settings.setVerifyMessageStructure(true);
        settings.setDecodeByDictionary(true);
        createCodec(dictionary, settings);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testVerifyMessageStructureSetting3() throws Exception {
        TCPIPSettings settings = new TCPIPSettings();
        settings.setVerifyMessageStructure(true);
        settings.setDepersonalizationIncomingMessages(false);
        createCodec(dictionary, settings);
    }

    @Test
    public void testVerifyMessageStructure() throws Exception {
        TCPIPSettings settings = new TCPIPSettings();
        settings.setVerifyMessageStructure(true);
        settings.setDecodeByDictionary(true);
        settings.setDepersonalizationIncomingMessages(false);
        FIXCodec codec = createCodec(dictionary, settings);

        IoBuffer buffer = IoBuffer.wrap("8=FIXT.1.1\0019=77\00135=Z\00134=1152\00149=FIX_CSV_ds1\00152=20151005-15:47:02.785\00156=FGW\00199999=fake\001298=4\00110=115\001".getBytes());
        AbstractProtocolDecoderOutput outputDec = new MockProtocolDecoderOutput();
        codec.decode(session, buffer, outputDec);

        IMessage message = (IMessage)outputDec.getMessageQueue().poll();
        Assert.assertEquals(ErrorMessage.MESSAGE_NAME, message.getName());
        Assert.assertEquals("Unknown tag: 99999, verify tags enabled", new ErrorMessage(message).getCause());
    }

    @Test
    public void testXmlSubMessage() throws Exception {
        IDictionaryStructure dictionary = loadDictionaryFromResource("dictionary/FIX50.XML_MESSAGE.xml");

        TCPIPSettings settings = new TCPIPSettings();
        settings.setDecodeByDictionary(false);

        FIXCodec codec = createCodec(dictionary, settings);
        AbstractProtocolDecoderOutput outputDec = new MockProtocolDecoderOutput();

        @SuppressWarnings("ConfusingOctalEscapeSequence")
        String message =
                "8=FIXT.1.1\0019=1232\00135=777\00149=SEN\00156=TARGET\00134=2\00152=20121212-00:00:00\001" +
                        "11111=777.888999\001" +
                        "22222=StringField\001" +
                        "33333=333\001" +
                        "44444=-444.555666\001" +
                        "55555=Y\001" +
                        "66666=20170428\001" +
                        "77777=14:15:31.766\001" +
                        "88888=20170428-14:15:31.766\001" +
                        "99999=C\001" +
                        "7777=2\001" +                          // group
                        "7778=Tom\001" +
                        "7779=1\001" +
                        "7780=20001515-02:02:02.222\001" +
                        "7778=Jerry\001" +
                        "7779=2\001" +
                        "7780=20001515-03:03:03.333\001" +
                        "8888=CompString\001" +                 // component
                        "8889=-5\001" +
                        "8890=20001515-04:04:04.444\001" +
                        "100001=<xml_context>\012" +            // xml message
                        "    <udf name = \"String Field\" entity = \"SOME~ENTITY\" type = \"STRING\" value = \"It's work\" />\012" +
                        "    <udf name = \"Integer Field\" entity = \"SOME~ENTITY\" type = \"INTEGER\" value = \"13\" />\012" +
                        "    <udf name = \"Double Field\" entity = \"SOME~ENTITY\" type = \"DOUBLE\" value = \"444.555666\" />\012" +
                        "    <udf name = \"Big Decimal Field\" entity = \"SOME~ENTITY\" type = \"DOUBLE\" value = \"111.222333\" />\012" +
                        "    <udf name = \"Boolean Field\" entity = \"SOME~ENTITY\" type = \"DOUBLE\" value = \"Y\" />\012" +
                        "    <udf name = \"Character Field\" entity = \"SOME~ENTITY\" type = \"DOUBLE\" value = \"C\" />\012" +
                        "    <udf name = \"Local Date Field\" entity = \"SOME~ENTITY\" type = \"DOUBLE\" value = \"20170428\" />\012" +
                        "    <udf name = \"Local Time Field\" entity = \"SOME~ENTITY\" type = \"DOUBLE\" value = \"14:15:31.766\" />\012" +
                        "    <udf name = \"Local Date Time Field\" entity = \"SOME~ENTITY\" type = \"DOUBLE\" value = \"20170428-14:15:31.766\" />\012" +
                        "</xml_context>\012\001" +
                        "10=170\001";
        IoBuffer buffer = IoBuffer.wrap(message.getBytes());

        codec.decode(session, buffer, outputDec);

        IMessage decodedMessage = (IMessage) outputDec.getMessageQueue().poll();

        // check fields without by dictionary decoding
        Assert.assertEquals("FIXT.1.1", decodedMessage.getField("BeginString"));
        Assert.assertEquals("1232", decodedMessage.getField("BodyLength"));
        Assert.assertEquals("777", decodedMessage.getField("MsgType"));
        Assert.assertEquals("SEN", decodedMessage.getField("SenderCompID"));
        Assert.assertEquals("TARGET", decodedMessage.getField("TargetCompID"));
        Assert.assertEquals("2", decodedMessage.getField("MsgSeqNum"));
        Assert.assertEquals("20121212-00:00:00", decodedMessage.getField("SendingTime"));
        Assert.assertEquals("333", decodedMessage.getField("IntegerField"));
        Assert.assertEquals("-444.555666", decodedMessage.getField("DoubleField"));
        Assert.assertEquals("777.888999", decodedMessage.getField("BigDecimalField"));
        Assert.assertEquals("Y", decodedMessage.getField("BooleanField"));
        Assert.assertEquals("C", decodedMessage.getField("CharacterField"));
        Assert.assertEquals("20170428", decodedMessage.getField("LocalDateField"));
        Assert.assertEquals("14:15:31.766", decodedMessage.getField("LocalTimeField"));
        Assert.assertEquals("20170428-14:15:31.766", decodedMessage.getField("LocalDateTimeField"));
        Assert.assertEquals("170", decodedMessage.getField("CheckSum"));

        // check group
        Assert.assertEquals("2", decodedMessage.getField("NoRepGroups"));
        Assert.assertEquals("Jerry", decodedMessage.getField("StringGroup"));   // last value expected
        Assert.assertEquals("2", decodedMessage.getField("IntegerGroup"));      // last value expected
        Assert.assertEquals("20001515-03:03:03.333",
                            decodedMessage.getField("LocalDateTimeGroup"));              // last value expected

        // check component
        Assert.assertEquals("CompString", decodedMessage.getField("StringComp"));
        Assert.assertEquals("-5", decodedMessage.getField("IntegerComp"));
        Assert.assertEquals("20001515-04:04:04.444", decodedMessage.getField("LocalDateTimeComp"));

        // check xml sub message without by dictionary decoding
        IMessage xmlSubMessage = (IMessage)decodedMessage.getField("XmlSubMessage");

        Assert.assertEquals("It's work", xmlSubMessage.getField("StringField"));
        Assert.assertEquals("13", xmlSubMessage.getField("IntegerField"));
        Assert.assertEquals("444.555666", xmlSubMessage.getField("DoubleField"));
        Assert.assertEquals("111.222333", xmlSubMessage.getField("BigDecimalField"));
        Assert.assertEquals("Y", xmlSubMessage.getField("BooleanField"));
        Assert.assertEquals("C", xmlSubMessage.getField("CharacterField"));
        Assert.assertEquals("20170428", xmlSubMessage.getField("LocalDateField"));
        Assert.assertEquals("14:15:31.766", xmlSubMessage.getField("LocalTimeField"));
        Assert.assertEquals("20170428-14:15:31.766", xmlSubMessage.getField("LocalDateTimeField"));

        settings.setDecodeByDictionary(true);
        settings.setDepersonalizationIncomingMessages(false);

        codec = createCodec(dictionary, settings);

        outputDec = new MockProtocolDecoderOutput();
        buffer = IoBuffer.wrap(message.getBytes());

        codec.decode(session, buffer, outputDec);

        decodedMessage = (IMessage) outputDec.getMessageQueue().poll();

        // check fields with by dictionary decoding
        Assert.assertEquals("StringField", decodedMessage.getField("StringField"));
        Assert.assertEquals((Integer)333, decodedMessage.getField("IntegerField"));
        Assert.assertEquals((Double)(-444.555666), decodedMessage.getField("DoubleField"));
        Assert.assertEquals(new BigDecimal("777.888999"), decodedMessage.getField("BigDecimalField"));
        Assert.assertEquals((Boolean)true, decodedMessage.getField("BooleanField"));
        Assert.assertEquals((Character)'C', decodedMessage.getField("CharacterField"));

        LocalDate date = DateTimeUtility.toLocalDate(UtcDateOnlyConverter.convert("20170428"));
        Assert.assertEquals(date, decodedMessage.getField("LocalDateField"));

        LocalTime time = DateTimeUtility.toLocalTime(UtcTimeOnlyConverter.convert("14:15:31.766"));
        Assert.assertEquals(time, decodedMessage.getField("LocalTimeField"));

        LocalDateTime dateTime = DateTimeUtility.toLocalDateTime(
                UtcTimestampConverter.convert("20170428-14:15:31.766"));
        Assert.assertEquals(dateTime, decodedMessage.getField("LocalDateTimeField"));

        //header
        IMessage header = decodedMessage.getField("header");
        Assert.assertEquals("FIXT.1.1", header.getField("BeginString"));
        Assert.assertEquals((Integer)1232, header.getField("BodyLength"));
        Assert.assertEquals("777", header.getField("MsgType"));
        Assert.assertEquals("SEN", header.getField("SenderCompID"));
        Assert.assertEquals("TARGET", header.getField("TargetCompID"));
        Assert.assertEquals((Integer)2, header.getField("MsgSeqNum"));
        Assert.assertEquals(DateTimeUtility.toLocalDateTime(UtcTimestampConverter.convert("20121212-00:00:00")),
                header.getField("SendingTime"));

        // trailer
        IMessage trailer = (IMessage) decodedMessage.getField("trailer");
        Assert.assertEquals("170", trailer.getField("CheckSum"));

        // check group
        IMessage groupContainer = (IMessage)decodedMessage.getField("RepGroup");
        List<IMessage> group = groupContainer.<List<IMessage>>getField("NoRepGroup");
        Assert.assertEquals(2, group.size());

        IMessage groupMessage = group.get(0);
        Assert.assertEquals("Tom", groupMessage.getField("StringGroup"));
        Assert.assertEquals((Integer)1, groupMessage.getField("IntegerGroup"));
        Assert.assertEquals(DateTimeUtility.toLocalDateTime(
                UtcTimestampConverter.convert("20001515-02:02:02.222")),
                groupMessage.getField("LocalDateTimeGroup"));
        groupMessage = group.get(1);
        Assert.assertEquals("Jerry", groupMessage.getField("StringGroup"));
        Assert.assertEquals((Integer)2, groupMessage.getField("IntegerGroup"));
        Assert.assertEquals(DateTimeUtility.toLocalDateTime(
                UtcTimestampConverter.convert("20001515-03:03:03.333")),
                groupMessage.getField("LocalDateTimeGroup"));

        // check component
        IMessage component = (IMessage)decodedMessage.getField("SomeComp");
        Assert.assertEquals("CompString", component.getField("StringComp"));
        //noinspection UnnecessaryParentheses
        Assert.assertEquals((Integer)(-5), component.getField("IntegerComp"));
        Assert.assertEquals(DateTimeUtility.toLocalDateTime(
                UtcTimestampConverter.convert("20001515-04:04:04.444")),
                component.getField("LocalDateTimeComp"));

        // check xml sub message with by dictionary decoding
        xmlSubMessage = (IMessage)decodedMessage.getField("XmlSubMessage");

        Assert.assertEquals("It's work", xmlSubMessage.getField("StringField"));
        Assert.assertEquals((Integer)13, xmlSubMessage.getField("IntegerField"));
        Assert.assertEquals((Double)444.555666, xmlSubMessage.getField("DoubleField"));
        Assert.assertEquals(new BigDecimal("111.222333"), xmlSubMessage.getField("BigDecimalField"));
        Assert.assertEquals((Boolean)true, xmlSubMessage.getField("BooleanField"));
        Assert.assertEquals((Character)'C', xmlSubMessage.getField("CharacterField"));

        date = DateTimeUtility.toLocalDate(UtcDateOnlyConverter.convert("20170428"));
        Assert.assertEquals(date, xmlSubMessage.getField("LocalDateField"));

        time = DateTimeUtility.toLocalTime(UtcTimeOnlyConverter.convert("14:15:31.766"));
        Assert.assertEquals(time, xmlSubMessage.getField("LocalTimeField"));

        dateTime = DateTimeUtility.toLocalDateTime(UtcTimestampConverter.convert("20170428-14:15:31.766"));
        Assert.assertEquals(dateTime, xmlSubMessage.getField("LocalDateTimeField"));

        // with depersonalization
        settings.setDecodeByDictionary(true);
        settings.setDepersonalizationIncomingMessages(true);

        codec = createCodec(dictionary, settings);

        outputDec = new MockProtocolDecoderOutput();
        buffer = IoBuffer.wrap(message.getBytes());

        codec.decode(session, buffer, outputDec);

        decodedMessage = (IMessage) outputDec.getMessageQueue().poll();

        // check fields with by dictionary decoding with depersonalization
        Assert.assertEquals("StringField", decodedMessage.getField("StringField"));
        Assert.assertEquals("333", decodedMessage.getField("IntegerField"));
        Assert.assertEquals("-444.555666", decodedMessage.getField("DoubleField"));
        Assert.assertEquals("777.888999", decodedMessage.getField("BigDecimalField"));
        Assert.assertEquals("Y", decodedMessage.getField("BooleanField"));
        Assert.assertEquals("C", decodedMessage.getField("CharacterField"));
        Assert.assertEquals("20170428", decodedMessage.getField("LocalDateField"));
        Assert.assertEquals("14:15:31.766", decodedMessage.getField("LocalTimeField"));
        Assert.assertEquals("20170428-14:15:31.766", decodedMessage.getField("LocalDateTimeField"));

        // header with depersonalization
        header = (IMessage) decodedMessage.getField("header");
        Assert.assertEquals("FIXT.1.1", header.getField("BeginString"));
        Assert.assertEquals("1232", header.getField("BodyLength"));
        Assert.assertEquals("777", header.getField("MsgType"));
        Assert.assertEquals("SEN", header.getField("SenderCompID"));
        Assert.assertEquals("TARGET", header.getField("TargetCompID"));
        Assert.assertEquals("2", header.getField("MsgSeqNum"));
        Assert.assertEquals("20121212-00:00:00", header.getField("SendingTime"));

        // trailer with depersonalization
        trailer = (IMessage) decodedMessage.getField("trailer");
        Assert.assertEquals("170", trailer.getField("CheckSum"));

        // check group with depersonalization
        List<IMessage> repGroup = decodedMessage.<List<IMessage>>getField("RepGroup");
        group = repGroup.get(0).<List<IMessage>>getField("NoRepGroup");
        Assert.assertEquals(2, group.size());

        groupMessage = group.get(0);
        Assert.assertEquals("Tom", groupMessage.getField("StringGroup"));
        Assert.assertEquals("1", groupMessage.getField("IntegerGroup"));
        Assert.assertEquals("20001515-02:02:02.222", groupMessage.getField("LocalDateTimeGroup"));
        groupMessage = group.get(1);
        Assert.assertEquals("Jerry", groupMessage.getField("StringGroup"));
        Assert.assertEquals("2", groupMessage.getField("IntegerGroup"));
        Assert.assertEquals("20001515-03:03:03.333", groupMessage.getField("LocalDateTimeGroup"));

        // check component with depersonalization
        List<IMessage> someComp = decodedMessage.<List<IMessage>>getField("SomeComp");
        Assert.assertEquals("CompString", someComp.get(0).getField("StringComp"));
        Assert.assertEquals("-5", someComp.get(0).getField("IntegerComp"));
        Assert.assertEquals("20001515-04:04:04.444", someComp.get(0).getField("LocalDateTimeComp"));

        // check xml sub message with by dictionary decoding with depersonalization
        xmlSubMessage = (IMessage)decodedMessage.getField("XmlSubMessage");

        Assert.assertEquals("It's work", xmlSubMessage.getField("StringField"));
        Assert.assertEquals("13", xmlSubMessage.getField("IntegerField"));
        Assert.assertEquals("444.555666", xmlSubMessage.getField("DoubleField"));
        Assert.assertEquals("111.222333", xmlSubMessage.getField("BigDecimalField"));
        Assert.assertEquals("Y", xmlSubMessage.getField("BooleanField"));
        Assert.assertEquals("C", xmlSubMessage.getField("CharacterField"));
        Assert.assertEquals("20170428", xmlSubMessage.getField("LocalDateField"));
        Assert.assertEquals("14:15:31.766", xmlSubMessage.getField("LocalTimeField"));
        Assert.assertEquals("20170428-14:15:31.766", xmlSubMessage.getField("LocalDateTimeField"));
    }

    @Test
    public void testAlternativeSeparator() throws Exception {
        TCPIPSettings settings = new TCPIPSettings();
        settings.setDecodeByDictionary(true);
        settings.setFieldSeparator("|");

        FIXCodec codec = createCodec(dictionary, settings);
        IMessage expectedMessage = getExpectedMessage();

        // | in the message end
        decodeAndCheckMessage(codec, "8=FIXT.1.1|9=155|35=Z|34=1152|49=FIX_CSV_ds1|52=20151005-15:47:02.785" +
                "|56=FGW|298=4|1166=1444060022986|295=1|299=test|48=7219943|22=8|1461=1|1462=FIX_CSV_ds1" +
                "|1463=D|1464=76|10=169|", expectedMessage);

        // | in the field value
        expectedMessage.addField("QuoteMsgID", "14440600|22986");
        ((IMessage)expectedMessage.getField("header")).addField("BodyLength", "156");
        ((IMessage)expectedMessage.getField("trailer")).addField("CheckSum", "38");
        decodeAndCheckMessage(codec, "8=FIXT.1.1|9=156|35=Z|34=1152|49=FIX_CSV_ds1|52=20151005-15:47:02.785" +
                "|56=FGW|298=4|1166=14440600|22986|295=1|299=test|48=7219943|22=8|1461=1|1462=FIX_CSV_ds1" +
                "|1463=D|1464=76|10=38|", expectedMessage);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testSendMessage() throws Exception {

        Map<String, Object> map = new HashMap<>();
        Map<String, Object> subMap1 = new HashMap<>();
        Map<String, Object> subMap2 = new HashMap<>();

        subMap1.put("NetChgPrevDay", "2163");
        subMap1.put("TestMessageIndicator", "MDRT006");
        subMap1.put("GroupDelimiter", "451");

        subMap2.put("451", "2163");
        subMap2.put("2711", "MDRT006");
        subMap2.put("GroupDelimiter", "451");

        List<Object> list = new ArrayList<>();

        list.add(subMap1);
        list.add(subMap2);

        map.put("2306", "6");
        map.put("BeginString", "FIXT.1.1");
        map.put("4220", list);
        map.put("2620", "modintsov");
        map.put("2047", "test");
        map.put("2010", "1");
        map.put("MsgType", "2161");
        map.put("3182", "225");
        map.put("9", "001");
        map.put("10", "2");

        FIXCodec codec = createCodec(dictionary, factory, new TCPIPSettings());

        IMessage iMessage = MessageUtil.convertToIMessage(map, factory, "namespace", "name");
        ProtocolEncoderOutput out = Mockito.mock(ProtocolEncoderOutput.class);
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {

                Assert.assertEquals(
                        //"8=FIXT.1.19=13935=216134=27582=CheckSum2010=12047=test2306=62620=modintsov3182=2254220=2451=2163464=MDRT006451=21632711=MDRT0067583=RefTagID10=151",
                        "8=FIXT.1.19=00135=21612010=12047=test2306=62620=modintsov3182=2254220=2451=2163464=MDRT006451=21632711=MDRT00610=2",
                        invocation.<IoBuffer>getArgument(0).getString(Charset.forName("UTF-8").newDecoder()));
                return null;
            }
        } ).when(out).write(Mockito.anyObject());

        codec.encode(null, iMessage, out);
        Assert.assertTrue(subMap1.containsKey("GroupDelimiter") && subMap2.containsKey("GroupDelimiter"));


        map.put(DirtyFixUtil.DIRTY_BEGIN_STRING, "FIXT.8.8");
        map.put(DirtyFixUtil.DIRTY_CHECK_SUM, "999");
        map.put(DirtyFixUtil.DIRTY_BODY_LENGTH, "777");

        map.put(DirtyFixUtil.DOUBLE_TAG, "2010=2;2010=3");
        //TODO: Implement DUPLICATE_TAG for subMessage
        //subMap2.put(DirtyFixUtil.DUPLICATE_TAG, "2711=ABC;2711=DEF");

        iMessage = MessageUtil.convertToIMessage(map, factory, "namespace", "name");
        out = Mockito.mock(ProtocolEncoderOutput.class);
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {

                Assert.assertEquals(
                        //"8=FIXT.8.89=77735=216134=27582=CheckSum2010=12047=test2306=62620=modintsov3182=2254220=2451=2163464=MDRT006451=21632711=MDRT0067583=RefTagID2010=22010=310=999",
                        "8=FIXT.8.89=77735=21612010=12010=22010=32047=test2306=62620=modintsov3182=2254220=2451=2163464=MDRT006451=21632711=MDRT00610=999",
                        invocation.<IoBuffer>getArgument(0).getString(Charset.forName("UTF-8").newDecoder()));
                return null;
            }
        } ).when(out).write(Mockito.anyObject());

        codec.encode(null, iMessage, out);
    }

	@Test
	public void testSingleValid() throws MessageParseException {
	    String input = "8=FIX.4.49=14535=D34=7149=ez_mbp352=20151013-12:32:45.516.56=PARFX11=144473956586638=10000000040=144=0.0154=1.55=USD/MXN59=060=20151013-12:32:45.51310=150";
	    IoBuffer buffer = IoBuffer.wrap(input.getBytes());
	    List<String> messages = new ArrayList<>();
	    String output = null;

	    while((output = FIXCodec.getFixString(buffer)) != null) {
	        messages.add(output);
	    }

	    Assert.assertEquals(1, messages.size());
	    Assert.assertEquals(input, messages.get(0));
	}

	@Test
	public void testMultipleValid() throws MessageParseException {
	    String message1 = "8=FIX.4.49=14535=D34=7149=ez_mbp352=20151013-12:32:45.516.56=PARFX11=144473956586638=10000000040=144=0.0154=1.55=USD/MXN59=060=20151013-12:32:45.51310=150";
	    String message2 = "8=FIX.4.49=14535=D34=7249=ez_mbp352=20151013-12:32:45.642.56=PARFX11=144473956599238=10000000040=144=0.0154=2.55=USD/MXN59=060=20151013-12:32:45.63810=160";
	    String input = message1 + message2;
	    IoBuffer buffer = IoBuffer.wrap(input.getBytes());
	    List<String> messages = new ArrayList<>();
	    String output = null;

	    while((output = FIXCodec.getFixString(buffer)) != null) {
            messages.add(output);
        }

	    Assert.assertEquals(2, messages.size());
        Assert.assertEquals(message1, messages.get(0));
        Assert.assertEquals(message2, messages.get(1));
	}

	@Test
	public void testValidEmptyInput() throws MessageParseException {
	    IoBuffer buffer = IoBuffer.wrap(new byte[0]);
	    List<String> messages = new ArrayList<>();
        String output = null;

        while((output = FIXCodec.getFixString(buffer)) != null) {
            messages.add(output);
        }

        Assert.assertEquals(0, messages.size());
	}

	@Test
	public void testValidNonEmptyNoMessage() throws MessageParseException {
	    IoBuffer buffer = IoBuffer.wrap("SoylentGreenIsPeople".getBytes());
	    List<String> messages = new ArrayList<>();
        String output = null;

        while((output = FIXCodec.getFixString(buffer)) != null) {
            messages.add(output);
        }

        Assert.assertEquals(0, messages.size());
	}

	@Test
	public void testInvalidBeginStringOffset() {
	    String input = "zzz8=FIX.4.49=14535=D34=7149=ez_mbp352=20151013-12:32:45.516.56=PARFX11=144473956586638=10000000040=144=0.0154=1.55=USD/MXN59=060=20151013-12:32:45.51310=150";
	    IoBuffer buffer = IoBuffer.wrap(input.getBytes());
        List<String> messages = new ArrayList<>();
        String output = null;

        try {
            while((output = FIXCodec.getFixString(buffer)) != null) {
                messages.add(output);
            }
        } catch(MessageParseException e) {
            Assert.assertEquals("BeginString index is higher than 0", e.getMessage());
            Assert.assertEquals(input, e.getRawMessage());
            Assert.assertEquals(input.indexOf("8=FIX"), buffer.position());
        }

        Assert.assertEquals(0, messages.size());
	}

	@Test
	public void testInvalidCheckSum() throws MessageParseException {
	    String message1 = "8=FIX.4.49=14535=D34=7149=ez_mbp352=20151013-12:32:45.516.56=PARFX11=144473956586638=10000000040=144=0.0154=1.55=USD/MXN59=060=20151013-12:32:45.513";
        String message2 = "8=FIX.4.49=14535=D34=7249=ez_mbp352=20151013-12:32:45.642.56=PARFX11=144473956599238=10000000040=144=0.0154=2.55=USD/MXN59=060=20151013-12:32:45.63810=160";
        String input = message1 + message2;
        IoBuffer buffer = IoBuffer.wrap(input.getBytes());
        List<String> messages = new ArrayList<>();
        String output = null;

        try {
            while((output = FIXCodec.getFixString(buffer)) != null) {
                messages.add(output);
            }
        } catch(MessageParseException e) {
            Assert.assertEquals("CheckSum is absent or invalid", e.getMessage());
            Assert.assertEquals(message1, e.getRawMessage());
            Assert.assertEquals(input.indexOf("8=FIX", 1), buffer.position());

            while((output = FIXCodec.getFixString(buffer)) != null) {
                messages.add(output);
            }
        }

        Assert.assertEquals(1, messages.size());
        Assert.assertEquals(message2, messages.get(0));
	}

	@Test
	public void testInvalidBodyLength() throws MessageParseException {
	    String message1 = "8=FIX.4.435=D34=7149=ez_mbp352=20151013-12:32:45.516.56=PARFX11=144473956586638=10000000040=144=0.0154=1.55=USD/MXN59=060=20151013-12:32:45.51310=150";
        String message2 = "8=FIX.4.49=14535=D34=7249=ez_mbp352=20151013-12:32:45.642.56=PARFX11=144473956599238=10000000040=144=0.0154=2.55=USD/MXN59=060=20151013-12:32:45.63810=160";
        String input = message1 + message2;
        IoBuffer buffer = IoBuffer.wrap(input.getBytes());
        List<String> messages = new ArrayList<>();
        String output = null;

        try {
            while((output = FIXCodec.getFixString(buffer)) != null) {
                messages.add(output);
            }
        } catch(MessageParseException e) {
            Assert.assertEquals("BodyLength is absent or not a second tag", e.getMessage());
            Assert.assertEquals(message1, e.getRawMessage());
            Assert.assertEquals(input.indexOf("8=FIX") + 1, buffer.position());

            while((output = FIXCodec.getFixString(buffer)) != null) {
                messages.add(output);
            }
        }

        Assert.assertEquals(1, messages.size());
        Assert.assertEquals(message2, messages.get(0));
	}

	@Test
	public void testInvalidBodyLengthValue() throws MessageParseException {
	    String message1 = "8=FIX.4.49=15035=D34=7149=ez_mbp352=20151013-12:32:45.516.56=PARFX11=144473956586638=10000000040=144=0.0154=1.55=USD/MXN59=060=20151013-12:32:45.51310=150";
        String message2 = "8=FIX.4.49=14535=D34=7249=ez_mbp352=20151013-12:32:45.642.56=PARFX11=144473956599238=10000000040=144=0.0154=2.55=USD/MXN59=060=20151013-12:32:45.63810=160";
        String input = message1 + message2;
        IoBuffer buffer = IoBuffer.wrap(input.getBytes());
        List<String> messages = new ArrayList<>();
        String output = null;

        try {
            while((output = FIXCodec.getFixString(buffer)) != null) {
                messages.add(output);
            }
        } catch(MessageParseException e) {
            Assert.assertEquals("BodyLength value is invalid", e.getMessage());
            Assert.assertEquals(message1, e.getRawMessage());
            Assert.assertEquals(input.indexOf("8=FIX") + 1, buffer.position());

            while((output = FIXCodec.getFixString(buffer)) != null) {
                messages.add(output);
            }
        }

        Assert.assertEquals(1, messages.size());
        Assert.assertEquals(message2, messages.get(0));
	}

	@Test
	public void testInvalidGarbageBetweenMessages() throws MessageParseException {
	    String message1 = "8=FIX.4.49=14535=D34=7149=ez_mbp352=20151013-12:32:45.516.56=PARFX11=144473956586638=10000000040=144=0.0154=1.55=USD/MXN59=060=20151013-12:32:45.51310=150";
        String message2 = "8=FIX.4.49=14535=D34=7249=ez_mbp352=20151013-12:32:45.642.56=PARFX11=144473956599238=10000000040=144=0.0154=2.55=USD/MXN59=060=20151013-12:32:45.63810=160";
        String input = message1 + "876" + message2;
        IoBuffer buffer = IoBuffer.wrap(input.getBytes());
        List<String> messages = new ArrayList<>();
        String output = null;

        try {
            while((output = FIXCodec.getFixString(buffer)) != null) {
                messages.add(output);
            }
        } catch(MessageParseException e) {
            Assert.assertEquals("BeginString index is higher than 0", e.getMessage());
            Assert.assertEquals("876" + message2, e.getRawMessage());
            Assert.assertEquals(input.indexOf("8768=FIX") + 3, buffer.position());

            while((output = FIXCodec.getFixString(buffer)) != null) {
                messages.add(output);
            }
        }

        Assert.assertEquals(2, messages.size());
        Assert.assertEquals(message1, messages.get(0));
        Assert.assertEquals(message2, messages.get(1));
	}


    @Test
    public void testMessageFilter() throws Exception {
        StringBuilder builder = new StringBuilder();
        builder.append("8=FIX.4.49=14635=D34=71805=ez_mbp352=20151013-12:32:45.516.56=PARFX11=144473956586638=10000000040=144=0.0154=1.55=USD/MXN59=060=20151013-12:32:45.51310=150");
        builder.append("8=FIX.4.49=14635=D34=72805=ez_mbp352=20151013-12:32:45.516.56=PARFX11=144473956586638=10000000040=144=0.0154=1.55=USD/MXN59=060=20151013-12:32:45.51310=150");
        builder.append("8=FIX.4.49=14735=D34=72 805=ez_mbp352=20151013-12:32:45.516.56=PARFX11=144473956586638=10000000040=144=0.0154=1.55=USD/MXN59=060=20151013-12:32:45.51310=150");
        builder.append("8=FIX.4.49=14635=D34=74805=ez_mbp352=20151013-12:32:45.516.56=PARFX11=144473956586638=10000000040=144=0.0154=1.55=USD/MXN59=060=20151013-12:32:45.51310=150");

        TCPIPSettings settings = new TCPIPSettings();
        settings.setDepersonalizationIncomingMessages(true);
        settings.setDecodeByDictionary(false);
        settings.setFilterMessages("35:D; 34:71; 805:ez_mbp3");

        FIXCodec codec = createCodec(dictionary, settings);

        AbstractProtocolDecoderOutput outputDec = new MockProtocolDecoderOutput();

        IoBuffer buffer = IoBuffer.wrap(builder.toString().getBytes());
        codec.decode(session, buffer, outputDec);

        Assert.assertEquals(1, outputDec.getMessageQueue().size());
        IMessage message = (IMessage) outputDec.getMessageQueue().poll();
        Assert.assertEquals("D", message.getField("MsgType"));
        Assert.assertEquals("71", message.getField("MsgSeqNum"));

        // check without space in value
        settings.setFilterMessages("35:D; 34:71,72");
        codec = createCodec(dictionary, settings);
        outputDec = new MockProtocolDecoderOutput();
        buffer = IoBuffer.wrap(builder.toString().getBytes());
        codec.decode(session, buffer, outputDec);

        Assert.assertEquals(2, outputDec.getMessageQueue().size());
        message = (IMessage) outputDec.getMessageQueue().poll();
        Assert.assertEquals("D", message.getField("MsgType"));
        Assert.assertEquals("71", message.getField("MsgSeqNum"));

        message = (IMessage) outputDec.getMessageQueue().poll();
        Assert.assertEquals("D", message.getField("MsgType"));
        Assert.assertEquals("72", message.getField("MsgSeqNum"));

        // check with space in value
        settings.setFilterMessages("35:D; 34:71,72 ");
        codec = createCodec(dictionary, settings);
        outputDec = new MockProtocolDecoderOutput();
        buffer = IoBuffer.wrap(builder.toString().getBytes());
        codec.decode(session, buffer, outputDec);

        Assert.assertEquals(2, outputDec.getMessageQueue().size());
        message = (IMessage) outputDec.getMessageQueue().poll();
        Assert.assertEquals("D", message.getField("MsgType"));
        Assert.assertEquals("71", message.getField("MsgSeqNum"));

        message = (IMessage) outputDec.getMessageQueue().poll();
        Assert.assertEquals("D", message.getField("MsgType"));
        Assert.assertEquals("72 ", message.getField("MsgSeqNum"));
    }

    @Test
    public void testMessageFilterNegative() throws Exception {
        TCPIPSettings settings = new TCPIPSettings();
        FIXCodec codec = new FIXCodec();

        // no tag
        settings.setFilterMessages(":D; 34:71");
        try {
            codec = createCodec(dictionary, settings);
            Assert.fail("Must throw EPSCommonException with message " +
                    "[Invalid filter [:D; 34:71]. Must have format 'tag:value' delimited by ';']");
        } catch (EPSCommonException e) {
            Assert.assertEquals("Invalid filter [:D; 34:71]. Must have format 'tag:value' delimited by ';'", e.getMessage());
        }

        // no value
        settings.setFilterMessages("35:; 34:71");
        try {
            codec = createCodec(dictionary, settings);
            Assert.fail("Must throw EPSCommonException with message " +
                    "[Invalid filter [35:; 34:71]. Must have format 'tag:value' delimited by ';']");
        } catch (EPSCommonException e) {
            Assert.assertEquals("Invalid filter [35:; 34:71]. Must have format 'tag:value' delimited by ';'", e.getMessage());
        }

        // empty tag
        settings.setFilterMessages(" :D; 34:71");
        try {
            codec = createCodec(dictionary, settings);
            Assert.fail("Must throw EPSCommonException with message [Invalid filter [ :D; 34:71]. Tag is empty");
        } catch (EPSCommonException e) {
            Assert.assertEquals("Invalid filter [ :D; 34:71]. Tag is empty", e.getMessage());
        }
    }

    private void decodeAndCheckMessage(FIXCodec codec, String message, IMessage expectedMessage) throws Exception {
        IoSession session = new DummySession();
        AbstractProtocolDecoderOutput outputDec = new MockProtocolDecoderOutput();
        IoBuffer buffer = IoBuffer.wrap(message.getBytes(CharsetSupport.getCharsetInstance()));
        codec.decode(session, buffer, outputDec);
        Assert.assertEquals(outputDec.getMessageQueue().size(), 1);
        Object decoded = outputDec.getMessageQueue().poll();
        Assert.assertTrue(decoded instanceof IMessage);
        AbstractTest.equals(expectedMessage, (IMessage)decoded);
        assertArrayEquals(message.getBytes(CharsetSupport.getCharsetInstance()),
                ((IMessage) decoded).getMetaData().getRawMessage());
    }

    private FIXCodec createCodec(IDictionaryStructure dictionary, IMessageFactory messageFactory, TCPIPSettings settings) {
        FIXCodec codec = new FIXCodec();
        codec.init(serviceContext, settings, messageFactory, dictionary);
        return codec;
    }

    private FIXCodec createCodec(IDictionaryStructure dictionary, TCPIPSettings settings) {
        return createCodec(dictionary, factory, settings);
    }

    private IDictionaryStructure loadDictionaryFromResource(String name) throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(name)) {
            return new XmlDictionaryStructureLoader().load(in);
        }
    }

    @NotNull
    private IMessage getExpectedMessage() {
        IMessage header = new FixMessageFactory().createMessage("header", "FIX_5_0");
        header.addField("BeginString", "FIXT.1.1");
        header.addField("SenderCompID", "FIX_CSV_ds1");
        header.addField("SendingTime", "20151005-15:47:02.785");
        header.addField("TargetCompID", "FGW");
        header.addField("MsgType", "Z");
        header.addField("MsgSeqNum", "1152");
        header.addField("BodyLength", "155");
        IMessage trailer = new FixMessageFactory().createMessage("trailer", "FIX_5_0");
        trailer.addField("CheckSum", "169");
        IMessage targetPartyIds = new FixMessageFactory().createMessage("NoTargetPartyIDs", "FIX_5_0");
        targetPartyIds.addField("TargetPartyRole","76");
        targetPartyIds.addField("TargetPartyIDSource","D");
        targetPartyIds.addField("TargetPartyID","FIX_CSV_ds1");
        IMessage targetParty = new FixMessageFactory().createMessage("TargetParty", "FIX_5_0");
        targetParty.addField("NoTargetPartyIDs", ImmutableList.of(targetPartyIds));
        IMessage noQuoteEntries = new FixMessageFactory().createMessage("NoQuoteEntries", "FIX_5_0");
        noQuoteEntries.addField("QuoteEntryID",  "test");
        noQuoteEntries.addField("SecurityID",  "7219943");
        noQuoteEntries.addField("SecurityIDSource",  "8");
        IMessage quotCxlEntriesGrp = new FixMessageFactory().createMessage("QuotCxlEntriesGrp", "FIX_5_0");
        quotCxlEntriesGrp.addField("NoQuoteEntries", ImmutableList.of(noQuoteEntries));


        IMessage expectedMessage = new FixMessageFactory().createMessage("QuoteCancel", "FIX_5_0");
        expectedMessage.addField("header", header);
        expectedMessage.addField("trailer", trailer);
        expectedMessage.addField("TargetParty", ImmutableList.of(targetParty));
        expectedMessage.addField("QuoteCancelType", "4");
        expectedMessage.addField("QuoteMsgID", "1444060022986");
        expectedMessage.addField("QuotCxlEntriesGrp", ImmutableList.of(quotCxlEntriesGrp));
        return expectedMessage;
    }
}
