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
package com.exactpro.sf.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.time.LocalDate;
import java.time.LocalTime;

import com.exactpro.sf.common.impl.messages.DefaultMessageFactory;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.loaders.IDictionaryStructureLoader;
import com.exactpro.sf.common.messages.structures.loaders.XmlDictionaryStructureLoader;
import com.exactpro.sf.services.fix.converter.DirtyQFJIMessageConverterTest;
import com.exactpro.sf.services.fix.converter.dirty.struct.RawMessage;

import quickfix.ConfigError;
import quickfix.DataDictionary;
import quickfix.Message;
import quickfix.field.BeginString;
import quickfix.field.BodyLength;
import quickfix.field.MsgSeqNum;
import quickfix.field.MsgType;
import quickfix.field.SenderCompID;
import quickfix.field.SendingTime;
import quickfix.field.TargetCompID;

public class ConverterTest extends AbstractTest {

    private final IMessageFactory messageFactory = DefaultMessageFactory.getFactory();
    private static IDictionaryStructureLoader loader = new XmlDictionaryStructureLoader();
    private static Map<String, DataDictionary> fixDixtionaries = new HashMap<>();
    private static Map<String, IDictionaryStructure> sfDictionaries = new HashMap<>();

    protected static DataDictionary getFixDictionary(String fileName) throws FileNotFoundException, IOException, ConfigError {
        DataDictionary result = fixDixtionaries.get(fileName);
        if (result == null) {
            try (InputStream inputStream = DirtyQFJIMessageConverterTest.class.getClassLoader().getResourceAsStream("dictionary/" + fileName)) {
                result = new DataDictionary(inputStream);
            }

            fixDixtionaries.put(fileName, result);
        }
        return result;
    }

    protected static IDictionaryStructure getSfDictionary(String fileName) throws FileNotFoundException, IOException {
        IDictionaryStructure result = sfDictionaries.get(fileName);
        if (result == null) {
            try (InputStream inputStream = DirtyQFJIMessageConverterTest.class.getClassLoader().getResourceAsStream("dictionary/" + fileName)) {
                result = loader.load(inputStream);
            }
            sfDictionaries.put(fileName, result);
        }
        return result;
    }

    protected IMessage createNDEntries() {
        IMessage imessage = messageFactory.createMessage("MDIncGrp_NoMDEntries", "FIX_5_0");
        imessage.addField("MDUpdateAction", '0');
        LocalDate date = DateTimeUtility.nowLocalDate();
        LocalTime time = DateTimeUtility.nowLocalTime();
        imessage.addField("MDEntryDate", date);
        imessage.addField("MDEntryTime", time);
        return imessage;
    }

    protected IMessage createIMessage() {
        List<IMessage> temp = new ArrayList<>();
        IMessage iMessage = messageFactory.createMessage("QuoteCancel", "FIX_5_0");
        IMessage header = messageFactory.createMessage("header", "FIX_5_0");
        IMessage trailer = messageFactory.createMessage("trailer", "FIX_5_0");
        IMessage targetParty = messageFactory.createMessage("TargetParty", "FIX_5_0");
        IMessage noTargetPartyIDs = messageFactory.createMessage("NoTargetPartyIDs", "FIX_5_0");
        IMessage noQuoteEntries = messageFactory.createMessage("NoQuoteEntries", "FIX_5_0");
        IMessage quotCxlEntriesGrp = messageFactory.createMessage("QuotCxlEntriesGrp", "FIX_5_0");

        iMessage.addField("QuoteCancelType", 4);

        trailer.addField("CheckSum", 169);
        iMessage.addField("trailer", trailer);

        noTargetPartyIDs.addField("TargetPartyRole", 76);
        noTargetPartyIDs.addField("TargetPartyID", "FIX_CSV_ds1");
        noTargetPartyIDs.addField("TargetPartyIDSource", "D");
        temp.add(noTargetPartyIDs);
        targetParty.addField("NoTargetPartyIDs", temp);
        iMessage.addField("TargetParty", targetParty);
        temp = new ArrayList<>();

        iMessage.addField("QuoteMsgID", "1444060022986");

        noQuoteEntries.addField("SecurityIDSource", 8);
        noQuoteEntries.addField("SecurityID", 7219943);
        noQuoteEntries.addField("QuoteEntryID", "test");
        temp.add(noQuoteEntries);
        quotCxlEntriesGrp.addField("NoQuoteEntries", temp);
        iMessage.addField("QuotCxlEntriesGrp", quotCxlEntriesGrp);

        header.addField("BeginString", "FIXT.1.1");
        header.addField("SendingTime", "20151005-18:47:02.785");
        header.addField("BodyLength", 155);
        header.addField("TargetCompID", "FGW");
        header.addField("SenderCompID", "FIX_CSV_ds1");
        header.addField("MsgSeqNum", 1152);
        header.addField("MsgType", "Z");
        iMessage.addField("header", header);
        return iMessage;
    }

