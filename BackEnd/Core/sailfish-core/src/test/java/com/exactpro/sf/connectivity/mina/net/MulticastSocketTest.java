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
package com.exactpro.sf.connectivity.mina.net;

import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.junit.Ignore;
import org.junit.Test;

import com.exactpro.sf.util.AbstractTest;

@Ignore
public class MulticastSocketTest extends AbstractTest
{
	private static final Logger logger = LoggerFactory.getLogger(MulticastSocketTest.class);

	public static final int PORT = 18567;

	@Test
	public void testConnect() throws InterruptedException
	{
		MulticastSocketConnector connector = new MulticastSocketConnector();

		connector.setHandler(new IoHandlerAdapter()
		{
			@Override
			public void sessionCreated(IoSession session) throws Exception
			{
				logger.debug("SessionCreated");
				System.out.println();
			}


			@Override
			public void sessionClosed(IoSession session) throws Exception
			{
				logger.debug("SessionClosed");
				System.out.println("SessionClosed");
			}


			@Override
			public void sessionOpened(IoSession session) throws Exception
			{
				session.getConfig().setBothIdleTime(5);
				logger.debug("SessionOpened");
				System.out.println("SessionOpened");
			}


			@Override
			public void messageReceived(IoSession session, Object message)
					throws Exception
			{
				System.out.println("messageReceived:" + ((IoBuffer)message).getHexDump());
			}


			@Override
			public void sessionIdle(IoSession session, IdleStatus status)
					throws Exception
			{
				System.out.println("Idle");
			}
		});



		connector.connect(new InetSocketAddress("230.0.0.1", 4446));

		Thread.sleep(20000);

		connector.dispose();

	}



	/*public void testNIOConnect()
	{
		NioDatagramConnector connector = new NioDatagramConnector();

        connector.setHandler(new IoHandlerAdapter()
		{
			@Override
			public void sessionCreated(IoSession session) throws Exception
			{
				System.out.println("SessionCreated");
			}


			@Override
			public void sessionClosed(IoSession session) throws Exception
			{
				System.out.println("SessionClosed");
			}


			@Override
			public void sessionOpened(IoSession session) throws Exception
			{
				System.out.println("SessionOpened");
			}
		});

        InetSocketAddress address = new InetSocketAddress("localhost", PORT);
        ConnectFuture connFuture = connector.connect(address);

        connFuture.awaitUninterruptibly();


	}*/







}
