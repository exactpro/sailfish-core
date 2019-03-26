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
package com.exactpro.sf.testwebgui.restapi.xml;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "bbstatus")
public class XmlBbExecutionStatus {

    private String status;

    private String errorMessage;

    private List<String> warnMessages;

    private int progress;

    private List<XmlBBNodeStatus> slaveStatuses;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public List<String> getWarnMessages() {
        return warnMessages;
    }

    public void setWarnMessages(List<String> warnMessages) {
        this.warnMessages = warnMessages;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public List<XmlBBNodeStatus> getSlaveStatuses() {
        return slaveStatuses;
    }

    public void setSlaveStatuses(List<XmlBBNodeStatus> slaveStatuses) {
        this.slaveStatuses = slaveStatuses;
    }
}
