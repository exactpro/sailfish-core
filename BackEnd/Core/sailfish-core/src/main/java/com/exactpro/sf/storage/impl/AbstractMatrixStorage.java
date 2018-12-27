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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.configuration.workspace.WorkspaceStructureException;
import com.exactpro.sf.matrixhandlers.IMatrixProvider;
import com.exactpro.sf.storage.IMatrix;
import com.exactpro.sf.storage.IMatrixStorage;
import com.exactpro.sf.storage.MatrixUpdateListener;
import com.exactpro.sf.storage.StorageException;

public abstract class AbstractMatrixStorage implements IMatrixStorage {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final IWorkspaceDispatcher dispatcher;
    protected final List<MatrixUpdateListener> listeners;

    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public AbstractMatrixStorage(IWorkspaceDispatcher dispatcher) {
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher cannot be null");
        this.listeners = new CopyOnWriteArrayList<>();
    }

    protected void notifyListeners() {
        for(MatrixUpdateListener listener : listeners) {
            listener.onEvent();
        }
    }

    @Override
    public void subscribeForUpdates(MatrixUpdateListener listener) {
        listeners.add(listener);
    }

    @Override
    public void unSubscribeForUpdates(MatrixUpdateListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void reloadMatrix(IMatrix matrix, IMatrixProvider matrixProvider) {
        Objects.requireNonNull(matrixProvider, "matrixProvider cannot be null");

        try(InputStream input = matrixProvider.getMatrix()) {
            File matrixFile = dispatcher.getFile(FolderType.MATRIX, matrix.getFilePath());

            try(OutputStream output = new BufferedOutputStream(new FileOutputStream(matrixFile))) {
                IOUtils.copy(input, output);
            }

            matrix.setName(matrixProvider.getName());
            matrix.setDate(new Date());
        } catch(Exception e) {
            throw new StorageException("Failed to reload matrix: " + matrix.getName(), e);
        }

        updateReloadedMatrix(matrix);
        notifyListeners();
    }

    protected abstract void updateReloadedMatrix(IMatrix matrix);

    protected String uploadMatrixToDisk(InputStream stream, String name) {
        Objects.requireNonNull(name, "name cannot be null");

        String folderName = UUID.randomUUID().toString();
        File result;

        try {
            result = dispatcher.createFile(FolderType.MATRIX, true, folderName, FilenameUtils.getName(name));
        } catch(WorkspaceStructureException e) {
            throw new StorageException("Failed to create matrix file: " + name, e);
        }

        try(BufferedOutputStream writer = new BufferedOutputStream(new FileOutputStream(result))) {
            if(stream != null) {
                IOUtils.copy(stream, writer);
            }
        } catch(IOException e) {
            throw new StorageException("Failed to write to matrix file: " + name, e);
        }

        return folderName + File.separator + name;
    }

    protected void removeMatrixFolder(IMatrix matrix) {
        try {
            File matricesFolder = dispatcher.getFolder(FolderType.MATRIX);
            File matrixFolder = new File(matricesFolder, FilenameUtils.getPath(matrix.getFilePath()));
            FileUtils.deleteDirectory(matrixFolder);
        } catch(IOException e) {
            throw new StorageException("Failed to remove matrix from disk: " + matrix.getFilePath());
        }
    }

    protected void writeMatrixMetadata(IMatrix storedMatrix) {
        try {
            File matrixFolder = dispatcher.getFile(FolderType.MATRIX, storedMatrix.getFilePath()).getParentFile();
            String matrixDir = matrixFolder.getName();
            File matrixMetadata = dispatcher.createFile(FolderType.MATRIX, true, matrixDir, "matrix-metadata.json");
            OBJECT_MAPPER.writeValue(matrixMetadata, storedMatrix);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }
}
