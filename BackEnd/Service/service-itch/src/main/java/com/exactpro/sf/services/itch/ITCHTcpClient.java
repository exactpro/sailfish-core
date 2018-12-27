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
package com.exactpro.sf.services.itch;

import com.exactpro.sf.common.codecs.AbstractCodec;
import com.exactpro.sf.common.codecs.CodecFactory;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.services.MessageHelper;
import com.exactpro.sf.services.ServiceException;
import com.exactpro.sf.services.ServiceStatus;
import com.exactpro.sf.services.codecs.HackedProtocolCodecFilter;
import com.exactpro.sf.services.mina.AbstractMINATCPService;
import com.exactpro.sf.services.mina.MINASession;
import com.exactpro.sf.services.util.ServiceUtil;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class ITCHTcpClient extends AbstractMINATCPService implements IITCHClient {
	private volatile boolean disconnecting = false;
	private volatile boolean connectingFromMatrix = false;

	private volatile boolean reconecting;
	private volatile boolean disposeWhenSessionClosed;

	private Runnable reconectCommand;
    private Runnable sentHeartbeatCommand;

    private int reconnectiongTimeout;
    private ITCHCodecSettings codecSettings;
    protected String namespace;

    public ITCHTcpClient() {
		this.reconectCommand = new Runnable() {
			@Override
			public void run() {
				try {
				    internalStart();
				}
				catch ( Exception e ) {
					logger.error("Could not connect", e);
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
    protected void initFilterChain(DefaultIoFilterChainBuilder filterChain) throws Exception {
        super.initFilterChain(filterChain);

        if(getSettings().isCompressionUsed()) {
            getCodecSettings().setChunkDelimiter(ByteBuffer.allocate(2).putShort((short)getSettings().getCompressedChunkDelimeter()).array());

            // ProtocolCodecFilter stores ProtocolDecoderOutput in session. So if we use more then one ProtocolCodecFilter
            // ProtocolDecoderOutput instance will be shared between all of them.
            // Hacked filter does not store ProtocolDecoderOutput instance in session
            CodecFactory codecFactory = new CodecFactory(serviceContext, messageFactory, null, ITCHDeflateCodec.class, getCodecSettings());
            HackedProtocolCodecFilter decompressorFilter = new HackedProtocolCodecFilter(codecFactory);

            filterChain.addBefore("codec", "decompressor", decompressorFilter);
        }
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
        super.internalStart();
        connect();
	}

    @Override
    protected MINASession createSession(IoSession session) {
        return new ITCHSession(getServiceName(), session, loggingConfigurator, getSettings().getMarketDataGroup()) {
            @Override
            protected Object prepareMessage(Object message) {
                if(message instanceof IMessage) {
                    IMessage iMessage = (IMessage)message;
                    StringBuilder msg = new StringBuilder("Sending of the message [" + iMessage.getName() + "] with fields such as ");
                    Set<String> fields = iMessage.getFieldNames();

                    for(String field : fields) {
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
    }

    @Override
    protected void preConnect() throws Exception {
        disconnecting = false;
        super.preConnect();
    }

    @Override
    protected void handleNotConnected(Throwable throwable) {
        if(reconecting) {
            taskExecutor.schedule(reconectCommand, reconnectiongTimeout, TimeUnit.MILLISECONDS);
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

	protected void disconnected0() {
		if ( !disconnecting  && !connectingFromMatrix && disposeWhenSessionClosed) {

			try {
                disposeResources();
			}
			catch (Throwable e ) {
				logger.error("Problem while disconnecting session", e);
			}

			if ( reconecting ) {
                this.taskExecutor.schedule(reconectCommand, reconnectiongTimeout, TimeUnit.MILLISECONDS);
                this.changeStatus(ServiceStatus.WARNING, "Connection was forcibly closed by the remote machine."
                        + "Service will be reconnect after " + reconnectiongTimeout + " milliseconds");
            } else {
                this.changeStatus(ServiceStatus.ERROR, "Connection was forcibly closed by the remote machine");
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

	protected void sendLiteLogin() throws InterruptedException {
        ITCHTCPClientSettings settings = this.getSettings();

        IMessage liteLogin = this.messageFactory.createMessage("LoginRequestLite", this.namespace);

		liteLogin.addField("Username", settings.getUsername());
		liteLogin.addField("Flag1", settings.getFlag1());

		sendMessage(liteLogin);
	}

	protected void sendLogin() throws InterruptedException {
        ITCHTCPClientSettings settings = this.getSettings();

        IMessage liteLogin = this.messageFactory.createMessage("LoginRequest", this.namespace);

		liteLogin.addField("Username", settings.getUsername());

		sendMessage(liteLogin);
	}

	protected void sendHeartBeat() throws InterruptedException {
        ITCHSession session = getSession();

        if(session == null) {
            throw new ServiceException("Could not send a heartbeat. Client is not connected");
        }

        logger.info("Client sent UnitHeader with namespace {}", this.namespace);
        IMessage unitHeader = this.messageFactory.createMessage("UnitHeader", this.namespace);

		unitHeader.addField("Length", 8);
		unitHeader.addField("MessageCount",(short) 0);
        unitHeader.addField("MarketDataGroup", (short)session.getMarketDataGroup());
		unitHeader.addField("SequenceNumber", (long)session.getSequenceNumber() + 1);

		sendMessage(unitHeader);
	}

	@Override
	protected void postConnect() throws Exception {
	    super.postConnect();
        ITCHTCPClientSettings settings = this.getSettings();

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
            this.taskExecutor.schedule(sentHeartbeatCommand, 0, TimeUnit.MILLISECONDS);
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
}
