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

public class RunMatrixSettings {

	private String range;
	private boolean continueOnFailed;
	private boolean autoStart;
	private boolean ignoreAskForContinue;
	private String environment;
	private String encoding;
	private boolean autoRun;
	private boolean runNetDumper;
	private boolean skipOptional;

	public String getRange() {
		return range;
	}

	public void setRange(String range) {
		this.range = range;
	}

	public boolean isContinueOnFailed() {
		return continueOnFailed;
	}

	public void setContinueOnFailed(boolean continueOnFailed) {
		this.continueOnFailed = continueOnFailed;
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

	public String getEnvironment() {
		return environment;
	}

	public void setEnvironment(String environment) {
		this.environment = environment;
	}

	public String getEncoding() {
		return encoding;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	public boolean isAutoRun() {
		return autoRun;
	}

	public void setAutoRun(boolean isAutoRun) {
		this.autoRun = isAutoRun;
	}

    public boolean isSkipOptional() {
        return skipOptional;
    }

    public void setSkipOptional(boolean skipOptional) {
        this.skipOptional = skipOptional;
    }

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("RunMatrixSettings [range=");
		builder.append(range);
		builder.append(", continueOnFailed=");
		builder.append(continueOnFailed);
		builder.append(", autoStart=");
		builder.append(autoStart);
		builder.append(", ignoreAskForContinue=");
		builder.append(ignoreAskForContinue);
		builder.append(", environment=");
		builder.append(environment);
		builder.append(", encoding=");
		builder.append(encoding);
		builder.append(", autoRun=");
		builder.append(autoRun);
		builder.append(", skipOptional=");
		builder.append(skipOptional);
		builder.append("]");
		return builder.toString();
	}

	public boolean isRunNetDumper() {
		return runNetDumper;
	}

	public void setRunNetDumper(boolean runNetDumper) {
		this.runNetDumper = runNetDumper;
	}
}
