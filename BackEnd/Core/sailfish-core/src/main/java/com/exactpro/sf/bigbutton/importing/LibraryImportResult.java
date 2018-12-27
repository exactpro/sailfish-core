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
package com.exactpro.sf.bigbutton.importing;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import com.exactpro.sf.bigbutton.library.Library;

@XmlRootElement(name = "importresult")
@SuppressWarnings("serial")
public class LibraryImportResult implements Serializable {
	
	@XmlTransient
	private Library library;
	
	private long numExecutors = 0;
	
	private long numScripts = 0;
	
	private long numServices = 0;
	
	private long id;

    private Set<ImportError> commonErrors = new HashSet<>();

    private Set<ImportError> globalsErrors = new HashSet<>();

    private Set<ImportError> executorErrors = new HashSet<>();

    private Set<ImportError> scriptListErrors = new HashSet<>();
	
	public void incNumExecutors() {
		
		this.numExecutors++;
		
	}
	
	public void incNumScripts() {
		
		this.numScripts++;
		
	}

	public void incNumServices() {
	
		this.numServices++;
	
	}
	
	public long getNumExecutors() {
		return numExecutors;
	}

	public void setNumExecutors(long numExecutors) {
		this.numExecutors = numExecutors;
	}

	public long getNumScripts() {
		return numScripts;
	}

	public void setNumScripts(long numScripts) {
		this.numScripts = numScripts;
	}

	public long getNumServices() {
		return numServices;
	}

	public void setNumServices(long numServices) {
		this.numServices = numServices;
	}
	
	@XmlTransient
	public Library getLibrary() {
		return library;
	}

	public void setLibrary(Library library) {
		this.library = library;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

    public Set<ImportError> getExecutorErrors() {
        return executorErrors;
    }

    public Set<ImportError> getScriptListErrors() {
        return scriptListErrors;
    }

    public int getAllErrorsQty() {

        return executorErrors.size() + scriptListErrors.size() + globalsErrors.size() + commonErrors.size();
    }

    public Set<ImportError> getGlobalsErrors() {
        return globalsErrors;
    }

    public void setGlobalsErrors(Set<ImportError> globalsErrors) {
        this.globalsErrors = globalsErrors;
    }

    public int getCriticalErrorsQty() {

        return globalsErrors.size() + commonErrors.size();
    }

    public int getNonCriticalErrorsQty() {

        return executorErrors.size() + scriptListErrors.size();
    }

    public Set<ImportError> getCommonErrors() {
        return commonErrors;
    }

    public void setCommonErrors(Set<ImportError> commonErrors) {
        this.commonErrors = commonErrors;
    }
	
}