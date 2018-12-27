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

import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.configuration.IDictionaryManager;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.scriptrunner.IConnectionManager;
import com.exactpro.sf.services.IServiceSettings;

public class MatrixConverterContext implements IMatrixConverterContext {

    private final IWorkspaceDispatcher workspaceDispatcher;
    private final IDictionaryManager dictionaryManager;
    private final IConnectionManager connectionManager;
    
    public MatrixConverterContext(IWorkspaceDispatcher workspaceDispatcher, IDictionaryManager dictionaryManager,
            IConnectionManager connectionManager) {
        this.workspaceDispatcher = workspaceDispatcher;
        this.dictionaryManager = dictionaryManager;
        this.connectionManager = connectionManager;
    }

    @Override
    public IWorkspaceDispatcher getWorkspaceDispatcher() {
        return this.workspaceDispatcher;
    }

    @Override
    public IDictionaryManager getDictionaryManager() {
        return this.dictionaryManager;
    }

    @Override
    public boolean isServiceExists(String environment, String service) {
        ServiceName serviceName = new ServiceName(environment, service);
        return this.connectionManager.getService(serviceName) != null;
    }
    
    @Override
    public SailfishURI getDictionaryURI(String environment, String service) {
        ServiceName serviceName = new ServiceName(environment, service);
        IServiceSettings settings = connectionManager.getServiceSettings(serviceName);
        return settings.getDictionaryName();
    }
}
