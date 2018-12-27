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

public class JsonError {

	public final static Object NO_ERROR = new JsonError("Success", "");
	
	private final String message;
	private final String rootCause;

	public JsonError(String message, String rootCause) {
		super();
		this.message = message;
		this.rootCause = rootCause;
	}

	public String getMessage() {
		return message;
	}

	public String getRootCause() {
		return rootCause;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("JsonError [message=");
		builder.append(message);
		builder.append(", rootCause=");
		builder.append(rootCause);
		builder.append("]");
		return builder.toString();
	}

}
