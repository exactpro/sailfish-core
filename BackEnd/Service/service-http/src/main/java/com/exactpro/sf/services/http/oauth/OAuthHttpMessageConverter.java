/******************************************************************************
 * Copyright 2009-2023 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.services.http.oauth;

import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.messages.oauth.HttpHeaderMessage;
import com.exactpro.sf.messages.oauth.HttpRequestMessage;
import com.exactpro.sf.messages.oauth.HttpResponseMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMessage;

import java.util.ArrayList;
import java.util.List;

public class OAuthHttpMessageConverter {

    private final IMessageFactory factory;

    public OAuthHttpMessageConverter(IMessageFactory factory) {
        this.factory = factory;
    }

    public HttpRequestMessage fullHttpRequestToMessage(FullHttpRequest request) {
        HttpRequestMessage message = new HttpRequestMessage(factory);
        message.setMethod(request.method().name());
        message.setUri(request.uri());
        headersAsList(request).forEach(message::addHeaders);
        message.setBody(extractContent(request));
        return message;
    }

    public HttpResponseMessage fullHttpResponseToMessage(FullHttpResponse response) {
        HttpResponseMessage message = new HttpResponseMessage(factory);
        message.setStatus(response.status().code());
        headersAsList(response).forEach(message::addHeaders);
        message.setBody(extractContent(response));
        return message;
    }

    private List<HttpHeaderMessage> headersAsList(HttpMessage message) {
        return new ArrayList<HttpHeaderMessage>() {{
            message.headers().forEach(header -> {
                HttpHeaderMessage headerMessage = new HttpHeaderMessage(factory);
                headerMessage.setName(header.getKey());
                headerMessage.setValue(header.getValue());
                add(headerMessage);
            });
        }};
    }

    private String extractContent(ByteBufHolder holder) {
        ByteBuf content = holder.content();
        if(!content.isReadable()) {
            return null;
        }
        byte[] bytes = new byte[content.readableBytes()];
        content.getBytes(content.readerIndex(), bytes);
        return new String(bytes);
    }

}
