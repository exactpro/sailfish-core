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
package com.exactpro.sf.storage.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.SerializeUtil;
import com.exactpro.sf.center.impl.SFLocalContext;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.suri.SailfishURIException;
import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.scriptrunner.AbstractScriptRunner;
import com.exactpro.sf.scriptrunner.IScriptRunListener;
import com.exactpro.sf.scriptrunner.ScriptContext;
import com.exactpro.sf.scriptrunner.ScriptProgress;
import com.exactpro.sf.scriptrunner.ScriptProgress.IScriptRunProgressListener;
import com.exactpro.sf.scriptrunner.TestScriptDescription;
import com.exactpro.sf.scriptrunner.TestScriptDescription.ScriptState;
import com.exactpro.sf.scriptrunner.TestScriptDescription.ScriptStatus;
import com.exactpro.sf.scriptrunner.ZipReport;
import com.exactpro.sf.scriptrunner.impl.jsonreport.beans.ReportProperties;
import com.exactpro.sf.scriptrunner.impl.jsonreport.beans.ReportRoot;
import com.exactpro.sf.storage.ITestScriptStorage;
import com.exactpro.sf.storage.LoadedTestScriptDescriptions;
import com.exactpro.sf.storage.entities.XmlReportProperties;
import com.exactpro.sf.util.DirectoryFilter;
import com.exactpro.sf.util.ReportFilter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class DefaultTestScriptStorage implements ITestScriptStorage {
    // TODO: make those values configurable
    public enum ScriptRunsLimit {
        ALL(0),
        DEFAULT(100);

        private final int value;

        ScriptRunsLimit(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(DefaultTestScriptStorage.class);

    public static final String JSON_EXTENSION = "json";
    public static final String REPORT_DATA_DIR = "reportData";
    public static final Path ROOT_JSON_REPORT_FILE = Paths.get(REPORT_DATA_DIR, "report." + JSON_EXTENSION);

    private static final String DATETIME_PATTERN_GROUP_NAME = "datetime";
    private static final Pattern DATETIME_PATTERN = Pattern.compile("[\\s\\S]+(?<datetime>[\\d]{8}_[\\d]{6}_[\\d]{3})");

    private static final ObjectMapper jsonObjectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final IWorkspaceDispatcher workspaceDispatcher;
    private IScriptRunListener scriptRunListener;

    public DefaultTestScriptStorage(IWorkspaceDispatcher workspaceDispatcher) {
    	this.workspaceDispatcher = workspaceDispatcher;
    }

    @Override
    public void updateTestScriptProperties(TestScriptDescription testScript) {
    }

    @Override
    public void setScriptRunListener(IScriptRunListener scriptRunListener) {
        this.scriptRunListener = scriptRunListener;
    }

    @Override
    public LoadedTestScriptDescriptions getTestScriptList(int limit) {

        try {
            Path reportRootDirPath = workspaceDispatcher.getFolder(FolderType.REPORT).toPath();
            Set<File> reportFiles = workspaceDispatcher.listFilesAsFiles(ReportFilter.getInstance(), FolderType.REPORT, true);
            boolean limitReports = (reportFiles.size() > limit && limit != ScriptRunsLimit.ALL.getValue());

            Set<File> filteredReportFiles = limitReports
                    ? reportFiles.stream()
                        .sorted(Comparator.comparingLong(this::extractTimestampFromReport).reversed())
                        .limit(limit)
                        .collect(Collectors.toSet())

                    : reportFiles;

            return new LoadedTestScriptDescriptions(
                    reportFiles.size(),
                    filteredReportFiles.size(),
                    filteredReportFiles.stream()
                            .map(reportFile -> getReport(reportFile, reportRootDirPath.relativize(reportFile.toPath())))
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList())
            );

        } catch (Exception e) {
            logger.error("unable to process stored reports", e);
            return new LoadedTestScriptDescriptions(0, 0, Collections.emptyList());
        }
    }

    private long extractTimestampFromReport(File reportFile) {
        Matcher matcher = DATETIME_PATTERN.matcher(reportFile.getName());

        if (!matcher.find()) {
            matcher = DATETIME_PATTERN.matcher(reportFile.getParent());
        }

        if (!matcher.find()) {
            return reportFile.lastModified();
        }

        try {
            return AbstractScriptRunner.getScriptFolderSuffix().parse(matcher.group(DATETIME_PATTERN_GROUP_NAME)).getTime();
        } catch (ParseException e) {
            logger.error("unable to extract timestamp from a file name - this is a bug!", e);
            return reportFile.lastModified();
        }
    }

	@Override
	public void clear(boolean deleteOnDisk) {
		if (deleteOnDisk) {
			try {
				workspaceDispatcher.removeFolder(FolderType.REPORT);
			} catch (IOException ex) {
				logger.error("Failed to remove all reports: ", ex);
			}
		}
		scriptRunListener.onScriptRunEvent(null);
	}

	@Override
	public List<TestScriptDescription> remove(boolean deleteOnDisk, List<TestScriptDescription> testScriptDescriptions) {
        List<TestScriptDescription> errors = new ArrayList<>();
		if (deleteOnDisk) {
			for (TestScriptDescription testScriptDescription : testScriptDescriptions) {
				try {
				    if(!testScriptDescription.isLocked()){
                        String zipFile = StringUtils.appendIfMissing(testScriptDescription.getWorkFolder(), ZipReport.ZIP);

                        if(workspaceDispatcher.exists(FolderType.REPORT, zipFile)) {
                            workspaceDispatcher.removeFile(FolderType.REPORT, zipFile);
                        } else {
                            workspaceDispatcher.removeFolder(FolderType.REPORT, testScriptDescription.getWorkFolder());
                        }
                    }else {
				        errors.add(testScriptDescription);
                    }
				} catch (IOException ex) {
					logger.error("Failed to remove all reports: ", ex);
				}
			}
		}
		scriptRunListener.onScriptRunEvent(null);
        return errors;
	}

    protected TestScriptDescription convertToTestScriptDescription(String workFolder, ReportRoot reportRoot) throws SailfishURIException {
        ReportProperties properties = reportRoot.getReportProperties();

        TestScriptDescription testScriptDescription = new TestScriptDescription(
                scriptRunListener,
                new Date(properties.getTimestamp()),
                workFolder,
                String.valueOf(properties.getMatrixFile()),
                properties.getRange(),
                properties.getAutostart(),
                reportRoot.getUserName());
        testScriptDescription.setLanguageURI(SailfishURI.parse(properties.getLanguageURI()));

        ScriptProgress scriptProgress = new ScriptProgress(0, new ScriptRunProgressListenerStub());
        scriptProgress.setFailed(properties.getFailed());
        scriptProgress.setLoaded((int) properties.getTotal());
        scriptProgress.setConditionallyPassed(properties.getConditionallyPassed());
        scriptProgress.setPassed(properties.getPassed());
        ScriptContext scriptContext = new ScriptContext(SFLocalContext.getDefault(), scriptProgress, null, null,
                reportRoot.getUserName(), 0, properties.getEnvironmentNameAttr());
        scriptContext.getServiceList().addAll(properties.getServices());

        testScriptDescription.setContext(scriptContext);
        testScriptDescription.setState(properties.getState());
        testScriptDescription.setStatus(properties.getStatus());
        testScriptDescription.setStartedTime(reportRoot.getStartTime().getEpochSecond());
        testScriptDescription.setFinishedTime(reportRoot.getFinishTime().getEpochSecond());
        testScriptDescription.setCause(SerializeUtil.deserializeBase64Obj(properties.getCause(), Throwable.class));

        return testScriptDescription;
    }

    protected TestScriptDescription convertToTestScriptDescription(String workFolder, XmlReportProperties properties) throws SailfishURIException {
        TestScriptDescription testScriptDescription = new TestScriptDescription(scriptRunListener,
                new Date(properties.getTimestamp()), workFolder,properties.getMatrixFileName(),
                properties.getRange(), properties.getAutostart(), properties.getUser());

        testScriptDescription.setLanguageURI(SailfishURI.parse(properties.getLanguageURI()));

        ScriptProgress scriptProgress = new ScriptProgress(0, new ScriptRunProgressListenerStub());
        scriptProgress.setFailed(properties.getFailed());
        scriptProgress.setLoaded(properties.getTotal());
        scriptProgress.setConditionallyPassed(properties.getCondtionallyPassed());
        scriptProgress.setPassed(properties.getPassed());
        ScriptContext scriptContext = new ScriptContext(SFLocalContext.getDefault(), scriptProgress, null, null,
                properties.getUser(), 0, properties.getEnvironmentName());

        testScriptDescription.setContext(scriptContext);
        testScriptDescription.setState(
                ScriptState.valueOf(properties.getState()));
        testScriptDescription.setStatus(
                ScriptStatus.valueOf(properties.getStatus()));
        testScriptDescription.setStartedTime(properties.getStartTime());
        testScriptDescription.setFinishedTime(properties.getFinishTime());
        testScriptDescription.setCause(SerializeUtil.deserializeBase64Obj(properties.getCause(), Throwable.class));

        return testScriptDescription;
    }

    protected File[] getReportDirectories(File workFolder){
        return workFolder.listFiles(DirectoryFilter.getInstance());
    }

    private TestScriptDescription getReport(File reportFile, Path relativeReportPath) {
        String reportRootPath = reportFile.toString();

        try {
            if (FilenameUtils.isExtension(reportRootPath, ZipReport.ZIP_EXTENSION)) {
                try (ZipFile zipFile = new ZipFile(reportFile)) {
                    ZipEntry zipJson = zipFile.getEntry(FilenameUtils.removeExtension(reportFile.getName()) + "/" + ROOT_JSON_REPORT_FILE);
                    try (InputStream stream = zipFile.getInputStream(zipJson)) {
                        return convertToTestScriptDescription(FilenameUtils.removeExtension(relativeReportPath.toString()), jsonObjectMapper.readValue(stream, ReportRoot.class));
                    }
                }
            }
            else if (FilenameUtils.isExtension(reportRootPath, JSON_EXTENSION)) {
                try (InputStream stream = new FileInputStream(reportFile)) {
                    return convertToTestScriptDescription(relativeReportPath.getParent().getParent().toString(), jsonObjectMapper.readValue(stream, ReportRoot.class));
                }
            }
            else {
                throw new IllegalArgumentException(String.format("Unknown file extension '%s'", FilenameUtils.getExtension(reportRootPath)));
            }
        } catch (Exception e) {
            logger.error(String.format("Unable to parse report '%s'", reportRootPath), e);
            return null;
        }
    }

    private class ScriptRunProgressListenerStub implements IScriptRunProgressListener{
        @Override
        public void onProgressChanged(long id) {

        }
    }

	@Override
	public void onScriptRunEvent(TestScriptDescription descr) {
	}
}
