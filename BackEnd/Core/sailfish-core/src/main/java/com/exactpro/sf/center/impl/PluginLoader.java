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

import com.exactpro.sf.center.IVersion;
import com.exactpro.sf.center.SFException;
import com.exactpro.sf.common.logging.CommonLoggers;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.common.util.Utils.FileExtensionFilter;
import com.exactpro.sf.configuration.ILoadableManager;
import com.exactpro.sf.configuration.LoadableManagerContext;
import com.exactpro.sf.configuration.suri.SailfishURIException;
import com.exactpro.sf.configuration.workspace.DefaultWorkspaceLayout;
import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.configuration.workspace.WorkspaceSecurityException;
import com.exactpro.sf.matrixhandlers.IMatrixProviderFactory;
import com.exactpro.sf.matrixhandlers.LocalMatrixProviderFactory;
import com.exactpro.sf.matrixhandlers.MatrixProviderHolder;
import com.exactpro.sf.scriptrunner.PreprocessorLoader;
import com.exactpro.sf.scriptrunner.ValidatorLoader;
import com.exactpro.sf.scriptrunner.services.PluginServiceLoader;
import com.exactpro.sf.util.DirectoryFilter;
import org.apache.log4j.LogManager;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;

public class PluginLoader {

	private static final Logger logger = LoggerFactory.getLogger(PluginLoader.class);
	private final Logger userEventsLogger = CommonLoggers.USER_EVENTS_LOGGER;

	public static final String LOG4J_PROPERTIES_FILE_NAME = "log.properties";
	public static final String CUSTOM_DICTIONARIES_XML = "custom_dictionaries.xml";
	public static final String VERSION_FILE_NAME = "VERSION";

	protected static final String SERVICES_XML_FILE_NAME = "services.xml";
	protected static final String ACTIONS_XML_FILE_NAME = "actions.xml";
	protected static final String DICTIONARIES_XML_FILE_NAME = "dictionaries.xml";
	protected static final String PREPROCESSORS_XML_FILE_NAME = "preprocessors.xml";
	protected static final String VALIDATORS_XML_FILE_NAME = "validators.xml";
	protected static final String ADAPTERS_XML_FILE_NAME = "adapters.xml";
	protected static final String DATA_XML_FILE_NAME = "data.xml";
    protected static final String SERVICES_FOLDER_NAME = "services";

	private final IWorkspaceDispatcher wd;

    private final ILoadableManager staticServiceManager;

    private final ILoadableManager actionManager;

    private final ILoadableManager dictionaryManager;

	private final PreprocessorLoader preprocessorLoader;

	private final ValidatorLoader validatorLoader;

    private final ILoadableManager adapterManager;

    private final ILoadableManager dataManager;

    private final ILoadableManager languageManager;

	private final MatrixProviderHolder matrixProviderHolder;

    private final ILoadableManager matrixConverterManager;

    private final ILoadableManager statisticsReportsLoader;

    private final PluginServiceLoader pluginServiceLoader;

    private final IVersion coreVersion;

    private final List<IVersion> pluginVersions;

	public PluginLoader(
            IWorkspaceDispatcher wd,
            ILoadableManager staticServiceManager,
            ILoadableManager actionManager,
            ILoadableManager dictionaryManager,
            PreprocessorLoader preprocessorLoader,
            ValidatorLoader validatorLoader,
            ILoadableManager adapterManager,
            ILoadableManager dataManager,
            ILoadableManager languageManager,
            MatrixProviderHolder matrixProviderHolder,
            ILoadableManager matrixConverterManager,
            ILoadableManager statisticsReportsLoader,
            PluginServiceLoader pluginServiceLoader,
            IVersion coreVersion) {
		if (wd == null) {
		    throw new NullPointerException("IWorkspaceDispatcher can't be null");
		}

		this.wd = wd;
		this.staticServiceManager = staticServiceManager;
		this.actionManager = actionManager;
		this.dictionaryManager = dictionaryManager;
		this.preprocessorLoader = preprocessorLoader;
		this.validatorLoader = validatorLoader;
		this.adapterManager = adapterManager;
		this.dataManager = dataManager;
		this.languageManager = languageManager;
		this.matrixProviderHolder = matrixProviderHolder;
		this.matrixConverterManager = matrixConverterManager;
        this.statisticsReportsLoader = statisticsReportsLoader;
        this.pluginServiceLoader = pluginServiceLoader;
        this.coreVersion = coreVersion;

		this.pluginVersions = new ArrayList<>();
	}

