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
package com.exactpro.sf.scriptrunner.actionmanager.actioncontext;

import java.util.concurrent.Future;

import com.exactpro.sf.common.services.ServiceInfo;
import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.scriptrunner.IServiceNotifyListener;
import com.exactpro.sf.services.IService;
import com.exactpro.sf.services.IServiceSettings;

public interface IActionServiceManager {
    // from IConnectionManager
    public <Service extends IService> Service getService(ServiceName serviceName);
    public ServiceName[] getServiceNames();
    public IService[] getStartedServices();
    public IServiceSettings getServiceSettings(ServiceName serviceName);
    public Future<?> addService(ServiceName serviceName, SailfishURI uri, IServiceSettings settings, IServiceNotifyListener exceptionListener);
    public Future<?> updateService(ServiceName serviceName, IServiceSettings settings, IServiceNotifyListener exceptionListener);
    public Future<?> initService(ServiceName serviceName, IServiceNotifyListener exceptionListener);
    public Future<?> disposeService(ServiceName serviceName, IServiceNotifyListener exceptionListener);
    public Future<?> removeService(ServiceName serviceName, IServiceNotifyListener exceptionListener);
    public Future<?> startService(ServiceName serviceName, IServiceNotifyListener exceptionListener);

    public ServiceInfo getServiceInfo(ServiceName serviceName);
}
