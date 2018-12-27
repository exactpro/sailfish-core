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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.center.IVersion;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.data.Data;
import com.exactpro.sf.configuration.data.DataListing;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.suri.SailfishURIException;
import com.exactpro.sf.configuration.suri.SailfishURIRule;
import com.exactpro.sf.configuration.suri.SailfishURIUtils;
import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.scriptrunner.ScriptRunException;

public class DataManager implements IDataManager, ILoadableManager {

	private static final Logger logger = LoggerFactory.getLogger(DataManager.class);

	private final IWorkspaceDispatcher workspaceDispatcher;
	private final Map<SailfishURI, String> location = new HashMap<>();
	private final Map<String, Path> pluginsMapping = new HashMap<>();
	private final Map<String, ClassLoader> pluginsClassloaders = new HashMap<>();

	public DataManager(IWorkspaceDispatcher workspaceDispatcher) {
        this.workspaceDispatcher = Objects.requireNonNull(workspaceDispatcher, "workspaceDispatcher cannot be null");
	}


	@Override
	public boolean exists(SailfishURI uri) {
	    return SailfishURIUtils.getMatchingValue(uri, location, SailfishURIRule.REQUIRE_RESOURCE) != null;
	}

	/**
	 * Create InputStream for known data
	 * @param uri of data
	 * @return InputStream
	 */
	@Override
	public InputStream getDataInputStream(SailfishURI uri) {
		String resource = SailfishURIUtils.getMatchingValue(uri, this.location, SailfishURIRule.REQUIRE_RESOURCE);

		if (resource == null) {
			throw new RuntimeException("No data found for URI: " + uri);
		}

		try {
            return new FileInputStream(this.workspaceDispatcher.getFile(FolderType.ROOT, resource));
        } catch (FileNotFoundException e) {
            throw new ScriptRunException("Could not create data input stream [" + resource + "]", e);
        }
	}

    /**
     * Loads specified resource from plugin
     * @param pluginAlias plugin alias
     * @param resourcePath path to resource
     * @return {@link InputStream} for resource
     */
    @Override
    public InputStream getDataInputStream(String pluginAlias, String resourcePath) {
        ClassLoader pluginClassLoader = pluginsClassloaders.get(pluginAlias);
        if (pluginClassLoader != null) {
            InputStream resource = pluginClassLoader.getResourceAsStream(resourcePath);
            if (resource != null) {
                return resource;
            }
            throw new EPSCommonException(String.format("Can't load resource %s for plugin %s", resourcePath, pluginAlias));
        }
        throw new EPSCommonException("Can't find classloader for plugin " + pluginAlias);
    }

    /**
	 * Create OutputStream for known data
	 * @param uri of data
	 * @return OutputStream
	 */
	@Override
	public OutputStream getDataOutputStream(SailfishURI uri, boolean append) {
	    String resource = SailfishURIUtils.getMatchingValue(uri, this.location, SailfishURIRule.REQUIRE_RESOURCE);

        if (resource == null) {
            throw new RuntimeException("No data found for URI: " + uri);
        }

        try {
            return new FileOutputStream(this.workspaceDispatcher.getOrCreateFile(FolderType.ROOT, resource), append);
        } catch (IOException e) {
            throw new ScriptRunException("Could not create data output stream [" + resource + "]", e);
        }
	}

	private void loadDataLocations(final InputStream xml, final String pathToDataFolder, IVersion version) {
		DataListing dataListing = null;

		try {
			JAXBContext jc = JAXBContext.newInstance(DataListing.class);
			Unmarshaller u = jc.createUnmarshaller();

			JAXBElement<DataListing> root = u.unmarshal(new StreamSource(xml), DataListing.class);
			dataListing = root.getValue();

			for (Data data : dataListing.getData()) {
			    SailfishURI dataURI = new SailfishURI(version.getAlias(), null, data.getTitle());

			    if (location.put(dataURI, pathToDataFolder + File.separator + data.getResource()) != null) {
					logger.warn("Duplicate Data URI {}", dataURI);
				}
			}
		} catch (JAXBException | SailfishURIException e) {
			throw new EPSCommonException("Failed to load config", e);
		}
	}


    /**
     * @param pluginAlias
     * @param relativepluginFolder
     */
    private void registerPluginPath(String pluginAlias, Path relativepluginFolder) {
        if (pluginsMapping.put(pluginAlias, relativepluginFolder) != null) {
		    logger.warn("Duplicate plugin alias {}", pluginAlias);
		}
    }

	@Override
    public void load(ILoadableManagerContext context) {
        loadDataLocations(context.getResourceStream(), context.getResourceFolder(), context.getVersion());
    }

    private void storeClassloaderForPlugin(String pluginAlias, ClassLoader pluginLoader) {
        if (pluginsClassloaders.put(pluginAlias, pluginLoader) != null) {
            logger.warn("Duplicated plugin {}", pluginAlias);
        }
        logger.info("Classloader for {} has been stored", pluginAlias);
    }


    @Override
    public void finalize(ILoadableManagerContext context) throws Exception {
        Objects.requireNonNull(context, "Context can't be null");
        IVersion versionClass = context.getVersion();
        registerPluginPath(versionClass.getAlias(), Paths.get(context.getResourceFolder()));
        storeClassloaderForPlugin(versionClass.getAlias(), versionClass.getClass().getClassLoader());
    }

    @Override
    public Path getRelativePathToPlugin(String pluginAlias) {
        return pluginsMapping.get(pluginAlias);
	}

    @Override
    public String getExtension(SailfishURI uri) {
        String resource = SailfishURIUtils.getMatchingValue(uri, this.location, SailfishURIRule.REQUIRE_RESOURCE);

        if(resource == null) {
            throw new RuntimeException("No data found for URI: " + uri);
        }

        return FilenameUtils.getExtension(resource);
    }
}
