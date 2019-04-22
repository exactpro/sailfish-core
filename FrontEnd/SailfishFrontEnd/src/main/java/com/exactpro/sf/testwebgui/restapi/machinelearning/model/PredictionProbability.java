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

import com.exactpro.sf.testwebgui.restapi.machinelearning.model.PredictionResultEntry.ClassValueEnum;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import javax.validation.Valid;

public class PredictionProbability {

    @Valid private ClassValueEnum value;
    @Valid private Float probabilityOfValue;

    public PredictionProbability value(ClassValueEnum value) {
        this.value = value;
        return this;
    }

    @JsonProperty("value")
    public ClassValueEnum getValue() {
        return value;
    }

    public void setValue(ClassValueEnum value) {
        this.value = value;
    }

    public PredictionProbability probalityOfValue(Float probalityOfValue) {
        probabilityOfValue = probalityOfValue;
        return this;
    }

    @JsonProperty("probabilityOfValue")
    public Float getProbabilityOfValue() {
        return probabilityOfValue;
    }

    public void setProbabilityOfValue(Float probabilityOfValue) {
        this.probabilityOfValue = probabilityOfValue;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }
        if (!(o instanceof PredictionProbability)) {
            return false;
        }

        PredictionProbability predictionProbabily = (PredictionProbability) o;
        EqualsBuilder equalsBuilder = new EqualsBuilder();
        return equalsBuilder.append(value, predictionProbabily.value).append(probabilityOfValue, predictionProbabily.probabilityOfValue).isEquals();
    }

    @Override
    public int hashCode() {

        HashCodeBuilder hashCodeBuilder = new HashCodeBuilder();

        return hashCodeBuilder.append(value).append(probabilityOfValue).toHashCode();
    }

    @Override
    public String toString() {

        ToStringBuilder builder = new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE);

        return builder.append("value", value)
                .append("probabilityOfValue", probabilityOfValue)
                .toString();
    }
}

