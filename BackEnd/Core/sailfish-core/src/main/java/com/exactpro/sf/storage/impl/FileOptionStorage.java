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

import static com.exactpro.sf.storage.impl.JSONSerializer.of;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.io.FilenameUtils.concat;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.configuration.workspace.WorkspaceSecurityException;
import com.exactpro.sf.storage.IOptionsStorage;
import com.exactpro.sf.storage.ISerializer;
import com.exactpro.sf.storage.StorageException;
import com.fasterxml.jackson.core.type.TypeReference;

public class FileOptionStorage implements IOptionsStorage {
    private static final ISerializer<Map<String, String>> SERIALIZER = of(new TypeReference<Map<String, String>>() {});
    private static final String OPTIONS_FILE_NAME = "options.json";

    private final String path;
    private final IWorkspaceDispatcher dispatcher;
    private final Map<String, String> options;

    public FileOptionStorage(String path, IWorkspaceDispatcher dispatcher) {
        this.path = requireNonNull(path, "path cannot be null");
        this.dispatcher = requireNonNull(dispatcher, "dispatcher cannot be null");

        try {
            if(dispatcher.exists(FolderType.ROOT, path, OPTIONS_FILE_NAME)) {
                File file = dispatcher.getFile(FolderType.ROOT, path, OPTIONS_FILE_NAME);

                try {
                    this.options = SERIALIZER.deserialize(file);
                } catch(Exception e) {
                    throw new StorageException("Failed to deserialize options file: " + file.getCanonicalPath());
                }
            } else {
                this.options = new HashMap<>();
            }
        } catch(WorkspaceSecurityException | IOException e) {
            throw new StorageException("Failed to initialize file: " + concat(path, OPTIONS_FILE_NAME), e);
        }
    }

    @Override
    public void setOption(String key, String value) throws StorageException {
        options.put(key, value);

        try {
            File file = dispatcher.createFile(FolderType.ROOT, true, path, OPTIONS_FILE_NAME);
            SERIALIZER.serialize(options, file);
        } catch(Exception e) {
            throw new StorageException("Failed to set option '" + key + "' to: " + value, e);
        }
    }

    @Override
    public String getOption(String key) throws StorageException {
        return options.get(key);
    }

    @Override
    public Map<String, String> getAllOptions() throws StorageException {
        return Collections.unmodifiableMap(options);
    }
}
