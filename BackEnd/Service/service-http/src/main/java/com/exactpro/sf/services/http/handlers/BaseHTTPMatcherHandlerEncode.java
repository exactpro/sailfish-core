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
package com.exactpro.sf.services.http.handlers;

import static com.exactpro.sf.common.messages.MessagePropertiesConstants.MESSAGE_TYPE_PROPERTY;
import static com.exactpro.sf.common.messages.MessagePropertiesConstants.PROTOCOL_PROPERTY;
import static com.exactpro.sf.common.messages.MetadataExtensions.getMessageProperties;
import static com.exactpro.sf.common.messages.MetadataExtensions.setMessageProperties;
import static com.exactpro.sf.common.messages.structures.StructureUtils.getAttributeValue;
import static com.exactpro.sf.services.http.HTTPClient.REQUEST_ID_PROPERTY;
import static com.exactpro.sf.services.http.HTTPClient.REQUEST_REFERENCE_PROPERTY;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.TextStringBuilder;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.messages.structures.StructureUtils;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.common.util.ICommonSettings;
import com.exactpro.sf.services.http.DecodingContentHelper;
import com.exactpro.sf.services.http.HTTPClient;
import com.exactpro.sf.services.http.HTTPClientSettings;
import com.exactpro.sf.services.http.HTTPMessageHelper;
import com.exactpro.sf.services.http.ResponseInformation;
import com.exactpro.sf.util.DateTimeUtility;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.base64.Base64;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;

/**
 * @author sergey.smirnov
 *
 */
public class BaseHTTPMatcherHandlerEncode extends MessageToMessageEncoder<IMessage> {


    private static final Logger logger = LoggerFactory.getLogger(BaseHTTPMatcherHandlerEncode.class);

    protected final IDictionaryStructure dictionary;
    protected final HTTPClientSettings settings;
    protected final IMessageFactory msgFactory;
    protected final String clientName;
    protected final Semaphore channelBusy;
    private final Queue<ResponseInformation> queue;
    private final String authString;
    private final HttpMethod defaultMethod;
    private final Map<String, String> customHeaders;
    private final AtomicReference<String> cookie;

    private final Cache<String, List<String>> requiredParametersCache = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .maximumSize(128)
            .build();

    /**
     * @param dictionary
     * @param settings
     * @param msgFactory
     * @param channelBusy
     * @param queue used to pass the information about next expected response to the decoder
     * @param cookie
     */
    public BaseHTTPMatcherHandlerEncode(IDictionaryStructure dictionary, ICommonSettings settings, IMessageFactory msgFactory, String clientName,
                                        Queue<ResponseInformation> queue, HttpMethod defaultHTTPMethod, Semaphore channelBusy, AtomicReference<String> cookie) {
        this.dictionary = dictionary;
        this.settings = (HTTPClientSettings)settings;
        this.msgFactory = msgFactory;
        this.clientName = clientName;
        this.authString = generateAuthString(this.settings.getUserName(), this.settings.getPassword());
        this.queue = queue;
        this.defaultMethod = defaultHTTPMethod;
        this.customHeaders = new HashMap<>();
        this.channelBusy = channelBusy;
        this.cookie = cookie;

        if(StringUtils.isNotBlank(this.settings.getCustomHeaders())) {
            for(String header : this.settings.getCustomHeaders().split(";")) {
                if(StringUtils.isNotBlank(header)) {
                    // The header's value may contain `=` symbol too.
                    // So we should split key and value by the first `=`
                    String[] keyValue = header.split("=", 2);

                    String key = keyValue[0].trim();
                    String value = keyValue[1].trim();

                    if(key.isEmpty() || value.isEmpty()) {
                        throw new EPSCommonException("Invalid key-value pair: " + header);
                    }

                    customHeaders.put(key, value);
                }
            }
        }
    }

    public BaseHTTPMatcherHandlerEncode(IDictionaryStructure dictionary, ICommonSettings settings, IMessageFactory msgFactory, String clientName,
            Queue<ResponseInformation> queue, Semaphore channelBusyLock, AtomicReference<String> cookie) {
        this(dictionary, settings, msgFactory, clientName, queue, HttpMethod.GET, channelBusyLock, cookie);
    }

    protected FullHttpRequest createHttpRequest(IMessageStructure messageStructure, IMessage message, URI endPoint) {
        HttpMethod httpMethod = detectHttpMethod(messageStructure, defaultMethod);
        IMessage uriParams  = message.getField(HTTPMessageHelper.REQUEST_URI_ATTRIBUTE);
        String httpUrl = buildPath(messageStructure, uriParams, endPoint);
        logger.debug("{} sent to {}", message.getName(), httpUrl);

        FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, httpMethod, httpUrl);
        request.headers().set(HttpHeaderNames.HOST, endPoint.getHost());

        String cookieValue = cookie.get();
        if(cookieValue != null) {
            request.headers().add(HttpHeaderNames.COOKIE, cookieValue);
        }

        if (authString != null) {
            request.headers().add(HttpHeaderNames.AUTHORIZATION, authString);
        }

        int contentLength = message.getMetaData().getRawMessage().length;
        request.headers().add(HttpHeaderNames.CONTENT_LENGTH, contentLength);

        StringBuilder encoding = new StringBuilder();

        //TODO use nettyy HttpContentDecompressor/Compressor
        for (DecodingContentHelper ch : DecodingContentHelper.values()) {
            encoding.append(ch.getName());
            encoding.append(", ");
        }

