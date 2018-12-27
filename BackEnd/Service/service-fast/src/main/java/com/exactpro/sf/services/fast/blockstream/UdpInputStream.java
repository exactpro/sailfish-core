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
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openfast.Message;
import org.openfast.MessageBlockReader;

public class UdpInputStream extends InputStream implements MessageBlockReader {
	private final static Logger logger = LoggerFactory.getLogger(UdpInputStream.class);

	protected static final int BUFFER_SIZE = 64 * 1024;
	private DatagramSocket socket;
	private ByteBuffer buffer;
	private boolean canReceivePacket;

	private IPacketHandler packetHandler;

	public UdpInputStream(DatagramSocket socket) {
		this(socket, BUFFER_SIZE, null);
	}

	public UdpInputStream(DatagramSocket socket, IPacketHandler packetHandler) {
		this(socket, BUFFER_SIZE, packetHandler);
	}

	public UdpInputStream(DatagramSocket socket, int bufferSize, IPacketHandler packetHandler) {
		this.packetHandler = packetHandler;
		this.socket = socket;
		this.buffer = ByteBuffer.allocate(bufferSize);
		buffer.flip();
	}

	@Override
	public int read() throws IOException {
		if (socket.isClosed())
			return -1;
		if (!buffer.hasRemaining()) {
			if (!canReceivePacket) {
				logger.debug("no data left in the buffer.");
				throw new FramingErrorException("Data is misaligned.");
			}
			buffer.clear();
			logger.debug("reading new packet");
			DatagramPacket packet = new DatagramPacket(buffer.array(),
					buffer.capacity());
			socket.receive(packet);
			buffer.flip();
			if (packetHandler != null)
				packetHandler.handlePacket(buffer.array()); //Reset context
			buffer.limit(packet.getLength());
		}
		canReceivePacket = false;
		return (buffer.get() & 0xFF);
	}

	public void clearBuffer() {
		logger.debug("clear buffer called");
		buffer.limit(0);
	}

	@Override
	public void messageRead(InputStream arg0, Message arg1) {
	}

	@Override
	public boolean readBlock(InputStream stream) {
		logger.debug("reading new packet allowed");
		this.canReceivePacket = true;
		return true;
	}
}
