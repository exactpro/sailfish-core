/******************************************************************************
 * Copyright 2009-2019 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.services;

import com.exactpro.sf.aml.script.actions.WaitAction;
import com.exactpro.sf.common.messages.AttributeNotFoundException;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.MessageNotFoundException;
import com.exactpro.sf.common.messages.MessageUtil;
import com.exactpro.sf.common.messages.MsgMetaData;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.util.EvolutionBatch;
import com.exactpro.sf.configuration.IDictionaryManager;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.IActionContext;

import java.util.Objects;

public abstract class AbstractInitiatorService extends AbstractService implements IInitiatorService {
    protected long latency;
    protected IMessageFactory messageFactory;
    protected IDictionaryStructure dictionary;
    protected MessageHelper messageHelper;

    protected abstract String getEndpointName();

    @Override
    protected void initDictionaryData() {
        SailfishURI dictionaryURI = Objects.requireNonNull(
                settings.getDictionaryName(), "dictionaryName cannot be null");
        IDictionaryManager dictionaryManager = Objects.requireNonNull(
                serviceContext.getDictionaryManager(), "dictionaryManager cannot be null");
        this.messageFactory = Objects.requireNonNull(
                dictionaryManager.getMessageFactory(dictionaryURI), "messageFactory cannot be null");
        this.dictionary = Objects.requireNonNull(
                dictionaryManager.getDictionary(dictionaryURI), "dictionary cannot be null");
        this.messageHelper = createMessageHelper(messageFactory, dictionary);
    }

    @Override
    public void start() {
        super.start();
        latency = 0;
    }

    @Override
    public long getLatency() {
        return latency;
    }

    @Override
    public IMessage receive(IActionContext actionContext, IMessage message) throws InterruptedException {
        return WaitAction.waitForMessage(actionContext, message, !isAdminMessage(message));
    }

    protected String getHumanReadable(IMessage message) {
        if (EvolutionBatch.MESSAGE_NAME.equals(message.getName()) && getSettings().isEvolutionSupportEnabled()) {
            return message.toString();
        }
        IMessageStructure messageStructure = dictionary.getMessages().get(message.getName());

        if(messageStructure == null) {
            throw new ServiceException("Unknown message: " + message.getName());
        }

        return MessageUtil.convertToIHumanMessage(messageFactory, messageStructure, message).toString();
    }

    protected abstract MessageHelper createMessageHelper(IMessageFactory messageFactory, IDictionaryStructure dictionary);

    protected MessageHelper getMessageHelper() {
        return messageHelper;
    }

    protected boolean isAdminMessage(IMessage message) {
        try {
            return getMessageHelper().isAdmin(message);
        } catch(MessageNotFoundException e) {
            throw new ServiceException("Unknown message: " + message.getName(), e);
        } catch(AttributeNotFoundException e) {
            throw new ServiceException("Incorrect dictionary", e);
        }
    }

    protected void saveMessage(boolean admin, IMessage message, String from, String to) {
        MsgMetaData metaData = message.getMetaData();

        metaData.setAdmin(admin);
        metaData.setFromService(from);
        metaData.setToService(to);
        metaData.setServiceInfo(serviceInfo);

        try {
            storage.storeMessage(message);
        } catch(Exception e) {
            logger.error("Failed to store message", e);
        }
    }

    protected void onMessageReceived(IMessage message) throws Exception {
        boolean admin = isAdminMessage(message);
        String endpointName = getEndpointName();
        if (EvolutionBatch.MESSAGE_NAME.equals(message.getName())) {
            if (getSettings().isEvolutionSupportEnabled()) {
                logger.info("Saving evolution batch message: {}", message);
                saveIncomingMessage(message, endpointName, admin);
            } else {
                logger.debug("Skip saving evolution batch message: {}", message);
            }
        } else {
            saveIncomingMessage(message, endpointName, admin);
        }
        long senderTime = getMessageHelper().getSenderTime(message);

        if(senderTime != 0) {
            latency = System.currentTimeMillis() - senderTime;
        }
    }

    private void saveIncomingMessage(IMessage message, String endpointName, boolean admin) throws ServiceHandlerException {
        logger.debug("Saving message received from {} (admin: {}): {}", endpointName, admin, message);
        saveMessage(admin, message, endpointName, getName());
        getServiceHandler().putMessage(getSession(), ServiceHandlerRoute.get(true, admin), message);
    }

    protected void onMessageSent(IMessage message) throws Exception {
        boolean admin = isAdminMessage(message);
        String endpointName = getEndpointName();
        logger.debug("Saving message sent to {} (admin: {}): {}", endpointName, admin, message);
        saveMessage(admin, message, getName(), endpointName);
        getServiceHandler().putMessage(getSession(), ServiceHandlerRoute.get(false, admin), message);
    }
}
