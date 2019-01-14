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
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Action implements IJsonReportNode {
    private static final String ACTION_NODE_TYPE = "action";

    private long id;
    private Long checkPointId;
    private List<IJsonReportNode> subNodes;
    private String name;
    private String description;
    private Set<Bug> bugs;
    private Set<Long> relatedMessages;
    private Status status;
    private List<Parameter> parameters;
    private List<LogEntry> logs;
    private Instant startTime;
    private Instant finishTime;

    public Action() {
        this.bugs = new HashSet<>();
        this.subNodes = new ArrayList<>();
        this.relatedMessages = new HashSet<>();
        this.logs = new ArrayList<>();
        this.bugs = new HashSet<>();
    }

    @Override public void addSubNodes(Collection<? extends IJsonReportNode> nodes) {
        for (IJsonReportNode child : nodes) {
            if (child instanceof Message) {
                this.relatedMessages.add(((Message) child).getId());
            } else if (child instanceof Action || child instanceof CustomMessage || child instanceof CustomTable || child instanceof CustomLink) {
                this.subNodes.add(child);
            } else if (child instanceof Bug) {
                this.bugs.add((Bug) child);
            } else if (child instanceof Verification) {
                this.subNodes.add(child);
                if (((Verification) child).getMessageId() != null) {
                    this.relatedMessages.add(((Verification) child).getMessageId());
                }
            } else if (child instanceof LogEntry) {
                this.logs.add((LogEntry) child);
            } else {
                throw new IllegalArgumentException("unsupported child node type: " + child.getClass().toString());
            }
        }
    }

    @Override public void addException(Throwable t) {
        if (this.status == null) {
            this.status = new Status(t);
        }
    }

    @JsonProperty("actionNodeType")
    public String getActionNodeType() {
        return ACTION_NODE_TYPE;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Long getCheckPointId() {
        return checkPointId;
    }

    public void setCheckPointId(Long checkPointId) {
        this.checkPointId = checkPointId;
    }

    public List<IJsonReportNode> getSubNodes() {
        return subNodes;
    }

    public void setSubNodes(List<IJsonReportNode> subNodes) {
        this.subNodes = subNodes;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<Bug> getBugs() {
        return bugs;
    }

    public void setBugs(Set<Bug> bugs) {
        this.bugs = bugs;
    }

    public Set<Long> getRelatedMessages() {
        return relatedMessages;
    }

    public void setRelatedMessages(Set<Long> relatedMessages) {
        this.relatedMessages = relatedMessages;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<Parameter> parameters) {
        this.parameters = parameters;
    }

    public List<LogEntry> getLogs() {
        return logs;
    }

    public void setLogs(List<LogEntry> logs) {
        this.logs = logs;
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
}
