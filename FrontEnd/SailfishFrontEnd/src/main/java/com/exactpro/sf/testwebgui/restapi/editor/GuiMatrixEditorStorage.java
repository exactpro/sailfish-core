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
package com.exactpro.sf.testwebgui.restapi.editor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.aml.AML;
import com.exactpro.sf.aml.AMLException;
import com.exactpro.sf.aml.AMLSettings;
import com.exactpro.sf.aml.IOutputStreamFactory;
import com.exactpro.sf.aml.JsonAML;
import com.exactpro.sf.aml.generator.Alert;
import com.exactpro.sf.aml.reader.struct.AMLMatrix;
import com.exactpro.sf.center.ISFContext;
import com.exactpro.sf.center.impl.SFLocalContext;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.scriptrunner.ScriptContext;
import com.exactpro.sf.scriptrunner.ScriptRunException;
import com.exactpro.sf.storage.IMatrix;

public class GuiMatrixEditorStorage {

	// FIXME: we can release memory a bit quicker by implementing HttpSessionBindingListener.valueUnbound

	private static final Logger logger = LoggerFactory.getLogger(GuiMatrixEditorStorage.class);

	// synchronize access to views
	// matrixId -> Matrix
	private final Map<Long, MatrixWithHistory> views = new HashMap<>();

	// matrixId -> Provider
	private final Map<Long, IReceivedMessageProvider> messageProviders = new HashMap<>();
	// matrixId -> last ExecutionId
	private final Map<Long, Long> executionids = new HashMap<>();

	private final IMatrixStorageProvider storageProvider;
	private EditorConfig config;

	public GuiMatrixEditorStorage(IMatrixStorageProvider storageProvider) {
		this.storageProvider = storageProvider;
	}

	public synchronized MatrixWithHistory getMatrix(long matrixId) throws IOException, InterruptedException, AMLException {
		if (!views.containsKey(matrixId)) {
			IMatrix matrix = storageProvider.getStorage().getMatrixById(matrixId);
			if (matrix == null)
				throw new RuntimeException("Can't find matrix with id = " + matrixId);
			MatrixWithHistory view = loadMatrixFromDisk(matrix, matrix.getFilePath(), SailfishURI.unsafeParse("AML_v3")); // hardcode AML version
			views.put(matrixId, view);
		}

		return views.get(matrixId);
	}

	public MatrixWithHistory updateMatrix(MatrixWithHistory matrix) throws IOException, InterruptedException, AMLException {
		matrix = updateMatrixOnFly(matrix);
		return matrix;
	}

	public void saveMatrix(MatrixWithHistory matrix) throws IOException, AMLException, InterruptedException {
		saveMatrixToDisk(matrix.getFilePath(), matrix.getLanguageURI(), matrix.getMatrix());
		releaseMatrix(matrix);
	}

	// force matrix reload from disk on next getMatrix() call
	public synchronized void releaseMatrix(long matrixId) {
		views.remove(matrixId);
	}

	// force matrix reload from disk on next getMatrix() call
	public synchronized void releaseMatrix(MatrixWithHistory matrix) {
		views.remove(matrix.getId());
	}

	private ScriptContext getScriptContext() {
    	ScriptContext ctx = new ScriptContext(SFLocalContext.getDefault(), null, null, null, "GUI_EDITOR", 0, config.getEnvironment());
    	return ctx;
	}

	private static AMLSettings getAMLSettings(String matrixPath, SailfishURI languageURI) throws IOException {
		IWorkspaceDispatcher wd = SFLocalContext.getDefault().getWorkspaceDispatcher();
		boolean isRealMatrix = !(matrixPath == null || matrixPath.trim().isEmpty());

		AMLSettings amlSetting = new AMLSettings();
		// for generator:
		File reportFolder = Files.createTempDirectory(wd.getFolder(FolderType.REPORT).toPath(), "gui_report").toFile();
		reportFolder.deleteOnExit();

		String reportFolderName = reportFolder.getName();
		amlSetting.setBaseDir(reportFolderName); // relative to REPORT
		amlSetting.setContinueOnFailed(false);
		amlSetting.setAutoStart(false);
		amlSetting.setOrigMatrixPath(matrixPath);

		if (isRealMatrix) {
			File matrixFile = wd.getFile(FolderType.MATRIX, matrixPath);
			try {
				FileUtils.copyFileToDirectory(matrixFile, reportFolder);
			} catch (IOException e) {
				throw new ScriptRunException("Could not copy matrix file [" + matrixPath
						+ "] to the temporary report folder [" + reportFolder.getAbsolutePath() + "]", e);
			}
			amlSetting.setMatrixPath(reportFolderName + File.separator + matrixFile.getName()); // relative to REPORT
		} else {
			amlSetting.setMatrixPath(""); // relative to REPORT
		}

		amlSetting.setLanguageURI(languageURI);

		// Don't create files on hdd:
		amlSetting.setOutputStreamFactory(new IOutputStreamFactory.NullOutputStreamFactory());

		return amlSetting;
	}

