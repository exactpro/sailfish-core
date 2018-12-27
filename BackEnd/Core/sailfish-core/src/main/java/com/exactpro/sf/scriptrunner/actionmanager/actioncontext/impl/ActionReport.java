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
package com.exactpro.sf.scriptrunner.actionmanager.actioncontext.impl;

import java.io.File;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;

import com.exactpro.sf.common.util.Pair;
import com.exactpro.sf.comparison.ComparisonResult;
import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.configuration.workspace.WorkspaceSecurityException;
import com.exactpro.sf.configuration.workspace.WorkspaceStructureException;
import com.exactpro.sf.scriptrunner.IScriptReport;
import com.exactpro.sf.scriptrunner.MessageLevel;
import com.exactpro.sf.scriptrunner.StatusDescription;
import com.exactpro.sf.scriptrunner.StatusType;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.IActionReport;
import com.exactpro.sf.scriptrunner.impl.ReportTable;
import com.exactpro.sf.scriptrunner.reportbuilder.textformatter.TextColor;
import com.exactpro.sf.scriptrunner.reportbuilder.textformatter.TextStyle;

public class ActionReport implements IActionReport {
    private final IScriptReport report;
    private final String reportFolder;
    private final boolean updateStatus;
    private final IWorkspaceDispatcher workspaceDispatcher;

    private Set<StatusType> elementStats = new HashSet<>();
    private Pair<String, Throwable> description;
    private ActionReport parent;
    private ActionReport child;

    public ActionReport(IScriptReport report, String reportFolder, boolean updateStatus, IWorkspaceDispatcher workspaceDispatcher) {
        this.report = Objects.requireNonNull(report, "report cannot be null");
        this.reportFolder = Objects.requireNonNull(reportFolder, "report folder cannot be null");
        this.updateStatus = updateStatus;
        this.workspaceDispatcher = Objects.requireNonNull(workspaceDispatcher, "workspace dispatcher cannot be null");

        Objects.requireNonNull(report.getReportStats(), "report stats cannot be null");
    }

    @Override
    public void createVerification(StatusType status, String name, String description, String statusDescription) {
        createVerification(status, name, description, statusDescription, null);
    }

    @Override
    public void createVerification(StatusType status, String name, String description, String statusDescription, ComparisonResult result) {
        createVerification(status, name, description, statusDescription, result, null);
    }

    @Override
    public void createVerification(StatusType status, String name, String description, String statusDescription, ComparisonResult result,
            Throwable cause) {
        checkEmbeddedReport();
        elementStats.add(status);
        this.description = tryFillDescription(statusDescription, cause, this.description);
        report.createVerification(name, description, new StatusDescription(status, statusDescription, cause, updateStatus), result);
    }

    @Override
    public void createMessage(StatusType status, MessageLevel level, String... messages) {
        checkEmbeddedReport();
        elementStats.add(status);
        report.createMessage(level, messages);
    }

    @Override
    public void createMessage(StatusType status, MessageLevel level, Throwable e, String... messages) {
        checkEmbeddedReport();
        elementStats.add(status);
        description = tryFillDescription("", e, description);
        report.createMessage(level, e, messages);
    }

    @Override
    public void createMessage(TextColor color, TextStyle style, String... messages) {
        checkEmbeddedReport();
        report.createMessage(color, style, messages);
    }

    @Override
    public void createTable(StatusType status, ReportTable table) {
        checkEmbeddedReport();
        elementStats.add(status);
        report.createTable(table);
    }

    @Override
    public void createParametersTable(String messageName, Object message) {
        checkEmbeddedReport();
        report.createParametersTable(messageName, message);
    }

    @Override
    public void createLinkToReport(StatusType status, String linkToReport) {
        checkEmbeddedReport();
        elementStats.add(status);
        report.createLinkToReport(linkToReport);
    }

    @Override
    public StatusType getTestCaseStatus() {
        return report.getReportStats().getTestCaseStatus();
    }

    @Override
    public File createFile(StatusType status, String... pathElements) throws WorkspaceStructureException, WorkspaceSecurityException {
        checkEmbeddedReport();
        elementStats.add(status);
        return workspaceDispatcher.createFile(FolderType.REPORT, false, ArrayUtils.insert(0, pathElements, reportFolder));
    }

    @Override
    public void close() {
        if(child != null) {
            throw new RuntimeException("Cannot close parent report while embedded report is open");
        }

        if(parent == null) {
            return;
        }

        parent.elementStats.add(getResultStatus(elementStats));
        parent.description = ObjectUtils.defaultIfNull(parent.description, description);
        parent.child = null;

        report.closeGroup(getStatusDescription());
    }

    @Override
    public StatusDescription getStatusDescription() {
        if (description != null) {
            return new StatusDescription(getResultStatus(elementStats), description.getFirst(), description.getSecond(), updateStatus);
        } else {
            return new StatusDescription(getResultStatus(elementStats), "", null, updateStatus);
        }
    }

    @Override
    public IActionReport createEmbeddedReport(String name, String description) {
        checkEmbeddedReport();
        report.openGroup(name, description);
        child = new ActionReport(report, description, updateStatus, workspaceDispatcher);
        child.parent = this;
        return child;
    }

    @Override
    public void createException(Throwable cause) {
        checkEmbeddedReport();
        elementStats.add(StatusType.FAILED);
        report.createException(cause);
    }

    private void checkEmbeddedReport() {
        if(child != null) {
            throw new RuntimeException("Cannot write to parent report while embedded report is opened");
        }
    }

    private Pair<String, Throwable> tryFillDescription(String statusDescription, Throwable cause, Pair<String, Throwable> targetDescription) {
        if (targetDescription == null && (statusDescription != null || cause != null)) {
            return new Pair<>(statusDescription, cause);
        }
        return targetDescription;
    }

    private StatusType getResultStatus(Set<StatusType> stats) {
        if (stats.contains(StatusType.FAILED) || stats.contains(StatusType.CONDITIONALLY_FAILED)) {
            return StatusType.FAILED;
        } else if(stats.contains(StatusType.CONDITIONALLY_PASSED)) {
            return StatusType.CONDITIONALLY_PASSED;
        } else {
            stats.remove(StatusType.SKIPPED);
            if (stats.isEmpty()) {
                return StatusType.SKIPPED;
            }
        }

        return StatusType.PASSED;
    }
}
