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
package com.exactpro.sf.testwebgui.notifications.scriptrunner;

import com.exactpro.sf.scriptrunner.TestScriptDescription.ScriptState;
import com.exactpro.sf.scriptrunner.TestScriptDescription.ScriptStatus;
import com.exactprosystems.webchannels.AbstractUpdateEvent;

public class ScriptrunUpdateEvent implements AbstractUpdateEvent {
	
	private String data;
	private String divId;
	private Long scriptRunId;
	private ScriptState state;
	private ScriptStatus status;
	
	public ScriptrunUpdateEvent(String data){
		this.data = data;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public String getDivId() {
		return divId;
	}

	public void setDivId(String divId) {
		this.divId = divId;
	}

	public Long getScriptRunId() {
		return scriptRunId;
	}

	public void setScriptRunId(Long scriptRunId) {
		this.scriptRunId = scriptRunId;
	}

	public ScriptState getState() {
		return state;
	}

	public void setState(ScriptState state) {
		this.state = state;
	}

	public ScriptStatus getStatus() {
		return status;
	}

	public void setStatus(ScriptStatus status) {
		this.status = status;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ScriptrunUpdateEvent [data=");
		builder.append(data);
		builder.append(", divId=");
		builder.append(divId);
		builder.append(", scriptRunId=");
		builder.append(scriptRunId);
		builder.append(", state=");
		builder.append(state);
		builder.append(", status=");
		builder.append(status);
		builder.append("]");
		return builder.toString();
	}

}
