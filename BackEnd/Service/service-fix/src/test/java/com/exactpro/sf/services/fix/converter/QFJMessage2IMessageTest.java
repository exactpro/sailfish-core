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
package com.exactpro.sf.services.fix.converter;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.quickfixj.CharsetSupport;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.MessageUtil;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.comparison.ComparatorSettings;
import com.exactpro.sf.comparison.ComparisonResult;
import com.exactpro.sf.comparison.ComparisonUtil;
import com.exactpro.sf.comparison.MessageComparator;
import com.exactpro.sf.configuration.factory.FixMessageFactory;
import com.exactpro.sf.scriptrunner.StatusType;
import com.exactpro.sf.services.fix.FixUtil;
import com.exactpro.sf.services.fix.QFJDictionaryAdapter;
import com.exactpro.sf.util.ConverterTest;
import com.exactpro.sf.util.DateTimeUtility;

import quickfix.CharField;
import quickfix.ConfigError;
import quickfix.DataDictionary;
import quickfix.FieldNotFound;
import quickfix.Group;
import quickfix.InvalidMessage;
import quickfix.Message;
import quickfix.StringField;

//@Ignore
public class QFJMessage2IMessageTest extends ConverterTest {
    private final FixMessageFactory messageFactory = new FixMessageFactory();
    private final String sfDictionary = "FIX50.TEST.xml";

    @Test
    public void testRepeatingGroupOrderedFields() throws IOException, InvalidMessage, MessageConvertException {
        String source = "8=FIXT.1.1\u00019=155\u000135=Z\u000149=FIX_CSV_ds1\u000156=FGW\u000134=1152\u000152=20151005-15:47:02.785\u00011166=1444060022986\u0001298=4\u00011461=1\u00011462=FIX_CSV_ds1\u00011463=D\u00011464=76\u0001295=1\u0001299=test\u000148=7219943\u000122=8\u000110=169\u0001";

        Message fixMessageSrc = new Message();
        IDictionaryStructure dictionary = getSfDictionary(sfDictionary);

        DataDictionary dataDict = new QFJDictionaryAdapter(dictionary);
        fixMessageSrc.fromString(source, dataDict, true);
        if (fixMessageSrc.getException() != null) {
            throw fixMessageSrc.getException();
        }

        QFJIMessageConverterSettings settings = new QFJIMessageConverterSettings(dictionary, messageFactory)
                .setVerifyTags(true)
                .setIncludeMilliseconds(true)
                .setOrderingFields(true);

        QFJIMessageConverter converter = new QFJIMessageConverter(settings);
        IMessage iMessage = converter.convert(fixMessageSrc);

        Message fixMessageTarget = converter.convert(iMessage, true);
        Assert.assertEquals(source, fixMessageTarget.toString());
    }

    @Test
    public void testRejects() throws Exception {

        String fixMessage = "8=FIXT.1.19=14635=Z34=115249=FIX_CSV_ds152=20151005-15:47:02.78556=FGW298=41166=1444060022986299=test295=148=721994322=81461=11462=FIX_CSV_ds11463=D1464=7610=169";
        Message fixMessageSrc = new Message();
        QFJDictionaryAdapter adapter = new QFJDictionaryAdapter(getSfDictionary("FIX50.TEST.xml"));

        fixMessageSrc.fromString(fixMessage, adapter, true);

        QFJIMessageConverter converter = new QFJIMessageConverter(getSettings(sfDictionary));
        IMessage iMessage = converter.convert(fixMessageSrc);
        Assert.assertTrue(iMessage.getMetaData().isRejected());
        Assert.assertNotNull(iMessage.getMetaData().getRejectReason());
        Assert.assertEquals(fixMessage.getBytes(CharsetSupport.getCharsetInstance()).length, iMessage.getMetaData().getRawMessage().length);
    }

