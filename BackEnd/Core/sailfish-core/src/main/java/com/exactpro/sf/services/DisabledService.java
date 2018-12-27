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

import com.exactpro.sf.common.services.ServiceName;

public class DisabledService implements IService {
    private final ServiceStatus serviceStatus = ServiceStatus.DISABLED;
    private volatile ServiceName serviceName;
    private volatile IServiceSettings disabledServiceSettings;

    @Override
    public void init(IServiceContext serviceContext, IServiceMonitor serviceMonitor, IServiceHandler handler, IServiceSettings settings, ServiceName name) {
        this.serviceName = name;
        this.disabledServiceSettings = settings;
    }

    @Override
    public void start() {

    }

    @Override
    public void dispose() {

    }

    @Override
    public IServiceHandler getServiceHandler() {
        return null;
    }

    @Override
    public void setServiceHandler(IServiceHandler handler) {

    }

    @Override
    public String getName() {
        return serviceName.getServiceName();
    }

    @Override
    public ServiceName getServiceName() {
        return serviceName;
    }

    @Override
    public ServiceStatus getStatus() {
        return serviceStatus;
    }

    @Override
    public IServiceSettings getSettings() {
        return disabledServiceSettings;
    }
}
