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

import org.openfast.Message;
import org.openfast.MessageBlockReader;
import org.openfast.template.type.codec.TypeCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

public class MulticastProxyInputStream extends InputStream implements MessageBlockReader {
    private final static Logger logger = LoggerFactory.getLogger(UdpInputStream.class);

    protected static final int BUFFER_SIZE = 64 * 1024;
    private ByteBuffer buffer;
    private Socket socket;
    private IPacketHandler packetHandler;
    private DataInputStream inputStream;


    public MulticastProxyInputStream(Socket socket, IPacketHandler packetHandler) throws IOException {
        this(socket, BUFFER_SIZE, packetHandler);
    }

    public MulticastProxyInputStream(Socket socket, int bufferSize, IPacketHandler packetHandler) throws IOException {
        this.packetHandler = packetHandler;
        this.socket = socket;
        this.buffer = ByteBuffer.allocate(bufferSize);
        buffer.flip();
        inputStream = new DataInputStream(socket.getInputStream());
    }

    @Override
    public int read() throws IOException {
        if (socket.isClosed())
            return -1;
        if (!buffer.hasRemaining()) {
            if (logger.isDebugEnabled()) {
                logger.debug("reading new packet");
            }
            buffer.clear();
            int msgSize = inputStream.readInt();
            byte[] msg = new byte[msgSize];
            inputStream.read(msg);
            buffer = ByteBuffer.wrap(msg);
            buffer.flip();
            if (packetHandler != null)
                packetHandler.handlePacket(buffer.array()); //Reset context
            buffer.limit(msgSize);
        }
        return (buffer.get() & 0xFF);
    }

    @Override
    public void messageRead(InputStream arg0, Message arg1) {
    }

    @Override
    public boolean readBlock(InputStream stream) {
        int n = (TypeCodec.UINT.decode(stream)).toInt();

        if (logger.isDebugEnabled()) {
            logger.debug("new block length:{}", n);
        }
        return true;
    }
}