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
package com.exactpro.sf.services.itch.multicast;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.services.ISession;
import com.exactpro.sf.services.ITaskExecutor;
import com.exactpro.sf.services.MessageHelper;
import com.exactpro.sf.services.itch.ITCHMessageHelper;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by alexey.zarovny on 11/29/14.
 */
public class ITCHMulticastTCPHandlerAdapter extends IoHandlerAdapter {
    public static final String LOGIN_REQUEST = "LoginRequest";
    public static final String REPLAY_REQUEST = "ReplayRequest";
    public static final String LOGOUT_REQUEST = "LogoutRequest";
    public static final String REPLAY_RESPONSE = "ReplayResponse";
    public static final String MARKET_DATA_GROUP = "MarketDataGroup";
    public static final String FIRST_MESSAGE = "FirstMessage";
    public static final String COUNT = "Count";
    public static final String STATUS = "Status";
    public static final String LOGIN_RESPONSE = "LoginResponse";
    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName() + "@" + Integer.toHexString(hashCode()));

    private final ITCHMulticastCache cache;
    private final ITCHMulticastServer service;
    private final ISession iSession;
    private final int sessionIdleTimeout;
    private final int loginTimeout;
    private final IMessageFactory msgFactory;
    private ITaskExecutor taskExecutor;
    private final byte mdGroup;
    private MessageHelper itchHandler;

    public ITCHMulticastTCPHandlerAdapter(ITCHMulticastCache cache, SailfishURI dictionaryURI, ITaskExecutor taskExecutor, ITCHMulticastServer service, ISession iSession, byte mdGroup, MessageHelper itchHandler,
                                          IMessageFactory msgFactory) {
        this.cache = cache;
        this.msgFactory = msgFactory;
        this.taskExecutor = taskExecutor;
        this.service = service;
        this.iSession = iSession;
        this.mdGroup = mdGroup;
        this.itchHandler = itchHandler;
        ITCHMulticastSettings multicastSetting = (ITCHMulticastSettings) service.getSettings();
        this.sessionIdleTimeout = multicastSetting.getSessionIdleTimeout();
        this.loginTimeout = multicastSetting.getLoginTimeout();

    }

    public void send(Object message) {
        //do not working
        throw new EPSCommonException("This method should not be used for this realization");
    }

    @Override
    public void sessionCreated(IoSession session) {
        logger.debug("Session created: {}", session);
        session.setAttribute("lastActivityTime", System.currentTimeMillis());
        session.getConfig().setIdleTime(IdleStatus.READER_IDLE, loginTimeout);
    }

    @Override
    public void sessionClosed(IoSession session) {
    }

    @Override
    public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
        session.close(true);
    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) {
        logger.error(cause.getMessage(), cause);
    }

    @Override
    public void messageReceived(final IoSession session, Object message) {
        if (message instanceof IMessage) {
            final IMessage iMsg = (IMessage) message;

            if (ITCHMessageHelper.MESSAGELIST_NAME.equals(iMsg.getName())) {
                taskExecutor.addTask(new Runnable() {
                    @Override
                    public void run() {
                        List<IMessage> list = iMsg.<List<IMessage>>getField(ITCHMessageHelper.SUBMESSAGES_FIELD_NAME);
                        for (IMessage msg : list) {
                            onMessage(msg, session);
                        }
                    }

                    @Override
                    public String toString() {
                        return ITCHMulticastTCPHandlerAdapter.class.getSimpleName();
                    }
                });

            }
        } else {
            throw new EPSCommonException("Something wrong: incorrect message type: " + message.getClass());
        }
    }

    /*
    * internal message handling
    * */
    private IMessage onMessage(IMessage iMessage, IoSession session) {
        if (iMessage.getName().equals(LOGIN_REQUEST)) {
            return onLogon(iMessage, session);
        } else if (iMessage.getName().equals(REPLAY_REQUEST)) {
            return onReplay(iMessage, session);
        } else if (iMessage.getName().equals(LOGOUT_REQUEST)) {

            return onLogout(iMessage, session);
        }
        return null;
    }

    private IMessage onLogout(IMessage iMessage, IoSession session) {
        handleMessage(iMessage, true, true, session.getRemoteAddress().toString());
        session.close(true);
        return null;
    }

    private IMessage onReplay(IMessage iMessage, IoSession session) {
        Object firstMessage = iMessage.getField(FIRST_MESSAGE);
        Object count = iMessage.getField(COUNT);
        final Object marketDataGroup = iMessage.getField(MARKET_DATA_GROUP);
        List<IMessage> messageList = new ArrayList<>();
        IMessage result = msgFactory.createMessage(REPLAY_RESPONSE, iMessage.getNamespace());
        result.addField(MARKET_DATA_GROUP, marketDataGroup);
        ITCHMulticastCache.Status status = ITCHMulticastCache.Status.REQUEST_ACCEPTED;
        if (firstMessage != null && count != null) {
            logger.debug("ReplayRequest works");
             status = cache.getMessages(Integer.valueOf(firstMessage.toString()), (Integer) count, new Byte(marketDataGroup.toString()), messageList);
            switch (status) {
                case REQUEST_ACCEPTED:
                    result.addField(FIRST_MESSAGE, firstMessage);
                    result.addField(COUNT, count);
                    result.addField(STATUS, toShort('A'));
                    break;
                case OUT_OF_RANGE:
                    result.addField(FIRST_MESSAGE, (long)0);
                    result.addField(COUNT, 0);
                    result.addField(STATUS, toShort('O'));
                    break;
                case INVALID_MARKET_DATA_GROUP:
                    result.addField(FIRST_MESSAGE, firstMessage);
                    result.addField(COUNT, count);
                    result.addField(STATUS, toShort('I'));
                    break;
                case REPLAY_UNAVAILABLE:
                    result.addField(FIRST_MESSAGE, (long)0);
                    result.addField(COUNT, 0);
                    result.addField(STATUS, toShort('U'));
            }
        }
        @SuppressWarnings("serial")
        Map<String, String> params = new HashMap<String, String>(){{
            put(ITCHMessageHelper.FIELD_MARKET_DATA_GROUP_NAME, String.valueOf(marketDataGroup));
            put(ITCHMessageHelper.FIELD_SEQUENCE_NUMBER_NAME, String.valueOf(0));
        }};
        result = itchHandler.prepareMessageToEncode(result, params);
        session.write(result);
        handleMessage(result, false, false, session.getRemoteAddress().toString());
        if (status == ITCHMulticastCache.Status.REQUEST_ACCEPTED) {
            for(IMessage msg:messageList){
                session.write(msg);
                handleMessage(msg, false, false, session.getRemoteAddress().toString());
            }
        }
        return result;
    }

    private void handleMessage(IMessage iMessage, boolean isFrom, boolean isAdmin, String remote) {
        service.handleMessage(isFrom, isAdmin, iMessage, iSession, remote);
    }

    private IMessage onLogon(IMessage iMessage, IoSession session) {
        handleMessage(iMessage, true, true, session.getRemoteAddress().toString());

        IMessage loginResponse;
        loginResponse = msgFactory.createMessage(LOGIN_RESPONSE, iMessage.getNamespace());
        if (iMessage.getField("Username") != null && iMessage.getField("Password") != null) {
            loginResponse.addField(STATUS, toShort('A'));
        } else {
            loginResponse.addField(STATUS, toShort('e'));
        }
        @SuppressWarnings("serial")
        Map<String, String> params = new HashMap<String, String>(){{
            put(ITCHMessageHelper.FIELD_MARKET_DATA_GROUP_NAME, String.valueOf(mdGroup));
            put(ITCHMessageHelper.FIELD_SEQUENCE_NUMBER_NAME, String.valueOf(0));
        }};
        loginResponse = itchHandler.prepareMessageToEncode(loginResponse, params);
        session.write(loginResponse);
        handleMessage(loginResponse, false, true, session.getRemoteAddress().toString());

        session.getConfig().setIdleTime(IdleStatus.READER_IDLE, sessionIdleTimeout);

        return loginResponse;
    }

    private short toShort(char character) {
        return (short) character;
    }
}
