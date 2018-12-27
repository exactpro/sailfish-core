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

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "testscriptrun")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = { "description", "matrixFileName", "id", "scriptState", "scriptStatus", "passed", "conditionallyPassed", "failed", "total",
        "problem", "cause", "testcases", "workFolder", "subFolder", "locked" })
public class XmlTestscriptRunDescription {

	private long id;
	
	private long passed;

    private long conditionallyPassed;

	private long failed;

    private long total;
	
	private String description;
	
	private String matrixFileName;
	
	private String scriptState;
	
	private String scriptStatus;
	
	private String problem;
	
	private String cause;
	
	private List<XmlTestCaseDescription> testcases;
	
	private String workFolder; 

	private String subFolder;

    private boolean locked;

	public String getWorkFolder(){
		return workFolder;
	}
	
	public void setWorkFolder(String workFolder){
		this.workFolder=workFolder;
	}
	
	public String getSubFolder(){
		return subFolder;
	}
	
	public void setSubFolder(String subFolder){
		this.subFolder=subFolder;
	}
	
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getScriptState() {
		return scriptState;
	}

	public void setScriptState(String scriptState) {
		this.scriptState = scriptState;
	}

	public String getScriptStatus() {
		return scriptStatus;
	}

	public void setScriptStatus(String scriptStatus) {
		this.scriptStatus = scriptStatus;
	}

	public String getProblem() {
		return problem;
	}

	public void setProblem(String problem) {
		this.problem = problem;
	}

	public String getCause() {
		return cause;
	}

	public void setCause(String cause) {
		this.cause = cause;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getMatrixFileName() {
		return matrixFileName;
	}

	public void setMatrixFileName(String matrixFileName) {
		this.matrixFileName = matrixFileName;
	}

    public long getPassed() {
        return passed;
    }

    public void setPassed(long passed) {
        this.passed = passed;
    }

    public long getFailed() {
        return failed;
    }

    public void setFailed(long failed) {
        this.failed = failed;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public List<XmlTestCaseDescription> getTestcases() {
        return testcases;
    }

    public void setTestcases(List<XmlTestCaseDescription> testcases) {
        this.testcases = testcases;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public long getConditionallyPassed() {
        return conditionallyPassed;
    }

    public void setConditionallyPassed(long conditionallyPassed) {
        this.conditionallyPassed = conditionallyPassed;
    }
}
