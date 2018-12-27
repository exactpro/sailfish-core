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
package com.exactpro.sf.center.impl;

import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.aml.IPreprocessor;
import com.exactpro.sf.aml.IValidator;
import com.exactpro.sf.aml.converter.MatrixConverterLoader;
import com.exactpro.sf.aml.converter.MatrixConverterManager;
import com.exactpro.sf.aml.preprocessor.PreprocessorDefinition;
import com.exactpro.sf.bigbutton.RegressionRunner;
import com.exactpro.sf.center.IDisposable;
import com.exactpro.sf.center.ISFContext;
import com.exactpro.sf.center.IVersion;
import com.exactpro.sf.center.SFContextSettings;
import com.exactpro.sf.center.SFException;
import com.exactpro.sf.common.adapting.IAdapterManager;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.DataManager;
import com.exactpro.sf.configuration.DefaultAdapterManager;
import com.exactpro.sf.configuration.DictionaryManager;
import com.exactpro.sf.configuration.EnvironmentManager;
import com.exactpro.sf.configuration.IDataManager;
import com.exactpro.sf.configuration.IDictionaryManager;
import com.exactpro.sf.configuration.ILoggingConfigurator;
import com.exactpro.sf.configuration.LoggingConfiguration;
import com.exactpro.sf.configuration.LoggingConfigurator;
import com.exactpro.sf.configuration.netdumper.NetDumperService;
import com.exactpro.sf.configuration.recorder.FlightRecorderService;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.configuration.workspace.WorkspaceStructureException;
import com.exactpro.sf.embedded.machinelearning.MachineLearningService;
import com.exactpro.sf.embedded.mail.EMailService;
import com.exactpro.sf.embedded.statistics.StatisticsService;
import com.exactpro.sf.matrixhandlers.MatrixProviderHolder;
import com.exactpro.sf.scriptrunner.AbstractScriptRunner;
import com.exactpro.sf.scriptrunner.AsyncScriptRunner;
import com.exactpro.sf.scriptrunner.EnvironmentSettings;
import com.exactpro.sf.scriptrunner.EnvironmentSettings.StorageType;
import com.exactpro.sf.scriptrunner.IConnectionManager;
import com.exactpro.sf.scriptrunner.PreprocessorLoader;
import com.exactpro.sf.scriptrunner.ScriptRunnerSettings;
import com.exactpro.sf.scriptrunner.SyncScriptRunner;
import com.exactpro.sf.scriptrunner.ValidatorLoader;
import com.exactpro.sf.scriptrunner.actionmanager.ActionManager;
import com.exactpro.sf.scriptrunner.actionmanager.IActionManager;
import com.exactpro.sf.scriptrunner.impl.DefaultConnectionManager;
import com.exactpro.sf.scriptrunner.languagemanager.LanguageManager;
import com.exactpro.sf.scriptrunner.reportbuilder.DefaultReportWriter;
import com.exactpro.sf.scriptrunner.reportbuilder.IReportWriter;
import com.exactpro.sf.scriptrunner.services.DefaultStaticServiceManager;
import com.exactpro.sf.scriptrunner.services.IStaticServiceManager;
import com.exactpro.sf.scriptrunner.utilitymanager.IUtilityManager;
import com.exactpro.sf.scriptrunner.utilitymanager.UtilityManager;
import com.exactpro.sf.services.DefaultServiceContext;
import com.exactpro.sf.services.IServiceContext;
import com.exactpro.sf.services.ITaskExecutor;
import com.exactpro.sf.services.TaskExecutor;
import com.exactpro.sf.storage.IAuthStorage;
import com.exactpro.sf.storage.IEnvironmentStorage;
import com.exactpro.sf.storage.IMatrixStorage;
import com.exactpro.sf.storage.IMessageStorage;
import com.exactpro.sf.storage.IOptionsStorage;
import com.exactpro.sf.storage.IServiceStorage;
import com.exactpro.sf.storage.IStorage;
import com.exactpro.sf.storage.ITestScriptStorage;
import com.exactpro.sf.storage.impl.DatabaseAuthStorage;
import com.exactpro.sf.storage.impl.DatabaseEnvironmentStorage;
import com.exactpro.sf.storage.impl.DatabaseMatrixStorage;
import com.exactpro.sf.storage.impl.DatabaseMessageStorage;
import com.exactpro.sf.storage.impl.DatabaseOptionsStorage;
import com.exactpro.sf.storage.impl.DatabaseServiceStorage;
import com.exactpro.sf.storage.impl.DefaultTestScriptStorage;
import com.exactpro.sf.storage.impl.DummyAuthStorage;
import com.exactpro.sf.storage.impl.FileEnvironmentStorage;
import com.exactpro.sf.storage.impl.FileMatrixStorage;
import com.exactpro.sf.storage.impl.FileMessageStorage;
import com.exactpro.sf.storage.impl.FileOptionStorage;
import com.exactpro.sf.storage.impl.FileServiceStorage;
import com.exactpro.sf.storage.impl.HibernateFactory;
import com.exactpro.sf.storage.impl.HibernateStorage;
import com.exactpro.sf.storage.impl.MemoryServiceStorage;
import com.google.common.collect.ListMultimap;

