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

import java.util.Map;
import java.util.Set;

public class Message implements IJsonReportNode {
    private long id;
    private Set<Long> relatedActions;
    private String checkPoint;
    private String raw;
    private String from;
    private String to;
    private String msgName;
    private String content;
    private String contentHumanReadable;
    private String timestamp; //IMPORTANT: datetime format may divert from the default one

    public Message() {

    }

    public Message(Map<String, String> data) {
        this.id = Long.parseLong(data.get("Id"));
        this.contentHumanReadable = data.get("Content");
        this.content = data.get("ContentJson");
        this.checkPoint = data.get("UnderCheckPoint").isEmpty() ? null : data.get("UnderCheckPoint");
        this.raw = data.get("RawMessage");
        this.from = data.get("From");
        this.to = data.get("To");
        this.msgName = data.get("MsgName");
        this.timestamp = data.get("Timestamp");
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Set<Long> getRelatedActions() {
        return relatedActions;
    }

    public void setRelatedActions(Set<Long> relatedActions) {
        this.relatedActions = relatedActions;
    }

    public String getCheckPoint() {
        return checkPoint;
    }

    public void setCheckPoint(String checkPoint) {
        this.checkPoint = checkPoint;
    }

    public String getRaw() {
        return raw;
    }

    public void setRaw(String raw) {
        this.raw = raw;
    }

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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContentHumanReadable() {
        return contentHumanReadable;
    }

    public void setContentHumanReadable(String contentHumanReadable) {
        this.contentHumanReadable = contentHumanReadable;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
