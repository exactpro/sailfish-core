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

package com.exactpro.sf.scriptrunner;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import com.exactpro.sf.SerializeUtil;
import com.exactpro.sf.aml.AMLBlockType;
import com.exactpro.sf.aml.generator.AggregateAlert;
import com.exactpro.sf.aml.script.CheckPoint;
import com.exactpro.sf.comparison.ComparisonResult;
import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.scriptrunner.impl.ReportTable;

public class PropertiesReport implements IScriptReport{
    public static final String PROPERTIES_FILE_NAME = "test_script_properties.xml";
    public static final String ROOT_ELEMENT_NAME = "properties";
    public static final String STATE_ATTR_NAME = "state";
    public static final String STATUS_ATTR_NAME = "status";
    public static final String MATRIX_FILE_NAME_ATTR_NAME = "matrixFileName";
    public static final String TIMESTAMP_ATTR_NAME = "timestamp";
    public static final String ENVIRONMENT_NAME_ATTR_NAME = "environmentName";
    public static final String PASSED_ATTR_NAME = "passed";
    public static final String CONDITIONALLY_PASSED_ATTR_NAME = "condtionally_passed";
    public static final String LANGUAGE_URI_ATTR_NAME = "languageURI";
    public static final String FAILED_ATTR_NAME = "failed";
    public static final String TOTAL_ATTR_NAME = "total";
    public static final String WORK_FOLDER_ATTR_NAME = "scriptFolder";
    public static final String USER = "user";
    public static final String START_TIME = "startTime";
    public static final String FINISH_TIME = "finishTime";
    public static final String SERVICES = "services";
    public static final String RANGE = "range";
    public static final String AUTOSTART = "autostart";
    public static final String CAUSE = "cause";

    private final IWorkspaceDispatcher workspaceDispatcher;
    private final TestScriptDescription testScriptDescription;

    public PropertiesReport(String reportFolder, IWorkspaceDispatcher workspaceDispatcher, TestScriptDescription testScriptDescription){
        this.workspaceDispatcher = workspaceDispatcher;
        this.testScriptDescription = testScriptDescription;
    }

    @Override
    public void createReport(ScriptContext scriptContext, String name, String description, long scriptRunId, String environmentName,
            String userName) {
    }

    @Override
    public void addAlerts(Collection<AggregateAlert> alerts) {

    }

    @Override
    public void closeReport() {
        try {
            rewriteReport();
        } catch (Exception e) {
            throw new ScriptRunException("Can't write test script properties", e);
        }
    }

    /**
     * @throws IOException
     */
    private void rewriteReport() throws IOException {
            Element root = new Element(ROOT_ELEMENT_NAME);

            Element state = new Element(STATE_ATTR_NAME);
            Element status = new Element(STATUS_ATTR_NAME);
            Element matrixFile = new Element(MATRIX_FILE_NAME_ATTR_NAME);
            Element timestamp = new Element(TIMESTAMP_ATTR_NAME);
            Element environmentNameAttr = new Element(ENVIRONMENT_NAME_ATTR_NAME);
            Element languageURI = new Element(LANGUAGE_URI_ATTR_NAME);
            Element workFolder = new Element(WORK_FOLDER_ATTR_NAME);
            Element passed = new Element(PASSED_ATTR_NAME);
            Element conditionallyPassed = new Element(CONDITIONALLY_PASSED_ATTR_NAME);
            Element failed = new Element(FAILED_ATTR_NAME);
            Element total = new Element(TOTAL_ATTR_NAME);
            Element username = new Element(USER);
            Element startTime = new Element(START_TIME);
            Element finishTime = new Element(FINISH_TIME);
            Element services = new Element(SERVICES);
            Element range = new Element(RANGE);
            Element autostart = new Element(AUTOSTART);

            state.setText(testScriptDescription.getState().name());
            status.setText(testScriptDescription.getStatus().name());
            matrixFile.setText(testScriptDescription.getMatrixFileName());
            timestamp.setText(String.valueOf(testScriptDescription.getTimestamp().getTime()));
            environmentNameAttr.setText(testScriptDescription.getContext().getEnvironmentName());
            languageURI.setText(testScriptDescription.getLanguageURI().toString());
            workFolder.setText(testScriptDescription.getWorkFolder());

            IScriptProgress progress = testScriptDescription.getContext().getScriptProgress();
            passed.setText(String.valueOf(progress.getPassed()));
            conditionallyPassed.setText(String.valueOf(progress.getConditionallyPassed()));
            failed.setText(String.valueOf(progress.getFailed()));
            total.setText(String.valueOf(progress.getLoaded()));

            username.setText(testScriptDescription.getUsername());
            startTime.setText(Long.toString(testScriptDescription.getStartedTime()));
            finishTime.setText(Long.toString(testScriptDescription.getFinishedTime()));
            services.setText(testScriptDescription.getServices());
            range.setText(testScriptDescription.getRange());
            autostart.setText(String.valueOf(testScriptDescription.getAutoStart()));

            root.addContent(state);
            root.addContent(status);
            root.addContent(matrixFile);
            root.addContent(timestamp);
            root.addContent(environmentNameAttr);
            root.addContent(languageURI);
            root.addContent(workFolder);
            root.addContent(passed);
            root.addContent(conditionallyPassed);
            root.addContent(failed);
            root.addContent(total);
            root.addContent(username);
            root.addContent(startTime);
            root.addContent(finishTime);
            root.addContent(services);
            root.addContent(range);
            root.addContent(autostart);

            // Cause serializing
            if (testScriptDescription.getCause() != null) {
                root.addContent(SerializeUtil.serializeToBase64(testScriptDescription.getCause()));
            }

            Document doc = new Document(root);
            XMLOutputter outputter = new XMLOutputter();
            File file = workspaceDispatcher.createFile(FolderType.REPORT, true, testScriptDescription.getWorkFolder(), PROPERTIES_FILE_NAME);

            outputter.setFormat(Format.getPrettyFormat());

            try(FileOutputStream out = new FileOutputStream(file)) {
                outputter.output(doc, out);
            }
    }

    @Override
    public void flush() {

    }

    @Override
    public void createTestCase(String reference, String description, int order, int matrixOrder, String tcId, int tcHash, AMLBlockType type) {

    }

    @Override
    public void closeTestCase(StatusDescription status) {

    }

    @Override
    public void createAction(String name, String serviceName, String action, String msg, String description, Object inputParameters, CheckPoint checkPoint, String tag, int hash,
                             List<String> verificationsOrder) {

    }

    @Override
    public boolean isActionCreated() throws UnsupportedOperationException {
        return false;
    }

    @Override
    public void createAction(String name, String serviceName, String action, String msg, String description, List<Object> inputParameters, CheckPoint checkPoint, String tag, int hash,
                             List<String> verificationsOrder) {

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
