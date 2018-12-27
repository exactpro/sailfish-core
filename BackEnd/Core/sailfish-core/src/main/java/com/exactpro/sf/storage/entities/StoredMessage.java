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

import java.sql.Timestamp;
import java.util.Set;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class StoredMessage {
	
	private String from;
	private String to;
	private Timestamp arrived;
	private boolean admin;
	private String namespace;
	private String name;
	private String humanMessage;
	private String jsonMessage;
	private byte[] rawMessage;
	private String rejectReason;
	private Set<StoredField> fields;
	private boolean subMessage;
	private String serviceId;
	 // unique (per SailFish run time) Id
	private Long storedId;

	private Long id;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public boolean isSubMessage() {
		return subMessage;
	}

	public void setSubMessage(boolean subMessage) {
		this.subMessage = subMessage;
	}

	public StoredMessage() {
	}
	
	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public String getTo() {
		return to;
	}

	public void setTo(String to) {
		this.to = to;
	}

	public Timestamp getArrived() {
		return arrived;
	}

	public void setArrived(Timestamp arrived) {
		this.arrived = arrived;
	}

	public boolean isAdmin() {
		return admin;
	}

	public void setAdmin(boolean isAdmin) {
		this.admin = isAdmin;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getHumanMessage() {
		return humanMessage;
	}

	public void setHumanMessage(String humanMessage) {
		this.humanMessage = humanMessage;
	}

	public String getJsonMessage() {
        return jsonMessage;
    }

	public void setJsonMessage(String jsonMessage) {
        this.jsonMessage = jsonMessage;
    }
	
	public byte[] getRawMessage() {
		return rawMessage;
	}

	public void setRawMessage(byte[] rawMessage) {
		this.rawMessage = rawMessage;
	}

	public Set<StoredField> getFields() {
		return fields;
	}

	public void setFields(Set<StoredField> fields) {
		this.fields = fields;
	}

	public Long getStoredId() {
		return storedId;
	}

	public void setStoredId(Long storedId) {
		this.storedId = storedId;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this).
				append("id", id).
				append("storedId", storedId).
				append("from", from).
				append("to", to).
				append("arrived", arrived).
				append("IsAdmin", admin).
				append("namespace", namespace).
				append("name", name).
				append("humanMessage", humanMessage).
				append("rejectReason", rejectReason).
				toString();
	}

    public String getServiceId() {
        return this.serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }
}
