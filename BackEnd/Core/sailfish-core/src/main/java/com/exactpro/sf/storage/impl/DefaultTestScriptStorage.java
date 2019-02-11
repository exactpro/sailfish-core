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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipFile;

import com.exactpro.sf.scriptrunner.impl.jsonreport.beans.ReportProperties;
import com.exactpro.sf.scriptrunner.impl.jsonreport.beans.ReportRoot;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import com.exactpro.sf.scriptrunner.IScriptRunListener;
import com.exactpro.sf.scriptrunner.ScriptContext;
import com.exactpro.sf.scriptrunner.ScriptProgress;
import com.exactpro.sf.scriptrunner.TestScriptDescription;
import com.exactpro.sf.scriptrunner.ZipReport;
import com.exactpro.sf.storage.ITestScriptStorage;
import com.exactpro.sf.util.DirectoryFilter;
import com.exactpro.sf.util.ReportFilter;

public class DefaultTestScriptStorage implements ITestScriptStorage {

    private static final Logger logger = LoggerFactory.getLogger(DefaultTestScriptStorage.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
    }

    private final IWorkspaceDispatcher workspaceDispatcher;
    private IScriptRunListener scriptRunListener;

    public final static String REPORT_DIR = "reportData";
    public final static String REPORT_FILE = "report.json";

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
    public List<TestScriptDescription> getTestScriptList() {
    	Set<String> reportFiles;

    	try {
            reportFiles = workspaceDispatcher.listFiles(ReportFilter.getInstance(), FolderType.REPORT, true);
    	} catch (FileNotFoundException ex) {
    		logger.error("No REPORT folder", ex);
    		return Collections.emptyList();
    	}

		if (reportFiles.isEmpty()) {
			return Collections.emptyList();
		}

        List<TestScriptDescription> result  = new ArrayList<>();

        try {
            for(String reportFile : reportFiles) {
                try {
                    if(isZip(reportFile)) {
                        loadReportFromZip(reportFile, result);
                    } else {
                        File jsonReport = workspaceDispatcher.getFile(FolderType.REPORT, reportFile, REPORT_DIR,  REPORT_FILE);
                        logger.info("Property file : " + jsonReport.getAbsolutePath());

                        ReportRoot report = OBJECT_MAPPER.readValue(jsonReport, ReportRoot.class);
                        result.add(convertToTestScriptDescription(reportFile, report.getReportProperties()));
                    }
                } catch (FileNotFoundException ex) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("File {} in directory {} not exist", reportFile, REPORT_FILE);
                    }
                }
            }
        } catch(Exception e) {
            logger.error(e.getMessage(), e);
        }

        return result;
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

    protected TestScriptDescription convertToTestScriptDescription(String workFolder, ReportProperties propertiesRootElem) throws SailfishURIException {
        TestScriptDescription testScriptDescription = new TestScriptDescription(scriptRunListener,
                new Date(propertiesRootElem.getTimestamp()), workFolder,
                String.valueOf(propertiesRootElem.getMatrixFile()),
                propertiesRootElem.getRange(),
                propertiesRootElem.getAutostart(),
                propertiesRootElem.getUsername());
        testScriptDescription.setLanguageURI(SailfishURI.parse(propertiesRootElem.getLanguageURI()));

        ScriptProgress scriptProgress = new ScriptProgress(0, new ScriptRunProgressListenerStub());
        scriptProgress.setFailed(propertiesRootElem.getFailed());
        scriptProgress.setLoaded((int) propertiesRootElem.getTotal());
        scriptProgress.setConditionallyPassed(propertiesRootElem.getConditionallyPassed());
        scriptProgress.setPassed(propertiesRootElem.getPassed());
        ScriptContext scriptContext = new ScriptContext(SFLocalContext.getDefault(), scriptProgress, null, null,
                propertiesRootElem.getUsername(), 0, propertiesRootElem.getEnvironmentNameAttr());
        scriptContext.getServiceList().addAll(propertiesRootElem.getServices());

        testScriptDescription.setContext(scriptContext);
        testScriptDescription.setState(propertiesRootElem.getState());
        testScriptDescription.setStatus(propertiesRootElem.getStatus());
        testScriptDescription.setStartedTime(propertiesRootElem.getStartTime());
        testScriptDescription.setFinishedTime(propertiesRootElem.getFinishTime());
        testScriptDescription.setCause(SerializeUtil.deserializeBase64Obj(propertiesRootElem.getCause(), Throwable.class));

        return testScriptDescription;
    }

    protected File[] getReportDirectories(File workFolder){
        return workFolder.listFiles(DirectoryFilter.getInstance());
    }

    public static boolean isZip(String fileName){
        return FilenameUtils.isExtension(fileName, ZipReport.ZIP_EXTENSION);
    }

    protected void loadReportFromZip(String fileName, List<TestScriptDescription> result)
            throws IOException, SailfishURIException {
        File zip = workspaceDispatcher.getFile(FolderType.REPORT, fileName);
        String reportDir = FilenameUtils.getBaseName(zip.getName());
        String propertiesFile = reportDir + '/' + REPORT_DIR + '/' + REPORT_FILE;

        try (ZipFile zipFile = new ZipFile(zip)) {
            try(InputStream inputStream = zipFile.getInputStream(zipFile.getEntry(propertiesFile))) {
                ReportRoot report = OBJECT_MAPPER.readValue(inputStream, ReportRoot.class);
                result.add(convertToTestScriptDescription(fileName, report.getReportProperties()));
            }
        }
    }

    private class ScriptRunProgressListenerStub implements ScriptProgress.IScriptRunProgressListener{
        @Override
        public void onProgressChanged(long id) {

        }
    }

	@Override
	public void onScriptRunEvent(TestScriptDescription descr) {
	}
}
