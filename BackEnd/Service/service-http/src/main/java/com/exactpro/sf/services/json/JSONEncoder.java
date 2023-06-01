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
package com.exactpro.sf.services.json;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.BooleanUtils;

import com.exactpro.sf.common.messages.DirtyMessageTraverser;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.MessageStructureReader;
import com.exactpro.sf.common.messages.MessageStructureReaderHandlerImpl;
import com.exactpro.sf.common.messages.MsgMetaData;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.messages.structures.StructureUtils;
import com.exactpro.sf.common.util.ICommonSettings;
import com.exactpro.sf.services.http.AbstractHTTPEncoder;
import com.exactpro.sf.services.http.HTTPClientSettings;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.netty.channel.ChannelHandlerContext;

public class JSONEncoder extends AbstractHTTPEncoder {
    protected IDictionaryStructure dictionaryStructure;
    protected HTTPClientSettings settings;
    protected String clientName;
    protected IMessageFactory msgFactory;
    private final JsonSettings jsonSettings;

    private Map<String, IFieldStructure> dynamicStructures = new HashMap<>();

    public JSONEncoder() {
        this(new JsonSettings());
    }
    public JSONEncoder(JsonSettings settings) {
        Objects.requireNonNull(settings, "settings cannot be null");
        this.jsonSettings = settings;
    }

    @Override
    public void init(ICommonSettings settings, IMessageFactory msgFactory, IDictionaryStructure dictionary, String clientName) {
        this.settings = (HTTPClientSettings) settings;
        this.dictionaryStructure = dictionary;
        this.clientName = clientName;
        this.msgFactory = msgFactory;
    }

    @Override
    protected void encode(ChannelHandlerContext handlerContext, IMessage msg, List<Object> out) throws Exception {


        IMessageStructure messageStructure = dictionaryStructure.getMessages().get(msg.getName());
        MessageStructureReader structureReader = createMessageStructureReader(msg);

        ByteArrayOutputStream body = new ByteArrayOutputStream();

        try (JsonGenerator generator = new JsonFactory().createGenerator(body)) {
            boolean isObject = !BooleanUtils.toBoolean(StructureUtils.<Boolean>getAttributeValue(messageStructure, JSONMessageHelper.IS_NO_OBJECT_ATTR));
            boolean fromArray = BooleanUtils.toBoolean(StructureUtils.<Boolean>getAttributeValue(messageStructure, JSONMessageHelper.FROM_ARRAY_ATTR));
            ObjectMapper mapper = new ObjectMapper();
            IJsonNodeWrapper root = null;

            if (fromArray) {
                root = new ArrayNodeWrapper(mapper.createArrayNode());
            } else if (isObject) {
                root = new ObjectNodeWrapper(mapper.createObjectNode());
            }

            JSONVisitorEncode visitor = createVisitor(root, dictionaryStructure, msg, jsonSettings);
            structureReader.traverse(visitor, messageStructure, msg, MessageStructureReaderHandlerImpl.instance());
            boolean isSimpleRoot = BooleanUtils.toBoolean(StructureUtils.<Boolean>getAttributeValue(messageStructure, JSONMessageHelper.IS_SIMPLE_ROOT_VALUE_ATTR));
            JsonNode rootNode = isSimpleRoot
                    ? JSONVisitorUtility.extractSimpleRoot(visitor.getRoot(), messageStructure)
                    : visitor.getRoot();
            mapper.writeTree(generator, rootNode);
        }

        byte[] rawMessage = body.toByteArray();

        //set metadata
        MsgMetaData metadata = msg.getMetaData();
        metadata.setToService(settings.getURI());
        metadata.setFromService(clientName);
        metadata.setRawMessage(hasBodyFields(messageStructure) ? rawMessage : new byte[0]);

        out.add(msg);
    }

    protected JSONVisitorEncode createVisitor(IJsonNodeWrapper root, IDictionaryStructure dictionaryStructure, IMessage msg, JsonSettings jsonSettings) {
        return new JSONVisitorEncode(root, dictionaryStructure, () -> createMessageStructureReader(msg), dynamicStructures, jsonSettings, msg);
    }

    protected MessageStructureReader createMessageStructureReader(IMessage msg) {
        return msg.getMetaData().isDirty() ? new DirtyMessageTraverser() : MessageStructureReader.READER;
    }

    private boolean hasBodyFields(IMessageStructure message) {
        for (IFieldStructure field : message.getFields().values()) {
            Boolean isURIParam = BooleanUtils.toBoolean(StructureUtils.<Boolean>getAttributeValue(field, JSONMessageHelper.IS_URI_PARAM_ATTR));
            if (!isURIParam) {
                return true;
            }
        }
        return false;
    }
}
