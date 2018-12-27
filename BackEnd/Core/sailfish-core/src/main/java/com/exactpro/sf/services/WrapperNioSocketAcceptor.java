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

import java.io.IOException;
import java.net.SocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoProcessor;
import org.apache.mina.core.service.SimpleIoProcessorPool;
import org.apache.mina.core.session.IoSessionConfig;
import org.apache.mina.transport.socket.nio.NioProcessor;
import org.apache.mina.transport.socket.nio.NioSession;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Allows create NioSocketConnector with custom executor
 * @author nikita.smirnov
 */
public class WrapperNioSocketAcceptor {
    private final static Logger logger = LoggerFactory.getLogger(WrapperNioSocketAcceptor.class);
	private final NioSocketAcceptor nioSocketAcceptor;
	private final IoProcessor<NioSession> ioProcessor;
    private final ITaskExecutor taskExecutor;
	
	public WrapperNioSocketAcceptor(ITaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
        if (taskExecutor != null) {
            Executor executor = taskExecutor.getThreadPool();
			this.ioProcessor = new SimpleIoProcessorPool<NioSession>(NioProcessor.class, executor);
			this.nioSocketAcceptor = new NioSocketAcceptor(executor, ioProcessor);
		} else {
			this.ioProcessor = null;
			this.nioSocketAcceptor = new NioSocketAcceptor();
		}
	}
	
	public void bind() throws IOException {
		this.nioSocketAcceptor.bind();
	}
	
	public void bind(Iterable<? extends SocketAddress> localAddresses) throws IOException {
		this.nioSocketAcceptor.bind(localAddresses);
	}
	
	public void bind(SocketAddress localAddress) throws IOException {
		this.nioSocketAcceptor.bind(localAddress);
	}
	
	public void unbind() {
		this.nioSocketAcceptor.unbind();
	}
	
	public void unbind(SocketAddress localAddress) {
		this.nioSocketAcceptor.unbind(localAddress);
	}
	
	public Future<?> dispose() {
		return dispose(false);
	}
	
	public Future<?> dispose(final boolean awaitTermination) {
        if(taskExecutor != null){
            return taskExecutor.addTask(new Runnable() {
                @Override
                public void run() {
                    nioSocketAcceptor.dispose(awaitTermination);
                    ioProcessor.dispose();
                    logger.info("disposed in thread");
                }
                
                @Override
                public String toString() {
                    return WrapperNioSocketAcceptor.class.getSimpleName();
                }
            });
        } else {
            nioSocketAcceptor.dispose(awaitTermination);
            logger.info("disposed");
			return null;
        }
	}
	
	public void setCloseOnDeactivation(boolean disconnectClientsOnUnbind) {
		this.nioSocketAcceptor.setCloseOnDeactivation(disconnectClientsOnUnbind);
	}
	
	public void setHandler(IoHandler handler) {
		this.nioSocketAcceptor.setHandler(handler);
	}
	
	public void setReuseAddress(boolean reuseAddress) {
		this.nioSocketAcceptor.setReuseAddress(reuseAddress);
	}
	
	public void setDefaultLocalAddress(Iterable<? extends SocketAddress> localAddress) {
		this.nioSocketAcceptor.setDefaultLocalAddresses(localAddress);
	}
	
	public void setDefaultLocalAddress(SocketAddress localAddress) {
		this.nioSocketAcceptor.setDefaultLocalAddress(localAddress);
	}
	
	public IoSessionConfig getSessionConfig() {
		return this.nioSocketAcceptor.getSessionConfig();
	}
	
	public NioSocketAcceptor getNioSocketAcceptor() {
		return nioSocketAcceptor;
	}
	
	public DefaultIoFilterChainBuilder getFilterChain() {
		return this.nioSocketAcceptor.getFilterChain();
	}
	
	public boolean isDisposed() {
		return this.nioSocketAcceptor.isDisposed();
	}
	
	public boolean isActive() {
		return this.nioSocketAcceptor.isActive();
	}
	
}
