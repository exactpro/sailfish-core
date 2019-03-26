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
package com.exactpro.sf.scriptrunner;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import com.exactpro.sf.center.IDisposable;
import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.services.IService;
import com.exactpro.sf.services.IServiceSettings;
import com.exactpro.sf.services.ServiceDescription;

public interface IConnectionManager extends IDisposable
{
    Future<?> addService(ServiceDescription serviceDescription, IServiceNotifyListener exceptionListener);

	Future<?> initService(ServiceName serviceName, IServiceNotifyListener exceptionListener);

	Future<?> startService(ServiceName serviceName, IServiceNotifyListener exceptionListener);

	Future<?> disposeService(ServiceName serviceName, IServiceNotifyListener exceptionListener);

	Future<?> removeService(ServiceName serviceName, IServiceNotifyListener exceptionListener);

    Future<?> updateService(ServiceDescription serviceDescription, IServiceNotifyListener exceptionListener);

	Future<?> copyService(ServiceName from, ServiceName to, IServiceNotifyListener notifyListener);

    Future<?> removeEnvironment(String envName, IServiceNotifyListener notifyListener);

    Future<?> addEnvironment(final String envName, final IServiceNotifyListener notifyListener);

    Future<?> renameEnvironment(final String oldEnvName, final String newEnvName, final IServiceNotifyListener notifyListener);

    List<String> getEnvironmentList();

	/**
	 * Lock services which are used in a script.
	 * If another script will lock one of the locked services it will wait
	 * until service release in setServiceNotUsed method.
	 * @param names collection of services to be locked
	 * @throws InterruptedException
	 */
	void setServiceUsed(String[] name) throws InterruptedException;

	/**
	 * Release specified services.
	 * @param names collection of services to be released
	 */
	void setServiceNotUsed(String[] name);

	Set<String> getUsedServices();

	<Service extends IService> Service getService(ServiceName serviceName);

	ServiceName[] getServiceNames();

	IService[] getStartedServices();

	ServiceDescription[] getServicesDescriptions();

	ServiceDescription getServiceDescription(ServiceName serviceName);

	IServiceSettings getServiceSettings(ServiceName serviceName);

	void cleanup(List<String> services);

	void subscribeForEvents(IEnvironmentListener listener);

	void unSubscribeForEvents(IEnvironmentListener listener);

	List<IEnvironmentListener> getEnvironmentListeners();

    Map<String, String> getVariableSet(String name);

    void putVariableSet(String name, Map<String, String> variableSet);

    void removeVariableSet(String name);

    boolean isVariableSetExists(String name);

    Set<String> getVariableSets();

    void setEnvironmentVariableSet(String environmentName, String variableSetName);

    String getEnvironmentVariableSet(String environmentName);
}
