/******************************************************************************
 * Copyright 2009-2018 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.storage.util;

import java.io.IOException;

import com.exactpro.sf.common.impl.messages.DefaultMessageFactory;
import com.exactpro.sf.common.impl.messages.xml.configuration.JavaType;
import com.exactpro.sf.common.messages.IHumanMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.configuration.IDictionaryManager;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;

public class JsonHumanDecoder extends JsonMessageDecoder<IHumanMessage> {

    private IMessageFactory messageFactory;

    public JsonHumanDecoder(IDictionaryManager dictionaryManager) {
        super(dictionaryManager);
    }

    @Override
    public IHumanMessage parse(JsonParser parser, String protocol, SailfishURI dictionaryURI, String namespace, String name, boolean compact, boolean dirty) throws JsonParseException, IOException {
        this.messageFactory = (dictionaryManager != null && dictionaryURI != null)
                ? dictionaryManager.getMessageFactory(dictionaryURI)
                : DefaultMessageFactory.getFactory();
        return super.parse(parser, protocol, dictionaryURI, namespace, name, compact, dirty);
    }

    @Override
    protected IHumanMessage createMessage(String messageName) {
        return this.messageFactory.createHumanMessage(messageName);
    }

    @Override
    protected void handleField(IHumanMessage message, String fieldName, FieldInfo fieldInfo) {
        message.addField(fieldInfo.getFieldStructure(), fieldName, fieldInfo.getValue());
    }

    @Override
    protected Object parseValueCompact(JsonParser parser, IFieldStructure fieldStructure, JavaType javaType, boolean dirty) throws IOException {
        try {
            return super.parseValueCompact(parser, fieldStructure, javaType, dirty);
        } catch (Exception ignore) {}

        return parser.getValueAsString();
    }

    @Override
    protected Object parseValueFull(JsonParser parser, IFieldStructure fieldStructure, String name, Type type, boolean dirty) throws IOException {
        try {
            return super.parseValueFull(parser, fieldStructure, name, type, dirty);
        } catch (Exception ignore) {}

        return parser.getValueAsString();
    }
}
