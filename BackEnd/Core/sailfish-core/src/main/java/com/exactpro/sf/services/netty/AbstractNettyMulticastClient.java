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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.util.Objects;

import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.services.ServiceException;
import com.exactpro.sf.services.ServiceStatus;
import com.exactpro.sf.services.netty.sessions.AbstractNettySession;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFactory;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.InternetProtocolFamily;
import io.netty.channel.socket.nio.NioDatagramChannel;

public abstract class AbstractNettyMulticastClient extends AbstractNettyClient {
    
    //
    // Multicast docs:
    // http://redmine.exactprosystems.co.uk/projects/development/wiki/Multicast
    //
    
    protected NetworkInterface localNetworkInterface;
    
    protected InetSocketAddress multicastGroup;
    
    protected IDictionaryStructure dictionary;
    
    @Override
    public void connect() throws Exception {
        String interfaceIp = getSettings().getInterfaceIp();
        String mcastIp = getSettings().getMulticastIp();
        int mcastPort = getSettings().getMulticastPort();
        
        this.localNetworkInterface = NetworkInterface.getByInetAddress(InetAddress.getByName(interfaceIp));
        
        if (localNetworkInterface == null) {
            throw new ServiceException("Failed to resolve network interface via IP: " + interfaceIp);
        }
        
        this.multicastGroup = new InetSocketAddress(InetAddress.getByName(mcastIp), mcastPort);
        
        Bootstrap cb = new Bootstrap();
        cb.group(nioEventLoopGroup);
        cb.channelFactory(new NettyChannelFactory());
        cb.option(ChannelOption.SO_REUSEADDR, true);
        cb.option(ChannelOption.IP_MULTICAST_IF, localNetworkInterface);
        cb.option(ChannelOption.IP_MULTICAST_TTL, getSettings().getTtl());
        cb.localAddress(new InetSocketAddress(InetAddress.getByName(mcastIp), mcastPort));
        cb.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        cb.handler(NOOP_CHANNEL_INITIALIZER);
        
        Channel localChannel = cb.bind().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture bindFuture) throws Exception {
                if (!bindFuture.isSuccess()) {
                    return;
                }
                DatagramChannel channel = (DatagramChannel)bindFuture.channel();
                
                ChannelFuture future;
                String sourceIP = getSettings().getSourceIp();
                if (sourceIP == null) {
                    future = channel.joinGroup(multicastGroup, localNetworkInterface);
                } else {
                    future = channel.joinGroup(multicastGroup.getAddress(), localNetworkInterface, InetAddress.getByName(sourceIP));
                }
                future.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            }
        }).addListener(ChannelFutureListener.CLOSE_ON_FAILURE).syncUninterruptibly().channel();
        
        mainSession = createSession(localChannel);
        mainSession.withWriteLock(this::initChannel);
        mainSession.withWriteLock(this::initChannelCloseFuture);
    }
    
    @Override
    protected void initChannelCloseFuture(AbstractNettySession session, Channel channel) {
        channel.closeFuture().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    changeStatus(ServiceStatus.DISPOSED, "Service disposed", null);
                } else {
                    changeStatus(ServiceStatus.ERROR, "Failed to close channel", future.cause());
                }
            }
        });
    }
    
    @Override
    protected void initDictionaryData() {
        SailfishURI dictionaryURI = Objects.requireNonNull(settings.getDictionaryName(), "Dictionary URI is empty");
        dictionary = serviceContext.getDictionaryManager().getDictionary(dictionaryURI);
    }
    
    @Override
    public NettyMulticastClientSettings getSettings() {
        return (NettyMulticastClientSettings)settings;
    }
    
    private static class NettyChannelFactory implements ChannelFactory<Channel> {
        @Override
        public Channel newChannel() {
            return new NioDatagramChannel(InternetProtocolFamily.IPv4);
        }
    }
}