public class SFLocalContext implements ISFContext {

    private static final Logger logger = LoggerFactory.getLogger(SFLocalContext.class);

	private static volatile SFLocalContext context = null;

	private final IWorkspaceDispatcher workspaceDispatcher;

	// Context
	private final IServiceContext serviceContext;

	// Stores:
	private final IMessageStorage messageStorage;
	private final IMatrixStorage matrixStorage;
	private final ITestScriptStorage testScriptStorage;
	private final IAuthStorage authStorage;
	private final IOptionsStorage optionsStorage;

	// Core:
	private final AbstractScriptRunner scriptRunner;
	private final ActionManager actionManager;
	private final UtilityManager utilityManager;
	private final DefaultStaticServiceManager staticServiceManager;
	private final DefaultConnectionManager connectionManager;
	private final DictionaryManager dictionaryManager;
	private final DataManager dataManager;
	private final LanguageManager languageManager;

	//Disposable
	private final Queue<IDisposable> disposables = Collections.asLifoQueue(new LinkedList<IDisposable>());

	// Additional services:
	private final StatisticsService statisticsService;
	private final MachineLearningService machineLearningService;
	private EMailService mailService;
	private RegressionRunner regressionRunner;

	private FlightRecorderService flightRecorderService;

	private NetDumperService netDumperService;

	// Other:
	private final EnvironmentManager environmentManager;

	private final MatrixProviderHolder matrixProviderHolder;
	private final MatrixConverterManager matrixConverterManager;
	private final IReportWriter reportWriter;
	private final List<IValidator> validators;
	private final ListMultimap<IVersion, IValidator> pluginToValidators;
	private final List<IPreprocessor> preprocessors;
	private final ListMultimap<IVersion, PreprocessorDefinition> pluginToPreprocessors;
	private final List<IVersion> pluginVersions;
    private final Map<String, ClassLoader> pluginClassLoaders;

    private final IVersion version;
	private final String branchName;

    //from Configurator
    private String compilerClassPath;
    private final ITaskExecutor taskExecutor;
    private final ILoggingConfigurator loggingConfigurator;
    private final DefaultAdapterManager adapterManager;
    private final IServiceStorage serviceStorage;

	/**
	 * Note that getDefault() will return null until createContext() execution will finish.
	 * It means that SFLocalContext is useless in constructors and static initializations of Storages and Managers
	 * (For example static initializators of IService will get null from SFLocalContext.getDefault() )
	 */
	public static ISFContext getDefault() {
		synchronized (SFLocalContext.class) {
			return context;
		}
	}

	public static SFLocalContext createContext(IWorkspaceDispatcher wd, SFContextSettings settings) throws Exception {
	    SFLocalContext localContext = context;
	    if (localContext == null) {
            synchronized (SFLocalContext.class) {
                localContext = context;
                if (localContext == null) {
                    context = localContext = new SFLocalContext(wd, settings);
                    return localContext;
                } else {
                    throw new SFException("SFContext can be created only once");
                }
            }
        }
        return localContext;
	}

