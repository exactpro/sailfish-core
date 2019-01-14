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

import com.exactpro.sf.scriptrunner.StatusDescription;
import com.exactpro.sf.scriptrunner.StatusType;

public class Status {
    private StatusType status;
    private ReportException cause;
    private String description;

    public Status(StatusDescription description) {
        this.status = description.getStatus();
        this.description = description.getDescription();
        this.cause = description.getCause() != null ? new ReportException(description.getCause()) : null;
    }

    Status(Throwable t) {
        this.status = StatusType.FAILED;
        this.cause = new ReportException(t);
    }

    public StatusType getStatus() {
        return status;
    }

    public void setStatus(StatusType status) {
        this.status = status;
    }

    public ReportException getCause() {
        return cause;
    }

    public void setCause(ReportException cause) {
        this.cause = cause;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