    @Test
    public void testEmptyRepeatingGroup() throws Exception {
        String rawMessage = "8=FIX.4.4\u00019=95\u000135=W\u000149=PARFX\u000156=Bank_GA1_MD\u000134=54\u000152=20181220-09:24:07.690\u0001"
                + "262=1545297847670\u000155=EUR/USD\u0001268=0\u000110=137\u0001";

        Message fixMessageSrc = new Message();
        DataDictionary dataDict = getFixDictionary("FIX50.xml");
        fixMessageSrc.fromString(rawMessage, dataDict, true);
        if(fixMessageSrc.getException() != null) {
            throw fixMessageSrc.getException();
        }

        IDictionaryStructure dictionary = getSfDictionary("FIX50.TEST.xml");

        QFJIMessageConverter converter = new QFJIMessageConverter(getSettings(sfDictionary));
        IMessage iMessage = converter.convert(fixMessageSrc);

        Assert.assertFalse(iMessage.isFieldSet("MDIncGrp"));
        Assert.assertFalse(iMessage.isFieldSet("NoMDEntries"));

        System.out.println(MessageUtil.convertToIHumanMessage(messageFactory, dictionary.getMessages().get(iMessage.getName()), iMessage));

        //quickfix.Message fixMessageTarget = converter.convert(iMessage, true);

    }

    @Test
    public void testEmptyRepeatingGroup2() throws Exception {
        String rawMessage = "8=FIXT.1.1\u00019=105\u000135=b\u000149=TEST\u000156=TEST\u000134=0008\u000152=20190128-11:49:38.780000\u00011128=9\u0001"
                + "131=1548676178699\u0001297=0\u0001296=0\u000110=043\u0001";

        QFJDictionaryAdapter a = new QFJDictionaryAdapter(getSfDictionary("FIX50.TEST.xml"));

        Message fixMessageSrc = new Message();
        DataDictionary dataDict = getFixDictionary("FIX50.xml");
        fixMessageSrc.fromString(rawMessage, a, true);
        if(fixMessageSrc.getException() != null) {
            throw fixMessageSrc.getException();
        }

        IDictionaryStructure dictionary = getSfDictionary("FIX50.TEST.xml");
        QFJIMessageConverter converter = new QFJIMessageConverter(getSettings(sfDictionary));
        IMessage iMessage = converter.convert(fixMessageSrc);
        Assert.assertEquals(false, iMessage.getMetaData().isRejected());

        System.out.println(MessageUtil.convertToIHumanMessage(messageFactory, dictionary.getMessages().get(iMessage.getName()), iMessage));
    }

    @Test
    public void testIncludeMilliseconds() throws InvalidMessage, ConfigError, FieldNotFound, ParseException,
            FileNotFoundException, IOException, MessageConvertException {
        Message fixMessageSrc = new Message();
        DataDictionary dataDict = getFixDictionary("FIX50.xml");
        fixMessageSrc.fromString(
                "8=FIXT.1.19=13335=q34=349=openm_c_000000452=20150707-09:05:21.58056=ECN_EQR11=1436257616538000001248=875060=20150707-09:05:21.220100=1000530=110=200",
                dataDict, true);

        IDictionaryStructure dictionary = getSfDictionary("FIX50.TEST.xml");

        QFJIMessageConverter converter = new QFJIMessageConverter(getSettings(sfDictionary));
        IMessage iMessage = converter.convert(fixMessageSrc);

        Message fixMessageTarget = converter.convert(iMessage, true);
        String fixMessageString = fixMessageTarget.toString();

        Assert.assertTrue("With millisecond", fixMessageString.contains("52=20150707-09:05:21.580"));
        Assert.assertTrue("With millisecond", fixMessageString.contains("60=20150707-09:05:21.220"));

        QFJIMessageConverterSettings settings = new QFJIMessageConverterSettings(dictionary, messageFactory);
        converter = new QFJIMessageConverter(settings);

        fixMessageTarget = converter.convert(iMessage, true);
        fixMessageString = fixMessageTarget.toString();

        Assert.assertTrue("With millisecond", fixMessageString.contains("52=20150707-09:05:21"));
        Assert.assertTrue("With millisecond", fixMessageString.contains("60=20150707-09:05:21"));
    }

