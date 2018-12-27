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
package com.exactpro.sf.messages;

import java.util.Set;

import com.exactpro.sf.common.impl.messages.DefaultMessageFactory;
import com.exactpro.sf.common.messages.IHumanMessage;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.services.tcpip.TCPIPMessageHelper;

public class IncomingMessageFactory implements IMessageFactory {

    private final IMessageFactory delegateFactory;

    public IncomingMessageFactory(IMessageFactory delegateFactory) {
        this.delegateFactory = delegateFactory;
    }

    public IncomingMessageFactory() {
        this(DefaultMessageFactory.getFactory());
    }

    @Override
    public void init(String namespace, SailfishURI dictionaryURI) {
        delegateFactory.init(namespace, dictionaryURI);
    }

    @Override
    public IMessage createMessage(String name, String namespace) {
        return delegateFactory.createMessage(TCPIPMessageHelper.INCOMING_MESSAGE_NAME_AND_NAMESPACE,
                                            TCPIPMessageHelper.INCOMING_MESSAGE_NAME_AND_NAMESPACE);
    }

    @Override
    public IMessage createMessage(String name) {
        return createMessage(null, null);
    }
    
    @Override
    public IHumanMessage createHumanMessage(String name) {
        return delegateFactory.createHumanMessage(TCPIPMessageHelper.INCOMING_MESSAGE_NAME_AND_NAMESPACE);
    }

    @Override
    public void fillMessageType(IMessage message) {
        delegateFactory.fillMessageType(message);
    }

    @Override
    public Set<String> getUncheckedFields() {
        return delegateFactory.getUncheckedFields();
    }

    @Override
    public String getNamespace() {
        return delegateFactory.getNamespace();
    }

    @Override
    public SailfishURI getDictionaryURI() {
        return delegateFactory.getDictionaryURI();
    }

    @Override
    public String getProtocol() {
        return delegateFactory.getProtocol();
    }
}
