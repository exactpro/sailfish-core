/******************************************************************************
 * Copyright 2009-2022 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.services.http.handlers;

import java.util.List;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.services.http.HTTPMessageHelper;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpHeaders.Names;

/**
 * @author sergey.smirnov
 *
 */
public class MatcherHandlerUtil {
    
    public static IMessage extractHTTPHeader(HttpHeaders headers, IMessageFactory msgFactory, String dictionaryNamespace) {

        IMessage httpHeader = msgFactory.createMessage(HTTPMessageHelper.HTTPHEADER, dictionaryNamespace);
        for (String headerName : headers.names()) {
            List<String> headerValues = headers.getAll(headerName);
            if (!headerValues.isEmpty()) {
                httpHeader.addField(headerName, String.join(", ", headerValues));
            }
        }

        return httpHeader;
    }
    
    public static String getEncodingString(IMessage msg) {
        IMessage headers = msg.getField(HTTPMessageHelper.HTTPHEADER);
        return headers == null ? null : headers.getField(Names.CONTENT_ENCODING);
    }

}
