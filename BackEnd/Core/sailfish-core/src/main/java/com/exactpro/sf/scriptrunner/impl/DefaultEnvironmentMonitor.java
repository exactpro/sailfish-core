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
package com.exactpro.sf.scriptrunner.impl;

import com.exactpro.sf.scriptrunner.IConnectionManager;
import com.exactpro.sf.scriptrunner.IEnvironmentListener;
import com.exactpro.sf.services.EnvironmentEvent;
import com.exactpro.sf.services.IEnvironmentMonitor;
import com.exactpro.sf.services.ServiceEvent;
import com.exactpro.sf.storage.IServiceStorage;

public class DefaultEnvironmentMonitor implements IEnvironmentMonitor {

	private IConnectionManager manager;

	private IServiceStorage storage;

	public DefaultEnvironmentMonitor(IConnectionManager manager, IServiceStorage storage) {
		this.manager = manager;
		this.storage = storage;
	}

	@Override
	public void onEvent(ServiceEvent event) {
		this.storage.addServiceEvent(this.manager.getServiceDescription(event.getServiceName()), event);
		for (IEnvironmentListener listener : manager.getEnvironmentListeners()) {
			listener.onEvent(event);
		}
	}

    @Override
	public void onEvent(EnvironmentEvent event) {
		for (IEnvironmentListener listener : manager.getEnvironmentListeners()) {
			listener.onEvent(event);
		}
	}

}