    protected IMessage getAdditionalHeartbeat() {
        List<String> temp = new ArrayList<>();
        IMessage header = messageFactory.createMessage("header", "FIX_5_0");
        IMessage trailer = messageFactory.createMessage("trailer", "FIX_5_0");
        IMessage heartbeatTest = messageFactory.createMessage("HeartbeatTest", "FIX_5_0");
        header.addField("BeginString", "FIXT.1.1");
        header.addField("SendingTime", "20151005-18:47:02.785");
        header.addField("BodyLength", 155);
        header.addField("TargetCompID", "FGW");
        header.addField("SenderCompID", "FIX_CSV_ds1");
        header.addField("MsgSeqNum", 1152);
        header.addField("MsgType", "Z");
        heartbeatTest.addField("header", header);
        trailer.addField("CheckSum", 169);
        heartbeatTest.addField("trailer", trailer);
        temp.add("test1");
        temp.add("test2");
        temp.add("test3");
        heartbeatTest.addField("TestReqID", temp);
        return heartbeatTest;
    }

    protected boolean compareMessages(Message message, RawMessage raw) {
        return compareMessages(message, raw, true, true);
    }

    protected boolean compareMessages(Message message, RawMessage raw, int tag) {
        return compareMessages(message, raw, tag, false, true);
    }

    protected boolean compareMessages(Message message, RawMessage raw, boolean checkHeader, boolean checkTrailer) {
        return compareMessages(message, raw, 0, checkTrailer, checkHeader);
    }

    private boolean compareMessages(Message message, RawMessage raw, int tag, boolean checkTrailer, boolean checkHeader) {
        Map<Integer, List<String>> messageMap = getMapFromMessage(message.toString());
        Map<Integer, List<String>> rawMap = getMapFromMessage(raw.toString());
        for (int key : messageMap.keySet()) {
            if (messageMap.get(key) == null) {
                return false;
            }
            if (rawMap.get(key) == null) {
                return false;
            }

            // tag=10 is checksum
            if (key == tag || key == 10) {
                continue;
            }

            if (!checkHeader
                    && (key == MsgSeqNum.FIELD || key == MsgType.FIELD || key == BeginString.FIELD || key == BodyLength.FIELD
                            || key == SenderCompID.FIELD
                            || key == SendingTime.FIELD || key == TargetCompID.FIELD)) {
                /*
                 * if body contains header's field, then try to compare this
                 * field with original message's field
                 */
                if (messageMap.get(key).size() + 1 == rawMap.get(key).size()) {
                    if (!messageMap.get(key).get(0).equals(rawMap.get(key).get(1))) {
                        return false;
                    }
                }
                continue;
            }

            if (!checkTrailer && key == 10) {
                /*
                 * if body contains trailer's field, then try to compare this
                 * field with original message
                 */
                if (messageMap.get(key).size() + 1 == rawMap.get(key).size()) {
                    if (!messageMap.get(key).get(0).equals(rawMap.get(key).get(0))) {
                        return false;
                    }
                }
                continue;
            }

            if (messageMap.get(key).size() != rawMap.get(key).size()) {
                return false;
            }

            for (int i = 0; i < messageMap.get(key).size(); i++) {
                if (!messageMap.get(key).get(i).equals(rawMap.get(key).get(i))) {
                    return false;
                }
            }
        }
        return true;
    }

    protected String getDateFromMessage(RawMessage message, int typeDateTag) {
        Map<Integer, List<String>> messageMap = getMapFromMessage(message.toString());
        for (int key : messageMap.keySet()) {
            if (key == typeDateTag) {
                return messageMap.get(key).get(0);
            }
        }
        return null;
    }

    /**
     * For every tag we keep list of the values, because duplicate can be in the
     * message
     * 
     * @param message
     * @return
     */
    private Map<Integer, List<String>> getMapFromMessage(String message) {
        Map<Integer, List<String>> map = new HashMap<>();
        String[] array = message.split("");
        String[] temp;
        List<String> values;
        for (int i = 0; i < array.length; i++) {
            temp = array[i].split("=");
            if (map.get(Integer.valueOf(temp[0])) != null) {
                map.get(Integer.valueOf(temp[0])).add(temp[1]);
            } else {
                values = new ArrayList<>();
                values.add(temp[1]);
                map.put(Integer.valueOf(temp[0]), values);
            }
        }
        return map;
    }

}
