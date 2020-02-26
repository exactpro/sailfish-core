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
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import com.exactpro.sf.services.IServiceContext;
import com.exactpro.sf.services.netty.handlers.ExceptionInboundHandler;
import com.exactpro.sf.services.netty.sessions.AbstractNettySession;
import com.exactpro.sf.services.netty.sessions.NettyClientSession;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.nio.NioSocketChannel;

public abstract class AbstractNettyClient extends AbstractNettyService {
    
    protected abstract LinkedHashMap<String, ChannelHandler> createChannelHandlers(IServiceContext serviceContext);
    
    @Override
    public void connect() throws Exception {
        Bootstrap cb = new Bootstrap();
        cb.group(nioEventLoopGroup);
        cb.channel(NioSocketChannel.class);
        cb.option(ChannelOption.SO_REUSEADDR, true);
        cb.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        cb.handler(NOOP_CHANNEL_INITIALIZER);
        
        Channel localChannel = cb.connect(getHost(), getPort())
                .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
                .addListener(ChannelFutureListener.CLOSE_ON_FAILURE)
                .awaitUninterruptibly()
                .channel();
        
        mainSession = createSession(localChannel);
        mainSession.withWriteLock(this::initChannel);
        mainSession.withWriteLock(this::initChannelCloseFuture);
    }
    
    protected void initChannel(AbstractNettySession session, Channel channel) {
        Map<String, ChannelHandler> handlers = createChannelHandlers(serviceContext);
        handlers.forEach(channel.pipeline()::addLast);
        channel.pipeline().addLast(new ExceptionInboundHandler(this::onExceptionCaught));
    }
    
    @Override
    public NettyClientSettings getSettings() {
        return (NettyClientSettings)settings;
    }
    
    @NotNull
    @Override
    protected NettyClientSession createSession(Channel channel) {
        return new NettyClientSession(this, loggingConfigurator, channel);
    }
}