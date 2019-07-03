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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.exactpro.sf.scriptrunner.impl.jsonreport.IJsonReportNode;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TestCase implements IJsonReportNode {
    private final List<IJsonReportNode> actions;
    private final List<LogEntry> logs;
    private final List<Message> messages;
    private final List<Verification> verifications;
    @JsonIgnore private final BugCategory bugRoot;
    private final List<OutcomeSummary> outcomes;
    private Set<String> tags;
    private Instant startTime;
    private Instant finishTime;
    private String name;
    private String type;
    private String reference;
    private int order;
    private int matrixOrder;
    private String id;
    private int hash;
    private String description;
    private Status status;

    public TestCase() {
        this.outcomes = new ArrayList<>();
        this.actions = new ArrayList<>();
        this.logs = new ArrayList<>();
        this.messages = new ArrayList<>();
        this.bugRoot = new BugCategory("root");
        this.verifications = new ArrayList<>();
        this.tags = new HashSet<>();
    }

    @Override public void addSubNodes(Collection<? extends IJsonReportNode> nodes) {
        for (IJsonReportNode child : nodes) {
            if (child instanceof Action || child instanceof CustomMessage) {
                actions.add(child);
            } else if (child instanceof Message) {
                messages.add((Message)child);
            } else if (child instanceof Bug) {
                bugRoot.placeBugInTree((Bug)child);
            } else if (child instanceof LogEntry) {
                logs.add((LogEntry)child);
            } else if (child instanceof Verification) {
                verifications.add((Verification)child);
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

    public List<IJsonReportNode> getActions() {
        return actions;
    }

    public List<LogEntry> getLogs() {
        return logs;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public List<Verification> getVerifications() {
        return verifications;
    }

    public Collection<Bug> getBugs() {
        return bugRoot.getAllBugs();
    }

    @JsonProperty("bugs")
    public Collection<IJsonReportNode> getBugTree() {
        return bugRoot.getSubNodes();
    }

    public List<OutcomeSummary> getOutcomes() {
        return outcomes;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public int getMatrixOrder() {
        return matrixOrder;
    }

    public void setMatrixOrder(int matrixOrder) {
        this.matrixOrder = matrixOrder;
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

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Set<String> getTags() { return tags; }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }
}
