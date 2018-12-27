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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class Alert extends AbstractAlert {

    private static final long serialVersionUID = -3909143325850355977L;
    
    private final long line;
	private final long uid;
	private final String reference;
	private final String column;

	public Alert(String message) {
		this(-1,-1, null, null, message, null);
	}

	public Alert(String message, AlertType type) {
        this(-1, -1, null, null, message, type);
    }

	public Alert(long line, String message) {
        this(line, -1, null, null, message, null);
    }

	public Alert(long line, long uid, String message) {
		this(line, uid, null, null, message, null);
	}

	public Alert(long line, String message, AlertType type) {
        this(line, -1, null, null, message, type);
    }

	public Alert(long line, long uid, String message, AlertType type) {
        this(line, uid, null, null, message, type);
    }

	public Alert(long line, String reference, String message) {
        this(line, -1, reference, null, message, null);
    }

	public Alert(long line, long uid, String reference, String message) {
		this(line, uid, reference, null, message, null);
	}

	public Alert(long line, String reference, String message, AlertType type) {
        this(line, -1, reference, null, message, type);
    }

	public Alert(long line, long uid, String reference, String message, AlertType type) {
        this(line, uid, reference, null, message, type);
    }

	public Alert(long line, String reference, String column, String message) {
        this(line, -1, reference, column, message, null);
    }

	public Alert(long line, long uid, String reference, String column, String message) {
		this(line, uid, reference, column, message, null);
	}

	public Alert(long line, String reference, String column, String message, AlertType type) {
	    this(line, -1, reference, column, message, type);
	}

	public Alert(long line, long uid, String reference, String column, String message, AlertType type) {
	    super(column, message, type);
        this.line = line;
        this.uid = uid;
        this.reference = reference;
        this.column = column;
    }
	
	public long getLine() {
		return line;
	}

	public long getUid() {
		return uid;
	}

	public String getReference() {
		return reference;
	}

	public String getColumn() {
		return column;
	}

	@Override
    public boolean equals(Object o) {
	    if(o == this) {
            return true;
        }
	    
	    if (!super.equals(o)) {
	        return false;
	    }

        if(!(o instanceof Alert)) {
            return false;
        }

        Alert that = (Alert)o;
        EqualsBuilder builder = new EqualsBuilder();
        
        builder.append(this.line, that.line);
        builder.append(this.reference, that.reference);

        return builder.isEquals();
    }

    @Override
    public int hashCode() {
        HashCodeBuilder builder = new HashCodeBuilder();
        
        builder.append(super.hashCode());
        builder.append(this.line);
        builder.append(this.reference);
        
        return builder.toHashCode();
    }

	@Override
	public String toString() {
	    StringBuilder builder = new StringBuilder(this.type.getCapitalized());
	    if (this.line != -1) {
	        builder.append(" in line ").append(this.line);
	    }
	    if (StringUtils.isNotBlank(this.reference)) {
            builder.append(" reference '").append(this.reference).append('\'');
        }
	    if (StringUtils.isNotBlank(this.column)) {
	        builder.append(" column '").append(this.column).append('\'');
	    }
	    builder.append(": ").append(this.message);
	    return builder.toString();
	}

    @Override
    public Alert clone() {
        return new Alert(line, uid, reference, column, message, type);
    }
}
