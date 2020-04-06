/*******************************************************************************
 * Copyright 2009-2020 Exactpro (Exactpro Systems Limited)
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

package com.exactpro.sf.services.mina;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;

import com.exactpro.sf.common.codecs.AbstractCodec;
import com.exactpro.sf.common.codecs.CodecFactory;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.util.ICommonSettings;
import com.exactpro.sf.services.AbstractInitiatorService;
import com.exactpro.sf.services.IAcceptorService;
import com.exactpro.sf.services.ISession;
import com.exactpro.sf.services.ServiceException;
import com.exactpro.sf.services.ServiceHandlerException;
import com.exactpro.sf.services.WrapperNioSocketAcceptor;

public abstract class AbstractMINATCPServer extends AbstractInitiatorService implements IoHandler, IAcceptorService {

    protected final AtomicReference<MINAServerSession> serverSession;
    protected final AtomicReference<WrapperNioSocketAcceptor> acceptor;
    protected final Map<IoSession, MINASession> sessions;

    public AbstractMINATCPServer() {
        sessions = new ConcurrentHashMap<>();
        serverSession = new AtomicReference<>();
        acceptor = new AtomicReference<>();
    }

    @Override
    public List<ISession> getSessions() {
        return new ArrayList<>(sessions.values());
    }

    @Override
    protected void internalStart() throws IOException {
        WrapperNioSocketAcceptor tmpAcceptor = new WrapperNioSocketAcceptor(taskExecutor);

        tmpAcceptor.setReuseAddress(true);

        try {
            configureAcceptor(tmpAcceptor);
        } catch (Exception e) {
            throw new ServiceException("Can`t configure server`s acceptor", e);
        }

        CodecFactory codecFactory = new CodecFactory(serviceContext, messageFactory, dictionary, getCodecClass(), getCodecSettings());
        tmpAcceptor.setHandler(this);
        tmpAcceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(codecFactory));
        tmpAcceptor.bind(new InetSocketAddress(getSettings().getHost(), getSettings().getPort()));

        acceptor.set(tmpAcceptor);
        serverSession.set(createServerSession());
    }

    protected ICommonSettings getCodecSettings() {
        return getSettings();
    }

    @Override
    public AbstractMINATCPServerSettings getSettings() {
        return (AbstractMINATCPServerSettings) settings;
    }

    @Override
    public MINAServerSession getSession() {
        return serverSession.get();
    }

    @Override
    public void connect(){
        //TODO: Must throws exception and use 'accept' or other method for server.
        //throw new UnsupportedOperationException("Server can't connect to anyone but it can accept incoming connection");
    }

    @Override
    protected void disposeResources() {

        MINAServerSession session = serverSession.getAndSet(null);

        try {
            if (session != null && session.isConnected()) {
                session.close();
            }
        } catch (RuntimeException e) {
            logger.warn("Server session didn`t close", e);
        }

        WrapperNioSocketAcceptor localAcceptor = acceptor.getAndSet(null);
        try {
            if (localAcceptor != null) {
                localAcceptor.unbind();
                localAcceptor.dispose();
            }
        } catch (RuntimeException e) {
            logger.warn("Acceptor didn`t unbind or/and dispose", e);
        }

        super.disposeResources();
    }

    @Override
    protected String getEndpointName() {
        return String.format("Server for %s", serviceName);
    }

    @Override
    public void sessionCreated(IoSession session) {
        logger.debug("Session created. Session: " + session);
    }

    @Override
    public void sessionOpened(IoSession session) throws ServiceHandlerException {
        MINASession minaSession = createMINASession(session);
        onSessionOpen(minaSession);
        sessions.put(session, minaSession);
        handler.sessionOpened(serverSession.get());

        logger.debug("sessionOpened - {} session is opened", session);
    }

    @Override
    public void sessionClosed(IoSession session) throws Exception {
        onSessionClosed(sessions.remove(session));
        handler.sessionClosed(serverSession.get());
    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) {
        logger.error("Have error: {}", session, cause);
    }

    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        if (message instanceof  IMessage) {
            IMessage iMessage = (IMessage) message;
            onMessageReceived(iMessage);
            processMessage(serverSession.get(), (IMessage) message);
        }
    }

    @Override
    public void messageSent(IoSession session, Object message) throws Exception {
        if (message instanceof IMessage && session != null) {
            IMessage iMessage = (IMessage)message;
            onMessageSent(iMessage);
        }
    }

    /**
     * Method body is copied from {@link IoHandlerAdapter#inputClosed(IoSession)}
     */
    @Override
    public void inputClosed(IoSession session) {
        logger.debug("Session input closed: {}", session);
        session.close(true);
    }

    protected MINAServerSession createServerSession() {
        MINAServerSession serverSession = new MINAServerSession(this);
        loggingConfigurator.registerLogger(serverSession, getServiceName());
        logger.info("Server session created: {}", serverSession);
        return serverSession;
    }

    protected MINASession createMINASession(IoSession session) {
        MINASession minaSession = new MINASession(serviceName, session);
        loggingConfigurator.registerLogger(minaSession, getServiceName());
        logger.info("Server session created: {}", minaSession);
        return minaSession;
    }

    protected abstract Class<? extends AbstractCodec> getCodecClass();
    protected abstract void configureAcceptor(WrapperNioSocketAcceptor acceptor) throws Exception;
    protected abstract void onSessionOpen(MINASession session);
    protected abstract void processMessage(MINAServerSession session, IMessage message) throws Exception;
    protected abstract void onSessionClosed(MINASession session) throws Exception;
}
