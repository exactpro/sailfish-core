/*******************************************************************************
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

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "stknown_bugs")
@SequenceGenerator(name = "stknown_bugs_generator", sequenceName = "stknown_bugs_sequence")
public class KnownBug {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO, generator="stknown_bugs_generator")
    private Long id;

    /**
     * String in JSON array format [ ["Category1",] "Subject"] <br/>
     * Examples:<br/>
     * {@code ["Subject"]}<br/>
     * {@code ["Category1","Subject"]}<br/>
     */
    @Type(type = "com.exactpro.sf.storage.TruncatedString", parameters = {@Parameter(name = "length", value = "255")})
    @Column(name = "known_bug", unique = true)
    private String knownBug;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKnownBug() {
        return knownBug;
    }

    public void setKnownBug(String knownBug) {
        this.knownBug = knownBug;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KnownBug knownBug1 = (KnownBug) o;
        return Objects.equals(knownBug, knownBug1.knownBug);
    }

    @Override
    public int hashCode() {
        return Objects.hash(knownBug);
    }
}
