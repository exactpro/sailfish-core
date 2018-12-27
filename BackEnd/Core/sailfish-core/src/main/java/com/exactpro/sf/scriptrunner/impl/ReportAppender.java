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

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

import com.exactpro.sf.scriptrunner.LoggerRow;

public class ReportAppender extends AppenderSkeleton{

	public ReportAppender(){

		rows = new ArrayList<LoggerRow>();
		header = new ArrayList<String>();

		header.add("Timestamp");
		header.add("Level");
		header.add("Thread");
		header.add("Class");
		header.add("Message");
		header.add("StackTrace");
		setThreshold(org.apache.log4j.Level.WARN); //No debug and info messages

	}
	List<LoggerRow> rows;
	List<String> header;


	public List<LoggerRow> getRows() {
		return rows;
	}

	public void setRows(List<LoggerRow> rows) {
		this.rows = rows;
	}

	public List<String> getHeader() {
		return header;
	}

	public void setHeader(List<String> header) {
		this.header = header;
	}

	@Override
	public void close() {
		if(this.closed)
			return;
		this.closed = true;

	}

	@Override
	public boolean requiresLayout() {
		return false;
	}

	@Override
	protected void append(LoggingEvent arg0) {

		LoggerRow row = new LoggerRow();

		row.setTimestamp(arg0.timeStamp);		// Compiler says that getTimeStamp() is deprecated
		row.setLevel(arg0.getLevel());
		row.setThread(arg0.getThreadName());
		row.setClazz(arg0.getLoggerName());
		row.setMessage(arg0.getRenderedMessage());

		if(arg0.getThrowableInformation() != null){
			row.setEx((Exception)arg0.getThrowableInformation().getThrowable());
		}

		rows.add(row);
	}


}
