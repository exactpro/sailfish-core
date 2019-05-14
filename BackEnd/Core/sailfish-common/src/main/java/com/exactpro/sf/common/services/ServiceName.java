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

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings("serial")
public class ServiceName implements Cloneable, Serializable, Comparable<ServiceName> {

	public static final String ENVIRONMENT_SEPARATOR = "@";
	public static final String DEFAULT_ENVIRONMENT = "default";

	private final String environment;
	private final String serviceName;

	public ServiceName(String toParse) {
	    String[] splitted = toParse.split(ENVIRONMENT_SEPARATOR);
		if (splitted.length == 2) {
			this.environment = splitted[0].trim();
			this.serviceName = splitted[1].trim();
		} else {
			this.environment = DEFAULT_ENVIRONMENT;
			this.serviceName = toParse.trim();
		}
	}

	@JsonCreator
	public ServiceName(@JsonProperty("env") String env, @JsonProperty("serviceName") String serviceName) {
        this.environment = env == null ? DEFAULT_ENVIRONMENT : StringUtils.trim(env);
		this.serviceName = StringUtils.trim(serviceName);
	}

	public String getEnvironment() {
		return environment;
	}

	public String getServiceName() {
		return serviceName;
	}

	@JsonIgnore
	public boolean isDefault() {
        return DEFAULT_ENVIRONMENT.equals(environment);
	}

	@Override
	public String toString() {
        return environment + ENVIRONMENT_SEPARATOR + serviceName;
	}

	public static String toString(String env, String serviceName) {
        if(env == null) {
            env = DEFAULT_ENVIRONMENT;
        }
		return env+ENVIRONMENT_SEPARATOR+serviceName;
	}

	public static ServiceName parse(String serviceName) {
        return serviceName == null ? null : new ServiceName(serviceName);
    }

	@Override
	public int hashCode() {
        HashCodeBuilder builder = new HashCodeBuilder();

        builder.append(StringUtils.lowerCase(environment));
        builder.append(serviceName);

        return builder.toHashCode();
	}

	@Override
	public boolean equals(Object obj) {
        if(this == obj) {
            return true;
        }

        if(!(obj instanceof ServiceName)) {
            return false;
        }

        ServiceName that = (ServiceName)obj;
        EqualsBuilder builder = new EqualsBuilder();

        builder.append(StringUtils.lowerCase(environment), StringUtils.lowerCase(that.environment));
        builder.append(serviceName, that.serviceName);

        return builder.isEquals();
	}

	@Override
    public ServiceName clone() {
        return new ServiceName(environment, serviceName);
	}

    @Override
    public int compareTo(ServiceName o) {
        if(o == null) {
            return 1;
        }

        CompareToBuilder builder = new CompareToBuilder();

        builder.append(environment, o.environment, String.CASE_INSENSITIVE_ORDER);
        builder.append(serviceName, o.serviceName);

        return builder.toComparison();
    }
}
