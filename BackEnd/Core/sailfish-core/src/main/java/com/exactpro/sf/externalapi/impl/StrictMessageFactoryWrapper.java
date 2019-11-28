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

import org.jetbrains.annotations.NotNull;

import com.exactpro.sf.common.impl.messages.AbstractMessageFactory;
import com.exactpro.sf.common.impl.messages.StrictMessageWrapper;
import com.exactpro.sf.common.messages.IHumanMessage;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.MsgMetaData;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IFieldStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.suri.SailfishURI;

public class StrictMessageFactoryWrapper extends AbstractMessageFactory {

    private final IMessageFactory msgFactory;
    private final IDictionaryStructure dictionaryStructure;

    public StrictMessageFactoryWrapper(IMessageFactory msgFactory, IDictionaryStructure dictionaryStructure) {
        this.msgFactory = Objects.requireNonNull(msgFactory, "msgFactory must not be null");
        this.dictionaryStructure = Objects.requireNonNull(dictionaryStructure, "dictionaryStructure");
    }

    @Override
    public void init(SailfishURI dictionaryURI, IDictionaryStructure dictionary) {
        super.init(dictionaryURI, dictionary);
        msgFactory.init(dictionaryURI, dictionary);
    }

    @Override
    public void init(String namespace, SailfishURI dictionaryURI) {
        msgFactory.init(namespace, dictionaryURI);
    }

    @Override
    public IMessage createMessage(String name, String namespace) {
        return wrapMessage(msgFactory.createMessage(name, namespace));
    }

    @Override
    public IMessage createMessage(long id, String name, String namespace) {
        return wrapMessage(msgFactory.createMessage(id, name, namespace));
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

    @Override
    protected void addSubMessage(IFieldStructure fieldStructure, IMessage parentMessage) {
        IMessage subMessage = parentMessage.isFieldSet(fieldStructure.getName())
                ? parentMessage.getField(fieldStructure.getName())
                : msgFactory.createMessage(fieldStructure.getReferenceName(), fieldStructure.getNamespace());

        StrictMessageWrapper strictMessage =  wrapMessage(subMessage);

        parentMessage.addField(fieldStructure.getName(), strictMessage);
    }

    private StrictMessageWrapper wrapMessage(IMessage message) {
        IMessageStructure messageStructure = getMessageStructure(message.getNamespace(), message.getName());
        createComplexFields(message);
        return new StrictMessageWrapper(message, messageStructure);
    }

    @NotNull
    private IMessageStructure getMessageStructure(@NotNull String namespace, @NotNull String name) {
        if (!Objects.equals(namespace, getNamespace())) {
            throw new EPSCommonException(String.format("Unexpected namespace: [%s]. Expected: [%s]",namespace, getNamespace()));
        }

        IMessageStructure messageStructure = dictionaryStructure.getMessages().get(name);
        if (messageStructure == null) {
            throw new EPSCommonException("Dictionary '"+ getNamespace() +"' doesn't contain message '" + name + '\'');
        }
        return messageStructure;
    }
}
