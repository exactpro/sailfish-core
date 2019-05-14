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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.impl.messages.MapMessage;
import com.exactpro.sf.common.messages.IMessage;


public final class NTGServerTest extends IoHandlerAdapter
{

	//
	// FIELDS
	//

    private static final Logger logger = LoggerFactory.getLogger(NTGServerTest.class);

    private static final String PROP_FILE = "NTGServer.properties";

	private ServerStrategy strategy = ServerStrategy.Normal;

	// Number of missed server heartbeat messages forcing the client to
	// break the connection.
	//private static final int MAX_MISSED_HEARTBEATS = 5;

	// Accumulative idle timeout in milliseconds
	// for 5 missed server heartbeats
	//private static final int MAX_CLIENT_HEARTBEAT_TIMEOUT = 3000 * MAX_MISSED_HEARTBEATS ;

	//private Map<Long, IoSession> sessions = new HashMap<Long, IoSession>();

	// Client Heartbeat message timer
	//private Timer timerClientHeartbeat = null;


	//private IMessageDictionary dictionary = null;
	//private IMessageFactory messageFactory = null;

	private final ServerProperties serverProperties ;

	private final Map<String, String> validClients ;
    private final Map<Long, ConnectedClient> clients = new HashMap<>();
    private static final Object syncObjClients = new Object();

    NTGServerTest()
	throws IOException
	{
		serverProperties = readPropertiesFile(PROP_FILE );

		validClients = new HashMap<>();
		validClients.put("muriel", "muriel" );
		validClients.put("eustace", "eustace" );
		validClients.put("courage", "courage" );

		for( int i = 0 ; i < 10000 ; i++ )
		{
            String clnt = String.format("Client_%d", 1 + i);
			validClients.put(clnt, clnt );
		}
	}

    NTGServerTest(ServerStrategy strategy)
	throws IOException
	{
		this();
		this.strategy = strategy;

	}


	@Override
	public void sessionCreated(IoSession session)
	throws Exception
	{
		logger.trace( "    Server: sessionCreated() Session ID = [{}].", session.getId() );

		synchronized(syncObjClients)
		{
            if(!clients.containsKey(session.getId()))
			{
                clients.put(session.getId(), new ConnectedClient(session));
			}
			else
			{
				logger.trace( "    Server: sessionCreated(). !!! Session ID = [{}] already in the map.", session.getId());
			}
		}
	}


	@Override
	public void sessionOpened(IoSession session) throws Exception
	{
		logger.trace( "    Server: sessionOpened() Session ID = [{}].", session.getId() );

		synchronized(syncObjClients)
		{
            if(!clients.containsKey(session.getId()))
			{
                clients.put(session.getId(), new ConnectedClient(session));
			}
			else
			{
				logger.trace( "    Server: sessionOpened(). !!! Session ID = [{}] already in the map.", session.getId());
			}
		}
	}

	@Override
	public void sessionClosed(IoSession session) throws Exception
	{
		logger.trace( "    Server: sessionClosed(). Closing session ID = [{}].", session.getId());

		synchronized(syncObjClients)
		{
            if(clients.containsKey(session.getId()))
			{
                clients.remove(session.getId());
			}
		}
	}

	@Override
	public void sessionIdle(IoSession session, IdleStatus status)
	throws Exception
	{
		logger.trace( "    Server: onSessionIdle().");

        if(strategy != ServerStrategy.NoHeartbeat)
		{
            IMessage heartBeat = new MapMessage("NTG", "Heartbeat");
			heartBeat.addField("MessageHeader", getHeader());

			synchronized(syncObjClients)
			{
                if(clients.containsKey(session.getId()))
				{
                    ConnectedClient clnt = clients.get(session.getId());
					clnt.sentMessage(heartBeat);
				}
			}
		}
	}

	@Override
	public void exceptionCaught(IoSession session, Throwable cause)
	throws Exception
	{
		onException( cause );
	}

	@Override
	public void messageReceived(IoSession session, Object message)
	throws Exception
	{
		if(!( message instanceof IMessage ))
		{
			logger.trace("    Sever: message is not typeof [IMessage] but [{}]", message.getClass());
			return ;
		}

		logger.trace("    Server: Received message [{}]", message);

        IMessage ntgMessage = (IMessage) message;
		ConnectedClient clnt  = null;

		synchronized(syncObjClients)
		{
            if(clients.containsKey(session.getId()))
			{
                clnt = clients.get(session.getId());
			}
		}
        processMessage(clnt, ntgMessage);
	}


	@Override
	public void messageSent(IoSession session, Object message)
	throws Exception
	{
		onMessageSent();
	}

    private void processMessage(ConnectedClient cln, IMessage clientMessage)
	throws Exception
	{

        switch(strategy)
		{
		case HeartbeatOnly:
			break;

		case  LogonReject:
			strategyLogonReject(cln, clientMessage);
			break;

		case Normal:
			strategyNormal(cln, clientMessage);
			break;

		case NoHeartbeat:
			strategyNoHeartbeat(cln, clientMessage);
			break;
        default:
            break;
		}
	}