    @Test
    public void testSkipTags()
            throws FileNotFoundException, IOException, ConfigError, InvalidMessage, MessageConvertException {
        Message fixMessageSrc = new Message();
        DataDictionary dataDict = getFixDictionary("FIX50.xml");
        fixMessageSrc.fromString(
                "8=FIXT.1.19=13335=q34=349=openm_c_000000452=20150707-09:05:21.58056=ECN_EQR11=1436257616538000001248=875060=20150707-09:05:21.220100=1000530=110=200",
                dataDict, true);

        IDictionaryStructure dictionary = getSfDictionary("FIX50.TEST.xml");
        QFJIMessageConverterSettings settings = new QFJIMessageConverterSettings(dictionary, messageFactory).setSkipTags(true);
        QFJIMessageConverter converter = new QFJIMessageConverter(settings);
        IMessage iMessage = converter.convert(fixMessageSrc);

        IMessage msgHeader = (IMessage) iMessage.getField("header");
        IMessage msgTrailer = (IMessage) iMessage.getField("trailer");

        Assert.assertFalse(msgHeader.getFieldNames().contains("MsgSeqNum"));
        Assert.assertFalse(msgHeader.getFieldNames().contains("BodyLength"));
        Assert.assertFalse(msgTrailer.getFieldNames().contains("CheckSum"));
    }

    @Test
    public void testFixToIMessageAndBack() throws IOException, MessageConvertException, InvalidMessage, ConfigError {
        DataDictionary dataDictionary = getFixDictionary("FIX50.xml");
        Message message = new Message(
                "8=FIXT.1.19=17035=X34=78943=Y49=Sender52=20160630-10:37:21.00156=Target262=SomeReqID268=1279=0270=876.543271=123.5272=20160630273=10:37:21.003278=SomeEntryID711=1311=ABC10=090",
                dataDictionary);

        System.out.println(message);

        QFJIMessageConverter converter = new QFJIMessageConverter(getSettings(sfDictionary));

        IMessage iMessage = converter.convert(message);
        Message newMessage = converter.convert(iMessage, true);

        System.out.println(iMessage);
        System.out.println(newMessage);

        Assert.assertTrue(FixUtil.equals(message, newMessage));
    }

