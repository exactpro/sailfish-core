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

import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openfast.Message;
import org.openfast.MessageBlockReader;
import org.openfast.template.type.codec.TypeCodec;

public class StreamBlockLengthReader implements MessageBlockReader {
	private final static Logger logger = LoggerFactory.getLogger(StreamBlockLengthReader.class);

	@Override
	public void messageRead(InputStream arg0, Message arg1) {
	}

	@Override
	public boolean readBlock(InputStream stream) {
		int n = (TypeCodec.UINT.decode(stream)).toInt();
		logger.trace("Read message length:{}", n);
		return true;
	}

}
