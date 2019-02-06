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

import com.exactpro.sf.scriptrunner.impl.jsonreport.IJsonReportNode;
import com.exactpro.sf.util.BugDescription;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Bug implements IJsonReportNode {
    private final BugDescription description;
    private KnownBugStatus status;

    @JsonCreator
    public Bug(@JsonProperty("description") BugDescription description) {
        this.description = description;
        this.status = KnownBugStatus.NOT_REPRODUCED;
    }

    public Bug markAsReproduced() {
        this.status = KnownBugStatus.REPRODUCED;
        return this;
    }

    public BugDescription getDescription() {
        return description;
    }

    public KnownBugStatus getStatus() {
        return status;
    }

    public void setStatus(KnownBugStatus status) {
        this.status = status;
    }
}
