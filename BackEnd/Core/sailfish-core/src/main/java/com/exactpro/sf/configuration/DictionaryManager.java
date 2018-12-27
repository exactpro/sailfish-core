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
package com.exactpro.sf.configuration;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.aml.Dictionaries;
import com.exactpro.sf.aml.Dictionary;
import com.exactpro.sf.aml.DictionarySettings;
import com.exactpro.sf.center.IVersion;
import com.exactpro.sf.center.impl.PluginLoader;
import com.exactpro.sf.common.impl.messages.DefaultMessageFactory;
import com.exactpro.sf.common.impl.messages.DummyMessageFactory;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.loaders.IDictionaryStructureLoader;
import com.exactpro.sf.common.messages.structures.loaders.XmlDictionaryStructureLoader;
import com.exactpro.sf.common.messages.structures.loaders.XsdDictionaryStructureLoader;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.suri.SailfishURIException;
import com.exactpro.sf.configuration.suri.SailfishURIRule;
import com.exactpro.sf.configuration.suri.SailfishURIUtils;
import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.configuration.workspace.WorkspaceSecurityException;
import com.exactpro.sf.configuration.workspace.WorkspaceStructureException;
import com.exactpro.sf.scriptrunner.ScriptRunException;
import com.exactpro.sf.scriptrunner.utilitymanager.UtilityClass;
import com.exactpro.sf.scriptrunner.utilitymanager.UtilityInfo;
import com.exactpro.sf.scriptrunner.utilitymanager.UtilityManager;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

public class DictionaryManager implements IDictionaryManager, ILoadableManager {

	private static final Logger logger = LoggerFactory.getLogger(DictionaryManager.class);

	private final IWorkspaceDispatcher workspaceDispatcher;

	private final UtilityManager utilityManager;

	// URI -> file_name (relative to DICTIONARIES)
	private final Map<SailfishURI, String> location = new HashMap<>();
	// URI -> DictionarySettings
	private final Map<SailfishURI, DictionarySettings> dictSettings = new HashMap<>();
	// URI -> DictionaryStructure
	private final Map<SailfishURI, IDictionaryStructure> dicts = new HashMap<>();
	// URI -> IMessageFactory
	private final Map<SailfishURI, IMessageFactory> factories = new HashMap<>();
	// plugin alias -> list of dictionary URI's
	private final SetMultimap<String, SailfishURI> pluginDictTitles = HashMultimap.create();

	private long dictLoadedCounter = 0;

	private Map<SailfishURI, Long> dictionaryIds = new ConcurrentHashMap<>();

	private final List<IDictionaryManagerListener> eventListeners;

	public DictionaryManager(final IWorkspaceDispatcher workspaceDispatcher, UtilityManager utilityManager) {
        this.workspaceDispatcher = Objects.requireNonNull(workspaceDispatcher, "workspaceDispatcher cannot be null");
        this.utilityManager = Objects.requireNonNull(utilityManager, "utilityManager cannot be null");
		this.eventListeners = new CopyOnWriteArrayList<>();
	}

    @Override
    public synchronized Set<SailfishURI> getDictionaryURIs() {
        return new HashSet<>(location.keySet());
    }

    @Override
    public synchronized Set<SailfishURI> getDictionaryURIs(String pluginAlias) {
        if (pluginAlias == null || !this.pluginDictTitles.containsKey(pluginAlias)) {
            logger.error("Dictionary titles for plugin alias '{}' not found", pluginAlias);
            return null;
        }
        return this.pluginDictTitles.get(pluginAlias);
    }

    @Override
	public synchronized List<SailfishURI> getCachedDictURIs() {
		return new ArrayList<>(this.dicts.keySet());
	}

    @Override
    public synchronized Map<SailfishURI, String> getDictionaryLocations() {
        return new HashMap<>(location);
    }

