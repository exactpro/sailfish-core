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
package com.exactpro.sf.services.json;

import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.MessageStructureWriter;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.util.ICommonSettings;
import com.exactpro.sf.services.http.AbstractHTTPDecoder;
import com.exactpro.sf.services.http.handlers.MatcherHandlerUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.netty.channel.ChannelHandlerContext;

public class JSONDecoder extends AbstractHTTPDecoder {

    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName() + "@" + Integer.toHexString(hashCode()));

    private IDictionaryStructure dictionaryStructure;
    private IMessageFactory msgFactory;
    private final JsonSettings jsonSettings;

    public JSONDecoder() {
        this(new JsonSettings());
    }

    public JSONDecoder(boolean rejectUnexpectedFields) {
        this();
        this.jsonSettings.setRejectUnexpectedFields(rejectUnexpectedFields);
    }

    public JSONDecoder(JsonSettings settings) {
        Objects.requireNonNull(settings, "settings cannot be null");
        this.jsonSettings = settings;
    }

    @Override
    public void init(ICommonSettings settings, IMessageFactory msgFactory, IDictionaryStructure dictionary, String clientName) {
        this.dictionaryStructure = dictionary;
        this.msgFactory = msgFactory;
    }

    @Override
    protected void decode(ChannelHandlerContext handlerContext, IMessage msg, List<Object> out) throws Exception {

        IMessageStructure messageStructure = dictionaryStructure.getMessages().get(msg.getName());
        // error message
        if (messageStructure == null) {
            out.add(msg);
            return;
        }

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode root = null;
        if (msg.getMetaData().getRawMessage().length > 0) {
            try {
                root = objectMapper.readTree(handleContentEncoding(MatcherHandlerUtil.getEncodingString(msg), msg.getMetaData().getRawMessage()));
            } catch (JsonProcessingException e) {
                logger.error("Json parsing error", e);
                msg.getMetaData().setRejectReason(e.getMessage());
                out.add(msg);
                return;
            }
        }

        try {
            root = JSONVisitorUtility.preprocessMessageNode(root, messageStructure);
            MessageStructureWriter.WRITER.traverse(new JSONVisitorDecode(root, msgFactory, msg, jsonSettings), messageStructure);

            if (jsonSettings.isRejectUnexpectedFields()) {
                String rejectReason = JSONVisitorUtility.checkForUnexpectedFields(root, messageStructure);
                JSONVisitorUtility.addRejectReason(msg, rejectReason);
            }
        } catch (Exception e) {
            logger.error("error while traversing json structure", e);
            msg.getMetaData().setRejectReason(e.getMessage());
        }

        out.add(msg);
    }

}
