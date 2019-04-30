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

import java.io.Serializable;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"id", "explanation", "message"})
@Entity
@Table(name = "mlmessageparticipant")
@SequenceGenerator(name = "mlmessageparticipant_generator", sequenceName = "mlmessageparticipant_sequence")
public class MessageParticipant implements Serializable {

    private static final long serialVersionUID = 4727917001401554795L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "mlmessageparticipant_generator")
    private long id;

    private boolean explanation;

    private long timestamp;

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    public MessageParticipant() {
    }

    public MessageParticipant(boolean explanation, Message message) {
        this.explanation = explanation;
        this.message = message;
    }

    public MessageParticipant(boolean explanation, Message message, long messageTimestamp) {
        this(explanation, message);
        timestamp = messageTimestamp;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public boolean isExplanation() {
        return explanation;
    }

    public void setExplanation(boolean explanation) {
        this.explanation = explanation;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }
    
    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("id", id)
                .append("explanation", explanation)
                .append("message", message)
                .append("timestamp", timestamp)
                .toString();
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