    @Override
	public void load(ILoadableManagerContext context) {
		try {
		    final ClassLoader loader = context.getClassLoaders()[0];
		    final InputStream stream = context.getResourceStream();
		    final String dictionaryFolderPath = context.getResourceFolder();
            final IVersion version = context.getVersion();
		    
			JAXBContext jc = JAXBContext.newInstance(Dictionaries.class);
			Unmarshaller u = jc.createUnmarshaller();

			JAXBElement<Dictionaries> root = u.unmarshal(new StreamSource(stream), Dictionaries.class);
			Dictionaries dictionaries = root.getValue();

			for (Dictionary dict : dictionaries.getDictionary()) {

			    if (dict.getTitle() == null) {
                    throw new EPSCommonException("Null Title in config " + stream);
                }

                SailfishURI dictionaryURI = new SailfishURI(version.getAlias(), null, SailfishURIUtils.sanitize(dict.getTitle()));
                DictionarySettings settings = dictSettings.get(dictionaryURI);

			    this.pluginDictTitles.put(dictionaryURI.getPluginAlias(), dictionaryURI);

			    if (settings == null) {
			        settings = new DictionarySettings();
			        settings.setURI(dictionaryURI);
			        this.dictSettings.put(dictionaryURI, settings);
			    }

			    for (String className : dict.getUtilityClassName()){
			        UtilityClass utilityClass = utilityManager.load(loader, className, version);

			        for(String utilityClassAlias : utilityClass.getClassAliases()) {
			            SailfishURI utilityClassURI = new SailfishURI(version.getAlias(), utilityClassAlias);
			            settings.addUtilityClassURI(utilityClassURI);
			        }
                }

                for (String utilityURI : dict.getUtilityURI()) {
                    SailfishURI uri = SailfishURI.parse(utilityURI, SailfishURIRule.REQUIRE_PLUGIN, SailfishURIRule.REQUIRE_CLASS);
                    if (utilityManager.getUtilityClassByURI(uri) == null) {
                        logger.warn("SailfishURI {} is not registered", uri);
                    } else {
                        settings.addUtilityClassURI(uri);
                    }
                }

                String currentResource = this.location.get(dictionaryURI);
                String resource = dict.getResource();
                if (resource == null) {
                	logger.warn("resource (xml dictionary) not specified for dictionary {}", dictionaryURI);
                } else {
                	resource = dictionaryFolderPath + File.separator + resource;
                    if (currentResource != null && !currentResource.equals(resource)) {
                        logger.warn("Resources '{}' wasn't sent, because current value '{}' not null", resource, currentResource);
                    } else {
                    	this.location.put(dictionaryURI, resource);
                    }
                }

                String factoryClassName = dict.getFactoryClassName();
                if (factoryClassName != null) {
                    if (settings.getFactoryClass() != null && !settings.getFactoryClass().getCanonicalName().equals(factoryClassName)) {
                        logger.warn("Factory class '{}' wasn't sent, because current value '{}' not null", factoryClassName, settings.getFactoryClass());
                    } else {
                        try {
                            @SuppressWarnings("unchecked")
                            Class<? extends IMessageFactory> factoryClass = (Class<? extends IMessageFactory>) loader.loadClass(factoryClassName);
                            settings.setFactoryClass(factoryClass);
                        } catch (ClassNotFoundException e) {
                            logger.warn("{} dictionary has incorrect factory [{}]. DefaultMessageFactory will be used",
                                    dict.getTitle(), dict.getFactoryClassName());
                        }
                    }
                }
            }
        } catch (JAXBException | SailfishURIException e) {
			throw new EPSCommonException("Failed to load dictionary", e);
		}
	}

	@Override
    public void finalize(ILoadableManagerContext context) throws Exception {
        // TODO Auto-generated method stub
    }

