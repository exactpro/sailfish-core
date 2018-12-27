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
import java.net.SocketAddress;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.DefaultIoFilterChain;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.service.DefaultTransportMetadata;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoProcessor;
import org.apache.mina.core.service.IoService;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.AbstractIoSession;


public class MulticastSocketSession extends AbstractIoSession 
{
	static final TransportMetadata METADATA =
        new DefaultTransportMetadata(
                "net", "datagram", true, false,
                InetSocketAddress.class,
                DefaultMulticastDatagramSessionConfig.class, IoBuffer.class);
	
	private final IoService service;
    private final MulticastDatagramSessionConfig config;
    private final IoFilterChain filterChain = new DefaultIoFilterChain(this);
    private final MulticastSocket socket;
    private final IoHandler handler;
    private final InetSocketAddress localAddress;
    private final InetSocketAddress remoteAddress;
    private final IoProcessor<MulticastSocketSession> processor;
	
	
	/**
     * Creates a new connector-side session instance.
     */
	MulticastSocketSession(IoService service,
                        MulticastSocket socket, IoProcessor<MulticastSocketSession> processor) {
        this(service, socket, processor, socket.getRemoteSocketAddress());
    }
	
	
	/**
     * Creates a new acceptor-side session instance.
     */
	MulticastSocketSession(IoService service,
					MulticastSocket socket, IoProcessor<MulticastSocketSession> processor,
                        SocketAddress remoteAddress) 
    {
		super(service);
		
        this.service = service;
        this.socket = socket;
        this.config = new MulticastDatagramSessionConfig(socket);
        this.handler = service.getHandler();
        this.processor = processor;
        this.remoteAddress = (InetSocketAddress) remoteAddress;
        this.localAddress = (InetSocketAddress) socket.getLocalSocketAddress();
    }
	

	@Override
	public IoHandler getHandler() 
	{
		return this.handler;
	}

	
	@Override
	public SocketAddress getLocalAddress() {
		return this.localAddress;
	}

	
	@Override
	public SocketAddress getRemoteAddress() {
		return this.remoteAddress;
	}


	@Override
	public TransportMetadata getTransportMetadata() 
	{
		return METADATA;
	}
	
	
	@Override
	public IoService getService() {
        return service;
    }

	
    @Override
    public IoProcessor<MulticastSocketSession> getProcessor() {
        return processor;
    }

    
    @Override
    public MulticastDatagramSessionConfig getConfig() {
        return config;
    }

    
    @Override
    public IoFilterChain getFilterChain() {
        return filterChain;
    }
    
    
    public MulticastSocket getSocket()
    {
    	return this.socket;
    }


}
