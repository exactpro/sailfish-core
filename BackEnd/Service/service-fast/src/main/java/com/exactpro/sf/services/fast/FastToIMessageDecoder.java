/******************************************************************************
 * Copyright 2009-2019 Exactpro (Exactpro Systems Limited)
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

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.services.fast.converter.ConverterException;
import com.exactpro.sf.services.fast.converter.FastToIMessageConverter;
import com.exactpro.sf.services.fast.fixup.EofCheckedStream;
import org.openfast.Context;
import org.openfast.Message;
import org.openfast.template.type.codec.TypeCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class FastToIMessageDecoder {
	static final Logger logger = LoggerFactory.getLogger(FastToIMessageDecoder.class);

    private final FastToIMessageConverter converter;
    private final int skippedInitialCount;

    public FastToIMessageDecoder(FastToIMessageConverter converter, int skippedInitialCount) {
        this.converter = converter;
        this.skippedInitialCount = skippedInitialCount;
    }

    public DecodeResult decode(byte[] data, Context context) throws IOException, ConverterException {
        if (logger.isDebugEnabled()) {
            logger.debug("try to parse data {}", Arrays.toString(data));
        }
        try (InputStream is = new EofCheckedStream(new ByteArrayInputStream(data))) {
            int msgLen = TypeCodec.UINT.decode(is).toInt();
            if (is.available() < msgLen) {
                return DecodeResult.createNotEnoughDataResult();
            }
            Message fastMessage = decodeDataToFastMessage(context, is);
            int processedBytes = data.length - is.available();
            IMessage convertedMessage = converter.convert(fastMessage);
            logger.debug("Converted message {}", convertedMessage);
            byte[] rawMessage = Arrays.copyOf(data, processedBytes);
            convertedMessage.getMetaData().setRawMessage(rawMessage);
            return DecodeResult.createSuccessResult(convertedMessage, processedBytes);
        }
    }

    private Message decodeDataToFastMessage(Context context, InputStream is) {
        FASTMessageInputStream msgStream = new FASTMessageInputStream(is, context);
        Message fastMessage = msgStream.readMessage(skippedInitialCount);
        logger.debug("Decoded fast message {}", fastMessage);
        return fastMessage;
    }
}
