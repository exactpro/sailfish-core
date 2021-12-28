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
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

@Plugin(name = "WebLoggingAppender", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)
public class WebLoggingAppender extends AbstractAppender {

    private static final List<LogSubscriber> subscribers = new CopyOnWriteArrayList<LogSubscriber>();

    private WebLoggingAppender(String name, Filter filter, Layout layout) {
        super(name, filter, layout);
    }

    @PluginFactory
    public static WebLoggingAppender createAppender(
            @PluginAttribute("name") String name,
            @PluginElement("Filter") Filter filter,
            @PluginElement("Layout") Layout layout) {
        return new WebLoggingAppender(name,filter , layout);
    }

    public static void registerSubscriber(LogSubscriber subscriber) {
		subscribers.add(subscriber);
	}
	
	public static void unRegisterSubscriber(LogSubscriber subscriber) {
		subscribers.remove(subscriber);
	}

	@Override
    public void append(LogEvent event) {
        LogEvent immutableEvent = event.toImmutable();
		for (LogSubscriber subscriber : subscribers) {
			subscriber.onEvent(immutableEvent);
		}
	}
	
	@Override
	public String toString() {
		return new ToStringBuilder(this).toString();
	}

}
