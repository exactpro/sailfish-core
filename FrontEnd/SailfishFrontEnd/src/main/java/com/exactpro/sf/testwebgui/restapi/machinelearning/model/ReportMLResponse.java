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

public class ReportMLResponse {

    private @Valid String token = null;
    private @Valid List<PredictionResultEntry> predictions = new ArrayList<>();
    private @Valid List<ReportMessageDescriptor> userMarks = new ArrayList<>();

    public ReportMLResponse token(String token) {
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

    public ReportMLResponse predictions(List<PredictionResultEntry> predictions) {
        this.predictions = predictions;
        return this;
    }

    @JsonProperty("predictions")
    public List<PredictionResultEntry> getPredictions() {
        return predictions;
    }

    public void setPredictions(List<PredictionResultEntry> predictions) {
        this.predictions = predictions;
    }

    public ReportMLResponse userMarks(List<ReportMessageDescriptor> userMarks) {
        this.userMarks = userMarks;
        return this;
    }

    @JsonProperty("userMarks")
    public List<ReportMessageDescriptor> getUserMarks() {
        return userMarks;
    }

    public void setUserMarks(List<ReportMessageDescriptor> userMarks) {
        this.userMarks = userMarks;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReportMLResponse)) {
            return false;
        }
        ReportMLResponse reportMLResponse = (ReportMLResponse) o;
        EqualsBuilder equalsBuilder = new  EqualsBuilder();

        return equalsBuilder.append(token, reportMLResponse.token).append(predictions, reportMLResponse.predictions)
                .append(userMarks, reportMLResponse.userMarks).isEquals();
    }

    @Override
    public int hashCode() {
        HashCodeBuilder hashCodeBuilder = new HashCodeBuilder();

        return hashCodeBuilder.append(token).append(predictions).append(userMarks).toHashCode();
    }

    @Override
    public String toString() {

        ToStringBuilder toStringBuilder = new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE);

        return toStringBuilder.append("token", token)
                .append("predictions", predictions)
                .append("userMarks", userMarks).toString();
    }
}

