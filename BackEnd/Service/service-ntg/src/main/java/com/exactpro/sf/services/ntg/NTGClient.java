/******************************************************************************
 * Copyright 2009-2021 Exactpro (Exactpro Systems Limited)
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

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;

import com.exactpro.sf.common.codecs.AbstractCodec;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.services.MessageHelper;
import com.exactpro.sf.services.ServiceException;
import com.exactpro.sf.services.ServiceStatus;
import com.exactpro.sf.services.mina.AbstractMINATCPService;
import com.exactpro.sf.services.mina.MINASession;
import com.exactpro.sf.services.ntg.exceptions.InvalidClientStateException;
import com.exactpro.sf.services.ntg.exceptions.NotLoggedInException;

/**
 * This is an implementation of MINA IoHandlerAdapter for
 * NTG protocol.
 *
 * Encapsulate client all logic including heartbeats,
 * logon, logout....
 */
public final class NTGClient extends AbstractMINATCPService {
	private static final String MESSAGE_LOGON = "Logon";

	private String messageNamespace;
    private volatile NTGClientState state = NTGClientState.LoggedOut;
    private volatile NTGClientState statePrev = state;
	private Future<?> disconnectFuture;
	private volatile Future<?> reconnectFuture;
	private volatile boolean externalDisposing = false;

