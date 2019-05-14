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
package com.exactpro.sf.embedded.machinelearning.entities;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@JsonPropertyOrder({"id", "submittedAt", "submitter", "expectedMessage", "participants"})
@Entity
@Table(name = "mlfailedaction")
@SequenceGenerator(name = "mlfailedaction_generator", sequenceName = "mlfailedaction_sequence")
public class FailedAction implements Serializable {

    private static final long serialVersionUID = 4551060764131488588L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "mlfailedaction_generator")
    private long id;

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "expected_message_id", nullable = false)
    private Message expectedMessage;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinTable(
            name = "mlmessageparticipants",
            joinColumns = @JoinColumn(name = "failed_action_id", nullable = false),
            inverseJoinColumns = @JoinColumn(name = "message_participant_id", nullable = false)
    )
    private List<MessageParticipant> participants;

    @Column(nullable = false)
    private long submittedAt;

    @Column
    private String submitter;

    @Column
    private long periodStart;

    @Column
    private long periodEnd;

    @Column
    private String dictionaryURI;

    @Column
    private String name;

    @Column
    private String protocol;

    @Column
    private String namespace;

    public FailedAction() {
        this.participants = new ArrayList<>();
        this.submittedAt = System.currentTimeMillis();
    }

    public FailedAction(Message expectedMessage, MessageParticipant... messageParticipants) {
        this();
        this.expectedMessage = expectedMessage;
        participants.addAll(Arrays.asList(messageParticipants));
    }

    public void addParticipant(MessageParticipant participant) {
        participants.add(participant);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Message getExpectedMessage() {
        return expectedMessage;
    }

    public void setExpectedMessage(Message expectedMessage) {
        this.expectedMessage = expectedMessage;
    }

    public List<MessageParticipant> getParticipants() {
        return participants;
    }

    public void setParticipants(List<MessageParticipant> participants) {
        this.participants = participants;
    }

    public long getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(long submittedAt) {
        this.submittedAt = submittedAt;
    }

    public String getSubmitter() {
        return submitter;
    }

    public void setSubmitter(String user) {
        this.submitter = user;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("id", id)
                .append("expectedMessage", expectedMessage)
                .append("participants", participants)
                .append("submittedAt", submittedAt)
                .append("submitter", submitter)
                .append("periodStart", periodStart)
                .append("periodEnd", periodEnd)
                .append("namespace", namespace)
                .toString();
    }

    public long getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(long periodStart) {
        this.periodStart = periodStart;
    }

    public long getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(long periodEnd) {
        this.periodEnd = periodEnd;
    }

    public String getDictionaryURI() {
        return dictionaryURI;
    }

    public void setDictionaryURI(String dictionaryURI) {
        this.dictionaryURI = dictionaryURI;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }
}
