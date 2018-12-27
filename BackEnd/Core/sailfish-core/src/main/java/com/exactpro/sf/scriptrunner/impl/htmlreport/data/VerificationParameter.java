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
package com.exactpro.sf.scriptrunner.impl.htmlreport.data;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.exactpro.sf.comparison.ComparisonResult;
import com.exactpro.sf.comparison.Formatter;
import com.exactpro.sf.scriptrunner.StatusType;

public class VerificationParameter {
    private String id;
    private String name;
    private String expected;
    private String actual;
    private String precision;
    private String systemPrecision;
    private StatusType status;
    private String failReason;
    private String statusClass;
    private int level;
    private boolean header;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExpected() {
        return expected;
    }

    public void setExpected(ComparisonResult result) {
        this.expected = Formatter.formatExpected(result);
    }

    public String getActual() {
        return actual;
    }

    public void setActual(Object actual) {
        this.actual = Formatter.formatForHtml(actual, false);
    }

    public String getPrecision() {
        return precision;
    }

    public void setPrecision(Double precision) {
        if(precision != null) {
            this.precision = Formatter.formatForHtml(precision, false);
        }
    }

    public String getSystemPrecision() {
        return systemPrecision;
    }

    public void setSystemPrecision(Double systemPrecision) {
        if(systemPrecision != null) {
            this.systemPrecision = Formatter.formatForHtml(systemPrecision, false);
        }
    }

    public StatusType getStatus() {
        return status;
    }

    public void setStatus(StatusType status) {
        this.status = status;
    }

    public String getFailReason() {
        return failReason;
    }

    public void setFailReason(String failReason) {
        this.failReason = failReason;
    }

    public String getStatusClass() {
        return statusClass;
    }

    public void setStatusClass(String statusClass) {
        this.statusClass = statusClass;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public boolean isHeader() {
        return header;
    }

    public void setHeader(boolean header) {
        this.header = header;
    }

    @Override
    public String toString() {
        ToStringBuilder builder = new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE);

        builder.append("name", name);
        builder.append("expected", expected);
        builder.append("actual", actual);
        builder.append("precision", precision);
        builder.append("systemPrecision", systemPrecision);
        builder.append("status", status);
        builder.append("statusClass", statusClass);
        builder.append("level", level);
        builder.append("header", header);

        return builder.toString();
    }
}
