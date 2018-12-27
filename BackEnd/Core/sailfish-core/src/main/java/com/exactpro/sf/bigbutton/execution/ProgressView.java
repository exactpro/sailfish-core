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

import com.exactpro.sf.bigbutton.importing.ErrorType;
import com.exactpro.sf.bigbutton.importing.ImportError;
import com.exactpro.sf.bigbutton.library.Executor;
import com.exactpro.sf.bigbutton.library.Library;
import com.exactpro.sf.bigbutton.library.ScriptList;
import com.exactpro.sf.scriptrunner.StatusType;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("serial")
public class ProgressView implements Serializable {
	
	private List<ScriptList> enqueued = new ArrayList<>();
	
    private List<ScriptList> rejected = new ArrayList<>();

	private long numInQueue;
	
    private long numRejected;

	private Map<Executor, List<ScriptList>> executed = new HashMap<>();
	
	private Map<Executor, Long> numExecuted;
	
	private Map<Executor, ScriptList> running  = new HashMap<>();
	
	private int currentTotalProgressPercent;
	
	private List<ExecutorClient> allExecutors;
	
	private boolean finished;
	
	private BigButtonExecutionStatus status;

    private StatusType executionStatus;
	
	private String errorText;
	
    private List<String> warns;

	private File reportFile;
	
	private BbExecutionStatistics executionStatistics;
	
	private String libraryFileName;
	
	private Library library;

    private Map<ErrorType, Set<ImportError>> importErrors;

	public List<ScriptList> getEnqueued() {
		return enqueued;
	}

	public void setEnqueued(List<ScriptList> enqueued) {
		this.enqueued = enqueued;
	}

	public Map<Executor, List<ScriptList>> getExecuted() {
		return executed;
	}

	public void setExecuted(Map<Executor, List<ScriptList>> executed) {
		this.executed = executed;
	}

	public Map<Executor, ScriptList> getRunning() {
		return running;
	}

	public void setRunning(Map<Executor, ScriptList> running) {
		this.running = running;
	}

	public int getCurrentTotalProgressPercent() {
		return currentTotalProgressPercent;
	}

	public void setCurrentTotalProgressPercent(int currentTotalProgressPercent) {
		this.currentTotalProgressPercent = currentTotalProgressPercent;
	}

	public long getNumInQueue() {
		return numInQueue;
	}

	public void setNumInQueue(long numInQueue) {
		this.numInQueue = numInQueue;
	}

	public Map<Executor, Long> getNumExecuted() {
		return numExecuted;
	}

	public void setNumExecuted(Map<Executor, Long> numExecuted) {
		this.numExecuted = numExecuted;
	}

	public List<ExecutorClient> getAllExecutors() {
		return allExecutors;
	}

	public void setAllExecutors(List<ExecutorClient> allExecutors) {
		this.allExecutors = allExecutors;
	}

	public boolean isFinished() {
		return finished;
	}

	public void setFinished(boolean finished) {
		this.finished = finished;
	}

	public BigButtonExecutionStatus getStatus() {
		return status;
	}

	public void setStatus(BigButtonExecutionStatus status) {
		this.status = status;
	}

	public String getErrorText() {
		return errorText;
	}

	public void setErrorText(String errorText) {
		this.errorText = errorText;
	}

	public File getReportFile() {
		return reportFile;
	}

	public void setReportFile(File reportFile) {
		this.reportFile = reportFile;
	}

	public BbExecutionStatistics getExecutionStatistics() {
		return executionStatistics;
	}

	public void setExecutionStatistics(BbExecutionStatistics executionStatistics) {
		this.executionStatistics = executionStatistics;
	}

	public String getLibraryFileName() {
		return libraryFileName;
	}

	public void setLibraryFileName(String libraryFileName) {
		this.libraryFileName = libraryFileName;
	}

	public Library getLibrary() {
		return library;
	}

	public void setLibrary(Library library) {
		this.library = library;
	}

    public List<String> getWarns() {
        return warns;
    }

    public void setWarns(List<String> warns) {
        this.warns = warns;
    }

    public List<ScriptList> getRejected() {
        return rejected;
    }

    public void setRejected(List<ScriptList> rejected) {
        this.rejected = rejected;
    }

    public long getNumRejected() {
        return numRejected;
    }

    public void setNumRejected(long numRejected) {
        this.numRejected = numRejected;
    }

    public Map<ErrorType, Set<ImportError>> getImportErrors() {
        return importErrors;
    }

    public void setImportErrors(Map<ErrorType, Set<ImportError>> importErrors) {
        this.importErrors = importErrors;
    }

    public StatusType getExecutionStatus() {
        return executionStatus;
    }

    public void setExecutionStatus(StatusType executionStatus) {
        this.executionStatus = executionStatus;
    }
}
