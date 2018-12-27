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

import java.sql.Timestamp;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.actions.ConvertUtil;
import com.exactpro.sf.common.impl.messages.DefaultMessageFactory;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.MessageUtil;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.services.fix.converter.dirty.DirtyQFJIMessageConverter;
import com.exactpro.sf.services.fix.converter.dirty.FieldConst;
import com.exactpro.sf.services.fix.converter.dirty.struct.Field;
import com.exactpro.sf.services.fix.converter.dirty.struct.FieldList;
import com.exactpro.sf.services.fix.converter.dirty.struct.RawMessage;
import com.exactpro.sf.util.ConverterTest;

import quickfix.DataDictionary;
import quickfix.InvalidMessage;
import quickfix.Message;
import quickfix.field.BeginString;
import quickfix.field.BodyLength;
import quickfix.field.CheckSum;
import quickfix.field.MsgSeqNum;
import quickfix.field.MsgType;
import quickfix.field.SenderCompID;
import quickfix.field.SendingTime;
import quickfix.field.TargetCompID;
import quickfix.field.converter.UtcTimestampConverter;

public class DirtyQFJIMessageConverterTest extends ConverterTest {
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private final IMessageFactory messageFactory = DefaultMessageFactory.getFactory();

    private static DataDictionary dataDictionary;
    private static IDictionaryStructure dictionary;
    private static final Logger logger = LoggerFactory.getLogger(DirtyQFJIMessageConverterTest.class);
    private String MESSAGE = "8=FIXT.1.19=15535=Z34=115249=FIX_CSV_ds152=20151005-15:47:02.78556=FGW298=41166=1444060022986295=1299=test48=721994322=81461=11462=FIX_CSV_ds11463=D1464=7610=169";

