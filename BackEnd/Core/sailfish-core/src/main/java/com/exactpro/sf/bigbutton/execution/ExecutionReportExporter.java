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
package com.exactpro.sf.bigbutton.execution;

import com.csvreader.CsvWriter;
import com.exactpro.sf.bigbutton.library.Script;
import com.exactpro.sf.bigbutton.library.ScriptList;

import java.io.File;
import java.io.IOException;

public class ExecutionReportExporter {

    private static final String[] headers = new String[] { "Script List", "Status", "Num. Passed", "Num. Cond. Passed", "Num. Failed" };
	
	private File resultFile; 
	
	private CsvWriter writer;
	
	private boolean closed = false;
	
	public ExecutionReportExporter() throws IOException {
		
		resultFile = File.createTempFile("BBReport", null);
		
		writer = new CsvWriter(resultFile.getAbsolutePath());
		
		writeHeaders();
		
	}
	
	private void writeHeaders() throws IOException {
		writer.writeRecord(headers);
	}
	
	private void writeEmptyRow() throws IOException {

        writer.writeRecord(new String[] { "", "", "", "", "" });
		
	}
	
	private void writeScriptRow(Script script) throws IOException {

        String[] row = new String[5];

        ScriptExecutionStatistics statistics = script.getStatistics();
		
		int index = 0;
		
		row[index++] = script.getShortName();
		row[index++] = statistics.getStatus();
		row[index++] = Long.toString(statistics.getNumPassed());
        row[index++] = Long.toString(statistics.getNumConditionallyPassed());
		row[index++] = Long.toString(statistics.getNumFailed());
		
		writer.writeRecord(row);
		
	}
	
	private void writeListRecord(ScriptList list) throws IOException {

        writer.writeRecord(new String[] { list.getName(), "", "", "", "" });
		
	}
	
	public void writeList(ScriptList list) throws IOException {
		
		if(closed) {
			return;
		}
		
		writeListRecord(list);
		
		for(Script script : list.getScripts()) {
			writeScriptRow(script);
		}
		
		writeEmptyRow();
		
	}
	
	public void writeCompleted() {
		
		writer.close();
		
		this.closed = true;
		
	}
	
	public File getFile() {
		
		return resultFile;
		
	}
	
}
