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
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@JsonPropertyOrder({"class", "message", "stacktrace", "cause"})
public class ReportException implements IJsonReportNode {

    @JsonProperty("class")
    private String clazz;

    private String message;
    private String stacktrace;
    private ReportException cause;

    public ReportException() {

    }

    public ReportException(Throwable t) {
        this.message = t.getMessage();
        this.clazz = t.getClass().getCanonicalName();

        this.cause = t.getCause() != null ? new ReportException(t.getCause()) : null;

        this.stacktrace = Arrays.stream(t.getStackTrace())
                        .map(Object::toString)
                        .collect(Collectors.joining("\n"));
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ReportException getCause() {
        return cause;
    }

    public void setCause(ReportException cause) {
        this.cause = cause;
    }

    public String getStacktrace() {
        return stacktrace;
    }

    public void setStacktrace(String stacktrace) {
        this.stacktrace = stacktrace;
    }

    public String getClazz() {
        return clazz;
    }

    public void setClazz(String clazz) {
        this.clazz = clazz;
    }
}
