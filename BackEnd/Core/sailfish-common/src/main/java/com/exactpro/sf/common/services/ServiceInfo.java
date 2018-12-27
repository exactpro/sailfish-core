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
package com.exactpro.sf.common.services;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ServiceInfo {
    private final String id;
    private final ServiceName name;

    @JsonCreator
    public ServiceInfo(@JsonProperty("id") String id, @JsonProperty("name") ServiceName name) {
        this.id = id;
        this.name = name;
    }
    
    public String getID() {
        return id;
    }

    public ServiceName getName() {
        return name;
    }
    
    @Override
    public boolean equals(Object obj) {
        if(obj == this) {
            return true;
        }

        if(!(obj instanceof ServiceInfo)) {
            return false;
        }

        ServiceInfo that = (ServiceInfo)obj;
        EqualsBuilder builder = new EqualsBuilder();

        builder.append(this.id, that.id);
        builder.append(this.name, that.name);

        return builder.isEquals();
    }
    
    @Override
    public int hashCode() {
        HashCodeBuilder builder = new HashCodeBuilder();

        builder.append(this.id);
        builder.append(this.name);

        return builder.toHashCode();
    }
}