    @Test
    public void testIMessageToFixAndBack() throws FileNotFoundException, IOException, MessageConvertException {
        IMessage message = messageFactory.createMessage("MarketDataIncrementalRefresh", "FIX_5_0");
        IMessage header = messageFactory.createMessage("header", "FIX_5_0");

        header.addField("BeginString", "FIXT.1.1");
        header.addField("SendingTime", DateTimeUtility.toLocalDateTime(1467283041001L));
        header.addField("TargetCompID", "Target");
        header.addField("SenderCompID", "Sender");
        header.addField("MsgType", "X");
        header.addField("BodyLength", 170);
        header.addField("PossDupFlag", true);
        header.addField("MsgSeqNum", 789);
        message.addField("header", header);
        message.addField("MDReqID", "SomeReqID");

        IMessage mdIncGrp = messageFactory.createMessage("MDIncGrp", "FIX_5_0");
        IMessage mdIncGrp_NoMDEntries = messageFactory.createMessage("MDIncGrp_NoMDEntries", "FIX_5_0");
        List<IMessage> noMDEntries = new ArrayList<>();

        mdIncGrp_NoMDEntries.addField("MDUpdateAction", '0');
        mdIncGrp_NoMDEntries.addField("MDEntryTime", DateTimeUtility.toLocalTime(38241003L));
        mdIncGrp_NoMDEntries.addField("MDEntryDate", DateTimeUtility.toLocalDate(1467244800000L));
        mdIncGrp_NoMDEntries.addField("MDEntryID", "SomeEntryID");
        mdIncGrp_NoMDEntries.addField("MDEntryPx", new BigDecimal("876.543"));
        mdIncGrp_NoMDEntries.addField("MDEntrySize", 123.5);

        noMDEntries.add(mdIncGrp_NoMDEntries);
        mdIncGrp.addField("NoMDEntries", noMDEntries);
        message.addField("MDIncGrp", mdIncGrp);

        IMessage undInstrmtGrp = messageFactory.createMessage("UndInstrmtGrp", "FIX_5_0");
        IMessage undInstrmtGrp_NoUnderlyings = messageFactory.createMessage("UndInstrmtGrp_NoUnderlyings",
                "TRD_FIX_5_0");
        IMessage underlyingInstrument = messageFactory.createMessage("UnderlyingInstrument", "FIX_5_0");
        List<IMessage> noUnderlyings = new ArrayList<>();

        underlyingInstrument.addField("UnderlyingSymbol", "ABC");
        undInstrmtGrp_NoUnderlyings.addField("UnderlyingInstrument", underlyingInstrument);
        noUnderlyings.add(undInstrmtGrp_NoUnderlyings);
        undInstrmtGrp.addField("NoUnderlyings", noUnderlyings);
        mdIncGrp_NoMDEntries.addField("UndInstrmtGrp", undInstrmtGrp);

        IMessage trailer = messageFactory.createMessage("trailer", "FIX_5_0");

        trailer.addField("CheckSum", "090");
        message.addField("trailer", trailer);

        System.out.println(message);

        QFJIMessageConverter converter = new QFJIMessageConverter(getSettings(sfDictionary));

        Message fixMessage = converter.convert(message, true);
        IMessage newMessage = converter.convert(fixMessage);

        System.out.println(fixMessage);

        ComparisonResult comparisonResult = MessageComparator.compare(message, newMessage, new ComparatorSettings());

        Assert.assertEquals(0, ComparisonUtil.getResultCount(comparisonResult, StatusType.FAILED));
    }

    @Test
    public void testNullMessage() throws FileNotFoundException, IOException, MessageConvertException {
        QFJIMessageConverter converter = new QFJIMessageConverter(getSettings(sfDictionary));

        Assert.assertEquals(null, converter.convert(null));
        Assert.assertEquals(null, converter.convert(null, true));
    }

    @Test
    public void testDifferentTypes() throws FileNotFoundException, IOException, MessageConvertException, FieldNotFound {
        IMessage message = messageFactory.createMessage("MarketDataIncrementalRefresh", "TRD_FIX_5_0");
        IMessage header = messageFactory.createMessage("header", "TRD_FIX_5_0");

        header.addField("MsgType", "X");
        message.addField("header", header);

        IMessage mdIncGrp = messageFactory.createMessage("MDIncGrp", "TRD_FIX_5_0");
        IMessage mdIncGrp_NoMDEntries = messageFactory.createMessage("MDIncGrp_NoMDEntries", "TRD_FIX_5_0");
        List<IMessage> noMDEntries = new ArrayList<>();

        mdIncGrp_NoMDEntries.addField("MDEntryPx", 123.5); // double instead of
                                                           // BigDecimal
        mdIncGrp_NoMDEntries.addField("MDEntrySize", new BigDecimal("124.5")); // BigDecimal
                                                                               // instead
                                                                               // of
                                                                               // double
        mdIncGrp_NoMDEntries.addField("MDEntryID", 'S'); // char instead of
                                                         // String

        noMDEntries.add(mdIncGrp_NoMDEntries);
        mdIncGrp.addField("NoMDEntries", noMDEntries);
        message.addField("MDIncGrp", mdIncGrp);

        System.out.println(message);

        QFJIMessageConverter converter = new QFJIMessageConverter(getSettings(sfDictionary));

        Message fixMessage = converter.convert(message, true);

        System.out.println(fixMessage);

        Group group = fixMessage.getGroup(1, 268);

        Assert.assertTrue(group.getDecimal(270).equals(new BigDecimal("123.5")));
        Assert.assertTrue(group.getDouble(271) == 124.5);
        Assert.assertTrue("S".equals(group.getString(278)));
    }

