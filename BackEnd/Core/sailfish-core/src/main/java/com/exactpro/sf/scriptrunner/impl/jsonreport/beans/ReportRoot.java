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

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ReportRoot {
    private Instant finishTime;
    private Instant startTime;

    private final List<Alert> alerts = new ArrayList<>();
    //IMPORTANT: access should be synchronized
    private Set<Bug> bugs = new HashSet<>();
    private Map<String, String> plugins = new HashMap<>();
    private String hostName;
    private String userName;
    private String name;
    private long scriptRunId;
    private String version;
    private String branchName;
    private String description;
    private ReportException exception;
    private List<TestCaseMetadata> metadata = new ArrayList<>();
    private ReportProperties reportProperties;

    /**
     * //IMPORTANT: access should be synchronized!
     * @return
     */
    public List<Alert> getAlerts() {
        return alerts;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Instant getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(Instant finishTime) {
        this.finishTime = finishTime;
    }

    public Map<String, String> getPlugins() {
        return plugins;
    }

    public void setPlugins(Map<String, String> plugins) {
        this.plugins = plugins;
    }

    public Set<Bug> getBugs() {
        return bugs;
    }

    public void setBugs(Set<Bug> bugs) {
        this.bugs = bugs;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getScriptRunId() {
        return scriptRunId;
    }

    public void setScriptRunId(long scriptRunId) {
        this.scriptRunId = scriptRunId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ReportException getException() {
        return exception;
    }

    public void setException(ReportException exception) {
        this.exception = exception;
    }

    public List<TestCaseMetadata> getMetadata() {
        return metadata;
    }

    public void setMetadata(List<TestCaseMetadata> metadata) {
        this.metadata = metadata;
    }

    public ReportProperties getReportProperties() {
        return reportProperties;
    }

    public void setReportProperties(ReportProperties reportProperties) {
        this.reportProperties = reportProperties;
    }

}
