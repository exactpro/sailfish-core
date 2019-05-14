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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"id", "type", "entries"})
@Entity
@Table(name = "mlmessage")
@SequenceGenerator(name = "mlmessage_generator", sequenceName = "mlmessage_sequence")
public class Message implements Serializable {

    private static final long serialVersionUID = 5164756768220378180L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "mlmessage_generator")
    private long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "type_id", nullable = false)
    private MessageType type;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinTable(
            name = "mlmessageentries",
            joinColumns = @JoinColumn(name = "parent_message_id", nullable = false),
            inverseJoinColumns = @JoinColumn(name = "entry_id", nullable = false)
    )
    private List<MessageEntry> entries;

    private boolean dirty;

    public Message() {
        this.entries = new ArrayList<>();
    }

    public Message(MessageType type, boolean dirty, MessageEntry... entries) {
        this();
        this.type = type;
        this.entries.addAll(Arrays.asList(entries));
        this.dirty = dirty;
    }

    public Message(MessageType type, MessageEntry... entries) {
        this(type, false, entries);

    }

    public void addEntry(MessageEntry entry) {
        entries.add(entry);
    }
    
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public List<MessageEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<MessageEntry> entries) {
        this.entries = entries;
    }
    
    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("id", id)
                .append("dirty", dirty)
                .append("type", type)
                .append("entries", entries).toString();
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }
}