    @SuppressWarnings("serial")
    @Test
    public void testUnknownAndMissingMessageType() throws FileNotFoundException, IOException {
        QFJIMessageConverter converter = new QFJIMessageConverter(getSettings("FIX50.TEST.xml"));

        try {
            converter.convert(new Message());
        } catch (MessageConvertException e) {
            Assert.assertEquals(e.getMessage(), "Failed to get message type");
        }

        try {
            converter.convert(new Message() {
                {
                    getHeader().setField(new StringField(35, "UnknownMessage"));
                }
            });
        } catch (MessageConvertException e) {
            Assert.assertEquals(e.getMessage(), "Unknown message type: UnknownMessage");
        }
    }

    @SuppressWarnings("serial")
    @Test
    public void testUnknownAndExtraTag() throws FileNotFoundException, IOException {
        IDictionaryStructure dictionary = getSfDictionary("FIX50.TEST.xml");
        QFJIMessageConverterSettings settings = new QFJIMessageConverterSettings(dictionary, messageFactory)
                .setVerifyTags(true)
                .setIncludeMilliseconds(true);
        QFJIMessageConverter converter = new QFJIMessageConverter(settings);

        try {
            converter.convert(new Message() {
                {
                    getHeader().setField(new StringField(35, "X"));
                    setField(new StringField(876, "SoylentGreen"));
                }
            });
        } catch (MessageConvertException e) {
            Assert.assertEquals(e.getMessage(), "Unknown tag: 876");
        }

        try {
            converter.convert(new Message() {
                {
                    getHeader().setField(new StringField(35, "X"));
                    setField(new CharField(274, '0'));
                }
            });
        } catch (MessageConvertException e) {
            Assert.assertEquals(e.getMessage(), "Message 'MarketDataIncrementalRefresh' doesn't contain tag: 274");
        }
    }

    @SuppressWarnings("serial")
    @Test
    public void testEmptyFieldValue() throws FileNotFoundException, IOException, MessageConvertException {
        IDictionaryStructure dictionary = getSfDictionary("FIX50.TEST.xml");
        QFJIMessageConverterSettings settings = new QFJIMessageConverterSettings(dictionary, messageFactory)
                .setVerifyTags(true)
                .setIncludeMilliseconds(true);
        QFJIMessageConverter converter = new QFJIMessageConverter(settings);

        IMessage message = converter.convert(new Message() {
            {
                getHeader().setField(new StringField(35, "X"));
                setField(new StringField(262, ""));
            }
        });

        Assert.assertFalse(message.isFieldSet("MDReqID"));
    }

    @Test
    public void testMessageWithDataLengthTypes() throws Exception {
        IMessage message = messageFactory.createMessage("MarketDataIncrementalRefresh", "FIX_5_0");
        IMessage header = messageFactory.createMessage("header", "FIX_5_0");
        header.addField("BeginString", "FIXT.1.1");
        header.addField("SendingTime", DateTimeUtility.toLocalDateTime(1467283041001L));
        header.addField("TargetCompID", "Target");
        header.addField("SenderCompID", "Sender");
        header.addField("MsgType", "X");
        header.addField("BodyLength", 170);
        header.addField("PossDupFlag", true);
        header.addField("MsgSeqNum", 789);
        header.addField("XmlData", "TestText");
        header.addField("XmlDataLen", 8);

        message.addField("header", header);
        message.addField("RefSeqNum", 2);
        IMessage trailer = messageFactory.createMessage("trailer", "FIX_5_0");
        trailer.addField("CheckSum", "090");
        message.addField("trailer", trailer);

        System.out.println(message);

        QFJIMessageConverter converter = new QFJIMessageConverter(getSettings(sfDictionary));

        Message fixMessage = converter.convert(message, true);
        IMessage newMessage = converter.convert(fixMessage);

        System.out.println(fixMessage);

        ComparisonResult comparisonResult = MessageComparator.compare(message, newMessage, new ComparatorSettings());

        Assert.assertEquals(0, ComparisonUtil.getResultCount(comparisonResult, StatusType.FAILED));
    }

