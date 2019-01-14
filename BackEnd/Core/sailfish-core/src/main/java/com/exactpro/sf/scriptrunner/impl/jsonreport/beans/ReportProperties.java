/*******************************************************************************
 * Copyright 2009-2019 Exactpro (Exactpro Systems Limited)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 ******************************************************************************/

package com.exactpro.sf.scriptrunner.impl.jsonreport.beans;

import com.exactpro.sf.scriptrunner.TestScriptDescription.ScriptState;
import com.exactpro.sf.scriptrunner.TestScriptDescription.ScriptStatus;

public class ReportProperties {
    private ScriptState state;
    private ScriptStatus status;
    private String matrixFile;
    private long timestamp;
    private String environmentNameAttr;
    private String languageURI;
    private String workFolder;
    private long passed;
    private long conditionallyPassed;
    private long failed;
    private long total;
    private String username;
    private long startTime;
    private long finishTime;
    private String services;
    private String range;
    private boolean autostart;
    private String cause;

    public ReportProperties(ScriptState state, ScriptStatus status, String matrixFile, long timestamp, String environmentNameAttr, String languageURI,
            String workFolder, long passed, long conditionallyPassed, long failed, long total, String username, long startTime,
            long finishTime, String services, String range, boolean autostart, String cause) {

        this.state = state;
        this.status = status;
        this.matrixFile = matrixFile;
        this.timestamp = timestamp;
        this.environmentNameAttr = environmentNameAttr;
        this.languageURI = languageURI;
        this.workFolder = workFolder;
        this.passed = passed;
        this.conditionallyPassed = conditionallyPassed;
        this.failed = failed;
        this.total = total;
        this.username = username;
        this.startTime = startTime;
        this.finishTime = finishTime;
        this.services = services;
        this.range = range;
        this.autostart = autostart;
        this.cause = cause;
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

    public String getMatrixFile() {
        return matrixFile;
    }

    public void setMatrixFile(String matrixFile) {
        this.matrixFile = matrixFile;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getEnvironmentNameAttr() {
        return environmentNameAttr;
    }

    public void setEnvironmentNameAttr(String environmentNameAttr) {
        this.environmentNameAttr = environmentNameAttr;
    }

    public String getLanguageURI() {
        return languageURI;
    }

    public void setLanguageURI(String languageURI) {
        this.languageURI = languageURI;
    }

    public String getWorkFolder() {
        return workFolder;
    }

    public void setWorkFolder(String workFolder) {
        this.workFolder = workFolder;
    }

    public long getPassed() {
        return passed;
    }

    public void setPassed(long passed) {
        this.passed = passed;
    }

    public long getConditionallyPassed() {
        return conditionallyPassed;
    }

    public void setConditionallyPassed(long conditionallyPassed) {
        this.conditionallyPassed = conditionallyPassed;
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(long finishTime) {
        this.finishTime = finishTime;
    }

    public String getServices() {
        return services;
    }

    public void setServices(String services) {
        this.services = services;
    }

    public String getRange() {
        return range;
    }

    public void setRange(String range) {
        this.range = range;
    }

    public boolean getAutostart() {
        return autostart;
    }

    public void setAutostart(boolean autostart) {
        this.autostart = autostart;
    }

    public String getCause() {
        return cause;
    }

    public void setCause(String cause) {
        this.cause = cause;
    }
}
