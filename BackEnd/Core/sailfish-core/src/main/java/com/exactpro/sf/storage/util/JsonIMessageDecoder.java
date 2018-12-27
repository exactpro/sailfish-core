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

import com.exactpro.sf.common.impl.messages.AbstractMessageFactory;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.configuration.IDictionaryManager;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;

public class JsonIMessageDecoder extends JsonMessageDecoder<IMessage> {

    protected JSONMessageFactory messageFactory;
    
    public JsonIMessageDecoder(IDictionaryManager dictionaryManager) {
        super(dictionaryManager);
    }

    @Override
    public IMessage parse(JsonParser parser, String protocol, SailfishURI dictionaryURI, String namespace,
            String name, boolean compact, boolean dirty) throws JsonParseException, IOException {
        this.messageFactory = new JSONMessageFactory(protocol);
        messageFactory.init(namespace, dictionaryURI);
        IMessage message = super.parse(parser, protocol, dictionaryURI, namespace, name, compact, dirty);
        //        IMessage message = messageFactory.createMessage(name, namespace);
        //        MsgMetaData metaData = new MsgMetaData(namespace, name, new Date(timestamp));
        //        metaData.setProtocol(protocol);
        //TODO: set new MsgMetaData
        return message;
    }
    
    @Override
    protected IMessage createMessage(String messageName) {
        return this.messageFactory.createMessage(messageName);
    }
    
    @Override
    protected void handleField(IMessage message, String fieldName, FieldInfo fieledInfo) {
        message.addField(fieldName, fieledInfo.getValue());
    }
    
    private static class JSONMessageFactory extends AbstractMessageFactory {

        private final String protocol;
        
        public JSONMessageFactory(String protocol) {
            this.protocol = protocol;
        }

        @Override
        public String getProtocol() {
            return this.protocol;
        }
        
    }
}