	@Override
	public synchronized IDictionaryStructure getDictionary(SailfishURI uri) throws RuntimeException {
		IDictionaryStructure dict = SailfishURIUtils.getMatchingValue(uri, this.dicts, SailfishURIRule.REQUIRE_RESOURCE);

		if (dict == null) {

			this.dictLoadedCounter++;
			this.dictionaryIds.put(uri, this.dictLoadedCounter);

			String resource = SailfishURIUtils.getMatchingValue(uri, this.location, SailfishURIRule.REQUIRE_RESOURCE);

			if (resource == null) {
				throw new RuntimeException("No dictionary found for URI: " + uri);
			}

			dict = this.createMessageDictionary(resource);

			if (dict == null) {
				throw new RuntimeException("Can not create dictionary for URI: " + uri +", resource = "+resource);
			}

			this.dicts.put(uri, dict);

			logger.info("Dictionary {} was loaded", uri);
		}

		return dict;
	}

	@Override
	public synchronized DictionarySettings getSettings(SailfishURI uri) {
	    return SailfishURIUtils.getMatchingValue(uri, dictSettings, SailfishURIRule.REQUIRE_RESOURCE);
	}

	@Override
	public synchronized  IMessageFactory getMessageFactory(SailfishURI uri) {
		IMessageFactory factory = SailfishURIUtils.getMatchingValue(uri, factories, SailfishURIRule.REQUIRE_RESOURCE);
		if (factory == null) {
            factory = ObjectUtils.defaultIfNull(loadFactory(uri), DefaultMessageFactory.getFactory());
			factories.put(uri, factory);
		}
		return factory;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (Entry<SailfishURI, IDictionaryStructure> e : this.dicts.entrySet()) {
			if (sb.length() > 0)
				sb.append(", ");
			sb.append(e.getKey());
			sb.append(" = ");
			sb.append(e.getValue().getNamespace());
		}
		return sb.toString();
	}

	@Override
	public synchronized void invalidateDictionaries(SailfishURI ... uris) {
	    if (uris == null || uris.length == 0) {
		    this.invalidateEvent(Collections.unmodifiableSet(this.dicts.keySet()));
		    this.dicts.clear();
	        this.dictionaryIds.clear();
	        this.factories.clear();
            logger.info("All dictionaries have been invalidated");
	    } else {
		    this.invalidateEvent(new HashSet<>(Arrays.asList(uris)));
	        for (SailfishURI uri : uris) {
                this.dicts.remove(uri);
                this.dictionaryIds.remove(uri);
                this.factories.remove(uri);
            }
            StringBuilder builder = new StringBuilder("Dictionaries ")
                .append(Arrays.toString(uris))
                .append(" have been invalidated");

            logger.info("{}", builder);
	    }
	}


    private IMessageFactory loadFactory(SailfishURI uri) {
        try {
            DictionarySettings settings = SailfishURIUtils.getMatchingValue(uri, dictSettings, SailfishURIRule.REQUIRE_RESOURCE);

            if(settings == null) {
                return null;
            }

            Class<? extends IMessageFactory> clazz = settings.getFactoryClass();

            if (clazz == null) {
                return null;
            }

            IMessageFactory iMessageFactory = clazz.newInstance();
            SailfishURI dictUri = settings.getURI();
            iMessageFactory.init(getDictionary(dictUri).getNamespace(), dictUri);

            return iMessageFactory;
        } catch (Exception e) {
            logger.warn("Can not create message factory for SailfishURI: " + uri, e);
        }
        return null;
    }

