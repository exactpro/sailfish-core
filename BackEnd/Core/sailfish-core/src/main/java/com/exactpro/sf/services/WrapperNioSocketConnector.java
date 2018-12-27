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
package com.exactpro.sf.services;

import java.net.SocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoProcessor;
import org.apache.mina.core.service.SimpleIoProcessorPool;
import org.apache.mina.core.session.IoSessionConfig;
import org.apache.mina.core.session.IoSessionInitializer;
import org.apache.mina.transport.socket.nio.NioProcessor;
import org.apache.mina.transport.socket.nio.NioSession;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Allows create NioSocketConnector with custom executor
 * @author nikita.smirnov
 */
public class WrapperNioSocketConnector {
	private final static Logger logger = LoggerFactory.getLogger(WrapperNioSocketConnector.class);
	private final NioSocketConnector nioSocketConnector;
	private final IoProcessor<NioSession> ioProcessor;
    private final ITaskExecutor taskExecutor;
	
	public WrapperNioSocketConnector(ITaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
		if (taskExecutor != null) {
            Executor executor = taskExecutor.getThreadPool();
			this.ioProcessor = new SimpleIoProcessorPool<NioSession>(NioProcessor.class, executor);
			this.nioSocketConnector = new NioSocketConnector(executor, ioProcessor);
		} else {
			this.ioProcessor = null;
			this.nioSocketConnector = new NioSocketConnector();
		}
	}
	
	public void setConnectTimeoutMillis(long connectTimeoutInMillis) {
		this.nioSocketConnector.setConnectTimeoutMillis(connectTimeoutInMillis);
	}
	
	public ConnectFuture connect() {
		return this.nioSocketConnector.connect();
	}
	
	public ConnectFuture connect(SocketAddress remoteAddress) {
		return this.nioSocketConnector.connect(remoteAddress);
	}
	
	public ConnectFuture connect(IoSessionInitializer<? extends ConnectFuture> sessionInitializer) {
		return this.nioSocketConnector.connect(sessionInitializer);
	}
	
	public Future<?> dispose() {
		return dispose(false);
	}
	
	public Future<?> dispose(final boolean awaitTermination) {
        if(taskExecutor != null){
            return taskExecutor.addTask(new Runnable() {
                @Override
                public void run() {
                    nioSocketConnector.dispose(awaitTermination);
                    ioProcessor.dispose();
					logger.info("disposed in thread");
                }
                
                @Override
                public String toString() {
                    return WrapperNioSocketConnector.class.getSimpleName();
                }
            });
        } else {
            nioSocketConnector.dispose(awaitTermination);
			logger.info("disposed");
			return null;
        }
	}
	
	public void setHandler(IoHandler handler) {
		this.nioSocketConnector.setHandler(handler);
	}
	
	public void setDefaultRemoteAddress(SocketAddress defaultRemoteAddress) {
		this.nioSocketConnector.setDefaultRemoteAddress(defaultRemoteAddress);
	}
	
	public IoSessionConfig getSessionConfig() {
		return this.nioSocketConnector.getSessionConfig();
	}
	
	public NioSocketConnector getNioSocketConnector() {
		return nioSocketConnector;
	}
	
	public DefaultIoFilterChainBuilder getFilterChain() {
		return this.nioSocketConnector.getFilterChain();
	}
	
	public boolean isDisposed() {
		return this.nioSocketConnector.isDisposed();
	}
	
	public boolean isActive() {
		return this.nioSocketConnector.isActive();
	}

	public IoProcessor<NioSession> getIoProcessor() {
		return ioProcessor;
	}
}
