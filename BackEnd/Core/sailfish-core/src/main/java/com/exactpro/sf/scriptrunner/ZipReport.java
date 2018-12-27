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
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import com.exactpro.sf.aml.AMLBlockType;
import com.exactpro.sf.aml.generator.AggregateAlert;
import com.exactpro.sf.aml.script.CheckPoint;
import com.exactpro.sf.comparison.ComparisonResult;
import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.scriptrunner.EnvironmentSettings.ReportOutputFormat;
import com.exactpro.sf.scriptrunner.impl.ReportTable;

public class ZipReport implements IScriptReport {
    public static final String ZIP = ".zip";
    public static final String ZIP_EXTENSION = "zip";

    private static final FileFilter FILE_FILTER = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            return !FilenameUtils.isExtension(pathname.getName(), ZIP_EXTENSION);
        }
    };

    private long id;
    private final String workFolder;
    private final IWorkspaceDispatcher dispatcher;
    private final TestScriptDescription testscriptDescription;
    private final ReportOutputFormat reportOutputFormat;

    public ZipReport(String reportFolder, IWorkspaceDispatcher dispatcher, TestScriptDescription descr, ReportOutputFormat reportOutputFormat) {
        this.dispatcher = dispatcher;
        this.testscriptDescription = descr;
        this.workFolder = reportFolder;
        this.reportOutputFormat = reportOutputFormat;
    }

    @Override
    public void createReport(ScriptContext scriptContext, String name, String description,
            long scriptRunId, String environmentName, String userName) {
        this.id = scriptRunId;
    }

    @Override
    public void closeReport() {
        try {
            zipFiles(id, workFolder, testscriptDescription, dispatcher);
        } catch (IOException e) {
            throw new ScriptRunException("Can't zip report", e);
        }
    }

    private void zipFiles(long id, String requestUrl, TestScriptDescription descr, IWorkspaceDispatcher dispatcher) throws IOException {
        File path = dispatcher.getFile(FolderType.REPORT, requestUrl);

        // create zip file with name generated from folder name (like 'matrixName.csv_DDMMYYYY_RANDOM.zip')
        zipFilesInFolder(path, requestUrl, dispatcher);

        if(!reportOutputFormat.isEnableFiles()){
            removeFiles(path);
        }
    }

    @Override
    public void addAlerts(Collection<AggregateAlert> alerts) {
    }

    @Override
    public void flush() {
    }

    @Override
    public void createTestCase(String reference, String description, int order, int matrixOrder,
            String tcId, int tcHash, AMLBlockType type) {
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

    private void zipFilesInFolder(File reportFolder, String zipName, IWorkspaceDispatcher dispatcher) throws IOException {

        File report = dispatcher.createFile(FolderType.REPORT, true, zipName, zipName + ZIP);

        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(report))) {
            zip.setLevel(Deflater.DEFAULT_COMPRESSION);

            zipFolder(dispatcher, zip, zipName, null);
        }
    }

    private void zipFolder(IWorkspaceDispatcher dispatcher, ZipOutputStream zip, String relativeFolder, String zipFolder) throws IOException {
        zipFolder = buildPath(zipFolder, FilenameUtils.getName(relativeFolder));

        String relativeFile = null;
        for (String fileName : dispatcher.listFiles(FILE_FILTER, FolderType.REPORT, relativeFolder)) {
            relativeFile = buildPath(relativeFolder, fileName);
            if (dispatcher.getFile(FolderType.REPORT, relativeFile).isDirectory()) {
                zipFolder(dispatcher, zip, relativeFile, zipFolder);
            } else {
                zipFile(dispatcher, zip, relativeFile, zipFolder);
            }
        }
    }

    private void zipFile(IWorkspaceDispatcher dispatcher, ZipOutputStream zip, String relativeFile, String zipFolder) throws IOException {

        zipFolder = buildPath(zipFolder, FilenameUtils.getName(relativeFile));

        byte[] buf = new byte[1024];

        zip.putNextEntry(new ZipEntry(zipFolder));

        try (InputStream inputStream = new FileInputStream(dispatcher.getFile(FolderType.REPORT, relativeFile))) {
            int len;
            while ((len = inputStream.read(buf)) > 0) {
                zip.write(buf, 0, len);
            }
        } finally {
            zip.closeEntry();
        }
    }
    private String buildPath(String path, String file) {
        if (StringUtils.isEmpty(path)) {
            return file;
        } else {
            return FilenameUtils.separatorsToUnix(FilenameUtils.concat(path, file));
        }
    }

    private void removeFiles(File reportFolder) throws IOException {
        String reportFolderName = reportFolder.getName();
        for (String file : dispatcher.listFiles(FILE_FILTER, FolderType.REPORT, reportFolderName)){
            if(dispatcher.getFile(FolderType.REPORT, reportFolderName, file).isDirectory()){
                dispatcher.removeFolder(FolderType.REPORT, reportFolderName, file);
            } else {
                dispatcher.removeFile(FolderType.REPORT, reportFolderName, file);
            }
        }
    }
}
