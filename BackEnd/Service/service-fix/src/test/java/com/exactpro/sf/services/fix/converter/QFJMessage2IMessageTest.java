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

import com.exactpro.sf.common.impl.messages.DefaultMessageFactory;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.comparison.ComparatorSettings;
import com.exactpro.sf.comparison.ComparisonResult;
import com.exactpro.sf.comparison.ComparisonUtil;
import com.exactpro.sf.comparison.MessageComparator;
import com.exactpro.sf.scriptrunner.StatusType;
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
    private final IMessageFactory messageFactory = DefaultMessageFactory.getFactory();

    @Test
    public void testRepeatingGroupOrderedFields()
            throws FileNotFoundException, IOException, ConfigError, InvalidMessage, MessageConvertException {
        Message fixMessageSrc = new Message();
        DataDictionary dataDict = getFixDictionary("FIX50.xml");
        fixMessageSrc.fromString(
                "8=FIXT.1.19=14635=Z34=115249=FIX_CSV_ds152=20151005-15:47:02.78556=FGW298=41166=1444060022986295=1299=test48=721994322=81461=11462=FIX_CSV_ds11463=D1464=7610=169",
                dataDict, true);
        if (fixMessageSrc.getException() != null)
            throw fixMessageSrc.getException();
        IDictionaryStructure dictionary = getSfDictionary("FIX50.TEST.xml");
        QFJIMessageConverter converter = new QFJIMessageConverter(dictionary, messageFactory, false, true, false);
        IMessage iMessage = converter.convert(fixMessageSrc);

        quickfix.Message fixMessageTarget = converter.convert(iMessage, true);
        Assert.assertEquals(
                "8=FIXT.1.19=15535=Z34=115249=FIX_CSV_ds152=20151005-15:47:02.78556=FGW298=41166=1444060022986295=1299=test48=721994322=81461=11462=FIX_CSV_ds11463=D1464=7610=169",
                fixMessageTarget.toString());
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

        QFJIMessageConverter converter = new QFJIMessageConverter(dictionary, messageFactory, false, true, false);
        IMessage iMessage = converter.convert(fixMessageSrc);

        quickfix.Message fixMessageTarget = converter.convert(iMessage, true);
        String fixMessageString = fixMessageTarget.toString();

        Assert.assertTrue("With millisecond", fixMessageString.contains("52=20150707-09:05:21.580"));
        Assert.assertTrue("With millisecond", fixMessageString.contains("60=20150707-09:05:21.220"));

        converter = new QFJIMessageConverter(dictionary, messageFactory, false, false, false);

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
        QFJIMessageConverter converter = new QFJIMessageConverter(dictionary, messageFactory, false, false, true);
        IMessage iMessage = converter.convert(fixMessageSrc);

        IMessage msgHeader = (IMessage) iMessage.getField("header");
        IMessage msgTrailer = (IMessage) iMessage.getField("trailer");

        Assert.assertFalse(msgHeader.getFieldNames().contains("MsgSeqNum"));
        Assert.assertFalse(msgHeader.getFieldNames().contains("BodyLength"));
        Assert.assertFalse(msgTrailer.getFieldNames().contains("CheckSum"));
    }

    @Test
    public void testFixToIMessageAndBack()
            throws FileNotFoundException, IOException, MessageConvertException, InvalidMessage, ConfigError {
        DataDictionary dataDictionary = getFixDictionary("FIX50.xml");
        Message message = new Message(
                "8=FIXT.1.19=17035=X34=78943=Y49=Sender52=20160630-10:37:21.00156=Target262=SomeReqID268=1279=0270=876.543271=123.5272=20160630273=10:37:21.003278=SomeEntryID711=1311=ABC10=090",
                dataDictionary);

        System.out.println(message.toString());

        IDictionaryStructure dictionary = getSfDictionary("FIX50.TEST.xml");
        QFJIMessageConverter converter = new QFJIMessageConverter(dictionary, messageFactory, false, true, false);

        IMessage iMessage = converter.convert(message);
        Message newMessage = converter.convert(iMessage, true);

        System.out.println(iMessage.toString());
        System.out.println(newMessage.toString());

        Assert.assertEquals(message.toString(), newMessage.toString());
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

        IDictionaryStructure dictionary = getSfDictionary("FIX50.TEST.xml");
        QFJIMessageConverter converter = new QFJIMessageConverter(dictionary, messageFactory, false, true, false);

        Message fixMessage = converter.convert(message, true);
        IMessage newMessage = converter.convert(fixMessage);

        System.out.println(fixMessage.toString());

        ComparisonResult comparisonResult = MessageComparator.compare(message, newMessage, new ComparatorSettings());

        Assert.assertEquals(0, ComparisonUtil.getResultCount(comparisonResult, StatusType.FAILED));
    }

    @Test
    public void testNullMessage() throws FileNotFoundException, IOException, MessageConvertException {
        IDictionaryStructure dictionary = getSfDictionary("FIX50.TEST.xml");
        QFJIMessageConverter converter = new QFJIMessageConverter(dictionary, messageFactory, false, true, false);

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

        IDictionaryStructure dictionary = getSfDictionary("FIX50.TEST.xml");
        QFJIMessageConverter converter = new QFJIMessageConverter(dictionary, messageFactory, false, true, false);

        Message fixMessage = converter.convert(message, true);

        System.out.println(fixMessage.toString());

        Group group = fixMessage.getGroup(1, 268);

        Assert.assertTrue(group.getDecimal(270).equals(new BigDecimal("123.5")));
        Assert.assertTrue(group.getDouble(271) == 124.5);
        Assert.assertTrue(group.getString(278).equals("S"));
    }

    @SuppressWarnings("serial")
    @Test
    public void testUnknownAndMissingMessageType() throws FileNotFoundException, IOException {
        IDictionaryStructure dictionary = getSfDictionary("FIX50.TEST.xml");
        QFJIMessageConverter converter = new QFJIMessageConverter(dictionary, messageFactory, false, true, false);

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
        QFJIMessageConverter converter = new QFJIMessageConverter(dictionary, messageFactory, true, true, false);

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
        QFJIMessageConverter converter = new QFJIMessageConverter(dictionary, messageFactory, true, true, false);

        IMessage message = converter.convert(new Message() {
            {
                getHeader().setField(new StringField(35, "X"));
                setField(new StringField(262, ""));
            }
        });

        Assert.assertFalse(message.isFieldSet("MDReqID"));
    }

}
