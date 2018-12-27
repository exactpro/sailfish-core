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

import static com.exactpro.sf.services.ntg.NTGClient.NTGClientState;
import static com.exactpro.sf.services.ntg.NTGServerSettings.NTGServerStrategy;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.configuration.XMLConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.scriptrunner.SailFishTestCase;
import com.exactpro.sf.services.IService;

//This test need for specific hand testing
public final class TestNTGClientNTGServer extends SailFishTestCase
{
	public static void main(String[] args) throws Throwable
	{
        TestNTGClientNTGServer testMain = new TestNTGClientNTGServer();

		try
		{
			//SailFishTestCase.setUpBeforeClass();
			testMain.setUp();

            //testMain.testNTGClient_Start_Stop();
			//testMain.testMassiveLogon();
			//testMain.testServerStragey_UnconditionalFill();

			//testMain.testServerStragey_Amend_with_Replace_Cancel();
			//testMain.testServerStragey_PartFill();
			//testMain.testServerStragey_PartFillFill();
			//testMain.testServerStragey_Reject();
			//testMain.testServerStragey_CancelReject();

			testMain.tearDown();
			//SailFishTestCase.tearDownAfterClass();

		}
		catch( Exception e )
		{
			e.printStackTrace();
		}
		return;
	}

    private static final Logger logger = LoggerFactory.getLogger(TestNTGClientNTGServer.class);

	boolean runTest = false;

    String serverName = "NTGServer1";
    String clientName = "NTGClient1";
    IService ntgServer = null;

    NTGServerSettings serverSettings = null;

	protected void setUp() throws Exception
	{
		if(!runTest)
		{
			return;
		}

        //ntgServer = (IService)super.getEnv().getConnectionManager().getService( serverName );

		Thread.sleep( 1000 );
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
                            Map<String, NTGServerStrategy> strategy = new HashMap<>();

							int j = 0 ;

							for(Object instrValue : (Collection<?>)prop)
							{
								//String symbol = config.getString(String.format( "Environment.ConnectionManager.Services.Service(%d).strategies.strategy(%d).instrumentID", i, j));
								String symbol = instrValue.toString();

								if(! strategy.containsKey( symbol ))
								{
									String strategyValueString = config.getString(String.format( "Environment.ConnectionManager.Services.Service(%d).strategies.strategy(%d).strategyValue", i, j));
                                    NTGServerSettings.NTGServerStrategy strategyValue =
                                            Enum.valueOf(NTGServerSettings.NTGServerStrategy.class, strategyValueString);
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


	protected void tearDown() throws Exception
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

    public void testNTGClient_Start_Stop() throws IOException
	{
		if(!runTest )
		{
			return;
		}
		if( runTest )
		{
			return;
		}

        IService ntgClient = null;

		try
		{
            NTGClientSettings settingsClient = new NTGClientSettings();
			settingsClient.setServerIP( InetAddress.getLocalHost().getHostAddress() );
			settingsClient.setServerPort( 8181 );
            ntgClient = (IService) getContext().getEnvironmentManager().getConnectionManager().getService(ServiceName.parse(clientName));


            logger.debug("           client.getState() = {}", ((NTGClient) ntgClient).getState());
            logger.debug("           client.isConnected() = {}", ((NTGClient) ntgClient).isConnected());

			Thread.sleep( 5000 );

			assertTrue( String.format( "Client is expected to be connected IP [%s], port [%d].",
					settingsClient.getServerIP(), settingsClient.getServerPort() ),
                    ((NTGClient) ntgClient).isConnected());

            assertEquals(String.format("Client is expected to be in the state [%s].", NTGClientState.LoggedIn.toString()),
                    NTGClientState.LoggedIn, ((NTGClient) ntgClient).getState());



			Thread.sleep( 5000 );

            assertEquals(String.format("Client is expected to be in the state [%s].", NTGClientState.LoggedIn.toString()),
                    NTGClientState.LoggedIn, ((NTGClient) ntgClient).getState());

			Thread.sleep( 5000 );

            assertEquals(String.format("Client is expected to be in the state [%s].", NTGClientState.LoggedIn.toString()),
                    NTGClientState.LoggedIn, ((NTGClient) ntgClient).getState());

            ntgClient.dispose();

			Thread.sleep( 1000 );

            assertEquals(String.format("Client is expected to be in the state [%s].", NTGClientState.SessionClosed.toString()),
                    NTGClientState.SessionClosed, ((NTGClient) ntgClient).getState());

			Thread.sleep( 5000 );
            ntgClient.start();
			Thread.sleep( 5000 );

			assertTrue( String.format( "Client is expected to be connected IP [%s], port [%d].",
					settingsClient.getServerIP(), settingsClient.getServerPort() ),
                    ((NTGClient) ntgClient).isConnected());

            assertEquals(String.format("Client is expected to be in the state [%s].", NTGClientState.LoggedIn.toString()),
                    NTGClientState.LoggedIn, ((NTGClient) ntgClient).getState());

			Thread.sleep( 5000 );

            ntgClient.dispose();

			Thread.sleep( 1000 );

            assertEquals(String.format("Client is expected to be in the state [%s].", NTGClient.NTGClientState.SessionClosed.toString()),
                    NTGClientState.SessionClosed, ((NTGClient) ntgClient).getState());

		}
		catch( Exception e )
		{
			e.printStackTrace();
			fail();
		}
	}
		}