    private boolean strategyLogonReject(ConnectedClient cln, IMessage clientMessage)
	{
		return true;
	}

    private boolean strategyNormal(ConnectedClient cln, IMessage clientMessage)
	throws Exception
	{

        MessageType msgType = MessageType.getEnumValue(
				((IMessage)clientMessage.getField("MessageHeader")).getField("MessageType").toString());

		switch(msgType)
		{
		case Logon :
			replyToClientLogon( cln, clientMessage);
            return true;

        case Heartbeat:
            return true;

        case Logout:
			replyToClientLogout( cln, clientMessage);
            return true;

        default:
			throw new Exception(String.format( "Invalid message type [%s].",
					msgType.toString() ));
		}
    }

    private boolean strategyNoHeartbeat(ConnectedClient cln, IMessage clientMessage)
	throws Exception
	{

        MessageType msgType = MessageType.getEnumValue(
				((IMessage)clientMessage.getField("MessageHeader")).getField("MessageType").toString());

		switch(msgType)
		{
		case Logon :
			replyToClientLogon( cln, clientMessage);
            return true;

        case Heartbeat:
            logger.trace("NTGServer. Received heartbeat from client [{}]. Server heartbeat supressed.", cln.session.getId());
            return true;

        case Logout:
			replyToClientLogout( cln, clientMessage);
            return true;

        default:
			throw new Exception(String.format( "Invalid message type [%s].",
					msgType.toString() ));
		}
    }

    private void replyToClientLogon(ConnectedClient clnt, IMessage clientMessage)
	{
		IMessage logon = clientMessage.cloneMessage();
		clnt.addRecievdMessage(clientMessage);

		if(clnt.isLoggedIn())
		{
            IMessage header = new MapMessage("NTG", "MessageHeader");
			header.addField("StartOfMessage", 2); // StartOfMessage.ClientStartOfMessage;
			header.addField("MessageType", MessageType.LogonReply.getType());
			header.addField("MessageLength", 59);

            IMessage logonReply = new MapMessage("NTG", "LogonReply");
			logonReply.addField("MessageHeader", header);
			logonReply.addField("RejectCode", 1);
			//			logonReply.setRejectReason("Aready logged in." );
			logonReply.addField("PasswordExpiryDayCount", "333");

			clnt.sentMessage(logonReply);
			return;
		}


		if(! isClientDefined( (String)logon.getField("Username"), (String)logon.getField("Password") ))
		{
            IMessage header = new MapMessage("NTG", "MessageHeader");
            header.addField("StartOfMessage", 2); // StartOfMessage.ClientStartOfMessage;
            header.addField("MessageType", MessageType.LogonReply.getType());
            header.addField("MessageLength", 59);

            IMessage logonReply = new MapMessage("NTG", "LogonReply");
            logonReply.addField("MessageHeader", header);
            logonReply.addField("RejectCode", 2);
			//				logonReply.setRejectReason("Unknown client." );
            logonReply.addField("PasswordExpiryDayCount", "333");
			clnt.sentMessage(logonReply);
			return;
		}

        IMessage header = new MapMessage("NTG", "MessageHeader");
        header.addField("StartOfMessage", 2); // StartOfMessage.ClientStartOfMessage;
        header.addField("MessageType", MessageType.LogonReply.getType());
        header.addField("MessageLength", 59);

        IMessage logonReply = new MapMessage("NTG", "LogonReply");
        logonReply.addField("MessageHeader", header);
        logonReply.addField("RejectCode", 0);
		//		logonReply.setRejectReason("");
        logonReply.addField("PasswordExpiryDayCount", "333");
		clnt.sentMessage(logonReply);
		clnt.Login((String)logon.getField("Username"), (String)logon.getField("Password"));
	}

    private void replyToClientLogout(ConnectedClient cln, IMessage clientMessage)
	{
		//Logout msgLogout = new Logout(clientMessage);

	}


	private static IMessage getHeader()
	{
        IMessage header = new MapMessage("NTG", "MessageHeader");
		header.addField("StartOfMessage", 2); // StartOfMessage.ClientStartOfMessage;
		header.addField("MessageLength", 0);
		header.addField("MessageType", MessageType.Heartbeat.getType()); // MessageType.Heartbeat;

		return header;
	}


	public final void sendMessage(IoSession session, IMessage message)
	{
		try
		{
            if(session == null)
			{
				throw new IllegalArgumentException("Connection is not established. Invoke connect(host, port) first.");
			}

			session.write( message);

			logger.trace("    Server: Message has been successfully sent: [{}].", message);

		}
		catch (Exception e)
		{
			logger.error(e.getMessage(), e);
			e.printStackTrace();

		}
    }


	private void onMessageSent()
	{
		logger.trace( "    Server: onMessageSent().");
	}

	private void onException(Throwable throwable)
	{
		logger.trace( "    Server: onException().");
		throwable.printStackTrace();
	}


