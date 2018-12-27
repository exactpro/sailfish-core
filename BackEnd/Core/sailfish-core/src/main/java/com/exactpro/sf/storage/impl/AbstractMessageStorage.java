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
package com.exactpro.sf.storage.impl;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.impl.messages.DefaultMessageFactory;
import com.exactpro.sf.common.messages.IHumanMessage;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.MessageUtil;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.configuration.DictionaryManager;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.storage.IMessageStorage;
import com.exactpro.sf.storage.ScriptRun;
import com.exactpro.sf.storage.util.JsonMessageConverter;

public abstract class AbstractMessageStorage implements IMessageStorage {
    protected final Logger logger = LoggerFactory.getLogger(getClass().getName() + "@" + Integer.toHexString(hashCode()));

    protected final DictionaryManager dictionaryManager;
    
    public AbstractMessageStorage(DictionaryManager dictionaryManager) {
        this.dictionaryManager = Objects.requireNonNull(dictionaryManager, "Dictionary manager cannot be null");
    }
    
    @Override
    public void storeMessage(IMessage message) {
        IMessageFactory messageFactory = DefaultMessageFactory.getFactory();
        IDictionaryStructure dictionary = null;
        IMessageStructure messageStructure = null;
        
        SailfishURI dictionaryURI = message.getMetaData().getDictionaryURI();
        if (dictionaryURI != null) {
            dictionary = this.dictionaryManager.getDictionary(dictionaryURI);
            if (dictionary != null) {
                messageStructure = dictionary.getMessageStructure(message.getName());
            }
            messageFactory = this.dictionaryManager.getMessageFactory(dictionaryURI);
        }
        
        IHumanMessage humanMessage = MessageUtil.convertToIHumanMessage(messageFactory, messageStructure, message);
        String jsonMessage = JsonMessageConverter.toJson(message, dictionary);
        storeMessage(message, humanMessage, jsonMessage);
    }
    
    protected abstract void storeMessage(IMessage message, IHumanMessage humanMessage, String jsonMessage);

    protected ScriptRun createScriptRun(String name, String description) {
        String ip = null;
        String host = null;

        try {
            InetAddress address = InetAddress.getLocalHost();
            ip = address.getHostAddress();
            host = address.getHostName();
        } catch (UnknownHostException e) {
            logger.error(e.getMessage(), e);
        }

        // TODO: Use name of authenticated user
        String user = System.getProperty("user.name");
        ScriptRun scriptRun = new ScriptRun();

        scriptRun.setDescription(description);
        scriptRun.setScriptName(name);
        scriptRun.setUser(user);
        scriptRun.setStart(new Timestamp(System.currentTimeMillis()));
        scriptRun.setHostName(host);
        scriptRun.setMachineIP(ip);

        return scriptRun;
    }
}
