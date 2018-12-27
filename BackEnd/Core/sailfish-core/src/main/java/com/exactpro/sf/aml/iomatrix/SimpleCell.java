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
package com.exactpro.sf.aml.iomatrix;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.poi.ss.usermodel.CellStyle;

public class SimpleCell {
    private final String value;
    private final CellStyle cellStyle;

    public SimpleCell() {
        this("");
    }

    public SimpleCell(String value) {
        this(value, null);
    }

    public SimpleCell(String value, CellStyle style) {
        this.value = value;
        this.cellStyle = style;
    }

    public String getValue() {
        return value;
    }

    public CellStyle getCellStyle() {
        return cellStyle;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object obj){
        if(obj == this) {
            return true;
        }

        if(!(obj instanceof SimpleCell)) {
            return false;
        }

        SimpleCell that = (SimpleCell)obj;
        EqualsBuilder builder = new EqualsBuilder();

        builder.append(this.value, that.value);
        builder.append(this.cellStyle, that.cellStyle);

        return builder.isEquals();
    }

    @Override
    public int hashCode() {
        HashCodeBuilder builder = new HashCodeBuilder();

        builder.append(value);
        builder.append(cellStyle);

        return builder.toHashCode();
    }
}
