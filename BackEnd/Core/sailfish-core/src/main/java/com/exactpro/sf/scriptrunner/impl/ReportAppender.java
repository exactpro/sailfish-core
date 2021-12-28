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

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Filter.Result;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.filter.ThresholdFilter;

import com.exactpro.sf.scriptrunner.LoggerRow;

@Plugin(name = "Reportlog", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = false)
public class ReportAppender extends AbstractAppender {

    private final List<LoggerRow> rows = new ArrayList<>();
    private final List<String> header = new ArrayList<>();

    protected ReportAppender(String name, Filter filter) {
        super(name, filter, null, true, Property.EMPTY_ARRAY);

        header.add("Timestamp");
        header.add("Level");
        header.add("Thread");
        header.add("Class");
        header.add("Message");
        header.add("StackTrace");
    }

    @PluginFactory
    public static ReportAppender createAppender() {
        return new ReportAppender("ReportAppender", ThresholdFilter.createFilter(Level.WARN, Result.ACCEPT, Result.DENY));
    }


	public List<LoggerRow> getRows() {
		return rows;
	}

	public List<String> getHeader() {
		return header;
	}

    @Override
    public void append(LogEvent event) {
        LoggerRow row = new LoggerRow();

        row.setTimestamp(event.getTimeMillis());		// Compiler says that getTimeStamp() is deprecated
        row.setLevel(event.getLevel());
        row.setThread(event.getThreadName());
        row.setClazz(event.getLoggerName());
        row.setMessage(event.getMessage().getFormattedMessage());
        row.setThrowable(event.getMessage().getThrowable());
        rows.add(row);
    }
}
