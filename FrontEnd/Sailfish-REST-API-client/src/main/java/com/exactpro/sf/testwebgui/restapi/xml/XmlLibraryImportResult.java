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

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@XmlRootElement(name = "importresult")

public class XmlLibraryImportResult implements Serializable {

    private static final long serialVersionUID = -9092267213983176455L;
    private long numExecutors;

    private long numScripts;

    private long numServices;

    private long id;

    private Set<XmlImportError> commonErrors = new HashSet<>();

    private Set<XmlImportError> globalsErrors = new HashSet<>();

    private final Set<XmlImportError> executorErrors = new HashSet<>();

    private final Set<XmlImportError> scriptListErrors = new HashSet<>();

    public void incNumExecutors() {

        this.numExecutors++;

    }

    public void incNumScripts() {

        this.numScripts++;

    }

    public void incNumServices() {

        this.numServices++;

    }

    public long getNumExecutors() {
        return numExecutors;
    }

    public void setNumExecutors(long numExecutors) {
        this.numExecutors = numExecutors;
    }

    public long getNumScripts() {
        return numScripts;
    }

    public void setNumScripts(long numScripts) {
        this.numScripts = numScripts;
    }

    public long getNumServices() {
        return numServices;
    }

    public void setNumServices(long numServices) {
        this.numServices = numServices;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Set<XmlImportError> getExecutorErrors() {
        return executorErrors;
    }

    public Set<XmlImportError> getScriptListErrors() {
        return scriptListErrors;
    }

    public int getAllErrorsQty() {

        return executorErrors.size() + scriptListErrors.size() + globalsErrors.size() + commonErrors.size();
    }

    public Set<XmlImportError> getGlobalsErrors() {
        return globalsErrors;
    }

    public void setGlobalsErrors(Set<XmlImportError> globalsErrors) {
        this.globalsErrors = globalsErrors;
    }

    public int getCriticalErrorsQty() {

        return globalsErrors.size() + commonErrors.size();
    }

    public int getNonCriticalErrorsQty() {

        return executorErrors.size() + scriptListErrors.size();
    }

    public Set<XmlImportError> getCommonErrors() {
        return commonErrors;
    }

    public void setCommonErrors(Set<XmlImportError> commonErrors) {
        this.commonErrors = commonErrors;
    }

}