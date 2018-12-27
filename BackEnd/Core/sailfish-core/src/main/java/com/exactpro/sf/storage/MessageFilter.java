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
package com.exactpro.sf.storage;

import java.sql.Timestamp;
import java.util.Set;

public class MessageFilter {
    private String from;
    private String to;
    private String msgName;
    private String msgNameSpace;
    private String humanMessage;
    private Boolean showAdmin;
    private Timestamp startTime;
    private Timestamp finishTime;
    private Boolean sortOrder;
    private String rawMessage;
    private Set<String> servicesIdSet;

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getMsgName() {
        return msgName;
    }

    public void setMsgName(String msgName) {
        this.msgName = msgName;
    }

    public String getHumanMessage() {
        return humanMessage;
    }

    public void setHumanMessage(String humanMessage) {
        this.humanMessage = humanMessage;
    }

    public Boolean getShowAdmin() {
        return showAdmin;
    }

    public void setShowAdmin(Boolean showAdmin) {
        this.showAdmin = showAdmin;
    }

    public Timestamp getStartTime() {
        return startTime;
    }

    public void setStartTime(Timestamp startTime) {
        this.startTime = startTime;
    }

    public Timestamp getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(Timestamp finishTime) {
        this.finishTime = finishTime;
    }

    public Boolean getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Boolean sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getRawMessage() {
        return rawMessage;
    }

    public void setRawMessage(String rawMessage) {
        this.rawMessage = rawMessage;
    }

    public Set<String> getServicesIdSet() {
        return servicesIdSet;
    }

    public void setServicesIdSet(Set<String> servicesIdSet) {
        this.servicesIdSet = servicesIdSet;
    }

    public String getMsgNameSpace() {
        return msgNameSpace;
    }

    public void setMsgNameSpace(String msgNameSpace) {
        this.msgNameSpace = msgNameSpace;
    }
}
