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
package com.exactpro.sf.storage;

import com.exactpro.sf.configuration.IDictionaryManager;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.scriptrunner.EnvironmentSettings;

public class BaseStorageSettings {
    protected final IWorkspaceDispatcher workspaceDispatcher;
    protected final IDictionaryManager dictionaryManager;
    private final EnvironmentSettings environmentSettings;

    public BaseStorageSettings(IWorkspaceDispatcher workspaceDispatcher, IDictionaryManager dictionaryManager, EnvironmentSettings environmentSettings) {
        this.workspaceDispatcher = workspaceDispatcher;
        this.dictionaryManager = dictionaryManager;
        this.environmentSettings = environmentSettings;
    }

    public IWorkspaceDispatcher getWorkspaceDispatcher() {
        return workspaceDispatcher;
    }

    public IDictionaryManager getDictionaryManager() {
        return dictionaryManager;
    }

    public EnvironmentSettings getEnvironmentSettings() {
        return environmentSettings;
    }
}
