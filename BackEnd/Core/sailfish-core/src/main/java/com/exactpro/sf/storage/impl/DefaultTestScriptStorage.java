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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipFile;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.SerializeUtil;
import com.exactpro.sf.center.impl.SFLocalContext;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.suri.SailfishURIException;
import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.scriptrunner.IScriptRunListener;
import com.exactpro.sf.scriptrunner.PropertiesReport;
import com.exactpro.sf.scriptrunner.ScriptContext;
import com.exactpro.sf.scriptrunner.ScriptProgress;
import com.exactpro.sf.scriptrunner.TestScriptDescription;
import com.exactpro.sf.scriptrunner.ZipReport;
import com.exactpro.sf.storage.ITestScriptStorage;
import com.exactpro.sf.util.DirectoryFilter;
import com.exactpro.sf.util.ReportFilter;

public class DefaultTestScriptStorage implements ITestScriptStorage {

    private static final Logger logger = LoggerFactory.getLogger(DefaultTestScriptStorage.class);

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
        SAXBuilder builder = new SAXBuilder();

        try {
            for(String reportFile : reportFiles) {
                try {
                    if(isZip(reportFile)) {
                        loadReportFromZip(reportFile, result, builder);
                    } else {
                        File propertiesFile = workspaceDispatcher.getFile(FolderType.REPORT, reportFile, PropertiesReport.PROPERTIES_FILE_NAME);
                        logger.info("Property file : " + propertiesFile.getAbsolutePath());

                        try (FileInputStream propertiesFileStream = new FileInputStream(propertiesFile)) {
                            Document document = builder.build(propertiesFileStream);
                            result.add(convertToTestScriptDescription(reportFile, document.getRootElement()));
                        }
                    }
                } catch (FileNotFoundException ex) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("File {} in directory {} not exist", PropertiesReport.PROPERTIES_FILE_NAME, reportFile);
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

    protected TestScriptDescription convertToTestScriptDescription(String workFolder, Element propertiesRootElem) throws SailfishURIException {
        TestScriptDescription testScriptDescription = new TestScriptDescription(scriptRunListener,
                new Date(Long.valueOf(propertiesRootElem.getChildText(PropertiesReport.TIMESTAMP_ATTR_NAME))),workFolder,
                propertiesRootElem.getChildText(PropertiesReport.MATRIX_FILE_NAME_ATTR_NAME),
                propertiesRootElem.getChildText(PropertiesReport.RANGE),
                Boolean.valueOf(propertiesRootElem.getChildText(PropertiesReport.AUTOSTART)),
                propertiesRootElem.getChildText(PropertiesReport.USER));

        testScriptDescription.setLanguageURI(SailfishURI.parse(propertiesRootElem.getChildText(PropertiesReport.LANGUAGE_URI_ATTR_NAME)));

        ScriptProgress scriptProgress = new ScriptProgress(0, new ScriptRunProgressListenerStub());
        scriptProgress.setFailed(Long.valueOf(propertiesRootElem.getChildText(PropertiesReport.FAILED_ATTR_NAME)));
        scriptProgress.setLoaded(Integer.valueOf(propertiesRootElem.getChildText(PropertiesReport.TOTAL_ATTR_NAME)));
        scriptProgress.setConditionallyPassed(Long.valueOf(propertiesRootElem.getChildText(PropertiesReport.CONDITIONALLY_PASSED_ATTR_NAME)));
        scriptProgress.setPassed(Long.valueOf(propertiesRootElem.getChildText(PropertiesReport.PASSED_ATTR_NAME)));
        ScriptContext scriptContext = new ScriptContext(SFLocalContext.getDefault(), scriptProgress, null, null,
                propertiesRootElem.getChildText(PropertiesReport.USER), 0, propertiesRootElem.getChildText(PropertiesReport.ENVIRONMENT_NAME_ATTR_NAME));

        testScriptDescription.setContext(scriptContext);
        testScriptDescription.setState(
                TestScriptDescription.ScriptState.valueOf(propertiesRootElem.getChildText(PropertiesReport.STATE_ATTR_NAME)));
        testScriptDescription.setStatus(
                TestScriptDescription.ScriptStatus.valueOf(propertiesRootElem.getChildText(PropertiesReport.STATUS_ATTR_NAME)));
        testScriptDescription.setStartedTime(Long.parseLong(propertiesRootElem.getChildText(PropertiesReport.START_TIME)));
        testScriptDescription.setFinishedTime(Long.parseLong(propertiesRootElem.getChildText(PropertiesReport.FINISH_TIME)));
        testScriptDescription.setCause(SerializeUtil.deserializeBase64Obj(propertiesRootElem.getChildText(PropertiesReport.CAUSE), Throwable.class));

        return testScriptDescription;
    }

    protected File[] getReportDirectories(File workFolder){
        return workFolder.listFiles(DirectoryFilter.getInstance());
    }

    public static boolean isZip(String fileName){
        return FilenameUtils.isExtension(fileName, ZipReport.ZIP_EXTENSION);
    }

    protected void loadReportFromZip(String fileName, List<TestScriptDescription> result, SAXBuilder builder)
            throws IOException, SailfishURIException, JDOMException {
        File zip = workspaceDispatcher.getFile(FolderType.REPORT, fileName);
        String reportDir = FilenameUtils.getBaseName(zip.getName());
        String propertiesFile = reportDir + "/" + PropertiesReport.PROPERTIES_FILE_NAME;

        try (ZipFile zipFile = new ZipFile(zip)) {
            try(InputStream inputStream = zipFile.getInputStream(zipFile.getEntry(propertiesFile))) {
                Document document = builder.build(inputStream);
                result.add(convertToTestScriptDescription(fileName, document.getRootElement()));
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
