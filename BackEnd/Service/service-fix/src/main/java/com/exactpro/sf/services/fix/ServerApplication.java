/*******************************************************************************
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
package com.exactpro.sf.services.fix;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.IDataManager;
import com.exactpro.sf.configuration.ILoggingConfigurator;
import com.exactpro.sf.configuration.suri.SailfishURIException;
import com.exactpro.sf.services.IServiceContext;
import com.exactpro.sf.services.IServiceHandler;
import com.exactpro.sf.services.IServiceSettings;
import com.exactpro.sf.services.ISession;
import com.exactpro.sf.services.MessageHelper;
import com.exactpro.sf.services.ServiceHandlerRoute;
import com.exactpro.sf.services.fix.converter.MessageConvertException;
import com.exactpro.sf.services.fix.listener.IFIXListener;
import com.exactpro.sf.services.fix.listener.StoreListener;
import com.exactpro.sf.storage.IMessageStorage;
import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;

import static com.exactpro.sf.services.util.ServiceUtil.createErrorMessage;
import static com.exactpro.sf.services.util.ServiceUtil.loadValuesFromAlias;

public class ServerApplication extends AbstractApplication implements FIXServerApplication {
    private final Logger logger = LoggerFactory.getLogger(ILoggingConfigurator.getLoggerName(this));
    private ServiceName serviceName;
    private IMessageStorage messageStorage;
    private ILoggingConfigurator logConfigurator;

    private boolean keepMessagesInMemory;
    private final Map<SessionID, ISession> sessionMap = new HashMap<>();
    private SessionSettings settings;
    private MessageHelper messageHelper;
    private IServiceHandler handler;
    private final ISession fixServerSessionsContainer = new FixServerSessionsContainer(this);
    private final List<IFIXListener> listeners = new ArrayList<>();

    @Override
    public void init(IServiceContext serviceContext, ApplicationContext applicationContext, ServiceName serviceName) {
        super.init(serviceContext, applicationContext, serviceName);
        listeners.clear();
        this.logConfigurator = serviceContext.getLoggingConfigurator();
        this.messageStorage = serviceContext.getMessageStorage();
        this.serviceName = serviceName;
        this.handler = Objects.requireNonNull(applicationContext.getServiceHandler(), "'Service handler' parameter");
        this.messageHelper = applicationContext.getMessageHelper();
        this.settings = applicationContext.getSessionSettings();
        this.messageStorage = serviceContext.getMessageStorage();

        IServiceSettings serviceSettings = applicationContext.getServiceSettings();

        if (serviceSettings instanceof FIXServerSettings) {
            this.keepMessagesInMemory = ((FIXServerSettings)serviceSettings).getKeepMessagesInMemory();
        }

        IFIXListener storeHandler = new StoreListener(messageStorage, keepMessagesInMemory, handler);
        listeners.add(storeHandler);

        if (serviceSettings instanceof FIXServerSettings) {
            FIXServerSettings fixServiceSettings = (FIXServerSettings) serviceSettings;
            try {
                IDataManager dataManager = serviceContext.getDataManager();
                Set<String> listenersToLoad = loadValuesFromAlias(dataManager, fixServiceSettings.getListenerNames(), ",");
                loadListeners(listenersToLoad, fixServiceSettings, serviceContext);
            } catch (SailfishURIException e) {
                throw new EPSCommonException(e.getMessage(), e);
            }
        } else {
            throw new EPSCommonException("Incompatible service settings class: " + serviceSettings.getClass().getCanonicalName());
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
                        seqNum -= 1; // decrementing the target sequence might cause the problem with 'seq to height' if the client increments seq as well
                        logger.info("Set target seqNum after logout to: {}", seqNum);
                        try {
                            session.setNextTargetMsgSeqNum(seqNum);
                        } catch (IOException e) {
                            logger.error("Cannot set next target sequence: " + e.getMessage(), e);
                        }

                        // prevent incrementing our own sequence
                        int oursSeq = message.getHeader().getInt(MsgSeqNum.FIELD);
                        try {
                            session.setNextSenderMsgSeqNum(oursSeq);
                        } catch (IOException e) {
                            logger.error("Cannot set next sender sequence" + e.getMessage(), e);
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
        IMessage iMsg = convertToIMessage(message, sessionId.getTargetCompID(), sessionId.getSenderCompID(), ServiceHandlerRoute.FROM_ADMIN, null);
        for (IFIXListener listener : listeners) {
            try {
                listener.fromAdmin(iSession, iMsg);
            } catch (Exception e) {
                logger.error("Cannot process 'fromAdmin' message {} by listener {}",
                        iMsg.getName(), listener.getClass(), e);
            }
        }
    }

    @Override
    public void toApp(Message message, SessionID sessionId) throws DoNotSend {

    }

    @Override
    public void fromApp(Message message, SessionID sessionId) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        ISession iSession = getSession(sessionId);

        IMessage iMsg = convertToIMessage(message, sessionId.getTargetCompID(), sessionId.getSenderCompID(), ServiceHandlerRoute.FROM_APP, null);
        for (IFIXListener listener : listeners) {
            try {
                listener.fromApp(iSession, iMsg);
            } catch (Exception e) {
                logger.error("Cannot process 'fromApp' message {} by listener {}", iMsg.getName(), listener.getClass(), e);
            }
        }
    }

    @Override
    public void onMessageRejected(Message message, SessionID sessionId, String reason) {
        ISession iSession = getSession(sessionId);

        IMessage iMsg = convertToIMessage(message, sessionId.getTargetCompID(), sessionId.getSenderCompID(), null, reason);
        for (IFIXListener listener : listeners) {
            try {
                listener.onMessageRejected(iSession, iMsg, reason);
            } catch (Exception e) {
                logger.error("Cannot process 'onMessageRejected' for message {} by listener {}", iMsg.getName(), listener.getClass(), e);
            }
        }
    }

    @Override
    public List<ISession> getSessions() {
        return new ArrayList<>(sessionMap.values());
    }

    @Override
    public void startLogging() {
        if (logConfigurator != null) {
            logConfigurator.registerLogger(this, serviceName);
        }
    }

    protected ISession getSession(SessionID sessionID) {
        if (!sessionMap.containsKey(sessionID)) {
            FIXSession session = createFIXSession("ServerApplication", sessionID, messageStorage,
                    converter, messageHelper);
            session.setServiceInfo(serviceInfo);
            sessionMap.put(sessionID, session);
        }

        return fixServerSessionsContainer;
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
        IMessage iMsg = convertToIMessage(message, sessionId.getTargetCompID(), sessionId.getSenderCompID(), ServiceHandlerRoute.TO_ADMIN, null);

        for (IFIXListener listener : listeners) {
            try {
                listener.toAdmin(iSession, iMsg);
            } catch (Exception e) {
                logger.error("Cannot process 'toAdmin' message {} by listener {}", iMsg.getName(), listener.getClass(), e);
            }
        }
    }

    @Override
    public void onSendToApp(Message message, SessionID sessionId) {
        ISession iSession = getSession(sessionId);
        IMessage iMsg = convertToIMessage(message, sessionId.getTargetCompID(), sessionId.getSenderCompID(), ServiceHandlerRoute.TO_APP, null);

        for (IFIXListener listener : listeners) {
            try {
                listener.toApp(iSession, iMsg);
            } catch (Exception e) {
                logger.error("Cannot process 'toApp' message {} by listener {}", iMsg.getName(), listener.getClass(), e);
            }
        }
    }

    @Override
    public ISession getServerSession() {
        return fixServerSessionsContainer;
    }

    protected IMessage convertToIMessage(Message message, String from, String to, ServiceHandlerRoute route, String reason) {
        try {
            return reason != null
                    ? convert(message, from, to, message.isAdmin(), isOutComingRoute(route), false, true)
                    : convert(message, from, to, message.isAdmin(), isOutComingRoute(route));
        } catch (MessageConvertException e) {
            return createErrorMessage(e.getMessage(), extractRawData(message, isOutComingRoute(route)), from, to, this.serviceInfo, messageHelper.getMessageFactory());
        }
    }

    private void loadListeners(Set<String> listenersToLoad, IServiceSettings settings, IServiceContext context) {
        ServiceLoader<IFIXListener> serviceLoader = ServiceLoader.load(IFIXListener.class, this.getClass().getClassLoader());
        for (IFIXListener listener : serviceLoader) {
            String listenerClassName = listener.getClass().getSimpleName();
            if(listenersToLoad.remove(listenerClassName)) {
                listener.init(context, settings, messageHelper);
                registerListenerLogger(listener);
                listeners.add(listener);
            }
        }

        if(listenersToLoad.size() > 0) {
            throw new EPSCommonException("Can't load listeners with the following names: " +
                    String.join(",", listenersToLoad));
        }
    }

    private void registerListenerLogger(IFIXListener listener) {
        if(logConfigurator != null) {
            logConfigurator.registerLogger(listener, serviceName);
        }
    }
}
