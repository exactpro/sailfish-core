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

import com.exactpro.sf.bigbutton.importing.ImportError;

@SuppressWarnings("serial")
public class Service implements Serializable {
	
    private final String path;

    private StartMode startMode;

    private String name;

    private ImportError rejectCause;

    private long recordNumber;

    public Service(String path) {
        this.path = path;
    }
	
	public String getPath() {
		return path;
	}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Service other = (Service) obj;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		return true;
	}
	
	public StartMode getStartMode() {
        return startMode;
    }

    public void setStartMode(StartMode startMode) {
        this.startMode = startMode;
    }
	
	@Override
	public String toString() {
		return "Service [path=" + path + "]";
	}

    public boolean isRejected() {
        return rejectCause != null;
    }

    public long getRecordNumber() {
        return recordNumber;
    }

    public void setRecordNumber(long recordNumber) {
        this.recordNumber = recordNumber;
    }

    public ImportError getRejectCause() {
        return rejectCause;
    }

    public void setRejectCause(ImportError rejectCause) {
        this.rejectCause = rejectCause;
    }
	
}
