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
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.bigbutton.execution.ScriptExecutionStatistics;
import com.exactpro.sf.bigbutton.importing.ImportError;
import com.exactpro.sf.scriptrunner.StatusType;
import com.exactpro.sf.util.EMailUtil;

@SuppressWarnings("serial")
public class Script implements Serializable, IBBActionExecutor {

    private static final Logger logger = LoggerFactory.getLogger(Script.class);

    public static final String UNCLOSED_TABLE_HEADER = "<table border=\"1\"><tr><th>Script</th><th>Status</th><th style=\"color:green;\">Passed</th><th style=\"color:orange;\">Conditionally Passed</th><th style=\" color:red; \">Failed</th>";
    public static final String OPTIONAL_TABLE_HEADER = "<th style=\" color:red; \">Cause</th>";
    private long lineNumber;

	private String path;
	
	private SfApiOptions apiOptions;
	
	private SfApiOptions originalApiOptions;
	
	private String shortName;
	
    private ImportError rejectCause;

    private boolean finished;
	
    private final ScriptExecutionStatistics statistics = new ScriptExecutionStatistics();

    private String cause;

    private ScriptRemoteInformation remoteInformation = null;

    public static class ScriptRemoteInformation {
        private long testScriptId;
        private long matrixId;

        public ScriptRemoteInformation(long testScriptId, long matrixId) {
            this.testScriptId = testScriptId;
            this.matrixId = matrixId;
        }

        public long getTestScriptId() {
            return testScriptId;
        }

        public void setTestScriptId(long testScriptId) {
            this.testScriptId = testScriptId;
        }

        public long getMatrixId() {
            return matrixId;
        }

        public void setMatrixId(long matrixId) {
            this.matrixId = matrixId;
        }
    }

	public Script(long lineNumber){
	    this.lineNumber = lineNumber;
    }
	
	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public SfApiOptions getApiOptions() {
		return apiOptions;
	}

	public void setApiOptions(SfApiOptions apiOptions) {
		this.apiOptions = apiOptions;
	}

	@Override
	public String toString() {
		return "Script [path=" + path + "]";
	}

    public ScriptExecutionStatistics getStatistics() {
		return statistics;
	}

	public String getShortName() {
		return shortName;
	}

	public void setShortName(String shortName) {
		this.shortName = shortName;
	}

	public SfApiOptions getOriginalApiOptions() {
		return originalApiOptions;
	}

	public void setOriginalApiOptions(SfApiOptions originalApiOptions) {
		this.originalApiOptions = originalApiOptions;
	}

	public boolean isFinished() {
		return finished;
	}

	public void setFinished(boolean finished) {
		this.finished = finished;
	}

    public boolean isRejected() {
        return rejectCause != null;
    }

    public ImportError getRejectCause() {
        return rejectCause;
    }

    public void setRejectCause(ImportError rejectCause) {
        this.rejectCause = rejectCause;
    }

    public void addRejectCause(ImportError cause) {
        if (!isRejected()) {
            setRejectCause(new ImportError(lineNumber, String.format("Script  \"%s\" : error", shortName)));
        }
        rejectCause.addCause(cause);
    }

    public void addRejectCause(Collection<ImportError> errors) {
        if(errors.isEmpty()){
            return;
        }

        if (!isRejected()) {
            setRejectCause(new ImportError(lineNumber, String.format("Script List  \"%s\" : error", shortName)));
        }
        rejectCause.addCause(errors);
    }

    public long getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(long lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getCause() {
        return cause;
    }

    public void setCause(String cause) {
        this.cause = cause;
    }

    public ScriptRemoteInformation getRemoteInformation() {
        return remoteInformation;
    }

    public void setRemoteInformation(ScriptRemoteInformation remoteInformation) {
        this.remoteInformation = remoteInformation;
    }

    @Override
    @SuppressWarnings("incomplete-switch")
    public Set<BigButtonAction> getActions() {
        Set<BigButtonAction> actions;

        switch (getStatusType()){
        case FAILED:
            actions = originalApiOptions.getOnFailed();
            logger.info("Use failed script actions {}", actions);
            return actions;
        case CONDITIONALLY_PASSED:
            actions = originalApiOptions.getOnCondPassed();
            logger.info("Use conditionally passed script actions {}", actions);
            return actions;
        case PASSED:
            actions = originalApiOptions.getOnPassed();
            logger.info("Use passed script actions {}", actions);
            return actions;
        default:
            return Collections.emptySet();
        }

    }

    public StatusType getStatusType() {
        if (statistics.getNumFailed() > 0 || statistics.isExecutionFailed()) {
            return StatusType.FAILED;
        } else if (statistics.getNumConditionallyPassed() > 0) {
            return StatusType.CONDITIONALLY_PASSED;
        } else if (statistics.getNumPassed() == statistics.getTotal()) {
            return StatusType.PASSED;
        }

        return StatusType.NA;
    }

    @Override
    public String toHtmlTable() {
        StringBuilder table = new StringBuilder();

        table.append(EMailUtil.BR);
        table.append(UNCLOSED_TABLE_HEADER);

        if (cause != null) {
            table.append(OPTIONAL_TABLE_HEADER);
        }

        table.append(EMailUtil.CLOSE_TR);
        table.append(toHtmlTableRow());
        table.append(EMailUtil.CLOSE_TABLE);
        table.append(EMailUtil.BR);

        return table.toString();
    }

    @Override
    public String toHtmlTableRow() {
        StringBuilder row = new StringBuilder();

        row.append(EMailUtil.TR);
        row.append(EMailUtil.createTd(shortName));
        row.append(EMailUtil.createTd(statistics.getStatus()));
        row.append(EMailUtil.createGreenTd(String.valueOf(statistics.getNumPassed())));
        row.append(EMailUtil.createOrangeTd(String.valueOf(statistics.getNumConditionallyPassed())));
        row.append(EMailUtil.createRedTd(String.valueOf(statistics.getNumFailed())));

        if (cause != null) {
            row.append(EMailUtil.createRedTd(cause));
        }

        row.append(EMailUtil.CLOSE_TR);

        return row.toString();
    }
}