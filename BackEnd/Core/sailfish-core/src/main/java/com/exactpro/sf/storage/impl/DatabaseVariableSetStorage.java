/*******************************************************************************
 * Copyright 2009-2019 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.storage.impl;

import static com.google.common.collect.ImmutableList.of;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toCollection;
import static org.hibernate.criterion.Restrictions.ilike;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.storage.IStorage;
import com.exactpro.sf.storage.StorageException;
import com.exactpro.sf.storage.entities.StoredVariableSet;

public class DatabaseVariableSetStorage extends AbstractVariableSetStorage {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseVariableSetStorage.class);
    private static final String NAME = "name";

    private final IStorage storage;

    public DatabaseVariableSetStorage(IStorage storage) {
        this.storage = requireNonNull(storage, "storage cannot be null");
    }

    @Override
    public Map<String, String> get(String name) {
        LOGGER.debug("Getting variable set: {}", name);
        StoredVariableSet variableSet = getVariableSet(name);
        return variableSet != null ? variableSet.getVariables() : null;
    }

    @Override
    public void put(String name, Map<String, String> variableSet) {
        LOGGER.debug("Putting variable set '{}': {}", name, variableSet);
        StoredVariableSet storedVariableSet = getVariableSet(name);

        if(storedVariableSet == null) {
            storedVariableSet = new StoredVariableSet().setName(name);
            storage.add(storedVariableSet);
        }

        storedVariableSet.setVariables(checkVariableSet(variableSet));
        storage.update(storedVariableSet);
    }

    @Override
    public void remove(String name) {
        LOGGER.debug("Removing variable set: {}", name);
        StoredVariableSet storedVariableSet = getVariableSet(name);

        if(storedVariableSet == null) {
            throw new StorageException("Variable set does not exist: " + name);
        }

        storage.delete(storedVariableSet);
    }

    @Override
    public boolean exists(String name) {
        LOGGER.debug("Checking existence of variable set: {}", name);
        return getVariableSet(name) != null;
    }

    @Override
    public Set<String> list() {
        LOGGER.debug("Getting list of all variable sets");
        return storage.getAllEntities(StoredVariableSet.class)
                .stream()
                .map(StoredVariableSet::getName)
                .collect(toCollection(TreeSet::new));
    }

    private StoredVariableSet getVariableSet(String name) {
        List<StoredVariableSet> variableSets = storage.getAllEntities(StoredVariableSet.class, of(ilike(NAME, checkName(name))));
        return variableSets.isEmpty() ? null : variableSets.get(0);
    }
}
