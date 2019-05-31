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

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.IDictionaryManager;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.suri.SailfishURIRule;
import com.exactpro.sf.configuration.suri.SailfishURIUtils;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.scriptrunner.IConnectionManager;

public class MatrixConverterManager {
    private final IWorkspaceDispatcher workspaceDispatcher;
    private final IDictionaryManager dictionaryManager;
    private final IConnectionManager connectionManager;

    private final Map<SailfishURI, Class<? extends IMatrixConverter>> uriToClass;
    private final Map<SailfishURI, Class<? extends IMatrixConverterSettings>> uriToSettingsClass;

	protected MatrixConverterManager(IWorkspaceDispatcher workspaceDispatcher, IDictionaryManager dictionaryManager,
            IConnectionManager connectionManager, Map<SailfishURI, Class<? extends IMatrixConverter>> uriToClass,
            Map<SailfishURI, Class<? extends IMatrixConverterSettings>> uriToSettingsClass) {
        this.workspaceDispatcher = workspaceDispatcher;
        this.dictionaryManager = dictionaryManager;
        this.connectionManager = connectionManager;
        this.uriToClass = Collections.unmodifiableMap(uriToClass);
        this.uriToSettingsClass = Collections.unmodifiableMap(uriToSettingsClass);
    }

    public IMatrixConverter getMatrixConverter(SailfishURI converterURI) {
        try {
            SailfishURI uri = SailfishURIUtils.getMatchingURI(converterURI, uriToClass.keySet(), SailfishURIRule.REQUIRE_RESOURCE);
            Class<? extends IMatrixConverter> clazz = uriToClass.get(uri);
            IMatrixConverter converter = clazz.newInstance();
            IMatrixConverterContext context = new MatrixConverterContext(uri, workspaceDispatcher, dictionaryManager, connectionManager);
            converter.init(context);
            return converter;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new EPSCommonException("Failed to initialize matrix converter" + converterURI, e);
        }
    }

    public IMatrixConverterSettings getMatrixConverterSettings(SailfishURI converterURI) {
        try {
            Class<? extends IMatrixConverterSettings> clazz = SailfishURIUtils.getMatchingValue(converterURI, uriToSettingsClass, SailfishURIRule.REQUIRE_RESOURCE);
            return clazz.newInstance();
        } catch(InstantiationException | IllegalAccessException e) {
            throw new EPSCommonException("Failed to initialize matrix converter settings for converter: " + converterURI, e);
        }
    }

	public Set<SailfishURI> getMatrixConverters() {
        return Collections.unmodifiableSet(uriToClass.keySet());
	}
}
