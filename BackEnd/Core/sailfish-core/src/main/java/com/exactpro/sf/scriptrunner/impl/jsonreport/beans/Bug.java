/*******************************************************************************
 * Copyright 2009-2019 Exactpro (Exactpro Systems Limited)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 ******************************************************************************/

package com.exactpro.sf.scriptrunner.impl.jsonreport.beans;

import java.util.Set;

import com.exactpro.sf.scriptrunner.impl.jsonreport.IJsonReportNode;
import com.exactpro.sf.util.BugDescription;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Bug implements IJsonReportNode {
    @JsonIgnore
    private final BugDescription description;

    private KnownBugStatus status;
    private final String subject;
    private final Long id;
    private final Set<Long> relatedActionIds;

    @JsonCreator
    public Bug(@JsonProperty("status") KnownBugStatus status, @JsonProperty("subject") String subject, @JsonProperty("id") Long id, @JsonProperty("relatedActionIds") Set<Long> relatedActionIds) {
        this.status = status;
        this.subject = subject;
        this.description = null;
        this.id = id;
        this.relatedActionIds = relatedActionIds;
    }

    public Bug(BugDescription description, KnownBugStatus status, Long id, Set<Long> relatedActionIds) {
        this.description = description;
        this.status = status;
        this.subject = description.getSubject();
        this.id = id;
        this.relatedActionIds = relatedActionIds;
    }

    public void updateStatus(KnownBugStatus status) {
        if (status == KnownBugStatus.REPRODUCED) {
            this.status = status;
        }
    }

    @JsonIgnore
    public BugDescription getDescription() {
        return description;
    }

    public KnownBugStatus getStatus() {
        return status;
    }

    public String getSubject() {
        return subject;
    }

    public Long getId() {
        return id;
    }

    public Set<Long> getRelatedActionIds() {
        return relatedActionIds;
    }
}
