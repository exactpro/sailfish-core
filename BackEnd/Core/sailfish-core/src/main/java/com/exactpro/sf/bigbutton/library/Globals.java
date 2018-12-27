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
package com.exactpro.sf.bigbutton.library;

import com.exactpro.sf.bigbutton.importing.ImportError;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("serial")
public class Globals implements Serializable {
	
    private long lineNumber;

    private SfApiOptions apiOptions;
	
	private Set<String> serviceLists;

    private Set<ImportError> rejectCause = new HashSet<>();

	public SfApiOptions getApiOptions() {
		return apiOptions;
	}


	public void setApiOptions(SfApiOptions apiOptions) {
		this.apiOptions = apiOptions;
	}

    public Set<String> getServiceLists() {
		return serviceLists;
	}

	public void setServiceLists(Set<String> serviceLists) {
		this.serviceLists = serviceLists;
	}

    public boolean isRejected() {
        return !this.rejectCause.isEmpty();
    }

    public void addRejectCause(ImportError error) {
        this.rejectCause.add(error);
    }

    public Set<ImportError> getRejectCause() {
        return rejectCause;
    }

    public void setRejectCause(Set<ImportError> rejectCause) {
        this.rejectCause = rejectCause;
    }

    public long getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(long lineNumber) {
        this.lineNumber = lineNumber;
    }
	
}
