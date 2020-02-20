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

import com.exactpro.sf.scriptrunner.impl.jsonreport.IJsonReportNode;

import java.time.Instant;
import java.util.Collection;
import java.util.Set;

public class TestCaseMetadata {

    private int order;
    private Instant startTime;
    private Instant finishTime;
    private String name;
    private Status status;
    private String id;
    private int hash;
    private String description;
    private Long firstActionId;
    private Long lastActionId;
    private int failedActionCount;

    private Collection<IJsonReportNode> bugs;

    private String jsonFileName;
    private String jsonpFileName;
    private Set<String> tags;

    public TestCaseMetadata() {
    }

    public TestCaseMetadata(TestCase testCase, String jsonFileName, String jsonpFileName) {
        this.order = testCase.getOrder();
        this.id = testCase.getId();
        this.bugs = testCase.getBugTree();
        this.startTime = testCase.getStartTime();
        this.finishTime = testCase.getFinishTime();
        this.name = testCase.getName();
        this.status = testCase.getStatus();
        this.hash = testCase.getHash();
        this.description = testCase.getDescription();
        this.jsonFileName = jsonFileName;
        this.jsonpFileName = jsonpFileName;
        this.tags = testCase.getTags();
        this.firstActionId = testCase.getFirstActionId();
        this.lastActionId = testCase.getLastActionId();
        this.failedActionCount = testCase.getFailedActionsCount();
    }


    public Collection<IJsonReportNode> getBugs() {
        return bugs;
    }

    public void setBugs(Collection<IJsonReportNode> bugs) {
        this.bugs = bugs;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getJsonFileName() {
        return jsonFileName;
    }

    public void setJsonFileName(String jsonFilePath) {
        this.jsonFileName = jsonFilePath;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getHash() {
        return hash;
    }

    public void setHash(int hash) {
        this.hash = hash;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public String getJsonpFileName() {
        return jsonpFileName;
    }

    public void setJsonpFileName(String jsonpFileName) {
        this.jsonpFileName = jsonpFileName;
    }

    public Long getFirstActionId() {
        return firstActionId;
    }

    public void setFirstActionId(Long firstActionId) {
        this.firstActionId = firstActionId;
    }

    public Long getLastActionId() {
        return lastActionId;
    }

    public void setLastActionId(Long lastActionId) {
        this.lastActionId = lastActionId;
    }

    public int getFailedActionCount() {
        return failedActionCount;
    }

    public void setFailedActionCount(int failedActionCount) {
        this.failedActionCount = failedActionCount;
    }
}
