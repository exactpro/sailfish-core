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

import com.exactpro.sf.scriptrunner.LoggerRow;
import com.exactpro.sf.scriptrunner.impl.jsonreport.IJsonReportNode;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Objects;

public class LogEntry implements IJsonReportNode {
    private Instant timestamp;
    private String level;
    private String thread;
    private String message;
    private ReportException exception;

    @JsonProperty("class") private String clazz;

    public LogEntry() {

    }

    public LogEntry(LoggerRow row) {
        this.timestamp = Instant.ofEpochMilli(row.getTimestamp());
        this.level = Objects.toString(row.getLevel(), null);
        this.thread = row.getThread();
        this.message = row.getMessage();
        this.clazz = row.getClazz();
        this.exception = row.getThrowable() != null ? new ReportException(row.getThrowable()) : null;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getThread() {
        return thread;
    }

    public void setThread(String thread) {
        this.thread = thread;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ReportException getException() {
        return exception;
    }

    public void setException(ReportException exception) {
        this.exception = exception;
    }

    public String getClazz() {
        return clazz;
    }

    public void setClazz(String clazz) {
        this.clazz = clazz;
    }
}
