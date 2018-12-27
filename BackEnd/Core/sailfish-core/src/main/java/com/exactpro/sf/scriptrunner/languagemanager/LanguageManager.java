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
package com.exactpro.sf.scriptrunner.languagemanager;

import java.util.Collections;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeMap;

import com.exactpro.sf.center.IVersion;
import com.exactpro.sf.configuration.ILoadableManager;
import com.exactpro.sf.configuration.ILoadableManagerContext;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.suri.SailfishURIException;
import com.exactpro.sf.configuration.suri.SailfishURIRule;
import com.exactpro.sf.configuration.suri.SailfishURIUtils;
import com.exactpro.sf.scriptrunner.languagemanager.exceptions.LanguageManagerException;

public class LanguageManager implements ILoadableManager {

    public static final ILanguageFactory AUTO = new AutoLanguageFactory();

    @SuppressWarnings("serial")
    private final Map<SailfishURI, ILanguageFactory> uriToFactory = new TreeMap<SailfishURI, ILanguageFactory>() {{ put(AutoLanguageFactory.URI, AUTO); }};

    public LanguageManager() {}

    @Override
    public void load(ILoadableManagerContext context) {
        ClassLoader classLoader = context.getClassLoaders()[0];
        IVersion version = context.getVersion();
        
        try {
            for (ILanguageFactory languageFactory : ServiceLoader.load(ILanguageFactory.class, classLoader)) {
                SailfishURI languageURI = new SailfishURI(version.getAlias(), null, languageFactory.getName());
                uriToFactory.put(languageURI, languageFactory);
            }
        } catch(Exception e) {
            throw new LanguageManagerException("Failed to load language factories", e);
        }
    }

    @Override
    public void finalize(ILoadableManagerContext context) throws Exception {
        // TODO Auto-generated method stub
    }

    public ILanguageFactory getLanguageFactory(SailfishURI languageURI) {
        return SailfishURIUtils.getMatchingValue(languageURI, uriToFactory, SailfishURIRule.REQUIRE_RESOURCE);
    }

    public Set<ILanguageFactory> getLanguageFactoriesByPlugin(IVersion version) throws SailfishURIException {
        if(version == null) {
            return null;
        }

        SailfishURI pluginURI = new SailfishURI(version.getAlias());

        return SailfishURIUtils.getMatchingValues(pluginURI, uriToFactory, SailfishURIRule.REQUIRE_PLUGIN);
    }

    public Map<SailfishURI, ILanguageFactory> getLanguages() {
        return Collections.unmodifiableMap(uriToFactory);
    }

    public Set<SailfishURI> getLanguageURIs() {
        return Collections.unmodifiableSet(uriToFactory.keySet());
    }

    public boolean containsLanguage(SailfishURI languageURI) {
        return getLanguageFactory(languageURI) != null;
    }
}
