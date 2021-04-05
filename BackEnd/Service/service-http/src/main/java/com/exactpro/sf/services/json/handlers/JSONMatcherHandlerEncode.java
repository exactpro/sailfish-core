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
package com.exactpro.sf.services.json.handlers;

import java.net.URI;
import java.util.Queue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.util.ICommonSettings;
import com.exactpro.sf.services.http.ResponseInformation;
import com.exactpro.sf.services.http.handlers.BaseHTTPMatcherHandlerEncode;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpHeaders.Names;

public class JSONMatcherHandlerEncode extends BaseHTTPMatcherHandlerEncode {

    public JSONMatcherHandlerEncode(IDictionaryStructure dictionary, ICommonSettings settings, IMessageFactory msgFactory, String clientName,
                                    Queue<ResponseInformation> queue, Semaphore channelBusyLock, AtomicReference<String> cookie) {
        super(dictionary, settings, msgFactory, clientName, queue, channelBusyLock, cookie);
    }

    @Override
    protected FullHttpRequest createHttpRequest(IMessageStructure messageStructure, IMessage message, URI endPoint) {
        FullHttpRequest req = super.createHttpRequest(messageStructure, message, endPoint);
        req.headers().set(Names.CONTENT_TYPE, "application/json");
        return req;
    }
}
