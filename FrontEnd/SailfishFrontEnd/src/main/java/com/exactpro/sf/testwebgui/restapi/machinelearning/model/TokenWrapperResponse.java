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
import java.util.ArrayList;
import java.util.List;

public class TokenWrapperResponse {

    private @Valid String token = null;
    private @Valid List<ReportMessageDescriptor> active = new ArrayList<>();

    public TokenWrapperResponse token(String token) {
        this.token = token;
        return this;
    }

    @JsonProperty("token")
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @JsonProperty("active")
    public List<ReportMessageDescriptor> getActive() {
        return active;
    }

    public void setActive(List<ReportMessageDescriptor> active) {
        this.active = active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TokenWrapperResponse)) {
            return false;
        }
        TokenWrapperResponse response = (TokenWrapperResponse) o;
        EqualsBuilder equalsBuilder = new EqualsBuilder();

        return equalsBuilder.append(token, response.token).append(active, response.active).isEquals();
    }

    @Override
    public int hashCode() {

        HashCodeBuilder hashCodeBuilder = new HashCodeBuilder();

        return hashCodeBuilder.append(token).append(active).toHashCode();
    }

    @Override
    public String toString() {

        ToStringBuilder toStringBuilder = new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE);

        return toStringBuilder.append("token", token).append("active", active).toString();
    }
}

