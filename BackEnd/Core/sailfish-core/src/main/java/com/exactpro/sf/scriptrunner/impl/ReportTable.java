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
package com.exactpro.sf.scriptrunner.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.exactpro.sf.common.util.EPSCommonException;

public class ReportTable {
    private final String name;
    private final List<String> header;
    private final List<Map<String, String>> rows = new ArrayList<>();

    private boolean hasCheckPoints = false;

    public ReportTable(String name, List<String> header) {
        if(StringUtils.isBlank(name)) {
            throw new EPSCommonException("table name cannot be blank");
        }

        this.name = name;

        if(header == null || header.isEmpty()) {
            throw new EPSCommonException("header cannot be null or empty");
        }

        List<String> validatedHeader = new ArrayList<>();

        for(String column : header) {
            if(StringUtils.isBlank(column)) {
                throw new EPSCommonException("header cannot contain blank columns: " + header);
            }

            if(validatedHeader.contains(column)) {
                throw new EPSCommonException("duplicate column '" + column +"' in header: " + header);
            }

            validatedHeader.add(column);
        }

        this.header = Collections.unmodifiableList(validatedHeader);
    }

    public ReportTable addRow(Map<String, String> row) {
        Objects.requireNonNull(row, "table row cannot be null");

        for(String column : row.keySet()) {
            if(!header.contains(column)) {
                throw new EPSCommonException("unknown column '" + column +"' in row: " + row);
            }
        }

        rows.add(Collections.unmodifiableMap(new LinkedHashMap<>(row)));

        return this;
    }

    public ReportTable addRows(Collection<Map<String, String>> rows) {
        Objects.requireNonNull(rows, "table rows cannot be null");

        for (Map<String, String> row : rows) {
            addRow(row);
        }
        
        return this;
    }
    
    public boolean isEmpty() {
        return rows.isEmpty();
    }

    public String getName() {
        return name;
    }

    public List<String> getHeader() {
        return header;
    }

    public List<Map<String, String>> getRows() {
        return Collections.unmodifiableList(rows);
    }

    @Override
    public String toString() {
        ToStringBuilder builder = new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE);

        builder.append("name", name);
        builder.append("header", header);
        builder.append("rows", rows);

        return builder.toString();
    }

    public boolean isHasCheckPoints() {
        return hasCheckPoints;
    }

    public void setHasCheckPoints(boolean hasCheckPoints) {
        this.hasCheckPoints = hasCheckPoints;
    }
}
