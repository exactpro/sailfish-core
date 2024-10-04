/******************************************************************************
 * Copyright 2009-2024 Exactpro (Exactpro Systems Limited)
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

import org.apache.commons.configuration2.HierarchicalConfiguration;

import com.exactpro.sf.aml.Description;
import com.exactpro.sf.configuration.netdumper.NetDumperListenHost;
import com.exactpro.sf.configuration.netdumper.NetDumperListenPort;
import com.exactpro.sf.services.RequiredParam;
import org.apache.commons.configuration2.tree.ImmutableNode;

@XmlRootElement
public class FIXClientSettings extends FIXCommonSettings {

    private static final long serialVersionUID = 6706524622752557598L;

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
	@Description("Heartbeat interval in seconds.<br>"
	        + "Positive integer.")
	private int HeartBtInt = 1;

	@Description("Time between reconnection attempts in seconds.<br>"
	        + "Positive integer")
	private int ReconnectInterval;

	@Description("Use tag DefaultApplVerID in Logon message or not")
	private boolean useDefaultApplVerID = true;
	
	@RequiredParam
	@Description("Socket port for connecting to a session.\nPositive integer")
	@NetDumperListenPort
	private int SocketConnectPort;

	@Description("Value of Username(553) tag used in Logon(A) message")
	private String Username;

	@Description("Value of Password(554) tag used in Logon(A) message")
	private String Password;

	@Description("Value of NewPassword(925) tag used in Logon(A) message")
	private String NewPassword;

	private boolean EncryptPassword;

	private String EncryptionKeyFilePath;

	@Description("Value of ResetSeqNumFlag(141) tag used in Logon(A) message. If the value is not set the tag won't be sent in the Logon")
	private String ResetSeqNumFlag = "true";

    @Description("Number of seconds to wait for a logon response before disconnecting.")
    private int logonTimeout = 10;

    @Description("Number of seconds to wait for a logout response before disconnecting.")
    private int logoutTimeout = 10;

    @Description("Inactivity service timeout in seconds after which service will be shout down automatically. If 0 - do not shutdown service.")
    private int idleTimeout;

	@Description("Do logon when service started")
	private boolean doLogonOnStart = true;

	@Description("Value of DefaultCstmApplVerID(1408) tag used in Logon(A) message")
	private String DefaultCstmApplVerID;

	@Description("Value of ExtExecInst(8718) tag used in Logon(A) message")
	private String ExtExecInst;

	@Description("Sender initial sequence number.\n"
	        + "It is applied when service started.")
	private Integer SeqNumSender;

	@Description("Target initial sequence number.\n"
	        + "It is applied when service started.")
	private Integer SeqNumTarget;

	@Description("Add NextExpectedMsgSeqNum(789) tag into Logon(A) message")
	private boolean addNextExpectedMsgSeqNum;

	@Description("Ignore if tag ResetSeqNumFlag(141) is not present in the received Logon(A) message")
	private boolean ignoreAbsenceOf141tag;

	@Description("If set to checked, service does not store messages")
	private boolean performanceMode;

	@Description("Create new session after disconnecting on the server side")
	private boolean autorelogin = true;

	@Description("If specified than we will check for OrigSendingTime in resend request")
	private boolean requiresOrigSendingTime = true;

	@Description("Response To Resend Request By Heartbeats")
	private boolean fakeResendRequest;

    @Description("Regexp to extract the sequence number from the Reject message. Sequence number must be included in the group named <b>sequence</b>."
            + " For example regexp <b>Expected Sequence:(?<sequence>[\\d]+) Received:[\\d]+</b> extracts <b>1807</b> from text <b>Expected Sequence:1807 Received:13</b>")
	private String seqNumberfromRejectRegexp;

    @Description("Regexp to extract the sequence number from the Logout message. Sequence number must be included in the group named <b>sequence</b>."
            + " For example regexp <b>Expected Sequence:(?<sequence>[\\d]+) Received:[\\d]+</b> extracts <b>1807</b> from text <b>Expected Sequence:1807 Received:13</b>")
    private String seqNumberfromLogoutRegexp;
    @Description("Value for encryptMethod(98) field. Please note the value of this option doesn't trigger a logic related to encrypted / encrypted message")
    private String encryptMethod;

    public FIXClientSettings() {
        setApplicationClass(FIXApplication.class.getCanonicalName());
    }

    @Override
	public void load(HierarchicalConfiguration<ImmutableNode> config)
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

	public String getSocketConnectHost() {
		return SocketConnectHost;
	}

	public void setSocketConnectHost(String socketConnectHost) {
		SocketConnectHost = socketConnectHost;
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

    public int getSocketConnectPort() {
        return SocketConnectPort;
    }

    public void setSocketConnectPort(int socketConnectPort) {
        SocketConnectPort = socketConnectPort;
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

	public boolean isDoLogonOnStart() {
		return doLogonOnStart;
	}

	public void setDoLogonOnStart(boolean doLogonOnStart) {
		this.doLogonOnStart = doLogonOnStart;
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

	public boolean isUseDefaultApplVerID() {
		return useDefaultApplVerID;
	}

	public void setUseDefaultApplVerID(boolean useDefaultApplVerID) {
		this.useDefaultApplVerID = useDefaultApplVerID;
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

	public boolean isPerformanceMode() {
		return performanceMode;
	}

	public void setPerformanceMode(boolean performanceMode) {
		this.performanceMode = performanceMode;
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

    public boolean isRequiresOrigSendingTime() {
        return requiresOrigSendingTime;
    }

    public void setRequiresOrigSendingTime(boolean requiresOrigSendingTime) {
        this.requiresOrigSendingTime = requiresOrigSendingTime;
    }

    public boolean getFakeResendRequest(){
    	return fakeResendRequest;
	}

	public void setFakeResendRequest(boolean fakeResendRequest){
    	this.fakeResendRequest = fakeResendRequest;
	}

    public String getSeqNumberfromRejectRegexp() {
        return seqNumberfromRejectRegexp;
    }

    public void setSeqNumberfromRejectRegexp(String seqNumberfromRejectRegexp) {
        this.seqNumberfromRejectRegexp = seqNumberfromRejectRegexp;
    }

    public String getSeqNumberfromLogoutRegexp() {
        return seqNumberfromLogoutRegexp;
    }

    public void setSeqNumberfromLogoutRegexp(String seqNumberfromLogoutRegexp) {
        this.seqNumberfromLogoutRegexp = seqNumberfromLogoutRegexp;
    }

    public String getEncryptMethod() {
        return encryptMethod;
    }

    public void setEncryptMethod(String encryptMethod) {
        this.encryptMethod = encryptMethod;
    }

}