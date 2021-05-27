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

import static com.exactpro.sf.services.itch.soup.SOUPTcpMessageHelper.FIELD_REJECT_REASON_CODE;
import static com.exactpro.sf.services.itch.soup.SOUPTcpMessageHelper.FIELD_REQUESTED_SESSION;
import static com.exactpro.sf.services.itch.soup.SOUPTcpMessageHelper.FIELD_SEQUENCE_NUMBER;
import static com.exactpro.sf.services.itch.soup.SOUPTcpMessageHelper.FIELD_SESSION;
import static com.exactpro.sf.services.itch.soup.SOUPTcpMessageHelper.FIELD_USERNAME;
import static com.exactpro.sf.services.itch.soup.SOUPTcpMessageHelper.MESSAGE_ACCEPTED_LOGIN_PACKET;
import static com.exactpro.sf.services.itch.soup.SOUPTcpMessageHelper.MESSAGE_END_OF_SESSION_PACKET;
import static com.exactpro.sf.services.itch.soup.SOUPTcpMessageHelper.MESSAGE_LOGIN_REJECT_PACKET;
import static com.exactpro.sf.services.itch.soup.SOUPTcpMessageHelper.MESSAGE_LOGIN_REQUEST_PACKET;
import static com.exactpro.sf.services.itch.soup.SOUPTcpMessageHelper.MESSAGE_LOGOUT_REQUEST_PACKET;
import static com.exactpro.sf.services.itch.soup.SOUPTcpMessageHelper.MESSAGE_SERVER_HEARTBEAT_NAME;

import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;

import com.exactpro.sf.common.codecs.AbstractCodec;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.services.MessageHelper;
import com.exactpro.sf.services.WrapperNioSocketAcceptor;
import com.exactpro.sf.services.mina.AbstractMINATCPServer;
import com.exactpro.sf.services.mina.MINAServerSession;
import com.exactpro.sf.services.mina.MINASession;

public class SOUPTcpServer extends AbstractMINATCPServer {

    public static final String WRONG_USERNAME = "WUSER";

    @Override
    protected Class<? extends AbstractCodec> getCodecClass() {
        return SOUPTcpCodec.class;
    }

    @Override
    public SOUPTcpServerSettings getSettings() {
        return (SOUPTcpServerSettings) settings;
    }

    @Override
    protected void configureAcceptor(WrapperNioSocketAcceptor acceptor) {
        acceptor.getSessionConfig().setIdleTime(IdleStatus.WRITER_IDLE, Math.max(1, getSettings().getSendHeartBeatTimeout()));
        acceptor.getSessionConfig().setIdleTime(IdleStatus.READER_IDLE, Math.max(1, getSettings().getReadHeartBeatTimeout()));
    }

    @Override
    protected MINAServerSession createServerSession() {
        return new SOUPTcpServerSession(this);
    }

    @Override
    protected MINASession createMINASession(IoSession session) {
        SOUPTcpSession soupTcpSession = new SOUPTcpSession(serviceName, session, getSettings().getSendMessageTimeout());
        loggingConfigurator.registerLogger(soupTcpSession, serviceName);
        return soupTcpSession;
    }

    @Override
    protected MessageHelper createMessageHelper(IMessageFactory iMessageFactory, IDictionaryStructure iDictionaryStructure) {
        MessageHelper messageHelper = new SOUPTcpMessageHelper();
        messageHelper.init(iMessageFactory, iDictionaryStructure);
        return messageHelper;
    }

    @Override
    protected void onSessionOpen(MINASession session) {
        SOUPTcpSession result = (SOUPTcpSession) session;
        if (getSettings().getWaitLoginTimeout() > 0) {
            taskExecutor.schedule(() -> {
                if (StringUtils.isEmpty(result.getSessionId())) {
                    try {
                        closeSession(serverSession.get());
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }, getSettings().getWaitLoginTimeout(), TimeUnit.MILLISECONDS);
        }
    }

    @Override
    protected void processMessage(MINAServerSession session, IMessage message) {
        switch (message.getName()) {
        case MESSAGE_LOGIN_REQUEST_PACKET:

            logger.debug("Receive message: " + MESSAGE_LOGIN_REQUEST_PACKET);

            if (message.isFieldSet(FIELD_USERNAME)) {
                if (message.getField(FIELD_USERNAME).equals(WRONG_USERNAME)) {
                    send(session, getMessageHelper().createMessage(MESSAGE_LOGIN_REJECT_PACKET, MapUtils.putAll(new HashMap<>(), new Object[]{FIELD_REJECT_REASON_CODE, "A"})));
                    break;
                }
            }

            String sessionId;
            if (message.isFieldSet(FIELD_REQUESTED_SESSION)) {
                sessionId = message.getField(FIELD_REQUESTED_SESSION);
            } else {
                sessionId = RandomStringUtils.randomAlphanumeric(10);
            }

            sessions.values().forEach(minasession -> ((SOUPTcpSession) minasession).setSessionId(sessionId));

            send(session, getMessageHelper().createMessage(MESSAGE_ACCEPTED_LOGIN_PACKET, MapUtils.putAll(new HashMap<>(), new Object[]{FIELD_SESSION, sessionId, FIELD_SEQUENCE_NUMBER, 0})));
            break;
        case MESSAGE_LOGOUT_REQUEST_PACKET:

            logger.debug("Receive message: " + MESSAGE_LOGOUT_REQUEST_PACKET);
            closeSession(session);
            break;
        }
    }

    @Override
    public void sessionIdle(IoSession session, IdleStatus status) {
        if (status == IdleStatus.WRITER_IDLE && getSettings().isSendHeartBeats()) {
            IMessage hb = messageFactory.createMessage(MESSAGE_SERVER_HEARTBEAT_NAME, dictionary.getNamespace());
            hb = messageHelper.prepareMessageToEncode(hb, null);
            send(serverSession.get(), hb);
        } else if (status == IdleStatus.READER_IDLE) {
            closeSession(serverSession.get());
        }
    }

    private void closeSession(MINAServerSession session) {
        if (session == null || session.isClosed()) {
            return;
        }

        IMessage closeMessage = getMessageHelper().createMessage(MESSAGE_END_OF_SESSION_PACKET, Collections.emptyMap());
        taskExecutor.schedule(() -> {
            asyncSend(serverSession.get(), closeMessage);
            session.close();
        }, 10, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void internalInit() {}

    @Override
    protected void onSessionClosed(MINASession minaSession) throws Exception {}

    private void asyncSend(MINAServerSession minaSession, IMessage message) {
        try {
            if (minaSession != null && minaSession.isConnected()) {
                minaSession.send(messageHelper.prepareMessageToEncode(message, null));
            }
        } catch (InterruptedException e) {
            logger.warn("Interrupt sending message", e);
        }
    }

    private void send(MINAServerSession minaServerSession, IMessage message) {
        taskExecutor.addTask(() -> {
            asyncSend(minaServerSession, message);
        });
    }

    @Override
    protected SOUPTcpMessageHelper getMessageHelper() {
        return (SOUPTcpMessageHelper) super.getMessageHelper();
    }
}
