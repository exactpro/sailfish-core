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
package com.exactpro.sf.connectivity.mina.net;

import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.SystemUtils;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.DefaultConnectFuture;
import org.apache.mina.core.service.AbstractIoConnector;
import org.apache.mina.core.service.IoProcessor;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.IdleStatusChecker;
import org.apache.mina.core.session.IoSessionConfig;
import org.apache.mina.core.session.IoSessionInitializer;
import org.apache.mina.util.ExceptionMonitor;

public class MulticastSocketConnector extends AbstractIoConnector 
{
	// object used for checking session idle
    private IdleStatusChecker idleChecker;
    
    private final IoProcessor<MulticastSocketSession> processor;
    private final boolean createdProcessor;
    
    private final Executor executor;
    private final boolean createdExecutor;
    
    private NetworkInterface networkInterface;
	
	/**
     * Creates a new instance.
     */
    public MulticastSocketConnector() {
        this(null);
    }
    
    
    /**
     * Creates a new instance.
     */
    public MulticastSocketConnector(Executor executor) {
        super(new DefaultMulticastDatagramSessionConfig(), executor);
        
        idleChecker = new IdleStatusChecker();
        // we schedule the idle status checking task in this service executor
        // it will be woke up every seconds
        executeWorker(idleChecker.getNotifyingTask(), "idleStatusChecker");

        if ( executor == null ) 
        {
            this.executor = Executors.newCachedThreadPool();
            createdExecutor = true;
        } else {
            this.executor = executor;
            createdExecutor = false;
        }
        
        this.processor = new MulticastSocketProcessor(this.executor);
        this.createdProcessor = true;
    }
	
	
	@Override
	protected ConnectFuture connect0(SocketAddress remoteAddress,
			SocketAddress localAddress,
			IoSessionInitializer<? extends ConnectFuture> sessionInitializer) 
	{
		MulticastSocket socket = null;
        boolean success = false;
        try 
        {
        	InetSocketAddress address = (InetSocketAddress)remoteAddress;
        	
        	if (SystemUtils.IS_OS_WINDOWS) {
        		socket = new MulticastSocket(address.getPort());
        	} else {
        		socket = new MulticastSocket(address);
        	}
        	
        	if (networkInterface != null)
        		socket.setInterface(networkInterface.getInterfaceAddresses().get(0).getAddress());
        	
        	socket.joinGroup(address.getAddress());
        	
            ConnectFuture future = new DefaultConnectFuture();
            MulticastSocketSession session = new MulticastSocketSession(this, socket, this.processor);
            session.getConfig().setAll(getSessionConfig());
            
            initSession(session, future, sessionInitializer);
            
            // Forward the remaining process to the IoProcessor.
            session.getProcessor().add(session);
            success = true;
            
            idleChecker.addSession(session);
            
            return future;
            
        } catch ( Exception e ) 
        {
            return DefaultConnectFuture.newFailedFuture(e);
        } 
        finally {
            if (!success && socket != null) {
                try {
                    close(socket);
                } catch (Exception e) {
                    ExceptionMonitor.getInstance().exceptionCaught(e);
                }
            }
        }
	}
	
	
	private void close(MulticastSocket socket) throws Exception
	{
	    socket.close();
	}
	
	
	@Override
	protected void dispose0() throws Exception 
	{
		if ( isDisposing() ) 
		{
			if ( createdProcessor ) 
			{
				// stop the idle checking task
				idleChecker.getNotifyingTask().cancel();

				processor.dispose();
			}
		}
		
		if ( createdExecutor ) {
            ExecutorService e = (ExecutorService) executor;
            e.shutdown();
            while (!e.isTerminated()) {
                try {
                    e.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
                } catch (InterruptedException e1) {
                    // Ignore; it should end shortly.
                }
            }
        }
	}
	
	
	@Override
	public TransportMetadata getTransportMetadata() {
		return MulticastSocketSession.METADATA;
	}

    @Override
    public IoSessionConfig getSessionConfig() {
        return sessionConfig;
    }


    public NetworkInterface getNetworkInterface() {
		return networkInterface;
	}


	public void setNetworkInterface(NetworkInterface networkInterface) {
		this.networkInterface = networkInterface;
	}
	
}
