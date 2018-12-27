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

import java.io.Serializable;

@SuppressWarnings("serial")
public class ScriptExecutionStatistics implements Serializable {
	
	private volatile long numPassed;
	
    private volatile long numConditionallyPassed;
	
	private volatile long numFailed;
	
    private volatile long total;

	private volatile String status;
	
	public boolean isExecutionFailed() {
		return (status != null && (status.equals("INIT_FAILED") || status.equals("RUN_FAILED") || status.equals("CONNECTION_FAILED")));
	}

	public long getNumPassed() {
		return numPassed;
	}

	public void setNumPassed(long numPassed) {
		this.numPassed = numPassed;
	}

	public long getNumFailed() {
		return numFailed;
	}

	public void setNumFailed(long numFailed) {
		this.numFailed = numFailed;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
	
    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getNumConditionallyPassed() {
        return numConditionallyPassed;
    }

    public void setNumConditionallyPassed(long numConditionallyPassed) {
        this.numConditionallyPassed = numConditionallyPassed;
    }
}
