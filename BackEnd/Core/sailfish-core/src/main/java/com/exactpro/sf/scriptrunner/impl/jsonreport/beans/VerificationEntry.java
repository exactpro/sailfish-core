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

import com.exactpro.sf.aml.scriptutil.StaticUtil.IFilter;
import com.exactpro.sf.comparison.ComparisonResult;
import com.exactpro.sf.comparison.Formatter;
import com.exactpro.sf.scriptrunner.StatusType;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class VerificationEntry {
    private String name;

    //FIXME: implement some kind of container for those values. It should contain type, possible enum alias, and value
    private String actual;
    private String actualType;
    private String expected;
    private String expectedType;

    private StatusType status;
    private String hint;
    private Double precision;
    private Double systemPrecision;
    private List<VerificationEntry> subEntries;
    private ReportException exception;

    public VerificationEntry() {

    }

    public VerificationEntry(ComparisonResult result) {
        this.name = result.getName();

        this.actual = Objects.toString(result.getActual(), null);
        this.actualType = getType(result.getActual());

        this.expected = Formatter.formatExpected(result);
        this.expectedType = getType(result.getExpected());

        this.precision = result.getDoublePrecision();
        this.systemPrecision = result.getSystemPrecision();
        this.status = result.getStatus();
        this.exception = result.getException() != null ? new ReportException(result.getException()) : null;
        this.hint = result.getExceptionMessage();

        if (result.hasResults()) {
            this.subEntries = result.getResults().values().stream().map(VerificationEntry::new).collect(Collectors.toList());
        }
    }

    private String getType(Object object) {
        if (object == null) {
            return null;
        }

        if (object instanceof IFilter) {
            Object value = ((IFilter)object).hasValue() ? ((IFilter)object).getValue() : null;
            return value == null ? null : value.getClass().getSimpleName();
        }

        return object.getClass().getSimpleName();
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

    public String getHint() {
        return hint;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }

    public String getActualType() {
        return actualType;
    }

    public void setActualType(String actualType) {
        this.actualType = actualType;
    }

    public String getExpectedType() {
        return expectedType;
    }

    public void setExpectedType(String expectedType) {
        this.expectedType = expectedType;
    }
}
