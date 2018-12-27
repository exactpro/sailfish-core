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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.aml.script.actions.WaitAction;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.services.ServiceInfo;
import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.configuration.IDataManager;
import com.exactpro.sf.configuration.IDictionaryManager;
import com.exactpro.sf.configuration.ILoggingConfigurator;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.IActionContext;
import com.exactpro.sf.services.IInitiatorService;
import com.exactpro.sf.services.IServiceContext;
import com.exactpro.sf.services.IServiceHandler;
import com.exactpro.sf.services.IServiceMonitor;
import com.exactpro.sf.services.IServiceSettings;
import com.exactpro.sf.services.ISession;
import com.exactpro.sf.services.ITaskExecutor;
import com.exactpro.sf.services.IdleStatus;
import com.exactpro.sf.services.ServiceStatus;
import com.exactpro.sf.services.netty.handlers.ExceptionInboundHandler;
import com.exactpro.sf.services.util.ServiceUtil;
import com.exactpro.sf.storage.IMessageStorage;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

public abstract class NettyClientService implements IInitiatorService {

	protected final Logger logger = LoggerFactory.getLogger(this.getClass().getName() + "@" + Integer.toHexString(hashCode()));

    protected final ChannelFutureListener SET_CHANNEL = future -> setChannel(future.channel());

	protected IServiceContext serviceContext;
	protected ServiceName serviceName;
	protected IServiceHandler serviceHandler;
	protected NettyClientSettings settings;
	protected IMessageStorage storage;
	protected ServiceInfo serviceInfo;
	protected IServiceMonitor serviceMonitor;
	protected ILoggingConfigurator logConfigurator;
	protected ITaskExecutor taskExecutor;
	protected IDataManager dataManager;
	protected IWorkspaceDispatcher workspaceDispatcher;
	protected volatile ServiceStatus serviceStatus = ServiceStatus.CREATED;

	protected volatile NettySession nettySession; // FIXME volatile??
	protected volatile NioEventLoopGroup nioEventLoopGroup = null;

	protected IMessageFactory msgFactory;
	protected Future<?> hbFuture = null;

    private final ReadWriteLock channelLock = new ReentrantReadWriteLock();
    private Channel channel = null;

	// At least it should contains:
	// * YourCodec(s)
	// * MessagePersisterHandler
	// * NettyServiceHandler
	// Warning: order of the handlers matters. Please refer to netty documentation:
	//  http://netty.io/4.0/api/io/netty/channel/ChannelPipeline.html
	protected abstract void initChannelHandlers(IServiceContext serviceContext);
	protected abstract LinkedHashMap<String, ChannelHandler> getChannelHandlers();
	protected abstract void sendHeartBeat() throws InterruptedException;
	protected abstract void initService(IDictionaryManager dictionaryManager, IServiceSettings settings);
	// You can use it to implement multi-step connection (pre logon, load-balancers...)
	protected abstract int getPort();
	protected abstract String getHost();

    private class HeartBeatTask implements Runnable {
		@Override
		public void run() {
			try {
				// FIXME: synchronize?
				serviceHandler.sessionIdle(nettySession, IdleStatus.WRITER_IDLE);
				NettyClientService.this.sendHeartBeat();
			} catch (Exception e) {
				logger.error("OnIdle", e);
			}
		}
	}

	@Override
	public void init(
			final IServiceContext serviceContext,
			final IServiceMonitor serviceMonitor,
			final IServiceHandler handler,
			final IServiceSettings settings,
			final ServiceName name) {

		try {
            changeStatus(ServiceStatus.INITIALIZING, "Service initializing", null);

            this.serviceName = name;
            this.serviceContext = Objects.requireNonNull(serviceContext, "'Service context' parameter");
            this.serviceHandler = handler;
            this.settings = (NettyClientSettings) settings;
            this.msgFactory = this.serviceContext.getDictionaryManager().getMessageFactory(this.settings.getDictionaryName());
            this.storage = this.serviceContext.getMessageStorage();
            this.serviceInfo = serviceContext.lookupService(name);
            this.serviceMonitor = serviceMonitor;
            this.logConfigurator = this.serviceContext.getLoggingConfigurator();
            this.taskExecutor = this.serviceContext.getTaskExecutor();
            this.dataManager = this.serviceContext.getDataManager();
            this.workspaceDispatcher = Objects.requireNonNull(serviceContext.getWorkspaceDispatcher(), "'Workspace dispatcher' parameter");

            // most implementations override this method... so, it will throw ClassCastException if settings have incorrect type
            getSettings();

			initService(this.serviceContext.getDictionaryManager(), settings);

			changeStatus(ServiceStatus.INITIALIZED, "Service " + name + " have been initialized", null);
		} catch (Throwable ex) {
			changeStatus(ServiceStatus.ERROR, "Service " + name + " failed to init", ex);
		}
	}

	@Override
	public void start() {
		try {
			changeStatus(ServiceStatus.STARTING, "Starting service " + serviceName, null);

            logConfigurator.createIndividualAppender(this.getClass().getName() + "@" + Integer.toHexString(hashCode()),
                serviceName);

			nettySession = new NettySession(this, logConfigurator);

			if (nioEventLoopGroup == null) {
			    this.nioEventLoopGroup = new NioEventLoopGroup();
            }

			connect();

			changeStatus(ServiceStatus.STARTED, "Service " + serviceName + " started", null);
		} catch (Throwable ex) {
			changeStatus(ServiceStatus.ERROR, ex.getMessage(), ex);
		}
	}


