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
package com.exactpro.sf.storage.entities;

import java.util.Date;

import org.apache.commons.lang3.builder.ToStringBuilder;


public class StoredServiceEvent {
	
	private Long id;
	
	private String serviceId;
	private Date occured;
	private String message;
	private String details;
	private String type;
	private String level;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getServiceId() {
		return serviceId;
	}
		
	public void setServiceId(String serviceId) {
		this.serviceId = serviceId;
	}
	
	public Date getOccured() {
		return occured;
	}
	
	public void setOccured(Date occured) {
		this.occured = occured;
	}
	
	public String getMessage() {
		return message;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}
	
	public String getDetails() {
		return details;
	}
	
	public void setDetails(String details) {
		this.details = details;
	}
	
	public String getType() {
		return type;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public String getLevel() {
		return level;
	}
	
	public void setLevel(String level) {
		this.level = level;
	}
	
	@Override
	public String toString() {
		return new ToStringBuilder(this).
				append("serviceId", serviceId).
				append("occured", occured).
				append("message", message).
				append("details", details).
				append("type", type).
				append("level", level).
				append("id", id).
				toString();
	}
	
}
