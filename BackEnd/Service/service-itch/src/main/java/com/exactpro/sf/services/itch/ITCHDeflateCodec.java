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
package com.exactpro.sf.services.itch;

import com.exactpro.sf.common.codecs.AbstractCodec;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.util.HexDumper;
import com.exactpro.sf.common.util.ICommonSettings;
import com.exactpro.sf.services.IServiceContext;
import com.exactpro.sf.services.mina.MINAUtil;
import com.jcraft.jzlib.Inflater;
import com.jcraft.jzlib.JZlib;
import com.jcraft.jzlib.ZStream;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteOrder;
import java.util.Arrays;

@SuppressWarnings("deprecation")
public class ITCHDeflateCodec extends AbstractCodec {

    private static final Logger logger = LoggerFactory.getLogger(ITCHDeflateCodec.class);

	private static final int HEADER_SIZE = 6;

	private static final int DELIMITER_LENGTH = 2;

	private Inflater inflater;

	private byte[] uncompressed;

	private byte[] delimiter;

    public ITCHDeflateCodec() {

		logger.debug("Instance created");

		this.inflater = new Inflater();

		this.uncompressed = new byte[65536];

	}

	@Override
	public void encode(IoSession session, Object message,
			ProtocolEncoderOutput out) throws Exception {

		// Compression is not required for send messages
		out.write(message);

	}

	@Override
	public void init(IServiceContext serviceContext, ICommonSettings settings,
			IMessageFactory msgFactory, IDictionaryStructure dictionary) {

        this.delimiter = ((ITCHCodecSettings) settings).getChunkDelimiter();

		logger.debug("Delimiter is {}", this.delimiter);

	}

	@Override
	protected boolean doDecode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws Exception {

		boolean debugEnabled = logger.isDebugEnabled();

		if(debugEnabled) {

			logger.debug("Decode [limit: {}; remaining: {}; buffer:\n{}]", in.limit(), in.remaining(),
			        MINAUtil.getHexdumpAdv( in, in.remaining()));
		}

		in.mark();
		in.order(ByteOrder.LITTLE_ENDIAN);

		if (in.remaining() < HEADER_SIZE) {
			in.reset();
			return false;
		}

		byte[] actualDelimiter = new byte[DELIMITER_LENGTH];

		in.get(actualDelimiter);

		if(! Arrays.equals(delimiter, actualDelimiter)) {

			logger.error("Delimiter {} does not equeals to expected {}", actualDelimiter, this.delimiter);

		}

		int expectedDecompressedLength = in.getUnsignedShort();

		int chunkLength = in.getUnsignedShort();

		if (in.remaining() < chunkLength) {
			logger.debug("Received only part of bunch");
			in.reset();
			return false;
		}

		byte [] rawContent = new byte[(int)chunkLength];

		in.get(rawContent);

		byte[] decompressed = null;

		try {

			decompressed = decompress(rawContent);

		} catch (Exception e) {

			logger.error("Input could not be decompressed", e);
			return true;

		}

		if(debugEnabled) {

			logger.debug("Decompressed:\n{};", HexDumper.getHexdump(decompressed));

		}

		if(decompressed.length != expectedDecompressedLength) {
			logger.error("Lengs of the decompressed data {} is not equals to expected length {}",decompressed.length, expectedDecompressedLength);
		}

		IoBuffer buffer = IoBuffer.allocate(decompressed.length, false);

		buffer.put(decompressed);

		buffer.flip();

		out.write(buffer);

		return true;

	}

	private byte[] decompress(byte[] compressed){

		int err;

		int comprLen = compressed.length;

		inflater.setInput(compressed);
		inflater.setOutput(uncompressed);

		err = inflater.init();
		CHECK_ERR(inflater, err, "inflateInit");

		while ( inflater.total_in < comprLen) {

			inflater.avail_in = inflater.avail_out = 1; /* force small buffers */

			err = inflater.inflate(JZlib.Z_NO_FLUSH);

			if (err == JZlib.Z_STREAM_END) {
				break;
			}

			CHECK_ERR(inflater, err, "inflate");

		}

		err = inflater.end();
		CHECK_ERR(inflater, err, "inflateEnd");

		return Arrays.copyOfRange(uncompressed, 0, (int)inflater.getTotalOut() );

	}

	private void CHECK_ERR(ZStream z, int err, String msg) {
		if (err != JZlib.Z_OK) {
			if (z.msg != null)
				logger.error(z.msg);
			logger.error("{} error: {}", msg, err);
			throw new RuntimeException("Error on decode: " + msg + " error: " + err);
		}
	}

}
