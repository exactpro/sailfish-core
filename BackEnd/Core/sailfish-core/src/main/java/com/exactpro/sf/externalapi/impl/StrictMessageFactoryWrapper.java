/*******************************************************************************
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

package com.exactpro.sf.externalapi.impl;

import java.util.Objects;
import java.util.Set;

import com.exactpro.sf.common.impl.messages.StrictMessageWrapper;
import com.exactpro.sf.common.messages.IHumanMessage;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.suri.SailfishURI;

public class StrictMessageFactoryWrapper implements IMessageFactory {

    private final IMessageFactory msgFactory;
    private final IDictionaryStructure dictionaryStructure;

    public StrictMessageFactoryWrapper(IMessageFactory msgFactory, IDictionaryStructure dictionaryStructure) {
        this.msgFactory = msgFactory;
        this.dictionaryStructure = dictionaryStructure;
    }

    @Override
    public void init(String namespace, SailfishURI dictionaryURI) {
        msgFactory.init(namespace, dictionaryURI);
    }

    @Override
    public IMessage createMessage(String name, String namespace) {
        if (!Objects.equals(namespace, getNamespace())) {
            throw new EPSCommonException(String.format("Unexpected namespace: [%s]. Expected: [%s]", namespace, getNamespace()));
        }

        IMessageStructure messageStructure = dictionaryStructure.getMessageStructure(name);
        if (messageStructure == null) {
            throw new EPSCommonException("Can't find structure for message " + name);
        }

        return new StrictMessageWrapper(msgFactory, messageStructure);
    }

    @Override
    public IMessage createMessage(String name) {
        return createMessage(name, getNamespace());
    }

    @Override
    public IHumanMessage createHumanMessage(String name) {
        return msgFactory.createHumanMessage(name);
    }

    @Override
    public void fillMessageType(IMessage message) {
        msgFactory.fillMessageType(message);
    }

    @Override
    public Set<String> getUncheckedFields() {
        return msgFactory.getUncheckedFields();
    }

    @Override
    public String getNamespace() {
        return dictionaryStructure.getNamespace();
    }

    @Override
    public SailfishURI getDictionaryURI() {
        return msgFactory.getDictionaryURI();
    }

    @Override
    public String getProtocol() {
        return msgFactory.getProtocol();
    }
}
