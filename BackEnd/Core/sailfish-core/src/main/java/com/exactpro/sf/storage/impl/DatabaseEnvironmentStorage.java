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
package com.exactpro.sf.storage.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.storage.IEnvironmentStorage;
import com.exactpro.sf.storage.IStorage;
import com.exactpro.sf.storage.StorageException;
import com.exactpro.sf.storage.entities.StoredEnvironment;

public class DatabaseEnvironmentStorage implements IEnvironmentStorage {
    private final IStorage storage;

    public DatabaseEnvironmentStorage(IStorage storage) {
        this.storage = storage;
    }

    @Override
    public void add(String name) {
        if(StringUtils.isBlank(name)) {
            throw new StorageException("Environment name cannot be blank");
        }

        if(ServiceName.DEFAULT_ENVIRONMENT.equalsIgnoreCase(name)) {
            throw new StorageException("Default environment already exists");
        }

        StoredEnvironment environment = new StoredEnvironment();

        environment.setName(name);
        storage.add(environment);
    }

    @Override
    public void remove(String name) {
        if(StringUtils.isBlank(name)) {
            throw new StorageException("Environment name cannot be blank");
        }

        if(ServiceName.DEFAULT_ENVIRONMENT.equalsIgnoreCase(name)) {
            throw new StorageException("Cannot remove default environment");
        }

        StoredEnvironment environment = storage.getEntityByField(StoredEnvironment.class, "name", name);

        if(environment == null) {
            throw new StorageException("Environment doesn't exist: " + name);
        }

        environment.setServices(null);
        storage.delete(environment);
    }

    @Override
    public boolean exists(String name) {
        if(StringUtils.isBlank(name)) {
            throw new StorageException("Environment name cannot be blank");
        }

        if(ServiceName.DEFAULT_ENVIRONMENT.equalsIgnoreCase(name)) {
            return true;
        }

        return storage.getEntityByField(StoredEnvironment.class, "name", name) != null;
    }

    @Override
    public void rename(String oldName, String newName) {
        if(StringUtils.isBlank(oldName)) {
            throw new StorageException("Old environment name cannot be blank");
        }

        if(StringUtils.isBlank(newName)) {
            throw new StorageException("New environment name cannot be blank");
        }

        if(ServiceName.DEFAULT_ENVIRONMENT.equalsIgnoreCase(oldName)) {
            throw new StorageException("Cannot rename default environment to: " + newName);
        }

        if(ServiceName.DEFAULT_ENVIRONMENT.equalsIgnoreCase(newName)) {
            throw new StorageException("Cannot rename to default environment: " + newName);
        }

        StoredEnvironment oldEnvironment = storage.getEntityByField(StoredEnvironment.class, "name", oldName);
        StoredEnvironment newEnvironment = storage.getEntityByField(StoredEnvironment.class, "name", newName);

        if(oldEnvironment == null) {
            throw new StorageException("Environment doesn't exist: " + oldName);
        }

        if(newEnvironment != null) {
            throw new StorageException("Environment already exists: " + newName);
        }

        oldEnvironment.setName(newName);
        storage.update(oldEnvironment);
    }

    @Override
    public List<String> list() {
        List<StoredEnvironment> environments = storage.getAllEntities(StoredEnvironment.class);
        List<String> names = new ArrayList<>();

        for(StoredEnvironment environment : environments) {
            names.add(environment.getName());
        }

        return Collections.unmodifiableList(names);
    }
}