	@Override
	public void connect() throws Exception {
	    this.initChannelHandlers(this.serviceContext);

		final LinkedHashMap<String, ChannelHandler> handlers = getChannelHandlers();

		Bootstrap cb = new Bootstrap();
		// Fixme: use ITaskExecutor ?
		cb.group(nioEventLoopGroup);
		cb.channel(NioSocketChannel.class);
		cb.option(ChannelOption.SO_REUSEADDR, true);
		// we can configure java -Dio.netty.allocator.numDirectArenas=... -Dio.netty.allocator.numHeapArenas=...
		cb.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
		cb.handler(new ChannelInitializer<Channel>() {
			@Override
			protected void initChannel(Channel ch) throws Exception {
				for (Map.Entry<String, ChannelHandler> entry : handlers.entrySet()) {
					ch.pipeline().addLast(entry.getKey(), entry.getValue());
				}
				// add exception handler for inbound messages
				// outbound exceptions will be routed here by ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE
				ch.pipeline().addLast(new ExceptionInboundHandler(nettySession));

			}
		});

        Channel localChannel =  cb.connect(getHost(), getPort())
                .addListener(SET_CHANNEL)
				.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
				.addListener(ChannelFutureListener.CLOSE_ON_FAILURE)
				.syncUninterruptibly()
				.channel();

		initChannelCloseFuture(localChannel);
	}

	protected void initChannelCloseFuture(Channel channel) {
	    channel.closeFuture().addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
			    stopSendHeartBeats();
				//changeStatus(ServiceStatus.DISPOSED, "Connection closed", null);
			}
		});
	}

	@Override
	public void dispose() {
		this.changeStatus(ServiceStatus.DISPOSING, "Service disposing", null);
		
		try {
    		NettySession session = this.nettySession;
    		if (session != null) {
    		    nettySession = null;
    	        session.close();
    		}
		} catch (RuntimeException e) {
		    this.changeStatus(ServiceStatus.ERROR, "Session '"+serviceName +"'  has not been closed", e);
		} finally {
            Channel localChannel = getChannel();
            if (localChannel != null) {
                if (localChannel.isOpen()) {
                    if (!localChannel.close().awaitUninterruptibly(5, TimeUnit.SECONDS)) {
                        this.changeStatus(ServiceStatus.ERROR, "Channel '" + serviceName + "' has not been closed for 5 secons", null);
                    }
                }
                
                if (!localChannel.isOpen()) {
                    setChannel(null);
                }
            }
                
            NioEventLoopGroup nioEventLoopGroup = this.nioEventLoopGroup;
            
            if (nioEventLoopGroup != null) {
                if (!nioEventLoopGroup.isShutdown()) {
                    if(!nioEventLoopGroup.shutdownGracefully().awaitUninterruptibly(5, TimeUnit.SECONDS)) {
                        this.changeStatus(ServiceStatus.ERROR, "Events executor '" + serviceName + "' has not been closed for 5 secons", null);
                    }
                }
                
                if (this.nioEventLoopGroup.isShutdown()) {
                    this.nioEventLoopGroup = null;
                }
            }
            
            if(logConfigurator != null){
                logConfigurator.destroyIndividualAppender(this.getClass().getName() + "@" + Integer.toHexString(hashCode()),
                        serviceName);
            }
        }

		if (this.serviceStatus == ServiceStatus.DISPOSING) {
		    this.changeStatus(ServiceStatus.DISPOSED, "Service disposed", null);
		}
	}

	protected void changeStatus(ServiceStatus status, String message, Throwable e) {
		this.serviceStatus = status;
		ServiceUtil.changeStatus(this, serviceMonitor, status, message, e);
	}

	@Override
	public IServiceHandler getServiceHandler() {
		return serviceHandler;
	}

	@Override
	public void setServiceHandler(IServiceHandler handler) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getName() {
		return serviceName.toString();
	}

	@Override
	public ServiceName getServiceName() {
		return serviceName;
	}

	@Override
	public ServiceStatus getStatus() {
		return serviceStatus;
	}

	@Override
	public NettyClientSettings getSettings() {
		return settings;
	}

	@Override
	public ISession getSession() {
		return nettySession;
	}

	@Override
    public IMessage receive(IActionContext actionContext, IMessage msg) throws InterruptedException {
		return (IMessage) WaitAction.waitForMessage(actionContext, msg, !msg.getMetaData().isAdmin());
    }

	// Used in session
	public Channel getChannel() {
        try {
            this.channelLock.readLock().lock();
		return channel;
        } finally {
            this.channelLock.readLock().unlock();
        }
	}

	public void stop(String message, Throwable cause) {
		channel.close().addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
			    if (!future.isSuccess()) {
			        changeStatus(ServiceStatus.ERROR, "Failed to close channel", future.cause());
			    }
			}
		});
	}

	public void onExceptionCaught(Throwable cause) {
		stop("Exception caught in netty's pipeline", cause);
	}

	//FIXME typo in method name
	protected void statrtSendHeartBeats(long heartbeatInterval) {
		if (heartbeatInterval > 0) {
			if (hbFuture != null) {
				logger.error("IllegalState: heartbeat timer not stopped");
				hbFuture.cancel(true);
			}
			hbFuture = taskExecutor.addRepeatedTask(new HeartBeatTask(), heartbeatInterval, heartbeatInterval, TimeUnit.MILLISECONDS);
		}
	}

	protected void stopSendHeartBeats() {
		if (hbFuture != null) {
			hbFuture.cancel(false);
			hbFuture = null;
		}
	}

	private void setChannel(Channel channel) {
        try {
            this.channelLock.writeLock().lock();
            this.channel = channel;
        } finally {
            this.channelLock.writeLock().unlock();
        }
    }
}