    @BeforeClass
    public static void initClass() {
        try {
            dataDictionary = getFixDictionary("FIX50.xml");
            dictionary = getSfDictionary("FIX50.TEST.xml");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Try to convert Message to IMessage with inlineHeaderAndTrailer = true.
     * Compare result IMessage with IMessage, getting from converting Message
     * with QFJIMessageConverter. Test header and trailer in line.
     */

    @Test
    public void testConvertWithInline() throws Exception {
        try {
            Message message = getFixMessage();
            DirtyQFJIMessageConverter converter = new DirtyQFJIMessageConverter(dictionary, messageFactory, false, true,
                    false);
            IMessage messageInline = converter.convertDirty(message, true);
            IMessage original = converter.convert(message);
            IMessage header = (IMessage) original.getField("header");
            original.removeField("header");
            IMessage trailer = (IMessage) original.getField("trailer");
            original.removeField("trailer");

            for (String name : header.getFieldNames()) {
                Assert.assertEquals(header.<Object>getField(name), messageInline.<Object>getField(name));
                messageInline.removeField(name);
            }
            for (String name : trailer.getFieldNames()) {
                Assert.assertEquals(trailer.<Object>getField(name), messageInline.<Object>getField(name));
                messageInline.removeField(name);
            }
            for (String name : messageInline.getFieldNames()) {
                Assert.assertEquals(original.getField(name).toString(), messageInline.getField(name).toString());
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Try to convert Message to IMessage with inlineHeaderAndTrailer = false.
     * Compare result IMessage with IMessage, getting from converting Message
     * with QFJIMessageConverter.
     */

    @Test
    public void testConvertWithoutInline() throws Exception {
        try {
            Message message = getFixMessage();
            DirtyQFJIMessageConverter converter = new DirtyQFJIMessageConverter(dictionary, messageFactory, false, true,
                    false);

            IMessage iMessage = converter.convertDirty(message, false);
            IMessage original = converter.convert(message);

            for (String name : original.getFieldNames()) {
                Assert.assertEquals(original.getField(name).toString(), iMessage.getField(name).toString());
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Try to convert IMessage to RawMessage with extractAndTrailer = false.
     * IMessage obtain from Message, converted by DirtyQFJIMessageCnverter
     */

    @Test
    public void testConvertIMessageToRawWithoutExtract() throws Exception {
        try {
            Message message = getFixMessage();
            DirtyQFJIMessageConverter converter = new DirtyQFJIMessageConverter(dictionary, messageFactory, false, true,
                    false);
            IMessage iMessage = converter.convertDirty(message, false);
            RawMessage raw = converter.convertDirty(iMessage, "QuoteCancel", true, "FIXT.1.1", 1152,
                    "FIX_CSV_ds1", "FGW");
            Assert.assertTrue("Message " + message.toString() + " is different from RawMessage " + raw.toString(),
                    compareMessages(message, raw));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Try to convert IMessage to RawMessage with extractAndTrailer = true.
     * IMessage with field inline obtain from Message, converted by
     * DirtyQFJIMessageCnverter
     */

    @Test
    public void testConvertIMessageToRawWithExtract() throws Exception {
        try {
            Message message = getFixMessage();
            DirtyQFJIMessageConverter converter = new DirtyQFJIMessageConverter(dictionary, messageFactory, false, true,
                    false);
            IMessage iMessage = converter.convertDirty(message, true);
            String mock = null;
            RawMessage raw = converter.convertDirty(iMessage, "QuoteCancel", true, mock, 0, mock, mock);
            Assert.assertTrue("Message " + message.toString() + " is different from RawMessage " + raw.toString(),
                    compareMessages(message, raw));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }
    @Test
    public void testDirtyReplace() throws Exception{
        DirtyQFJIMessageConverter converter = new DirtyQFJIMessageConverter(dictionary, messageFactory, false, true,
                false);

        IMessage iMessage = messageFactory.createMessage("QuoteCancel", "FIX_5_0");
        IMessage trailer = messageFactory.createMessage("trailer", "FIX_5_0");
        IMessage header = messageFactory.createMessage("header", "FIX_5_0");
        iMessage.addField("trailer", trailer);
        iMessage.addField("header", header);

        iMessage.addField("BeginString", "FIXT.1.1");
        iMessage.addField("DirtySecurityIDSource", "8");
        iMessage.addField("DirtyQuoteCancelType", "4");
        iMessage.addField("DirtyCheckSum", "246");
        String expected = "9=11\u000122=8\u0001298=4\u00018=FIXT.1.1\u000110=246\u0001";
        String mock = null;
        RawMessage raw = converter.convertDirty(iMessage, "QuoteCancel", false, mock, 0, mock, mock);
        Assert.assertEquals(expected, raw.toString());

        iMessage.addField("DirtyHelloWorld", "Hello");

        exception.expect(MessageConvertException.class);
        converter.convertDirty(iMessage, "QuoteCancel", false, mock, 0, mock, mock);

        iMessage = messageFactory.createMessage("QuoteCancel", "FIX_5_0");
        iMessage.addField("BeginString", "FIXT.1.1");
        iMessage.addField("DirtySecurityIDSource", "8");
        iMessage.addField("DirtyQuoteCancelType", "4");
        iMessage.addField("DirtyCheckSum", "246");
        raw = converter.convertDirty(iMessage, "QuoteCancel", false, mock, 0, mock, mock);
        Assert.assertEquals(expected, raw.toString());

        iMessage = messageFactory.createMessage("QuoteCancel", "FIX_5_0");
        iMessage.addField("BeginString", "FIXT.1.1");
        iMessage.addField("Dirty22", "8");
        iMessage.addField("DirtyQuoteCancelType", "4");
        iMessage.addField("DirtyCheckSum", "246");
        raw = converter.convertDirty(iMessage, "QuoteCancel", false, mock, 0, mock, mock);
        Assert.assertEquals(expected, raw.toString());

        iMessage = messageFactory.createMessage("QuoteCancel", "FIX_5_0");
        iMessage.addField("BeginString", "FIXT.1.1");
        iMessage.addField("Dirty22222222222222", "8");
        iMessage.addField("DirtyQuoteCancelType", "4");
        iMessage.addField("DirtyCheckSum", "246");
        raw = converter.convertDirty(iMessage, "QuoteCancel", false, mock, 0, mock, mock);
        Assert.assertEquals(expected, raw.toString());
    }
    /**
     * Try to convert IMessage to RawMessage with filling header.
     */

    @Test
    public void testFillHeader() throws Exception {
        try {
            Message message = getFixMessage();
            DirtyQFJIMessageConverter converter = new DirtyQFJIMessageConverter(dictionary, messageFactory, false, true,
                    false);

            IMessage iMessage = createIMessage();
            iMessage.removeField("header");

            RawMessage raw = converter.convertDirty(iMessage, "QuoteCancel", true, "FIXT.1.1", 1152,
                    "FIX_CSV_ds1", "FGW");
            String timeExpected = UtcTimestampConverter.convert(new Timestamp(System.currentTimeMillis()), false, false).substring(0, 14);
            Assert.assertTrue("Message " + message.toString() + " is different from RawMessage " + raw.toString(),
                    compareMessages(message, raw, 52));
            String timeActual = getDateFromMessage(raw, 52);
            if (timeExpected != null) {
                Assert.assertEquals(timeExpected, timeActual.substring(0, 14));
            } else {
                Assert.fail("There is no SendingTime in the message " + raw.toString());
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Tests field order in result RawMessage
     */

    @Test
    public void testFieldOrder() throws Exception {
        try {
            Message message = getFixMessage();
            DirtyQFJIMessageConverter converter = new DirtyQFJIMessageConverter(dictionary, messageFactory, false, true,
                    false);
            IMessage iMessage = converter.convertDirty(message, false);

            IMessage header = (IMessage) iMessage.getField("header");
            iMessage.removeField("header");
            List<String> order = new ArrayList<>();
            order.add("9");
            order.add("8");
            order.add("34");
            order.add("35");
            order.add("49");
            order.add("56");
            order.add("52");
            header.addField("FieldOrder", order);
            iMessage.addField("header", header);
            order = new ArrayList<>();
            order.add("298");
            order.add("1166");
            order.add("TargetParty");
            iMessage.addField("FieldOrder", order);
            RawMessage raw = converter.convertDirty(iMessage, "QuoteCancel", true, "FIXT.1.1", 1152,
                    "FIX_CSV_ds1", "FGW");
            Assert.assertEquals(
                    "9=1558=FIXT.1.134=115235=Z49=FIX_CSV_ds156=FGW52=20151005-15:47:02.785298=41166=14440600229861461=11462=FIX_CSV_ds11463=D1464=76295=1299=test48=721994322=810=169",
                    raw.toString());

            order.add(1, "trailer");

            raw = converter.convertDirty(iMessage, "QuoteCancel", true, "FIXT.1.1", 1152, "FIX_CSV_ds1", "FGW");
            Assert.assertEquals(
                    "9=1558=FIXT.1.134=115235=Z49=FIX_CSV_ds156=FGW52=20151005-15:47:02.785298=410=1691166=14440600229861461=11462=FIX_CSV_ds11463=D1464=76295=1299=test48=721994322=8",
                    raw.toString());

            order.add(3, "header");

            raw = converter.convertDirty(iMessage, "QuoteCancel", true, "FIXT.1.1", 1152, "FIX_CSV_ds1", "FGW");
            Assert.assertEquals(
                    "298=410=1691166=14440600229869=1558=FIXT.1.134=115235=Z49=FIX_CSV_ds156=FGW52=20151005-15:47:02.7851461=11462=FIX_CSV_ds11463=D1464=76295=1299=test48=721994322=8",
                    raw.toString());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Try to convert IMessage with list of values in one field to RawMessage
     */

    @Test
    public void testMultipleValues() throws Exception {
        try {
            DirtyQFJIMessageConverter converter = new DirtyQFJIMessageConverter(dictionary, messageFactory, false, true,
                    false);
            IMessage heartbeatTest = getAdditionalHeartbeat();
            RawMessage raw = converter.convertDirty(heartbeatTest, "Heartbeat", true, "FIXT.1.1", 1152,
                    "FIX_CSV_ds1", "FGW");
            Assert.assertTrue(raw.toString().contains("112=test1"));
            Assert.assertTrue(raw.toString().contains("112=test2"));
            Assert.assertTrue(raw.toString().contains("112=test3"));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Try to convert Message to IMessage and result IMessage to RawMessage.
     * Compare RawMessage with original Message
     */

    @Test
    public void testConvertFromMessageToIMessageAndBack() throws Exception {
        try {
            Message message = getFixMessage();
            DirtyQFJIMessageConverter converter = new DirtyQFJIMessageConverter(dictionary, messageFactory, false, true,
                    false);
            IMessage iMessage = converter.convertDirty(message, false);
            RawMessage raw = converter.convertDirty(iMessage, "QuoteCancel", true, "", 0, "", "");
            Assert.assertTrue("Message " + message.toString() + " is different from RawMessage " + raw.toString(),
                    compareMessages(message, raw));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Try to add extra field to iMessage and convert. Compare string
     * representation of the result RawMessage with scheme.
     *
     * @throws Exception
     */

    @Test
    public void testExtraField() throws Exception {
        try {
            DirtyQFJIMessageConverter converter = new DirtyQFJIMessageConverter(dictionary, messageFactory, false, true,
                    false);

            IMessage iMessage = createIMessage();
            IMessage trailer = iMessage.getField("trailer");
            IMessage header = iMessage.getField("header");
            IMessage quotCxlEntriesGrp = iMessage.getField("QuotCxlEntriesGrp");
            List<IMessage> noQuoteEntries = quotCxlEntriesGrp.getField("NoQuoteEntries");

            quotCxlEntriesGrp.addField("LegAllInRate", "LegAllInRate");
            noQuoteEntries.get(0).addField("8774", "LegYield");
            iMessage.addField("8773", "MDHedgeLegQuantity");
            header.addField("8772", "MDHedgeLegYield");
            trailer.addField("8771", "MDHedgeLegPrice");

            RawMessage raw = converter.convertDirty(iMessage, "QuoteCancel", true, "FIXT.1.1", 1152,
                    "FIX_CSV_ds1", "FGW");
            Assert.assertEquals(
                    "8=FIXT.1.19=15535=Z49=FIX_CSV_ds156=FGW34=115252=20151005-18:47:02.7858772=MDHedgeLegYield"
                            + "1166=1444060022986298=41461=1" + "1462=FIX_CSV_ds11463=D1464=76295=1"
                            + "299=test48=721994322=88774=LegYield" + "8775=LegAllInRate"
                            + "8773=MDHedgeLegQuantity" + "10=1698771=MDHedgeLegPrice",
                    raw.toString());

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Try to exclude required field in the trailer and in the header and check
     * RawMessage after.
     *
     * @throws Exception
     */

    @Test
    public void testExcludeAutogeneratedTag() throws Exception {
        try {
            DirtyQFJIMessageConverter converter = new DirtyQFJIMessageConverter(dictionary, messageFactory, false, true,
                    false);

            Set<Entry<String, String>> fields = new LinkedHashSet<>();
            Set<Entry<String, String>> excluded = new HashSet<>();

            IMessage iMessage = messageFactory.createMessage("QuoteCancel", "FIX_5_0");
            IMessage trailer = messageFactory.createMessage("trailer", "FIX_5_0");
            IMessage header = messageFactory.createMessage("header", "FIX_5_0");
            iMessage.addField("trailer", trailer);
            iMessage.addField("header", header);

            header.addField(SendingTime.class.getSimpleName(), "20160720-12:39:59.391");

            RawMessage raw = converter.convertDirty(iMessage, "QuoteCancel", true, "FIXT.1.1", 1152,
                    "FIX_CSV_ds1", "FGW");
            String sourceMessage = raw.toString();
            Assert.assertEquals("Source message",
                    "8=FIXT.1.19=6035=Z49=FIX_CSV_ds156=FGW34=115252=20160720-12:39:59.39110=107",
                    sourceMessage);

            // Check trailer
            fields.clear();
            excluded.clear();

            fields.add(new AbstractMap.SimpleEntry<>(CheckSum.class.getSimpleName(),
                    String.valueOf(CheckSum.FIELD)));
            raw = converter.convertDirty(iMessage, "QuoteCancel", true, "FIXT.1.1", 1152, "FIX_CSV_ds1", "FGW");
            checkFieldExisting(raw.getField("trailer").getFields(), fields, excluded);

            for (Entry<String, String> entry : fields) {
                trailer.addField(entry.getKey(), FieldConst.EXCLUDED_FIELD);
                excluded.add(entry);

                raw = converter.convertDirty(iMessage, "QuoteCancel", true, "FIXT.1.1", 1152, "FIX_CSV_ds1",
                        "FGW");
                checkFieldExisting(raw.getField("trailer").getFields(), fields, excluded);

                sourceMessage = sourceMessage.replaceAll(entry.getValue() + "=[\\w\\.]+", "");
                Assert.assertEquals("Exclude " + entry.getKey(), sourceMessage, raw.toString());
            }

            // Check header
            fields.clear();
            excluded.clear();

            fields.add(new AbstractMap.SimpleEntry<>(BeginString.class.getSimpleName(),
                    String.valueOf(BeginString.FIELD)));
            fields.add(new AbstractMap.SimpleEntry<>(BodyLength.class.getSimpleName(),
                    String.valueOf(BodyLength.FIELD)));
            fields.add(new AbstractMap.SimpleEntry<>(MsgType.class.getSimpleName(),
                    String.valueOf(MsgType.FIELD)));
            fields.add(new AbstractMap.SimpleEntry<>(SenderCompID.class.getSimpleName(),
                    String.valueOf(SenderCompID.FIELD)));
            fields.add(new AbstractMap.SimpleEntry<>(TargetCompID.class.getSimpleName(),
                    String.valueOf(TargetCompID.FIELD)));
            fields.add(new AbstractMap.SimpleEntry<>(MsgSeqNum.class.getSimpleName(),
                    String.valueOf(MsgSeqNum.FIELD)));
            fields.add(new AbstractMap.SimpleEntry<>(SendingTime.class.getSimpleName(),
                    String.valueOf(SendingTime.FIELD)));

            raw = converter.convertDirty(iMessage, "QuoteCancel", true, "FIXT.1.1", 1152, "FIX_CSV_ds1", "FGW");
            checkFieldExisting(raw.getField("header").getFields(), fields, excluded);

            for (Entry<String, String> entry : fields) {
                header.addField(entry.getKey(), FieldConst.EXCLUDED_FIELD);
                excluded.add(entry);

                raw = converter.convertDirty(iMessage, "QuoteCancel", true, "FIXT.1.1", 1152, "FIX_CSV_ds1",
                        "FGW");
                checkFieldExisting(raw.getField("header").getFields(), fields, excluded);

                sourceMessage = sourceMessage.replaceFirst(entry.getValue() + "=[\\w\\.\\-\\:]+", "");
                Assert.assertEquals("Exclude " + entry.getKey(), sourceMessage, raw.toString());
            }

            Assert.assertEquals("Message without header fields", "", raw.toString());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    @Test
    /**
     * Try to convert IMessage with inline without extract, using filling header
     *
     * @throws Exception
     */
    public void testInlineWithoutExtract() throws Exception {
        try {
            Message message = getFixMessage();
            DirtyQFJIMessageConverter converter = new DirtyQFJIMessageConverter(dictionary, messageFactory, false, true,
                    false);
            IMessage iMessage = converter.convertDirty(message, true);
            RawMessage raw = converter.convertDirty(iMessage, "QuoteCancel", true, "FIXT.1.1", 1152,
                    "FIX_CSV_ds1", "FGW");
            Assert.assertTrue("Message " + message.toString() + " is different from RawMessage " + raw.toString(),
                    compareMessages(message, raw, false, false));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    @Test
    /**
     * Try to change counter group for field TargerParty, using GroupCounters
     *
     * @throws Exception
     */
    public void testGroupCounters() throws Exception {
        try {
            Message message = getFixMessage();
            DirtyQFJIMessageConverter converter = new DirtyQFJIMessageConverter(dictionary, messageFactory, false, true,
                    false);

            IMessage iMessage = converter.convertDirty(message, false);// createIMessage();

            // GroupCounters as Map<?, ?>
            Map<Object, Object> groupCounters = new ConvertUtil().toMap("1461", "2");
            IMessage targetParty = (IMessage) iMessage.getField("TargetParty");
            targetParty.addField("GroupCounters", groupCounters);
            testGroupCounter(iMessage, message, converter);

            // GroupCounters as IMessage
            IMessage groupCountersMsg = MessageUtil.convertToIMessage(groupCounters, DefaultMessageFactory.getFactory(),
                    "test", "GroupCounters");
            targetParty.addField("GroupCounters", groupCountersMsg);
            testGroupCounter(iMessage, message, converter);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Try to exclude counter group for field TargerParty, using GroupCounters
     *
     * @throws Exception
     */
    @Test
    public void testGroupCounterExclusion() throws Exception {
        try {
            Message message = getFixMessage();
            DirtyQFJIMessageConverter converter = new DirtyQFJIMessageConverter(dictionary, messageFactory, false, true,
                    false);

            IMessage iMessage = converter.convertDirty(message, false);// createIMessage();

            Map<Object, Object> groupCounters = new ConvertUtil().toMap("1461", FieldConst.EXCLUDED_FIELD);
            IMessage targetParty = (IMessage) iMessage.getField("TargetParty");
            targetParty.addField("GroupCounters", groupCounters);

            RawMessage raw = converter.convertDirty(iMessage, "QuoteCancel", false, "FIXT.1.1", 1152,
                    "FIX_CSV_ds1", "FGW");
            Assert.assertTrue("RawMessage does countain group counter: 1461", !raw.toString().contains("1461="));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Tests field order in result RawMessage. Negative test
     */

    @Test
    public void testIncorrectGroupCounters() {
        try {
            Message message = new Message();
            message.fromString(MESSAGE, dataDictionary, true);
            if (message.getException() != null) {
                throw message.getException();
            }
            DirtyQFJIMessageConverter converter = new DirtyQFJIMessageConverter(dictionary, messageFactory, false, true,
                    false);
            IMessage iMessage = converter.convertDirty(message, false);
            try {
                String groupCounters = "1461=3";
                iMessage.addField("GroupCounters", groupCounters);
                converter.convertDirty(iMessage, "QuoteCancel", true, "FIXT.1.1", 1152, "FIX_CSV_ds1", "FGW");
                Assert.fail("There is no exceptions was thrown");
            } catch (MessageConvertException e) {
                Assert.assertEquals("GroupCounters field is neither a Map nor a IMessage", e.getMessage());
            }

            iMessage = converter.convertDirty(message, false);
            try {
                Map<Object, Object> groupCounters = new ConvertUtil().toMap("", "=3");
                IMessage targetParty = (IMessage) iMessage.getField("TargetParty");
                targetParty.addField("GroupCounters", groupCounters);
                converter.convertDirty(iMessage, "QuoteCancel", false, "FIXT.1.1", 1152, "FIX_CSV_ds1", "FGW");
                Assert.fail("There is no exceptions was thrown");
            } catch (MessageConvertException e) {
                Assert.assertTrue(e.getMessage().startsWith("Unknown group counter field in GroupCounters: "));
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Tests field order in result RawMessage. Negative Test
     */

    @Test
    public void testIncorrectFieldOrder() {
        try {
            Message message = new Message();
            message.fromString(MESSAGE, dataDictionary, true);
            if (message.getException() != null) {
                throw message.getException();
            }
            DirtyQFJIMessageConverter converter = new DirtyQFJIMessageConverter(dictionary, messageFactory, false, true,
                    false);
            IMessage iMessage = converter.convertDirty(message, false);
            // Try to use Order Field, which contains integers
            try {
                List<Integer> orderInteger = new ArrayList<>();
                orderInteger.add(298);
                orderInteger.add(1166);
                iMessage.addField("FieldOrder", orderInteger);
                converter.convertDirty(iMessage, "QuoteCancel", true, "FIXT.1.1", 1152, "FIX_CSV_ds1", "FGW");
                Assert.fail("There is no exceptions was thrown");
            } catch (MessageConvertException e) {
                Assert.assertEquals("FieldOrder field is not a list of strings", e.getMessage());
            }
            iMessage = converter.convertDirty(message, false);
            try {
                int orderInteger = 298;
                iMessage.addField("FieldOrder", orderInteger);
                converter.convertDirty(iMessage, "QuoteCancel", true, "FIXT.1.1", 1152, "FIX_CSV_ds1", "FGW");
                Assert.fail("There is no exceptions was thrown");
            } catch (MessageConvertException e) {
                Assert.assertEquals("FieldOrder field is not a list", e.getMessage());
            }
            iMessage = converter.convertDirty(message, false);
            try {
                List<String> order = new ArrayList<>();
                order.add("-1");
                iMessage.addField("FieldOrder", order);
                converter.convertDirty(iMessage, "QuoteCancel", true, "FIXT.1.1", 1152, "FIX_CSV_ds1", "FGW");
                Assert.fail("There is no exceptions was thrown");
            } catch (MessageConvertException e) {
                Assert.assertEquals("Unknown field in field order: -1", e.getMessage());
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testDates() throws Exception {
        try {
            IMessage imessage = createNDEntries();
            String timeExpected = UtcTimestampConverter.convert(new Timestamp(System.currentTimeMillis()), false, false);
            DirtyQFJIMessageConverter converter = new DirtyQFJIMessageConverter(dictionary, messageFactory, false, true,
                    false);
            RawMessage raw = converter.convertDirty(imessage, "MDIncGrp_NoMDEntries", true, "FIXT.1.1", 1152,
                    "FIX_CSV_ds1", "FGW");
            String dateOnly = getDateFromMessage(raw, 272);
            String time = getDateFromMessage(raw, 273);
            Assert.assertEquals(dateOnly, timeExpected.substring(0, 8));
            Assert.assertEquals(time.substring(0, 8), timeExpected.substring(9, 17));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    @Test
    public void testFieldComparator() throws Exception {
        FieldList msg = new FieldList();

        msg.addField(new Field("7", "zxc"));
        msg.addField(new Field("8", "xcv"));
        msg.addField(new Field("1", "asd", 0));
        msg.addField(new Field("2", "sdf"));
        msg.addField(new Field("3", "dfg"));
        msg.addField(new Field("trailer", "end"));
        msg.addField(new Field("header", "start"));
        msg.addField(new Field("1", "fgh", 1));
        msg.addField(new Field("4", "ghj", 0));
        msg.addField(new Field("4", "hjk", 1));

        List<String> order = new ArrayList<>();

        order.add("4");
        order.add("1");
        order.add("3");
        order.add("4");
        order.add("2");
        order.add("1");

        msg.setOrder(order);
        msg.ensureOrder();

        Assert.assertEquals("header=start4=ghj1=asd3=dfg4=hjk2=sdf1=fgh7=zxc8=xcvtrailer=end", msg.toString());
    }

    private Message getFixMessage() throws InvalidMessage {
        Message message = new Message();
        message.fromString(MESSAGE, dataDictionary, true);
        if (message.getException() != null) {
            throw message.getException();
        }
        return message;
    }

    private void checkFieldExisting(FieldList message, Set<Entry<String, String>> fields,
            Set<Entry<String, String>> excluded) {

        Assert.assertTrue("Extra tag, fields: " + fields + "; excluded: " + excluded, fields.containsAll(excluded));
        for (Entry<String, String> field : fields) {
            if (excluded.contains(field)) {
                Assert.assertNull("Null field " + field.getKey(), message.getField(field.getValue()));
            } else {
                Assert.assertNotNull("Not null field " + field.getKey(), message.getField(field.getValue()));
            }
        }
    }

    private void testGroupCounter(IMessage iMessage, Message message, DirtyQFJIMessageConverter converter) throws MessageConvertException {
        RawMessage raw = converter.convertDirty(iMessage, "QuoteCancel", false, "FIXT.1.1", 1152,
                "FIX_CSV_ds1", "FGW");
        Assert.assertTrue("Message " + message.toString() + " is different from RawMessage " + raw.toString(),
                compareMessages(message, raw, 1461));
        Assert.assertTrue("RawMessage doesn't contain 1461=2", raw.toString().contains("1461=2"));
    }
}
