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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PredictionResultEntry {

    private @Valid Integer actionId = null;
    private @Valid Integer messageId = null;

    public enum ClassValueEnum {

        TRUE("true"), FALSE("false");

        private String value;

        ClassValueEnum(String v) {
            value = v;
        }

        public String value() {
            return value;
        }

        @Override
        @JsonValue
        public String toString() {
            return String.valueOf(value);
        }

        @JsonCreator
        public static ClassValueEnum fromValue(String v) {
            for (ClassValueEnum b : ClassValueEnum.values()) {
                if (String.valueOf(b.value).equals(v)) {
                    return b;
                }
            }
            return null;
        }
    }


    private @Valid ClassValueEnum classValue = null;
    private @Valid Float predictedClassProbability = null;

    public PredictionResultEntry actionId(Integer actionId) {
        this.actionId = actionId;
        return this;
    }

    @JsonProperty("actionId")
    public Integer getActionId() {
        return actionId;
    }

    public void setActionId(Integer actionId) {
        this.actionId = actionId;
    }

    public PredictionResultEntry messageId(Integer messageId) {
        this.messageId = messageId;
        return this;
    }

    @JsonProperty("messageId")
    public Integer getMessageId() {
        return messageId;
    }

    public void setMessageId(Integer messageId) {
        this.messageId = messageId;
    }

    public PredictionResultEntry classValue(ClassValueEnum classValue) {
        this.classValue = classValue;
        return this;
    }

    @JsonProperty("classValue")
    public ClassValueEnum getClassValue() {
        return classValue;
    }

    public void setClassValue(ClassValueEnum classValue) {
        this.classValue = classValue;
    }

    @JsonProperty("predictedClassProbability")
    public Float getPredictedClassProbability() {
        return predictedClassProbability;
    }

    public void setPredictedClassProbability(Float predictedClassProbability) {
        this.predictedClassProbability = predictedClassProbability;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PredictionResultEntry)) {
            return false;
        }
        PredictionResultEntry predictionResultEntry = (PredictionResultEntry) o;
        EqualsBuilder equalsBuilder = new EqualsBuilder();

        return equalsBuilder.append(actionId, predictionResultEntry.actionId)
                .append(messageId, predictionResultEntry.messageId)
                .append(classValue, predictionResultEntry.classValue)
                .append(predictedClassProbability, predictionResultEntry.predictedClassProbability).isEquals();
    }

    @Override
    public int hashCode() {

        HashCodeBuilder hashCodeBuilder = new HashCodeBuilder();
        return hashCodeBuilder.append(actionId).append(messageId).append(classValue).append(predictedClassProbability).toHashCode();
    }

    @Override
    public String toString() {

        ToStringBuilder toStringBuilder = new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE);
        toStringBuilder.append("actionId", actionId)
                .append("messageId", messageId)
                .append("classValue", classValue)
                .append("probabilities", predictedClassProbability);

        return toStringBuilder.toString();
    }
}

