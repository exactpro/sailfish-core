/******************************************************************************
 * Copyright 2009-2020 Exactpro (Exactpro Systems Limited)
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

import com.exactpro.sf.common.services.ServiceInfo;
import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.configuration.IDataManager;
import com.exactpro.sf.configuration.ILoggingConfigurator;
import com.exactpro.sf.services.util.ServiceUtil;
import com.exactpro.sf.storage.IMessageStorage;

public abstract class AbstractService implements IService {
    protected final Logger logger = LoggerFactory.getLogger(ILoggingConfigurator.getLoggerName(this));

    protected IServiceContext serviceContext;
    protected ServiceName serviceName;
    protected IServiceMonitor serviceMonitor;
    protected IServiceHandler handler;
    protected IServiceSettings settings;
    protected volatile ServiceStatus status;
    protected IDataManager dataManager;
    protected ITaskExecutor taskExecutor;
    protected ILoggingConfigurator loggingConfigurator;
    protected IMessageStorage storage;
    protected ServiceInfo serviceInfo;

    public AbstractService() {
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
            this.dataManager = Objects.requireNonNull(serviceContext.getDataManager(), "dataManager cannot be null");
            this.loggingConfigurator =  Objects.requireNonNull(serviceContext.getLoggingConfigurator(), "loggingConfigurator cannot be null");
            this.storage = Objects.requireNonNull(serviceContext.getMessageStorage(), "storage cannot be null");
            this.serviceInfo = Objects.requireNonNull(serviceContext.lookupService(getServiceName()), "serviceInfo cannot be null");
            if (settings.getSendMessageTimeout() < 1) {
                throw new ServiceException("Send message timeout should be grated than zero");
            }
            initDictionaryData();
            getSettings();
            internalInit();
            changeStatus(ServiceStatus.INITIALIZED, "Service initialized");
        } catch(Exception e) {
            initCleanup();
            changeStatus(ServiceStatus.ERROR, "Failed to initialize service", e);
            throw new ServiceException("Failed to initialize service", e);
        }
    }

    protected void initDictionaryData() {}

    protected void internalInit() throws Exception {}

    protected void initCleanup() {}

    @Override
    public void start() {
        try {
            if (getStatus() != ServiceStatus.INITIALIZED) {
                throw new ServiceException("Service should be initialized before starting");
            }
            changeStatus(ServiceStatus.STARTING, "Starting service");
            loggingConfigurator.createAndRegister(getServiceName(), this);
            internalStart();
            changeStatus(ServiceStatus.STARTED, "Started service");
        } catch(Exception e) {
            startCleanup();
            changeStatus(ServiceStatus.ERROR, "Failed to start service", e);
            throw new ServiceException("Failed to start service", e);
        }
    }

    protected void internalStart() throws Exception {}

    protected void startCleanup() {}

    @Override
    public void dispose() {
        try {
            changeStatus(ServiceStatus.DISPOSING, "Disposing service");
            internalDispose();
            changeStatus(ServiceStatus.DISPOSED, "Disposed service");
        } catch(Exception e) {
            changeStatus(ServiceStatus.ERROR, "Failed to dispose service", e);
            throw new ServiceException("Failed to dispose service", e);
        }
    }

    protected void internalDispose() {
        disposeResources();
    }

    protected void disposeResources() {
        if(loggingConfigurator != null) {
            loggingConfigurator.destroyAppender(getServiceName());
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

    @Override
    public String toString() {
        return getName();
    }
}
