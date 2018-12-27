/*******************************************************************************
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

package com.exactpro.sf.embedded.statistics.handlers;

import java.util.Objects;

import org.apache.commons.lang3.ObjectUtils;

import com.exactpro.sf.scriptrunner.StatusType;

public class ReportValue {

    private final Type type;
    private final Object value;

    private ReportValue(Object value, Type type) {
        this.value = Objects.requireNonNull(value, "'Value' parameter");
        this.type = Objects.requireNonNull(type, "'Type' parameter");
    }

    public boolean isMatrixId() {
        return type == Type.MATRIX_ID;
    }

    public boolean isTestCaseId() {
        return type == Type.TESTCASE_ID;
    }

    public boolean isActionId() {
        return type == Type.ACTION_ID;
    }

    public boolean isSimple() {
        return type == Type.SIMPLE_VALUE;
    }

    public boolean isRunStatus() {
        return type == Type.RUN_STATUS;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public String toString() {
        return this.value.toString();
    }

    protected enum Type {
        SIMPLE_VALUE,
        MATRIX_ID,
        TESTCASE_ID,
        RUN_STATUS,
        ACTION_ID
    }

    public static ReportValue createSimpleValue(Object value) {
        return new ReportValue(ObjectUtils.defaultIfNull(value, ""), Type.SIMPLE_VALUE);
    }

    public static ReportValue createMatrixIdValue(long value) {
        return new ReportValue(value, Type.MATRIX_ID);
    }

    public static ReportValue createTestCaseIdValue(String value) {
        return new ReportValue(value, Type.TESTCASE_ID);
    }

    public static ReportValue createActionIdValue(long value) {
        return new ReportValue(value, Type.ACTION_ID);
    }

    public static ReportValue createRunStatusValue(StatusType value) {
        return new ReportValue(value.name().replace("_", " "), Type.RUN_STATUS);
    }
}