	private MatrixWithHistory loadMatrixFromDisk(IMatrix matrix, String matrixPath, SailfishURI languageURI) throws IOException, InterruptedException, AMLException {
        AMLSettings amlSetting = getAMLSettings(matrixPath, languageURI);
        ISFContext defaultSFContext = SFLocalContext.getDefault();
        AML aml = new AML(amlSetting,
        			defaultSFContext.getWorkspaceDispatcher(),
                    defaultSFContext.getAdapterManager(),
                    defaultSFContext.getEnvironmentManager(),
                    defaultSFContext.getDictionaryManager(),
                    defaultSFContext.getStaticServiceManager(),
                    defaultSFContext.getLanguageManager(),
                    defaultSFContext.getActionManager(),
                    defaultSFContext.getUtilityManager(),
                    defaultSFContext.getCompilerClassPath());

        try {
        	aml.run(getScriptContext(), config.getEncoding());
        } catch (AMLException e) {
        	logger.info(e.getMessage(), e);
        	// we will read aml.errors
        }

        MatrixWithHistory result = new MatrixWithHistory(
        		matrix.getId(),
        		matrix.getName(),
        		matrix.getDescription(),
        		languageURI,
        		matrixPath,
        		matrix.getDate());

        result.newCommitedSnapshot(aml.getFullMatrix(), aml.getAlertCollector().getAlerts());
        return result;
    }

	private MatrixWithHistory updateMatrixOnFly(MatrixWithHistory matrix) throws IOException, InterruptedException, AMLException {
        AMLSettings amlSetting = getAMLSettings(null, matrix.getLanguageURI());

        AML aml = new JsonAML(amlSetting,
                              SFLocalContext.getDefault().getWorkspaceDispatcher(),
                              SFLocalContext.getDefault().getAdapterManager(),
                              SFLocalContext.getDefault().getEnvironmentManager(),
                              SFLocalContext.getDefault().getDictionaryManager(),
                              SFLocalContext.getDefault().getStaticServiceManager(),
                              SFLocalContext.getDefault().getLanguageManager(),
                              SFLocalContext.getDefault().getActionManager(),
                              SFLocalContext.getDefault().getUtilityManager(),
                              SFLocalContext.getDefault().getCompilerClassPath(),
                              matrix.getMatrix());

        Set<Alert> errors = new HashSet<>();
        try {
        	aml.run(getScriptContext(), "UTF-8");
        } catch (AMLException e) {
        	logger.info(e.getMessage(), e);
        	errors.addAll(e.getAlertCollector().getAlerts());
        }
        errors.addAll(aml.getAlertCollector().getAlerts());

        matrix.commitSnapshot(aml.getFullMatrix(), errors);

		return matrix;
	}

	private void saveMatrixToDisk(String matrixPath, SailfishURI languageURI, AMLMatrix matrix)
			throws IOException, InterruptedException, AMLException {
        AMLSettings amlSetting = getAMLSettings(matrixPath, languageURI);
        AML aml = new JsonAML(amlSetting,
                              SFLocalContext.getDefault().getWorkspaceDispatcher(),
                              SFLocalContext.getDefault().getAdapterManager(),
                              SFLocalContext.getDefault().getEnvironmentManager(),
                              SFLocalContext.getDefault().getDictionaryManager(),
                              SFLocalContext.getDefault().getStaticServiceManager(),
                              SFLocalContext.getDefault().getLanguageManager(),
                              SFLocalContext.getDefault().getActionManager(),
                              SFLocalContext.getDefault().getUtilityManager(),
                              SFLocalContext.getDefault().getCompilerClassPath(),
                              matrix);
        try {
        	aml.run(getScriptContext(), this.config.getEncoding());
        } catch (AMLException e) {
        	logger.info(e.getMessage(), e);
        	// ignore this errors
        }
		aml.writeMatrix(aml.getFullMatrix());
	}

	public synchronized void addMessageProvider(Long matrixId, IReceivedMessageProvider provider) {
		messageProviders.put(matrixId, provider);
	}

	public synchronized IReceivedMessageProvider getMessageProvider(long matrixId) {
		return messageProviders.get(matrixId);
	}

	public synchronized IReceivedMessageProvider releaseMessageProvider(long matrixId) {
		executionids.remove(matrixId);
		return messageProviders.remove(matrixId);
	}

	public synchronized long getLastExecutionId(long matrixId) {
		Long result = executionids.get(matrixId);
		return (result == null) ? -1 : result;
	}

	public synchronized void putExecutionId(long matrixId, long executionId) {
		executionids.put(matrixId, executionId);
	}

	public synchronized void configure(EditorConfig config) {
		this.config = config;
		if (config.getEncoding() == null) {
			this.config.setEncoding("UTF-8");
		}
		if (config.getEnvironment() == null) {
			this.config.setEnvironment("default");
		}

	}

	public EditorConfig getConfig() {
		return config;
	}

}

