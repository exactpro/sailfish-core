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
package com.exactpro.sf.embedded.statistics.storage;

import java.io.Serializable;

import com.exactpro.sf.embedded.statistics.entities.TestCaseRunStatus;

@SuppressWarnings("serial")
public class TestCaseRunComments implements Serializable {
	
	private String comment;
	
	private String fixedVersion;
	
	private TestCaseRunStatus status;
	
	public TestCaseRunComments() {
		
	}

	public TestCaseRunComments(TestCaseRunComments toCopy) {
		
		this.comment = toCopy.getComment();
		this.fixedVersion = toCopy.getFixedVersion();
		this.status = toCopy.getStatus();
		
	}
	
	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public String getFixedVersion() {
		return fixedVersion;
	}

	public void setFixedVersion(String fixedVersion) {
		this.fixedVersion = fixedVersion;
	}

	public TestCaseRunStatus getStatus() {
		return status;
	}

	public void setStatus(TestCaseRunStatus status) {
		this.status = status;
	}
	
}
