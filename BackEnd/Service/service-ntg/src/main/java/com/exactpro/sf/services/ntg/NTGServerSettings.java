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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.configuration.HierarchicalConfiguration;

import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.suri.SailfishURIException;
import com.exactpro.sf.services.AbstractServiceSettings;

@XmlRootElement
public class NTGServerSettings extends AbstractServiceSettings
{
    private static final long serialVersionUID = -1948906915528524254L;

    public enum NTGServerKey
	{
		serverIP,
		serverPort,
		heartbeatTimeout,
		maxMissedHeartbeats,
		strategies;
	}

    public enum NTGServerStrategy
	{
		UnconditionalFill,
		PartFill,
		PartFillFill,
		CancelReject,
		Reject,
		Amend;
	}

	// Default IP address.
	// Key "serverIP"
    private String serverIP = NTGUtility.getMachineIP();

	// Default connection port.
    // Key "serverPort"
    private int serverPort = 8181;

    // Idle timeout in milliseconds (if no message has been sent then
	// client must sent heartbeat message)
	// Key "heartbeatTimeout"
	private int heartbeatTimeout = 30000;

	// Number of missed heartbeat messages forcing the client
	// to break the connection.
	// Key "maxMissedHeartbeats"
	private int maxMissedHeartbeats = 5;

	// Strategy for InstrumentID
	// Key "strategy"
    private Map<String, NTGServerStrategy> strategy =
		new HashMap<>();

	// Default delay between sends.
    // Key "sendDelay"
	private int sendDelay = 16;

	// Default delay between sends.
    // Key "defaultStrategy"
    private NTGServerStrategy defaultStrategy = NTGServerStrategy.UnconditionalFill;

	//private String dictionary;

	private SailfishURI dictionaryName;

	/*
	public final String getDictionary() {
		return dictionary;
	}

	public final void setDictionary(String dictionary) {
		this.dictionary = dictionary;
	}
	*/

    public void setDefaultStrategy(NTGServerStrategy defaultStrategy)
	{
		this.defaultStrategy = defaultStrategy;
	}

    public NTGServerStrategy getDefaultStrategy()
	{
		return defaultStrategy;
	}

	/*
	* Getters
	*/
	public int getHeartbeatTimeout()
	{
		return this.heartbeatTimeout;
	}

	public int getMaxMissedHeartbeats()
	{
		return this.maxMissedHeartbeats;
	}

	public int getServerPort()
	{
		return this.serverPort;
	}

	public String getServerIP()
	{
		return this.serverIP;
	}

    public Map<String, NTGServerStrategy> getStrategy()
	{
		return Collections.unmodifiableMap(this.strategy );
		//return this.strategy;
	}


	public void setSendDelay(int sendDelay)
	{
		this.sendDelay =  sendDelay;
	}

	public int getSendDelay()
	{
		return this.sendDelay;
	}



	/*
	* Setters
	*/
	public void setHeartbeatTimeout(int heartbeatTimeout)
	{
		this.heartbeatTimeout = heartbeatTimeout;
	}

	public void setMaxMissedHeartbeats(int maxMissedHeartbeats)
	{
		this.maxMissedHeartbeats = maxMissedHeartbeats;
	}

	public void setServerPort(int serverPort)
	{
		this.serverPort = serverPort;
	}

	public void setServerIP(String serverIP)
	{
		 this.serverIP = serverIP;
	}

    public void setStrategy(Map<String, NTGServerStrategy> strategy)
	{
		this.strategy = strategy;
	}

	/**
	 *
	 * @return timeout in milliseconds which is equivalent
	 * to max missed heartbeats
	 */
	public int getForceLogoutTimeout()
	{
		return this.heartbeatTimeout * this.maxMissedHeartbeats;
	}

	public int getHeartbeatTimeoutInSeconds()
	{
		return this.heartbeatTimeout / 1000;
	}






	@Override
	public void load( HierarchicalConfiguration config )
	{
		this.heartbeatTimeout = config.getInt("heartbeatTimeout", 30000);
		this.maxMissedHeartbeats = config.getInt("maxMissedHeartbeats", 5);
		this.serverPort = config.getInt( "serverPort", 8181);
		this.serverIP = config.getString("serverIP", "127.0.0.1 ");
		this.sendDelay = config.getInt("sendDelay", 16 );

		try {
            this.dictionaryName = SailfishURI.parse(config.getString("dictionaryName"));
        } catch(SailfishURIException e) {
            throw new EPSCommonException(e);
        }

        this.defaultStrategy = NTGServerStrategy.valueOf(
                config.getString("defaultStrategy", NTGServerStrategy.UnconditionalFill.toString()));

		Object strategyColl = config.getProperty( "strategies.strategy.instrumentID" );

		if(strategyColl instanceof Collection<?>)
		{
			strategy = new HashMap<>();

			for(int i = 0 ; i < ((Collection<?>) strategyColl).size(); i++)
			{
				String symbol = config.getString(String.format( "strategies.strategy(%d).instrumentID", i));

				String strategyValueString = config.getString(String.format( "strategies.strategy(%d).strategyValue", i));
                NTGServerStrategy strategyValue =
                        Enum.valueOf(NTGServerStrategy.class, strategyValueString);

				if(!strategy.containsKey( symbol ))
				{
					strategy.put( symbol, strategyValue );
				}
			}
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
