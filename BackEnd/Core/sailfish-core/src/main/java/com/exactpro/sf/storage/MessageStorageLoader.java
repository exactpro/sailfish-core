/*******************************************************************************
 * Copyright 2009-2020 Exactpro (Exactpro Systems Limited)
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
import com.exactpro.sf.storage.impl.AbstractMessageStorage;

public class MessageStorageLoader implements ILoadableManager {

    private final List<IAbstractMessageStorageFactory> secondaryMessageStorageFactory;

    public MessageStorageLoader() {
        this.secondaryMessageStorageFactory = new LinkedList<>();
    }

    @Override
    public void load(ILoadableManagerContext context) throws Exception {
        for (IAbstractMessageStorageFactory factory : ServiceLoader.load(IAbstractMessageStorageFactory.class, context.getClassLoaders()[0])) {
            secondaryMessageStorageFactory.add(factory);
        }
    }

    public List<AbstractMessageStorage> getSecondaryMessageStorages(IWorkspaceDispatcher dispatcher, EnvironmentSettings environmentSettings, IDictionaryManager dictionaryManager){
        return secondaryMessageStorageFactory
                .stream()
                .map(factory -> factory.createMessageStorage(dispatcher, environmentSettings, dictionaryManager))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    //Unused
    @Override
    public void finalize(ILoadableManagerContext context) throws Exception {}
}