	private synchronized void createDictionary(String filename, DictionarySettings settings, boolean overwrite) {

	    Objects.requireNonNull(filename, "Dictionary file is null");
	    Objects.requireNonNull(settings, "Dictionary settings is null");
	    Objects.requireNonNull(settings.getURI(), "Dictionary Sailfish URI is null");

        SailfishURI uri = settings.getURI();

        if (DictionaryManager.this.getDictionaryURIs().contains(uri)) {
            if (!overwrite) {
                throw new EPSCommonException(String.format("Concurrent dictionary registration with SailfishURI: '%s' and name: '%s' already exists", uri, filename));
            }
            invalidateDictionaries(uri);
            return;
        }


		Dictionary dictionary = new Dictionary();

		dictionary.setTitle(uri.getResourceName());
		dictionary.setResource(filename);

        for (SailfishURI suri : settings.getUtilityClassURIs()) {
            dictionary.getUtilityURI().add(suri.toString());
		}

		Dictionaries dictionaries = null;

		if (this.workspaceDispatcher.exists(FolderType.CFG, PluginLoader.CUSTOM_DICTIONARIES_XML)) {

			try {
				File customDictionariesXml = this.workspaceDispatcher.getFile(FolderType.CFG, PluginLoader.CUSTOM_DICTIONARIES_XML);

				JAXBContext jc = JAXBContext.newInstance(Dictionaries.class);
				Unmarshaller u = jc.createUnmarshaller();

				dictionaries = (Dictionaries) u.unmarshal(customDictionariesXml);

			} catch (Exception e) {
				throw new RuntimeException("Can't read " + PluginLoader.CUSTOM_DICTIONARIES_XML, e);
			}

		} else {
			dictionaries = new Dictionaries();
		}

		dictionaries.getDictionary().add(dictionary);

		try {
			File customDictionariesXml = this.workspaceDispatcher.createFile(FolderType.CFG, true, PluginLoader.CUSTOM_DICTIONARIES_XML);

			JAXBContext jc = JAXBContext.newInstance(Dictionaries.class);
			Marshaller m = jc.createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

			m.marshal(dictionaries, customDictionariesXml);

		} catch (Exception e) {
			throw new RuntimeException("Can't write to " + PluginLoader.CUSTOM_DICTIONARIES_XML, e);
		}

		dictSettings.put(uri, settings);
		location.put(uri, "cfg" + File.separator + "dictionaries" + File.separator + filename);
        pluginDictTitles.put(ObjectUtils.defaultIfNull(uri.getPluginAlias(), IVersion.GENERAL), uri);
		createEvent(uri);
	}

	/**
	 *
	 * Load dictionary from xml/xsd file
	 * @param pathName file path (relative to {ROOT})
	 * @return
	 */
	@Override
	public IDictionaryStructure createMessageDictionary(String pathName) throws RuntimeException {
	    try {
	        IDictionaryStructureLoader loader = createStructureLoader(pathName);

    	    File targetFile = this.workspaceDispatcher.getFile(FolderType.ROOT, pathName);

        	try (InputStream in = new BufferedInputStream(new FileInputStream(targetFile))) {
        		return loader.load(in);
        	}

	    } catch (Exception e) {
            throw new ScriptRunException("Could not create dictionary [" + pathName + "]", e);
        }
	}


	@Override
	public IMessageFactory createMessageFactory() {
		return DefaultMessageFactory.getFactory();
	}

	@Override
	public UtilityInfo getUtilityInfo(SailfishURI dictionaryURI, SailfishURI utilityURI, Class<?>... argTypes) throws SailfishURIException {
	    if(utilityURI.isAbsolute()) {
	        return utilityManager.getUtilityInfo(utilityURI, argTypes);
	    }

	    DictionarySettings settings = SailfishURIUtils.getMatchingValue(dictionaryURI, dictSettings, SailfishURIRule.REQUIRE_RESOURCE);

	    if(settings == null) {
	        return null;
	    }

	    for(SailfishURI utilityClassURI : settings.getUtilityClassURIs()) {
	        UtilityInfo utilityInfo = utilityManager.getUtilityInfo(utilityURI.merge(utilityClassURI), argTypes);

            if(utilityInfo != null) {
                return utilityInfo;
            }
	    }

	    return null;
	}


    @Override
    public Set<SailfishURI> getUtilityURIs(SailfishURI dictionaryURI) {
        DictionarySettings settings = SailfishURIUtils.getMatchingValue(dictionaryURI, dictSettings, SailfishURIRule.REQUIRE_RESOURCE);

        if (settings == null) {
            return Collections.emptySet();
        }

        Set<SailfishURI> result = new HashSet<>();

        for (SailfishURI utilityClassURI : settings.getUtilityClassURIs()) {
            Set<UtilityInfo> infos = utilityManager.getUtilityInfos(utilityClassURI);
            for (UtilityInfo info : infos) {
                result.add(info.getURI());
            }
        }

        return result;
    }

