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

import static com.exactpro.sf.common.messages.MessagePropertiesConstants.MESSAGE_TYPE_PROPERTY;
import static com.exactpro.sf.common.messages.MessagePropertiesConstants.PROTOCOL_PROPERTY;
import static com.exactpro.sf.common.messages.MetadataExtensions.setMessageProperties;
import static com.exactpro.sf.common.messages.structures.StructureUtils.getAttributeValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

import org.jetbrains.annotations.NotNull;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.MsgMetaData;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.util.ICommonSettings;
import com.exactpro.sf.services.http.HTTPClient;
import com.exactpro.sf.services.http.HTTPClientSettings;
import com.exactpro.sf.services.http.HTTPMessageHelper;
import com.exactpro.sf.services.http.ResponseInformation;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author sergey.smirnov
 *
 */
public class BaseHTTPMatcherHandlerDecode extends MessageToMessageDecoder<FullHttpResponse> {

    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName() + "@" + Integer.toHexString(hashCode()));

    protected final IDictionaryStructure dictionary;
    protected final HTTPClientSettings settings;
    protected final IMessageFactory msgFactory;
    protected final String clientName;
    protected final Semaphore channelBusy;
    protected final AtomicReference<String> cookie;

    private final Queue<ResponseInformation> queue;
    private final Map<Integer, String> errorsMapping = new HashMap<>();

    /**
     * @param dictionary
     * @param settings
     * @param msgFactory
     * @param clientName
     * @param queue contains information about the next expected response
     * @param channelBusy
     * @param cookie
     */
    public BaseHTTPMatcherHandlerDecode(IDictionaryStructure dictionary, ICommonSettings settings, IMessageFactory msgFactory, String clientName,
            Queue<ResponseInformation> queue, Semaphore channelBusy, AtomicReference<String> cookie) {
        this.dictionary = dictionary;
        this.settings = (HTTPClientSettings)settings;
        this.msgFactory = msgFactory;
        this.clientName = clientName;
        this.queue = queue;
        this.channelBusy = channelBusy;
        this.cookie = cookie;

        for(IMessageStructure messageStructure : dictionary.getMessages().values()) {
            Integer errorCode = getAttributeValue(messageStructure, HTTPMessageHelper.ERROR_CODE_ATTRIBUTE);
            if (errorCode != null) {
                errorsMapping.put(errorCode, messageStructure.getName());
            }
        }
    }

    @Override
    protected void decode(ChannelHandlerContext handlerContext, FullHttpResponse msg, List<Object> out) throws Exception {
        logger.debug("http response received: {}", msg);

        // TODO: check the status code first. If code is 200 and we don't have an element in queue we should throw an error
        ResponseInformation responseInformation = queue.poll();
        String messageName = responseInformation == null ? null : responseInformation.getType();
        byte[] rawMessage = new byte[msg.content().readableBytes()];
        msg.content().readBytes(rawMessage);

        if (!HttpUtil.isKeepAlive(msg)) {
            handlerContext.close();
        }

        //all bytes are read, we can release channel for make next request
        channelBusy.release();

        boolean approved = false;
        int code = msg.status().code();

        if (errorsMapping.containsKey(code)) {
            messageName = errorsMapping.get(code);
            approved = true;
        }

        IMessage decodeResult;
        if (approved || HttpResponseStatus.OK.equals(msg.status()) || HttpResponseStatus.NO_CONTENT.equals(msg.status())) {

            decodeResult = msgFactory.createMessage(messageName, dictionary.getNamespace());

            MsgMetaData metadata = decodeResult.getMetaData();
            metadata.setFromService(settings.getURI());
            metadata.setToService(clientName);
            metadata.setRawMessage(rawMessage);

        } else {
            decodeResult = msgFactory.createMessage("Error", dictionary.getNamespace());
            decodeResult.getMetaData().setFromService(settings.getURI());
            decodeResult.getMetaData().setToService(clientName);
            decodeResult.getMetaData().setRawMessage(rawMessage);

            decodeResult.addField("ResponseCode", msg.status().code());
        }

        if (responseInformation != null) {
            Map<String, String> properties = new HashMap<>(responseInformation.getProperties());
            properties.putIfAbsent(PROTOCOL_PROPERTY, HTTPClient.PROTOCOL_TYPE);
            properties.putIfAbsent(MESSAGE_TYPE_PROPERTY, responseInformation.getType());
            setMessageProperties(decodeResult.getMetaData(), properties);
        }

        decodeResult.addField(HTTPMessageHelper.HTTPHEADER, MatcherHandlerUtil.extractHTTPHeader(msg.headers(), msgFactory, dictionary.getNamespace()));
        setCookie(msg);
        out.add(decodeResult);
    }

    protected void setCookie(@NotNull FullHttpResponse msg) {
        String gettingCookie = msg.headers().get(HttpHeaderNames.SET_COOKIE);
        if (gettingCookie != null) {
            cookie.set(gettingCookie);
        }
    }

}
