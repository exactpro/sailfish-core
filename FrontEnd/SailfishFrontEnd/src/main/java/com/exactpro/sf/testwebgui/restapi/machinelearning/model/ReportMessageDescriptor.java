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

package com.exactpro.sf.testwebgui.restapi.machinelearning.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import javax.validation.Valid;

public class ReportMessageDescriptor {

    private @Valid long actionId = -1L;
    private @Valid long messageId = -1L;

    public ReportMessageDescriptor actionId(Integer actionId) {
        this.actionId = actionId;
        return this;
    }

    @JsonProperty("actionId")
    public long getActionId() {
        return actionId;
    }

    public void setActionId(long actionId) {
        this.actionId = actionId;
    }

    public ReportMessageDescriptor messageId(Integer messageId) {
        this.messageId = messageId;
        return this;
    }

    @JsonProperty("messageId")
    public long getMessageId() {
        return messageId;
    }

    public void setMessageId(long messageId) {
        this.messageId = messageId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReportMessageDescriptor)) {
            return false;
        }
        ReportMessageDescriptor reportMessageDescriptor = (ReportMessageDescriptor) o;
        EqualsBuilder equalsBuilder = new EqualsBuilder();

        return equalsBuilder.append(actionId, reportMessageDescriptor.actionId)
                .append(messageId, reportMessageDescriptor.messageId).isEquals();
    }

    @Override
    public int hashCode() {

        HashCodeBuilder hashCodeBuilder = new HashCodeBuilder();

        return hashCodeBuilder.append(actionId).append(messageId).toHashCode();
    }

    @Override
    public String toString() {

        ToStringBuilder toStringBuilder = new ToStringBuilder(ToStringStyle.MULTI_LINE_STYLE);

        return toStringBuilder.append("actionId", actionId).append("messageId", messageId).toString();
    }
}

