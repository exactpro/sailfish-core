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
package com.exactpro.sf.testwebgui.restapi.xml;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "response")
public class XmlResponse {

	private String message;
	private String rootCause;
	
	public XmlResponse() {
		
	}

	public XmlResponse(String message, String rootCause) {
		this.message = message;
		this.rootCause = rootCause;
	}
	
	public XmlResponse(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}

	
	public void setMessage(String message) {
		this.message = message;
	}

	
	public String getRootCause() {
		return rootCause;
	}

	
	public void setRootCause(String rootCause) {
		this.rootCause = rootCause;
	}
	
}