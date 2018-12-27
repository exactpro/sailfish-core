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
package com.exactpro.sf;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class Response {
	
	private int id;
	private String message;
	private String cause;
	
	public Response(String message) {
		this.message = message;
	}
	
	public String getMessage() {
		return message;
	}
	
	public String getCause() {
		return cause;
	}
	
	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	// Static members
	
	protected static Response fromXml(Node n) {
		Element el = (Element)n;
		
		Response res = new Response(Util.getTextContent(el, "message"));
		res.cause = Util.getTextContent(el, "rootCause");
		String idString = Util.getTextContent(el, "id");
		if(idString != null) {
			res.id = Integer.parseInt(idString);
		}
		return res;
	}	
	
}
