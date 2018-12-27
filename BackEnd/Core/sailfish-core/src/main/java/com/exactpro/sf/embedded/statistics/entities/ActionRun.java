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
package com.exactpro.sf.embedded.statistics.entities;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.exactpro.sf.scriptrunner.StatusType;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table(name="stactionruns")
@SequenceGenerator(name="stactionruns_generator", sequenceName="stactionruns_sequence")
public class ActionRun {

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO, generator="stactionruns_generator")
	private Long id;

    @Type(type = "com.exactpro.sf.storage.TruncatedString", parameters = {@Parameter(name = "length", value = "255")})
	private String description;

	private int status;

    @Type(type = "com.exactpro.sf.storage.TruncatedString", parameters = {@Parameter(name = "length", value = "255")})
	private String failReason;

	private long rank;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "action_id", nullable = false)
	private Action action;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "service_id", nullable = true)
	private Service service;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "tc_run_id", nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private TestCaseRun tcRun;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "msg_type_id", nullable = true)
	private MessageType msgType;

    @Column(name = "tag", nullable = true)
    @Type(type = "com.exactpro.sf.storage.TruncatedString", parameters = {@Parameter(name = "length", value = "255")})
    private String tag;

    @OneToMany(mappedBy = "id.actionRun", cascade = CascadeType.ALL)
    private Set<ActionRunKnownBug> actionRunKnownBugs = new HashSet<>();

    private int hash;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public StatusType getStatus() {
		return StatusType.getStatusType(status);
	}

	public void setStatus(StatusType status) {
		this.status = status.getId();
	}

	public String getFailReason() {
		return failReason;
	}

	public void setFailReason(String failReason) {
		this.failReason = failReason;
	}

	public long getRank() {
		return rank;
	}

	public void setRank(long rank) {
		this.rank = rank;
	}

	public Action getAction() {
		return action;
	}

	public void setAction(Action action) {
		this.action = action;
	}

	public Service getService() {
		return service;
	}

	public void setService(Service service) {
		this.service = service;
	}

	public TestCaseRun getTcRun() {
		return tcRun;
	}

	public void setTcRun(TestCaseRun tcRun) {
		this.tcRun = tcRun;
	}

	public MessageType getMsgType() {
		return msgType;
	}

	public void setMsgType(MessageType msgType) {
		this.msgType = msgType;
	}

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public Set<ActionRunKnownBug> getActionRunKnownBugs() {
        return actionRunKnownBugs;
    }

    public void setActionRunKnownBugs(Set<ActionRunKnownBug> actionRunKnownBugs) {
        this.actionRunKnownBugs = actionRunKnownBugs;
    }

    public void setHash(int hash) {
        this.hash = hash;
    }

    public void addKnownBug(KnownBug knownBug, boolean reproduced) {
        ActionRunKnownBug actionRunKnownBug = new ActionRunKnownBug(this, knownBug, reproduced);
        actionRunKnownBugs.add(actionRunKnownBug);
    }
}
