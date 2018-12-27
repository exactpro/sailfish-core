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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.io.FilenameUtils;

import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.storage.DefaultMatrix;
import com.exactpro.sf.storage.FileBackedList;
import com.exactpro.sf.storage.IMatrix;

public class FileMatrixStorage extends AbstractMatrixStorage {
    private static final String MATRICES_DIR = "matrices";

    private final List<DefaultMatrix> matrices;
    private final AtomicLong matrixId;
    private final ReadWriteLock lock = new ReentrantReadWriteLock(true);

    public FileMatrixStorage(String path, IWorkspaceDispatcher dispatcher) {
        super(dispatcher);
        Objects.requireNonNull(path, "path cannot be null");

        this.matrices = new FileBackedList<>(FilenameUtils.concat(path, MATRICES_DIR), JSONSerializer.of(DefaultMatrix.class), dispatcher);

        if(matrices.isEmpty()) {
            matrixId = new AtomicLong();
        } else {
            matrixId = new AtomicLong(matrices.get(matrices.size() - 1).getId());
        }
    }

    @Override
    public IMatrix addMatrix(InputStream stream, String name, String description, String creator, SailfishURI languageURI, String link, SailfishURI matrixProviderURI) {
        String filePath = uploadMatrixToDisk(stream, name);
        DefaultMatrix matrix = new DefaultMatrix(matrixId.incrementAndGet(), name, description, creator, languageURI, filePath, new Date(), link, matrixProviderURI);

        try {
            lock.writeLock().lock();
            matrices.add(matrix);
        } finally {
            lock.writeLock().unlock();
        }

        writeMatrixMetadata(matrix);

        notifyListeners();

        return matrix;
    }

    @Override
    public void updateMatrix(IMatrix matrix) {
        try {
            lock.writeLock().lock();

            for(int i = 0; i < matrices.size(); i++) {
                DefaultMatrix defaultMatrix = matrices.get(i);

                if(defaultMatrix.getId().equals(matrix.getId())) {
                    defaultMatrix.setDescription(matrix.getDescription());
                    defaultMatrix.setName(matrix.getName());
                    defaultMatrix.setFilePath(matrix.getFilePath());
                    matrices.set(i, defaultMatrix);

                    writeMatrixMetadata(defaultMatrix);
                    break;
                }
            }
        } finally {
            lock.writeLock().unlock();
        }

        notifyListeners();
    }

    @Override
    public void removeMatrix(IMatrix matrix) {
        try {
            lock.writeLock().lock();

            for(int i = 0; i < matrices.size(); i++) {
                DefaultMatrix storedMatrix = matrices.get(i);

                if(storedMatrix.getId().equals(matrix.getId())) {
                    try {
                        removeMatrixFolder(storedMatrix);
                    } finally {
                        matrices.remove(i);
                    }

                    break;
                }
            }
        } finally {
            lock.writeLock().unlock();
        }

        notifyListeners();
    }

    @Override
    public List<IMatrix> getMatrixList() {
        List<IMatrix> result = new ArrayList<>();

        try {
            lock.readLock().lock();
            result.addAll(matrices);
        } finally {
            lock.readLock().unlock();
        }

        Collections.reverse(result);

        return result;
    }

    @Override
    public IMatrix getMatrixById(long matrixId) {
        try {
            lock.readLock().lock();

            for(int i = 0; i < matrices.size(); i++) {
                DefaultMatrix matrix = matrices.get(i);

                if(matrix.getId().equals(matrixId)) {
                    return matrix;
                }
            }
        } finally {
            lock.readLock().unlock();
        }

        return null;
    }

    @Override
    protected void updateReloadedMatrix(IMatrix matrix) {
        try {
            lock.writeLock().lock();

            for(int i = 0; i < matrices.size(); i++) {
                DefaultMatrix defaultMatrix = matrices.get(i);

                if(defaultMatrix.getId().equals(matrix.getId())) {
                    defaultMatrix.setName(matrix.getName());
                    defaultMatrix.setDate(matrix.getDate());
                    matrices.set(i, defaultMatrix);

                    writeMatrixMetadata(defaultMatrix);
                    break;
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
}
