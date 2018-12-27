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
package com.exactpro.sf.services.ntg;

import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.aml.Description;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.suri.SailfishURIException;
import com.exactpro.sf.services.RequiredParam;
import com.exactpro.sf.services.mina.AbstractMINASettings;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class NTGClientSettings extends AbstractMINASettings {
    private static final long serialVersionUID = -4261061063272466531L;

    public enum NTGClientKey {
		  serverIP
		, serverPort
		, connectTimeout
		, heartbeatTimeout
		, maxMissedHeartbeats
		, login
		, password
		, newPassword
		, loginTimeout
		, logoutTimeout
		, reconnectAttempts
		, messageVersion
		, lowlevelService
		, autosendHeartbeat
		, defaultDictionaryDirectory
		;
	}


	/**
	 * Real time IP address.
	 * Key "serverIP"
	 */
	@RequiredParam
	@Description("Real time IP address")
    private String serverIP = NTGUtility.getMachineIP();

    /**
     * Real time connection port.
     *  Key "serverPort"
     */
	@RequiredParam
	@Description("Real time connection port")
    private int serverPort = 8181;

    /**
     * Connect timeout in milliseconds.
     * Key "connectTimeout"
     */
	@Description("Connect timeout in milliseconds.")
	private int connectTimeout = 30000;

	/**
	 * login timeout in milliseconds.
	 * Key "loginTimeout"
	 */
	@Description("login timeout in milliseconds.")
	private int loginTimeout = 30000;

	/**
	 * logout timeout in milliseconds.
	 * Key "logoutTimeout"
	 */
	@Description("logout timeout in milliseconds.")
	private int logoutTimeout = 5000;

	/**
	 * Idle timeout in milliseconds (if no message has been sent then
	 * client must sent Heartbeat message)
	 * Key "heartbeatTimeout"
	 */
	@Description("Idle timeout in milliseconds (if no message has been sent\n" +
			"then client must sent Heartbeat message)")
	private int heartbeatTimeout = 30000;

	/**
	 * Quantity of reconnect attempts after server connection drop
	 * Key "reconnectAttempts"
	 */
	@Description("Quantity of reconnect attempts after server connection drop")
	private int reconnectAttempts = 10;


	/**
	 *  Number of missed Heartbeat messages forcing the client
	 *  to break the connection.
	 *  Key "maxMissedHeartbeats"
	 */
	@Description("Number of missed Heartbeat messages forcing\n" +
			"the client to break the connection.")
	private int maxMissedHeartbeats = 5;

	/**
	 * User ID
	 * Key "login"
	 */
	@RequiredParam
	@Description("User ID")
    private String login = "user" ;

    /**
     * User password
     * Key "password"
     */
	@RequiredParam
	@Description("User password")
    private String password = "password" ;

    /**
     * User newPassword
     * Key "newPassword"
     */
    private String newPassword = "";

    private boolean lowlevelService = true;
    @Description("If true client sends heartbeat (response or timeout heartbeat).")
    private boolean autosendHeartbeat = true;

	/**
	 * Version of the messages used in session.
	 * Key "messageVersion"
	 */
	@Description("Version of the messages used in session.")
	private byte messageVersion = 1;

	private int idleTimeout;

	private boolean doLoginOnStart;

    /**
     * Connect timeout in milliseconds.
     * @return connect timeout
     */
	public int getConnectTimeout() {
		return this.connectTimeout;
	}

	/**
	 * Login timeout in milliseconds
	 * @return
	 */
	public int getLoginTimeout() {
		return this.loginTimeout;
	}

	public int getLogoutTimeout() {
		return this.logoutTimeout;
	}

	/**
	 * Idle timeout in milliseconds (if no message has been sent then
	 * client must sent heartbeat message)
	 *
	 * @return
	 */
	public int getHeartbeatTimeout() {
		return this.heartbeatTimeout;
	}

	/**
	 * Number of missed heartbeat messages forcing the client to break the connection.
	 *	 @return
	 */
	public int getMaxMissedHeartbeats() {
		return this.maxMissedHeartbeats;
	}

	public int getServerPort() {
		return this.serverPort;
	}

	public String getServerIP() {
		return this.serverIP;
	}

	public String getLogin() {
		return this.login;
	}

	public String getPassword() {
		return this.password;
	}

	public String getNewPassword() {
		return this.newPassword;
	}

	/*
	* Setters
	*/
	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	/**
	 * Login timeout in milliseconds
	 * @return
	 */
	public void setLoginTimeout(int loginTimeout) {
		this.loginTimeout = loginTimeout;
	}

	public void setLogoutTimeout(int logoutTimeout) {
		this.logoutTimeout = logoutTimeout;
	}

	public void setHeartbeatTimeout(int heartbeatTimeout) {
		this.heartbeatTimeout = heartbeatTimeout;
	}

	public void setMaxMissedHeartbeats(int maxMissedHeartbeats) {
		this.maxMissedHeartbeats = maxMissedHeartbeats;
	}

	public void setServerPort(int serverPort) {
		this.serverPort = serverPort;
	}

	public void setServerIP(String serverIP) {
		this.serverIP = serverIP;
	}

	/**
	 *
	 * @return timeout in milliseconds which is equivalent
	 * to max missed heartbeats
	 */
	public int getForceLogoutTimeout() {
		return this.heartbeatTimeout * this.maxMissedHeartbeats;
	}

	public int getHeartbeatTimeoutInSeconds() {
		return this.heartbeatTimeout / 1000;
	}

	public void setLogin(String login) {
		this.login = login;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setNewPassword(String newPassword) {
		this.newPassword = newPassword;
	}

	@Override
	public String toString() {

		ToStringBuilder toString = new ToStringBuilder(this);

		toString.append("serverIP", serverIP);
		toString.append("serverPort", serverPort);
		toString.append("reconnectAttempts", reconnectAttempts);
		toString.append("connectTimeout", connectTimeout);
		toString.append("loginTimeout", loginTimeout);
		toString.append("logoutTimeout", logoutTimeout);
		toString.append("heartbeatTimeout", heartbeatTimeout);
		toString.append("maxMissedHeartbeats", maxMissedHeartbeats);
		toString.append("login", login);
		toString.append("password", password);
		toString.append("newPassword", newPassword);
		toString.append("messageVersion", messageVersion);
		toString.append("lowlevelService", lowlevelService);
		toString.append("autosendHeartBeat", autosendHeartbeat);
		toString.append("idleTimeout", idleTimeout);

		return toString.toString();
	}

	@Override
	public void load( HierarchicalConfiguration config ) {
		this.connectTimeout = config.getInt("connectTimeout", 30000);
		this.loginTimeout = config.getInt("loginTimeout", 30000);
		this.logoutTimeout = config.getInt("logoutTimeout", 5000);
		this.heartbeatTimeout = config.getInt("heartbeatTimeout", 30000);
		this.maxMissedHeartbeats = config.getInt("maxMissedHeartbeats", 5);
		this.serverPort = config.getInt( "serverPort", 8181);
		this.serverIP= config.getString("serverIP", "127.0.0.1");
		this.reconnectAttempts = config.getInt("reconnectAttempts", 10);
		this.login = config.getString( "login", "user");
		this.password= config.getString("password", "password");
		this.newPassword = config.getString("newPassword", null);
		this.messageVersion = config.getByte("messageVersion", (byte)1 );
		//this.dictionaryPath = config.getString("dictionaryPath");
		this.lowlevelService = config.getBoolean("lowlevelService");
		this.autosendHeartbeat = config.getBoolean("autosendHeartBeat");

		try {
            this.dictionaryName = SailfishURI.parse(config.getString("dictionaryName"));
        } catch(SailfishURIException e) {
            throw new EPSCommonException(e);
        }
	}

	public void setReconnectAttempts(int reconnectAttempts) {
		this.reconnectAttempts = reconnectAttempts;
	}

	public int getReconnectAttempts() {
		return reconnectAttempts;
	}

	public void setMessageVersion(byte messageVersion) {
		this.messageVersion = messageVersion;
	}

	public byte getMessageVersion() {
		return messageVersion;
	}

	public boolean isLowLevelService() {
		return lowlevelService;
	}

	public void setLowLevelService(boolean lowlevelService) {
		this.lowlevelService = lowlevelService;
	}

	public boolean isAutosendHeartbeat() {
		return autosendHeartbeat;
	}

	public void setAutosendHeartbeat(boolean autosendHeartbeat) {
		this.autosendHeartbeat = autosendHeartbeat;
	}

	public int getIdleTimeout() {
		return idleTimeout;
	}

	public void setIdleTimeout(int idleTimeout) {
		this.idleTimeout = idleTimeout;
	}

	public boolean isDoLoginOnStart() {
		return doLoginOnStart;
	}

	public void setDoLoginOnStart(boolean doLoginOnStart) {
		this.doLoginOnStart = doLoginOnStart;
	}
}
