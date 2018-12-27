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
package com.exactpro.sf.configuration.recorder;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;

@SuppressWarnings("serial")
public class FlightRecorderOptions implements Serializable {
	
	private static final String STORAGE_PREFIX = "flightrecorder.";
	
	private String recordsFolder;
	
	private String jdkPath;
	
	public void fillFromMap(Map<String, String> options) throws Exception {
		
		for(Map.Entry<String, String> entry : options.entrySet()) {
			
			if(entry.getKey().startsWith(STORAGE_PREFIX)) {
				
				BeanUtils.setProperty(this, entry.getKey().replace(STORAGE_PREFIX, ""), entry.getValue());
				
			}
			
		}
		
	}
	
	public Map<String, String> toMap() throws Exception {
		
		@SuppressWarnings("unchecked")
		Map<String, String> description = BeanUtils.describe(this);
		
		Map<String, String> result = new HashMap<String, String>();
		
		for(Map.Entry<String, String> entry : description.entrySet()) {
			
			if(!entry.getKey().equals("class")) {
			
				result.put(STORAGE_PREFIX + entry.getKey(), entry.getValue());
			
			}
			
		}
		
		return result;
		
	}
	

	public String getRecordsFolder() {
		return recordsFolder;
	}

	public void setRecordsFolder(String recordsFolder) {
		this.recordsFolder = recordsFolder;
	}

	public String getJdkPath() {
		return jdkPath;
	}

	public void setJdkPath(String jdkPath) {
		this.jdkPath = jdkPath;
	}
	
}
