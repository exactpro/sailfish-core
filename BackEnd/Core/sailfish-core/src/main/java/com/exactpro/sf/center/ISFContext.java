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
package com.exactpro.sf.center;

import java.util.List;
import java.util.Map;

import com.exactpro.sf.common.adapting.IAdapterManager;
import com.exactpro.sf.aml.IPreprocessor;
import com.exactpro.sf.aml.IValidator;
import com.exactpro.sf.aml.converter.MatrixConverterManager;
import com.exactpro.sf.aml.preprocessor.PreprocessorDefinition;
import com.exactpro.sf.bigbutton.RegressionRunner;
import com.exactpro.sf.configuration.EnvironmentManager;
import com.exactpro.sf.configuration.IDataManager;
import com.exactpro.sf.configuration.IDictionaryManager;
import com.exactpro.sf.configuration.ILoggingConfigurator;
import com.exactpro.sf.configuration.netdumper.NetDumperService;
import com.exactpro.sf.configuration.recorder.FlightRecorderService;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.embedded.machinelearning.MachineLearningService;
import com.exactpro.sf.embedded.mail.EMailService;
import com.exactpro.sf.embedded.statistics.StatisticsService;
import com.exactpro.sf.matrixhandlers.MatrixProviderHolder;
import com.exactpro.sf.scriptrunner.AbstractScriptRunner;
import com.exactpro.sf.scriptrunner.IConnectionManager;
import com.exactpro.sf.scriptrunner.actionmanager.IActionManager;
import com.exactpro.sf.scriptrunner.languagemanager.LanguageManager;
import com.exactpro.sf.scriptrunner.reportbuilder.IReportWriter;
import com.exactpro.sf.scriptrunner.services.IStaticServiceManager;
import com.exactpro.sf.scriptrunner.utilitymanager.IUtilityManager;
import com.exactpro.sf.services.IServiceContext;
import com.exactpro.sf.services.ITaskExecutor;
import com.exactpro.sf.storage.IAuthStorage;
import com.exactpro.sf.storage.IMatrixStorage;
import com.exactpro.sf.storage.IMessageStorage;
import com.exactpro.sf.storage.IOptionsStorage;
import com.exactpro.sf.storage.IServiceStorage;
import com.exactpro.sf.storage.ITestScriptStorage;
import com.google.common.collect.ListMultimap;

public interface ISFContext {

    IServiceContext getServiceContext();

	IWorkspaceDispatcher getWorkspaceDispatcher();

	// Storages:
	IMessageStorage getMessageStorage();
	IMatrixStorage getMatrixStorage();
	ITestScriptStorage getTestScriptStorage();
	IOptionsStorage getOptionsStorage();
	IAuthStorage getAuthStorage();

	// Core:
	AbstractScriptRunner getScriptRunner();
	IActionManager getActionManager();
	IUtilityManager getUtilityManager();
	IStaticServiceManager getStaticServiceManager();
	IConnectionManager getConnectionManager();
	IDictionaryManager getDictionaryManager();
	IDataManager getDataManager();
	LanguageManager getLanguageManager();

	// Additional services:
	StatisticsService getStatisticsService();
	MachineLearningService getMachineLearningService();
	EMailService getEMailService();
	RegressionRunner getRegressionRunner();
	FlightRecorderService getFlightRecorderService();
	NetDumperService getNetDumperService();

	// Other
	EnvironmentManager getEnvironmentManager();
    IReportWriter getReportWriter();

    // don't use it in your code. It is prefered to use <code>@ManagedProperty(value="#{" + BeanUtil.MATRIX_PROVIDER_HOLDER + "}")</code>
    @Deprecated
    MatrixProviderHolder getMatrixProviderHolder();
    MatrixConverterManager getMatrixConverterManager();

    List<IValidator> getValidators();
    ListMultimap<IVersion, IValidator> getPluginToValidatorsMap();
	List<IPreprocessor> getPreprocessors();
	ListMultimap<IVersion, PreprocessorDefinition> getPluginToPreprocessorsMap();
	List<IVersion> getPluginVersions();

    Map<String, ClassLoader> getPluginClassLoaders();

	String getVersion();
	String getBranchName();

    void dispose();

    //from Configurator
    IAdapterManager getAdapterManager();
    String getCompilerClassPath();
    ITaskExecutor getTaskExecutor();
    ILoggingConfigurator getLoggingConfigurator();
    IServiceStorage getServiceStorage();
}
