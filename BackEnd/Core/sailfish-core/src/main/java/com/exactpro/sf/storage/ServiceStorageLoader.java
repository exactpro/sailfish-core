/*******************************************************************************
 * Copyright 2009-2021 Exactpro (Exactpro Systems Limited)
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

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

import com.exactpro.sf.configuration.IDictionaryManager;
import com.exactpro.sf.configuration.ILoadableManager;
import com.exactpro.sf.configuration.ILoadableManagerContext;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.scriptrunner.EnvironmentSettings;

public class ServiceStorageLoader implements ILoadableManager {

    private final List<IAbstractServiceStorageFactory> secondaryServiceStorageFactory;

    public ServiceStorageLoader() {
        this.secondaryServiceStorageFactory = new LinkedList<>();
    }

    @Override
    public void load(ILoadableManagerContext context) throws Exception {
        for (IAbstractServiceStorageFactory factory : ServiceLoader.load(IAbstractServiceStorageFactory.class, context.getClassLoaders()[0])) {
            secondaryServiceStorageFactory.add(factory);
        }
    }

    public List<IServiceStorage> getSecondaryServiceStorages(IWorkspaceDispatcher dispatcher, EnvironmentSettings environmentSettings, IDictionaryManager dictionaryManager){
        return secondaryServiceStorageFactory
                .stream()
                .map(factory -> factory.createServiceStorage(dispatcher, environmentSettings, dictionaryManager))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public void finalize(ILoadableManagerContext context) throws Exception {}
}
