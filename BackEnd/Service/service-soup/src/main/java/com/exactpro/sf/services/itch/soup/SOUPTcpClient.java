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
package com.exactpro.sf.services.itch.soup;

import static com.exactpro.sf.services.itch.soup.SOUPMessageHelper.PACKET_HEADER_MESSAGE;
import static com.exactpro.sf.services.itch.soup.SOUPMessageHelper.SEQUENCED_DATA_PACKET;
import static com.exactpro.sf.services.itch.soup.SOUPMessageHelper.SEQUENCED_HEADER_MESSAGE;
import static com.exactpro.sf.services.itch.soup.SOUPMessageHelper.SOUP_BIN_TCP_HEADER_NAME;
import static com.exactpro.sf.services.itch.soup.SOUPMessageHelper.UNSEQUENCED_DATA_PACKET;
import static com.exactpro.sf.services.itch.soup.SOUPTcpMessageHelper.FIELD_REQUESTED_SEQUENCE_NUMBER;
import static com.exactpro.sf.services.itch.soup.SOUPTcpMessageHelper.FIELD_REQUESTED_SESSION;
import static com.exactpro.sf.services.itch.soup.SOUPTcpMessageHelper.MESSAGE_CLIENT_HEARTBEAT_NAME;
import static com.exactpro.sf.services.itch.soup.SOUPTcpMessageHelper.MESSAGE_LOGIN_REQUEST_PACKET;
import static com.exactpro.sf.services.itch.soup.SOUPTcpMessageHelper.MESSAGE_LOGOUT_REQUEST_PACKET;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.mina.core.session.IoSession;

import com.exactpro.sf.common.codecs.AbstractCodec;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.MsgMetaData;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.common.util.EvolutionBatch;
import com.exactpro.sf.services.MessageHelper;
import com.exactpro.sf.services.ServiceHandlerRoute;
import com.exactpro.sf.services.itch.ITCHCodecSettings;
import com.exactpro.sf.services.itch.ITCHSession;
import com.exactpro.sf.services.itch.ITCHTcpClient;
import com.exactpro.sf.services.mina.MINASession;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

public class SOUPTcpClient extends ITCHTcpClient {
    private static final List<String> IGNORE_MESSAGES = Lists.newArrayList(SOUP_BIN_TCP_HEADER_NAME, UNSEQUENCED_DATA_PACKET, SEQUENCED_DATA_PACKET, PACKET_HEADER_MESSAGE);
    private SoupTcpCodecSettings codecSettings;

    @Override
    protected void internalInit() throws Exception {
        super.internalInit();
        ITCHCodecSettings originalCodecSettings = super.getCodecSettings();
        this.codecSettings = new SoupTcpCodecSettings(getSettings().isEvolutionSupportEnabled());
        codecSettings.setDictionaryURI(originalCodecSettings.getDictionaryURI());
        codecSettings.setMsgLength(originalCodecSettings.getMsgLength());
        codecSettings.setFilterValues(originalCodecSettings.getFilterValues());
        codecSettings.setChunkDelimiter(originalCodecSettings.getChunkDelimiter());
        codecSettings.setPreprocessingEnabled(originalCodecSettings.isPreprocessingEnabled());
        codecSettings.setEvolutionSupportEnabled(originalCodecSettings.isEvolutionSupportEnabled());
    }

    protected String getHeartBeatMessageName() {
        return MESSAGE_CLIENT_HEARTBEAT_NAME;
    }

    @Override
    protected Class<? extends AbstractCodec> getCodecClass() throws Exception {
        return SOUPTcpCodec.class;
	}

    @Override
    public SoupTcpCodecSettings getCodecSettings() {
        return codecSettings;
    }

    @Override
    protected MessageHelper createMessageHelper(IMessageFactory messageFactory, IDictionaryStructure dictionary) {
        MessageHelper messageHelper = new SOUPTcpMessageHelper();
        messageHelper.init(messageFactory, dictionary);
        return messageHelper;
    }

	@Override
	protected void sendHeartBeat() throws InterruptedException {
		taskExecutor.addTask(new Runnable() {
			// Avoid "DEAD LOCK: IoFuture.await()"
			@Override
			public void run() {
                IMessage hb = messageFactory.createMessage(getHeartBeatMessageName(), namespace);
				hb = messageHelper.prepareMessageToEncode(hb, null);
				try {
					sendMessage(hb);
				} catch (InterruptedException e) {
					logger.error(e.getMessage(), e);
				}
			}
		});
	}

