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
package com.exactpro.sf.services.soap;

import static com.exactpro.sf.common.messages.structures.StructureUtils.getAttributeValue;

import java.util.List;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPMessage;

import com.exactpro.sf.common.messages.DefaultMessageStructureVisitor;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.MessageStructureWriter;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.util.ICommonSettings;
import com.exactpro.sf.services.http.AbstractHTTPDecoder;
import com.exactpro.sf.services.http.handlers.MatcherHandlerUtil;

import io.netty.channel.ChannelHandlerContext;

public class SOAPDecoder extends AbstractHTTPDecoder {

    private IDictionaryStructure dictionaryStructure;
    private IMessageFactory messageFactory;

    @Override
    public void init(ICommonSettings settings, IMessageFactory msgFactory, IDictionaryStructure dictionary, String clientName) {
        this.messageFactory = msgFactory;
        this.dictionaryStructure = dictionary;
    }
    
    @Override
    protected void decode(ChannelHandlerContext handlerContext, IMessage msg, List<Object> out) throws Exception {

        IMessageStructure messageStructure = dictionaryStructure.getMessages().get(msg.getName());
        //error message
        if (messageStructure == null) {
            out.add(msg);
            return;
        }

        if (msg.getMetaData().getRawMessage().length > 0) {

            MessageFactory factory = MessageFactory.newInstance();
            SOAPMessage soapMessage = factory.createMessage(new MimeHeaders(),
                    handleContentEncoding(MatcherHandlerUtil.getEncodingString(msg), msg.getMetaData().getRawMessage()));

            String targetNameSpace = getAttributeValue(messageStructure, SOAPMessageHelper.TNS);

            DefaultMessageStructureVisitor visitor = new SOAPVisitorDecode(soapMessage.getSOAPBody(), messageFactory, msg, targetNameSpace);
            MessageStructureWriter.WRITER.traverse(visitor, messageStructure);
        }

        out.add(msg);
    }

}
