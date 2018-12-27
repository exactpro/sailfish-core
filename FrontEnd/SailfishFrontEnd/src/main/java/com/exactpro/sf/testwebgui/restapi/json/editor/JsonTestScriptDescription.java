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
package com.exactpro.sf.testwebgui.restapi.json.editor;

import java.util.List;

import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.embedded.statistics.entities.Tag;
import com.exactpro.sf.scriptrunner.IScriptProgress;
import com.exactpro.sf.scriptrunner.TestScriptDescription;
import com.exactpro.sf.scriptrunner.TestScriptDescription.ScriptState;
import com.exactpro.sf.scriptrunner.TestScriptDescription.ScriptStatus;
import com.exactpro.sf.testwebgui.restapi.editor.RunMatrixSettings;

public class JsonTestScriptDescription {

	private final long id;

	private final ScriptState state;
	private final ScriptStatus status;
	private final String description;
	private final Throwable cause;

	private final String matrixFileName;
	private final RunMatrixSettings settings;

	private final SailfishURI languageURI;
	private final String progress;

	private final long startedTime;
	private final long finishedTime;
	private final String pauseReason;
	private final long pauseTimeout;
	private final String username;
	private final List<Tag> tags;

	// From ScriptContext:
	private final List<String> serviceList;
	private final IScriptProgress scriptProgress;
	private final JsonMatrix messageContainer;

	public JsonTestScriptDescription(TestScriptDescription descr, JsonMatrix container) {
		this.id = descr.getId();
		this.state = descr.getState();
		this.status = descr.getStatus();
		this.description = descr.getDescription();
		this.cause = descr.getCause();

		this.matrixFileName = descr.getMatrixFileName();
		this.settings = new RunMatrixSettings();
		this.settings.setRange(descr.getRange());
		this.settings.setContinueOnFailed(descr.getContinueOnFailed());
		this.settings.setAutoStart(descr.getAutoStart());
		this.settings.setIgnoreAskForContinue(descr.isSuppressAskForContinue());
		this.settings.setEnvironment(descr.getContext().getEnvironmentName());
		this.settings.setEncoding(descr.getEncoding());
		this.settings.setAutoRun(false); // FIXME
		this.settings.setSkipOptional(descr.isSkipOptional());

		this.languageURI = descr.getLanguageURI();
		this.progress = descr.getProgress();

		this.startedTime = descr.getStartedTime();
		this.finishedTime = descr.getFinishedTime();
		this.pauseReason = descr.getPauseReason();
		this.pauseTimeout = descr.getPauseTimeout();
		this.username = descr.getUsername();
		this.tags = descr.getTags();

		this.serviceList = descr.getContext().getServiceList();
		this.scriptProgress = descr.getContext().getScriptProgress();
		this.messageContainer = container;
	}

	public long getId() {
		return id;
	}

	public ScriptState getState() {
		return state;
	}

	public ScriptStatus getStatus() {
		return status;
	}

	public String getDescription() {
		return description;
	}

	public Throwable getCause() {
		return cause;
	}

	public String getMatrixFileName() {
		return matrixFileName;
	}

	public RunMatrixSettings getSettings() {
		return settings;
	}

	public SailfishURI getLanguageURI() {
		return languageURI;
	}

	public String getProgress() {
		return progress;
	}

	public long getStartedTime() {
		return startedTime;
	}

	public long getFinishedTime() {
		return finishedTime;
	}

	public String getPauseReason() {
		return pauseReason;
	}

	public long getPauseTimeout() {
		return pauseTimeout;
	}

	public String getUsername() {
		return username;
	}

	public List<String> getServiceList() {
		return serviceList;
	}

	public IScriptProgress getScriptProgress() {
		return scriptProgress;
	}

	public JsonMatrix getMessageContainer() {
		return messageContainer;
	}

	public List<Tag> getTags() {
		return tags;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("TestScriptDescriptionJson [id=");
		builder.append(id);
		builder.append(", state=");
		builder.append(state);
		builder.append(", status=");
		builder.append(status);
		builder.append(", description=");
		builder.append(description);
		builder.append(", cause=");
		builder.append(cause);
		builder.append(", matrixFileName=");
		builder.append(matrixFileName);
		builder.append(", settings=");
		builder.append(settings);
		builder.append(", amlVersion=");
		builder.append(languageURI);
		builder.append(", progress=");
		builder.append(progress);
		builder.append(", startedTime=");
		builder.append(startedTime);
		builder.append(", finishedTime=");
		builder.append(finishedTime);
		builder.append(", pauseReason=");
		builder.append(pauseReason);
		builder.append(", pauseTimeout=");
		builder.append(pauseTimeout);
		builder.append(", username=");
		builder.append(username);
		builder.append(", tags=");
		builder.append(tags);
		builder.append(", serviceList=");
		builder.append(serviceList);
		builder.append(", scriptProgress=");
		builder.append(scriptProgress);
		builder.append(", messageContainer=");
		builder.append(messageContainer);
		builder.append("]");
		return builder.toString();
	}


}
