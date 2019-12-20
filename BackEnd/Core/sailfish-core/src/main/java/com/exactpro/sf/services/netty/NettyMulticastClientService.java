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
package com.exactpro.sf.services.netty;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.util.LinkedHashMap;

import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.configuration.IDictionaryManager;
import com.exactpro.sf.services.IServiceSettings;
import com.exactpro.sf.services.ServiceException;
import com.exactpro.sf.services.ServiceStatus;
import com.exactpro.sf.services.netty.handlers.ExceptionInboundHandler;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFactory;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.InternetProtocolFamily;
import io.netty.channel.socket.nio.NioDatagramChannel;

public abstract class NettyMulticastClientService extends NettyClientService {

	//
	// Multicast docs:
	// http://redmine.exactprosystems.co.uk/projects/development/wiki/Multicast
	//

	protected NetworkInterface localNetworkInterface;

	protected InetSocketAddress multicastGroup;

	protected IDictionaryStructure dictionary;

	@Override
	public void start() {
		try {
			changeStatus(ServiceStatus.STARTING, "Starting service " + serviceName, null);

            logConfigurator.createIndividualAppender(getClass().getName() + "@" + Integer.toHexString(hashCode()),
                    serviceName);

			nettySession = createSession();
            nioEventLoopGroup = new NioEventLoopGroup();

            initChannelHandlers(serviceContext);

			connect();

			changeStatus(ServiceStatus.STARTED, "Service " + serviceName + " started", null);
		} catch (Throwable ex) {
			changeStatus(ServiceStatus.ERROR, ex.getMessage(), ex);
		}
	}


	@Override
	public void connect() throws Exception {

	    try {
	        channelLock.writeLock().lock();
            LinkedHashMap<String, ChannelHandler> handlers = getChannelHandlers();

            String interfaceIp = getSettings().getInterfaceIp();
            String mcastIp = getSettings().getMulticastIp();
            int mcastPort = getSettings().getMulticastPort();

            this.localNetworkInterface = NetworkInterface.getByInetAddress(Inet4Address.getByName(interfaceIp));

            if (this.localNetworkInterface == null) {
                throw new ServiceException("Failed to resolve network interface via IP: " + interfaceIp);
            }

            this.multicastGroup = new InetSocketAddress(InetAddress.getByName(mcastIp), mcastPort);

            Bootstrap cb = new Bootstrap();
            // Fixme: use ITaskExecutor ?
            cb.group(nioEventLoopGroup);
            cb.channelFactory(new ChannelFactory<Channel>() {
                @Override
                public Channel newChannel() {
                    // Force IPv4
                    return new NioDatagramChannel(InternetProtocolFamily.IPv4);
                }
            });
            cb.option(ChannelOption.SO_REUSEADDR, true);
            cb.option(ChannelOption.IP_MULTICAST_IF, localNetworkInterface);
            cb.option(ChannelOption.IP_MULTICAST_TTL, getSettings().getTtl());
            cb.localAddress(new InetSocketAddress(InetAddress.getByName(mcastIp), mcastPort));
            // we can configure java -Dio.netty.allocator.numDirectArenas=... -Dio.netty.allocator.numHeapArenas=...
            cb.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
            cb.handler(new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel ch) throws Exception {
                    handlers.forEach((key, value) -> ch.pipeline().addLast(key, value));
                    // add exception handler for inbound messages
                    // outbound exceptions will be routed here by ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE
                    ch.pipeline().addLast(new ExceptionInboundHandler(nettySession));

                }
            });

            Channel localChannel = cb.bind().addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture bindFuture) throws Exception {
                    if (!bindFuture.isSuccess()) {
                        return;
                    }
                    DatagramChannel channel = (DatagramChannel) bindFuture.channel();
                    // TODO: heartbeat loss detection

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

            localChannel.closeFuture().addListener((ChannelFutureListener) future -> changeStatus(ServiceStatus.DISPOSED, "Connection closed", null));
            setChannel(localChannel);
        } finally {
	        channelLock.writeLock().unlock();
        }
	}

	public void stop(String message, Throwable cause) {
		changeStatus(ServiceStatus.DISPOSING, message, cause);
		stopSendHeartBeats();

		Channel localChannel = getChannel();
		if (localChannel != null) {
			((DatagramChannel) localChannel).leaveGroup(multicastGroup, localNetworkInterface).addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					if (!future.isSuccess()) {
						logger.error("Failed to leave multicast group [{}]", multicastGroup);
					}
					localChannel.close().addListener(new ChannelFutureListener() {
						@Override
						public void operationComplete(ChannelFuture future) throws Exception {
							if (future.isSuccess()) {
								changeStatus(ServiceStatus.DISPOSED, "", null);
							} else {
								changeStatus(ServiceStatus.ERROR, "Failed to close channel", future.cause());
							}
						}
					});
				}
			});
		}

	}


	@Override
	protected void initService(IDictionaryManager dictionaryManager, IServiceSettings settings) {
        if(settings.getDictionaryName() == null) {
            throw new IllegalArgumentException("'dictionaryName' parameter incorrect");
        }

        this.dictionary = dictionaryManager.getDictionary(settings.getDictionaryName());

        if(dictionary == null) {
            throw new ServiceException("can't create dictionary");
        }
	}

	@Override
	public NettyMulticastClientSettings getSettings() {
		return (NettyMulticastClientSettings) settings;
	}

}
