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
package com.exactpro.sf.services;

import java.util.Date;

import org.apache.commons.lang3.builder.ToStringBuilder;

import com.exactpro.sf.common.services.ServiceName;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ServiceEvent {

	private static long prevTimestamp = 0;

	public enum Level {

		DEBUG,
		INFO,
		WARN,
		ERROR

	}

	public enum Type {

		CREATING,
		CREATED,
		INITIALIZING,
		INITIALIZED,
		STARTING,
		STARTED,
		WARNING,
		DISPOSING,
		DISPOSED,
		ERROR,
		LOGGEDIN,
		LOGGEDOFF,
		INFO;

		public static Type convert(ServiceStatus status)
		{
			return Type.valueOf(status.toString());
		}

	}

    protected Type type;
    protected String message;
    protected Date occurred;
    protected String details;
    protected Level level;
    protected ServiceName serviceName;

	public ServiceEvent(ServiceName serviceName, Level level, Type type, String message, String details) {

		this.type = type;
		this.level = level;
		this.serviceName = serviceName;

		long timestamp = new Date().getTime();

		if ( timestamp == prevTimestamp ) {
			++prevTimestamp;
		} else {
			prevTimestamp = timestamp;
		}

		this.occurred = new Date(prevTimestamp);

		this.message = message;
		this.details = details;

	}

    @JsonCreator
    public ServiceEvent(@JsonProperty("serviceName") ServiceName serviceName,
            @JsonProperty("level") Level level,
            @JsonProperty("type") Type type,
            @JsonProperty("occurred") Date occurred,
            @JsonProperty("message") String message,
            @JsonProperty("details") String details) {

		this.serviceName = serviceName;
		this.type = type;
		this.level = level;
		this.occurred = occurred;
		this.message = message;
		this.details = details;

	}

    public Type getType() {
		return type;
	}


    public String getMessage() {
		return message;
	}


    public Date getOccurred() {
		return occurred;
	}

    public String getDetails() {
		return details;
	}

    public Level getLevel() {
		return level;
	}

    public ServiceName getServiceName() {
		return serviceName;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this).
				append("type", type).
				append("message", message).
				append("occured", occurred).
				append("details", details).
				append("level", level).
				append("serviceName", serviceName).
				toString();
	}

}
