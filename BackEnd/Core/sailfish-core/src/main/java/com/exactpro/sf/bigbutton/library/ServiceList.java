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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings("serial")
public class ServiceList extends AbstractLibraryItem implements Serializable {
	
	private String name;
	
    private long lineNumber;

	private StartMode startMode;
	
    private ImportError rejectCause;

	private List<Service> services = new ArrayList<>();
    private Set<String> serviceNames = new LinkedHashSet<>();

    @Override
	public void addNested(Service item) {
        if ( !serviceNames.add(item.getName())) {
            if (!isRejected()) {
                this.rejectCause = new ImportError(this.lineNumber, String.format("Service List \"%s\" error", this.name));
            }
            addRejectCause(new ImportError(item.getRecordNumber(), "Duplicate service with [" + item.getName() + "] name"));
        }

        this.services.add(item);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<Service> getServices() {
		return services;
	}

	public void setServices(List<Service> services) {
		this.services = services;
	}

    public StartMode getStartMode() {
        return startMode;
    }

    public void setStartMode(StartMode startMode) {
        this.startMode = startMode;
    }

    public long getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(long lineNumber) {
        this.lineNumber = lineNumber;
    }

    public boolean isRejected() {
        return this.rejectCause != null;
    }

    public void addRejectCause(ImportError error) {
        this.rejectCause.addCause(error);
    }

    public ImportError getRejectCause() {
        return rejectCause;
    }

    public void setRejectCause(ImportError rejectCause) {
        this.rejectCause = rejectCause;
    }
	
}
