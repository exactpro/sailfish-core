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
package com.exactpro.sf.services.itch.multicast;

import com.exactpro.sf.aml.Description;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.services.AbstractServiceSettings;
import org.apache.commons.configuration.HierarchicalConfiguration;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by alexey.zarovny on 11/18/14.
 */
@XmlRootElement
public class ITCHMulticastSettings extends AbstractServiceSettings {
    private static final long serialVersionUID = -1572982435405711157L;

    @Description("Mulicast primary address")
    private String primaryAddress;
    @Description("Multicast primary port")
    private int primaryPort;
    @Description("Multicast secondary address")
    private String secondaryAddress;
    @Description("Multicast secondary port")
    private int secondaryPort;
    @Description("Tcp replay channel port")
    private int tcpPort;
    @Description("Market data group")
    private byte marketDataGroup;
    @Description("Default message length")
    private int msgLength = 1;
    private boolean storeMessages = true;
    @Description("Dictionary name")
    private SailfishURI dictionaryName;
    @Description("Cashe size for replay")
    private int cacheSize = 2048;
    @Description("Session idle timeout in second")
    private int sessionIdleTimeout = 5;
    @Description("Login timeout")
    private int loginTimeout = 5;
    @Description("Heartbeat interval")
    private int heartbeatInterval = 2;
    @Description("Store HeartBeats")
    private boolean storeHeartbeat = true;


    @Override
    public void load(HierarchicalConfiguration config) {
        //intentionally left blank
    }

    public String getPrimaryAddress() {
        return primaryAddress;
    }

    public void setPrimaryAddress(String primaryAddress) {
        this.primaryAddress = primaryAddress;
    }

    public int getPrimaryPort() {
        return primaryPort;
    }

    public void setPrimaryPort(int primaryPort) {
        this.primaryPort = primaryPort;
    }

    public String getSecondaryAddress() {
        return secondaryAddress;
    }

    public void setSecondaryAddress(String secondaryAddress) {
        this.secondaryAddress = secondaryAddress;
    }

    public int getSecondaryPort() {
        return secondaryPort;
    }

    public void setSecondaryPort(int secondaryPort) {
        this.secondaryPort = secondaryPort;
    }

    public int getTcpPort() {
        return tcpPort;
    }

    public void setTcpPort(int tcpPort) {
        this.tcpPort = tcpPort;
    }

    public byte getMarketDataGroup() {
        return marketDataGroup;
    }

    public void setMarketDataGroup(byte marketDataGroup) {
        this.marketDataGroup = marketDataGroup;
    }

    public int getMsgLength() {
        return msgLength;
    }

    public void setMsgLength(int msgLength) {
        this.msgLength = msgLength;
    }

    public boolean isStoreMessages() {
        return storeMessages;
    }

    public void setStoreMessages(boolean storeMessages) {
        this.storeMessages = storeMessages;
    }

    @Override
    public SailfishURI getDictionaryName() {
        return dictionaryName;
    }

    @Override
    public void setDictionaryName(SailfishURI dictionaryName) {
        this.dictionaryName = dictionaryName;
    }

    public int getCacheSize() {
        return cacheSize;
    }

    public void setCacheSize(int cacheSize) {
        this.cacheSize = cacheSize;
    }

    public int getSessionIdleTimeout() {
        return sessionIdleTimeout;
    }

    public void setSessionIdleTimeout(int sessionIdleTimeout) {
        this.sessionIdleTimeout = sessionIdleTimeout;
    }

    public int getLoginTimeout() {
        return loginTimeout;
    }

    public void setLoginTimeout(int loginTimeout) {
        this.loginTimeout = loginTimeout;
    }

    public int getHeartbeatInterval() {
        return heartbeatInterval;
    }

    public void setHeartbeatInterval(int heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
    }

    public boolean isStoreHeartbeat() {
        return storeHeartbeat;
    }

    public void setStoreHeartbeat(boolean storeHeartbeat) {
        this.storeHeartbeat = storeHeartbeat;
    }
}
