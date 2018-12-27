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
package com.exactpro.sf.testwebgui.messages;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.csvreader.CsvWriter;

public class CsvMessageWriter {

	private List<String> columnHeaders;

	public CsvMessageWriter(List<String> columns, boolean includeRawMessage) {
		this.columnHeaders = columns;
		if(includeRawMessage) {
			this.columnHeaders.add("rawMessage");
		}
	}

	private String[] createHeader() {
		String[] headers = columnHeaders.toArray(new String[columnHeaders.size()]);
		return headers;
	}
	
	private String[] splitMessage(MessageAdapter message) {
		List<String> formatedMessage = new ArrayList<String>();
		if(columnHeaders.contains("timestamp")) formatedMessage.add(message.getTimestamp());
		if(columnHeaders.contains("name")) formatedMessage.add(message.getName());
		if(columnHeaders.contains("from")) formatedMessage.add(message.getFrom());
		if(columnHeaders.contains("to")) formatedMessage.add(message.getTo());
		if(columnHeaders.contains("content")) formatedMessage.add(message.getHumanReadable());
		if(columnHeaders.contains("rawMessage")) formatedMessage.add(message.getRawMessage());
		
		String[] result = formatedMessage.toArray(new String[formatedMessage.size()]);
		return result;
	}
	
	public void writeAndClose(File file, List<MessageAdapter> messages) throws IOException {
		
		CsvWriter writer = new CsvWriter(new FileWriter(file), ',');
		writer.writeRecord( createHeader() );
		for(MessageAdapter m : messages) {
			writer.writeRecord(splitMessage(m));
		}
		writer.close();
	}	
}
