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
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
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
import com.exactpro.sf.scriptrunner.ScriptProgress.IScriptRunProgressListener;
import com.exactpro.sf.scriptrunner.TestScriptDescription;
import com.exactpro.sf.scriptrunner.TestScriptDescription.ScriptState;
import com.exactpro.sf.scriptrunner.TestScriptDescription.ScriptStatus;
import com.exactpro.sf.scriptrunner.ZipReport;
import com.exactpro.sf.scriptrunner.impl.jsonreport.beans.ReportProperties;
import com.exactpro.sf.scriptrunner.impl.jsonreport.beans.ReportRoot;
import com.exactpro.sf.storage.ITestScriptStorage;
import com.exactpro.sf.storage.entities.XmlReportProperties;
import com.exactpro.sf.util.DirectoryFilter;
import com.exactpro.sf.util.ReportFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class DefaultTestScriptStorage implements ITestScriptStorage {

    private static final Logger logger = LoggerFactory.getLogger(DefaultTestScriptStorage.class);

    public static final String REPORT_DATA_DIR = "reportData";
    public static final String ROOT_JSON_REPORT_FILE = REPORT_DATA_DIR + "/report.json";

    @Deprecated
    public static final String XML_PROPERTIES_FILE = "test_script_properties.xml";

    private static final ObjectMapper jsonObjectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);


    private final ThreadLocal<Unmarshaller> xmlUnmarshaller = ThreadLocal.withInitial(() -> {
        try {
            return JAXBContext.newInstance(XmlReportProperties.class).createUnmarshaller();
        } catch (JAXBException e) {
            logger.error("Unable to create xml report properties unmarshaller", e);
            ExceptionUtils.rethrow(e);
        }
        return null;
    });

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

        try {
            return reportFiles.stream().map(this::getReport).filter(Objects::nonNull).collect(Collectors.toList());
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

    private TestScriptDescription getReport(String reportRootPath) {
        try {
            File reportRoot = workspaceDispatcher.getFile(FolderType.REPORT, reportRootPath);

            if (FilenameUtils.isExtension(reportRootPath, ZipReport.ZIP_EXTENSION)) {
                try (ZipFile zipFile = new ZipFile(reportRoot)) {
                    ZipEntry zipXmlProperties = zipFile.getEntry(FilenameUtils.removeExtension(reportRoot.getName()) + "/" + XML_PROPERTIES_FILE);

                    if (zipXmlProperties != null) {
                        try (InputStream stream = zipFile.getInputStream(zipXmlProperties)) {
                            return convertToTestScriptDescription(reportRootPath, (XmlReportProperties) xmlUnmarshaller.get().unmarshal(stream));
                        }
                    } else {
                        ZipEntry zipJson = zipFile.getEntry(FilenameUtils.removeExtension(reportRoot.getName()) + "/" + ROOT_JSON_REPORT_FILE);
                        try (InputStream stream = zipFile.getInputStream(zipJson)) {
                            return convertToTestScriptDescription(reportRootPath, jsonObjectMapper.readValue(stream, ReportRoot.class));
                        }
                    }
                }
            }
            else {
                if (workspaceDispatcher.exists(FolderType.REPORT, reportRootPath, XML_PROPERTIES_FILE)) {
                    File xmlPropertiesFile = workspaceDispatcher.getFile(FolderType.REPORT, reportRootPath, XML_PROPERTIES_FILE);
                    try (InputStream stream = new FileInputStream(xmlPropertiesFile)) {
                        return convertToTestScriptDescription(reportRootPath, (XmlReportProperties) xmlUnmarshaller.get().unmarshal(stream));
                    }
                } else {
                    try (InputStream stream = new FileInputStream(workspaceDispatcher.getFile(FolderType.REPORT, reportRootPath, ROOT_JSON_REPORT_FILE))) {
                        return convertToTestScriptDescription(reportRootPath, jsonObjectMapper.readValue(stream, ReportRoot.class));
                    }
                }
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
