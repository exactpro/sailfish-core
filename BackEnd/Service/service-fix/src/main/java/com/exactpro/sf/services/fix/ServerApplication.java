/*******************************************************************************
 * Copyright 2009-2019 Exactpro (Exactpro Systems Limited)
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

import static com.exactpro.sf.services.ServiceHandlerRoute.FROM_ADMIN;
import static com.exactpro.sf.services.ServiceHandlerRoute.FROM_APP;
import static com.exactpro.sf.services.ServiceHandlerRoute.TO_ADMIN;
import static com.exactpro.sf.services.ServiceHandlerRoute.TO_APP;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.MsgMetaData;
import com.exactpro.sf.common.services.ServiceInfo;
import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.configuration.ILoggingConfigurator;
import com.exactpro.sf.services.IServiceContext;
import com.exactpro.sf.services.IServiceHandler;
import com.exactpro.sf.services.ISession;
import com.exactpro.sf.services.MessageHelper;
import com.exactpro.sf.services.ServiceHandlerRoute;
import com.exactpro.sf.services.fix.converter.MessageConvertException;
import com.exactpro.sf.services.fix.converter.dirty.DirtyQFJIMessageConverter;
import com.exactpro.sf.storage.IMessageStorage;

import quickfix.DoNotSend;
import quickfix.FieldNotFound;
import quickfix.IncorrectDataFormat;
import quickfix.IncorrectTagValue;
import quickfix.Message;
import quickfix.RejectLogon;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionSettings;
import quickfix.UnsupportedMessageType;
import quickfix.field.MsgSeqNum;
import quickfix.field.MsgType;
import quickfix.field.NewPassword;
import quickfix.field.Password;
import quickfix.field.ResetSeqNumFlag;
import quickfix.field.Text;
import quickfix.field.Username;

public class ServerApplication implements FIXServerApplication {

    private final Logger logger = LoggerFactory.getLogger(getClass().getName() + "@" + Integer.toHexString(hashCode()));
    private ServiceName serviceName;
    private IMessageStorage messageStorage;
    private ILoggingConfigurator logConfigurator;

    private boolean keepMessagesInMemory;
    private final Map<SessionID, ISession> sessionMap = new HashMap<>();
    private DirtyQFJIMessageConverter converter;
    private SessionSettings settings;
    private MessageHelper messageHelper;
    private IServiceHandler handler;
    private ServiceInfo serviceInfo;
    private final ISession fixServerSessionsContainer = new FixServerSessionsContainer(this);

    @Override
    public void init(IServiceContext serviceContext, ApplicationContext applicationContext, ServiceName serviceName) {
        this.logConfigurator = serviceContext.getLoggingConfigurator();
        this.messageStorage = serviceContext.getMessageStorage();
        this.serviceName = serviceName;
        this.handler = Objects.requireNonNull(applicationContext.getServiceHandler(), "'Service handler' parameter");
        this.messageHelper = applicationContext.getMessageHelper();
        this.converter = applicationContext.getConverter();
        this.settings = applicationContext.getSessionSettings();

        this.messageStorage = serviceContext.getMessageStorage();
        this.serviceInfo = serviceContext.lookupService(serviceName);

        if (applicationContext.getServiceSettings() instanceof FIXServerSettings) {
            this.keepMessagesInMemory = ((FIXServerSettings)applicationContext.getServiceSettings()).getKeepMessagesInMemory();
        }
    }

    @Override
    public void onCreate(SessionID sessionId) {
        logger.info("onCreate: {}", sessionId);
    }

    @Override
    public void onLogon(SessionID sessionId) {
        logger.info("onLogon: {}", sessionId);
    }

    @Override
    public void onLogout(SessionID sessionId) {
        logger.info("onLogout: {}", sessionId);
    }

    @Override
    public void toAdmin(Message message, SessionID sessionId) throws DoNotSend {
        ISession iSession = getSession(sessionId);

        try {

            Session session = Session.lookupSession(sessionId);
            String msgType = getMessageType(message);

            if (MsgType.LOGON.equals(msgType)) {
                String userName = (String)settings.getSessionProperties(sessionId).get("Username");
                String password = (String)settings.getSessionProperties(sessionId).get("Password");
                String newPassword = (String)settings.getSessionProperties(sessionId).get("NewPassword");
                if (userName != null) {
                    logger.debug("Username = {}", userName);
                    message.setString(Username.FIELD, userName);
                }
                if (password != null) {
                    logger.debug("Password = {}", password);
                    message.setString(Password.FIELD, password);
                }
                if (newPassword != null) {
                    logger.debug("NewPassword = {}", newPassword);
                    message.setString(NewPassword.FIELD, newPassword);
                }

                // set reset SeqNum if required
                String resetSeqNumFlag = settings.getSessionProperties(sessionId).getProperty(FIXClient.ResetSeqNumFlag);
                if (resetSeqNumFlag != null) {
                    message.setBoolean(ResetSeqNumFlag.FIELD, BooleanUtils.toBoolean(resetSeqNumFlag));
                }

            }

            if (MsgType.LOGOUT.equals(msgType)) {
                if (message.isSetField(Text.FIELD)) {
                    String text = message.getString(Text.FIELD);
                    if (text.startsWith("MsgSeqNum too low, expecting ")) {
                        // extract 4 from the text: MsgSeqNum too low, expecting
                        // 4 but received 1
                        logger.debug("Logout received - {}", message);
                        int seqNum = Integer.parseInt(text.split(" ")[4]);
                        // experimentally checked
                        // only here set next seq num as seqMum-1.
                        seqNum = seqNum - 1;
                        logger.debug("set seqNum after logout to: {}", seqNum);
                        try {
                            session.setNextTargetMsgSeqNum(seqNum);
                        } catch (IOException e) {
                            logger.error(e.getMessage(), e);
                        }

                        int targSeq = message.getHeader().getInt(MsgSeqNum.FIELD);
                        try {
                            session.setNextTargetMsgSeqNum(targSeq);
                        } catch (IOException e) {
                            logger.error(e.getMessage(), e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            handler.exceptionCaught(iSession, e);
        }
    }

    @Override
    public void fromAdmin(Message message, SessionID sessionId) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
        ISession iSession = getSession(sessionId);

        storeMessage(iSession, message, sessionId.getTargetCompID(), sessionId.getSenderCompID(), FROM_ADMIN);
    }

    @Override
    public void toApp(Message message, SessionID sessionId) throws DoNotSend {

    }

    @Override
    public void fromApp(Message message, SessionID sessionId) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        ISession iSession = getSession(sessionId);

        storeMessage(iSession, message, sessionId.getTargetCompID(), sessionId.getSenderCompID(), FROM_APP);
    }

    @Override
    public void onMessageRejected(Message message, SessionID sessionId, String reason) {
        ISession iSession = getSession(sessionId);

        storeMessage(iSession, message, sessionId.getTargetCompID(), sessionId.getSenderCompID(), reason);
    }

    @Override
    public List<ISession> getSessions() {
        return new ArrayList<>(sessionMap.values());
    }

    @Override
    public void startLogging() {
        logConfigurator.createIndividualAppender(getClass().getName() + "@" + Integer.toHexString(hashCode()), serviceName);
    }

    @Override
    public void stopLogging() {
        logConfigurator.destroyIndividualAppender(getClass().getName() + "@" + Integer.toHexString(hashCode()), serviceName);
    }

    protected ISession getSession(SessionID sessionID) {
        if (!sessionMap.containsKey(sessionID)) {
            FIXSession session = new FIXSession("ServerApplication", sessionID, messageStorage, converter, messageHelper);
            session.setServiceInfo(serviceInfo);
            sessionMap.put(sessionID, session);
        }

        return fixServerSessionsContainer;
    }

    protected void storeMessage(ISession iSession, Message message, String from, String to, ServiceHandlerRoute route, String reason) {
        if (keepMessagesInMemory) {
            try {
                IMessage iMsg = convert(message, from, to, message.isAdmin());
                iMsg.getMetaData().setRejectReason(reason);
                try {
                    messageStorage.storeMessage(iMsg);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }

                if (route != null) {
                    handler.putMessage(iSession, route, iMsg);
                }
            } catch (Exception e) {
                handler.exceptionCaught(iSession, e);
            }
        }
    }

    protected void storeMessage(ISession iSession, Message message, String from, String to, ServiceHandlerRoute route) {
        storeMessage(iSession, message, from, to, route, null);
    }

    protected void storeMessage(ISession iSession, Message message, String from, String to, String reason) {
        storeMessage(iSession, message, from, to, null, reason);
    }

    private IMessage convert(Message message, String from, String to, boolean isAdmin) throws MessageConvertException {
        IMessage msg = converter.convert(message);
        MsgMetaData meta = msg.getMetaData();
        meta.setFromService(from);
        meta.setToService(to);
        meta.setAdmin(isAdmin);
        meta.setServiceInfo(serviceInfo);
        return msg;
    }

    private String getMessageType(Message message) {
        try {
            return message.getHeader().getString(MsgType.FIELD);
        } catch (FieldNotFound e) {
            logger.error(e.getMessage());
        }
        return null;
    }

    @Override
    public void onConnectionProblem(String reason) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onSendToAdmin(Message message, SessionID sessionId) {
        ISession iSession = getSession(sessionId);

        storeMessage(iSession, message, sessionId.getSenderCompID(), sessionId.getTargetCompID(), TO_ADMIN);
    }

    @Override
    public void onSendToApp(Message message, SessionID sessionId) {
        ISession iSession = getSession(sessionId);

        storeMessage(iSession, message, sessionId.getSenderCompID(), sessionId.getTargetCompID(), TO_APP);
    }

    @Override
    public ISession getServerSession() {
        return fixServerSessionsContainer;
    }
}
