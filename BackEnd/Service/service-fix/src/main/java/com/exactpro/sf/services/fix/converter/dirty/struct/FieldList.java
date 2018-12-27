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
package com.exactpro.sf.services.fix.converter.dirty.struct;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.exactpro.sf.services.fix.converter.dirty.FieldConst;

public class FieldList {
    private static final List<String> LENGTH_EXCLUDED_FIELDS = Arrays.asList(FieldConst.BEGIN_STRING, FieldConst.BODY_LENGTH, FieldConst.CHECKSUM);

    private List<Field> fields;
    private List<String> order;

    public FieldList() {
        fields = new ArrayList<>();
        order = new ArrayList<>();
    }

    public void addField(Field field) {
        fields.add(Objects.requireNonNull(field, "field cannot be null"));
    }

    public Field getField(String name) {
        for(Field field : fields) {
            if(field.getName().equals(name)) {
                return field;
            }
        }

        return null;
    }

    public List<Field> getFields() {
        return Collections.unmodifiableList(fields);
    }

    public void setOrder(List<String> order) {
        this.order = Objects.requireNonNull(order, "order cannot be null");
    }

    public List<String> getOrder() {
        return Collections.unmodifiableList(order);
    }

    public void ensureOrder() {
        Collections.sort(this.fields, new FieldComparator(this.order));
        
        for (Field field : this.fields) {
            field.ensureOrder();
        }
    }
    
    public int getChecksum(Charset charset) {
        Objects.requireNonNull(charset, "charset cannot be null");

        int checksum = 0;

        for(Field field : fields) {
            if(FieldConst.CHECKSUM.equals(field.getName())) {
                continue;
            }

            checksum += field.getChecksum(charset);
        }

        return checksum & 0xFF;
    }

    public int getLength(Charset charset) {
        Objects.requireNonNull(charset, "charset cannot be null");

        int length = 0;

        for(Field field : fields) {
            if(LENGTH_EXCLUDED_FIELDS.contains(field.getName())) {
                continue;
            }

            length += field.getLength(charset);
        }

        return length;
    }

    public byte[] getBytes(Charset charset) {
        Objects.requireNonNull(charset, "charset cannot be null");

        return toString().getBytes(charset);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        for(Field field : fields) {
            builder.append(field);
        }

        return builder.toString();
    }
}