	@Override
	protected void sendLiteLogin() throws InterruptedException {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void sendLogin() throws InterruptedException {
		sendMessage(createLogin());
	}

	@Override
	public void messageSent(IoSession session, Object message) throws Exception {
		// override MITCHTcpClient.messageSent: we don't use "IncludedMessages"
		logger.debug("Message sent");

		if (!(message instanceof IMessage)) {
			throw new EPSCommonException("Received message is not" + IMessage.class);
		}

		IMessage msg = (IMessage) message;
		MsgMetaData metaData = msg.getMetaData();
        metaData.setToService(getEndpointName());
		metaData.setFromService(getName());
		metaData.setServiceInfo(serviceInfo);

        storage.storeMessage(msg);

        if(logger.isDebugEnabled()) {
            logger.debug("Message sent: {}", getHumanReadable(msg));
        }

        getServiceHandler().putMessage(getSession(), ServiceHandlerRoute.get(false, metaData.isAdmin()), msg);
	}

	@Override
	public void messageReceived(IoSession session, Object message) throws Exception {
		// override MITCHAbstractClient.messageReceived: we don't use "IncludedMessages"
		logger.debug("Message {} was received", message);
        // TODO: probably we should delegate call to onMessageReceived method instead of recreating logic here

		if (!(message instanceof IMessage)) {
			throw new EPSCommonException("Received message is not instance of " + IMessage.class);
		}

		IMessage msg = (IMessage) message;
		MsgMetaData metaData = msg.getMetaData();
		metaData.setToService(getName());
		metaData.setServiceInfo(serviceInfo);

        if (EvolutionBatch.MESSAGE_NAME.equals(msg.getName()) && !getSettings().isEvolutionSupportEnabled()) {
            logger.info("Skip processing {} message", EvolutionBatch.MESSAGE_NAME);
            return;
        }

        storage.storeMessage(msg);

        if(logger.isDebugEnabled()) {
            logger.debug("Message received: {} ", getHumanReadable(msg));
        }

        getServiceHandler().putMessage(getSession(), ServiceHandlerRoute.get(true, metaData.isAdmin()), msg);
	}

    @Override
    public SOUPTcpClientSettings getSettings() {
        return (SOUPTcpClientSettings) super.getSettings();
    }

    @Override
    protected void preDisconnect() throws Exception {
        if (getSettings().isSendLogoutOnDisconnect()) {
            IMessage logout = messageFactory.createMessage(MESSAGE_LOGOUT_REQUEST_PACKET, namespace);
            logout = messageHelper.prepareMessageToEncode(logout, null);
            sendMessage(logout);
        }
        super.preDisconnect();
    }

    @Override
    protected SOUPTcpMessageHelper getMessageHelper() {
        return (SOUPTcpMessageHelper) super.getMessageHelper();
    }

    protected IMessage createLogin(){
        SOUPTcpClientSettings settings = getSettings();

        return getMessageHelper().createMessage(MESSAGE_LOGIN_REQUEST_PACKET, ImmutableMap.<String, Object> builder()
                .put("Member", settings.getMember())
                .put("UserName", settings.getUsername())
                .put("Password", settings.getPassword())
                .put("Ticket", 0L)
                .put("Version", settings.getVersion())
                .put(FIELD_REQUESTED_SESSION, settings.getRequestedSession())
                .put(FIELD_REQUESTED_SEQUENCE_NUMBER, settings.getRequestedSequenceNumber())
                .build()
        );
    }

    @Override
    protected MINASession createSession(IoSession session) {
        SOUPTcpClientSettings settings = getSettings();
        return new InternalSession(getServiceName(), session, settings.getMarketDataGroup(), settings.getSendMessageTimeout(), getMessageHelper());
    }

    static class InternalSession extends ITCHSession {
        private final MessageHelper messageHelper;

        public InternalSession(ServiceName serviceName, IoSession session, byte marketDataGroup, long sendMessageTimeout, MessageHelper messageHelper) {
            super(serviceName, session, marketDataGroup, sendMessageTimeout);
            this.messageHelper = Objects.requireNonNull(messageHelper, "'Message helper' parameter");
        }

        @Override
        protected Object prepareMessage(Object message) {
            if (message instanceof IMessage) {
                return messageHelper.prepareMessageToEncode((IMessage)message, Collections.emptyMap());
            }
            return super.prepareMessage(message);
        }

        @Override
        protected boolean filterResultFromSendRaw(IMessage result) {
            String messageName = result.getName();
            return IGNORE_MESSAGES.contains(messageName);
        }

        @Override
        protected void removeSessionFields(IMessage result) {
            result.removeField(SEQUENCED_HEADER_MESSAGE);
        }
    }
}
