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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

public class WebLoggingAppender extends AppenderSkeleton{

	private static List<LogSubscriber> subscribers = new CopyOnWriteArrayList<LogSubscriber>();
	
	public WebLoggingAppender() {

	}
	
	public static void registerSubscriber(LogSubscriber subscriber) {
		subscribers.add(subscriber);
	}
	
	public static void unRegisterSubscriber(LogSubscriber subscriber) {
		subscribers.remove(subscriber);
	}
	
	@Override
	public void close() {
		subscribers.clear();
	}

	@Override
	public boolean requiresLayout() {
		return false;
	}

	@Override
	protected void append(LoggingEvent event) {
		for (LogSubscriber subscriber : subscribers) {
			subscriber.onEvent(event);
		}
	}
	
	@Override
	public String toString() {
		return new ToStringBuilder(this).toString();
	}

}