	public LoadInfo load() throws FileNotFoundException, WorkspaceSecurityException, SailfishURIException {

	    LoadInfo loadInfo = new LoadInfo();

		// load core
		loadPluginFrom(FolderType.ROOT, ".", loadInfo);

		Set<ClassLoader> pluginClassLoaders = new HashSet<>();
        for (String pluginPath : wd.listFiles(DirectoryFilter.getInstance(), FolderType.PLUGINS)) {
            try {
                userEventsLogger.info("Start loading {} plugin", pluginPath);
                IVersion version = extractVersion(wd, pluginPath);
                ClassLoader pluginClassLoader = loadPluginFrom(FolderType.PLUGINS, pluginPath, loadInfo);
                userEventsLogger.info("Plugin {} version {} successfully loaded", pluginPath, version.buildShortVersion());
                if (pluginClassLoader != null) {
                    pluginClassLoaders.add(pluginClassLoader);
                }
            } catch (Exception e) {
                userEventsLogger.error("Can't load plugin from {} - path[{}]. Reason: {}", pluginPath, wd.getFile(FolderType.PLUGINS, pluginPath), e.getMessage());
            }
        }

        pluginClassLoaders.remove(PluginLoader.class.getClassLoader());
        loadInfo.appendClassLoaders(pluginClassLoaders);

        LoadableManagerContext context = new LoadableManagerContext();
        context.setClassLoaders(pluginClassLoaders.toArray(new ClassLoader[0]));

        try {
            if(actionManager != null) {
                actionManager.finalize(context);
            }
        } catch(Exception e) {
            throw new EPSCommonException("Failed to finalize action manager", e);
        }

        loadCustomDictionaries();

        return loadInfo;
	}

