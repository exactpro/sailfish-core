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

import com.exactpro.sf.scriptrunner.MessageLevel;
import com.exactpro.sf.scriptrunner.impl.jsonreport.IJsonReportNode;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CustomMessage implements IJsonReportNode {
    private String message;
    private String color;
    private String style;
    private MessageLevel level;
    private ReportException exception;

    public CustomMessage() {

    }

    public CustomMessage(String message, String color, String style, MessageLevel level, Throwable t) {
        this.message = message;
        this.level = level;
        this.exception = t != null ? new ReportException(t) : null;
        this.color = color;
        this.style = style;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public MessageLevel getLevel() {
        return level;
    }

    public void setLevel(MessageLevel level) {
        this.level = level;
    }

    public ReportException getException() {
        return exception;
    }

    public void setException(ReportException exception) {
        this.exception = exception;
    }
}
