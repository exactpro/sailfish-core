/*******************************************************************************
 * Copyright 2009-2019 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.scriptrunner.impl;

import java.util.List;
import java.util.stream.Collectors;

import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.configuration.workspace.WorkspaceStructureException;
import com.exactpro.sf.scriptrunner.ScriptContext;

public class ReportServices extends DefaultScriptReport {

    private final String reportFolder;

    public ReportServices(String reportFolder) {
        this.reportFolder = reportFolder;
    }

    @Override
    public void createReport(ScriptContext scriptContext, String name, String description, long scriptRunId, String environmentName, String userName) {

        List<String> matrixServicesNames = scriptContext.getServiceList();

        try {
            scriptContext.getServiceMarshalManager().saveServices(
                    scriptContext
                            .getEnvironmentManager()
                            .getServiceStorage()
                            .getServiceDescriptions()
                            .stream()
                            .filter(desc -> matrixServicesNames.contains(desc.getServiceName().toString()))
                            .collect(Collectors.toList()),
                    scriptContext
                            .getWorkspaceDispatcher().
                            createFolder(FolderType.REPORT, reportFolder, "services").toPath());
        } catch (WorkspaceStructureException e) {
            throw new EPSCommonException("Can`t create scripts folder", e);
        }
    }
}
