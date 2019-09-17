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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.exactpro.sf.scriptrunner.impl.jsonreport.IJsonReportNode;
import com.fasterxml.jackson.annotation.JsonSetter;

public class Action implements IJsonReportNode {

    private long id;
    private String matrixId;
    private String serviceName;
    private Long checkPointId;
    private List<IJsonReportNode> subNodes;
    private String name;
    private String messageType;
    private String description;
    private Set<Bug> bugs;
    private Set<Long> relatedMessages;
    private Status status;
    private List<Parameter> parameters;
    private List<LogEntry> logs;
    private Instant startTime;
    private Instant finishTime;
    private String outcome;
    private boolean isRunning;

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
                relatedMessages.add(((Message)child).getId());
            } else if (child instanceof Action || child instanceof CustomMessage || child instanceof CustomTable || child instanceof CustomLink) {
                subNodes.add(child);
            } else if (child instanceof Bug) {
                bugs.add((Bug)child);
            } else if (child instanceof Verification) {
                subNodes.add(child);
                if (((Verification) child).getMessageId() != null) {
                    relatedMessages.add(((Verification)child).getMessageId());
                }
            } else if (child instanceof LogEntry) {
                logs.add((LogEntry)child);
            } else {
                throw new IllegalArgumentException("unsupported child node type: " + child.getClass());
            }
        }
    }

    @Override public void addException(Throwable t) {
        if(status == null) {
            this.status = new Status(t);
        }
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getMatrixId() {
        return matrixId;
    }

    public void setMatrixId(String matrixId) {
        this.matrixId = matrixId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
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

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
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

    @JsonSetter("parameters")
    public void setParameters(List<Parameter> parameters) {
        this.parameters = parameters;
    }

    public void setParameters(Parameter... parameters) {
        this.parameters = Arrays.asList(parameters);
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

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public boolean getIsRunning() {
        return isRunning;
    }

    public void setIsRunning(boolean running) {
        isRunning = running;
    }
}
