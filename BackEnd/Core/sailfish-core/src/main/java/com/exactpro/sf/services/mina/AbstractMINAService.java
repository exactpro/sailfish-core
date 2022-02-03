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
package com.exactpro.sf.services.mina;

import java.io.IOException;
import java.util.concurrent.Future;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.jetbrains.annotations.NotNull;

import com.exactpro.sf.common.codecs.AbstractCodec;
import com.exactpro.sf.common.codecs.CodecFactory;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.common.util.ICommonSettings;
import com.exactpro.sf.services.AbstractInitiatorService;
import com.exactpro.sf.services.IdleStatus;
import com.exactpro.sf.services.ServiceException;
import com.exactpro.sf.services.ServiceStatus;
import com.exactpro.sf.services.util.ServiceUtil;

public abstract class AbstractMINAService extends AbstractInitiatorService implements IoHandler {
    static final String CODEC_FILTER_NAME = "codec";
    private static final String RESET_BY_PEER_ERROR_MESSAGE = "connection reset by peer";

    protected static final long DEFAULT_TIMEOUT = 5000L;

    protected volatile MINASession session;
    private volatile Future<?> connectionAbortedFuture;

    protected void initConnector() throws Exception {

        disposeConnector();
    }

    @Override
    protected void internalStart() throws Exception {
        initConnector();
    }

    protected abstract String getHostname();

    protected abstract int getPort();

    protected void preConnect() throws Exception {
        MINASession session = getSession();

        if(session != null && !session.waitClose(getDisconnectTimeout())) {
            logger.error("Service is still connected to - {}:{}", getHostname(), getPort());
            this.session = null;
        }
    }

    public boolean isConnected() {
        return isConnected(getSession());
    }

    protected boolean isConnected(MINASession session) {
        return session != null && session.isConnected();
    }

    protected int getReaderIdleTimeout() {
        return 0;
    }

    protected int getWriterIdleTimeout() {
        return (int)(DEFAULT_TIMEOUT / 1000);
    }

    protected abstract Class<? extends AbstractCodec> getCodecClass() throws Exception;

    protected ICommonSettings getCodecSettings() {
        return getSettings();
    }

    protected void initFilterChain(DefaultIoFilterChainBuilder filterChain) throws Exception {
        CodecFactory codecFactory = new CodecFactory(serviceContext, messageFactory, dictionary, getCodecClass(), getCodecSettings(), this::getUpdateCodec);
        filterChain.addLast(CODEC_FILTER_NAME, new ProtocolCodecFilter(codecFactory));

        if (getSettings().isUseSSL()) {
            String format = "UseSSL is enabled, but requred parameter %s are missing";

            if (getSettings().getSslProtocol() == null) {
                throw new EPSCommonException(String.format(format, "SslProtocol"));
            }

            if (getSettings().getSslKeyStore() != null || getSettings().getKeyStoreType() != null) {
                if (getSettings().getSslKeyStore() == null) {
                    throw new EPSCommonException(String.format(format, "SslKeyStore"));
                } else if (getSettings().getKeyStoreType() == null) {
                    throw new EPSCommonException(String.format(format, "KeyStoreType"));
                }
            }
            char[] sslKeyStorePassword = getSettings().getSslKeyStorePassword() == null ? null : getSettings().getSslKeyStorePassword().toCharArray();

            filterChain.addFirst("SSLFilter", MINAUtil.createSslFilter(true, getSettings().getSslProtocol(),
                    getSettings().getKeyStoreType(), getSettings().getSslKeyStore(),
                    sslKeyStorePassword));
        }
    }

    @NotNull
    protected AbstractCodec getUpdateCodec(AbstractCodec codec) {
        return codec;
    }

    protected long getConnectTimeout() {
        return DEFAULT_TIMEOUT;
    }

    protected void handleNotConnected(Throwable throwable) {
        throw new ServiceException(String.format("Cannot establish session to address: %s:%s", getHostname(), getPort()), throwable);
    }

    protected MINASession createSession(IoSession session) {
        MINASession minaSession = new MINASession(getServiceName(), session, getSettings().getSendMessageTimeout());
        loggingConfigurator.registerLogger(minaSession, getServiceName());
        return minaSession;
    }

    protected void postConnect() throws Exception {}

    @Override
    public void connect() throws Exception {
        connect(getConnectTimeout());
    }

    protected abstract ConnectFuture getConnectFuture() throws Exception;

    public void connect(long timeout) throws Exception {
        logger.info("Connecting to - {}:{}", getHostname(), getPort());

        preConnect();

        ConnectFuture connectFuture = getConnectFuture();
        connectFuture.awaitUninterruptibly(timeout);

        if(!connectFuture.isConnected()) {
            handleNotConnected(connectFuture.getException());
            return;
        }
        changeStatus(status -> status == ServiceStatus.WARNING, ServiceStatus.STARTED, "Service connected");

        session = createSession(connectFuture.getSession());
        postConnect();

        logger.info("Connected to - {}:{}", getHostname(), getPort());
    }

