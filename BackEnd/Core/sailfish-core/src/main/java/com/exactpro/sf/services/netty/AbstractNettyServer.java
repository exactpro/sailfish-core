/******************************************************************************
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
package com.exactpro.sf.services.netty;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.jetbrains.annotations.NotNull;

import com.exactpro.sf.services.IAcceptorService;
import com.exactpro.sf.services.IServiceContext;
import com.exactpro.sf.services.ISession;
import com.exactpro.sf.services.ServiceStatus;
import com.exactpro.sf.services.netty.handlers.ExceptionInboundHandler;
import com.exactpro.sf.services.netty.handlers.NettyServerHandler;
import com.exactpro.sf.services.netty.handlers.NettyServiceHandler;
import com.exactpro.sf.services.netty.sessions.AbstractNettySession;
import com.exactpro.sf.services.netty.sessions.NettyClientSession;
import com.exactpro.sf.services.netty.sessions.NettyServerSession;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelId;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public abstract class AbstractNettyServer extends AbstractNettyService implements IAcceptorService {

    private final ConcurrentMap<ChannelId, NettyClientSession> activeSessionMap = new ConcurrentHashMap<>();
    
    public Map<ChannelId, NettyClientSession> getActiveSessionMap() {
        return ImmutableMap.copyOf(activeSessionMap);
    }
    
    protected abstract LinkedHashMap<String, ChannelHandler> createChannelHandlers(IServiceContext serviceContext, AbstractNettySession session);
    
    @Override
    public List<ISession> getSessions() {
        return ImmutableList.copyOf(activeSessionMap.values());
    }
    
    protected void initClientSession(AbstractNettySession session, Channel channel) {
        Map<String, ChannelHandler> handlers = createChannelHandlers(serviceContext, session);
        handlers.forEach(channel.pipeline()::addLast);
        channel.pipeline().addLast(new NettyServiceHandler(handler, session),
                new NettyServerHandler(this::stopClientChannel),
                new ExceptionInboundHandler(this::onClientExceptionCaught));
        initChannelCloseFuture(session, channel);
    }
    
    private void onClientExceptionCaught(Channel channel, Throwable cause) {
        String message = "Exception caught in netty's pipeline";
        changeStatus(ServiceStatus.WARNING, message, cause);
        stopClientChannel(channel);
    }
    
    @Override
    public void connect() throws Exception {
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(nioEventLoopGroup, nioEventLoopGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        NettyClientSession clientSession = createClientSession(ch);
                        clientSession.withWriteLock(AbstractNettyServer.this::initClientSession);
                        activeSessionMap.put(ch.id(), clientSession);
                    }
                });
        
        Channel localChannel = serverBootstrap.bind(getPort())
                .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
                .addListener(ChannelFutureListener.CLOSE_ON_FAILURE)
                .awaitUninterruptibly()
                .channel();
        
        mainSession = createSession(localChannel);
        mainSession.withWriteLock(this::initChannelCloseFuture);
    }
    
    @Override
    public NettyServerSettings getSettings() {
        return (NettyServerSettings)settings;
    }
    
    public void stopClientChannel(Channel channel) {
        AbstractNettySession session = activeSessionMap.remove(channel.id());
        if (session == null) {
            logger.warn("Session with channel " + channel.id() + " is null");
        } else {
            session.close();
        }
    }
    
    @Override
    protected int getPort() {
        return getSettings().getPort();
    }
    
    @NotNull
    protected NettyClientSession createClientSession(Channel channel) {
        NettyClientSession session = new NettyClientSession(this, channel);
        loggingConfigurator.registerLogger(session, serviceName);
        return session;
    }
    
    @NotNull
    @Override
    protected NettyServerSession createSession(Channel channel) {
        NettyServerSession serverSession = new NettyServerSession(this, channel);
        loggingConfigurator.registerLogger(serverSession, serviceName);
        return serverSession;
    }
}