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
package com.exactpro.sf.services.fast;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.configuration.HierarchicalConfiguration;

import com.exactpro.sf.aml.Description;
import com.exactpro.sf.services.RequiredParam;

@XmlRootElement
public class FASTClientSettings extends FASTCodecSettings {

    private static final long serialVersionUID = -8839117107070514695L;

    @RequiredParam
	@Description("Host address")
	private String address;

	@RequiredParam
	@Description("Connection port")
	private int port;

	@RequiredParam
	@Description("Network interface")
	private String networkInterface;

	@Description("Incativity service timeout in seconds after which service will be shout down automatically. If 0 - do not shoutdown service.")
	private int idleTimeout;

	@Description("Message filter expression")
	private String messageFilterExpression;

	@Description("Begin string")
	private String beginString;
	
	@Description("ApplVerID")
	private String applVerID;

    @Description("Connect client after start")
    private boolean autoconnect;

    public boolean isAutoconnect() {
        return autoconnect;
    }

    public void setAutoconnect(boolean autoconnect) {
        this.autoconnect = autoconnect;
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

	public String getMessageFilterExpression() {
		return messageFilterExpression;
	}

	public void setMessageFilterExpression(
			String messageFilterExpression) {
		this.messageFilterExpression = messageFilterExpression;
	}

	@Override
	public void load(HierarchicalConfiguration config)
	{
		//this.address = config.getString("address");
		//this.port = config.getInt("port", 0);
	}

	public String getBeginString()
	{
		return beginString;
	}

	public void setBeginString(String beginString)
	{
		this.beginString = beginString;
	}

	public String getApplVerID()
	{
		return applVerID;
	}

	public void setApplVerID(String applVerID)
	{
		this.applVerID = applVerID;
	}

}
