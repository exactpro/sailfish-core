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
package com.exactpro.sf.testwebgui.general;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.Map;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import javax.servlet.ServletException;

@ManagedBean(name = "errorBean")
@RequestScoped
@SuppressWarnings("serial")
public class ErrorBean implements Serializable {
	
	private Map<String, Object> request;
	
	public ErrorBean() {
		
		FacesContext context = FacesContext.getCurrentInstance();
		request	= context.getExternalContext().getRequestMap();
		
	}
	
	public Integer getStatusCode() {
		return (Integer)request.get("javax.servlet.error.status_code");
	}
	
	public String getErrorMessage() {
		return (String)request.get("javax.servlet.error.message");
	}
	
	public String getRequestUri() {
		return (String)request.get("javax.servlet.error.request_uri");
	}
	
	public String getExceptionClass() {
		return request.get("javax.servlet.error.exception_type").toString();
	}

	public String getStackTrace() {		

		Throwable ex = (Throwable) request.get("javax.servlet.error.exception");

		StringWriter sw = new StringWriter();

		PrintWriter pw = new PrintWriter(sw);

		fillStackTrace(ex, pw);

		return sw.toString();

	}

	private static void fillStackTrace(Throwable t, PrintWriter w) {

		if (t == null) {
			return;
		}

		t.printStackTrace(w);

		if (t instanceof ServletException) {

			Throwable cause = ((ServletException) t).getRootCause();

			if (cause != null) {

				w.println("Root cause:");

				fillStackTrace(cause, w);

			}

		} else if (t instanceof SQLException) {

			Throwable cause = ((SQLException) t).getNextException();

			if (cause != null) {

				w.println("Next exception:");

				fillStackTrace(cause, w);

			}

		} else {

			Throwable cause = t.getCause();

			if (cause != null) {

				w.println("Cause:");

				fillStackTrace(cause, w);

			}

		}

	}
	
}