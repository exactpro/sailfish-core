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
package com.exactpro.sf.services.soap;

import java.util.LinkedHashMap;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicReference;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.services.http.HTTPClient;
import com.exactpro.sf.services.http.ResponseInformation;
import com.exactpro.sf.services.soap.handlers.SOAPMatcherHandlerEncode;

import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.MessageToMessageEncoder;

public class SOAPClient extends HTTPClient {

    @Override
    protected void fillDecodeLayer(LinkedHashMap<String, ChannelHandler> handlers) {

        SOAPDecoder decoder = new SOAPDecoder();
        decoder.init(settings, msgFactory, dictionary, getName());
        handlers.put("decoder", decoder);
    }

    @Override
    protected void fillEncodeLayer(LinkedHashMap<String, ChannelHandler> handlers) {

        SOAPEncoder encoder = new SOAPEncoder();
        encoder.init(settings, msgFactory, dictionary, getName());
        handlers.put("encoder", encoder);
    }
    
    @Override
    protected MessageToMessageEncoder<IMessage> getEncodeMatcherHandler(Queue<ResponseInformation> queue, AtomicReference<String> cookie) {
        return new SOAPMatcherHandlerEncode(dictionary, settings, msgFactory, getName(), queue, channelBusy, cookie);
    }
    

}
