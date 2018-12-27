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
package com.exactpro.sf.services.fix;

import org.junit.Ignore;
import org.junit.Test;

import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.services.CollectorServiceHandler;
import com.exactpro.sf.util.AbstractTest;

import quickfix.ConfigError;
import quickfix.DefaultMessageFactory;
import quickfix.FileLogFactory;
import quickfix.FileStoreFactory;
import quickfix.LogFactory;
import quickfix.MessageFactory;
import quickfix.MessageStoreFactory;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionSettings;
import quickfix.SocketInitiator;

public class FIXClientTest extends AbstractTest {

	@Ignore //Test for manual testing
    @Test
	public void testInit() throws ConfigError, InterruptedException
	{
		FIXApplication application = new FIXApplication();
		application.init(serviceContext, new ApplicationContext(null, new CollectorServiceHandler(), new FIXClientSettings(), new SessionSettings(), null, null, null), ServiceName.parse("fix"));
//		application.setSessionName("wwer");
		SessionSettings settings = new SessionSettings();
		SessionID sessionID = new SessionID("FIXT.1.1", "BANZAI1", "EXEC1");
//		settings.setString(sessionID, "BeginString", "FIXT.1.1");
//		settings.setString(sessionID, "SenderCompID", "BANZAI1");
//		settings.setString(sessionID, "TargetCompID", "EXEC1");
		settings.setString(sessionID, "SocketConnectPort", "777");
		settings.setString(sessionID, "SessionName", "connection1");
		settings.setString(sessionID, "FileStorePath", "examples/target/data/banzai");
		settings.setString(sessionID, "FileLogPath", "examples/target/data/banzai/logs");
		settings.setString(sessionID, "FileLogHeartbeats", "Y");
		settings.setString(sessionID, "ConnectionType", "initiator");
		settings.setString(sessionID, "SocketConnectHost", "172.27.72.67");
		settings.setString(sessionID, "StartTime", "00:00:00");
		settings.setString(sessionID, "EndTime", "00:00:00");
		settings.setString(sessionID, "HeartBtInt", "30");
		settings.setString(sessionID, "ReconnectInterval", "1");
		settings.setString(sessionID, "CheckLatency", "N");
		settings.setString(sessionID, "DefaultApplVerID", "FIX.5.0");
		// Username=ABNAM01S02
		// Password=tnp123
		// ResetSeqNumFlag=Y
		// ValidateUserDefinedFields=N

		

		//MessageStoreFactory messageStoreFactory = new MemoryStoreFactory();
		MessageStoreFactory messageStoreFactory = new FileStoreFactory(settings);
		//LogFactory logFactory = new ScreenLogFactory(true, true, true, true);
		//LogFactory logFactory = new ScreenLogFactory(false, false, false, false);
		LogFactory logFactory = new FileLogFactory(settings);
		MessageFactory messageFactory = new DefaultMessageFactory();

		SocketInitiator initiator = new SocketInitiator(application, messageStoreFactory, settings, logFactory,
				messageFactory, null);
		initiator.start();

		FIXSession iSession = new FIXSession("fix", sessionID, null, null, null);
		application.addSessionId(sessionID, iSession);
//		application.setSessionName("sessionName");

		Session.lookupSession(sessionID).logon();
		Thread.sleep(10000);
	}
}
