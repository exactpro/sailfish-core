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
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

@JsonPropertyOrder({"id", "name", "dictionaryURI", "protocol"})
@Entity
@Table(name = "mlmessagetype", uniqueConstraints = { @UniqueConstraint(columnNames = { "name", "protocol", "dictionaryURI" }) })
@SequenceGenerator(name = "mlmessagetype_generator", sequenceName = "mlmessagetype_sequence")
public class MessageType implements Serializable {

    private static final long serialVersionUID = -938746513978148149L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "mlmessagetype_generator")
    private long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String dictionaryURI;

    @Column(nullable = false)
    private String protocol;


    protected MessageType() {}

    public MessageType(String name, String dictionaryURI, String protocol) {
        this.name = Objects.requireNonNull(name, "'Name' is null");
        this.dictionaryURI = Objects.requireNonNull(dictionaryURI, "'Dictionary URI' is null");
        this.protocol = Objects.requireNonNull(protocol, "'Protocol' is null");
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDictionaryURI() {
        return dictionaryURI;
    }

    public String getProtocol() {
        return protocol;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("id", this.id)
                .append("name", this.name)
                .append("dictionaryURI", this.dictionaryURI)
                .append("protocol", this.protocol).toString();
    }

    @Override
    public boolean equals(Object o) {
        if(!(o instanceof MessageType)) {
            return false;
        }

        MessageType that = (MessageType)o;
        return new EqualsBuilder()
                .append(this.name, that.name)
                .append(this.dictionaryURI, that.dictionaryURI)
                .append(this.protocol, that.protocol).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(this.name)
                .append(this.dictionaryURI)
                .append(this.protocol).toHashCode();
    }
}
