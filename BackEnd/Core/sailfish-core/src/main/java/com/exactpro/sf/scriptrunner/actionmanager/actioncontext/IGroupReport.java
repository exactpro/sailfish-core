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
package com.exactpro.sf.scriptrunner.actionmanager.actioncontext;

import java.io.File;

import com.exactpro.sf.comparison.ComparisonResult;
import com.exactpro.sf.configuration.workspace.WorkspaceSecurityException;
import com.exactpro.sf.configuration.workspace.WorkspaceStructureException;
import com.exactpro.sf.scriptrunner.MessageLevel;
import com.exactpro.sf.scriptrunner.StatusDescription;
import com.exactpro.sf.scriptrunner.StatusType;
import com.exactpro.sf.scriptrunner.impl.ReportTable;
import com.exactpro.sf.scriptrunner.reportbuilder.textformatter.TextColor;
import com.exactpro.sf.scriptrunner.reportbuilder.textformatter.TextStyle;

public interface IGroupReport extends AutoCloseable {
    // from IScriptReport
    void createVerification(StatusType status, String name, String description, String statusDescription);

    void createVerification(StatusType status, String name, String description, String statusDescription,
                            ComparisonResult result);

    void createVerification(StatusType status, String name, String description, String statusDescription,
                            ComparisonResult result, Throwable cause);

    void createMessage(StatusType status, MessageLevel level, String... messages);

    void createMessage(StatusType status, MessageLevel level, Throwable e, String... messages);

    void createMessage(TextColor color, TextStyle style, String... messages);

    void createTable(StatusType status, ReportTable table);

    void createLinkToReport(StatusType status, String linkToReport);

    void createParametersTable(String messageName, Object message);

    File createFile(StatusType status, String... pathElements)
            throws WorkspaceStructureException, WorkspaceSecurityException;

    void createException(Throwable cause);

    StatusType getTestCaseStatus();

    StatusDescription getStatusDescription();
}