	private SFLocalContext(IWorkspaceDispatcher wd, SFContextSettings settings) throws Exception {
		// Initialization:
		// 1) Workspace is ready
		this.workspaceDispatcher = wd;

		// 2) Read configs
		EnvironmentSettings envSettings = new EnvironmentSettings(settings.getEnvironmentConfig());
        envSettings.load(settings.getEnvironmentConfig());

        // logging configuration
        LoggingConfiguration loggingConfiguration = new LoggingConfiguration(settings.getLoggingConfig());
        loggingConfiguration.load(settings.getLoggingConfig());

        this.version = new CoreVersion();
        this.branchName = version.getBranch();

        compilerClassPath = settings.getCompilerClassPath();
		// Set configurator's properties AsSoonAsPossible

        SessionFactory sessionFactory = null;
        IStorage storage = null;

        if(envSettings.getStorageType() == StorageType.DB) {
            sessionFactory = HibernateFactory.getInstance().getSessionFactory(this.workspaceDispatcher);
            Session session = null;

            try {
                session = sessionFactory.openSession();
                session.createQuery("from StoredOption where optionName = 'test'").uniqueResult();
            } catch(Exception e) {
                throw new EPSCommonException("Failed to establish connection to database", e);
            } finally {
                if(session != null) {
                    session.close();
                }
            }

            storage = new HibernateStorage(sessionFactory);
        }

        // 3) Init storages
		staticServiceManager = new DefaultStaticServiceManager();

		matrixStorage = createMatrixStorage(envSettings, sessionFactory);

		authStorage = createAuthStorage(envSettings, storage, workspaceDispatcher, settings.isAuthEnabled());

		testScriptStorage = new DefaultTestScriptStorage(workspaceDispatcher);

		optionsStorage = createOptionsStorage(envSettings, storage, workspaceDispatcher);

		reportWriter = new DefaultReportWriter(wd);
        taskExecutor = new TaskExecutor();
		this.disposables.add(taskExecutor);

		loggingConfigurator = new LoggingConfigurator(wd, loggingConfiguration);

        // 4) Create Managers
        adapterManager = DefaultAdapterManager.getDefault();

		utilityManager = new UtilityManager();

		languageManager = new LanguageManager();

		actionManager = new ActionManager(utilityManager, languageManager);

		dictionaryManager = new DictionaryManager(workspaceDispatcher, utilityManager);

		dataManager = new DataManager(workspaceDispatcher);

		matrixProviderHolder = new MatrixProviderHolder();
		MatrixConverterLoader matrixConverterLoader = new MatrixConverterLoader();

		PreprocessorLoader preprocessorLoader = new PreprocessorLoader(dataManager);
		ValidatorLoader validatorLoader = new ValidatorLoader();

        this.statisticsService = new StatisticsService();

        // 5) Load core & plugins
        PluginLoader pluginLoader = new PluginLoader(
        		workspaceDispatcher,
                staticServiceManager,
                actionManager,
                dictionaryManager,
        		preprocessorLoader,
        		validatorLoader,
        		adapterManager,
                dataManager,
        		languageManager,
				matrixProviderHolder,
                matrixConverterLoader,
                statisticsService,
                this.version);

        LoadInfo loadInfo = pluginLoader.load();
        loadInfo.appendClassPath(compilerClassPath);
        compilerClassPath = loadInfo.getClassPath();

        messageStorage = createMessageStorage(envSettings, sessionFactory, dictionaryManager);
        this.disposables.add(this.messageStorage);

        serviceStorage = createServiceStorage(envSettings, sessionFactory, workspaceDispatcher, staticServiceManager, dictionaryManager, messageStorage);
        this.disposables.add(serviceStorage);

        this.serviceContext = new DefaultServiceContext(dictionaryManager, messageStorage, serviceStorage, loggingConfigurator, taskExecutor, dataManager, wd);

        // 6) Init all services:
        this.connectionManager = new DefaultConnectionManager(
        		staticServiceManager,
        		serviceStorage,
        		createEnvironmentStorage(envSettings, storage, workspaceDispatcher),
        		this.serviceContext);
        this.disposables.add(this.connectionManager);

        this.matrixConverterManager = matrixConverterLoader.create(workspaceDispatcher, dictionaryManager, connectionManager);

		this.environmentManager = new EnvironmentManager(
				messageStorage,
                serviceStorage,
                connectionManager,
				envSettings);

		ScriptRunnerSettings runnerSettings = new ScriptRunnerSettings();
		runnerSettings.setCompilerPriority(envSettings.getMatrixCompilerPriority());
		runnerSettings.setExcludedMessages(envSettings.getExcludedMessages());
		this.scriptRunner = envSettings.isAsyncRunMatrix()
				? new AsyncScriptRunner(workspaceDispatcher, dictionaryManager, actionManager, utilityManager, languageManager, preprocessorLoader, validatorLoader, runnerSettings, statisticsService, environmentManager, testScriptStorage, adapterManager, staticServiceManager, compilerClassPath)
				: new SyncScriptRunner(workspaceDispatcher, dictionaryManager, actionManager, utilityManager, languageManager, preprocessorLoader, validatorLoader, runnerSettings, statisticsService, environmentManager, testScriptStorage, adapterManager, staticServiceManager, compilerClassPath);
		this.disposables.add(this.scriptRunner);

        this.mailService = new EMailService();

		this.flightRecorderService = new FlightRecorderService(taskExecutor, this.optionsStorage);

		this.netDumperService = new NetDumperService(connectionManager, workspaceDispatcher, optionsStorage);
		this.netDumperService.init();

        this.regressionRunner = new RegressionRunner(taskExecutor, workspaceDispatcher, mailService, optionsStorage, statisticsService);

		this.regressionRunner.init();

		this.validators = Collections.unmodifiableList(validatorLoader.getValidators());
		this.pluginToValidators = validatorLoader.getPluginToValidatorsMap();
		this.preprocessors = Collections.unmodifiableList(preprocessorLoader.getPreprocessors());
		this.pluginToPreprocessors = preprocessorLoader.getPluginToPreprocessorsMap();
		this.pluginVersions = pluginLoader.getPluginVersions();
        this.pluginClassLoaders = pluginVersions.stream().collect(Collectors.collectingAndThen(Collectors.toMap(IVersion::getAlias, x -> x.getClass().getClassLoader()), Collections::unmodifiableMap));

        this.machineLearningService = new MachineLearningService(workspaceDispatcher, dictionaryManager, dataManager, pluginClassLoaders);
    }

