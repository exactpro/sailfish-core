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

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class OAuthHttpRequestBuilder {
    private static final Logger logger = LoggerFactory.getLogger(OAuthHttpRequestBuilder.class);

    public static final String GRANT_TYPE_CLIENT_CREDENTIALS = "client_credentials";
    public static final String GRANT_TYPE_PASSWORD = "password";

    private static final String ENCODING_UTF_8 = "UTF-8";
    private static final String GRANT_TYPE_KEY_NAME = "grant_type";
    private static final String USERNAME_KEY_NAME = "username";
    private static final String PASSWORD_KEY_NAME = "password";
    private static final String APPLICATION_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";

    public static FullHttpRequest clientCredentialsRequest(URI uri, String username, String password) {
        return clientCredentialsRequest(uri, username, password, false);
    }

    public static FullHttpRequest clientCredentialsRequest(
            URI uri, String username, String password, boolean usePasswordGrantType
    ) {
        FullHttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.POST, uri.getRawPath()
        );
        ByteBuf buf = request.content().clear();
        String params = formatParameters(new HashMap<String, String>() {{
            put(USERNAME_KEY_NAME, username);
            put(PASSWORD_KEY_NAME, password);
            if(usePasswordGrantType) {
                put(GRANT_TYPE_KEY_NAME, GRANT_TYPE_PASSWORD);
            } else {
                put(GRANT_TYPE_KEY_NAME, GRANT_TYPE_CLIENT_CREDENTIALS);
            }
        }});
        buf.writeBytes(params.getBytes());
        request.headers()
                .set(HttpHeaderNames.HOST, uri.getHost())
                .set(HttpHeaderNames.CONNECTION, "close")
                .set(HttpHeaderNames.CONTENT_TYPE, APPLICATION_X_WWW_FORM_URLENCODED)
                .set(HttpHeaderNames.CONTENT_LENGTH, buf.readableBytes());
        return request;
    }

    private static String formatParameters(Map<String, String> params) {
        return params.entrySet().stream().map(entry -> {
                    try {
                        return entry.getKey() + "=" + URLEncoder.encode(entry.getValue(), ENCODING_UTF_8);
                    } catch (UnsupportedEncodingException e) {
                        logger.warn("The {} value by the {} key can't be encoded", entry.getValue(), entry.getKey(), e);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.joining("&"));
    }

}
