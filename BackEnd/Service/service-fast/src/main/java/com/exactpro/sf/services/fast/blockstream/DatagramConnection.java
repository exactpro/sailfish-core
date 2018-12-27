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
package com.exactpro.sf.services.fast.blockstream;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.MulticastSocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openfast.MessageBlockReader;
import org.openfast.session.Connection;

public class DatagramConnection implements Connection {
	private final static Logger logger = LoggerFactory.getLogger(DatagramConnection.class);
	private MulticastSocket socket;
	private InetAddress group;
	private UdpInputStream inputStream;

	public DatagramConnection(MulticastSocket socket, InetAddress group,
			boolean isBlockEncoded, IPacketHandler packetHandler) {
		if (isBlockEncoded) {
			this.inputStream = new BlockEncodedUdpInputStream(socket, packetHandler);
		} else {
			this.inputStream = new UdpInputStream(socket, packetHandler);
		}
		this.socket = socket;
		this.group = group;
	}

	@Override
	public void close() {
		if (socket == null) {
			return;
		}
		try {
			socket.leaveGroup(group);
		} catch (IOException e) {
			logger.debug("Failed to close socket", e);
		}
		socket.close();
	}

	@Override
	public UdpInputStream getInputStream() {
		return inputStream;
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		throw new UnsupportedOperationException(
				"Multicast sending not currently supported.");
	}

	public MessageBlockReader getBlockReader() {
		return inputStream;
	}
}