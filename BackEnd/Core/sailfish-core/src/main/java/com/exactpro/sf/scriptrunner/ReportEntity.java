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
package com.exactpro.sf.scriptrunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import com.exactpro.sf.common.messages.IMessage;

public class ReportEntity implements Iterable<ReportEntity> {
    private final String name;
    private final Object value;
    private final List<ReportEntity> fields;

    public ReportEntity(String name, Object value) {
        Objects.requireNonNull(name, "name cannot be null");

        this.value = value;
        List<ReportEntity> fields = new ArrayList<>();

        if(value instanceof Iterable<?>) {
            int index = 0;
            Iterable<?> iterable = (Iterable<?>)value;

            for(Object element : iterable) {
                fields.add(new ReportEntity(Integer.toString(index++), element));
            }
        } else if(value instanceof Map<?, ?>) {
            Map<?, ?> map = (Map<?, ?>)value;

            for(Object key : map.keySet()) {
                fields.add(new ReportEntity(Objects.toString(key), map.get(key)));
            }
        } else if(value instanceof IMessage) {
            IMessage message = (IMessage)value;

            if(!StringUtils.isNumeric(name)) {
                name = message.getName();
            }

            for(String fieldName : message.getFieldNames()) {
                fields.add(new ReportEntity(fieldName, message.getField(fieldName)));
            }
        }

        this.name = name;
        this.fields = Collections.unmodifiableList(fields);
    }

    public String getName() {
        return name;
    }

    @SuppressWarnings("unchecked")
    public <T> T getValue() {
        return (T)value;
    }

    public List<ReportEntity> getFields() {
        return fields;
    }

    public boolean hasFields() {
        return !fields.isEmpty();
    }

    @Override
    public Iterator<ReportEntity> iterator() {
        return fields.iterator();
    }
}
