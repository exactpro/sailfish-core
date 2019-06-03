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

import com.exactpro.sf.comparison.ComparisonResult;
import com.exactpro.sf.comparison.Formatter;
import com.exactpro.sf.scriptrunner.StatusType;
import com.exactpro.sf.scriptrunner.impl.jsonreport.JsonReport;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class VerificationEntry {
    private String name;
    private String actual;
    private String expected;
    private StatusType status;
    private Double precision;
    private Double systemPrecision;
    private List<VerificationEntry> subEntries;
    private ReportException exception;

    public VerificationEntry() {

    }

    public VerificationEntry(ComparisonResult result) {
        this.name = result.getName();
        this.actual = Objects.toString(result.getActual(), null);
        this.expected = Formatter.formatExpected(result.getExpected());
        this.precision = result.getDoublePrecision();
        this.systemPrecision = result.getSystemPrecision();
        this.status = result.getStatus();
        this.exception = result.getException() != null ? new ReportException(result.getException()) : null;

        if (result.hasResults()) {
            this.subEntries = result.getResults().values().stream().map(VerificationEntry::new).collect(Collectors.toList());
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getActual() {
        return actual;
    }

    public void setActual(String actual) {
        this.actual = actual;
    }

    public String getExpected() {
        return expected;
    }

    public void setExpected(String expected) {
        this.expected = expected;
    }

    public StatusType getStatus() {
        return status;
    }

    public void setStatus(StatusType status) {
        this.status = status;
    }

    public Double getPrecision() {
        return precision;
    }

    public void setPrecision(Double precision) {
        this.precision = precision;
    }

    public Double getSystemPrecision() {
        return systemPrecision;
    }

    public void setSystemPrecision(Double systemPrecision) {
        this.systemPrecision = systemPrecision;
    }

    public List<VerificationEntry> getSubEntries() {
        return subEntries;
    }

    public void setSubEntries(List<VerificationEntry> subEntries) {
        this.subEntries = subEntries;
    }

    public ReportException getException() {
        return exception;
    }

    public void setException(ReportException exception) {
        this.exception = exception;
    }
}
