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

import com.exactpro.sf.bigbutton.library.Executor;

import java.io.Serializable;

@SuppressWarnings("serial")
public class ListExecutionStatistics implements Serializable {

    private volatile boolean rejected;
	
	private volatile long numPassed;

    private volatile long numConditionallyPassed;
	
	private volatile long numFailed;

	private volatile int executionPercent;
	
	private volatile int successPercent;
	
	private volatile Executor executor;
	
	public long getNumPassed() {
		return numPassed;
	}

    public void incNumPassed() {
        this.numPassed++;
	}

	public long getNumFailed() {
		return numFailed;
	}

    public void incNumFailed() {
        this.numFailed++;
	}

	public int getExecutionPercent() {
		return executionPercent;
	}

	public void setExecutionPercent(int executionPercent) {
		this.executionPercent = executionPercent;
	}

	public Executor getExecutor() {
		return executor;
	}

	public void setExecutor(Executor executor) {
		this.executor = executor;
	}

	public int getSuccessPercent() {
		return successPercent;
	}

	public void setSuccessPercent(int successPercent) {
		this.successPercent = successPercent;
	}

    public boolean isRejected() {
        return rejected;
    }

    public void setRejected(boolean rejected) {
        this.rejected = rejected;
    }

    public long getNumConditionallyPassed() {
        return numConditionallyPassed;
    }

    public void incNumConditionallyPassed() {
        this.numConditionallyPassed++;
    }
	
}
