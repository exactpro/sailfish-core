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
package com.exactpro.sf.aml.reader.struct;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.poi.ss.usermodel.CellStyle;

import com.exactpro.sf.aml.AMLException;
import com.exactpro.sf.aml.AMLLangUtil;
import com.exactpro.sf.aml.generator.matrix.Column;
import com.exactpro.sf.aml.iomatrix.SimpleCell;
import com.exactpro.sf.aml.visitors.IAMLElementVisitor;
import com.exactpro.sf.common.util.StringUtil;

public class AMLElement implements Cloneable {
    private static final AtomicLong UID_GENERATOR = new AtomicLong();

    private static final String NULL_CELL_MESSAGE = "cell cannot be null";
    private static final String NULL_CELLS_MESSAGE = "cells cannot be null";
    private static final String NULL_COLUMN_MESSAGE = "column cannot be null";

    protected final int line;
    protected final long uid;
    protected final Map<String, SimpleCell> cells;
    protected final boolean skipOptional;

    public AMLElement() {
        this(0);
    }

    public AMLElement(int line) {
        this(line, Collections.<String, SimpleCell>emptyMap(), false);
    }

    public AMLElement(int line, Map<String, SimpleCell> cells) {
        this(line, cells, false);
    }

    public AMLElement(int line, Map<String, SimpleCell> cells, boolean skipOptional) {
        Objects.requireNonNull(cells, NULL_CELLS_MESSAGE);

        this.line = line;
        this.uid = UID_GENERATOR.incrementAndGet();
        this.cells = new LinkedHashMap<>();
        this.skipOptional = skipOptional;

        for(Entry<String, SimpleCell> e : cells.entrySet()) {
            setCell(e.getKey(), e.getValue());
        }
    }

    public int getLine() {
        return line;
    }

    public long getUID() {
        return uid;
    }

    public Map<String, SimpleCell> getCells() {
        return Collections.unmodifiableMap(cells);
    }

    public SimpleCell getCell(String name) {
        return cells.get(checkValue(name));
    }

    public SimpleCell getCell(Column column) {
        Objects.requireNonNull(column, NULL_COLUMN_MESSAGE);
        return getCell(column.getName());
    }

    public String getValue(String name) {
        SimpleCell cell = getCell(name);
        return cell != null ? cell.getValue() : null;
    }

    public String getValue(Column column) {
        Objects.requireNonNull(column, NULL_COLUMN_MESSAGE);
        return getValue(column.getName());
    }

    public AMLElement setCell(String name, SimpleCell cell) {
        Objects.requireNonNull(cell, NULL_CELL_MESSAGE);
        checkValue(cell.getValue());
        cells.put(checkValue(name), cell);

        return this;
    }

    public AMLElement setCell(Column column, SimpleCell cell) {
        Objects.requireNonNull(column, NULL_COLUMN_MESSAGE);
        return setCell(column.getName(), cell);
    }

    public AMLElement setValue(String name, String value) {
        SimpleCell existingCell = getCell(name);
        CellStyle style = existingCell != null ? existingCell.getCellStyle() : null;

        return setCell(name, new SimpleCell(value, style));
    }

    public AMLElement setValue(Column column, String value) {
        Objects.requireNonNull(column, NULL_COLUMN_MESSAGE);
        return setValue(column.getName(), value);
    }

    public AMLElement removeCell(String name) {
        cells.remove(checkValue(name));
        return this;
    }

    public AMLElement removeCell(Column column) {
        Objects.requireNonNull(column, NULL_COLUMN_MESSAGE);
        return removeCell(column.getName());
    }

    public boolean containsCell(String name) {
        return cells.containsKey(checkValue(name));
    }

    public boolean containsCell(Column column) {
        Objects.requireNonNull(column, NULL_COLUMN_MESSAGE);
        return containsCell(column.getName());
    }

    public boolean isExecutable() {
        String value = getValue(Column.Execute);
        return AMLLangUtil.isExecutable(value, skipOptional);
    }

    public void accept(IAMLElementVisitor visitor) throws AMLException {
        visitor.visit(this);
    }

    private String checkValue(String value) {
        if(value == null || !StringUtil.isStripped(value)) {
            throw new IllegalArgumentException("Invalid value: " + value);
        }

        return value;
    }

    @Override
    public String toString() {
        ToStringBuilder builder = new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE);

        builder.append("line", line);
        builder.append("cells", cells.values());
        builder.append("skipOptional", skipOptional);

        return builder.toString();
    }

    public AMLElement clone() {
        return new AMLElement(line, cells, skipOptional);
    }
}
