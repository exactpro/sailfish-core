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
package com.exactpro.sf.embedded.statistics.storage.reporting;

import com.exactpro.sf.scriptrunner.StatusType;

import java.io.Serializable;

@SuppressWarnings("serial")
public class ActionInfoRow implements Serializable {

	private long rank;

	private String description;

	private String failReason;

	private String action;

	private String msgType;

	private String service;

    private String tag;

    private StatusType statusType;

    private int hash;

	public ActionInfoRow(long rank, String description, String failReason, String action, String msgType,
                         String service, String tag, int status, int hash) {
		super();
		this.rank = rank;
		this.description = description;
		this.failReason = failReason;
		this.action = action;
		this.msgType = msgType;
		this.service = service;
        this.tag = tag;
        this.statusType = StatusType.getStatusType(status);
        this.hash = hash;
	}

	public long getRank() {
		return rank;
	}

	public void setRank(long rank) {
		this.rank = rank;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getFailReason() {
		return failReason;
	}

	public void setFailReason(String failReason) {
		this.failReason = failReason;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getMsgType() {
		return msgType;
	}

	public void setMsgType(String msgType) {
		this.msgType = msgType;
	}

	public String getService() {
		return service;
	}

	public void setService(String service) {
		this.service = service;
	}

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    private void setStatusType(int status) {
        statusType = StatusType.getStatusType(status);
    }

    public StatusType getStatusType() {
        return statusType;
    }

    public int getHash() {
        return hash;
    }

    public void setHash(int hash) {
        this.hash = hash;
    }
}
