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

import com.exactpro.sf.services.ntg.NTGServerSettings.NTGServerStrategy;
import com.exactpro.sf.util.AbstractTest;
import org.apache.commons.configuration.XMLConfiguration;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class TestSettings extends AbstractTest
{
	@Test
	public void testServerSettings()
	{
		try
		{
			String configFilePath = new File(".").getCanonicalPath();

			String fileFQN = configFilePath + File.separator
			+ "cfg" + File.separator
			+ "config.xml";

			if((new File(fileFQN)).exists())
			{
				XMLConfiguration config = new XMLConfiguration( fileFQN );
				config.load();

				Object servers = config.getProperty("Environment.ConnectionManager.Services.Service[@name]");

				if(servers instanceof Collection<?>)
				{
					for(int i = 0 ; i < ((Collection<?>) servers).size(); i++)
					{
						String key = String.format( "Environment.ConnectionManager.Services.Service(%d)[@type]", i );
						String keyValue = config.getString(key );

                        if (keyValue.equals("NTG-Server"))
						{
							String instIdKey = String.format( "Environment.ConnectionManager.Services.Service(%d).strategies.strategy.instrumentID", i );
							Object prop = config.getList(instIdKey);

							if(prop instanceof Collection<?>)
							{
                                NTGServerSettings serverSettings = new NTGServerSettings();
                                Map<String, NTGServerStrategy> strategy = new HashMap<String, NTGServerStrategy>();

								int j = 0 ;

								for(Object instrValue : (Collection<?>)prop)
								{
									//String symbol = config.getString(String.format( "Environment.ConnectionManager.Services.Service(%d).strategies.strategy(%d).instrumentID", i, j));
									String symbol = instrValue.toString();

									if(! strategy.containsKey( symbol ))
									{
										String strategyValueString = config.getString(String.format( "Environment.ConnectionManager.Services.Service(%d).strategies.strategy(%d).strategyValue", i, j));
                                        NTGServerStrategy strategyValue =
                                                Enum.valueOf(NTGServerStrategy.class, strategyValueString);
										strategy.put( symbol, strategyValue );
									}
									++j;
								}

								int heartbeatTimeout = config.getInt(String.format("Environment.ConnectionManager.Services.Service(%d).heartbeatTimeout", i ));
								int maxMissedHeartbeats = config.getInt(String.format("Environment.ConnectionManager.Services.Service(%d).maxMissedHeartbeats", i ));
								int serverPort = config.getInt( String.format("Environment.ConnectionManager.Services.Service(%d).serverPort", i ));
								String serverIP = config.getString(String.format("Environment.ConnectionManager.Services.Service(%d).serverIP", i ));

								serverSettings.setHeartbeatTimeout( heartbeatTimeout );
								serverSettings.setMaxMissedHeartbeats( maxMissedHeartbeats );
								serverSettings.setServerPort( serverPort ) ;
								serverSettings.setServerIP( serverIP ) ;

								serverSettings.setStrategy( strategy );
							}
							break;
						}
						Assert.fail();
					}
				}
			}
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			Assert.fail();
		}
	}
}
