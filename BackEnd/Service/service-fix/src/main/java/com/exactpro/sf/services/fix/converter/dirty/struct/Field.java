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
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Field {
    private String name;
    private String value;
    private int index;
    private Field counter;
    private FieldList fields;
    private List<FieldList> groups;
    private String data;

    public Field(String name, String value) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.value = Objects.requireNonNull(value, "value cannot be null");
        this.data = name + "=" + value + "\1";
    }

    public Field(String name, String value, int index) {
        this(name, value);
        this.index = index;
    }

    public Field(String name, FieldList fields) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.fields = Objects.requireNonNull(fields, "fields cannot be null");
    }

    public Field(String name, Field counter, List<FieldList> groups) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.counter = counter;
        this.groups = Objects.requireNonNull(groups, "groups cannot be null");
    }

    public boolean isSimple() {
        return value != null;
    }

    public boolean isComponent() {
        return fields != null;
    }

    public boolean isGroup() {
        return groups != null;
    }

    public FieldList getFields() {
        return fields;
    }

    public Field getCounter() {
        return counter;
    }

    public List<FieldList> getGroups() {
        return Collections.unmodifiableList(groups);
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public int getIndex() {
        return index;
    }

    public int getChecksum(Charset charset) {
        Objects.requireNonNull(charset, "charset cannot be null");

        int checksum = 0;

        if(isSimple()) {
            for(byte b : data.getBytes(charset)) {
                checksum += b;
            }
        } else if(isComponent()) {
            checksum = fields.getChecksum(charset);
        } else if(isGroup()) {
            if(counter != null) {
                checksum += counter.getChecksum(charset);
            }

            for(FieldList group : groups) {
                checksum += group.getChecksum(charset);
            }
        }

        return checksum & 0xFF;
    }

    public int getLength(Charset charset) {
        Objects.requireNonNull(charset, "charset cannot be null");

        int length = 0;

        if(isSimple()) {
            length = data.getBytes(charset).length;
        } else if(isComponent()) {
            length = fields.getLength(charset);
        } else if(isGroup()) {
            if(counter != null) {
                length += counter.getLength(charset);
            }

            for(FieldList group : groups) {
                length += group.getLength(charset);
            }
        }

        return length;
    }

    public void ensureOrder() {
        if (isComponent()) {
            fields.ensureOrder();
        } else if (isGroup()) {
            for (FieldList group : groups) {
                group.ensureOrder();
            }
        }
    }

    public byte[] getBytes(Charset charset) {
        Objects.requireNonNull(charset, "charset cannot be null");

        return toString().getBytes(charset);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        if(isSimple()) {
            builder.append(data);
        } else if(isComponent()) {
            builder.append(fields);
        } else if(isGroup()) {
            if(counter != null) {
                builder.append(counter);
            }

            for(FieldList group : groups) {
                builder.append(group);
            }
        }

        return builder.toString();
    }
}