	@Override
	public final void messageReceived(IoSession session, Object message) throws Exception {
	    super.messageReceived(session, message);

		if(!( message instanceof IMessage )) {
            logger.error("Message is not typeof [IMessage] but [{}]", message.getClass());
			return;
		}

        IMessage ntgMessage = (IMessage)message;

		taskExecutor.addTask(new Runnable() {
            @Override
            public void run() {
                try {
                    processMessage(ntgMessage);
                } catch(Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        });
	}

	@Override
	public final void messageSent(IoSession session, Object message) throws Exception {
        super.messageSent(session, message);

		if(!( message instanceof IMessage )) {
            logger.error("Message is not typeof [IMessage] but [{}]", message.getClass());
			return;
		}

        IMessage ntgMessage = (IMessage) message;

        if(state == NTGClientState.SessionCreated && MESSAGE_LOGON.equals(ntgMessage.getName())) {
		    // Logon handling for AML 3
            updateState(NTGClientState.WaitingLogonReply);
		}
	}

	@Override
	public final void sessionClosed(IoSession session) throws Exception {
        super.sessionClosed(session);

        if(state.value >= NTGClientState.SessionCreated.value) {
            destroyHeartbeatTimer();
            this.state = NTGClientState.SessionClosed;
		}
        Long reconnectTimeout = getSettings().getReconnectTimeout();
        if(reconnectTimeout != 0L) {
            scheduleReconnectingTask(reconnectTimeout);
        }
	}

	@Override
	public final void sessionIdle(IoSession session, IdleStatus status) throws Exception {
        super.sessionIdle(session, status);
        taskExecutor.addTask(() -> {
            try {
                sendHeartbeat();
            } catch(InterruptedException e) {
                throw new ServiceException("Failed to send heartbeat", e);
            }
        });
	}

    @Override
    protected void internalStart() throws Exception {
        super.internalStart();
        connect();
    }

    private void autoLogin() throws InterruptedException {
        if (getSettings().isDoLoginOnStart()) {
            login();

            WaitLogin waitLogin = new WaitLogin();
            Thread waitThread = new Thread(waitLogin);
            waitThread.start();
            waitThread.join();

            if(waitLogin.getExitCode() != ExitCode.BySuccessLogin) {
                logger.debug("{}", waitLogin.getExitCode());
                String msg = String.format("Cannot login during the predefined timeout: [%d]. User [%s], password [%s], IP [%s], port [%d].",
                        getSettings().getLoginTimeout(), getSettings().getLogin(), getSettings().getPassword(),
                        getSettings().getServerIP(), getSettings().getServerPort());

                logger.error(msg);
                throw new ServiceException(msg);
            }
        } else {
            logger.debug("Service {}. DoLoginOnStart == false", this);
        }
    }

    @Override
    protected void internalInit() throws Exception {
        this.messageNamespace = dictionary.getNamespace();
        NTGClientSettings settings = getSettings();
        if(settings.getReconnectTimeout() < 0L) {
            throw new IllegalStateException("Reconnect Timeout can't be less than zero.");
        }
        if(settings.getReconnectTimeout() != 0L && (!settings.isAutosendHeartbeat() || !settings.isDoLoginOnStart())) {
            throw new IllegalStateException("Using reconnect without 'Do Login On Start' and 'Autosend Heartbeat' is not allowed.");
        }
    }

	@Override
    public NTGSession getSession() {
        return (NTGSession) super.getSession();
	}

	public final void sendMessage(IMessage message) throws InterruptedException {
		sendMessageInternal(message, true);
	}

	private void sendMessageInternal(IMessage message, boolean waitFuture) throws InterruptedException {
        if((state != NTGClientState.LoggedIn) && !getSettings().isLowLevelService()) {
            logger.debug("NTGClient. ERROR: not logged on to the server. Cannot send the messge.");
			throw new NotLoggedInException();
		}

        NTGSession session = getSession();

        if(session == null) {
            logger.error("Service {} sendMessage: session is null", this);
            logger.error("State: {}", state);

            if(state == NTGClientState.SessionDropped) {
                throw new EPSCommonException("Could not send message - connection was closed by server.");
            } else {
                throw new EPSCommonException("Could not send message - connection closed.");
            }
        } else {
            session.send(message);
		}

		logger.debug("Message has been successfully sent: [{}].", message);
	}

	@Override
    protected int getWriterIdleTimeout() {
        return getSettings().getHeartbeatTimeoutInSeconds();
    }

    @Override
    protected Class<? extends AbstractCodec> getCodecClass() throws Exception {
        return NTGCodec.class;
    }

    @Override
    protected String getHostname() {
        return getSettings().getServerIP();
    }

    @Override
    protected int getPort() {
        return getSettings().getServerPort();
    }

    @Override
    protected long getConnectTimeout() {
        return getSettings().getConnectTimeout();
    }

    @Override
    protected MINASession createSession(IoSession session) {
        NTGSession ntgSession = new NTGSession(serviceName, session, getSettings().getSendMessageTimeout()) {
            @Override
            protected Object prepareMessage(Object message) {
                if (message instanceof IMessage) {
                    getMessageHelper().prepareMessageToEncode((IMessage)message, null);
                }

                return message;
            }
        };
        loggingConfigurator.registerLogger(ntgSession, getServiceName());
        return ntgSession;
    }

    @Override
    protected void postConnect() throws Exception {
        super.postConnect();
        updateState(NTGClientState.SessionCreated);
        restartDisconnectTimer();

        autoLogin();
    }

    private void updateState(NTGClientState newState) {
		statePrev = state;
		state = newState;
	}

	@Override
    protected void preDisconnect() throws Exception {
        if(getSession() != null) {
            destroyHeartbeatTimer();
        }

        super.preDisconnect();
    }

    @Override
    protected long getDisconnectTimeout() {
        return getSettings().getLoginTimeout();
    }

    @Override
    protected void postDisconnect() throws Exception {
        super.postDisconnect();

        if(getSession() != null) {
            updateState(NTGClientState.SessionClosed);
        }
    }

    @Override
    public void dispose() {
        externalDisposing = true;
        super.dispose();
    }

    @Override
    public void preConnect() throws Exception {
        externalDisposing = false;
        super.preConnect();
    }

    @Override
    protected void handleNotConnected(Throwable throwable) {
	    Long reconnectTimeout = getSettings().getReconnectTimeout();
	    if(reconnectTimeout == 0L) {
	        super.handleNotConnected(throwable);
        } else {
            scheduleReconnectingTask(reconnectTimeout);
        }
    }

    @Override
    protected void connectionAborted(IoSession session, Throwable cause) {
        Long reconnectTimeout = getSettings().getReconnectTimeout();
        super.connectionAborted(session, cause);
        if(reconnectTimeout != 0) {
            scheduleReconnectingTask(reconnectTimeout);
        }
    }

    @Override
    protected void disposeResources() {
        if (externalDisposing) {
            cancelCurrentReconnectFuture();
        }
	    super.disposeResources();
    }

    private void cancelCurrentReconnectFuture() {
	    if(reconnectFuture != null && !reconnectFuture.isDone()) {
            logger.info("Canceling reconnect task for " + getServiceName());
            reconnectFuture.cancel(true);
            logger.info("Reconnect task for " + getServiceName() + " is cancelled");
        }
    }

    private void scheduleReconnectingTask(Long reconnectionTimeout) {
        if (externalDisposing) {
            logger.info("External disposing. Do not execute reconnect");
            return;
        }
        changeStatus(ServiceStatus.WARNING, "Connection was closed. Try to reconnect in "
                + reconnectionTimeout + " milliseconds");
        cancelCurrentReconnectFuture();
        reconnectFuture = taskExecutor.schedule(this::reconnect, reconnectionTimeout, TimeUnit.MILLISECONDS);
    }

    private void reconnect() {
        try {
            disposeResources();
        } catch (Exception ex) {
            logger.error("Error during disposing resources", ex);
        }

        if(loggingConfigurator != null)
            loggingConfigurator.createAndRegister(getServiceName(), this);

        try {
            internalStart();
        } catch (Exception ex) {
            logger.error("Error during reconnecting", ex);
        }
    }

	private void destroyHeartbeatTimer() {
        if(disconnectFuture != null) {
			try {
				disconnectFuture.cancel(false);
				disconnectFuture = null;
			} catch(Exception e){
				logger.error(e.getMessage(), e);
			}
		}
	}

	public void login() throws InterruptedException {
        NTGSession session = getSession();

        if(session != null && (state == NTGClientState.SessionCreated || state == NTGClientState.LoggedOut)) {
			IMessage message = messageFactory.createMessage(MESSAGE_LOGON, messageNamespace);
            message.addField("CompID", getSettings().getLogin());
            message.addField("Username", getSettings().getLogin());
            message.addField("Password", getSettings().getPassword());
            message.addField("NewPassword", getSettings().getNewPassword());
            message.addField("MessageVersion", getSettings().getMessageVersion());

			// set state _before_ actual send (LogonReply will be received in different thread)
            updateState(NTGClientState.WaitingLogonReply);
            session.send(message);
		}
	}

	public void logout() throws InterruptedException {
        NTGSession session = getSession();

        if(session != null && state == NTGClientState.LoggedIn) {
            session.setLoggedOn(false);

			IMessage message = messageFactory.createMessage("Logout", messageNamespace);
			message.addField("LogoutReason", "ClientRequest");
            message.addField("Reason", "ClientRequest");

            updateState(NTGClientState.LoggedOut);
            session.send(message);

			//FIXME
			try {
                Thread.sleep(getSettings().getLogoutTimeout());
			} catch(Exception e){
				logger.warn(e.getMessage(), e);
			}
		}
	}

	/**
	 * Evaluate message whether it can be processed by client.
	 * This is particular case of Heartbeat, Logon ...
	 *
     * @param message instance of NTG message
	 * @throws Exception
	 */
    private void processMessage(IMessage message) throws Exception {
		String msgName = message.getName();
        logger.debug("isMessageProcessed state [{}]", state);

        switch(state) {
		case WaitingLogonReply:
            if("LogonReply".equals(msgName)) {
				evaluateServerReply(message);
            } else if("LogonResponse".equals(msgName)) {
                evaluateServerReply(message);
            } else if("Heartbeat".equals(msgName)) {
				restartDisconnectTimer();
            } else if("Reject".equals(msgName)) {
			    evaluateReject(message);
			} else {
				rejectMessage(message, RejectReason.NotLoggedIn);
			}
			break;

		case LoggedIn:
            if("Heartbeat".equals(msgName)) {
				restartDisconnectTimer();
			}
			break;

		case LoggedOut :
			rejectMessage(message, RejectReason.NotLoggedIn);
			break;

		case SessionCreated:
            if("Heartbeat".equals(msgName)) {
				restartDisconnectTimer();
			}
			break;

		case SessionClosed :
			break;

		default:
			throw new InvalidClientStateException(
                    String.format("Client state [%s] cannot be processed.", state.toString()));
		}
	}

    private void evaluateServerReply(IMessage serverMessage) {
		Integer rejectCode = Integer.parseInt( serverMessage.getField( "RejectCode" ).toString()) ;

		switch(rejectCode) {
		case 0:
            updateState(NTGClientState.LoggedIn);
			break;

		default:
			// set state to the previous one
			updateState(statePrev);
			break;
		}
	}

    private void evaluateReject(IMessage serverMessage) throws Exception {
	    Integer rejectCode = serverMessage.getField("RejectCode");
	    String rejectReason = serverMessage.getField("RejectReason");
	    String errorMessage = String.format("Received reject - code: %s, reason: %s", rejectCode, rejectReason);

	    logger.error(errorMessage);

        if(getSettings().isDoLoginOnStart()) {
	        dispose();
	        changeStatus(ServiceStatus.ERROR, errorMessage, null);
	    } else {
	        disconnect();
	    }
	}

    private void rejectMessage(IMessage serverMessage, RejectReason rejectReason) throws InterruptedException {
		String msgType = (String)((IMessage)serverMessage.getField("MessageHeader")).getField("MessageType");

		IMessage message = messageFactory.createMessage("Reject", messageNamespace);

		message.addField("RejectedMessageType", msgType );
		message.addField("RejectCode", 4);
		message.addField("RejectReason",rejectReason.toString());
		if (serverMessage.isFieldSet("ClOrdID")) {
			message.addField("ClOrdID", serverMessage.getField("ClOrdID"));
            message.addField("ClientOrderID", serverMessage.getField("ClientOrderID"));
		} else {
			message.addField("ClOrdID", "00000000000000000000");
            message.addField("ClientOrderID", "00000000000000000000");
		}

        sendMessageInternal(message, false);
	}

	private void sendHeartbeat() throws InterruptedException {
        if(!getSettings().isAutosendHeartbeat()) {
	        return;
	    }

        if(state != NTGClientState.LoggedIn) {
            return;
        }

		IMessage message = messageFactory.createMessage("Heartbeat", messageNamespace);
        NTGSession session = getSession();

        if(session != null) {
            session.send(message);
        } else {
            logger.error("sendHeartbeat: session is null");
        }

		logger.debug("Message has been successfully sent: [{}].", message);
	}

	private void restartDisconnectTimer() throws InterruptedException {
		if (!isConnected()) {
            logger.debug("session is null. Don't start Heartbeat timer");
			return;
		}

        logger.debug("Set time for logout() in {} milliseconds.", getSettings().getForceLogoutTimeout());

        sendHeartbeat();

        if(disconnectFuture != null) {
			disconnectFuture.cancel(false);
		}

		disconnectFuture = taskExecutor.schedule(new Runnable() {
			@Override
			public void run() {
				logger.debug("Going to invoke disconnect() due to missed more then allowed server heartbeats {}.",
                        getSettings().getMaxMissedHeartbeats());
                try {
                    disconnect();
                } catch(Exception e) {
                    throw new ServiceException(e);
                }
			}
        }, getSettings().getForceLogoutTimeout(), TimeUnit.MILLISECONDS);
	}

    public NTGClientState getState() {
        return state;
	}

	// Logon operation runnable task
	class WaitLogin implements Runnable {
		private ExitCode exitCode = ExitCode.Undefined;

		@Override
		public void run() {
			exitCode = ExitCode.ByTimeout;
            long timeEnd = System.currentTimeMillis() + getSettings().getLoginTimeout();

			do {
				Thread.yield();

                if(state == NTGClientState.LoggedIn) {
					exitCode = ExitCode.BySuccessLogin;
					break;
				}
			}
			while(timeEnd > System.currentTimeMillis());

            if(exitCode == ExitCode.BySuccessLogin) {
                NTGSession session = getSession();

                if(session != null) {
                    session.setLoggedOn(true);
                    String msg = String.format("Successfully logged in. User [%s], password [%s], IP [%s], port [%d].",
                            getSettings().getLogin(), getSettings().getPassword(),
                            getSettings().getServerIP(), getSettings().getServerPort());
                    logger.debug(msg);
                } else {
                    String msg = String.format("Successfully logged in, but session is null. User [%s], password [%s], IP [%s], port [%d].",
                            getSettings().getLogin(), getSettings().getPassword(),
                            getSettings().getServerIP(), getSettings().getServerPort());
                    logger.error(msg);
                }

			}
		}

		public ExitCode getExitCode() {
			return exitCode;
		}
	}

	/************* Enums  ***********/

    public enum NTGClientState {
		SessionDropped ( 0 )
		, SessionCreated ( 1 )
		, LoggedOut ( 2 )
		, WaitingLogonReply ( 3 )
		, LoggedIn ( 4 )
		, SessionClosed( 5 );

		final int value;

        NTGClientState(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}

        public static NTGClientState getEnumValue(int value) {
			switch (value) {
			case 1:
				return SessionCreated;

			case 2:
				return LoggedOut;

			case 3:
				return WaitingLogonReply;
			case 4:
				return LoggedIn;

			case 5:
				return SessionClosed;

			default:
				throw new EPSCommonException(
                        String.format("Value [%d] is out of range of NTGClientState.", value));
			}
		}
	}

	enum RejectReason {
        NotLoggedIn
    }

	enum DisconnectReason {
		ClientRequest,
        Unknown
    }

	enum LogoutReason {
		ClientRequest,
        Unknown
    }

	public enum ExitCode {
		Undefined,
		ByTimeout,
        BySuccessLogin
    }

	@Override
    public NTGClientSettings getSettings() {
        return (NTGClientSettings) super.getSettings();
	}

    @Override
    protected MessageHelper createMessageHelper(IMessageFactory messageFactory, IDictionaryStructure dictionary) {
        MessageHelper messageHelper = new NTGMessageHelper();
        messageHelper.init(this.messageFactory, this.dictionary);
        return messageHelper;
    }

    @Override
    public MessageHelper getMessageHelper() {
        return super.getMessageHelper();
    }
}
