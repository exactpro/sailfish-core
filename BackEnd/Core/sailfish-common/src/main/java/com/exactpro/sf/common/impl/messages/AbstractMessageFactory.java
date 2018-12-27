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
package com.exactpro.sf.common.impl.messages;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.exactpro.sf.common.messages.IHumanMessage;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.configuration.suri.SailfishURI;

// all custom message factories should extend this one
public abstract class AbstractMessageFactory implements IMessageFactory {
    private String namespace;
    private SailfishURI dictionaryURI;

    @Override
    public void init(String namespace, SailfishURI dictionaryURI) {
        if(StringUtils.isBlank(namespace)) {
            throw new IllegalArgumentException("namespace cannot be blank");
        }

        this.namespace = namespace;
        this.dictionaryURI = Objects.requireNonNull(dictionaryURI, "dictionaryURI cannot be null");
    }

    @Override
    public IMessage createMessage(String name, String namespace) {
        IMessage message = new MapMessage(namespace, name);

        message.getMetaData().setDictionaryURI(dictionaryURI);
        message.getMetaData().setProtocol(getProtocol());

        return message;
    }

    @Override
    public IMessage createMessage(String name) {
        return createMessage(name, namespace);
    }

    @Override
    public IHumanMessage createHumanMessage(String name) {
        return new HumanMessage();
    }
    
    @Override
    public void fillMessageType(IMessage message) {
        // TODO Auto-generated method stub
    }

    @Override
    public Set<String> getUncheckedFields() {
        return Collections.emptySet();
    }

    @Override
    public String getNamespace() {
        return namespace;
    }

    @Override
    public SailfishURI getDictionaryURI() {
        return dictionaryURI;
    }
}
