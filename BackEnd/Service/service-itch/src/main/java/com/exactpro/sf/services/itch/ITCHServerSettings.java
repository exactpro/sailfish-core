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

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.configuration.HierarchicalConfiguration;

import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.suri.SailfishURIException;
import com.exactpro.sf.services.AbstractServiceSettings;

@XmlRootElement
public class ITCHServerSettings extends AbstractServiceSettings
{
    private static final long serialVersionUID = -112039599183469654L;

    private String realTimeChannelAddress;

	private int realTimeChannelPort;

	private int heartBeatTimeout;

	private SailfishURI dictionaryName;

	public int getHeartBeatTimeout()
	{
		return heartBeatTimeout;
	}


	public void setHeartBeatTimeout(int heartBeatTimeout)
	{
		this.heartBeatTimeout = heartBeatTimeout;
	}


	public String getRealTimeChannelAddress()
	{
		return realTimeChannelAddress;
	}


	public void setRealTimeChannelAddress(String realTimeChannelAddress)
	{
		this.realTimeChannelAddress = realTimeChannelAddress;
	}


	public int getRealTimeChannelPort()
	{
		return realTimeChannelPort;
	}


	public void setRealTimeChannelPort(int realTimeChannelPort)
	{
		this.realTimeChannelPort = realTimeChannelPort;
	}


	@Override
	public void load(HierarchicalConfiguration config)
	{
		this.realTimeChannelAddress = config.getString("realTimeChannelAddress");
		this.realTimeChannelPort = config.getInt("realTimeChannelPort");
		this.heartBeatTimeout = config.getInt("heartBeatTimeout");

		try {
            this.dictionaryName = SailfishURI.parse(config.getString("dictionaryName"));
        } catch(SailfishURIException e) {
            throw new EPSCommonException(e);
        }
	}


	@Override
	public SailfishURI getDictionaryName() {
		return dictionaryName;
	}

	@Override
	public void setDictionaryName(SailfishURI dictionaryName) {
		this.dictionaryName = dictionaryName;
	}
}