	private IMessageStorage createMessageStorage(EnvironmentSettings envSettings, SessionFactory sessionFactory, DictionaryManager dictionaryManager) throws WorkspaceStructureException, FileNotFoundException {
		switch (envSettings.getStorageType()) {
        case DB:
            return new DatabaseMessageStorage(workspaceDispatcher, sessionFactory, dictionaryManager);
        case FILE:
            return new FileMessageStorage(envSettings.getFileStoragePath(), envSettings.isStoreAdminMessages(), workspaceDispatcher, dictionaryManager);
        default:
            throw new EPSCommonException("Unsupported message storage type. Check your descriptor.xml file.");
        }
	}

    private IMatrixStorage createMatrixStorage(EnvironmentSettings envSettings, SessionFactory sessionFactory) {
        switch(envSettings.getStorageType()) {
        case DB:
            return new DatabaseMatrixStorage(sessionFactory, workspaceDispatcher);
        case FILE:
            return new FileMatrixStorage(envSettings.getFileStoragePath(), workspaceDispatcher);
        default:
            throw new EPSCommonException("Unsupported matrix storage type. Check your descriptor.xml file.");
        }
	}

    private IServiceStorage createServiceStorage(EnvironmentSettings envSettings, SessionFactory sessionFactory, IWorkspaceDispatcher workspaceDispatcher, IStaticServiceManager staticServiceManager, IDictionaryManager dictionaryManager,
            IMessageStorage messageStorage) {
        switch(envSettings.getStorageType()) {
        case DB:
            return new DatabaseServiceStorage(sessionFactory, staticServiceManager, dictionaryManager, messageStorage);
		case FILE:
            return new FileServiceStorage(envSettings.getFileStoragePath(), workspaceDispatcher, staticServiceManager, messageStorage);
        case MEMORY:
            return new MemoryServiceStorage();
		default:
            throw new EPSCommonException("Unsupported service storage type. Check your descriptor.xml file.");
		}
	}

    private IEnvironmentStorage createEnvironmentStorage(EnvironmentSettings envSettings, IStorage storage, IWorkspaceDispatcher workspaceDispatcher) {
        switch(envSettings.getStorageType()) {
        case DB:
            return new DatabaseEnvironmentStorage(storage);
		case FILE:
            return new FileEnvironmentStorage(envSettings.getFileStoragePath(), workspaceDispatcher);
		default:
            throw new EPSCommonException("Unsupported environment storage type. Check your descriptor.xml file.");
        }
    }

    private IOptionsStorage createOptionsStorage(EnvironmentSettings envSettings, IStorage storage, IWorkspaceDispatcher workspaceDispatcher) {
        switch(envSettings.getStorageType()) {
        case DB:
            return new DatabaseOptionsStorage(storage);
        case FILE:
            return new FileOptionStorage(envSettings.getFileStoragePath(), workspaceDispatcher);
        default:
            throw new EPSCommonException("Unsupported options storage type. Check your descriptor.xml file.");
        }
    }

