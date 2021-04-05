/******************************************************************************
 * Copyright 2009-2021 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.services.http;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.util.ICommonSettings;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

public abstract class AbstractHTTPDecoder extends MessageToMessageDecoder<IMessage> {
    
    private static final Logger logger = LoggerFactory.getLogger(AbstractHTTPDecoder.class);
    
    public abstract void init(ICommonSettings settings, IMessageFactory msgFactory, IDictionaryStructure dictionary, String clientName);
    
    @Override
    public void exceptionCaught(ChannelHandlerContext handlerContext, Throwable cause) throws Exception {
        logger.error("Decode problem", cause);
    }
    
    public static InputStream handleContentEncoding(String contentEncoding, byte[] content) throws IOException {
        
        InputStream base = new ByteArrayInputStream(content); 

        if (contentEncoding != null) {
            for (DecodingContentHelper ch : DecodingContentHelper.values()) {
                if (ch.getName().equals(contentEncoding)) {
                    return ch.getStream(base);
                }
            }
            logger.warn("Decoder for '{}' isn't found", contentEncoding);
        }

        return base;
    }
}