    protected void preDisconnect() throws Exception {
        if(!isConnected()) {
            throw new ServiceException(String.format("Not connected to - %s:%s", getHostname(), getPort()));
        }
    }

    protected long getDisconnectTimeout() {
        return DEFAULT_TIMEOUT;
    }

    public void disconnect() throws Exception {
        logger.info("Disconnecting from - {}:{}", getHostname(), getPort());

        preDisconnect();
        MINASession session = getSession();

        if(session != null) {
            logger.info("Closing session: {}", session);
            session.close();

            if(!session.waitClose(getDisconnectTimeout())) {
                logger.error("Session hasn't been closed in specified timeout");
            }
        }

        changeStatus(status -> status != ServiceStatus.ERROR, ServiceStatus.WARNING, "Connection closed");
        
        postDisconnect();

        logger.info("Disconnected from - {}:{}", getHostname(), getPort());
    }

    protected void postDisconnect() throws Exception {}

    protected abstract void disposeConnector();

    @Override
    protected void disposeResources() {
        try {
            try {
                if(isConnected()) {
                    disconnect();
                }
            } catch(Exception e) {
                logger.error("Failed to disconnect service", e);
            } finally {
                disposeConnector();
            }
        } catch(Exception e) {
            logger.error("Failed to dispose inner resources", e);
        } finally {
            super.disposeResources();
        }
    }

    @Override
    public MINASession getSession() {
        return session;
    }

    @Override
    public AbstractMINASettings getSettings() {
        return (AbstractMINASettings)super.getSettings();
    }

    @Override
    public void sessionCreated(IoSession session) throws Exception {
        logger.info("Session created: {}", session);
    }

    @Override
    public void sessionOpened(IoSession session) throws Exception {
        logger.info("Session opened: {}", session);
        MINASession minaSession = getSession();

        if(minaSession != null) {
            getServiceHandler().sessionOpened(minaSession);
        }
    }

    @Override
    public void sessionClosed(IoSession session) throws Exception {
        logger.info("Session closed: {}", session);
        MINASession minaSession = getSession();

        if(minaSession != null) {
            getServiceHandler().sessionClosed(minaSession);
        }
    }

    @Override
    public void sessionIdle(IoSession session, org.apache.mina.core.session.IdleStatus status) throws Exception {
        logger.info("Session idle: {} (status: {})", session, status);
        MINASession minaSession = getSession();

        if(minaSession != null) {
            getServiceHandler().sessionIdle(minaSession, IdleStatus.fromMinaStatus(status));
        }
    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        logger.error("Session exception: {}", session, cause);

        String stackTrace = ExceptionUtils.getStackTrace(cause);
        IMessage message = ServiceUtil.createErrorMessage(stackTrace, "Unknown", getName(), serviceInfo, messageFactory);

        try {
            storage.storeMessage(message);
        } catch(Exception e) {
            logger.error("Failed to store error message", e) ;
        }

        MINASession minaSession = getSession();

        if(minaSession != null) {
            getServiceHandler().exceptionCaught(minaSession, cause);
        }

        if (isConnectionAbortedException(cause)) {
            Future<?> future = connectionAbortedFuture;
            if(future != null && !future.isDone() ) {
                logger.info("Canceling connection aborted task for " + getServiceName());
                future.cancel(true);
                logger.info("Connection aborted task for " + getServiceName() + " is cancelled");
            }
            connectionAbortedFuture = taskExecutor.addTask(() -> connectionAborted(session, cause));
        }
    }

    private static boolean isConnectionAbortedException(Throwable cause) {
        return cause instanceof IOException && StringUtils.containsIgnoreCase(cause.getMessage(), RESET_BY_PEER_ERROR_MESSAGE);
    }

    /**
     * Callback that will be called in case the connection was aborted.
     *
     * It will happen if the {@link IOException} with {@link #RESET_BY_PEER_ERROR_MESSAGE} message is caught by
     * the {@link #exceptionCaught} method.
     * @param session the current session
     * @param cause the abortion cause
     */
    protected void connectionAborted(IoSession session, Throwable cause) {
        logger.error("Connection aborted for session: {}", session);
    }

    @Override
    protected String getEndpointName() {
        return getHostname() + ':' + getPort();
    }

    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        logger.debug("Message received: {} (session: {})", message, session);

        if(message instanceof IMessage) {
            onMessageReceived((IMessage)message);
        }
    }

    @Override
    public void messageSent(IoSession session, Object message) throws Exception {
        logger.debug("Message sent: {} (session: {})", message, session);

        if(message instanceof IMessage) {
            onMessageSent((IMessage)message);
        }
    }

    /**
     * Method body is copied from {@link IoHandlerAdapter#inputClosed(IoSession)}
     */
    @Override
    public void inputClosed(IoSession session) throws Exception {
        logger.debug("Session input closed: {}", session);
        session.close(true);
    }
}
