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
package com.exactpro.sf.aml.converter;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.center.IVersion;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.IDictionaryManager;
import com.exactpro.sf.configuration.ILoadableManager;
import com.exactpro.sf.configuration.ILoadableManagerContext;
import com.exactpro.sf.configuration.ResourceAliases;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.suri.SailfishURIException;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.scriptrunner.IConnectionManager;

public class MatrixConverterLoader implements ILoadableManager {

    private static final Logger logger = LoggerFactory.getLogger(MatrixConverterLoader.class);

    private final Map<SailfishURI, Class<? extends IMatrixConverter>> uriToClass = new HashMap<>();
    private final Map<SailfishURI, Class<? extends IMatrixConverterSettings>> uriToSettingsClass = new HashMap<>();

    @Override
    public void load(ILoadableManagerContext context) throws Exception {
        ClassLoader classLoader = context.getClassLoaders()[0];
        IVersion version = context.getVersion();
        ServiceLoader<IMatrixConverter> converters = ServiceLoader.load(IMatrixConverter.class, classLoader);

        try {
            for (IMatrixConverter converter : converters) {
                Class<? extends IMatrixConverter> converterClass = converter.getClass();
                ResourceAliases resourceAliases = converterClass.getAnnotation(ResourceAliases.class);
                SettingsClassName settingsClassName = converterClass.getAnnotation(SettingsClassName.class);

                if(resourceAliases == null) {
                    throw new EPSCommonException("No resource annotation for matrix converter class: " + converterClass.getName());
                }

                if(settingsClassName == null) {
                    throw new EPSCommonException("No settings class name annotation for matrix converter class: " + converterClass.getName());
                }

                String[] classAliases = resourceAliases.value();
                Class<? extends IMatrixConverterSettings> settingsClass = classLoader.loadClass(settingsClassName.value()).asSubclass(IMatrixConverterSettings.class);

                if(classAliases.length == 0) {
                    throw new EPSCommonException("No resource aliases for matrix converter class: " + converterClass.getName());
                }

                for (String alias : classAliases) {
                    SailfishURI converterURI = new SailfishURI(version.getAlias(), null, alias);
                    this.uriToClass.put(converterURI, converterClass);
                    this.uriToSettingsClass.put(converterURI, settingsClass);
                }

                logger.info("MatrixConverter {} has been loaded", converterClass.getCanonicalName());
            }
        } catch (ServiceConfigurationError | SailfishURIException e) {
            logger.error("Failed to load MatrixConverters", e);
        }
    }

    @Override
    public void finalize(ILoadableManagerContext context) throws Exception { }

    public MatrixConverterManager create(IWorkspaceDispatcher workspaceDispatcher, IDictionaryManager dictionaryManager, IConnectionManager connectionManager) {
        return new MatrixConverterManager(workspaceDispatcher, dictionaryManager, connectionManager, this.uriToClass, this.uriToSettingsClass);
    }

}
