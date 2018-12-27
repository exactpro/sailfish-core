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

import com.exactpro.sf.services.IService;
import com.exactpro.sf.services.ntg.NTGServerSettings.NTGServerStrategy;
import com.exactpro.sf.util.AbstractTest;
import org.apache.commons.configuration.XMLConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

@Ignore //This test need for specific hand testing
public class TestNTGClient extends AbstractTest {
    private static final Logger logger = LoggerFactory.getLogger(TestNTGClient.class);

	@SuppressWarnings("unused")
	private final Random rnd = new Random(Calendar.getInstance().getTime().getTime());

	boolean runTest = false;

    String serverName = "NTGServer1";
    String clientName = "NTGClient1";
    IService ntgServer = null;

    NTGServerSettings serverSettings = null;

	@Test
	public void testClientLogonLogout()
	{
		if( !runTest )
		{
			return;
		}


        IService ntgClient = null;


		try
		{
            //			ntgClient = (IService)TestScriptSupport.getEnvironment().getConnectionManager().getService( clientName );
			//			Thread.sleep( 1000 );
			//
			//			logger.trace( "Starting client" );
            //			ntgClient.start();
			//			Thread.sleep( 5000 );
		}
		catch( Exception e )
		{
			e.printStackTrace();
		}
		finally
		{
            if (null != ntgClient)
			{
                ntgClient.dispose();
			}
		}
	}

	@Before
	public void setUp() throws Exception
	{
		if(!runTest)
		{
			return;
		}

        //ntgServer = (IService)TestScriptSupport.getEnvironment().getConnectionManager().getService( serverName );
		Thread.sleep( 1000 );

        ntgServer.start();
        logger.trace("NTGServer: server has  started");

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
                            serverSettings = new NTGServerSettings();
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
							int sendDelay = config.getInt( String.format("Environment.ConnectionManager.Services.Service(%d).sendDelay", i ));

							serverSettings.setHeartbeatTimeout( heartbeatTimeout );
							serverSettings.setMaxMissedHeartbeats( maxMissedHeartbeats );
							serverSettings.setServerPort( serverPort ) ;
							serverSettings.setServerIP( serverIP ) ;
							serverSettings.setSendDelay( sendDelay );

							serverSettings.setStrategy( strategy );
						}
						break;
					}
				}
			}
		}
	}

	@After
	public void tearDown() throws Exception
	{
		if(!runTest)
		{
			return;
		}

        if (null != ntgServer)
		{
			try
			{
                ntgServer.dispose();

			}
			catch( Exception e )
			{
				e.printStackTrace();
			}
			finally
			{
                ntgServer = null;
                logger.trace("NTGServer: server has been stopped.");
			}
		}
	}
}
