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
package com.exactpro.sf.services.websocket;

import java.util.Objects;

import javax.net.ssl.SSLException;

import org.jetbrains.annotations.NotNull;

import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.IDictionaryManager;
import com.exactpro.sf.services.IServiceContext;
import com.exactpro.sf.services.IServiceSettings;
import com.exactpro.sf.services.ServiceStatus;
import com.exactpro.sf.services.http.HTTPClient;
import com.exactpro.sf.services.netty.NettySession;
import com.exactpro.sf.services.netty.handlers.MessagePersisterHandler;
import com.exactpro.sf.services.netty.handlers.NettyServiceHandler;
import com.exactpro.sf.services.websocket.handlers.BaseAuthorizationManager;
import com.exactpro.sf.services.websocket.handlers.HandlerDecode;
import com.exactpro.sf.services.websocket.handlers.HandlerEncode;
import com.exactpro.sf.services.websocket.handlers.HandlerEncode.FrameType;
import com.exactpro.sf.services.websocket.handlers.IHandshaker;
import com.exactpro.sf.services.websocket.handlers.RawMessagePacker;
import com.exactpro.sf.services.websocket.handlers.RawMessageUnpacker;
import com.exactpro.sf.services.websocket.handlers.WebSocketHandshakeHandler;

import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

/**
 * @author sergey.smirnov
 *
 */
public abstract class WebSocketClient extends HTTPClient {


    private WebSocketHandshakeHandler clientHandshakerHandler;
    private WebSocketClientSettings settings;
    private final FrameType frameType;

    /**
     * 
     */
    public WebSocketClient() {
        this(FrameType.BINARY);
    }

    /**
     * 
     */
    public WebSocketClient(FrameType frameType) {
        this.frameType = Objects.requireNonNull(frameType, "'Frame type' parameter");
    }
    /* (non-Javadoc)
     * @see com.exactpro.sf.services.http.HTTPClient#initService(com.exactpro.sf.configuration.IDictionaryManager, com.exactpro.sf.services.IServiceSettings)
     */
    @Override
    protected void initService(IDictionaryManager dictionaryManager, IServiceSettings settings) {
        super.initService(dictionaryManager, settings);
        this.settings = (WebSocketClientSettings) Objects.requireNonNull(settings, "'Service settings' parameter");
    }

    /* (non-Javadoc)
     * @see com.exactpro.sf.services.http.HTTPClient#initChannelHandlers()
     */
    @Override
    protected void initChannelHandlers(IServiceContext serviceContext) {
        handlers.clear();

        if ("wss".equalsIgnoreCase(uri.getScheme())) {
            try {
                SslContextBuilder sslContextBuilder = SslContextBuilder.forClient()
                        .trustManager(InsecureTrustManagerFactory.INSTANCE);
                setupClientCertificate(sslContextBuilder);

                SslContext sslContext = sslContextBuilder.build();
                handlers.put("ssl", sslContext.newHandler(PooledByteBufAllocator.DEFAULT));
            } catch (SSLException e) {
                throw new EPSCommonException("Filed to create ssl handler",e);
            }
        }

        handlers.put("http", new HttpClientCodec());    // ▲/▼
        handlers.put("aggregator", new HttpObjectAggregator(settings.getMaxHTTPMessageSize())); //▼
        this.clientHandshakerHandler = new WebSocketHandshakeHandler(settings.getMaxWebSocketFrameSize());
        handlers.put("handshaker", clientHandshakerHandler); // ▼
        handlers.put("buffer builder", new HandlerDecode(msgFactory)); // ▼
        fillDecodeLayer(handlers);
        handlers.put("raw-message-unpacker-incoming", new RawMessageUnpacker(getName(), settings.getURI())); // ▼
        handlers.put("frame encoder", new HandlerEncode(frameType)); // ▲
        handlers.put("message-persister", new MessagePersisterHandler(storage, serviceInfo));
        handlers.put("handler", new NettyServiceHandler(serviceHandler, getSession(), msgFactory, getSettings().isEvolutionSupportEnabled())); //▲/▼
        handlers.put("auth-manager", getAuthorizationManager(clientHandshakerHandler)); // ▲/▼
        handlers.put("raw-message-unpacker-outgoing", new RawMessageUnpacker(getName(), settings.getURI())); // ▲
        fillEncodeLayer(handlers);
        handlers.put("raw-message-packer", new RawMessagePacker()); // ▲
    }

    protected BaseAuthorizationManager getAuthorizationManager(IHandshaker handshakeHandler) {
        return new BaseAuthorizationManager(handshakeHandler, settings);
    }

    @Override
    public void start() {
        try {
            changeStatus(ServiceStatus.STARTING, "Starting service " + serviceName, null);

            logConfigurator.createAppender(serviceName);
            logConfigurator.registerLogger(this, serviceName);

            // FIXME: clean resources in dispose method and ERROR status in root class
            if (nioEventLoopGroup == null) {
                nioEventLoopGroup = new NioEventLoopGroup();
            }
            nettySession = new NettySession(this);
            logConfigurator.registerLogger(nettySession, getServiceName());

            if (settings.isAutoConnect()) {
                connect();
            }

            changeStatus(ServiceStatus.STARTED, "Service " + serviceName + " started", null);

        } catch (Exception ex) {
            if (serviceStatus != ServiceStatus.ERROR) {
                changeStatus(ServiceStatus.ERROR, ex.getMessage(), ex);
            }
        }
    }

    @Override
    public void connect() throws Exception {
        try {
            if (serviceStatus != ServiceStatus.STARTING
                    && serviceStatus != ServiceStatus.STARTED
                    && serviceStatus != ServiceStatus.WARNING) {
                throw new IllegalStateException("Service: " + serviceName + ". Illegal service status " + serviceStatus + " for connect action");
            }
            super.connect();
            startHandshake();
            Thread connector = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted() && !clientHandshakerHandler.isHandshakeComplete()) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            });

            connector.start();
            connector.join(settings.getExpectedTimeOfStarting());
            connector.interrupt();
            if (!clientHandshakerHandler.isHandshakeComplete()) {
                throw new EPSCommonException("Handshake not complete");
            }
        } catch (Exception ex) {
            changeStatus(ServiceStatus.ERROR, ex.getMessage(), ex);
            throw ex;
        }
    }

    protected void startHandshake() throws Exception {
        // do nothing cause handshake start on channelActive() in BaseAuthorizationManager
    }

    /* (non-Javadoc)
     * @see com.exactpro.sf.services.netty.NettyClientService#dispose()
     */
    @Override
    public void disposeService(@NotNull NettySession session) {
        try {
            if (clientHandshakerHandler != null) {
                clientHandshakerHandler.close(getChannel());
            }
        } finally {
            super.disposeService(session);
        }
    }

    @Override
    public WebSocketClientSettings getSettings() {
        return settings;
    }
}
