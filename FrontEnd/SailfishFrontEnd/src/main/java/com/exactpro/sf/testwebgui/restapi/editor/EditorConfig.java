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
package com.exactpro.sf.testwebgui.restapi.editor;

import java.io.Serializable;

@SuppressWarnings("serial")
public class EditorConfig implements Serializable {

	private String encoding;

	private String environment;
	
	private boolean continueIfFailed = false;
	
	private boolean autoStart = false;
	
	private boolean ignoreAskForContinue = false;
	
	private boolean showAllMessages = false;

	public boolean isContinueIfFailed() {
		return continueIfFailed;
	}

	public void setContinueIfFailed(boolean continueIfFailed) {
		this.continueIfFailed = continueIfFailed;
	}

	public boolean isAutoStart() {
		return autoStart;
	}

	public void setAutoStart(boolean autoStart) {
		this.autoStart = autoStart;
	}

	public boolean isIgnoreAskForContinue() {
		return ignoreAskForContinue;
	}

	public void setIgnoreAskForContinue(boolean ignoreAskForContinue) {
		this.ignoreAskForContinue = ignoreAskForContinue;
	}

	public boolean isShowAllMessages() {
		return showAllMessages;
	}

	public void setShowAllMessages(boolean showAllMessages) {
		this.showAllMessages = showAllMessages;
	}

	public String getEncoding() {
		return encoding;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	public String getEnvironment() {
		return environment;
	}

	public void setEnvironment(String environment) {
		this.environment = environment;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("EditorConfig [encoding=");
		builder.append(encoding);
		builder.append(", environment=");
		builder.append(environment);
		builder.append(", continueIfFailed=");
		builder.append(continueIfFailed);
		builder.append(", autoStart=");
		builder.append(autoStart);
		builder.append(", ignoreAskForContinue=");
		builder.append(ignoreAskForContinue);
		builder.append(", showAllMessages=");
		builder.append(showAllMessages);
		builder.append("]");
		return builder.toString();
	}

}
