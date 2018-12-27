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
package com.exactpro.sf.testwebgui.notifications.events;

import com.exactpro.sf.testwebgui.servlets.PollingServlet;
import com.exactprosystems.webchannels.IUpdateRequestListener;
import com.exactprosystems.webchannels.IUpdateRetriever;
import org.apache.log4j.spi.LoggingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

@SuppressWarnings("deprecation")
public class EventRetriever implements IUpdateRetriever, LogSubscriber{

	private static final Logger logger = LoggerFactory.getLogger(EventRetriever.class);

	private List<IUpdateRequestListener> listeners;

	private Queue<LoggingEvent> eventQueue;

	public EventRetriever() {
		listeners = new ArrayList<>();
		eventQueue = new LinkedList<>();
	}

	@Override
	public void registerUpdateRequest(IUpdateRequestListener listener) {

		logger.debug("Listener {} register in EventRetriever {}", listener, this);

		if (listener instanceof EventSubscriber) {
			listeners.add(listener);
		} else {
			throw new RuntimeException("Listener = " + listener + " is not instance of " + EventSubscriber.class);
		}

	}

	@Override
	public void unregisterUpdateRequest(IUpdateRequestListener listener)  {

		logger.debug("Listener {} unregister in EventRetriever {}", listener, this);

		if (listener instanceof EventSubscriber) {
			listeners.remove(listener);
		} else {
			throw new RuntimeException("Listener = " + listener + " is not instance of " + EventSubscriber.class);
		}

	}

	@Override
	public void synchronizeUpdateRequest(IUpdateRequestListener listener)   {

		Queue<LoggingEvent> copyEvents = null;

		synchronized (eventQueue) {
			copyEvents = new LinkedList<>(eventQueue);
		}

		for (LoggingEvent event : copyEvents) {
			Event updateEvent = new Event();
			updateEvent.setEvent(event);
			notifyListener(listener, updateEvent);
		}
	}

	@Override
	public void destroy() {
		listeners.clear();
	}

	private void notifyListener(IUpdateRequestListener listener, Event event) {
		try {
			listener.onEvent(event);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	public void onEvent(LoggingEvent event) {

		synchronized (eventQueue) {
			eventQueue.offer(event);
			if (eventQueue.size() > PollingServlet.BUFFER_SIZE) {
				eventQueue.poll();
			}
		}

		Event updateEvent = new Event();
		updateEvent.setEvent(event);

		for (IUpdateRequestListener listener : listeners) {
			notifyListener(listener, updateEvent);
		}

	}

}
