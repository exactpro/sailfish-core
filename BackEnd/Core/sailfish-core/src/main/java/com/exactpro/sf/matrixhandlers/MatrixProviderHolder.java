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
package com.exactpro.sf.matrixhandlers;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.center.IVersion;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.suri.SailfishURIException;
import com.exactpro.sf.configuration.suri.SailfishURIRule;
import com.exactpro.sf.configuration.suri.SailfishURIUtils;

public class MatrixProviderHolder {

	private static final Logger logger = LoggerFactory.getLogger(MatrixProviderHolder.class);

	private final Map<SailfishURI, IMatrixProviderFactory> uriToFactory = new HashMap<>();

    public MatrixProviderHolder() {}

    public void registerMatrixProvider(IVersion version, IMatrixProviderFactory factory) {
        try {
            SailfishURI matrixProviderURI = new SailfishURI(version.getAlias(), null, factory.getAlias());
            IMatrixProviderFactory currentFactory = this.uriToFactory.put(matrixProviderURI, factory);

            if (currentFactory != null && !currentFactory.getClass().equals(factory.getClass())) {
                logger.warn("Duplicate Matrix provider registration {}", matrixProviderURI);
            }
        } catch(Exception e) {
            throw new EPSCommonException("Failed to load matrix provider factory: " + factory.getAlias(), e);
        }
    }

    public IMatrixProviderFactory getMatrixProviderFactory(SailfishURI matrixProviderURI) {
        return SailfishURIUtils.getMatchingValue(matrixProviderURI, uriToFactory, SailfishURIRule.REQUIRE_RESOURCE);
    }

    public Set<IMatrixProviderFactory> getMatrixProviderFactoriesByPlugin(IVersion version) throws SailfishURIException {
        if(version == null) {
            return null;
        }

        SailfishURI pluginURI = new SailfishURI(version.getAlias());

        return SailfishURIUtils.getMatchingValues(pluginURI, uriToFactory, SailfishURIRule.REQUIRE_PLUGIN);
    }

    public Set<SailfishURI> getProviderURIs() {
    	return Collections.unmodifiableSet(this.uriToFactory.keySet());
    }

    public Map<String, IMatrixProvider> getMatrixProviders(String link, SailfishURI matrixProviderURI) {
        IMatrixProviderFactory factory = getMatrixProviderFactory(matrixProviderURI);
        Map<String, IMatrixProvider> result = new HashMap<>();
        if (factory == null) {
            logger.warn("Failed to find MatirxProvider for type '{}'", matrixProviderURI);
            return null;
        }
        try {
            for (String matrixLink : factory.resolveLinks(link)) {
                result.put(matrixLink, factory.getMatrixProvider(matrixLink));
            }
        }
        // for backwards compatibility
        catch (UnsupportedOperationException e) {
            for (IMatrixProvider provider : factory.getMatrixProviders(link)) {
                try {
                    Field matrixLinkField = provider.getClass().getDeclaredField("link");
                    matrixLinkField.setAccessible(true);
                    String matrixLink = (String) matrixLinkField.get(provider);

                    result.put(matrixLink, provider);
                } catch (NoSuchFieldException | IllegalAccessException exception) {
                    logger.warn("Unable to extract matrix link form provider", exception);
                }
            }
        }
        return result;
    }

    public IMatrixProvider getMatrixProvider(String link, SailfishURI matrixProviderURI) {
        IMatrixProviderFactory factory = getMatrixProviderFactory(matrixProviderURI);
        if (factory == null) {
            logger.warn("Failed to find MatirxProvider for type '{}'", matrixProviderURI);
            return null;
        }
        return factory.getMatrixProvider(link);
    }
}
