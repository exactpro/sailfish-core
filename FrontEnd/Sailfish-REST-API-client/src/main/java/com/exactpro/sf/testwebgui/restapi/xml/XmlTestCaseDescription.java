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
package com.exactpro.sf.testwebgui.restapi.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Created by alexey.zarovny on 2/26/15.
 */
@XmlRootElement(name = "testcase")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"description", "testcaseName", "matrixOrder", "id", "status", "failedAction"})
public class XmlTestCaseDescription {

    private String description;
    private String id;
    private int matrixOrder;
    private String testcaseName;
    private String status;
    private XmlFailedAction failedAction;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getMatrixOrder() {
        return matrixOrder;
    }
    
    public void setMatrixOrder(int execOrder) {
        this.matrixOrder = execOrder;
    }
    
    public String getTestcaseName() {
        return testcaseName;
    }

    public void setTestcaseName(String testcaseName) {
        this.testcaseName = testcaseName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public XmlFailedAction getFailedAction() {
        return failedAction;
    }

    public void setFailedAction(XmlFailedAction failedAction) {
        this.failedAction = failedAction;
    }
}
