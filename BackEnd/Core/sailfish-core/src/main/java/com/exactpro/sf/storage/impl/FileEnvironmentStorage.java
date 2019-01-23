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

import static com.exactpro.sf.common.services.ServiceName.DEFAULT_ENVIRONMENT;
import static com.exactpro.sf.storage.impl.JSONSerializer.of;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.io.FilenameUtils.concat;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;

import com.exactpro.sf.common.util.StringUtil;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.storage.FileBackedList;
import com.exactpro.sf.storage.IEnvironmentStorage;
import com.exactpro.sf.storage.StorageException;

public class FileEnvironmentStorage implements IEnvironmentStorage {
    private static final String ENVIRONMENTS_DIR = "environments";

    private final List<FileEnvironment> environments;

    public FileEnvironmentStorage(String path, IWorkspaceDispatcher dispatcher) {
        Objects.requireNonNull(path, "path cannot be null");
        Objects.requireNonNull(dispatcher, "dispatcher cannot be null");

        String directory = concat(path, ENVIRONMENTS_DIR);
        this.environments = new FileBackedList<>(directory, of(FileEnvironment.class), dispatcher);

        if(!environments.isEmpty()) {
            try {
                environments.get(0);
            } catch(Exception e) {
                int index = 0;

                for(String environmentName : new FileBackedList<>(directory, of(String.class), dispatcher)) {
                    environments.set(index++, new FileEnvironment().setName(environmentName));
                }
            }
        }

        if(!exists(DEFAULT_ENVIRONMENT)) {
            add(DEFAULT_ENVIRONMENT);
        }
    }

    @Override
    public void add(String name) {
        requireNonExisting(name);
        environments.add(new FileEnvironment().setName(name));
    }

    @Override
    public void remove(String name) {
        requireNonDefault(name, "Cannot remove default environment");
        requireExisting(name);
        environments.removeIf(environment -> environment.name.equalsIgnoreCase(name));
    }

    @Override
    public boolean exists(String name) {
        if(StringUtils.isBlank(name)) {
            throw new StorageException("Environment name cannot be blank");
        }

        StringUtil.validateFileName(name);

        return environments.stream().anyMatch(environment -> environment.name.equalsIgnoreCase(name));
    }

    @Override
    public void rename(String oldName, String newName) {
        requireNonDefault(oldName, "Cannot rename default environment to: " + newName);
        requireNonDefault(newName, "Cannot rename to default environment: " + oldName);
        requireExisting(oldName);
        requireNonExisting(newName);
        updateEnvironment(oldName, environment -> environment.setName(newName));
    }

    @Override
    public void setVariableSet(String name, String variableSet) {
        requireExisting(name);
        updateEnvironment(name, environment -> environment.setVariableSet(variableSet));
    }

    @Override
    public String getVariableSet(String name) {
        requireExisting(name);
        return environments.stream()
                .filter(environment -> environment.name.equalsIgnoreCase(name))
                .findFirst()
                .get()
                .variableSet;
    }

    @Override
    public List<String> list() {
        return environments.stream().map(FileEnvironment::getName).collect(toList());
    }

    public static class FileEnvironment {
        private String name;
        private String variableSet;

        public String getName() {
            return name;
        }

        public FileEnvironment setName(String name) {
            this.name = name;
            return this;
        }

        public String getVariableSet() {
            return variableSet;
        }

        public FileEnvironment setVariableSet(String variableSet) {
            this.variableSet = variableSet;
            return this;
        }
    }

    private String requireNonExisting(String name) {
        if(exists(name)) {
            throw new StorageException("Environment already exists: " + name);
        }

        return name;
    }

    private String requireExisting(String name) {
        if(!exists(name)) {
            throw new StorageException("Environment does not exist:  " + name);
        }

        return name;
    }

    private String requireNonDefault(String name, String message) {
        if(DEFAULT_ENVIRONMENT.equalsIgnoreCase(name)) {
            throw new StorageException(message);
        }

        return name;
    }

    private void updateEnvironment(String name, Consumer<FileEnvironment> consumer) {
        environments.replaceAll(environment -> {
            if(environment.name.equalsIgnoreCase(name)) {
                consumer.accept(environment);
            }

            return environment;
        });
    }
}
