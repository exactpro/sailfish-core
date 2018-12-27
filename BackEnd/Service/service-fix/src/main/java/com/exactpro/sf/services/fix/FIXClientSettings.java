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
package com.exactpro.sf.services.fix;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.configuration.HierarchicalConfiguration;

import com.exactpro.sf.aml.Description;
import com.exactpro.sf.aml.InputMask;
import com.exactpro.sf.configuration.netdumper.NetDumperListenHost;
import com.exactpro.sf.configuration.netdumper.NetDumperListenPort;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.services.AbstractServiceSettings;
import com.exactpro.sf.services.RequiredParam;

@XmlRootElement
public class FIXClientSettings extends AbstractServiceSettings {

    private static final long serialVersionUID = 6706524622752557598L;

    @RequiredParam
	@Description("Directory to store sequence number and message files.")
	private String FileStorePath = "store/fix/sessions";
	@Description("Save Heartbeats to logs or not.")
	private boolean logHeartbeats;
	/*ConnectionType - add this param in client class*/
	@RequiredParam
	@Description("Host to connect to.<br>"
	        + "Valid IP address in the format of x.x.x.x or a domain name.")
	@NetDumperListenHost
	private String SocketConnectHost;
	@Description("For week long sessions, the starting day of week for the session. Use in combination with StartTime.<br>"
	        + "Day of week in English using any abbreviation (i.e. mo, mon, mond, monda, monday are all valid).")
	private String StartDate;
	@Description("For week long sessions, the ending day of week for the session. Use in combination with EndTime.<br>"
	        + "Day of week in English using any abbreviation (i.e. mo, mon, mond, monda, monday are all valid).")
	private String EndDate;
	@RequiredParam
    @Description("Time of day that this FIX session becomes activated.<br>"
            + "time in the format of HH:MM:SS, time is represented in UTC.")
    @InputMask("99:99:99")
    private String StartTime = "00:00:00";
	@RequiredParam
	@Description("Time of day that this FIX session becomes deactivated.<br>"
	        + "time in the format of HH:MM:SS, time is represented in UTC  ")
    @InputMask("99:99:99")
	private String EndTime = "00:00:00";
	@RequiredParam
	@Description("Heartbeat interval in seconds.<br>"
	        + "Positive integer.")
	private int HeartBtInt = 1;
	@Description("Time between reconnection attempts in seconds.<br>"
	        + "Positive integer")
	private int ReconnectInterval;
	@Description("Use tag DefaultApplVerID in Logon message or not")
	private boolean useDefaultApplVerID = true;
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
	private String DefaultApplVerID = "9";
	@Description("If set to checked, messages must be received from the counterparty within a defined number of seconds (see MaxLatency).<br>"
	        + "It is useful to turn this off if a system uses localtime for it's timestamps instead of GMT.")
	private boolean CheckLatency;
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
	private String BeginString;
	
