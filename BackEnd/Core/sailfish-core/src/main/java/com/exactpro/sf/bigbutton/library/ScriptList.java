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

import com.exactpro.sf.bigbutton.execution.ListExecutionStatistics;
import com.exactpro.sf.bigbutton.importing.ImportError;
import com.exactpro.sf.scriptrunner.StatusType;
import com.exactpro.sf.util.EMailUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@SuppressWarnings("serial")
public class ScriptList extends AbstractLibraryItem implements Comparable<ScriptList>, IBBActionExecutor {
    private static final Logger logger = LoggerFactory.getLogger(ScriptList.class);

    private final static String TABLE_HEADER = "<tr><th>Script list</th><th>Executed on</th><th>Status</th><th style=\"color:green;\">Passed</th><th style=\"color:orange;\">Conditionally Passed</th><th style=\"color:red;\">Failed</th></tr>";

    private final String name;

	private final String uiName;

    private final long lineNumber;

	private final List<Script> scripts = new ArrayList<>();

	private final Set<String> serviceLists;

	private final SfApiOptions apiOptions;

	private final long priority;

	private final ListExecutionStatistics executionStatistics = new ListExecutionStatistics();

	private final String executor;

	private volatile Script currentScript;

    private ImportError rejectCause;

    private volatile ScriptListStatus status = ScriptListStatus.INACTIVE;

    public enum ScriptListStatus{
        INACTIVE,
	    RUNNING,
        SKIPPED,
        EXECUTED;

	    public boolean isSkipped(){
	        return this.equals(SKIPPED);
        }

        public boolean isExecuted(){
            return this.equals(EXECUTED);
        }
    }

	public ScriptList(String name, String executor, Set<String> serviceLists,
            SfApiOptions apiOptions, long priority, long lineNumber) {
		super();
		this.executor = executor;
		this.name = name;
		this.serviceLists = serviceLists;
		this.apiOptions = apiOptions;
		this.priority = priority;
        this.lineNumber = lineNumber;
        this.uiName = this.executor != null ? (this.name + " > " + this.executor) : this.name;
	}

	@Override
    public void addNested(Script item) {

		this.scripts.add(item);

	}

	public String getName() {
		return name;
	}

	public String getUiName() {
        return uiName;
    }

	public String getExecutor() {
        return executor;
    }

	public List<Script> getScripts() {
		return scripts;
	}

	public Set<String> getServiceLists() {
		return serviceLists;
	}

	public SfApiOptions getApiOptions() {
		return apiOptions;
	}

	public long getPriority() {
		return priority;
	}

	public ListExecutionStatistics getExecutionStatistics() {
		return executionStatistics;
	}

	@Override
    public String toString() {
        return "ScriptList [name=" + name + "]";
	}

	public Script getCurrentScript() {
		return currentScript;
	}

	public void setCurrentScript(Script currentScript) {
		this.currentScript = currentScript;
	}

	@Override
	public int compareTo(ScriptList o) {

		return Long.compare(priority, o.priority);

	}

    public boolean isRejected() {
        return rejectCause != null;
    }

    public void addRejectCause(ImportError cause) {
        if (!isRejected()) {
            setRejectCause(new ImportError(this.lineNumber, String.format("Script List \"%s\" : error", this.name)));
        }
        this.rejectCause.addCause(cause);
    }

    public void addRejectCause(Collection<ImportError> errors) {
        if(errors.isEmpty()){
            return;
        }

        if (!isRejected()) {
            setRejectCause(new ImportError(this.lineNumber, String.format("Script List  \"%s\" : error", this.name)));
        }
        this.rejectCause.addCause(errors);
    }

    public ImportError getRejectCause() {
        return rejectCause;
    }

    public void setRejectCause(ImportError rejectCause) {
        this.rejectCause = rejectCause;
    }

    public long getLineNumber() {
        return lineNumber;
    }


    public ScriptListStatus getStatus() {
        return status;
    }

    public void setStatus(ScriptListStatus status) {
	    if(logger.isDebugEnabled()){
	        logger.debug("ScriptList {} change status from {} to {}.", name, this.status.name(), status.name());
        }
        this.status = status;
    }

    @Override
    @SuppressWarnings("incomplete-switch")
    public Set<BigButtonAction> getActions() {
        Set<BigButtonAction> actions;

        switch (getStatusType()){
        case FAILED:
            actions = apiOptions.getOnFailed();
            logger.info("Use failed script list actions {}", actions);
            break;
        case CONDITIONALLY_PASSED:
            actions = apiOptions.getOnCondPassed();
            logger.info("Use conditionally passed script list actions {}", actions);
            break;
        case PASSED:
            actions = apiOptions.getOnPassed();
            logger.info("Use passed script list actions {}", actions);
            break;
        default:
            actions = Collections.emptySet();
            break;
        }

        return  actions;
    }

    public StatusType getStatusType() {

        if (executionStatistics.getNumFailed() > 0) {
            return StatusType.FAILED;
        } else if (executionStatistics.getNumConditionallyPassed() > 0) {
            return StatusType.CONDITIONALLY_PASSED;
        } else if (executionStatistics.getSuccessPercent() == 100) {
            return StatusType.PASSED;
        }

        return StatusType.NA;
    }

    @Override
    public String toHtmlTable() {
        StringBuilder table = new StringBuilder();

        table.append(EMailUtil.BR);
        table.append(EMailUtil.TABLE);
        table.append(TABLE_HEADER);

        table.append(toHtmlTableRow());

        table.append(EMailUtil.CLOSE_TABLE);
        table.append(EMailUtil.BR);

        if(status.isExecuted()){
            table.append(Script.UNCLOSED_TABLE_HEADER);
            table.append(Script.OPTIONAL_TABLE_HEADER);
            table.append(EMailUtil.CLOSE_TR);
            for(IBBActionExecutor script : scripts){
                table.append(script.toHtmlTableRow());
            }
            table.append(EMailUtil.CLOSE_TABLE);
        }

        table.append(EMailUtil.BR);

        return table.toString();
    }

    @Override
    public String toHtmlTableRow() {

        StringBuilder row = new StringBuilder();

        row.append(EMailUtil.createTd(name));
        row.append(EMailUtil.createTd(executor));
        row.append(EMailUtil.createTd(status.name()));
        row.append(EMailUtil.createGreenTd(String.valueOf(executionStatistics.getNumPassed())));
        row.append(EMailUtil.createOrangeTd(String.valueOf(executionStatistics.getNumConditionallyPassed())));
        row.append(EMailUtil.createRedTd(String.valueOf(executionStatistics.getNumFailed())));


        return row.toString();
    }

}
