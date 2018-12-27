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

import java.io.Serializable;
import java.util.Set;

import com.exactpro.sf.bigbutton.importing.ImportError;

@SuppressWarnings("serial")
public class Executor implements Serializable {
	
	private String name;
	
	private String path;
	
    private long lineNumber;

	private Set<String> services;
	
	private SfApiOptions apiOptions;
	
	private int timeout;
	
	private Daemon daemon;

    private ImportError rejectCause;

	public String getHttpUrl() {
		
		String result = this.path;
		
		if(!result.startsWith("http")) {
			
			result = "http://" + result;
			
		}
		
		return result;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public Set<String> getServices() {
		return services;
	}

	public void setServices(Set<String> services) {
		this.services = services;
	}

	public SfApiOptions getApiOptions() {
		return apiOptions;
	}

	public void setApiOptions(SfApiOptions apiOptions) {
		this.apiOptions = apiOptions;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public Daemon getDaemon() {
		return daemon;
	}

	public void setDaemon(Daemon daemon) {
		this.daemon = daemon;
	}

    public boolean isRejected() {
        return this.rejectCause != null;
    }

    public void addRejectCause(ImportError cause) {
        if (!isRejected()) {
            setRejectCause(new ImportError(this.lineNumber, String.format("Executor \"%s\" : error", this.name)));
        }
        this.rejectCause.addCause(cause);
    }

    public ImportError getRejectCause() {
        return rejectCause;
    }

    public long getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(long lineNumber) {
        this.lineNumber = lineNumber;
    }

    public void setRejectCause(ImportError rejectCause) {
        this.rejectCause = rejectCause;
    }
}