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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.center.impl.SFLocalContext;
import com.exactpro.sf.services.ChangeEnvironmentEvent;
import com.exactpro.sf.testwebgui.notifications.messages.EnvironmentUpdateResponse;
import com.exactpro.sf.testwebgui.notifications.messages.ServiceUpdateResponse;
import com.exactprosystems.webchannels.IUpdateRequestListener;
import com.exactprosystems.webchannels.channel.AbstractChannel;

@SuppressWarnings("deprecation")
public class EnvironmentUpdateSubscriber implements IUpdateRequestListener{

	private static final Logger logger = LoggerFactory.getLogger(EnvironmentUpdateSubscriber.class);
	private final AbstractChannel channel;
	private String id;
	private final Set<ServiceUpdateResponse> eventCache = new HashSet<>();
    private Future<?> future;

	public EnvironmentUpdateSubscriber(String id, AbstractChannel channel) {
		this.id = id;
		this.channel = channel;
		logger.info("Create EnvironmentUpdateSubscriber {} for channel {}", new Object[] {this, channel});

	}

	@Override
	public void onEvent(Object event) {

		logger.debug("Event {} happend for EnvironmentUpdateSubscriber {}", event, this);

		if (event instanceof ServiceUpdateEvent) {

			ServiceUpdateEvent updateEvent = (ServiceUpdateEvent) event;
			ServiceUpdateResponse updateResponse = new ServiceUpdateResponse();
			updateResponse.setRequestId(id);

			ServiceName serviceName = updateEvent.getEvent().getServiceName();
			updateResponse.setEnvName(serviceName.getEnvironment());

			synchronized (eventCache) {
				eventCache.add(updateResponse);
			}

            if (future == null || future.isDone()) {
                future = SFLocalContext.getDefault().getTaskExecutor().schedule(new Runnable() {
					@Override
					public void run() {
						synchronized (eventCache) {
							Map<String, ServiceUpdateResponse> envServiceMap = new HashMap<>();
							for(ServiceUpdateResponse serviceUpdateResponse: eventCache) {
								String serviceName = serviceUpdateResponse.getEnvName();
								if (!envServiceMap.containsKey(serviceName)) {
									envServiceMap.put(serviceName, serviceUpdateResponse);
								}
							}
							for(ServiceUpdateResponse serviceUpdateResponse: envServiceMap.values()) {
								channel.sendMessage(serviceUpdateResponse);
							}
							eventCache.clear();
						}
					}

					@Override
					public String toString() {
					    return EnvironmentUpdateSubscriber.class.getSimpleName();
					}
				}, 1, TimeUnit.SECONDS);
			}

		} else if (event instanceof EnvironmentUpdateEvent){
            EnvironmentUpdateEvent environmentUpdateEvent = (EnvironmentUpdateEvent)event;
            ChangeEnvironmentEvent environmentEvent = (ChangeEnvironmentEvent)environmentUpdateEvent.getEvent();
            EnvironmentUpdateResponse environmentUpdateResponse = new EnvironmentUpdateResponse();
            environmentUpdateResponse.setRequestId(id);
            environmentUpdateResponse.setEnvName(environmentEvent.getName());
            environmentUpdateResponse.setStatus(environmentEvent.getStatus());
            environmentUpdateResponse.setNewEnvName(environmentEvent.getNewEnvName());
            channel.sendMessage(environmentUpdateResponse);
        } else {
			throw new RuntimeException("Incorrect event type " + event.getClass());
		}

	}

	@Override
	public void destroy() {
		//channel = null;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this).append("id", id).toString();
	}

}
