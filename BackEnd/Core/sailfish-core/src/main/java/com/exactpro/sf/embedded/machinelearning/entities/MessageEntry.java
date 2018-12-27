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

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@JsonPropertyOrder({"id", "name", "type", "value", "message", "values", "messages"})
@Entity
@Table(name = "mlmessageentry")
@SequenceGenerator(name = "mlmessageentry_generator", sequenceName = "mlmessageentry_sequence")
public class MessageEntry implements Serializable {

    private static final long serialVersionUID = 1854556747091961906L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "mlmessageentry_generator")
    private long id;

    // TODO: implement ENUM: simple, message, simple array, message array
    @Column(nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private EntryType type;

    @Column(nullable = false)
    private String name;

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "simple_value_id", nullable = true)
    private SimpleValue value;

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "message_id", nullable = true)
    private Message message;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinTable(name = "mlsimplearray", joinColumns = {
            @JoinColumn(name = "entry_id", nullable = false, updatable = true) }, inverseJoinColumns = {
                    @JoinColumn(name = "simple_value_id", nullable = false, updatable = true) })
    private List<SimpleValue> values;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinTable(name = "mlmessagearray", joinColumns = {
            @JoinColumn(name = "entry_id", nullable = false, updatable = true) }, inverseJoinColumns = {
                    @JoinColumn(name = "message_id", nullable = false, updatable = true) })
    private List<Message> messages;

    public MessageEntry() {
    }

    public MessageEntry(String name, EntryType type, SimpleValue value, Message message, List<SimpleValue> values, List<Message> messages) {
        this.name = name;
        this.type = type;
        this.value = value;
        this.values = values;
        this.message = message;
        this.messages = messages;
    }

    public MessageEntry(String name, SimpleValue value) {
        this(name, EntryType.SIMPLE, Objects.requireNonNull(value, "'Value' is null"), null, null, null);
    }

    public MessageEntry(String name, Message message) {
        this(name, EntryType.MESSAGE, null, Objects.requireNonNull(message, "'Message' is null"), null, null);
    }

    public MessageEntry(String name, SimpleValue... values) {
        this(name, EntryType.SIMPLE_ARRAY, null, null, Arrays.asList(Objects.requireNonNull(values, "'Values' is null")), null);
    }

    public MessageEntry(String name, Message... messages) {
        this(name, EntryType.MESSAGE_ARRAY, null, null, null, Arrays.asList(Objects.requireNonNull(messages, "'Messages' is null")));
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public EntryType getType() {
        return type;
    }

    public void setType(EntryType type) {
        this.type = type;
    }

    public SimpleValue getValue() {
        return value;
    }

    public void setValue(SimpleValue value) {
        this.value = value;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public List<SimpleValue> getValues() {
        return values;
    }

    public void setValues(List<SimpleValue> values) {
        this.values = values;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    @Override
    public String toString() {
        ToStringBuilder builder = new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("id", this.id)
                .append("type", this.type)
                .append("name", this.name);
        switch (this.type) {
        case SIMPLE:
            builder.append("value", this.value);
            break;
        case MESSAGE:
            builder.append("message", this.message);
            break;
        case SIMPLE_ARRAY:
            builder.append("values", this.values);
            break;
        case MESSAGE_ARRAY:
            builder.append("messages", this.messages);
            break;
        default:
            builder.append("value", this.value)
                .append("message", this.message)
                .append("values", this.values)
                .append("messages", this.messages);
            break;
        }

        return builder.toString();
    }
}
