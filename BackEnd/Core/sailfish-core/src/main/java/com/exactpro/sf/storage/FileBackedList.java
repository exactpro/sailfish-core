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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.AbstractList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.function.Predicate;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.configuration.workspace.WorkspaceSecurityException;

public class FileBackedList<E> extends AbstractList<E> implements RandomAccess {
    protected final File path;
    protected final ISerializer<E> serializer;

    protected int size = 0;

    public FileBackedList(File path, ISerializer<E> serializer) {
        this.path = Objects.requireNonNull(path, "path cannot be null");
        this.serializer = Objects.requireNonNull(serializer, "serializer cannot be null");

        initPath(path);
    }

    public FileBackedList(String path, ISerializer<E> serializer, IWorkspaceDispatcher dispatcher) {
        Objects.requireNonNull(path, "path cannot be null");
        Objects.requireNonNull(dispatcher, "dispatcher cannot be null");
        this.serializer = Objects.requireNonNull(serializer, "serializer cannot be null");

        try {
            if(dispatcher.exists(FolderType.ROOT, path)) {
                File existingDir = dispatcher.getFile(FolderType.ROOT, path);

                if(!existingDir.isDirectory()) {
                    throw new EPSCommonException("Path is not a directory: " + path);
                }

                File topDir = dispatcher.createFolder(FolderType.ROOT, path);

                if(!Files.isSameFile(existingDir.toPath(), topDir.toPath())) {
                    FileUtils.copyDirectory(existingDir, topDir, true);
                }

                this.path = topDir;
            } else {
                this.path = dispatcher.createFolder(FolderType.ROOT, path);
                return;
            }

            initPath(this.path);
        } catch(WorkspaceSecurityException | IOException e) {
            throw new EPSCommonException("Failed to initialize path: " + path, e);
        }
    }

    private void initPath(File path) {
        if(!path.isDirectory()) {
            throw new EPSCommonException("Path is not a directory: " + path);
        }

        if(!path.exists() && !path.mkdirs()) {
            throw new EPSCommonException("Failed to create a directory: " + path);
        }

        File[] files = path.listFiles();
        BitSet indices = new BitSet(size = files.length);

        for(File file : files) {
            if(!file.isFile()) {
                throw new EPSCommonException("Directory can contain only files: " + path);
            }

            String name = file.getName();

            if(!StringUtils.isNumeric(name)) {
                throw new EPSCommonException("File names should be only numeric in: " + path);
            }

            indices.set(Integer.valueOf(name));
        }

        if(indices.nextClearBit(0) != size) {
            throw new EPSCommonException("Inconsistent indices in: " + path);
        }
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public E get(int index) {
        if(index < 0 || index >= size()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size());
        }

        return loadElement(index);
    }

    @Override
    public E set(int index, E element) {
        E value = get(index);
        saveElement(index, element);
        return value;
    }

    @Override
    public void add(int index, E element) {
        if(index < 0 || index > size()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size());
        }

        moveElements(index, 1);
        saveElement(index, element);
    }

    // for performance improvement
    @Override
    public boolean addAll(Collection<? extends E> c) {
        return addAll(size(), c);
    }

    // for performance improvement
    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        if(index < 0 || index > size()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size());
        }

        if(c.isEmpty()) {
            return false;
        }

        moveElements(index, c.size());

        for(E e : c) {
            saveElement(index++, e);
        }

        return true;
    }

    @Override
    public E remove(int index) {
        E value = get(index);
        removeElement(index);
        moveElements(++index, -1);
        return value;
    }

    // for performance improvement
    @Override
    public boolean removeAll(Collection<?> c) {
        return removeIf(element -> c.contains(element));
    }

    // for performance improvement
    @Override
    public boolean retainAll(Collection<?> c) {
        return removeIf(element -> !c.contains(element));
    }

    // for performance improvement
    @Override
    protected void removeRange(int fromIndex, int toIndex) {
        for(int i = fromIndex; i < toIndex; i++) {
            removeElement(i);
        }

        moveElements(toIndex, fromIndex - toIndex);
    }

    @Override
    public boolean removeIf(Predicate<? super E> filter) {
        int shift = 0;

        for(int i = 0; i < size(); i++) {
            if(filter.test(get(i))) {
                shift--;
                removeElement(i);
                continue;
            }

            if(shift != 0) {
                moveElement(i, i + shift);
            }
        }

        changeSize(shift);

        return shift != 0;
    }

    protected void removeElement(int index) {
        if(!new File(path, Integer.toString(index)).delete()) {
            throw new EPSCommonException("Failed to remove element: " + index);
        }
    }

    protected void moveElements(int fromIndex, int shift) {
        if(shift < 0) {
            for(int i = fromIndex; i < size(); i++) {
                moveElement(i, i + shift);
            }
        } else {
            for(int i = size() - 1; i >= fromIndex; i--) {
                moveElement(i, i + shift);
            }
        }

        changeSize(shift);
    }

    private void moveElement(int sourceIndex, int targetIndex) {
        try {
            Path source = new File(path, Integer.toString(sourceIndex)).toPath();
            Path target = new File(path, Integer.toString(targetIndex)).toPath();
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch(WorkspaceSecurityException | IOException e) {
            throw new EPSCommonException("Failed to move element from: " + sourceIndex + ", to: " + targetIndex, e);
        }
    }

    private E loadElement(int index) {
        try {
            File file = new File(path, Integer.toString(index));
            return serializer.deserialize(file);
        } catch(Exception e) {
            throw new EPSCommonException("Failed to load element: " + index, e);
        }
    }

    private void saveElement(int index, E object) {
        try {
            File file = new File(path, Integer.toString(index));
            serializer.serialize(object, file);
        } catch(Exception e) {
            throw new EPSCommonException("Failed to save element: " + index, e);
        }
    }

    protected void changeSize(int diff) {
        modCount += Math.abs(diff);
        size += diff;
    }
}