	private ClassLoader loadPluginFrom(FolderType folderType, String pluginPath, LoadInfo loadInfo) {
		try {
			if (!wd.exists(folderType, pluginPath)) {
				throw new EPSCommonException("Plugin folder '{" + folderType + "}/" + pluginPath + "' not found");
			}
		} catch (WorkspaceSecurityException e) {
			throw new EPSCommonException("Cannot access plugin's folder '{" + folderType + "}/" + pluginPath + "'", e);
		}

		//
        // Create ClassLoader
        //
        ClassLoader classLoader = PluginLoader.class.getClassLoader();
		if (folderType == FolderType.PLUGINS) {
			try {
				wd.getFile(folderType, pluginPath, "cfg");
			} catch (FileNotFoundException e) {
				logger.info("Plugin folder '{{}}/{}/cfg' not found", folderType, pluginPath);
			}

            // only plug-ins should have special ClassLoader
            if (wd.exists(folderType, pluginPath, "libs")) {
                try {
                    StringBuilder classPath = new StringBuilder();

                    Set<String> libs = wd.listFiles(new FileExtensionFilter("jar"), folderType, pluginPath, "libs");
                    URL[] urls = new URL[libs.size()];
                    Iterator<String> libsIterator = libs.iterator();
                    for (int i = 0; i < libs.size(); i++) {
                        String lib = libsIterator.next();
                        File jar = wd.getFile(folderType, pluginPath, "libs", lib);
                        urls[i] = jar.toURI().toURL();
                        classPath.append(jar.getAbsolutePath()).append(System.getProperty("path.separator"));
                    }
                    classLoader = new URLClassLoader(urls, classLoader);

                    loadInfo.appendClassPath(classPath.toString());
                } catch (FileNotFoundException e) {
                    throw new EPSCommonException("Plugin folder '{" + folderType + "}/" + pluginPath + "/libs' not found", e);
                } catch (MalformedURLException e) {
                    throw new EPSCommonException("Can't resolve some plugin's file path to URL", e);
                }
            }
        }

		IVersion version = null;

		if (folderType == FolderType.PLUGINS) {
			try {
                version = extractVersion(wd, pluginPath);

			    if(version.getMajor() != coreVersion.getMajor() || version.getMinor() != coreVersion.getMinor()) {
                    throw new SFException(String.format("Plugin '%s' has unsupported version: %s.%s (expected: %s.%s)", pluginPath,
                            version.getMajor(), version.getMinor(), coreVersion.getMajor(), coreVersion.getMinor()));
			    }

                if(version.getMinCoreRevision() > coreVersion.getMaintenance()) {
                    throw new SFException(String.format("Plugin '%s' need newer core revision: %s.%s.%s (expected: %s.%s.%s or higher)", pluginPath,
                            coreVersion.getMajor(), coreVersion.getMinor(), coreVersion.getMaintenance(),
                            version.getMajor(), version.getMinor(), version.getMaintenance()));
                }

			    pluginVersions.add(version);
            } catch (FileNotFoundException | WorkspaceSecurityException e) {
                logger.error("Version file does not exist '{{}}/{}/{}': ", folderType, pluginPath, VERSION_FILE_NAME, e);
			} catch(IOException e) {
			    logger.error("Failed to load version file '{{}}/{}/{}': ", folderType, pluginPath, VERSION_FILE_NAME, e);
            }
		} else {
            version = coreVersion;
            pluginVersions.add(version);
		}

		if (version == null) {
		    logger.warn("Plugin '{}' wasn't loaded - no plugin version", pluginPath);
		    return null;
		}

		logger.info("Loading {}", version);

		// root = {resolved folderType} / {pluginPath}
        String root = new File(DefaultWorkspaceLayout.getInstance().getPath(new File("."), folderType), pluginPath).getPath();

		//
		// Load log.properties
		//
		try {
		    File file = wd.getFile(folderType, pluginPath, LOG4J_PROPERTIES_FILE_NAME);
		    logger.info("Loading logger configuration: {{}}/{}/{}", folderType, pluginPath, LOG4J_PROPERTIES_FILE_NAME);
		    try {
                new PropertyConfigurator().doConfigure(file.getPath(), LogManager.getLoggerRepository());
		    } catch (Throwable e) {
		        throw new EPSCommonException("Failed to configure logger. {" + folderType + "}/" + pluginPath + "/" + LOG4J_PROPERTIES_FILE_NAME, e);
		    }
		} catch (FileNotFoundException ex) {
			logger.info("No logger configurations in plugin: {}", pluginPath);
		}

		//
		// LoadableContext
		//
		LoadableManagerContext loadableContext = new LoadableManagerContext();
        loadableContext.setResourceFolder(root);
        loadableContext.setVersion(version);
        loadableContext.setClassLoaders(classLoader);

        //
		// load services (Services + ServiceSettings)
		//
        if (staticServiceManager != null) {
		    try {
			    File file = wd.getFile(folderType, pluginPath, "cfg", SERVICES_XML_FILE_NAME);
                logger.info("Loading services: {{}}/{}/cfg/{}", folderType, pluginPath, SERVICES_XML_FILE_NAME);
			    try (InputStream stream = new FileInputStream(file)) {
			        staticServiceManager.load(loadableContext.setResourceStream(stream));
                } catch (Exception e) {
				    throw new EPSCommonException("Could not load {" + folderType + "}/" + pluginPath + "/cfg/" + SERVICES_XML_FILE_NAME, e);
			    }
		    } catch (FileNotFoundException e) {
                logger.info("No services in plugin: {}", pluginPath);
            }
        } else {
            logger.info("Ignore services [No ServiceManager]. Plugin: {}", pluginPath);
		}

		//
        // Load languages
        //
		if (languageManager != null) {
            try {
                languageManager.load(loadableContext);
            } catch (Exception e) {
                throw new EPSCommonException("Failed to initialize language manager in plugin" + pluginPath);
            }
		} else {
		    logger.info("Ignore languages [No LanguageManager]. Plugin: {}", pluginPath);
        }

		//
		// load Actions (Actions + Utils)
		//
		if (actionManager != null && languageManager != null) {
		    try {
			    File file = wd.getFile(folderType, pluginPath, "cfg", ACTIONS_XML_FILE_NAME);
    			logger.info("Loading actions: {{}}/{}/cfg/{}", folderType, pluginPath, ACTIONS_XML_FILE_NAME);
			    try (InputStream stream = new FileInputStream(file)) {
			        actionManager.load(loadableContext.setResourceStream(stream));
                } catch (Exception e) {
				    throw new EPSCommonException("Failed to initialize action manager. {" + folderType + "}/" + pluginPath + "/cfg/" + ACTIONS_XML_FILE_NAME, e);
			    }
		    } catch (FileNotFoundException e) {
    			logger.info("No actions in plugin: {}", pluginPath);
    		}
		} else {
		    logger.info("Ignore actions [No ActionManager or LanguageManager]. Plugin: {}", pluginPath);
		}

		//
		// Load dictionaries (dictionaries + utils)
		//
		if (dictionaryManager != null) {
		    try {
			    File file = wd.getFile(folderType, pluginPath, "cfg", DICTIONARIES_XML_FILE_NAME);
                logger.info("Loading dictionaries: {{}}/{}/cfg/{}", folderType, pluginPath, DICTIONARIES_XML_FILE_NAME);
			    String pathToDictionaries = Paths.get(root, "cfg", "dictionaries").toString();
			    try (InputStream stream = new FileInputStream(file)) {
			        dictionaryManager.load(loadableContext.setResourceStream(stream).setResourceFolder(pathToDictionaries));
                } catch (Exception e) {
				    throw new EPSCommonException("Failed to initialize dictionary manager. {" + folderType + "}/" + pluginPath + "/cfg/" + DICTIONARIES_XML_FILE_NAME, e);
			    }
		    } catch (FileNotFoundException e) {
    			logger.info("No dictionaries in plugin: {}", pluginPath);
    		}
		} else {
		    logger.info("Ignore dictionaries and utils [No DictionaryManager]. Plugin: {}", pluginPath);
		}

		//
		// Load Validators
		//
		if (validatorLoader != null) {
		    try {
			    File file = wd.getFile(folderType, pluginPath, "cfg", VALIDATORS_XML_FILE_NAME);
    			logger.info("Loading validators: {{}}/{}/cfg/{}", folderType, pluginPath, VALIDATORS_XML_FILE_NAME);
			    try (InputStream stream = new FileInputStream(file)) {
				    validatorLoader.loadValidator(classLoader, stream, version);
			    } catch (IOException | EPSCommonException e) {
				    throw new EPSCommonException("Failed to initialize validators manager. {" + folderType + "}/" + pluginPath + "/cfg/" + VALIDATORS_XML_FILE_NAME, e);
			    }
		    } catch (FileNotFoundException e) {
    			logger.info("No validators in plugin: {}", pluginPath);
    		}
		} else {
		    logger.info("Ignore validators [No ValidatorLoader]. Plugin: {}", pluginPath);
		}

		//
		// Load Adapters
		// configureAdapters();  private IAdapterManager adapterManager; DefaultAdapterManager.getDefault();
		if (adapterManager != null) {
		    try {
			    File file = wd.getFile(folderType, pluginPath, "cfg", ADAPTERS_XML_FILE_NAME);
    			logger.info("Loading adapters:{{}}/{}/cfg/{}", folderType, pluginPath, ADAPTERS_XML_FILE_NAME);
			    try (InputStream stream = new FileInputStream(file)) {
			        adapterManager.load(loadableContext.setResourceStream(stream));
                } catch (Exception e) {
				    throw new EPSCommonException("Failed to initialize adapters manager. {" + folderType + "}/" + pluginPath + "/cfg/" + ADAPTERS_XML_FILE_NAME, e);
			    }
		    } catch (FileNotFoundException e) {
    			logger.info("No adapters in plugin: {}", pluginPath);
    		}
		} else {
		    logger.info("Ignore adapters [No AdapterManager]. Plugin: {}", pluginPath);
		}

		//
		// load data
		//
		if (dataManager != null) {
		    try {
			    File file = wd.getFile(folderType, pluginPath, "cfg", DATA_XML_FILE_NAME);
    			logger.info("Loading data : {{}}/{}/cfg/{}", folderType, pluginPath, DATA_XML_FILE_NAME);
			    String pathToData = Paths.get(root, "data").toString();
			    try (InputStream stream = new FileInputStream(file)) {
			        dataManager.load(loadableContext.setResourceStream(stream).setResourceFolder(pathToData));
			    } catch (Exception e) {
				    throw new EPSCommonException("Failed to initialize data manager. {" + folderType + "}/" + pluginPath + "/cfg/" + DATA_XML_FILE_NAME, e);
			    }
		    } catch (FileNotFoundException e) {
    			logger.info("No data in plugin: {}", pluginPath);
    		} finally {
                try {
                    loadableContext.setResourceFolder(root);
                    dataManager.finalize(loadableContext);
                } catch (Exception e) {
                    throw new EPSCommonException("Failed to finalize action manager", e);
                }
    		}
		} else {
		    logger.info("Ignore data [No DataManager]. Plugin: {}", pluginPath);
		}

		//
		// Load Preprocessors, should be loading after data loading
		//
		if (preprocessorLoader != null) {
		    try {
		        File file = wd.getFile(folderType, pluginPath, "cfg", PREPROCESSORS_XML_FILE_NAME);
                logger.info("Loading preprocessors: {{}}/{}/cfg/{}", folderType, pluginPath, PREPROCESSORS_XML_FILE_NAME);
		        try (InputStream stream = new FileInputStream(file)) {
		            preprocessorLoader.loadPreprocessors(classLoader, stream, version);
		        } catch (IOException | EPSCommonException e) {
		            throw new EPSCommonException("Failed to initialize preprocessors manager. {" + folderType + "}/" + pluginPath + "/cfg/" + PREPROCESSORS_XML_FILE_NAME, e);
		        }
		    } catch (FileNotFoundException e) {
    		    logger.info("No preprocessors in plugin: {}", pluginPath);
    		}
		} else {
		    logger.info("Ignore preprocessors [No PreprocessorLoader]. Plugin: {}", pluginPath);
		}

		//
		// Load MatrixProviders:
		//
		if (matrixProviderHolder != null) {
		    ServiceLoader<IMatrixProviderFactory>  factories = ServiceLoader.load(IMatrixProviderFactory.class, classLoader);
		    try {
			    for (IMatrixProviderFactory factory : factories) {
			        //FIXME: workaround to load LocalMatrixProviderFactory only for core
			        if(!version.isGeneral() && factory instanceof LocalMatrixProviderFactory) {
			            continue;
			        }

				    factory.init(wd);
				    matrixProviderHolder.registerMatrixProvider(version, factory);
				    logger.info("MatrixProvider {} had been loaded", factory.getClass().getCanonicalName());
			    }

		    } catch (ServiceConfigurationError e) {
    			logger.error("Failed to load MatrixProvider from plugin {}", pluginPath);
    		}
		} else {
		    logger.info("Ignore matrix providers [No MatrixProviderHolder]. Plugin: {}", pluginPath);
		}

		//
        // Load MatrixConverters:
        //
		if (matrixConverterManager != null) {
		    try {
                matrixConverterManager.load(loadableContext);
            } catch (ServiceConfigurationError e) {
			    logger.error("Failed to load MatrixConverter", e);
		    } catch (Exception e) {
                throw new EPSCommonException(e);
            }
		} else {
		    logger.info("Ignore matrix converters [No MatrixConverterManager]. Plugin: {}", pluginPath);
		}

        //
        // Load StatisticsReports
        //
        if (statisticsReportsLoader != null) {
            try {
                statisticsReportsLoader.load(loadableContext);
            } catch (ServiceConfigurationError e) {
                logger.error("Failed to load StatisticsReports", e);
            } catch (Exception e) {
                throw new EPSCommonException(e);
            }
        } else {
            logger.info("Ignore statistic reports [No StatisticsReportsLoader]. Plugin: {}", pluginPath);
        }

        // Collect service description files
        if(pluginServiceLoader != null) {
            try {
                logger.info("Collecting service descriptions from: {{}}/{}/{}", folderType, pluginPath, SERVICES_FOLDER_NAME);

                if(wd.exists(folderType, pluginPath, SERVICES_FOLDER_NAME)) {
                    Set<String> fileNames = wd.listFiles(null, folderType, pluginPath, SERVICES_FOLDER_NAME);

                    for(String fileName : fileNames) {
                        File file = wd.getFile(folderType, pluginPath, SERVICES_FOLDER_NAME, fileName);
                        pluginServiceLoader.addDescription(file);
                    }
                }
            } catch(Throwable t) {
                throw new EPSCommonException("Failed to collect service descriptions from: {" + folderType + "}/" + pluginPath + "/" + SERVICES_FOLDER_NAME, t);
            }
        } else {
            logger.info("Ignore plugin service descriptions [No PluginServiceLoader]. Plugin: {}", pluginPath);
        }

        return classLoader;
	}