	@Override
	public long getDictionaryId(SailfishURI uri) {
	    return SailfishURIUtils.getMatchingValue(uri, dictionaryIds, SailfishURIRule.REQUIRE_RESOURCE);
	}


	@Override
	public void subscribeForEvents(IDictionaryManagerListener listener){
		this.eventListeners.add(listener);
	}

	@Override
	public void unSubscribeForEvents(IDictionaryManagerListener listener){
		this.eventListeners.remove(listener);
	}

    public void invalidateEvent(Set<SailfishURI> uris) {
        for (IDictionaryManagerListener listener : eventListeners) {
            try {          
                listener.invalidateEvent(uris);
            } catch (RuntimeException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }


    public void createEvent(SailfishURI uri) {
        for (IDictionaryManagerListener listener : eventListeners){
            listener.createEvent(uri);
        }
    }


	@Override
	public IDictionaryRegistrator registerDictionary(final String title, final boolean overwrite) throws WorkspaceStructureException, WorkspaceSecurityException {

	    final SailfishURI suri;
	    try {
            suri = new SailfishURI(IVersion.GENERAL, null, title);
        } catch (SailfishURIException e) {
            throw new EPSCommonException(String.format("Name '%s' is incorrect", title));
        }
	    synchronized (this) {
	        if (!overwrite && this.getDictionaryURIs().contains(suri)) {
	            throw new EPSCommonException(String.format("Dictionary with title %s and suri %s already registred", title, suri));
	        }
        }

	    Path relativePath = Paths.get("cfg", "dictionaries", title + ".xml");
        this.workspaceDispatcher.createFile(FolderType.ROOT, overwrite, relativePath.toString());
        return new DictionaryRegistrator(suri, relativePath, overwrite);
	}

	private IDictionaryStructureLoader createStructureLoader(String pathName) throws FileNotFoundException, WorkspaceSecurityException {
        String extension = FilenameUtils.getExtension(pathName);

        if ("xml".equals(extension.toLowerCase())) {
            return new XmlDictionaryStructureLoader();
        } else if ("xsd".equals(extension.toLowerCase())) {
            return new XsdDictionaryStructureLoader();
        }

        throw new EPSCommonException("Unresolved dictionary extension: '" + extension + "'");
	}

    private class DictionaryRegistrator implements IDictionaryRegistrator {

	    private final DictionarySettings dictionarySettings;
	    private final boolean overwrite;
	    private final Path relativePath;

        public DictionaryRegistrator(SailfishURI dictionarySURI, Path relativePath, boolean overwrite) {
            this.relativePath = relativePath;
            this.overwrite = overwrite;
            this.dictionarySettings = new DictionarySettings();
            this.dictionarySettings.setURI(dictionarySURI);
            this.dictionarySettings.setFactoryClass(DummyMessageFactory.class);
        }

        @Override
        public SailfishURI registrate() {

            try {
                DictionaryManager.this.createDictionary(this.relativePath.getFileName().toString(), this.dictionarySettings, this.overwrite);
            } catch (Exception e) {
                throw new EPSCommonException(String.format("Could not create dictionary with SailfishURI: '%s'", this.dictionarySettings.getURI()), e);
            }

            return this.dictionarySettings.getURI();
        }

        @Override
        public String getPath() {
            return this.relativePath.toString();
        }

        @Override
        public IDictionaryRegistrator addUtilityClassURI(SailfishURI uri) {
            this.dictionarySettings.addUtilityClassURI(uri);
            return this;
        }

        @Override
        public IDictionaryRegistrator addUtilityClassURI(Collection<SailfishURI> uri) {
            for (SailfishURI sailfishURI : uri) {
                addUtilityClassURI(sailfishURI);
            }
            return this;
        }

        @Override
        public IDictionaryRegistrator setFactoryClass(Class<? extends IMessageFactory> factoryClass) {
            this.dictionarySettings.setFactoryClass(factoryClass);
            return this;
        }
	}
}