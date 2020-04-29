/*******************************************************************************
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
 ******************************************************************************/

package com.exactpro.sf.configuration.factory;

import java.io.InputStream;

import org.junit.Assert;
import org.junit.Test;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.MessageUtil;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.loaders.XmlDictionaryStructureLoader;
import com.exactpro.sf.storage.util.JsonIMessageDecoder;
import com.exactpro.sf.storage.util.JsonMessageDecoder;

public class TestFIXHumanMessage {

    @Test
    public void testDirtyFIXHumanFormat() throws Exception {
        String msg = "{\n"
                + "    \"id\": 2165752,\n"
                + "    \"timestamp\": 1570183516009,\n"
                + "    \"name\": \"NewOrderSingle\",\n"
                + "    \"namespace\": \"FIX_5_0\",\n"
                + "    \"dictionaryURI\": \"FIX_5_0\",\n"
                + "    \"protocol\": \"FIX\",\n"
                + "    \"rejectReason\": null,\n"
                + "    \"admin\": false,\n"
                + "    \"dirty\": true,\n"
                + "    \"message\": {\n"
                + "        \"AccountType\": \"3\",\n"
                + "        \"ClOrdID\": \"excluded field\",\n"
                + "        \"DisplayQty\": \"769\",\n"
                + "        \"OrdType\": \"1\",\n"
                + "        \"OrderCapacity\": \"R\",\n"
                + "        \"OrderQty\": \"896\",\n"
                + "        \"SecurityID\": \"8011001\",\n"
                + "        \"SecurityIDSource\": \"8\",\n"
                + "        \"Side\": \"2\",\n"
                + "        \"TradingParty\": {\n"
                + "            \"NoPartyIDs\": [\n"
                + "                {\n"
                + "                    \"PartyID\": \"NGALL1FX01\",\n"
                + "                    \"PartyIDSource\": \"D\",\n"
                + "                    \"PartyRole\": \"76\"\n"
                + "                },\n"
                + "                {\n"
                + "                    \"PartyID\": \"0\",\n"
                + "                    \"PartyIDSource\": \"P\",\n"
                + "                    \"PartyRole\": \"3\"\n"
                + "                },\n"
                + "                {\n"
                + "                    \"PartyID\": \"3\",\n"
                + "                    \"PartyIDSource\": \"P\",\n"
                + "                    \"PartyRole\": \"12\"\n"
                + "                },\n"
                + "                {\n"
                + "                    \"PartyID\": \"0\",\n"
                + "                    \"PartyIDSource\": \"P\",\n"
                + "                    \"PartyRole\": \"122\"\n"
                + "                }\n"
                + "            ]\n"
                + "        },\n"
                + "        \"TransactTime\": \"20180205-10:38:08.000008\",\n"
                + "        \"header\": {\n"
                + "            \"ApplVerID\": \"9\",\n"
                + "            \"BeginString\": \"FIXT.1.1\",\n"
                + "            \"BodyLength\": \"239\",\n"
                + "            \"MsgSeqNum\": \"2\",\n"
                + "            \"MsgType\": \"D\",\n"
                + "            \"SenderCompID\": \"NGALL1FX01\",\n"
                + "            \"SendingTime\": \"20191004-10:05:16.010000\",\n"
                + "            \"TargetCompID\": \"FGW\"\n"
                + "        },\n"
                + "        \"trailer\": {\n"
                + "            \"CheckSum\": \"055\"\n"
                + "        }\n"
                + "    }\n"
                + "}";


        IDictionaryStructure dictionary;
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream("dictionary/FIX50.TEST.xml")) {
            dictionary = new XmlDictionaryStructureLoader().load(stream);
        }

        JsonMessageDecoder<IMessage> jsonMessageDecoder = new JsonIMessageDecoder(null);

        IMessage iMessage = jsonMessageDecoder.decode(msg, true);

        Assert.assertEquals("header={BeginString(8)=FIXT.1.1; BodyLength(9)=239; MsgType(35)=ORDER_SINGLE(D); SenderCompID(49)=NGALL1FX01; TargetCompID(56)=FGW; MsgSeqNum(34)=2; "
                + "SendingTime(52)=20191004-10:05:16.010000; ApplVerID(1128)=FIX50SP2(9)}; ClOrdID(11)=excluded field; TradingParty={NoPartyIDs(453)=[{PartyID(448)=NGALL1FX01; "
                + "PartyIDSource(447)=PROPRIETARY_CUSTOM_CODE(D); PartyRole(452)=DESK_ID(76)}; {PartyID(448)=0; PartyIDSource(447)=P; PartyRole(452)=CLIENT_ID(3)}; {PartyID(448)=3; PartyIDSource(447)=P; PartyRole(452)=EXECUTING_TRADER(12)}; {PartyID(448)=0; "
                + "PartyIDSource(447)=P; PartyRole(452)=122}]}; SecurityID(48)=8011001; SecurityIDSource(22)=EXCHANGE_SYMBOL(8); OrdType(40)=MARKET(1); Side(54)=SELL(2); OrderQty(38)=896; DisplayQty(1138)=769; AccountType(581)=HOUSE(3); OrderCapacity(528)=RISKLESS_PRINCIPAL(R); "
                + "TransactTime(60)=20180205-10:38:08.000008; trailer={CheckSum(10)=055}",
                MessageUtil.convertToIHumanMessage(new FixMessageFactory(), dictionary.getMessages().get(iMessage.getName()), iMessage).toString());
    }
}