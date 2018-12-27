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

public class TestScriptRun {
	
	private int id;
	private String description;
	private String matrixName;
	private int passed;
	private int failed;
	private int total;
	private String problem;
	private String cause;
	private String state;
	private String status;
	
	public TestScriptRun(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}
	
	public String getDescription() {
		return description;
	}

	protected void setDescription(String description) {
		this.description = description;
	}

	public String getMatrixName() {
		return matrixName;
	}

	protected void setMatrixName(String matrixName) {
		this.matrixName = matrixName;
	}
	
	public int getPassed() {
		return passed;
	}

	protected void setPassed(int passed) {
		this.passed = passed;
	}

	public int getFailed() {
		return failed;
	}

	protected void setFailed(int failed) {
		this.failed = failed;
	}

	public int getTotal() {
		return total;
	}

	protected void setTotal(int total) {
		this.total = total;
	}

	public String getProblem() {
		return problem;
	}

	protected void setProblem(String problem) {
		this.problem = problem;
	}

	public String getCause() {
		return cause;
	}

	protected void setCause(String cause) {
		this.cause = cause;
	}

	public String getState() {
		return state;
	}

	protected void setState(String state) {
		this.state = state;
	}

	public String getStatus() {
		return status;
	}

	protected void setStatus(String status) {
		this.status = status;
	}
	
	// Static members

	public static enum State {
		FINISHED,
		CANCELLED,
		READY,
		RUNNING,
		PREPARING,
		INITIAL
	}
	
	public static enum Status {
		INIT_FAILED,
		PREPARING,
		EXECUTED,
		NONE
	}
	
	protected static TestScriptRun fromXml(Node n) {
		Element el = (Element)n;
		
		TestScriptRun run = new TestScriptRun(Integer.parseInt(Util.getTextContent(el, "id")));
		fillFromXml(el, run);
		
		return run;
	}
	
	protected static void fillFromXml(Element el, TestScriptRun run) {
		String val = Util.getTextContent(el, "description");
		if (val != null)
			run.description = val;
		
		val = Util.getTextContent(el, "matrixFileName");
		if (val != null)
			run.matrixName = val;
		
		val = Util.getTextContent(el, "scriptState");
		if (val != null)
			run.state = val;
		
		val = Util.getTextContent(el, "scriptStatus");
		if (val != null)
			run.status = val;
		
		val = Util.getTextContent(el, "passed");
		if (val != null)
			run.passed = Integer.parseInt(val);
		
		val = Util.getTextContent(el, "failed");
		if (val != null)
			run.failed = Integer.parseInt(val);
		
		val = Util.getTextContent(el, "total");
		if (val != null)
			run.total = Integer.parseInt(val);
		
		val = Util.getTextContent(el, "problem");
		if (val != null)
			run.problem = val;
		
		val = Util.getTextContent(el, "cause");
		if (val != null)
			run.cause = val;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("TestScriptRun [id=");
		builder.append(id);
		builder.append(", description=");
		builder.append(description);
		builder.append(", matrixName=");
		builder.append(matrixName);
		builder.append(", passed=");
		builder.append(passed);
		builder.append(", failed=");
		builder.append(failed);
		builder.append(", total=");
		builder.append(total);
		builder.append(", problem=");
		builder.append(problem);
		builder.append(", cause=");
		builder.append(cause);
		builder.append(", state=");
		builder.append(state);
		builder.append(", status=");
		builder.append(status);
		builder.append("]");
		return builder.toString();
	}
	
}
