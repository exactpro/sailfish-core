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

import java.io.Serializable;
import java.util.Objects;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public abstract class AbstractAlert implements Serializable, Cloneable  {

    private static final long serialVersionUID = 7710470521286990805L;

    protected final String column;
    protected final String message;
    protected final AlertType type;
    
    public AbstractAlert(String column, String message, AlertType type) {
        this.column = column;
        this.message = Objects.requireNonNull(message, "Message can not be null");
        this.type = ObjectUtils.defaultIfNull(type, AlertType.ERROR);
    }

    public AbstractAlert(String column, String message) {
        this(column, message, null);
    }
    
    public AbstractAlert(String message) {
        this(null, message, null);
    }
    
    public String getColumn() {
        return column;
    }
    
    public String getMessage() {
        return message;
    }
    
    public AlertType getType() {
        return type;
    }
    
    @Override
    public boolean equals(Object o) {
        if(o == this) {
            return true;
        }

        if(!(o instanceof AbstractAlert)) {
            return false;
        }

        AbstractAlert that = (AbstractAlert)o;
        EqualsBuilder builder = new EqualsBuilder();
        
        builder.append(this.type, that.type);
        builder.append(this.column, that.column);
        builder.append(this.message, that.message);

        return builder.isEquals();
    }
    
    @Override
    public int hashCode() {
        HashCodeBuilder builder = new HashCodeBuilder();
        
        builder.append(this.type);
        builder.append(this.column);
        builder.append(this.message);
        
        return builder.toHashCode();
    }
}
