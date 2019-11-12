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
package com.exactpro.sf.scriptrunner.impl;

import com.exactpro.sf.aml.AMLBlockType;
import com.exactpro.sf.aml.generator.AggregateAlert;
import com.exactpro.sf.aml.script.CheckPoint;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.comparison.ComparisonResult;
import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.configuration.workspace.WorkspaceStructureException;
import com.exactpro.sf.scriptrunner.IReportStats;
import com.exactpro.sf.scriptrunner.IScriptReport;
import com.exactpro.sf.scriptrunner.LoggerRow;
import com.exactpro.sf.scriptrunner.MessageLevel;
import com.exactpro.sf.scriptrunner.OutcomeCollector;
import com.exactpro.sf.scriptrunner.ScriptContext;
import com.exactpro.sf.scriptrunner.StatusDescription;
import com.exactpro.sf.scriptrunner.services.IStaticServiceManager;
import com.exactpro.sf.services.ServiceMarshalManager;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ReportServices implements IScriptReport {

    private final IStaticServiceManager staticServiceManager;
    private final String reportFolder;

    public ReportServices(IStaticServiceManager staticServiceManager, String reportFolder) {
        this.staticServiceManager = staticServiceManager;
        this.reportFolder = reportFolder;
    }

    @Override
    public void createReport(ScriptContext scriptContext, String name, String description, long scriptRunId, String environmentName, String userName) {

        // Code for export service coped from ServiceMarshalManager.exportService
        // Added filter by environment name

        List<String> matrixServicesNames = scriptContext.getServiceList();

        try {
            new ServiceMarshalManager(staticServiceManager, scriptContext.getDictionaryManager()).saveServices(
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

    @Override
    public void addAlerts(Collection<AggregateAlert> aggregatedAlerts) {
    }

    @Override
    public void closeReport() {
    }

    @Override
    public void flush() {
    }

    @Override
    public void createTestCase(String reference, String description, int order, int matrixOrder, String tcId, int tcHash, AMLBlockType type, Set<String> tags) {
    }

    @Override
    public void closeTestCase(StatusDescription status) {
    }

    @Override
    public boolean isActionCreated() throws UnsupportedOperationException {
        return false;
    }

    @Override
    public void createAction(String id, String serviceName, String name, String messageType, String description, IMessage parameters, CheckPoint checkPoint, String tag, int hash, List<String> verificationsOrder, String outcome) {
    }

    @Override
    public void closeAction(StatusDescription status, Object actionResult) {
    }

    @Override
    public void openGroup(String name, String description) {
    }

    @Override
    public void closeGroup(StatusDescription status) {
    }

    @Override
    public void createVerification(String name, String description, StatusDescription status, ComparisonResult result) {
    }

    @Override
    public void createMessage(MessageLevel level, String... messages) {
    }

    @Override
    public void createMessage(MessageLevel level, Throwable e, String... messages) {
    }

    @Override
    public void createException(Throwable cause) {
    }

    @Override
    public void createTable(ReportTable table) {
    }

    @Override
    public void createLogTable(List<String> header, List<LoggerRow> rows) {
    }

    @Override
    public void setOutcomes(OutcomeCollector outcomes) {
    }

    @Override
    public void createLinkToReport(String linkToReport) {
    }

    @Override
    public IReportStats getReportStats() {
        return null;
    }
}
