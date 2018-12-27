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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

public class ReportRow {
    private final Map<String, ReportValue> rowValues = new HashMap<>();
    private final List<String> header;
    private final RowType rowType;

    private ReportRow(List<String> header, RowType rowType) {
        Objects.requireNonNull(header, "'Header' parameter");
        for (String column : header) {
            if (StringUtils.isBlank(column)) {
                throw new IllegalArgumentException(String.format("Header %s contains blank column",
                        StringUtils.join(header, ",")));
            }
        }
        this.header = header;
        this.rowType = Objects.requireNonNull(rowType, "'Row type' parameter");
    }

    public List<String> getHeader() {
        return header;
    }

    public boolean isMatrixRow() {
        return rowType == RowType.MATRIX;
    }

    public boolean isSimpleRow() {
        return rowType == RowType.SIMPLE;
    }

    public void put(String columnName, ReportValue value) {
        if (!header.contains(columnName)) {
            throw new IllegalArgumentException("Unknown column: " + columnName);
        }
        rowValues.put(columnName, value);
    }

    public ReportValue get(String columnName) {
        if (header.contains(columnName)) {
            return rowValues.get(columnName);
        }
        throw new IllegalArgumentException("Unknown column: " + columnName);
    }

    protected enum RowType {
        MATRIX,
        TEST_CASE,
        SIMPLE
    }

    public static ReportRow createMatrixRow(List<String> header) {
        return new ReportRow(header, RowType.MATRIX);
    }

    public static ReportRow createTestCaseRow(List<String> header) {
        return new ReportRow(header, RowType.TEST_CASE);
    }

    public static ReportRow createSimpleRow(List<String> header) {
        return new ReportRow(header, RowType.SIMPLE);
    }
}
