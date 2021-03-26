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

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.aml.script.actions.WaitAction;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.configuration.IDictionaryManager;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.IActionContext;
import com.exactpro.sf.services.AbstractService;
import com.exactpro.sf.services.IInitiatorService;
import com.exactpro.sf.services.IServiceSettings;
import com.exactpro.sf.services.ServiceEvent.Type;
import com.exactpro.sf.services.ServiceEventFactory;
import com.exactpro.sf.services.ServiceStatus;
import com.exactpro.sf.services.netty.internal.NettyEmbeddedPipeline;
import com.exactpro.sf.services.netty.internal.handlers.RawSendHandler;
import com.exactpro.sf.services.netty.sessions.AbstractNettySession;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;

public abstract class AbstractNettyService extends AbstractService implements IInitiatorService {
    
    protected final Logger logger = LoggerFactory.getLogger(getClass().getName() + '@' + Integer.toHexString(hashCode()));
    
    protected IMessageFactory msgFactory;
    protected IWorkspaceDispatcher workspaceDispatcher;
    
    @Nullable
    protected volatile NioEventLoopGroup nioEventLoopGroup;
    protected volatile AbstractNettySession mainSession;
    
    @NotNull
    protected final ConcurrentMap<AbstractNettySession, Future<?>> heartBeatFuture = new ConcurrentHashMap<>();
    
    /**Doesn't perform any channel initialization*/
    protected static final ChannelInitializer<Channel> NOOP_CHANNEL_INITIALIZER = new ChannelInitializer<Channel>() {
        @Override
        protected void initChannel(Channel ch) throws Exception {
        }
    };
    
    private class HeartBeatTask implements Runnable {
        
        private final AbstractNettySession session;
        
        HeartBeatTask(AbstractNettySession session) {
            this.session = Objects.requireNonNull(session, "Session can't be null");
        }
        
        @Override
        public void run() {
            try {
                sendHeartBeat(session);
            } catch (Exception e) {
                logger.error("Heartbeat not sending in session: {}", session, e);
            }
        }
    }
    
    @Override
    public AbstractNettySession getSession() {
        return mainSession;
    }
    
    @Override
    public IMessage receive(IActionContext actionContext, IMessage msg) throws InterruptedException {
        return WaitAction.waitForMessage(actionContext, msg, !msg.getMetaData().isAdmin());
    }
    
    @Override
    protected void internalInit() throws Exception {
        super.internalInit();
        this.msgFactory = Objects.requireNonNull(serviceContext.getDictionaryManager().getMessageFactory(settings.getDictionaryName()));
        this.workspaceDispatcher = Objects.requireNonNull(serviceContext.getWorkspaceDispatcher(), "'Workspace dispatcher' parameter");
        
        initService(serviceContext.getDictionaryManager(), settings);
    }
    
    @Override
    protected void internalStart() throws Exception {
        super.internalStart();
        nioEventLoopGroup = new NioEventLoopGroup();
        connect();
    }
    
    @Override
    protected void internalDispose() {
        try {
            heartBeatFuture.keySet().forEach(this::stopSendHeartBeats);
            mainSession.close();
        } catch (RuntimeException e) {
            serviceMonitor.onEvent(ServiceEventFactory.createEventError(getServiceName(), Type.DISPOSING, "Session close", "Session '" + serviceName + "'  has not been closed", e));
        } finally {
            super.internalDispose();
        }
    }
    
    @Override
    protected void disposeResources() {
        try {
            NioEventLoopGroup nioEventLoopGroup = this.nioEventLoopGroup;
            if (nioEventLoopGroup != null) {
                if (!nioEventLoopGroup.isShutdown()) {
                    try {
                        if (!nioEventLoopGroup.shutdownGracefully().awaitUninterruptibly(5, TimeUnit.SECONDS)) {
                            serviceMonitor.onEvent(ServiceEventFactory.createEventError(getServiceName(), Type.DISPOSING, "Events executor close", "Events executor '" + serviceName + "' has not been closed for 5 seconds"));
                        }
                        this.nioEventLoopGroup = null;
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
        } finally {
            super.disposeResources();
        }
    }
    
    protected abstract void sendHeartBeat(AbstractNettySession session) throws InterruptedException;
    
    /**
     * This method is called before session closing.
     * Please call super.disposeService from finally block of your implementation
     * @param session not null session
     */
    protected void disposeService(@NotNull AbstractNettySession session) {
    }
    
    protected abstract int getPort();
    
    protected abstract String getHost();
    
    protected void initChannelCloseFuture(AbstractNettySession session, Channel channel) {
        channel.closeFuture().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (!future.isSuccess()) {
                    changeStatus(ServiceStatus.ERROR, "Failed to close channel", future.cause());
                }
                stopSendHeartBeats(session);
            }
        });
    }

    protected void addRawSendHandler(ChannelPipeline pipeline) {
        NettyEmbeddedPipeline embeddedPipeline = createEmbeddedPipeline();
        if (embeddedPipeline != null) {
            loggingConfigurator.registerLogger(embeddedPipeline, getServiceName());
        }
        pipeline.addLast(new RawSendHandler(embeddedPipeline, this::acceptToSendRaw));
    }

    /**
     * Filters for decoded messages when user tries to send raw
     * @param message the decoded message
     * @return {@code true} if the message should be sent further. Otherwise, returns {@code false}
     */
    protected boolean acceptToSendRaw(IMessage message) {
        return true;
    }

    /**
     *
     * @return embedded pipeline to use in sending raw messages or {@code null} if it is not supported
     */
    @Nullable
    protected NettyEmbeddedPipeline createEmbeddedPipeline() {
        return null;
    }
    
    @NotNull
    protected abstract AbstractNettySession createSession(Channel channel);
    
    /**
     * @deprecated Please use {@link #AbstractService#internalInit()}
     */
    @Deprecated
    protected void initService(IDictionaryManager dictionaryManager, IServiceSettings settings) {
    }
    
    protected void startSendHeartBeats(AbstractNettySession session, long heartbeatInterval) {
        if (heartbeatInterval > 0) {
            heartBeatFuture.compute(session, (nettySession, future) -> {
                if (future != null) {
                    logger.error("IllegalState: heartbeat timer not stopped");
                    future.cancel(true);
                }
                return taskExecutor.addRepeatedTask(new HeartBeatTask(session), heartbeatInterval, heartbeatInterval, TimeUnit.MILLISECONDS);
            });
        }
    }
    
    protected void stopSendHeartBeats(AbstractNettySession session) {
        Future<?> future = heartBeatFuture.remove(session);
        if (future != null) {
            future.cancel(false);
        }
    }
    
    public void stop(String message, Throwable cause) {
        disposeResources();
    }
    
    public void onExceptionCaught(Channel channel, Throwable cause) {
        String message = "Exception caught in netty's pipeline in channel " + channel.id();
        changeStatus(ServiceStatus.ERROR, message, cause);
        stop(message, cause);
    }
}