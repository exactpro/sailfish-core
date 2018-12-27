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
import java.net.DatagramSocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openfast.IntegerValue;
import org.openfast.ScalarValue;
import org.openfast.template.LongValue;
import org.openfast.template.type.codec.TypeCodec;

public class BlockEncodedUdpInputStream extends UdpInputStream {
	private final static Logger logger = LoggerFactory.getLogger(BlockEncodedUdpInputStream.class);

	private long remainingBlockLen = 0;

	public BlockEncodedUdpInputStream(DatagramSocket socket) {
		this(socket, null);
	}

	public BlockEncodedUdpInputStream(DatagramSocket socket, IPacketHandler packetHandler) {
		this(socket, BUFFER_SIZE, packetHandler);
	}


	public BlockEncodedUdpInputStream(DatagramSocket socket, int bufferSize, IPacketHandler packetHandler) {
		super(socket, bufferSize, packetHandler);
	}

	@Override
	public int read() throws IOException {
		if (remainingBlockLen == 0) {
			logger.debug("Read after end of block.");
			throw new FramingErrorException("Read after end of block.");
		}
		remainingBlockLen--;
		int res = super.read();
        if(logger.isTraceEnabled()) {
            logger.trace("Reading int:{}", Integer.toHexString(res));
        }
		return res;
	}

	@Override
	public void clearBuffer() {
		super.clearBuffer();
		remainingBlockLen = 0;
	}

	@Override
	public boolean readBlock(InputStream stream) {
		if (remainingBlockLen == 0) {
			logger.debug("reading new block");
			super.readBlock(stream);
			remainingBlockLen = 65536;
            ScalarValue value = TypeCodec.UINT.decode(stream);
            if(value instanceof IntegerValue){
                this.remainingBlockLen = ((IntegerValue) value).value;
            } else if(value instanceof LongValue){
                this.remainingBlockLen = ((LongValue) value).value;
            }
			logger.debug("new block length:{}", remainingBlockLen);
			return true;
		}
		logger.debug("reading new message in previous block (block remaining length:{}", remainingBlockLen);
		return true;
	}
}
