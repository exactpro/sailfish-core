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
package com.exactpro.sf.testwebgui.restapi.json;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LogMessage {

	private final String message;

	private final String level;

	private final String[] stacktrace;

	public LogMessage(@JsonProperty("message") String message, 
			@JsonProperty("level") String level, 
			@JsonProperty("stacktrace") String[] stacktrace) {
		super();
		this.message = message;
		this.level = level;
		this.stacktrace = stacktrace;
	}

	public String getMessage() {
		return message;
	}

	public String getLevel() {
		return level;
	}

	public String[] getStacktrace() {
		return stacktrace;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("LogMessage [message=");
		builder.append(message);
		builder.append(", level=");
		builder.append(level);
		builder.append(", stacktrace=");
		builder.append(Arrays.toString(stacktrace));
		builder.append("]");
		return builder.toString();
	}

}
