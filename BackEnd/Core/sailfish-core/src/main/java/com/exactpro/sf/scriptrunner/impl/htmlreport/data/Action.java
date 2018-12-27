/******************************************************************************
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
package com.exactpro.sf.scriptrunner.impl.htmlreport.data;

import com.exactpro.sf.aml.script.CheckPoint;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.List;

public class Action extends BaseEntity {
    private static final int NEXT_AFTER_ROOT = 1;
    private int id;
    private String messageName;
    private List<ActionParameter> parameters;
    private long startTime;
    private String linkToReport;
    private boolean hasHeaders;
    private MachineLearningData machineLearningData;
    private CheckPoint checkPoint;
    private List<String> verificationsOrder;

    public Action() {
        super();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMessageName() {
        return messageName;
    }

    public void setMessageName(String messageName) {
        this.messageName = messageName;
    }

    public List<ActionParameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<ActionParameter> parameters) {
        this.parameters = parameters;
        if (parameters != null) {
            for (int i = NEXT_AFTER_ROOT; i < parameters.size(); i++) {
                if (parameters.get(i).isHeader()) {
                    this.hasHeaders = true;
                    break;
                }
            }
        }
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public String getLinkToReport() {
        return linkToReport;
    }

    public void setLinkToReport(String linkToReport) {
        this.linkToReport = linkToReport;
    }

    public boolean isHasHeaders() {
        return hasHeaders;
    }

    public void setHasHeaders(boolean hasHeaders) {
        this.hasHeaders = hasHeaders;
    }

    public MachineLearningData getMachineLearningData() {
        return machineLearningData;
    }

    public void setMachineLearningData(MachineLearningData machineLearningData) {
        this.machineLearningData = machineLearningData;
    }

    public CheckPoint getCheckPoint() {
        return checkPoint;
    }

    public void setCheckPoint(CheckPoint checkPoint) {
        this.checkPoint = checkPoint;
    }

    public List<String> getVerificationsOrder() {
        return verificationsOrder;
    }

    public void setVerificationsOrder(List<String> verificationsOrder) {
        this.verificationsOrder = verificationsOrder;
    }

    @Override
    public String toString() {
        ToStringBuilder builder = new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE);

        builder.append("id", id);
        builder.append("messageName", messageName);
        builder.append("parameters", parameters.size());
        builder.append("startTime", startTime);
        builder.append("linkToReport", linkToReport);
        builder.append("hasHeaders", hasHeaders);
        builder.append("machineLearningData", machineLearningData);
        builder.append("checkPoint", checkPoint);
        builder.append("verificationsOrder", verificationsOrder);
        builder.append(super.toString());

        return builder.toString();
    }
}
