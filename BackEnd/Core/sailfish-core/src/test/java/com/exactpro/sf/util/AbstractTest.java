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
package com.exactpro.sf.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.PropertyConfigurator;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.loaders.IDictionaryStructureLoader;
import com.exactpro.sf.common.messages.structures.loaders.XmlDictionaryStructureLoader;
import com.exactpro.sf.common.services.ServiceInfo;
import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.aml.script.DefaultSettings;
import com.exactpro.sf.center.SFContextSettings;
import com.exactpro.sf.center.impl.SFLocalContext;
import com.exactpro.sf.comparison.ComparatorSettings;
import com.exactpro.sf.comparison.ComparisonResult;
import com.exactpro.sf.comparison.ComparisonUtil;
import com.exactpro.sf.comparison.MessageComparator;
import com.exactpro.sf.configuration.IDataManager;
import com.exactpro.sf.configuration.IDictionaryManager;
import com.exactpro.sf.configuration.IEnvironmentManager;
import com.exactpro.sf.configuration.workspace.DefaultWorkspaceDispatcherBuilder;
import com.exactpro.sf.configuration.workspace.DefaultWorkspaceLayout;
import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.scriptrunner.DebugController;
import com.exactpro.sf.scriptrunner.IConnectionManager;
import com.exactpro.sf.scriptrunner.IReportStats;
import com.exactpro.sf.scriptrunner.IScriptConfig;
import com.exactpro.sf.scriptrunner.IScriptReport;
import com.exactpro.sf.scriptrunner.ScriptContext;
import com.exactpro.sf.scriptrunner.ScriptRunException;
import com.exactpro.sf.scriptrunner.StatusType;
import com.exactpro.sf.services.IServiceContext;
import com.exactpro.sf.storage.IMessageStorage;
import com.exactpro.sf.storage.IServiceStorage;

public class AbstractTest {

    private static final Logger logger = LoggerFactory.getLogger(AbstractTest.class);

	protected static final String BIN_FOLDER_PATH = "build/test-results";

	protected static final File BASE_DIR = new File((System.getProperty("basedir") == null) ? "." : System.getProperty("basedir"));
	protected static final File SAILFISH_DICTIONARY_PATH = new File("cfg/dictionaries/");
	protected static final String WORKSPACE_FOLDER = BIN_FOLDER_PATH + File.separator + "workspace" + File.separator;
    protected static final String LOG_PROPERTIES_PATH = "log4j.properties";

    protected static IServiceContext serviceContext;
    protected static IWorkspaceDispatcher workspaceDispatcher;

	private static boolean isLoggingAlreadyConfigured = false;
	private static boolean isAlreadyConfigured = false;

    private static final String DEFAULT_CFG_FILENAME = "sf.cfg.xml";

    public static class TestWorkspaceLayout extends DefaultWorkspaceLayout {

    	@Override
    	public String getPath(File root, FolderType folderType) {
    		switch (folderType) {
    		case CFG:
				return new StringBuilder(root.getPath())
	                .append(File.separator).append("cfg")
	                .toString();
			case MATRIX:
				return root.getPath();
			case CSV:
				return WORKSPACE_FOLDER + "csv";
			case LOGS:
				return WORKSPACE_FOLDER + "logs";
			case REPORT:
				return WORKSPACE_FOLDER + "report";
			case TEST_LIBRARY:
    			return WORKSPACE_FOLDER + "test_library";
			case ROOT:
				return root.getPath();
			default:
    			return super.getPath(root, folderType);
    		}
    	}
    }

    @BeforeClass
    public static void initTestToolsTestCase() throws Throwable {
		synchronized(logger) {
		    try {
    			if(!isLoggingAlreadyConfigured) {
                    try (InputStream inputStream = AbstractTest.class.getClassLoader().getResourceAsStream(LOG_PROPERTIES_PATH);) {
                        PropertyConfigurator.configure(inputStream);
                    }
    			}
		    } finally {
		        isLoggingAlreadyConfigured = true;
		    }
		    try {
		        if (!isAlreadyConfigured) {
		            workspaceDispatcher = new DefaultWorkspaceDispatcherBuilder()
                        .addWorkspaceLayer(new File(BASE_DIR.getAbsolutePath(), "src/main/workspace"), new TestWorkspaceLayout())
                    	.addWorkspaceLayer(new File(BASE_DIR.getAbsolutePath(), "src/gen/workspace"), new TestWorkspaceLayout())
                    	.addWorkspaceLayer(new File(BASE_DIR.getAbsolutePath(), "src/provided/workspace"), new TestWorkspaceLayout())
                    	.addWorkspaceLayer(new File(BASE_DIR.getAbsolutePath(), "src/test/workspace"), new TestWorkspaceLayout())
							.addWorkspaceLayer(new File(BASE_DIR.getAbsolutePath(), "src/main/plugin"), new TestWorkspaceLayout())
							.addWorkspaceLayer(new File(BASE_DIR.getAbsolutePath(), "src/gen/plugin"), new TestWorkspaceLayout())
							.addWorkspaceLayer(new File(BASE_DIR.getAbsolutePath(), "src/provided/plugin"), new TestWorkspaceLayout())
							.addWorkspaceLayer(new File(BASE_DIR.getAbsolutePath(), "src/test/plugin"), new TestWorkspaceLayout())
						.addWorkspaceLayer(Files.createTempDirectory("sf-tests").toFile(), DefaultWorkspaceLayout.getInstance())
                    	.build();

					SFContextSettings settings = new SFContextSettings();
                    settings.setConfig(loadDefaultEnvironment(workspaceDispatcher));
                    settings.setCompilerClassPath(System.getProperty("java.class.path"));
                    serviceContext = SFLocalContext.createContext(workspaceDispatcher, settings).getServiceContext();
                    serviceContext = Mockito.spy(serviceContext);

                    Mockito.when(serviceContext.lookupService(ArgumentMatchers.any(ServiceName.class))).thenAnswer(invocation -> {
                        ServiceName serviceName = invocation.getArgument(0);
                        return new ServiceInfo(serviceName.toString(), serviceName);
                    });
		        }
		    } catch (Throwable e) {
		    	System.err.println(e);
	            logger.error(e.getMessage(), e);
	            throw e;
		    } finally {
		        isAlreadyConfigured = true;
		    }
		}
	}