    @Test
    public void testMessageWithDataNotLengthTypes() throws Exception {
        IMessage message = messageFactory.createMessage("MarketDataIncrementalRefresh", "FIX_5_0");
        IMessage header = messageFactory.createMessage("header", "FIX_5_0");
        header.addField("BeginString", "FIXT.1.1");
        header.addField("SendingTime", DateTimeUtility.toLocalDateTime(1467283041001L));
        header.addField("TargetCompID", "Target");
        header.addField("SenderCompID", "Sender");
        header.addField("MsgType", "X");
        header.addField("BodyLength", 170);
        header.addField("PossDupFlag", true);
        header.addField("MsgSeqNum", 789);
        header.addField("XmlData", "TestText");

        message.addField("header", header);
        message.addField("RefSeqNum", 2);
        IMessage trailer = messageFactory.createMessage("trailer", "FIX_5_0");
        trailer.addField("CheckSum", "090");
        message.addField("trailer", trailer);

        System.out.println(message);

        QFJIMessageConverter converter = new QFJIMessageConverter(getSettings(sfDictionary));

        Message fixMessage = converter.convert(message, true);

        header.addField("XmlDataLen", 8);

        IMessage newMessage = converter.convert(fixMessage);

        System.out.println(fixMessage);

        ComparisonResult comparisonResult = MessageComparator.compare(message, newMessage, new ComparatorSettings());

        Assert.assertEquals(0, ComparisonUtil.getResultCount(comparisonResult, StatusType.FAILED));
    }

    //Special case for Signature which violates above assumption.
    //Field Signature with tag 89 and type DATA has a paired field with tag 93 and type LENGTH.
    @Test
    public void testMessageWithDataNotLengthTypesForSignature() throws Exception {
        IMessage message = messageFactory.createMessage("MarketDataIncrementalRefresh", "FIX_5_0");
        IMessage header = messageFactory.createMessage("header", "FIX_5_0");
        header.addField("BeginString", "FIXT.1.1");
        header.addField("SendingTime", DateTimeUtility.toLocalDateTime(1467283041001L));
        header.addField("TargetCompID", "Target");
        header.addField("SenderCompID", "Sender");
        header.addField("MsgType", "X");
        header.addField("BodyLength", 170);
        header.addField("PossDupFlag", true);
        header.addField("MsgSeqNum", 789);
        header.addField("XmlData", "TestText");
        header.addField("XmlDataLen", 8);

        message.addField("header", header);
        message.addField("RefSeqNum", 2);
        IMessage trailer = messageFactory.createMessage("trailer", "FIX_5_0");
        trailer.addField("CheckSum", "090");
        trailer.addField("Signature", "signature");
        message.addField("trailer", trailer);

        System.out.println(message);

        QFJIMessageConverter converter = new QFJIMessageConverter(getSettings(sfDictionary));

        Message fixMessage = converter.convert(message, true);

        trailer.addField("SignatureLength", 9);

        IMessage newMessage = converter.convert(fixMessage);

        System.out.println(fixMessage);

        ComparisonResult comparisonResult = MessageComparator.compare(message, newMessage, new ComparatorSettings());

        Assert.assertEquals(0, ComparisonUtil.getResultCount(comparisonResult, StatusType.FAILED));
    }

    private QFJIMessageConverterSettings getSettings(String sfDictionary) throws IOException {
        IDictionaryStructure dictionary = getSfDictionary(sfDictionary);
        return new QFJIMessageConverterSettings( dictionary, messageFactory).setIncludeMilliseconds(true);
    }
}