	@RequiredParam
	@Description("Socket port for connecting to a session.\nPositive integer")
	@NetDumperListenPort
	private int SocketConnectPort;
	@RequiredParam
	@Description("Your ID as associated with this FIX session.")
	private String SenderCompID;
	@RequiredParam
	@Description("Counter parties ID as associated with this FIX session.")
	private String TargetCompID;
	@Description("Value of Username(553) tag used in Logon(A) message")
	private String Username;
	@Description("Value of Password(554) tag used in Logon(A) message")
	private String Password;
	@Description("Value of NewPassword(925) tag used in Logon(A) message")
	private String NewPassword;
	private boolean EncryptPassword = false;
	private String EncryptionKeyFilePath;
	@Description("Value of ResetSeqNumFlag(141) tag used in Logon(A) message")
	private String ResetSeqNumFlag = "true";
	@Description("If set to unchecked, user defined fields will not be rejected if they are not defined in the data dictionary, or are present in messages they do not belong to.")
	private boolean ValidateUserDefinedFields = true;
	@Description("If set to unchecked, fields that are out of order (i.e. body fields in the header, or header fields in the body) will not be rejected. Useful for connecting to systems which do not properly order fields.")
	private boolean ValidateFieldsOutOfOrder = true;
    @Description("If set to unchecked, messages which contain values not presented in the range will not be rejected.")
    private boolean ValidateFieldsOutOfRange = true;
	@Description("If set to unchecked, fields without values (empty) will not be rejected. Useful for connecting to systems which improperly send empty tags.")
	private boolean ValidateFieldsHaveValues = true;
	@Description("Determines if milliseconds should be added to timestamp fields. Only available for AML 3")
	private boolean millisecondsInTimeStampFields = true;
    @Description("Determines if microseconds should be added to timestamp fields. Only available for AML 3. This value is more priority than 'Milliseconds In Time Stamp Fields'")
    private boolean microsecondsInTimeStampFields = false;
	@Description("Number of seconds to wait for a logon response before disconnecting.")
	private int logonTimeout = 10;
    @Description("Number of seconds to wait for a logout response before disconnecting.")
    private int logoutTimeout = 10;
	@Description("Inactivity service timeout in seconds after which service will be shout down automatically. If 0 - do not shutdown service.")
	private int idleTimeout;
	@Description("Do logon when service started")
	private boolean doLogonOnStart = true;
	@Description("Client send reject if message is invalid")
	private boolean RejectInvalidMessage = true;
	@Description("Value of DefaultCstmApplVerID(1408) tag used in Logon(A) message")
	private String DefaultCstmApplVerID;
	@Description("Value of ExtExecInst(8718) tag used in Logon(A) message")
	private String ExtExecInst;
	@Description("If CheckLatency is set to checked, this defines the number of seconds latency allowed for a message to be processed. Default is 120.")
	private int MaxLatency = 120;
	@Description("Sender initial sequence number.\n"
	        + "It is applied when service started.")
	private Integer SeqNumSender;
	@Description("Target initial sequence number.\n"
	        + "It is applied when service started.")
	private Integer SeqNumTarget;
	@RequiredParam
	@Description("Dictionary title")
	private SailfishURI dictionaryName;
	@Description("If set to checked, no reject sent on incoming message with duplicate tags Allow duplicated tags.")
	private boolean duplicateTagsAllowed = false;
	//private String nsPrefix = "";
	@Description("If set to checked, allow unknown fields in messages. This is intended for unknown fields with tags < 5000\n" + 
	        "(not user defined fields)")
	private boolean allowUnknownMsgFields = false;
	@Description("Add NextExpectedMsgSeqNum(789) tag into Logon(A) message")
	private boolean addNextExpectedMsgSeqNum = false;
	@Description("Ignore if tag ResetSeqNumFlag(141) is not present in the received Logon(A) message")
	private boolean ignoreAbsenceOf141tag = false;
	@Description("Receive limit in bytes to emulate Slow Consumer")
	private int receiveLimit = 0;
	@Description("If set to checked, service does not store messages")
	private boolean performanceMode = false;
	@Description("Create new session after disconnecting on the server side")
	private boolean autorelogin = true;

	@Description("Determines if sequence numbers should be reset to 1 after a normal logout termination")
	private boolean resetOnLogout;

	@Description("Determines if sequence numbers should be reset to 1 after an abnormal termination")
	private boolean resetOnDisconnect;

	@Description("If specified, the session start and end will be converted from default zone to UTC")
	private boolean useLocalTime = false;

	@Description("If specified than we will check for OrigSendingTime in resend request")
	private boolean requiresOrigSendingTime = true;

	@Description("Check the next expected target SeqNum against the received SeqNum. Default is checked.\n"
	        + "If a mismatch is detected, apply the following logic:\n"
	        + "\t * if lower than expected SeqNum , logout\n"
	        + "\t * if higher, send a resend request")
	private boolean validateSequenceNumbers = true;
	@Description("Enables SSL usage for QFJ acceptor or initiator")
	private boolean useSSL = false;
	@Description("KeyStore to use with SSL")
	private String sslKeyStore;//TODO: Use Alias for DataManager
	@Description("KeyStore password to use with SSL")
	private String sslKeyStorePassword;
	@Description("Controls which particular protocols for secure connection are enabled for handshake")
	private String sslEnabledProtocols;
	@Description("Controls which particular SSL cipher suites are enabled for secure connection")
	private String sslCipherSuites;
	@Description("Enables fields ordering in raw message by dictionary")
	private boolean orderingFields = false;

    @Override
	public void load(HierarchicalConfiguration config)
	{
		// do nothing
	}

	public int getLogonTimeout() {
		return logonTimeout;
	}

	public void setLogonTimeout(int logonTimeout) {
		this.logonTimeout = logonTimeout;
	}

	public int getIdleTimeout() {
		return idleTimeout;
	}

	public void setIdleTimeout(int idleTimeout) {
		this.idleTimeout = idleTimeout;
	}