        encoding.delete(encoding.length()-2, encoding.length());
        request.headers().add(HttpHeaderNames.ACCEPT_ENCODING, encoding.toString());

        for(Entry<String, String> e : customHeaders.entrySet()) {
            request.headers().add(e.getKey(), e.getValue());
        }

        return request;
    }

    @Nullable
    protected String generateAuthString(String username, String password) {
        if (StringUtils.isNoneBlank(username, password)) {
            ByteBuf byteBuf = Unpooled.copiedBuffer(username + ':' + password, StandardCharsets.UTF_8);
            return "Basic " + Base64.encode(byteBuf).toString(StandardCharsets.UTF_8);
        }

        return null;
    }

    @Override
    protected void encode(ChannelHandlerContext handlerContext, IMessage message, List<Object> out) throws Exception {

        String messageType = message.getName();
        IMessageStructure messageStructure = dictionary.getMessages().get(messageType);
        String responseName = getAttributeValue(messageStructure, HTTPMessageHelper.REQUEST_RESPONSE_ATTRIBUTE);
        URI endPoint = new URI(settings.getURI());

        FullHttpRequest request = createHttpRequest(messageStructure, message, endPoint);

        if (message.getMetaData().getRawMessage() != null) {
            request.content().writeBytes(message.getMetaData().getRawMessage());
        }

        message.addField(HTTPMessageHelper.HTTPHEADER, MatcherHandlerUtil.extractHTTPHeader(request.headers(), msgFactory, dictionary.getNamespace()));

        Map<String, String> requestProperties = defaultIfNull(getMessageProperties(message.getMetaData()), new HashMap<>());
        requestProperties.put(MESSAGE_TYPE_PROPERTY, messageType);
        requestProperties.put(PROTOCOL_PROPERTY, HTTPClient.PROTOCOL_TYPE);
        setMessageProperties(message.getMetaData(), requestProperties);

        Map<String, String> responseProperties = Collections.singletonMap(REQUEST_REFERENCE_PROPERTY,
                requestProperties.getOrDefault(REQUEST_ID_PROPERTY, Long.toString(message.getMetaData().getId())));

        queue.offer(new ResponseInformation(responseName, responseProperties));

        out.add(message);
        out.add(request);
    }

    private String buildPath(IMessageStructure messageStructure, IMessage message, URI endPoint) {
        if(messageStructure.getAttributes().keySet().contains(HTTPMessageHelper.REQUEST_URI_ATTRIBUTE)) {
            TextStringBuilder pathBuilder = new TextStringBuilder();
            pathBuilder.append(StructureUtils.<String>getAttributeValue(messageStructure, HTTPMessageHelper.REQUEST_URI_ATTRIBUTE));
            IFieldStructure uriMessageStructure = messageStructure.getFields().get(HTTPMessageHelper.REQUEST_URI_ATTRIBUTE);
            
            if(uriMessageStructure == null || message == null) {
                return pathBuilder.toString();
            }

            List<String> requiredParamsNames = getRequiredPathQueryParams(pathBuilder.toString());

            for(IFieldStructure entry : uriMessageStructure.getFields().values()) {
                String paramName = entry.getName();
                Object instanceValue = message.getField(paramName);
                if (instanceValue != null) {
                    String value;
                    if (instanceValue instanceof LocalDateTime) {
                        String format = StringUtils.defaultIfEmpty(getAttributeValue(entry, "dateFormat"), "yyyy-MM-dd");
                        value = DateTimeUtility.createFormatter(format).format(message.<LocalDateTime>getField(paramName));
                    } else {
                        value = message.getField(paramName).toString();
                    }
                    pathBuilder.replaceAll("{" + paramName + "}", escapeQueryParameter(value));
                    requiredParamsNames.remove(paramName);
                }
            }

            if (!requiredParamsNames.isEmpty()) {
                throw new EPSCommonException("Required parameters " + requiredParamsNames + " doesnt filled at path or query parameters of http request");
            }

            return pathBuilder.toString().replaceAll("\\[[^\\]]*?[{}].*?\\]", "").replaceAll("\\[(.*?)\\]", "$1");
        }

        return endPoint.getRawPath();
    }

    private String escapeQueryParameter(String param) {
        try {
            return URLEncoder.encode(param, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("invalid param value " + param, e);
        }
    }

    private HttpMethod detectHttpMethod(IMessageStructure messageStructure, HttpMethod defaultMethod) {
        if(messageStructure.getAttributes().keySet().contains(HTTPMessageHelper.REQUEST_METHOD_ATTRIBUTE)) {
            return HttpMethod.valueOf(getAttributeValue(messageStructure, HTTPMessageHelper.REQUEST_METHOD_ATTRIBUTE));
        }

        return defaultMethod;
    }

    private List<String> getRequiredPathQueryParams(String params) {

        List<String> requiredParams = requiredParametersCache.getIfPresent(params);

        if (requiredParams != null) {
            return requiredParams;
        } else {
            requiredParams = new ArrayList<>();
        }

        //drop all optional params
        params = params.replaceAll("\\[.*[{}].*\\]", "").replaceAll("\\[(.*?)\\]", "");
        Matcher matcher = Pattern.compile("\\{[^{}]+\\}").matcher(params);
        while (matcher.find()) {
            String group = matcher.group();
            group = group.substring(1, group.length() - 1);
            requiredParams.add(group);
        }

        requiredParametersCache.put(params, requiredParams);
        return requiredParams;
    }

}
