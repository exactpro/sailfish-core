/******************************************************************************
 * Copyright 2009-2023 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.services.http;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import javax.net.ssl.SSLException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.exactpro.sf.configuration.dictionary.DictionaryValidationError;
import com.exactpro.sf.services.http.dictionary.OAuthDictionaryValidatorFactory;
import com.exactpro.sf.services.http.oauth.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.IDictionaryManager;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.suri.SailfishURIException;
import com.exactpro.sf.services.IServiceContext;
import com.exactpro.sf.services.IServiceSettings;
import com.exactpro.sf.services.ServiceException;
import com.exactpro.sf.services.ServiceStatus;
import com.exactpro.sf.services.http.handlers.BaseHTTPMatcherHandlerDecode;
import com.exactpro.sf.services.http.handlers.BaseHTTPMatcherHandlerEncode;
import com.exactpro.sf.services.netty.NettyClientService;
import com.exactpro.sf.services.netty.NettySession;
import com.exactpro.sf.services.netty.handlers.MessagePersisterHandler;
import com.exactpro.sf.services.netty.handlers.NettyServiceHandler;
import com.exactpro.sf.services.util.ServiceUtil;

import io.netty.buffer.PooledByteBufAllocator;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

public abstract class HTTPClient extends NettyClientService {
    public static final String PROTOCOL_TYPE = "HTTP";
    public static final String REQUEST_ID_PROPERTY = "requestId";
    public static final String REQUEST_REFERENCE_PROPERTY = "requestRef";
    private static final int MAX_PARALLEL_REQUESTS = 1;

    protected final LinkedHashMap<String, ChannelHandler> handlers = new LinkedHashMap<String, ChannelHandler>(){
        @Override
        public ChannelHandler put(String key, ChannelHandler value) {
            if (containsKey(key)) {
                throw new EPSCommonException("Duplicate channel key found - " + key + ". Please check your handlers keys");
            }
            return super.put(key, value);
        }
    };
    protected HTTPClientSettings settings;
    protected IDictionaryStructure dictionary;
    protected URI uri;
    protected final Semaphore channelBusy = new Semaphore(MAX_PARALLEL_REQUESTS);
    private IMessageFactory messageFactory;
    protected final AtomicReference<String> cookie = new AtomicReference<>();
    protected AccessToken accessToken = null;

    @Override
    protected void initChannelHandlers(IServiceContext serviceContext) {

        Queue<ResponseInformation> queue = new ArrayDeque<>();

        handlers.clear();

        if(isTokenAuthenticationEnabled()) {
            try {
                accessToken = requestOAuthToken();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                throw new RuntimeException(e);
            }
        }

        if ("https".equalsIgnoreCase(uri.getScheme())) {
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

        handlers.put("http", new HttpClientCodec());
        handlers.put("aggregator", new HttpObjectAggregator(settings.getMaxHTTPMessageSize()));
        handlers.put("matcher-decoder", getDecodeMatcherHandler(queue, cookie));
        fillDecodeLayer(handlers);
        // BaseHTTPMatcherHandlerEncode.encode(ChannelHandlerContext, IMessage, List) (and implementations in its ancestors)
        // mutates and passes through an outbound IMessage while also producing a FullHttpResponse. Since we need to store
        // mutated version of the message, we, at the moment, cannot easily remove message passthrough from encode handler to
        // convert it to a terminal handler for IMessage and place it after persistence. Therefore we have to leave pipeline
        // as it is and have to consume the message in a dummy handler after it was persisted.
        handlers.put("outbound-imessage-blackhole", new OutboundBlackholeHandler<>(IMessage.class)); // FIXME: rework pipeline and remove it
        handlers.put("message-persister", new MessagePersisterHandler(storage, serviceInfo));
        handlers.put("handler", new NettyServiceHandler(serviceHandler, getSession(), messageFactory, getSettings().isEvolutionSupportEnabled()));
        handlers.put("matcher-encoder", getEncodeMatcherHandler(queue, cookie));
        if(isTokenAuthenticationEnabled()) {
            handlers.put("token-authorization", new ChannelInboundHandlerAdapter() {
                @Override
                public void channelRead(@NotNull ChannelHandlerContext context, @NotNull Object msg) throws Exception {
                    if (accessToken != null && msg instanceof HttpRequest) {
                        HttpRequest request = (HttpRequest) msg;
                        request.headers().set("Authorization", "Bearer " + accessToken.getAccessToken());
                    }
                    super.channelRead(context, msg);
                }
            });
        }
        fillEncodeLayer(handlers);
    }

    protected void setupClientCertificate(SslContextBuilder sslContextBuilder) {
        boolean hasClientCertificate = isNotBlank(settings.getClientCertificate());
        boolean hasPrivateKey = isNotBlank(settings.getPrivateKey());
        if (hasClientCertificate ^ hasPrivateKey) {
            throw new IllegalArgumentException("Both Client Certificate and Private Key should be specified or should not be specified at all");
        }
        if (hasClientCertificate) {
            try (InputStream cert = loadByAliasOrFromValue(settings.getClientCertificate());
                    InputStream key = loadByAliasOrFromValue(settings.getPrivateKey())) {
                sslContextBuilder.keyManager(cert, key, settings.getKeyPhrase());
            } catch (IOException e) {
                throw new EPSCommonException("Cannot read certificate or private key", e);
            }
        }
    }

    protected InputStream loadByAliasOrFromValue(String aliasOrValue) {
        if (aliasOrValue.startsWith(ServiceUtil.ALIAS_PREFIX)) {
            String alias = aliasOrValue.substring(ServiceUtil.ALIAS_PREFIX.length());
            try {
                return dataManager.getDataInputStream(SailfishURI.parse(alias));
            } catch (SailfishURIException e) {
                throw new EPSCommonException("Cannot parse alias " + alias, e);
            }
        }
        return IOUtils.toInputStream(aliasOrValue, StandardCharsets.UTF_8);
    }

    protected AccessToken requestOAuthToken() {
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            URI uri = new URI(settings.getTokenRequestUrl().trim());

            Bootstrap b = new Bootstrap();
            b.group(workerGroup);
            b.channel(NioSocketChannel.class);
            b.option(ChannelOption.SO_KEEPALIVE, true);
            b.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(@NotNull SocketChannel ch) throws Exception {
                    if("https".equalsIgnoreCase(uri.getScheme())) {
                        try {
                            SslContextBuilder sslContextBuilder = SslContextBuilder.forClient()
                                    .trustManager(InsecureTrustManagerFactory.INSTANCE);
                            setupClientCertificate(sslContextBuilder);

                            SslContext sslContext = sslContextBuilder.build();
                            ch.pipeline().addLast(sslContext.newHandler(PooledByteBufAllocator.DEFAULT));
                        } catch (SSLException e) {
                            throw new EPSCommonException("Filed to create ssl handler", e);
                        }
                    }
                    ch.pipeline().addLast(
                            new HttpResponseDecoder(),
                            new HttpRequestEncoder(),
                            new HttpObjectAggregator(settings.getMaxHTTPMessageSize())
                    );
                }
            });

            logger.info("Connecting to access token endpoint ({}, {})", uri.getHost(), uri.getPort());

            OAuthHttpMessageConverter messageConverter = new OAuthHttpMessageConverter(messageFactory);
            TokenResponseChannelHandler tokenResponseHandler = new TokenResponseChannelHandler(messageConverter);

            Channel channel = b.connect(uri.getHost(), uri.getPort()).sync().channel();
            channel.pipeline().addLast(
                    new OAuthHttpMessageConverterHandler(messageConverter),
                    new MessagePersisterHandler(storage, serviceInfo),
                    new NettyServiceHandler(
                            serviceHandler, getSession(), messageFactory, getSettings().isEvolutionSupportEnabled()
                    ),
                    tokenResponseHandler
            );

            logger.info("Sending request to the endpoint");
            FullHttpRequest request = OAuthHttpRequestBuilder.clientCredentialsRequest(
                    uri, settings.getUserName(), settings.getPassword(), true
            );
            channel.writeAndFlush(request);
            channel.closeFuture().sync();

            logger.info("Request is sent successfully");

            AccessTokenError accessTokenError = tokenResponseHandler.getAccessTokenError();
            if(accessTokenError != null) {
                String errorMessage = "Access Token error: " + accessTokenError.getError();
                logger.error(errorMessage);
                throw new ServiceException(errorMessage);
            }
            Throwable tokenHandlerException = tokenResponseHandler.getException();
            if(tokenHandlerException != null) {
                logger.error(tokenHandlerException.getMessage(), tokenHandlerException);
                throw new ServiceException(tokenHandlerException);
            }
            AccessToken accessToken = tokenResponseHandler.getAccessToken();
            if(accessToken == null) {
                String errorMessage = "Can't request access token";
                logger.error(errorMessage);
                throw new ServiceException(errorMessage);
            }
            return accessToken;
        } catch (URISyntaxException | InterruptedException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        } finally {
            workerGroup.shutdownGracefully();
        }
    }

    @Override
    protected LinkedHashMap<String, ChannelHandler> getChannelHandlers() {
        return handlers;
    }

    @Override
    protected void sendHeartBeat() throws InterruptedException {
        //do nothing
    }

    @Override
    protected void initService(IDictionaryManager dictionaryManager, IServiceSettings settings) {
        this.settings = (HTTPClientSettings)settings;
        this.dictionary = dictionaryManager.getDictionary(settings.getDictionaryName());
        this.messageFactory = dictionaryManager.getMessageFactory(settings.getDictionaryName());
        try {
            this.uri = new URI(this.settings.getURI().trim());
            if (uri.getPort() == -1) {
                throw new ServiceException("Specified URI don't contains port");
            }
            if(isTokenAuthenticationEnabled()) {
                if(isBlank(this.settings.getUserName())) {
                    throw new ServiceException("Username is not specified");
                }
                if(isBlank(this.settings.getPassword())) {
                    throw new ServiceException("Password is not specified");
                }
            }
        } catch (URISyntaxException e) {
            throw new ServiceException(e.getMessage(), e);
        }

        if(isTokenAuthenticationEnabled()) {
            List<DictionaryValidationError> errors = OAuthDictionaryValidatorFactory.INSTANCE
                    .validate(dictionary, true, null);
            if (!errors.isEmpty()) {
                String listOfMessages = errors.stream()
                        .map(DictionaryValidationError::getMessage)
                        .collect(Collectors.joining(", "));
                String errorMessage = "These required messages are not in the dictionary: " + listOfMessages;
                logger.error(errorMessage);
                throw new ServiceException(errorMessage);
            }
        }
    }

    @Override
    protected void onStarting() {
        resetSemaphoreState();
        super.onStarting();
    }

    private void resetSemaphoreState() {
        // Restore semaphore state
        // Reset permits to zero
        channelBusy.drainPermits();
        // And make one available
        channelBusy.release();
        int availablePermits = channelBusy.availablePermits();
        if (availablePermits != MAX_PARALLEL_REQUESTS) {
            throw new IllegalStateException(
                    String.format("internal semaphore state is no valid. Expected %d permits, but has %d",
                            MAX_PARALLEL_REQUESTS, availablePermits
                    )
            );
        }
    }

    private boolean isTokenAuthenticationEnabled() {
        return isNotBlank(settings.getTokenRequestUrl());
    }

        @Override
    protected void disposeService(@NotNull NettySession session) {
        try {
            channelBusy.release();
        } finally {
            super.disposeService(session);
        }
    }

    @Override
    protected int getPort() {
        return uri.getPort();
    }

    @Override
    protected String getHost() {
         return uri.getHost();
    }

    protected URI getURI() {
        return uri;
    }

    @Override
    public HTTPClientSettings getSettings() {
        return settings;
    }

    @Override
    protected void initChannelCloseFuture(Channel channel) {
        channel.closeFuture().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                changeStatus(status -> status != ServiceStatus.DISPOSING && status != ServiceStatus.DISPOSED && status != ServiceStatus.ERROR,
                        ServiceStatus.WARNING, "Connection closed");
                logger.info("Channel closed to [{}], hash {}", channel.remoteAddress(), channel.hashCode());

                //DO NOT release channelBusy semaphore at this place because this function calls before critical section ends
            }
        });
    }

    /*
     * Fill handlers to pipeline from http layer
     */
    protected abstract void fillDecodeLayer(LinkedHashMap<String, ChannelHandler> handlers);

    /*
     * Fill handlers to pipeline from core
     */
    protected abstract void fillEncodeLayer(LinkedHashMap<String, ChannelHandler> handlers);

    protected MessageToMessageEncoder<IMessage> getEncodeMatcherHandler(Queue<ResponseInformation> queue, AtomicReference<String> cookie) {
        return new BaseHTTPMatcherHandlerEncode(dictionary, settings, msgFactory, getName(), queue, channelBusy, cookie);
    }

    protected MessageToMessageDecoder<FullHttpResponse> getDecodeMatcherHandler(Queue<ResponseInformation> queue, AtomicReference<String> cookie) {
        return new BaseHTTPMatcherHandlerDecode(dictionary, settings, msgFactory, getName(), queue, channelBusy, cookie);
    }

    //FIXME: Inherit Netty service from IInitiatorService
    protected void changeStatus(Predicate<ServiceStatus> predicate, ServiceStatus status, String message) {
        if (predicate.test(getStatus())) {
            changeStatus(status, message, null);
        }
    }
    
    @Override
    protected NettySession createSession() {
        HTTPSession httpSession = new HTTPSession(this);
        logConfigurator.registerLogger(httpSession, getServiceName());
        return httpSession;
    }


    @Override
    public void connect() throws Exception {
        super.connect();
        if (serviceStatus == ServiceStatus.WARNING) {
            changeStatus(ServiceStatus.STARTED, "Service " + serviceName + " connected", null);
        }
        if (logger.isInfoEnabled()) {
            Channel localChannel = getChannel();
            if (localChannel != null) {
                logger.info("Channel opened to [{}], hash {}", localChannel.remoteAddress(), localChannel.hashCode());
            } else {
                logger.info("Channel is not set for service [{}]", serviceName);
            }
        }
    }
}
