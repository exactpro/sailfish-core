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

import java.util.List;
import java.util.Objects;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.common.util.StringUtil;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.storage.FileBackedList;
import com.exactpro.sf.storage.IEnvironmentStorage;
import com.exactpro.sf.storage.StorageException;
import com.google.common.collect.ImmutableList;

public class FileEnvironmentStorage implements IEnvironmentStorage {
    private static final String ENVIRONMENTS_DIR = "environments";

    private final List<String> environments;

    public FileEnvironmentStorage(String path, IWorkspaceDispatcher dispatcher) {
        Objects.requireNonNull(path, "path cannot be null");
        Objects.requireNonNull(dispatcher, "dispatcher cannot be null");

        this.environments = new FileBackedList<>(FilenameUtils.concat(path, ENVIRONMENTS_DIR), JSONSerializer.of(String.class), dispatcher);
    }

    @Override
    public void add(String name) {
        if(exists(name)) {
            throw new StorageException("Environment already exists: " + name);
        }

        environments.add(name);
    }

    @Override
    public void remove(String name) {
        if(ServiceName.DEFAULT_ENVIRONMENT.equalsIgnoreCase(name)) {
            throw new StorageException("Cannot remove default environment");
        }

        if(!exists(name)) {
            throw new StorageException("Environment doesn't exist: " + name);
        }

        environments.remove(name);
    }

    @Override
    public boolean exists(String name) {
        if(StringUtils.isBlank(name)) {
            throw new StorageException("Environment name cannot be blank");
        }

        if(ServiceName.DEFAULT_ENVIRONMENT.equalsIgnoreCase(name)) {
            return true;
        }

        StringUtil.validateFileName(name);

        return environments.contains(name);
    }

    @Override
    public void rename(String oldName, String newName) {
        if(ServiceName.DEFAULT_ENVIRONMENT.equalsIgnoreCase(oldName)) {
            throw new StorageException("Cannot rename default environment to: " + newName);
        }

        if(!exists(oldName)) {
            throw new StorageException("Environment doesn't exist: " + oldName);
        }

        if(exists(newName)) {
            throw new StorageException("Environment already exists: " + newName);
        }

        environments.remove(oldName);
        environments.add(newName);
    }

    @Override
    public List<String> list() {
        return ImmutableList.copyOf(environments);
    }
}