    private IAuthStorage createAuthStorage(EnvironmentSettings envSettings, IStorage storage, IWorkspaceDispatcher workspaceDispatcher, boolean authEnabled) {
        if(!authEnabled) {
            return new DummyAuthStorage();
        }

        switch(envSettings.getStorageType()) {
        case DB:
            return new DatabaseAuthStorage(storage);
        case FILE:
            throw new EPSCommonException("Authentication is not supported in file storage mode");
        default:
            throw new EPSCommonException("Unsupported auth storage type. Check your descriptor.xml file.");
		}
	}

	@Override
	public IServiceContext getServiceContext() {
        return serviceContext;
    }

	@Override
	public IConnectionManager getConnectionManager() {
		return this.connectionManager;
	}

	@Override
	public IStaticServiceManager getStaticServiceManager() {
		return staticServiceManager;
	}

	@Override
	public IMessageStorage getMessageStorage() {
		return this.messageStorage;
	}

	@Override
	public IWorkspaceDispatcher getWorkspaceDispatcher() {
	    return this.workspaceDispatcher;
	}

	@Override
	public IMatrixStorage getMatrixStorage() {
		return this.matrixStorage;
	}

	@Override
	public IDictionaryManager getDictionaryManager() {
		return dictionaryManager;
	}

	@Override
    public IAuthStorage getAuthStorage() {
        return this.authStorage;
	}

    @Override
    public ITestScriptStorage getTestScriptStorage() {
        return testScriptStorage;
    }

    @Override
	public AbstractScriptRunner getScriptRunner() {
		return scriptRunner;
	}

	@Override
	public void dispose() {
	    while (!this.disposables.isEmpty()) {
	        try {
	            this.disposables.remove().dispose();
	        } catch (RuntimeException e) {
	            logger.error(e.getMessage(), e);
            }
        }
	}

	@Override
	public StatisticsService getStatisticsService() {
		return statisticsService;
	}

	@Override
    public MachineLearningService getMachineLearningService() {
        return machineLearningService;
    }

    @Override
    public EMailService getEMailService() {
        return mailService;
    }

    @Override
    public FlightRecorderService getFlightRecorderService() {
        return flightRecorderService;
    }

    @Override
    public NetDumperService getNetDumperService() {
    	return netDumperService;
    }

    @Override
	public IOptionsStorage getOptionsStorage() {
		return optionsStorage;
	}

	@Override
	public IReportWriter getReportWriter() {
		return this.reportWriter;
	}

	@Override
    public EnvironmentManager getEnvironmentManager() {
		return environmentManager;
	}

    @Override
	public IActionManager getActionManager() {
		return actionManager;
	}

	@Override
	public IUtilityManager getUtilityManager() {
		return utilityManager;
	}

	@Override
	public IDataManager getDataManager() {
		return dataManager;
	}

	@Override
	public LanguageManager getLanguageManager() {
	    return languageManager;
	}

	@Override
	public MatrixProviderHolder getMatrixProviderHolder() {
		return matrixProviderHolder;
	}

	@Override
	public MatrixConverterManager getMatrixConverterManager() {
		return matrixConverterManager;
	}

	@Override
	public List<IValidator> getValidators() {
		return validators;
	}

	@Override
	public List<IPreprocessor> getPreprocessors() {
		return preprocessors;
	}

	@Override
	public RegressionRunner getRegressionRunner() {
		return regressionRunner;
	}

	@Override
    public List<IVersion> getPluginVersions() {
        return pluginVersions;
    }

    @Override
	public String getVersion() {
        return version.buildVersion();
	}

    @Override
	public String getBranchName() {
		return branchName;
	}

    @Override
    public ListMultimap<IVersion, IValidator> getPluginToValidatorsMap() {
        return this.pluginToValidators;
    }

    @Override
    public ListMultimap<IVersion, PreprocessorDefinition> getPluginToPreprocessorsMap() {
        return this.pluginToPreprocessors;
    }

    @Override
    public Map<String, ClassLoader> getPluginClassLoaders() {
        return this.pluginClassLoaders;
    }

    @Override
    public String getCompilerClassPath() {
        return compilerClassPath;
    }

    public void setCompilerClassPath(String compilerClassPath) {
        this.compilerClassPath = compilerClassPath;
    }

    @Override
    public ITaskExecutor getTaskExecutor() {
        return taskExecutor;
    }

    @Override
    public ILoggingConfigurator getLoggingConfigurator() {
        return loggingConfigurator;
    }

    @Override
    public IAdapterManager getAdapterManager() {
        return adapterManager;
    }

    @Override
    public IServiceStorage getServiceStorage() {
        return serviceStorage;
    }
}