    private void loadCustomDictionaries() {
        try {
            File file = wd.getFile(FolderType.ROOT, ".", "cfg", CUSTOM_DICTIONARIES_XML);
            logger.info("Loading dictionaries: {{}}/{}/cfg/{}", FolderType.ROOT, ",", CUSTOM_DICTIONARIES_XML);
            String pathToDictionaries = Paths.get("././.", "cfg", "dictionaries").toString();
            try (InputStream stream = new FileInputStream(file)) {
                LoadableManagerContext context = new LoadableManagerContext(coreVersion, pathToDictionaries, stream, PluginLoader.class.getClassLoader());
                dictionaryManager.load(context);
            } catch (Exception e) {
                throw new EPSCommonException(
                        "Failed to initialize dictionary manager. {" + FolderType.ROOT + "}/" + "." + "/cfg/" + CUSTOM_DICTIONARIES_XML);
            }

        } catch (FileNotFoundException e) {
            logger.info("No custom dictionaries");
        }
	}

    public ValidatorLoader getValidatorLoader() {
		return validatorLoader;
	}

    public List<IVersion> getPluginVersions() {
        return Collections.unmodifiableList(pluginVersions);
    }

    public static IVersion extractVersion(IWorkspaceDispatcher dispatcher, String pluginPath) throws IOException {
        File versionFile = dispatcher.getFile(FolderType.PLUGINS, pluginPath, VERSION_FILE_NAME);
        return Version.loadVersion(versionFile);
    }
}