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
package com.exactpro.sf.services.itch;

import com.exactpro.sf.aml.Description;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.services.RequiredParam;
import com.exactpro.sf.services.mina.AbstractMINASettings;
import com.exactpro.sf.services.util.ServiceUtil;
import org.apache.commons.configuration.HierarchicalConfiguration;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ITCHClientSettings extends AbstractMINASettings
{
    private static final long serialVersionUID = 4660409151397687772L;

    @RequiredParam
	@Description("Host name or ip address of the remote server")
	private String address;

	@RequiredParam
	@Description("Port that remote server listen to")
	private int port;

	@Description("IP address of network card (NIC) – used only for ITCH Multicast channel. \n" +
			"Should be set in case of several NICs is setup on machine.")
	private String networkInterface;

	@Description("Send Heartbeat every idleTimeout seconds")
	private int idleTimeout;

	@RequiredParam
	@Description("ID of target Market Data Group should be used in UnitHeader")
	private byte marketDataGroup;
	@RequiredParam
    @Description("The number of bytes for length field (1 or 2)")
	private int msgLength = 1;

    @Description("Determine will messages be persisted to storage or not. " +
            "Note: this setting is deprecated; please use 'Persist messages' instead")
    @Deprecated
	private boolean storeMessages = true;

	@RequiredParam
	@Description("Dictionary title")
	private SailfishURI dictionaryName;
    @Description("Fields value for which allow receive messages.\n"
            + "Example: '1, A' or '"+ ServiceUtil.ALIAS_PREFIX + "dataA'")
	private String filterValues;

	@Description("ITCH Preprocessor's class name")
	private String preprocessor;

	public byte getMarketDataGroup() {
		return marketDataGroup;
	}

	public void setMarketDataGroup(byte marketDataGroup) {
		this.marketDataGroup = marketDataGroup;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getNetworkInterface() {
		return networkInterface;
	}

	public void setNetworkInterface(String networkInterface) {
		this.networkInterface = networkInterface;
	}

	public int getIdleTimeout() {
		return idleTimeout;
	}

	public void setIdleTimeout(int idleTimeout) {
		this.idleTimeout = idleTimeout;
	}

	@Override
	public void load(HierarchicalConfiguration config)
	{
		this.storeMessages = config.getBoolean("storeMessages", true);
		//this.address = config.getString("address");
		//this.port = config.getInt("port", 0);
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
		setPersistMessages(storeMessages);
	}

	@Override
	public SailfishURI getDictionaryName() {
		return dictionaryName;
	}

	@Override
	public void setDictionaryName(SailfishURI dictionaryName) {
		this.dictionaryName = dictionaryName;
	}

	public String getFilterValues() {
		return filterValues;
	}

	public void setFilterValues(String filterValues) {
		this.filterValues = filterValues;
	}

	public String getPreprocessor() {
		return preprocessor;
	}

	public void setPreprocessor(String preprocessor) {
		this.preprocessor = preprocessor;
	}

}
