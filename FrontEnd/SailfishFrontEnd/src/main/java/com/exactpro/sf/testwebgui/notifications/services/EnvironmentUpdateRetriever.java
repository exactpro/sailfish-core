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
package com.exactpro.sf.testwebgui.notifications.services;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.scriptrunner.IEnvironmentListener;
import com.exactpro.sf.services.EnvironmentEvent;
import com.exactpro.sf.services.ServiceChangeUpdateEvent;
import com.exactpro.sf.services.ServiceEvent;
import com.exactpro.sf.services.ServiceStatusUpdateEvent;
import com.exactprosystems.webchannels.AbstractUpdateEvent;
import com.exactprosystems.webchannels.IUpdateRequestListener;
import com.exactprosystems.webchannels.IUpdateRetriever;

@SuppressWarnings("deprecation")
public class EnvironmentUpdateRetriever implements IUpdateRetriever, IEnvironmentListener {

	private static final Logger logger = LoggerFactory.getLogger(EnvironmentUpdateRetriever.class);

	private List<IUpdateRequestListener> listeners;

	public EnvironmentUpdateRetriever() {
		listeners = new ArrayList<>();
	}

	@Override
	public void onEvent(ServiceEvent event) {
		if(event instanceof ServiceStatusUpdateEvent || event instanceof ServiceChangeUpdateEvent) {
			ServiceUpdateEvent updateEvent = new ServiceUpdateEvent();
			updateEvent.setEvent(event);

			for (IUpdateRequestListener listener : listeners) {
				notifyListener(listener, updateEvent);
			}
		}
	}

    @Override
	public void onEvent(EnvironmentEvent event) {
        EnvironmentUpdateEvent environmentUpdateEvent = new EnvironmentUpdateEvent(event);
        for (IUpdateRequestListener listener : listeners) {
            notifyListener(listener, environmentUpdateEvent);
        }
	}

	private void notifyListener(IUpdateRequestListener listener, AbstractUpdateEvent event) {
		try {
			listener.onEvent(event);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	public void registerUpdateRequest(IUpdateRequestListener listener)  {

		logger.debug("Listener {} register in ServiceUpdateRetriever {}", listener, this);

		if (listener instanceof EnvironmentUpdateSubscriber) {
			listeners.add(listener);
		} else {
			throw new RuntimeException("Listener = " + listener + " is not instance of " + EnvironmentUpdateSubscriber.class);
		}

	}

	@Override
	public void unregisterUpdateRequest(IUpdateRequestListener listener)  {

		logger.debug("Listener {} unregister in ServiceUpdateRetriever {}", listener, this);

		if (listener instanceof EnvironmentUpdateSubscriber) {
			listeners.remove(listener);
		} else {
			throw new RuntimeException("Listener = " + listener + " is not instance of " + EnvironmentUpdateSubscriber.class);
		}

	}

	@Override
	public void synchronizeUpdateRequest(IUpdateRequestListener listener)  {

		// Not needed yet, because it sends only notifications about updates

	}

	@Override
	public void destroy() {
		listeners.clear();
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this).toString();
	}

}
