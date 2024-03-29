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
package com.exactpro.sf.configuration.dictionary;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Assert;
import org.junit.Test;

import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.loaders.XmlDictionaryStructureLoader;
import com.exactpro.sf.configuration.dictionary.interfaces.IDictionaryValidator;
import com.exactpro.sf.util.AbstractTest;

public class TestFIXDictionaryValidator extends AbstractTest {
    private final IDictionaryValidator dictionaryValidator = new FullFIXDictionaryValidatorFactory().createDictionaryValidator();

    @Test
    public void testFIXDictionaryValidatorPositive() throws IOException {
        InputStream in = getClass().getClassLoader().getResourceAsStream("dictionary/FIX50.TEST.xml");

        IDictionaryStructure dictionary = new XmlDictionaryStructureLoader().load(in);

        List<DictionaryValidationError> errors = dictionaryValidator.validate(dictionary, true, null);

        for (DictionaryValidationError error : errors) {
            System.err.println(error.getError());
        }
        Assert.assertEquals(0, errors.size());
    }

    @Test
    public void testFIXDictionaryValidatorNegative() throws IOException {
        InputStream in = getClass().getClassLoader().getResourceAsStream("dictionary/FIX50.TEST.ALL.ERRORS.xml");

        IDictionaryStructure dictionary = new XmlDictionaryStructureLoader().load(in);

        List<DictionaryValidationError> errors = dictionaryValidator.validate(dictionary, true, null);

        for (DictionaryValidationError error : errors) {
            System.err.println(error.getError());
        }
        String[] errorMessages = {
                "Field  <strong>\"FieldWithIncorrectFixType\"</strong>  contains  incorrect value <strong>\"INCORRECTTYPE\"</strong> for <strong>\"fixtype\"</strong> attribute",
                "Message  <strong>\"UndSecAltIDGrp_NoUnderlyingSecurityAltID\"</strong> doesn't contain fixtype attribute",
                "Field [1534] is missing in <fields> section for [CMRiskInstrumentScopesGrp_NoRiskInstrumentScopes] group",
                "Field [1671] is missing in <fields> section for [CMPartyDetailsGrp_NoPartyDetails] group",
                "Message  <strong>\"trailerInvalidName\"</strong> doesn't contain CheckSum field",
                "Message  <strong>\"UndlyInstrumentPtysSubGrp_NoUndlyInstrumentPartySubIDs\"</strong> doesn't contain tag attribute",
                "Field <strong>[DeleteReason]</strong> in <strong>[MarketDataIncrementalRefresh]</strong> message duplicated in <strong>[NoMDEntries]</strong>, <strong>[MDIncGrp]</strong>",
                "Message  <strong>\"MarketDataIncrementalRefresh\"</strong> doesn't contain header field",
                "Message  <strong>\"MarketDataIncrementalRefresh\"</strong> doesn't contain trailer field",
                "Message  <strong>\"QuoteCancel\"</strong> doesn't contain attributes",
                "Message  <strong>\"UndlyInstrumentParties_NoUndlyInstrumentParties\"</strong> doesn't contain <strong>\"entity_type\"</strong> attribute",
                "Message  <strong>\"UndSecAltIDGrp\"</strong> contains incorrect value <strong>\"InvalidComponent\"</strong> for <strong>\"entity_type\"</strong> attribute",
                "Message  <strong>\"OrderMassCancelRequest\"</strong> doesn't contain IsAdmin attribute",
                "Field <strong>[SecurityID]</strong> in <strong>[OrderMassCancelRequest]</strong> message duplicated in <strong>[NoPartyIDs]</strong>, <strong>[OrderMassCancelRequest]</strong>",
                "Field <strong>[ClOrdID]</strong> in <strong>[OrderMassCancelRequest]</strong> message duplicated in <strong>[Parties]</strong>, <strong>[OrderMassCancelRequest]</strong>",
                "Message  <strong>\"headerInvalidNumberOfFields\"</strong> doesn't contain MsgType field",
                "Message  <strong>\"headerInvalidNumberOfFields\"</strong> doesn't contain SenderCompID field",
                "Message  <strong>\"headerInvalidNumberOfFields\"</strong> doesn't contain TargetCompID field",
                "Message  <strong>\"headerInvalidNumberOfFields\"</strong> doesn't contain MsgSeqNum field",
                "Not enough fields to check order of fields in message <strong>\"headerInvalidNumberOfFields\"</strong>. " +
                "Checking the order of fields from 0 index. Number of fields to be validated: 3. Actual number of fields in message: 2. Expected number of fields in message: 3",
                "Message  <strong>\"OrderMassCancelRequest\"</strong> doesn't contain header field",
                "Message  <strong>\"OrderMassCancelRequest\"</strong> doesn't contain trailer field",
                "Field [1677] is missing in <fields> section for [CMPartyRisksLimitsGrp_NoPartyRisksLimits] group",
                "Message  <strong>\"UndInstrmtGrp_NoUnderlyings\"</strong> doesn't contain attributes",
                "Field [1669] is missing in <fields> section for [CMRiskLimitsGrp_NoRiskLimits] group",
                "Message  <strong>\"PartyRiskLimitsReport\"</strong> doesn't contain MessageType attribute",
                "Message  <strong>\"PartyRiskLimitsReport\"</strong> doesn't contain header field",
                "Message  <strong>\"PartyRiskLimitsReport\"</strong> doesn't contain trailer field",
                "Message  <strong>\"YieldData\"</strong> doesn't contain attributes",
                "Field [1529] is missing in <fields> section for [CMRiskLimitTypesGrp_NoRiskLimitTypes] group",
                "Message  <strong>\"headerInvalidName\"</strong> doesn't contain BeginString field",
                "Message  <strong>\"headerInvalidName\"</strong> doesn't contain BodyLength field",
                "Message  <strong>\"headerInvalidName\"</strong> doesn't contain MsgType field",
                "Message  <strong>\"headerInvalidName\"</strong> doesn't contain SenderCompID field",
                "Message  <strong>\"headerInvalidName\"</strong> doesn't contain TargetCompID field",
                "The message <strong>headerInvalidName</strong> must have a field <strong>BeginString</strong> at index 0 but has the <strong>FieldWithDupTagInMessage</strong> field",
                "The message <strong>headerInvalidName</strong> must have a field <strong>BodyLength</strong> at index 1 but has the <strong>OnBehalfOfCompID</strong> field",
                "The message <strong>headerInvalidName</strong> must have a field <strong>MsgType</strong> at index 2 but has the <strong>DeliverToCompID</strong> field",
                "Message  <strong>\"NewOrderList\"</strong> doesn't contain <strong>\"entity_type\"</strong> attribute",
                "Message  <strong>\"Heartbeat\"</strong> is missing in dictionary",
                "Message  <strong>\"TestRequest\"</strong> is missing in dictionary",
                "Message  <strong>\"Logon\"</strong> is missing in dictionary",
                "Message  <strong>\"Logout\"</strong> is missing in dictionary",
                "Message  <strong>\"Reject\"</strong> is missing in dictionary",
                "Message  <strong>\"ResendRequest\"</strong> is missing in dictionary",
                "Message  <strong>\"SequenceReset\"</strong> is missing in dictionary",
                "Message  <strong>\"header\"</strong> is missing in dictionary",
                "Message  <strong>\"trailer\"</strong> is missing in dictionary",
                "Value for 'tag' attribute is missing or have incorrect type for [MsgTypeGrp_NoMsgTypes] group",
                "Value for 'tag' attribute is missing or have incorrect type for [HopGrp_NoHops] group",
                "Duplicated tag 8 in filed FieldWithDupTagInMessage in message headerInvalidName",
                "Duplicated tag 8 in filed BeginString in message headerInvalidNumberOfFields",
                "Duplicated tag 386 in filed NoTradingSessionsDup in message TrdgSesGrp_Dup",
                "Duplicated tag 386 in filed NoTradingSessions in message TrdgSesGrp",
                "Duplicated tag 386 in filed NoTradingSessions",
                "Message for value [2] name [TAG_NOT_DEFINED_FOR_THIS_MESSAGE_TYPE] in MsgType is  missing in dictionary",
                "Field <strong>\"EncodedListExecInstLen\"</strong> must be of type <strong>\"LENGTH\"</strong> for FIX version > 4.2 or <strong>\"INT\"</strong> for FIX version <= 4.2 instead of <strong>\"STRING\"</strong>",
                "Message <strong>\"UnderlyingInstrument\"</strong> doesn't contain field with tag <strong>\"362\"</strong> and type <strong>\"LENGTH\"</strong>",
                //Special case for Signature which violates above assumption.
                //Field Signature with tag 89 and type DATA has a paired field with tag 93 and type LENGTH.
                "Message <strong>\"trailerInvalidName\"</strong> doesn't contain field with tag <strong>\"93\"</strong> and type <strong>\"LENGTH\"</strong>"
        };

        Assert.assertEquals(errorMessages.length, errors.size());

        for (int i = 0; i < errors.size(); i++) {
            Assert.assertTrue("Iteration " + i + " " + errors.get(i).getError(), ArrayUtils.contains(errorMessages, errors.get(i).getError()));
        }

    }
}
