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

import com.exactpro.sf.configuration.DictionaryManager;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.scriptrunner.EnvironmentSettings;
import org.apache.commons.configuration.EnvironmentConfiguration;
import org.hibernate.SessionFactory;

public class DBStorageSettings extends BaseStorageSettings {
    private final SessionFactory sessionFactory;

    public DBStorageSettings(IWorkspaceDispatcher workspaceDispatcher, SessionFactory sessionFactory, DictionaryManager dictionaryManager, EnvironmentSettings environmentSettings
    ) {
        super(workspaceDispatcher, dictionaryManager, environmentSettings);
        this.sessionFactory = sessionFactory;
    }

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

}