	public static void equals(IMessage msg, IMessage result) {
        equals(msg, result, new ComparatorSettings());
    }

    public static void equals(IMessage msg, IMessage result, ComparatorSettings settings) {
        ComparisonResult comparisonResult = MessageComparator.compare(msg, result, settings);

        Assert.assertNotNull(comparisonResult);
        Assert.assertEquals(0, ComparisonUtil.getResultCount(comparisonResult, StatusType.NA));
        Assert.assertEquals(0, ComparisonUtil.getResultCount(comparisonResult, StatusType.FAILED));
        Assert.assertEquals(getFieldCount(msg), ComparisonUtil.getResultCount(comparisonResult, StatusType.PASSED));
    }

	protected static HierarchicalConfiguration loadDefaultEnvironment(IWorkspaceDispatcher wd) {
		try {
			File file = wd.getFile(FolderType.CFG, DEFAULT_CFG_FILENAME);
			return new XMLConfiguration(file);
		} catch (ConfigurationException e) {
			throw new ScriptRunException("Exception during configuration loading", e);
		} catch (Throwable e) {
			logger.error("Exception during environment initialization", e);
			throw new ScriptRunException("Exception during environment initialization", e);
		}
	}

	protected IDictionaryStructure loadMessageDictionary(InputStream inputStream) throws IOException {
	    IDictionaryStructureLoader loader = new XmlDictionaryStructureLoader();
	    return loader.load(inputStream);
	}

    /**
     * Creates {@link ScriptContext} mock suitable to be used for {@link DefaultSettings} init
     */
    protected ScriptContext getScriptContext() {
        ScriptContext scriptContext = Mockito.mock(ScriptContext.class);
        IScriptReport scriptReport = Mockito.mock(IScriptReport.class);
        IReportStats reportStats = Mockito.mock(IReportStats.class);
        IEnvironmentManager environmentManager = Mockito.mock(IEnvironmentManager.class);
        IConnectionManager connectionManager = Mockito.mock(IConnectionManager.class);
        IWorkspaceDispatcher workspaceDispatcher = Mockito.mock(IWorkspaceDispatcher.class);
        IScriptConfig scriptConfig = Mockito.mock(IScriptConfig.class);
        DebugController debugController = Mockito.mock(DebugController.class);
        IDictionaryManager dictionaryManager = Mockito.mock(IDictionaryManager.class);
        IMessageStorage messageStorage = Mockito.mock(IMessageStorage.class);
        IServiceStorage serviceStorage = Mockito.mock(IServiceStorage.class);
        IDataManager dataManager = Mockito.mock(IDataManager.class);
        Logger logger = Mockito.mock(Logger.class);

        Mockito.when(scriptContext.getScriptConfig()).thenReturn(scriptConfig);
        Mockito.when(scriptContext.getReport()).thenReturn(scriptReport);
        Mockito.when(scriptContext.getEnvironmentManager()).thenReturn(environmentManager);
        Mockito.when(scriptContext.getWorkspaceDispatcher()).thenReturn(workspaceDispatcher);
        Mockito.when(scriptContext.getDebugController()).thenReturn(debugController);
        Mockito.when(scriptContext.getDictionaryManager()).thenReturn(dictionaryManager);
        Mockito.when(scriptContext.getDataManager()).thenReturn(dataManager);
        Mockito.when(environmentManager.getConnectionManager()).thenReturn(connectionManager);
        Mockito.when(environmentManager.getMessageStorage()).thenReturn(messageStorage);
        Mockito.when(environmentManager.getServiceStorage()).thenReturn(serviceStorage);
        Mockito.when(scriptConfig.getReportFolder()).thenReturn(BIN_FOLDER_PATH);
        Mockito.when(scriptConfig.getLogger()).thenReturn(logger);
        Mockito.when(scriptReport.getReportStats()).thenReturn(reportStats);

        return scriptContext;
    }

    private static int getFieldCount(IMessage message) {
        int all = 0;
        for (String fieldName : message.getFieldNames()) {
            Object field = message.getField(fieldName);

            if (field instanceof IMessage) {
                all += getFieldCount((IMessage) field);
                continue;
            }

            if (field instanceof List<?>) {
                for (Object nested : (List<?>)field) {
                    if (nested instanceof IMessage) {
                        all += getFieldCount((IMessage) nested);
                    } else {
                        all++;
                    }
                }
                continue;
            }

            all++;
        }
        return all;
    }
}
