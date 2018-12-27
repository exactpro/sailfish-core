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

import com.exactpro.sf.services.ntg.NTGClientTest.ClientStrategy;
import com.exactpro.sf.services.ntg.NTGServerTest.MessageType;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;

@SuppressWarnings("unused")
public final class RunTaskTestClient implements Runnable
{
	int clientID;
	String directoryXSD;
	String IP;
	int port;
	String name;
	String password;
	ClientStrategy strategy = ClientStrategy.SessionOnly;
    List<MessageType> msgs = new ArrayList<MessageType>();
    NTGClientTest testClient = null;

	CountDownLatch latch = null;

	private boolean passed = false;

	public RunTaskTestClient( CountDownLatch latch, int clientID, String directoryXSD, String IP, int port,
			String name, String password )
	{
		this.latch = latch;
		this.directoryXSD = directoryXSD;
		this.IP = IP;
		this.port = port;
		this.name = name;
		this.password = password;
		this.clientID = clientID;
	}

    public RunTaskTestClient(CountDownLatch latch, int clientID, String directoryXSD, String IP, int port,
                             String name, String password,
                             ClientStrategy strategy, List<MessageType> msgs)
	{
		this(latch, clientID, directoryXSD,  IP,  port, name, password);
		this.strategy = strategy;
		this.msgs = msgs;
	}

	@Override
	public void run()
	{
		try
		{
			System.out.println( getTimeStamp() +
					String.format("  RunTaskTestClient.run() Client [%d]: Is wating for countdown.",
							clientID ));

			this.latch.await();

			System.out.println( getTimeStamp() +
					String.format("  RunTaskTestClient.run() Client [%d]: Is running.",
							clientID ));

			Thread.sleep( clientID * 10);

            NTGClientSettings settings = new NTGClientSettings();


            testClient = new NTGClientTest(clientID, settings);
			Thread.sleep(1000);
			testClient.getClient().start();

			Thread.sleep(1000);

			if(testClient.getClient().isConnected())
			{
				System.out.println( getTimeStamp() +
						String.format("  RunTaskTestClient.run() Client [%d]: Trying logging in.",
								clientID ));

				//				//testClient.getClient().login("muriel", "muriel");

				System.out.println( getTimeStamp() +
						String.format("  RunTaskTestClient.run() Client [%d]: Success login.",
								clientID ));

				Thread.sleep(30000);

				System.out.println( getTimeStamp() +
						String.format("  RunTaskTestClient.run() Client [%d]: Logging out.",
								clientID ));

				testClient.getClient().dispose();

				while(testClient.getClient().isConnected())
				{
					Thread.sleep(1000);
				}

				System.out.println( getTimeStamp() +
						String.format("  RunTaskTestClient.run() Client [%d]: end.",
								clientID ));
			}
		}
		catch( Exception ex )
		{
			ex.printStackTrace();
		}
		finally
		{
		}
	}

	public void ShutdownClient()
	{
	}

	private String getTimeStamp()
	{
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
	}
}
