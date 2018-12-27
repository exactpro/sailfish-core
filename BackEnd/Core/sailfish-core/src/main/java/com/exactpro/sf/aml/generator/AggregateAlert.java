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
package com.exactpro.sf.aml.generator;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AggregateAlert extends AbstractAlert {

    private static final long serialVersionUID = 2462215746549541784L;

    protected final Set<Long> lines;
    protected final Set<Long> uids;
    
    protected AggregateAlert(Set<Long> lines, Set<Long> uids, String column, String message, AlertType type) {
        super(column, message, type);
        if (uids == null || uids.isEmpty()) {
            throw new IllegalArgumentException("Set of unique identifiers can't be empty");
        }
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("Set of line numbers can't be empty");
        }
        
        this.lines = Collections.unmodifiableSet(lines);
        this.uids = Collections.unmodifiableSet(uids);
    }

    public String joinLines() {
        return join(this.lines.stream());
    }
    
    public Set<Long> getLines() {
        return lines;
    }
    
    public Set<Long> getUids() {
        return uids;
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (this.lines.size() > 1) {
            builder.append("Multiple ")
                    .append(this.type.getCapitalized())
                    .append(" in lines '").append(joinLines()).append('\'');

        } else {
            builder.append(this.type.getCapitalized())
                    .append(" in line ").append(joinLines());
        }
        builder.append(" column '").append(this.column).append('\'')
                .append(": ").append(this.message);
        return builder.toString();
    }
    
    private String join(Stream<?> stream) {
        return stream.filter(Objects::nonNull)
        .sorted()
        .map(Object::toString)
        .collect(Collectors.joining(", "));
    }

    public static AlertBuilder builder() {
        return new AlertBuilder();
    }
    
    static class AlertBuilder {
        private String column;
        private String message;
        private AlertType type;
        
        private final Set<Long> lines = new HashSet<>();
        private final Set<Long> uids = new HashSet<>();
        
        public AggregateAlert build() {
            return new AggregateAlert(lines, uids, column, message, type);
        }
        
        public AlertBuilder process(AbstractAlert alert) {
            return this.setColumn(alert.column)
             .setMessage(alert.message)
             .setType(alert.type);    
        }
        
        public AlertBuilder addLine(Long line) {
            this.lines.add(Objects.requireNonNull(line, "Line number can't be empty"));
            return this;
        }
        
        public AlertBuilder addUid(Long uid) {
            this.uids.add(Objects.requireNonNull(uid, "Unique identifier can't be empty"));
            return this;
        }
        
        public AlertBuilder setColumn(String column) {
            if (this.column != null && !this.column.equals(column)) {
                throw new IllegalArgumentException("New value '" + column + "' is not equals with current '" + this.column + "'");
            }
            this.column = column;
            return this;
        }
        
        public AlertBuilder setMessage(String message) {
            if (this.message != null && !this.message.equals(message)) {
                throw new IllegalArgumentException("New value '" + message + "' is not equals with current '" + this.message + "'");
            }
            this.message = message;
            return this;
        }
        
        public AlertBuilder setType(AlertType type) {
            if (this.type != null && !this.type.equals(type)) {
                throw new IllegalArgumentException("New value '" + type + "' is not equals with current '" + this.type + "'");
            }
            this.type = type;
            return this;
        }
    }
}
