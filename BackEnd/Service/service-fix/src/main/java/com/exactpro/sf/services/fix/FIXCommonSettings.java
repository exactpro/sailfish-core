/******************************************************************************
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
package com.exactpro.sf.services.fix;

import com.exactpro.sf.aml.Description;
import com.exactpro.sf.aml.EnumeratedValues;
import com.exactpro.sf.aml.Ignore;
import com.exactpro.sf.aml.InputMask;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.externalapi.DictionaryProperty;
import com.exactpro.sf.externalapi.DictionaryType;
import com.exactpro.sf.services.AbstractServiceSettings;
import com.exactpro.sf.services.RequiredParam;

public class FIXCommonSettings extends AbstractServiceSettings {
    @Ignore
    protected Class<?> application;

    @RequiredParam
    @Description("Version of FIX this session should use.\n"
            + "<table border=0 cellspacing=3 cellpadding=0>"
            + "    <tr bgcolor=\"#ccccff\" align=\"left\"><th>Value"
            + "    <tr bgcolor=\"#eeeeff\"><td><code>FIX.4.0</code>"
            + "    <tr bgcolor=\"#eeeeff\"><td><code>FIX.4.1</code>"
            + "    <tr bgcolor=\"#eeeeff\"><td><code>FIX.4.2</code>"
            + "    <tr bgcolor=\"#eeeeff\"><td><code>FIX.4.3</code>"
            + "    <tr bgcolor=\"#eeeeff\"><td><code>FIX.4.4</code>"
            + "    <tr bgcolor=\"#eeeeff\"><td><code>FIXT.1.1</code>"
            + "</table>")
    @EnumeratedValues({"FIX.4.0", "FIX.4.1", "FIX.4.2", "FIX.4.3", "FIX.4.4", "FIXT.1.1"})
    protected String BeginString;

    @RequiredParam
    @Description("Your ID as associated with this FIX session.")
    protected String SenderCompID;

    @RequiredParam
    @Description("Counter parties ID as associated with this FIX session.")
    protected String TargetCompID;

    @RequiredParam
    @Description("Directory to store sequence number and message files.")
    protected String FileStorePath = "store/fix/sessions";

    @RequiredParam
    @Description("Time of day that this FIX session becomes activated.<br>"
            + "time in the format of HH:MM:SS, time is represented in UTC.<br>"
            + "If the session creation time is not between StartTime and EndTime the session state will be reset "
            + "(incoming / outgoing sequences and message cache will be drop).<br>"
            + "The bounds move daily. That means next day after the start at 'EndTime' the bounds will be moved")
    @InputMask("99:99:99")
    protected String StartTime = "00:00:00";

    @RequiredParam
    @Description("Time of day that this FIX session becomes deactivated.<br>"
            + "time in the format of HH:MM:SS, time is represented in UTC.<br>"
            + "If the session creation time is not between StartTime and EndTime the session state will be reset "
            + "(incoming / outgoing sequences and message cache will be drop).<br>"
            + "The bounds move daily. That means next day after the start at 'EndTime' the bounds will be moved")
    @InputMask("99:99:99")
    protected String EndTime = "00:00:00";

    @RequiredParam
    @Description("Specifies the default application version ID for the session.<br>"
            + "This can either be the ApplVerID enum (see the ApplVerID field) or the BeginString for the default version.<br>"
            + "<table border=0 cellspacing=3 cellpadding=0>"
            + "    <tr bgcolor=\"#ccccff\" align=\"left\"><th>Value<th>FIX version"
            + "    <tr bgcolor=\"#eeeeff\"><td><code>2</code><td>FIX40"
            + "    <tr bgcolor=\"#eeeeff\"><td><code>3</code><td>FIX41"
            + "    <tr bgcolor=\"#eeeeff\"><td><code>4</code><td>FIX42"
            + "    <tr bgcolor=\"#eeeeff\"><td><code>5</code><td>FIX43"
            + "    <tr bgcolor=\"#eeeeff\"><td><code>6</code><td>FIX44"
            + "    <tr bgcolor=\"#eeeeff\"><td><code>7</code><td>FIX50"
            + "    <tr bgcolor=\"#eeeeff\"><td><code>8</code><td>FIX50SP1"
            + "    <tr bgcolor=\"#eeeeff\"><td><code>9</code><td>FIX50SP2"
            + "</table>")
    @EnumeratedValues({"2", "3", "4", "5", "6", "7", "8", "9"})
    protected String DefaultApplVerID = "9";

    @RequiredParam
    @Description("Dictionary title")
    @DictionaryProperty(type = DictionaryType.MAIN)
    protected SailfishURI dictionaryName;

    @Description("Determines if sequence numbers should be reset to 1 after a normal logout termination")
    protected boolean resetOnLogout;

    @Description("Determines if sequence numbers should be reset to 1 after an abnormal termination")
    protected boolean resetOnDisconnect;

    @Description("Save Heartbeats to logs or not.")
    protected boolean logHeartbeats;

    @Description("Check required tags or not.")
    protected boolean checkRequiredTags = true;

    @Description("If set to checked, messages must be received from the counterparty within a defined number of seconds (see MaxLatency).<br>"
            + "It is useful to turn this off if a system uses localtime for it's timestamps instead of GMT.")
    protected boolean CheckLatency;

    @Description("If CheckLatency is set to checked, this defines the number of seconds latency allowed for a message to be processed. Default is 120.")
    protected int MaxLatency = 120;

    @Description("If set to checked, allow unknown fields in messages. This is intended for unknown fields with tags < 5000\n" +
            "(not user defined fields)")
    protected boolean allowUnknownMsgFields;

    @Description("Determines if milliseconds should be added to timestamp fields. Only available for AML 3")
    protected boolean millisecondsInTimeStampFields = true;

    @Description("Determines if microseconds should be added to timestamp fields. Only available for AML 3. This value is more priority than 'Milliseconds In Time Stamp Fields'")
    protected boolean microsecondsInTimeStampFields;

    @Description("Receive limit in bytes to emulate Slow Consumer")
    protected int receiveLimit;

    @Description("Client send reject if message is invalid")
    protected boolean RejectInvalidMessage = true;
    
    @Description("If set to unchecked, user defined fields will not be rejected if they are not defined in the data dictionary, or are present in messages they do not belong to.")
    protected boolean ValidateUserDefinedFields = true;

    @Description("If set to unchecked, fields that are out of order (i.e. body fields in the header, or header fields in the body) will not be rejected. Useful for connecting to systems which do not properly order fields.")
    protected boolean ValidateFieldsOutOfOrder = true;

    @Description("If set to unchecked, messages which contain values not presented in the range will not be rejected.")
    protected boolean ValidateFieldsOutOfRange = true;

    @Description("If set to unchecked, fields without values (empty) will not be rejected. Useful for connecting to systems which improperly send empty tags.")
    protected boolean ValidateFieldsHaveValues = true;

    @Description("If set to checked, no reject sent on incoming message with duplicate tags Allow duplicated tags.")
    protected boolean duplicateTagsAllowed;

    @Description("If specified, the session start and end will be converted from default zone to UTC")
    protected boolean useLocalTime;

    @Description("Enables SSL usage for QFJ acceptor or initiator")
    protected boolean useSSL;

    @Description("KeyStore to use with SSL")
    protected String sslKeyStore;//TODO: Use Alias for DataManager

    @Description("KeyStore password to use with SSL")
    protected String sslKeyStorePassword;

    @Description("Controls which particular protocols for secure connection are enabled for handshake")
    protected String sslEnabledProtocols;

    @Description("Controls which particular SSL cipher suites are enabled for secure connection")
    protected String sslCipherSuites;

    @Description("Enables fields ordering in raw message by dictionary")
    protected boolean orderingFields;

    @Description("Check the next expected target SeqNum against the received SeqNum. Default is checked.\n"
            + "If a mismatch is detected, apply the following logic:\n"
            + "\t * if lower than expected SeqNum , logout\n"
            + "\t * if higher, send a resend request")
    protected boolean validateSequenceNumbers = true;

    @Description("SenderSubId value. If it is empty it won't be set in the sent message's header")
    protected String senderSubID;

    @Description("TargetSubId value. If it is empty it won't be set in the sent message's header")
    protected String targetSubID;

    public String getBeginString() {
        return BeginString;
    }

    public void setBeginString(String beginString) {
        BeginString = beginString;
    }

    public String getSenderCompID() {
        return SenderCompID;
    }

    public void setSenderCompID(String senderCompID) {
        SenderCompID = senderCompID;
    }

    public String getTargetCompID() {
        return TargetCompID;
    }

    public void setTargetCompID(String targetCompID) {
        TargetCompID = targetCompID;
    }

    @Override
    public SailfishURI getDictionaryName() {
        return dictionaryName;
    }

    @Override
    public void setDictionaryName(SailfishURI dictionaryName) {
        this.dictionaryName = dictionaryName;
    }

    public boolean isResetOnLogout() {
        return resetOnLogout;
    }

    public void setResetOnLogout(boolean resetOnLogout) {
        this.resetOnLogout = resetOnLogout;
    }

    public boolean isResetOnDisconnect() {
        return resetOnDisconnect;
    }

    public void setResetOnDisconnect(boolean resetOnDisconnect) {
        this.resetOnDisconnect = resetOnDisconnect;
    }

    public String getFileStorePath() {
        return FileStorePath;
    }

    public void setFileStorePath(String fileStorePath) {
        FileStorePath = fileStorePath;
    }

    public String getStartTime() {
        return StartTime;
    }

    public void setStartTime(String startTime) {
        StartTime = startTime;
    }

    public String getEndTime() {
        return EndTime;
    }

    public void setEndTime(String endTime) {
        EndTime = endTime;
    }

    public String getDefaultApplVerID() {
        return DefaultApplVerID;
    }

    public void setDefaultApplVerID(String defaultApplVerID) {
        DefaultApplVerID = defaultApplVerID;
    }

    public boolean isCheckLatency() {
        return CheckLatency;
    }

    public void setCheckLatency(boolean checkLatency) {
        CheckLatency = checkLatency;
    }

    public int getMaxLatency() {
        return MaxLatency;
    }

    public void setMaxLatency(int maxLatency) {
        MaxLatency = maxLatency;
    }

    public boolean isAllowUnknownMsgFields() {
        return allowUnknownMsgFields;
    }

    public void setAllowUnknownMsgFields(boolean allowUnknownMsgFields) {
        this.allowUnknownMsgFields = allowUnknownMsgFields;
    }

    public boolean isMillisecondsInTimeStampFields() {
        return millisecondsInTimeStampFields;
    }

    public void setMillisecondsInTimeStampFields(boolean millisecondsInTimeStampFields) {
        this.millisecondsInTimeStampFields = millisecondsInTimeStampFields;
    }

    public boolean isMicrosecondsInTimeStampFields() {
        return microsecondsInTimeStampFields;
    }

    public void setMicrosecondsInTimeStampFields(boolean microsecondsInTimeStampFields) {
        this.microsecondsInTimeStampFields = microsecondsInTimeStampFields;
    }

    public int getReceiveLimit() {
        return receiveLimit;
    }

    public void setReceiveLimit(int receiveLimit) {
        this.receiveLimit = receiveLimit;
    }

    public boolean isValidateUserDefinedFields() {
        return ValidateUserDefinedFields;
    }

    public void setValidateUserDefinedFields(boolean validateUserDefinedFields) {
        ValidateUserDefinedFields = validateUserDefinedFields;
    }

    public boolean isValidateFieldsOutOfOrder() {
        return ValidateFieldsOutOfOrder;
    }

    public void setValidateFieldsOutOfOrder(boolean validateFieldsOutOfOrder) {
        ValidateFieldsOutOfOrder = validateFieldsOutOfOrder;
    }

    public boolean isValidateFieldsOutOfRange() {
        return ValidateFieldsOutOfRange;
    }

    public void setValidateFieldsOutOfRange(boolean validateFieldsOutOfRange) {
        ValidateFieldsOutOfRange = validateFieldsOutOfRange;
    }

    public boolean isValidateFieldsHaveValues() {
        return ValidateFieldsHaveValues;
    }

    public void setValidateFieldsHaveValues(boolean validateFieldsHaveValues) {
        ValidateFieldsHaveValues = validateFieldsHaveValues;
    }

    public boolean isDuplicateTagsAllowed() {
        return duplicateTagsAllowed;
    }

    public void setDuplicateTagsAllowed(boolean duplicateTagsAllowed) {
        this.duplicateTagsAllowed = duplicateTagsAllowed;
    }

    public boolean isLogHeartbeats() {
        return logHeartbeats;
    }

    public void setLogHeartbeats(boolean logHeartbeats) {
        this.logHeartbeats = logHeartbeats;
    }

    public boolean isCheckRequiredTags() {
        return checkRequiredTags;
    }

    public void setCheckRequiredTags(boolean checkRequiredTags) {
        this.checkRequiredTags = checkRequiredTags;
    }

    public boolean isUseLocalTime() {
        return useLocalTime;
    }

    public void setUseLocalTime(boolean useLocalTime) {
        this.useLocalTime = useLocalTime;
    }

    public boolean isUseSSL() {
        return useSSL;
    }

    public void setUseSSL(boolean useSSL) {
        this.useSSL = useSSL;
    }

    public String getSslKeyStore() {
        return sslKeyStore;
    }

    public void setSslKeyStore(String sslKeyStore) {
        this.sslKeyStore = sslKeyStore;
    }

    public String getSslKeyStorePassword() {
        return sslKeyStorePassword;
    }

    public void setSslKeyStorePassword(String sslKeyStorePassword) {
        this.sslKeyStorePassword = sslKeyStorePassword;
    }

    public String getSslEnabledProtocols() {
        return sslEnabledProtocols;
    }

    public void setSslEnabledProtocols(String sslEnabledProtocols) {
        this.sslEnabledProtocols = sslEnabledProtocols;
    }

    public String getSslCipherSuites() {
        return sslCipherSuites;
    }

    public void setSslCipherSuites(String sslCipherSuites) {
        this.sslCipherSuites = sslCipherSuites;
    }

    public boolean isOrderingFields() {
        return orderingFields;
    }

    public void setOrderingFields(boolean orderingFields) {
        this.orderingFields = orderingFields;
    }

    public boolean isRejectInvalidMessage() {
        return RejectInvalidMessage;
    }

    public void setRejectInvalidMessage(boolean rejectInvalidMessage) {
        RejectInvalidMessage = rejectInvalidMessage;
    }

    public boolean isValidateSequenceNumbers() {
        return validateSequenceNumbers;
    }

    public void setValidateSequenceNumbers(boolean validateSequenceNumbers) {
        this.validateSequenceNumbers = validateSequenceNumbers;
    }

    public String getSenderSubID() {
        return senderSubID;
    }

    public void setSenderSubID(String senderSubID) {
        this.senderSubID = senderSubID;
    }

    public String getTargetSubID() {
        return targetSubID;
    }

    public void setTargetSubID(String targetSubID) {
        this.targetSubID = targetSubID;
    }

    public void setApplicationClass(String clazz) {
        if (clazz == null) {
            return;
        }
        try {
            this.application = Class.forName(clazz);
        } catch (ClassNotFoundException e) {
            throw new EPSCommonException("Cannot load application class: "+clazz, e);
        }
    }

    public Class<?> getApplicationClass() {
        return application;
    }
}
