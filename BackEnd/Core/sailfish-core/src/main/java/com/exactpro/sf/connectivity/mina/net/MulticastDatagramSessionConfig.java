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

import java.net.MulticastSocket;
import java.net.SocketException;

import org.apache.mina.core.RuntimeIoException;
import org.apache.mina.core.session.IoSessionConfig;
import org.apache.mina.transport.socket.AbstractDatagramSessionConfig;

public class MulticastDatagramSessionConfig extends AbstractDatagramSessionConfig 
{
    /** The associated socket */
    private final MulticastSocket socket;

    /**
     * Creates a new instance of NioDatagramSessionConfig, associated
     * with the given DatagramChannel.
     *
     * @param channel The associated DatagramChannel
     */
    MulticastDatagramSessionConfig(MulticastSocket socket) {
        this.socket = socket;
    }
    
    
    /**
     * Get the Socket receive buffer size for this DatagramChannel.
     * 
     * @return the DatagramChannel receive buffer size.
     * @throws RuntimeIoException if the socket is closed or if we 
     * had a SocketException
     * 
     * @see DatagramSocket#getReceiveBufferSize()
     */
    public int getReceiveBufferSize() {
        try {
            return socket.getReceiveBufferSize();
        } catch (SocketException e) {
            throw new RuntimeIoException(e);
        }
    }

    /**
     * Set the Socket receive buffer size for this DatagramChannel. <br>
     * <br>
     * Note : The underlying Socket may not accept the new buffer's size.
     * The user has to check that the new value has been set. 
     * 
     * @param receiveBufferSize the DatagramChannel receive buffer size.
     * @throws RuntimeIoException if the socket is closed or if we 
     * had a SocketException
     * 
     * @see DatagramSocket#setReceiveBufferSize()
     */
    public void setReceiveBufferSize(int receiveBufferSize) {
        try {
            socket.setReceiveBufferSize(receiveBufferSize);
        } catch (SocketException e) {
            throw new RuntimeIoException(e);
        }
    }

    /**
     * Tells if SO_BROADCAST is enabled.
     * 
     * @return <code>true</code> if SO_BROADCAST is enabled
     * @throws RuntimeIoException If the socket is closed or if we get an
     * {@link SocketException} 
     */
    public boolean isBroadcast() {
        try {
            return socket.getBroadcast();
        } catch (SocketException e) {
            throw new RuntimeIoException(e);
        }
    }

    public void setBroadcast(boolean broadcast) {
        try {
            socket.setBroadcast(broadcast);
        } catch (SocketException e) {
            throw new RuntimeIoException(e);
        }
    }

    /**
     * 
     * @throws RuntimeIoException If the socket is closed or if we get an
     * {@link SocketException} 
     */
    public int getSendBufferSize() {
        try {
            return socket.getSendBufferSize();
        } catch (SocketException e) {
            throw new RuntimeIoException(e);
        }
    }

    /**
     * 
     * @throws RuntimeIoException If the socket is closed or if we get an
     * {@link SocketException} 
     */
    public void setSendBufferSize(int sendBufferSize) {
        try {
            socket.setSendBufferSize(sendBufferSize);
        } catch (SocketException e) {
            throw new RuntimeIoException(e);
        }
    }

    /**
     * Tells if SO_REUSEADDR is enabled.
     * 
     * @return <code>true</code> if SO_REUSEADDR is enabled
     * @throws RuntimeIoException If the socket is closed or if we get an
     * {@link SocketException} 
     */
    public boolean isReuseAddress() {
        try {
            return socket.getReuseAddress();
        } catch (SocketException e) {
            throw new RuntimeIoException(e);
        }
    }

    /**
     * 
     * @throws RuntimeIoException If the socket is closed or if we get an
     * {@link SocketException} 
     */
    public void setReuseAddress(boolean reuseAddress) {
        try {
            socket.setReuseAddress(reuseAddress);
        } catch (SocketException e) {
            throw new RuntimeIoException(e);
        }
    }

    /**
     * Get the current Traffic Class for this Socket, if any. As this is
     * not a mandatory feature, the returned value should be considered as 
     * a hint. 
     * 
     * @return The Traffic Class supported by this Socket
     * @throws RuntimeIoException If the socket is closed or if we get an
     * {@link SocketException} 
     */
    public int getTrafficClass() {
        try {
            return socket.getTrafficClass();
        } catch (SocketException e) {
            throw new RuntimeIoException(e);
        }
    }

    /**
     * {@inheritDoc}
     * @throws RuntimeIoException If the socket is closed or if we get an
     * {@link SocketException} 
     */
    public void setTrafficClass(int trafficClass) {
        try {
            socket.setTrafficClass(trafficClass);
        } catch (SocketException e) {
            throw new RuntimeIoException(e);
        }
    }
    
    
    public int getSoTimeout() 
    {
    	try {
            return socket.getSoTimeout();
        } catch (SocketException e) {
            throw new RuntimeIoException(e);
        }
	}

	
	public void setSoTimeout(int soTimeout) {
		try {
            socket.setSoTimeout(soTimeout);
        } catch (SocketException e) {
            throw new RuntimeIoException(e);
        }
	}

    @Override
    public void setAll(IoSessionConfig config) {
        super.setAll(config);
        if (config instanceof DefaultMulticastDatagramSessionConfig) {
            setSoTimeout(((DefaultMulticastDatagramSessionConfig)config).getSoTimeout());
        }
    }
}
