/******************************************************************************
 * Copyright 2009-2024 Exactpro (Exactpro Systems Limited)
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

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;

import com.exactpro.sf.common.codecs.AbstractCodec;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.common.util.SendMessageFailedException;
import com.exactpro.sf.services.MessageHelper;
import com.exactpro.sf.services.ServiceException;
import com.exactpro.sf.services.ServiceStatus;
import com.exactpro.sf.services.mina.AbstractMINATCPService;
import com.exactpro.sf.services.mina.MINASession;
import com.exactpro.sf.services.util.ServiceUtil;

public class ITCHTcpClient extends AbstractMINATCPService implements IITCHClient {
    private volatile boolean disconnecting;
    private volatile boolean connectingFromMatrix;

	private volatile boolean reconecting;
	private volatile boolean disposeWhenSessionClosed;
	private volatile boolean externalDisposing = false;

    private final Runnable reconectCommand;
    private final Runnable sentHeartbeatCommand;

    private volatile Future<?> reconnectFuture;

    private int reconnectiongTimeout;
    private ITCHCodecSettings codecSettings;
    protected String namespace;

    public ITCHTcpClient() {
		this.reconectCommand = new Runnable() {
			@Override
			public void run() {
			    if(!externalDisposing) {
                    try {
                        internalStart();
                    } catch (Exception e) {
                        logger.error("Could not connect", e);
                    }
                }
			}
		};

        this.sentHeartbeatCommand = new Runnable() {
            @Override
            public void run() {
                try {
                    sendHeartBeat();
                } catch (Exception e) {
                    logger.error("Could not sent heartbeat", e);
                }
            }
        };
	}

    @Override
    protected void internalInit() throws Exception {
        codecSettings = new ITCHCodecSettings();
        codecSettings.setMsgLength(getSettings().getMsgLength());
        codecSettings.setFilterValues(ServiceUtil.loadStringFromAlias(serviceContext.getDataManager(), getSettings().getFilterValues(), ","));
        codecSettings.setDictionaryURI(getSettings().getDictionaryName());
        codecSettings.setEvolutionSupportEnabled(getSettings().isEvolutionSupportEnabled());
        codecSettings.setTrimLeftPaddingEnabled(getSettings().isTrimLeftPadding());
        if (getSettings().isCompressionUsed()) {
            codecSettings.setChunkDelimiter(ByteBuffer.allocate(2).putShort((short)getSettings().getCompressedChunkDelimeter()).array());
        }

        namespace = dictionary.getNamespace();
        reconecting = getSettings().isReconnecting();
        reconnectiongTimeout = getSettings().getReconnectingTimeout();
        disposeWhenSessionClosed = getSettings().isDisposeWhenSessionClosed();
    }

    @Override
    protected MessageHelper createMessageHelper(IMessageFactory messageFactory, IDictionaryStructure dictionary) {
        MessageHelper messageHelper = new ITCHMessageHelper();
        messageHelper.init(messageFactory, dictionary);
        return messageHelper;
    }

	@Override
    protected int getWriterIdleTimeout() {
        return getSettings().getHeartbeatTimeout();
    }

	@Override
	public void messageSent(IoSession session, Object message) throws Exception {
        for (IMessage subMessage : ITCHMessageHelper.extractSubmessages(message)) {
            super.messageSent(session, subMessage);
		}
	}

    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        for (IMessage subMessage : ITCHMessageHelper.extractSubmessages(message)) {
            super.messageReceived(session, subMessage);
        }
    }

	public boolean sendMessage(IMessage message) throws InterruptedException {
        ITCHSession session = getSession();

        if(session == null) {
            logger.error("Session is null");
            throw new EPSCommonException("Could not send message. Client is not connected");
        }

        session.send(message);

        if(logger.isDebugEnabled()) {
            logger.debug("The message [{}] was sent.", getHumanReadable(message));
        }

		return true;
	}

	@Override
    public void internalStart() throws Exception {
        externalDisposing = false;
        super.internalStart();
        try {
            connect();
        } catch (SendMessageFailedException ex) {
            ITCHTCPClientSettings settings = getSettings();
            if (reconecting && disposeWhenSessionClosed
                    && (settings.isDoLoginOnStart() || settings.isDoLiteLoginOnStart())) {
                // We need to avoid throwing the exception from internalStart method.
                // Otherwise, the start method will throw the exception and the service won't be started at all
                // We should give a chance to reconnecting task
                logger.error("Failed to perform login to the endpoint {}. Wait for reconnect", getEndpointName(), ex);
                changeStatus(ServiceStatus.WARNING, "Failed to perform login", ex);
            } else {
                throw ex;
            }
        }
	}

	@Override
    public void dispose() {
        externalDisposing = true;
        super.dispose();
    }

    @Override
    protected MINASession createSession(IoSession session) {
        ITCHSession itchSession = new ITCHSession(getServiceName(), session,
                getSettings().getMarketDataGroup(), getSettings().getSendMessageTimeout()) {
            @Override
            protected Object prepareMessage(Object message) {
                if (message instanceof IMessage) {
                    IMessage iMessage = (IMessage)message;
                    StringBuilder msg = new StringBuilder("Sending of the message [" + iMessage.getName() + "] with fields such as ");
                    Set<String> fields = iMessage.getFieldNames();

                    for (String field : fields) {
                        msg.append("[").append(field).append("], ");
                    }

                    logger.debug(msg.substring(0, msg.length() - 2));

                    Map<String, String> params = new HashMap<>();

                    params.put(ITCHMessageHelper.FIELD_MARKET_DATA_GROUP_NAME, String.valueOf(getSession().getMarketDataGroup()));
                    params.put(ITCHMessageHelper.FIELD_SEQUENCE_NUMBER_NAME, String.valueOf(getSession().incrementAndGetSequenceNumber()));

                    logger.info("Prepared message: {}", message);

                    return getMessageHelper().prepareMessageToEncode(iMessage, params);
                }

                return super.prepareMessage(message);
            }
        };
        loggingConfigurator.registerLogger(itchSession, getServiceName());
        return itchSession;
    }

    @Override
    protected void preConnect() throws Exception {
        disconnecting = false;
        super.preConnect();
    }

    @Override
    protected void handleNotConnected(Throwable throwable) {
        if(reconecting) {
            cancelReconnectFuture();
            reconnectFuture = taskExecutor.schedule(reconectCommand, reconnectiongTimeout, TimeUnit.MILLISECONDS);
            logger.error("Cannot establish session to address: {}:{}", getHostname(), getPort());
        } else {
            super.handleNotConnected(throwable);
        }
    }

    @Override
    public void sessionClosed(IoSession session) throws Exception {
        super.sessionClosed(session);
        disconnected0();
    }

    @Override
    protected void connectionAborted(IoSession session, Throwable cause) {
        super.connectionAborted(session, cause);
        // The connection was hard-reset. Need to perform the reconnection if it is configured
        disconnected0();
    }

    protected void disconnected0() {
		if ( !disconnecting  && !connectingFromMatrix && disposeWhenSessionClosed) {

			try {
                disposeResources();
			}
			catch (Exception e ) {
				logger.error("Problem while disconnecting session", e);
			}

			if ( reconecting ) {
			    cancelReconnectFuture();
                reconnectFuture = taskExecutor.schedule(reconectCommand, reconnectiongTimeout, TimeUnit.MILLISECONDS);
                changeStatus(ServiceStatus.WARNING, "Connection was forcibly closed by the remote machine."
                        + "Service will be reconnect after " + reconnectiongTimeout + " milliseconds");
            } else {
                changeStatus(ServiceStatus.ERROR, "Connection was forcibly closed by the remote machine");
			}
		} else {
			if (connectingFromMatrix) {
				connectingFromMatrix = false;
			}
		}
	}

	@Override
    protected void preDisconnect() throws Exception {
	    disconnecting = true;
	    super.preDisconnect();
	}

    private void cancelReconnectFuture() {
        Future<?> future = reconnectFuture;
        if(future != null && !future.isDone()) {
            logger.info("Canceling reconnect task for " + getServiceName());
            future.cancel(true);
            logger.info("Canceled reconnect task for " + getServiceName());
        }
    }

	protected void sendLiteLogin() throws InterruptedException {
        ITCHTCPClientSettings settings = getSettings();

        IMessage liteLogin = messageFactory.createMessage("LoginRequestLite", namespace);

		liteLogin.addField("Username", settings.getUsername());
		liteLogin.addField("Flag1", settings.getFlag1());

		sendMessage(liteLogin);
	}

	protected void sendLogin() throws InterruptedException {
        ITCHTCPClientSettings settings = getSettings();

        IMessage liteLogin = messageFactory.createMessage("LoginRequest", namespace);

		liteLogin.addField("Username", settings.getUsername());

		sendMessage(liteLogin);
	}

	protected void sendHeartBeat() throws InterruptedException {
        ITCHSession session = getSession();

        if(session == null) {
            throw new ServiceException("Could not send a heartbeat. Client is not connected");
        }

        logger.info("Client sent UnitHeader with namespace {}", namespace);
        IMessage unitHeader = messageFactory.createMessage("UnitHeader", namespace);

		unitHeader.addField("Length", 8);
		unitHeader.addField("MessageCount",(short) 0);
        unitHeader.addField("MarketDataGroup", (short)session.getMarketDataGroup());
		unitHeader.addField("SequenceNumber", (long)session.getSequenceNumber() + 1);

		sendMessage(unitHeader);
	}

	@Override
	protected void postConnect() throws Exception {
	    super.postConnect();
        ITCHTCPClientSettings settings = getSettings();

		if(settings.isDoLiteLoginOnStart()) {
			sendLiteLogin();
		}

		if(settings.isDoLoginOnStart()) {
			sendLogin();
		}
	}

	@Override
	public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
        super.sessionIdle(session, status);

        if(getSettings().isSendHeartBeats()) {
            taskExecutor.schedule(sentHeartbeatCommand, 0, TimeUnit.MILLISECONDS);
		}
	}

	@Override
    public ITCHSession getSession() {
        return (ITCHSession) super.getSession();
    }

    @Override
    public ITCHTCPClientSettings getSettings() {
        return (ITCHTCPClientSettings) super.getSettings();
	}

	@Override
    protected ITCHCodecSettings getCodecSettings() {
        return codecSettings;
    }

    @Override
    protected Class<? extends AbstractCodec> getCodecClass() throws Exception {
        return ITCHCodec.class;
    }

	public void setConnectingFromMatrix(boolean connectingFromMatrix) {
		this.connectingFromMatrix = connectingFromMatrix;
	}

    @Override
    protected String getHostname() {
        return getSettings().getAddress();
    }

    @Override
    protected int getPort() {
        return getSettings().getPort();
    }
    @Override
    protected AbstractCodec getUpdateCodec(AbstractCodec codec) {
        return getSettings().isCompressionUsed() ? new ITCHDeflateCodec(codec) : codec;
    }
}