	public String getFileStorePath() {
		return FileStorePath;
	}

	public void setFileStorePath(String fileStorePath) {
		FileStorePath = fileStorePath;
	}

	public boolean isLogHeartbeats() {
		return logHeartbeats;
	}

	public void setLogHeartbeats(boolean logHeartbeats) {
		this.logHeartbeats = logHeartbeats;
	}

	public String getSocketConnectHost() {
		return SocketConnectHost;
	}

	public void setSocketConnectHost(String socketConnectHost) {
		SocketConnectHost = socketConnectHost;
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

	public int getHeartBtInt() {
		return HeartBtInt;
	}

	public void setHeartBtInt(int heartBtInt) {
		HeartBtInt = heartBtInt;
	}

	public int getReconnectInterval() {
		return ReconnectInterval;
	}

	public void setReconnectInterval(int reconnectInterval) {
		ReconnectInterval = reconnectInterval;
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


	public String getBeginString() {
		return BeginString;
	}

	public void setBeginString(String beginString) {
		BeginString = beginString;
	}

	public int getSocketConnectPort() {
		return SocketConnectPort;
	}

	public void setSocketConnectPort(int socketConnectPort) {
		SocketConnectPort = socketConnectPort;
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

	public String getUsername() {
		return Username;
	}

	public void setUsername(String username) {
		Username = username;
	}

	public String getPassword() {
		return Password;
	}

	public void setPassword(String password) {
		Password = password;
	}

	public String getNewPassword() {
		return NewPassword;
	}

	public void setNewPassword(String newPassword) {
		NewPassword = "".equals(newPassword) ? null : newPassword;
	}

	public boolean isEncryptPassword() {
		return EncryptPassword;
	}

	public void setEncryptPassword(boolean EncryptPassword) {
		this.EncryptPassword = EncryptPassword;
	}

	public String getEncryptionKeyFilePath() {
		return EncryptionKeyFilePath;
	}

	public void setEncryptionKeyFilePath(String encryptionKeyFilePath) {
		EncryptionKeyFilePath = encryptionKeyFilePath;
	}

	public String getResetSeqNumFlag() {
		return ResetSeqNumFlag;
	}

	// Determines if sequence numbers should be reset when recieving a logon request. Acceptors only.
	public void setResetSeqNumFlag(String resetSeqNumFlag) {
		ResetSeqNumFlag = resetSeqNumFlag;
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

	public boolean isValidateFieldsHaveValues() {
		return ValidateFieldsHaveValues;
	}

	public void setValidateFieldsHaveValues(boolean validateFieldsHaveValues) {
		ValidateFieldsHaveValues = validateFieldsHaveValues;
	}

    public boolean isMillisecondsInTimeStampFields() {
        return millisecondsInTimeStampFields;
    }

    public void setMillisecondsInTimeStampFields(
            boolean millisecondsInTimeStampFields) {
        this.millisecondsInTimeStampFields = millisecondsInTimeStampFields;
    }

	public boolean isDoLogonOnStart() {
		return doLogonOnStart;
	}

	public void setDoLogonOnStart(boolean doLogonOnStart) {
		this.doLogonOnStart = doLogonOnStart;
	}

	public boolean isRejectInvalidMessage() {
		return RejectInvalidMessage;
	}

	public void setRejectInvalidMessage(boolean rejectInvalidMessage) {
		RejectInvalidMessage = rejectInvalidMessage;
	}

	public String getDefaultCstmApplVerID() {
		return DefaultCstmApplVerID;
	}

	public void setDefaultCstmApplVerID(String str) {
		DefaultCstmApplVerID = str;
	}

	public String getExtExecInst() {
		return ExtExecInst;
	}

	public void setExtExecInst(String str) {
		ExtExecInst = str;
	}

	public Integer getSeqNumSender() {
		return SeqNumSender;
	}

	public void setSeqNumSender(Integer seqNumSender) {
		SeqNumSender = seqNumSender;
	}

	public Integer getSeqNumTarget() {
		return SeqNumTarget;
	}

	public void setSeqNumTarget(Integer seqNumTarget) {
		SeqNumTarget = seqNumTarget;
	}

	public int getMaxLatency() {
		return MaxLatency;
	}

	public void setMaxLatency(int maxLatency) {
		MaxLatency = maxLatency;
	}

	public boolean isUseDefaultApplVerID() {
		return useDefaultApplVerID;
	}

	public void setUseDefaultApplVerID(boolean useDefaultApplVerID) {
		this.useDefaultApplVerID = useDefaultApplVerID;
	}

	@Override
	public SailfishURI getDictionaryName() {
		return dictionaryName;
	}

	@Override
	public void setDictionaryName(SailfishURI dictionaryName) {
		this.dictionaryName = dictionaryName;
	}

	public boolean isDuplicateTagsAllowed() {
		return duplicateTagsAllowed;
	}

	public void setDuplicateTagsAllowed(boolean duplicateTagsAllowed) {
		this.duplicateTagsAllowed = duplicateTagsAllowed;
	}

	public boolean isAllowUnknownMsgFields() {
		return allowUnknownMsgFields;
	}

	public void setAllowUnknownMsgFields(boolean allowUnknownMsgFields) {
		this.allowUnknownMsgFields = allowUnknownMsgFields;
	}


	public boolean isAddNextExpectedMsgSeqNum() {
		return addNextExpectedMsgSeqNum;
	}

	public void setAddNextExpectedMsgSeqNum(boolean addNextExpectedMsgSeqNum) {
		this.addNextExpectedMsgSeqNum = addNextExpectedMsgSeqNum;
	}

	public boolean isIgnoreAbsenceOf141tag() {
		return ignoreAbsenceOf141tag;
	}

	public void setIgnoreAbsenceOf141tag(boolean ignoreAbsenceOf141tag) {
		this.ignoreAbsenceOf141tag = ignoreAbsenceOf141tag;
	}

	public int getReceiveLimit() {
		return receiveLimit;
	}

	public void setReceiveLimit(int receiveLimit) {
		this.receiveLimit = receiveLimit;
	}

	public boolean isPerformanceMode() {
		return performanceMode;
	}

	public void setPerformanceMode(boolean performanceMode) {
		this.performanceMode = performanceMode;
	}

	public boolean isResetOnLogout() {
		return this.resetOnLogout;
	}

	public void setResetOnLogout(boolean resetOnLogout) {
		this.resetOnLogout = resetOnLogout;
	}

	public boolean isResetOnDisconnect() {
		return this.resetOnDisconnect;
	}

	public void setResetOnDisconnect(boolean resetOnDisconnect) {
		this.resetOnDisconnect = resetOnDisconnect;
	}

	public boolean isAutorelogin() {
		return autorelogin;
	}

	public void setAutorelogin(boolean logonAfterServerLogout) {
		this.autorelogin = logonAfterServerLogout;
	}

    public String getStartDate() {
        return StartDate;
    }

    public String getEndDate() {
        return EndDate;
    }

    public void setStartDate(String startDate) {
        StartDate = startDate;
    }

    public void setEndDate(String endDate) {
        EndDate = endDate;
    }

    public int getLogoutTimeout() {
        return logoutTimeout;
    }

    public void setLogoutTimeout(int logoutTimeout) {
        this.logoutTimeout = logoutTimeout;
    }

	public boolean isUseLocalTime() { return useLocalTime;	}

	public void setUseLocalTime(boolean useLocalTime) {	this.useLocalTime = useLocalTime; }

    public boolean isRequiresOrigSendingTime() {
        return requiresOrigSendingTime;
    }

    public void setRequiresOrigSendingTime(boolean requiresOrigSendingTime) {
        this.requiresOrigSendingTime = requiresOrigSendingTime;
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

    public boolean isValidateFieldsOutOfRange() {
        return ValidateFieldsOutOfRange;
    }

    public void setValidateFieldsOutOfRange(boolean validateFieldsOutOfRange) {
        ValidateFieldsOutOfRange = validateFieldsOutOfRange;
    }

    public boolean isMicrosecondsInTimeStampFields() {
        return microsecondsInTimeStampFields;
    }

    public void setMicrosecondsInTimeStampFields(boolean microsecondsInTimeStampFields) {
        this.microsecondsInTimeStampFields = microsecondsInTimeStampFields;
    }
    
    public boolean isValidateSequenceNumbers() {
        return validateSequenceNumbers;
    }
    public void setValidateSequenceNumbers(boolean validateSequenceNumbers) {
        this.validateSequenceNumbers = validateSequenceNumbers;
    }
    
    public boolean isOrderingFields() {
        return orderingFields;
    }

    public void setOrderingFields(boolean orderingFields) {
        this.orderingFields = orderingFields;
    }
}