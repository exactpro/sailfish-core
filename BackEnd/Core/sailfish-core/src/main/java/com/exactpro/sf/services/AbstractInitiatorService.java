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
package com.exactpro.sf.services;

import java.util.Objects;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.aml.script.actions.WaitAction;
import com.exactpro.sf.common.messages.AttributeNotFoundException;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.MessageNotFoundException;
import com.exactpro.sf.common.messages.MessageUtil;
import com.exactpro.sf.common.messages.MsgMetaData;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.services.ServiceInfo;
import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.configuration.IDictionaryManager;
import com.exactpro.sf.configuration.ILoggingConfigurator;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.IActionContext;
import com.exactpro.sf.services.util.ServiceUtil;
import com.exactpro.sf.storage.IMessageStorage;

public abstract class AbstractInitiatorService implements IInitiatorService {
    protected final Logger logger = LoggerFactory.getLogger(getClass().getName() + "@" + Integer.toHexString(hashCode()));

    protected IServiceContext serviceContext;
    protected ServiceName serviceName;
    protected IServiceMonitor serviceMonitor;
    protected IServiceHandler handler;
    protected IServiceSettings settings;
    protected volatile ServiceStatus status;
    protected ITaskExecutor taskExecutor;
    protected ILoggingConfigurator loggingConfigurator;
    protected IMessageStorage storage;
    protected ServiceInfo serviceInfo;
    protected IMessageFactory messageFactory;
    protected IDictionaryStructure dictionary;
    protected MessageHelper messageHelper;

    public AbstractInitiatorService() {
        status = ServiceStatus.CREATED;
    }

    @Override
    public void init(IServiceContext serviceContext, IServiceMonitor serviceMonitor, IServiceHandler handler, IServiceSettings settings, ServiceName serviceName) {
        this.serviceContext = Objects.requireNonNull(serviceContext, "serviceContext cannot be null");
        this.serviceName = Objects.requireNonNull(serviceName, "serviceName cannot be null");
        this.serviceMonitor = Objects.requireNonNull(serviceMonitor, "serviceMonitor cannot be null");

        try {
            changeStatus(ServiceStatus.INITIALIZING, "Initializing service");

            this.handler = Objects.requireNonNull(handler, "handler cannot be null");
            this.settings = Objects.requireNonNull(settings, "settings cannot be null");
            this.taskExecutor = Objects.requireNonNull(serviceContext.getTaskExecutor(), "taskExecutor cannot be null");
            this.loggingConfigurator =  Objects.requireNonNull(serviceContext.getLoggingConfigurator(), "loggingConfigurator cannot be null");
            this.storage = Objects.requireNonNull(serviceContext.getMessageStorage(), "storage cannot be null");
            this.serviceInfo = Objects.requireNonNull(serviceContext.lookupService(getServiceName()), "serviceInfo cannot be null");

            SailfishURI dictionaryURI = Objects.requireNonNull(getSettings().getDictionaryName(), "dictionaryURI cannot be null");
            IDictionaryManager dictionaryManager = Objects.requireNonNull(serviceContext.getDictionaryManager(), "dictionaryManager cannot be null");

            this.messageFactory = Objects.requireNonNull(dictionaryManager.getMessageFactory(dictionaryURI), "messageFactory cannot be null");
            this.dictionary = Objects.requireNonNull(dictionaryManager.getDictionary(dictionaryURI), "dictionary cannot be null");
            this.messageHelper = createMessageHelper(messageFactory, dictionary);

            getSettings();
            internalInit();

            changeStatus(ServiceStatus.INITIALIZED, "Service initialized");
        } catch(Throwable e) {
            initCleanup();
            changeStatus(ServiceStatus.ERROR, "Failed to initialize service", e);
            throw new ServiceException("Failed to initialize service", e);
        }
    }

    protected abstract void internalInit() throws Exception;

    protected void initCleanup() {}

    @Override
    public void start() {
        try {
            if (getStatus() != ServiceStatus.INITIALIZED) {
                throw new ServiceException("Service should be initialized before starting");
            }
            
            changeStatus(ServiceStatus.STARTING, "Staring service");
            loggingConfigurator.createIndividualAppender(logger.getName(), getServiceName());
            internalStart();
            changeStatus(ServiceStatus.STARTED, "Staring service");
        } catch(Throwable e) {
            startCleanup();
            changeStatus(ServiceStatus.ERROR, "Failed to start service", e);
            throw new ServiceException("Failed to start service", e);
        }
    }

    protected abstract void internalStart() throws Exception;

    protected void startCleanup() {}

    @Override
    public void dispose() {
        try {
            changeStatus(ServiceStatus.DISPOSING, "Disposing service");
            internalDispose();
            changeStatus(ServiceStatus.DISPOSED, "Disposed service");
        } catch(Throwable e) {
            changeStatus(ServiceStatus.ERROR, "Failed to dispose service", e);
            throw new ServiceException("Failed to dispose service", e);
        }
    }

    protected void internalDispose() {
        disposeResources();
    }

    protected void disposeResources() {
        if(loggingConfigurator != null) {
            loggingConfigurator.destroyIndividualAppender(logger.getName(), getServiceName());
        }
    }

    @Override
    public IServiceHandler getServiceHandler() {
        return handler;
    }

    @Override
    public void setServiceHandler(IServiceHandler handler) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getName() {
        return getServiceName().toString();
    }

    @Override
    public ServiceName getServiceName() {
        return serviceName;
    }

    @Override
    public ServiceStatus getStatus() {
        return status;
    }

    @Override
    public AbstractServiceSettings getSettings() {
        return (AbstractServiceSettings)settings;
    }

    @Override
    public IMessage receive(IActionContext actionContext, IMessage message) throws InterruptedException {
        return WaitAction.waitForMessage(actionContext, message, !isAdminMessage(message));
    }
    
    protected void changeStatus(Predicate<ServiceStatus> predicate, ServiceStatus status, String message) {
        if (predicate.test(this.status)) {
            changeStatus(status, message);
        }
    }

    protected void changeStatus(ServiceStatus status, String message) {
        changeStatus(status, message, null);
    }
    
    protected void changeStatus(ServiceStatus status, String message, Throwable e) {
        ServiceUtil.changeStatus(this, serviceMonitor, this.status = status, message, e);

        if(status == ServiceStatus.ERROR) {
            disposeResources();
        }
    }

    protected String getHumanReadable(IMessage message) {
        IMessageStructure messageStructure = dictionary.getMessageStructure(message.getName());

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

    protected abstract String getEndpointName();

    protected void onMessageReceived(IMessage message) throws Exception {
        boolean admin = isAdminMessage(message);
        String endpointName = getEndpointName();
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

    @Override
    public String toString() {
        return getName();
    }
}
