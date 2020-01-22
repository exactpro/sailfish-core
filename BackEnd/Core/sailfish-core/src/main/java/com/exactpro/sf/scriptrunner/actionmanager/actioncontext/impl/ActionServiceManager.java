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
package com.exactpro.sf.scriptrunner.actionmanager.actioncontext.impl;

import java.io.OutputStream;
import java.util.Objects;
import java.util.concurrent.Future;

import com.exactpro.sf.common.services.ServiceInfo;
import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.scriptrunner.IConnectionManager;
import com.exactpro.sf.scriptrunner.IServiceNotifyListener;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.IActionServiceManager;
import com.exactpro.sf.services.IService;
import com.exactpro.sf.services.IServiceSettings;
import com.exactpro.sf.services.ServiceDescription;
import com.exactpro.sf.services.ServiceMarshalManager;
import com.exactpro.sf.storage.IServiceStorage;

public class ActionServiceManager implements IActionServiceManager {
    private final IConnectionManager connectionManager;
    private final IServiceStorage serviceStorage;
    private final ServiceMarshalManager serviceMarshalManager;

    public ActionServiceManager(IConnectionManager connectionManager, IServiceStorage serviceStorage, ServiceMarshalManager serviceMarshalManager) {
        this.connectionManager = Objects.requireNonNull(connectionManager, "connection manager cannot be null");
        this.serviceStorage = Objects.requireNonNull(serviceStorage, "service storage cannot be null");
        this.serviceMarshalManager = Objects.requireNonNull(serviceMarshalManager, "service marshal manager cannot be null");
    }

    @Override
    public <Service extends IService> Service getService(ServiceName serviceName) {
        return connectionManager.getService(serviceName);
    }

    @Override
    public ServiceName[] getServiceNames() {
        return connectionManager.getServiceNames();
    }

    @Override
    public IService[] getStartedServices() {
        return connectionManager.getStartedServices();
    }

    @Override
    public IServiceSettings getServiceSettings(ServiceName serviceName) {
        return connectionManager.getServiceSettings(serviceName);
    }

    @Override
    public void serializeServiceSettings(ServiceName serviceName, OutputStream out) {
        serviceMarshalManager.serializeService(connectionManager.getServiceDescription(serviceName), out);
    }

    @Override
    public Future<?> addService(ServiceName serviceName, SailfishURI uri, IServiceSettings settings, IServiceNotifyListener exceptionListener) {
        ServiceDescription serviceDescription = new ServiceDescription(uri);

        serviceDescription.setEnvironment(serviceName.getEnvironment());
        serviceDescription.setName(serviceName.getServiceName());
        serviceDescription.setSettings(settings);

        return connectionManager.addService(serviceDescription, exceptionListener);
    }

    @Override
    public Future<?> updateService(ServiceName serviceName, IServiceSettings settings, IServiceNotifyListener exceptionListener) {
        ServiceDescription serviceDescription = connectionManager.getServiceDescription(serviceName);
        serviceDescription.setSettings(settings);
        return connectionManager.updateService(serviceDescription, exceptionListener);
    }

    @Override
    public Future<?> initService(ServiceName serviceName, IServiceNotifyListener exceptionListener) {
        return connectionManager.initService(serviceName, exceptionListener);
    }

    @Override
    public Future<?> disposeService(ServiceName serviceName, IServiceNotifyListener exceptionListener) {
        return connectionManager.disposeService(serviceName, exceptionListener);
    }

    @Override
    public Future<?> removeService(ServiceName serviceName, IServiceNotifyListener exceptionListener) {
        return connectionManager.removeService(serviceName, exceptionListener);
    }

    @Override
    public Future<?> startService(ServiceName serviceName, IServiceNotifyListener exceptionListener) {
        return connectionManager.startService(serviceName, exceptionListener);
    }

    @Override
    public ServiceInfo getServiceInfo(ServiceName serviceName) {
        return serviceStorage.lookupService(serviceName);
    }
}