	private ServerProperties readPropertiesFile(String propfileName ) throws IOException
	{
        if(propfileName == null)
		{
			throw new IllegalArgumentException("Parameter [propfileName] cannot be null.");
		}

		ServerProperties serverProperties = new ServerProperties();
		String mappedPropertiesFile = getBaseDir() + File.separator + propfileName;

        if(new File(mappedPropertiesFile).exists())
		{
			try
			{
                //InputStream is = (InputStream) NTGClient.class.getResourceAsStream(propfileName);
				//is.close();
				Properties prop = new Properties();
				prop.load(new FileInputStream( mappedPropertiesFile ));
                return new ServerProperties(prop);

			}
			catch(Exception e)
			{
				e.printStackTrace();
				logger.trace("Failed to read from [{}] file.", mappedPropertiesFile);
			}
		}
		return serverProperties;
	}

	private class ServerProperties
	{
        Properties prop;

		public ServerProperties()
		{
		}

		public ServerProperties( Properties prop )
		{
			this.prop = prop;
		}

        private final int heartbeatTimeout = 3000;

		// Number of missed client heartbeat messages forcing the client to
		// break the connection.
        private final int maxMissedHeartbeats = 5;


		public int getHeartbeatTimeout()
		{
            if(prop != null && prop.containsKey("HeartbeatTimeout"))
			{
				return Integer.parseInt( prop.getProperty("HeartbeatTimeout"));
			}
			else
			{
				return heartbeatTimeout;
			}
		}

		public int getMaxMissedHeartbeats()
		{
            if(prop != null && prop.containsKey("MaxMissedHeartbeats"))
			{
				return Integer.parseInt( prop.getProperty("MaxMissedHeartbeats"));
			}
			else
			{
				return maxMissedHeartbeats;
			}
		}

		public int getMaxClientHeartbeatTimeout()
		{
			return getHeartbeatTimeout() * getMaxMissedHeartbeats();
		}

	}

	private String getBaseDir()
	{
        return (System.getProperty("basedir") == null) ? "." : System.getProperty("basedir");
    }

    private boolean isClientDefined(String name, String password) {

        if(validClients.containsKey(name)) {
            String thisPassword = validClients.get(name);

            if(thisPassword.compareTo(password) == 0) {
                return true;
            }
        }
        return false;
    }


	final class ConnectedClient implements Comparable <ConnectedClient>
	{
        private final IoSession session;
        private final List<IMessage> messagesIn = new ArrayList<>();
        private final List<IMessage> messagesOut = new ArrayList<>();
		private String name;
		private String password;
        private final String newPassword = "XYZ";
        private Timer timerClientHeartbeat;
        private boolean loggedIn;

		public ConnectedClient( IoSession session )
		{
			this.session = session;
		}

		public void Login(String name, String password )
		{
			this.name = name;
			this.password = password;
			loggedIn = true;
		}

		public IoSession getSession()
		{
            return session;
		}

		public void addRecievdMessage(IMessage message)
		{
			messagesIn.add(message);
		}

		public String getName()
		{
			return name;
		}

		public String getPassword()
		{
			return password;
		}

		public boolean isLoggedIn()
		{
			return loggedIn;
		}

		public String getNewPassword()
		{
			return newPassword;
		}

		public int getDaysPasswordExpiredIn()
		{
			return 0;
		}

		public void sentMessage(IMessage message)
		{
			session.write( message);
			messagesOut.add(message);
		}

		@Override
		public int compareTo(ConnectedClient other)
		{
            if(session.getId() != other.getSession().getId())
			{
                return Long.compare(session.getId(), other.getSession().getId());
            } else if(name.compareTo(other.getName()) != 0)
			{
                return name.compareTo(other.getName());
            } else if(password.compareTo(other.getPassword()) != 0)
			{
                return password.compareTo(other.getPassword());
			}
			else
			{
				return 0;
			}
		}

		public void restartTimer()
		{
			timerClientHeartbeat = null;
			timerClientHeartbeat = new Timer();

			// Restart timer
			//this.timerClientHeartbeat.scheduleAtFixedRate(new TimerTask()
            timerClientHeartbeat.schedule(new TimerTask()
			{
				@Override
				public void run()
				{
					logger.trace( "    Client [{}]: Session ID [{}] due to client missed heartbeats.", name, session.getId() );
					//session.close( true );
				}
			}, serverProperties.getMaxClientHeartbeatTimeout() );
		}
	}

	public enum ServerStrategy
	{
		// Normal functioning
		Normal,

		// Do not generate heartbeat
		NoHeartbeat,

		// Only heartbeats, other messages are supressed
		HeartbeatOnly,

		// Heartbeats and reject attempt to logon
		LogonReject,

        UnexpectedShutdown
    }

	public enum MessageType {
	    LogonReply("B"),
	    Heartbeat("0"),
	    Logon("A"),
	    Logout("5");

        private final String type;

        MessageType(String type) {
	        this.type = type;
	    }

	    public String getType() {
	        return type;
	    }

	    public static MessageType getEnumValue(String type) {
	        for(MessageType e : values()) {
	            if(e.type.equals(type)) {
	                return e;
	            }
	        }

            return null;
	    }
	}
}
