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

import java.io.ByteArrayOutputStream;
import java.util.List;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPMessage;

import com.exactpro.sf.common.messages.DefaultMessageStructureVisitor;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.MessageStructureReader;
import com.exactpro.sf.common.messages.MessageStructureReaderHandlerImpl;
import com.exactpro.sf.common.messages.MsgMetaData;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.util.ICommonSettings;
import com.exactpro.sf.services.http.AbstractHTTPEncoder;
import com.exactpro.sf.services.http.HTTPClientSettings;

import io.netty.channel.ChannelHandlerContext;

public class SOAPEncoder extends AbstractHTTPEncoder {

    private IDictionaryStructure dictionaryStructure;
    private HTTPClientSettings settings;
    private String clientName;

    @Override
    public void init(ICommonSettings settings, IMessageFactory msgFactory, IDictionaryStructure dictionary, String clientName) {
        this.settings = (HTTPClientSettings) settings;
        this.dictionaryStructure = dictionary;
        this.clientName = clientName;
    }

    @Override
    protected void encode(ChannelHandlerContext handlerContext, IMessage msg, List<Object> out) throws Exception {

        MessageFactory factory = MessageFactory.newInstance();
        SOAPMessage soapMessage = factory.createMessage();
        SOAPBody body = soapMessage.getSOAPBody();

        IMessageStructure messageStructure = dictionaryStructure.getMessages().get(msg.getName());

        String targetNameSpace = getAttributeValue(messageStructure, SOAPMessageHelper.TNS);

        DefaultMessageStructureVisitor visitor = new SOAPVisitorEncode(body, targetNameSpace);
        MessageStructureReader.READER.traverse(visitor, messageStructure, msg, MessageStructureReaderHandlerImpl.instance());
        soapMessage.saveChanges();

        // setup metadata
        ByteArrayOutputStream rawMessage = new ByteArrayOutputStream();
        soapMessage.writeTo(rawMessage);
        MsgMetaData metadata = msg.getMetaData();
        metadata.setToService(settings.getURI());
        metadata.setFromService(clientName);
        metadata.setRawMessage(rawMessage.toByteArray());
        
        out.add(msg);
    }

}
