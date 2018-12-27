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
package com.exactpro.sf.comparison;

import java.util.*;

import org.apache.commons.lang3.StringUtils;

import com.exactpro.sf.aml.scriptutil.ExpressionResult;
import com.exactpro.sf.common.messages.MsgMetaData;
import com.exactpro.sf.comparison.table.Row;
import com.exactpro.sf.comparison.table.Table;
import com.exactpro.sf.scriptrunner.StatusType;
import com.exactpro.sf.util.BugDescription;

public class ComparisonResult implements Iterable<ComparisonResult> {
    private String name;
    private Object actual;
    private Object expected;
    private Double doublePrecision;
    private Double systemPrecision;
    private StatusType status;
    private ExpressionResult expressionResult;
    private Throwable exception;
    private Map<String, ComparisonResult> results = new LinkedHashMap<>();
    private Set<BugDescription> allKnownBugs = Collections.emptySet();
    private Set<BugDescription> reproducedBugs = Collections.emptySet();
    private ComparisonResult parent;
    private MsgMetaData metaData;

    public ComparisonResult(String name) {
        setName(name);
    }

    /**
     * Executes the downward cloning with null as parent value and exclude
     * external entities
     */
    public ComparisonResult(ComparisonResult origin) {
        this.name = origin.name;
        this.actual = origin.actual;
        this.expected = origin.expected;
        this.doublePrecision = origin.doublePrecision;
        this.systemPrecision = origin.systemPrecision;
        this.status = origin.status;
        this.expressionResult = origin.expressionResult;
        this.exception = origin.exception;
        for (ComparisonResult comparisonResult : origin) {
            addResult(new ComparisonResult(comparisonResult));
        }
        if (!origin.allKnownBugs.isEmpty()) {
            setAllKnownBugs(new HashSet<>(origin.allKnownBugs));
        }
        if (!origin.reproducedBugs.isEmpty()) {
            setReproducedBugs(new HashSet<>(origin.reproducedBugs));
        }
        this.metaData = origin.metaData;
    }

    public String getName() {
        return name;
    }

    public ComparisonResult setName(String name) {
        if(StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("name cannot be blank");
        }

        this.name = name;
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T getActual() {
        return (T)actual;
    }

    public ComparisonResult setActual(Object actual) {
        this.actual = actual;
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T getExpected() {
        return (T)expected;
    }

    public ComparisonResult setExpected(Object expected) {
        this.expected = expected;
        return this;
    }

    public Double getDoublePrecision() {
        return doublePrecision;
    }

    public ComparisonResult setDoublePrecision(Double doublePrecision) {
        this.doublePrecision = doublePrecision;
        return this;
    }

    public Double getSystemPrecision() {
        return systemPrecision;
    }

    public ComparisonResult setSystemPrecision(Double systemPrecision) {
        this.systemPrecision = systemPrecision;
        return this;
    }

    public StatusType getStatus() {
        return status;
    }

    public ComparisonResult setStatus(StatusType status) {
        this.status = status;
        return this;
    }

    public ExpressionResult getExpressionResult() {
        return expressionResult;
    }

    public List<?> getEmbeddedListFilter() {
        return expressionResult != null ? expressionResult.getEmbeddedListFilter() : null;
    }

    public ComparisonResult setExpressionResult(ExpressionResult expressionResult) {
        this.expressionResult = expressionResult;
        return this;
    }

    public String getExceptionMessage() {
        return exception != null ? exception.getMessage() : "";
    }

    public Throwable getException() {
        return exception;
    }

    public ComparisonResult setException(Throwable exception) {
        this.exception = exception;
        return this;
    }

    public boolean hasResults() {
        return !results.isEmpty();
    }

    public Map<String, ComparisonResult> getResults() {
        return Collections.unmodifiableMap(results);
    }

    public ComparisonResult getResult(String name) {
        return results.get(name);
    }

    public ComparisonResult addResult(ComparisonResult value) {
        String name = value.getName();

        if(results.containsKey(name)) {
            throw new IllegalArgumentException("Result with a duplicate name: " + value);
        }

        value.parent = this;
        results.put(name, value);

        return this;
    }

    public Set<BugDescription> getAllKnownBugs() {
        return Collections.unmodifiableSet(allKnownBugs);
    }

    public ComparisonResult setAllKnownBugs(Set<BugDescription> allKnownBugs) {
        this.allKnownBugs = Objects.requireNonNull(allKnownBugs, "allKnownBugs cannot be null");
        return this;
    }

    public Set<BugDescription> getReproducedBugs() {
        return Collections.unmodifiableSet(reproducedBugs);
    }

    public ComparisonResult setReproducedBugs(Set<BugDescription> reproducedBugs) {
        this.reproducedBugs = Objects.requireNonNull(reproducedBugs, "reproducedBugs cannot be null");
        return this;
    }

    public ComparisonResult getParent() {
        return parent;
    }

    public MsgMetaData getMetaData() {
        return metaData;
    }

    public ComparisonResult setMetaData(MsgMetaData metaData) {
        this.metaData = Objects.requireNonNull(metaData, "metaData cannot be null");
        return this;
    }

    @Override
    public Iterator<ComparisonResult> iterator() {
        return results.values().iterator();
    }

    @Override
    public String toString() {
        return toTable().toString();
    }

    public Table toTable() {
        Table table = toTable(this, 0);
        table.add(new Row());
        return table;
    }

    private Table toTable(ComparisonResult result, int offset) {
        Table table = new Table(offset);
        table.setTitle(result.getName());

        String status = Objects.toString(result.getStatus(), "");
        String expected = Formatter.formatExpected(result);
        String actual = Formatter.formatForHtml(result.getActual(), false);

        if(result.getExpected() != null) {
            if(result.getDoublePrecision() != null) {
                expected += " +- " + result.getDoublePrecision();
            }

            if(result.getSystemPrecision() != null) {
                expected += " % " + result.getSystemPrecision();
            }
        }

        table.add(new Row(offset, result.getName(), expected, actual, status));

        for(ComparisonResult subResult : result) {
            if(!subResult.hasResults()) {
                status = Objects.toString(subResult.getStatus(), "");
                expected = Formatter.formatExpected(subResult);
                actual = Formatter.formatForHtml(subResult.getActual(), false);

                if(subResult.getExpected() != null) {
                    if(subResult.getDoublePrecision() != null) {
                        expected += " +- " + subResult.getDoublePrecision();
                    }

                    if(subResult.getSystemPrecision() != null) {
                        expected += " % " + subResult.getSystemPrecision();
                    }
                }

                table.add(new Row(offset, subResult.getName(), expected, actual, status));
            } else {
                Table subTable = toTable(subResult, offset + 2);
                table.add(new Row());
                table.add(subTable);
            }
        }

        return table;
    }
}
