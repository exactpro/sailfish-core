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
package com.exactpro.sf.services.fast;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.openfast.Context;
import org.openfast.Message;
import org.openfast.MessageBlockReader;
import org.openfast.session.FastConnectionException;

import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.services.IServiceContext;
import com.exactpro.sf.services.IServiceHandler;
import com.exactpro.sf.services.IServiceMonitor;
import com.exactpro.sf.services.IServiceSettings;
import com.exactpro.sf.services.ServiceStatus;
import com.exactpro.sf.services.fast.blockstream.DatagramConnection;
import com.exactpro.sf.services.fast.blockstream.IPacketHandler;
import com.exactpro.sf.services.fast.converter.FastToIMessageConverter;

public class FASTUdpClient extends FASTAbstractClient {

	private boolean resetContext;

	private DatagramConnection datagramConnection;
	
	@Override
	public void init(
			IServiceContext serviceContext,
			final IServiceMonitor serviceMonitor,
			final IServiceHandler handler,
			final IServiceSettings settings,
			final ServiceName name) {
		super.init(serviceContext, serviceMonitor, handler, settings, name);
		this.resetContext = getSettings().isResetContextAfterEachUdpPacket();
	}

	public FASTUdpClient() {
		changeStatus(ServiceStatus.CREATED, "Service created", null);
	}


	@Override
	protected DatagramConnection getConnection(String remoteAddr, int port,
			String interfaceAddress) throws FastConnectionException {

		try {
			NetworkInterface netIface = null;
			if (interfaceAddress != null && !interfaceAddress.equals("")){
				if (interfaceAddress.matches("([0-9]{1,3}\\.){3}[0-9]{1,3}")) {
					netIface = NetworkInterface.getByInetAddress(InetAddress.getByName(interfaceAddress));
				} else {
					netIface = NetworkInterface.getByName(interfaceAddress);
				}
			}

			MulticastSocket socket;
//			if (interfaceAddress == null || interfaceAddress.equals("")) {
				socket = new MulticastSocket(port);
//			} else {
//				SocketAddress saddr = new InetSocketAddress(interfaceAddress, port);
//				socket = new MulticastSocket(saddr);
//			}

			InetAddress groupAddress = InetAddress.getByName(remoteAddr);

			if (netIface == null){
				socket.joinGroup(groupAddress);
			} else {
				socket.joinGroup(
						new InetSocketAddress(groupAddress,port),
						netIface
				);
			}

			this.datagramConnection = new DatagramConnection(
					socket,
					groupAddress,
					getSettings().isStreamBlockEncoded(),
					new IPacketHandler() {

						@Override
						public void handlePacket(byte[] packetData) {
							if (resetContext) {
								getReceiveContext().reset();
							}
						}
					}
			);
			return this.datagramConnection;
		} catch (IOException e) {
			FastConnectionException expt = new FastConnectionException(
					"Failed to create connection"
			);
			expt.initCause(e);
			throw expt;
		}
	}

	@Override
	protected void send(Object message) {
		throw new EPSCommonException("Can not send message " + message +
				" from udp client connection"
		);
	}


	@Override
	protected void doStart() {
		initConnection();
	}


	@Override
	protected synchronized void closeSession() {
		if (connection != null)
			connection.close();
		connection = null;
	}


	@Override
	protected MessageBlockReader getBlockReader() {
		return datagramConnection.getBlockReader();
	}

	@Override
	protected boolean recoverFromInputError(InputStream underlyingStream) {
		this.datagramConnection.getInputStream().clearBuffer();
		return true;
	}

	@Override
	protected void handleReceivedMessage(
			Message fastMessage,
			FastToIMessageConverter converter,
			byte[] rawMessage) {
//		if (resetContext) {
//			getReceiveContext().reset();
//		}
		super.handleReceivedMessage(fastMessage, converter, rawMessage);
	}


	private Context getReceiveContext() {
		return msgInStream.getContext();
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this).append("name", serviceName).toString();
	}

	@Override
	public void connect() throws Exception
	{
	}
}
