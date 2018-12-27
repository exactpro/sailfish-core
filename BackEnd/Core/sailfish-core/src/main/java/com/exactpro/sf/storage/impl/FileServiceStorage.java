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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.io.filefilter.FileFilterUtils;
import java.time.Instant;

import com.exactpro.sf.common.services.ServiceInfo;
import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.common.util.StringUtil;
import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.configuration.workspace.WorkspaceSecurityException;
import com.exactpro.sf.scriptrunner.services.IStaticServiceManager;
import com.exactpro.sf.services.IServiceSettings;
import com.exactpro.sf.services.ServiceDescription;
import com.exactpro.sf.services.ServiceEvent;
import com.exactpro.sf.storage.IMessageStorage;
import com.exactpro.sf.storage.IObjectFlusher;
import com.exactpro.sf.storage.ISerializer;
import com.exactpro.sf.storage.IServiceStorage;
import com.exactpro.sf.storage.ServiceEventList;
import com.exactpro.sf.storage.SortCriterion;
import com.exactpro.sf.storage.StorageException;
import com.exactpro.sf.storage.StorageFilter;
import com.exactpro.sf.storage.StorageResult;
import com.exactpro.sf.storage.util.ServiceStorageHelper;
import com.google.common.collect.ImmutableList;

public class FileServiceStorage implements IServiceStorage {
    private static final String EVENTS_DIR = "events";
    private static final String SERVICES_DIR = "services";
    private static final int BUFFER_SIZE = 50;

    private final String path;
    private final IWorkspaceDispatcher dispatcher;
    private final IStaticServiceManager manager;
    private final IMessageStorage storage;

    private final Map<ServiceName, ServiceDescription> descriptionMap = new HashMap<>();
    private final Map<ServiceName, List<FileServiceEvent>> eventsMap = new HashMap<>();
    private final Map<ServiceName, IObjectFlusher<FileServiceEvent>> flusherMap = new HashMap<>();

    private final ReadWriteLock descriptionLock = new ReentrantReadWriteLock(true);
    private final ReadWriteLock eventsLock = new ReentrantReadWriteLock(true);
    private final ReadWriteLock flusherLock = new ReentrantReadWriteLock(true);

    private final ISerializer<ServiceDescription> serializer = new DescriptionSerializer();

    public FileServiceStorage(String path, IWorkspaceDispatcher dispatcher, IStaticServiceManager manager, IMessageStorage storage) {
        this.path = Objects.requireNonNull(path, "path cannot be null");
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher cannot be null");
        this.manager = Objects.requireNonNull(manager, "manager cannot be null");
        this.storage = Objects.requireNonNull(storage, "storage cannot be null");

        try {
            if(!dispatcher.exists(FolderType.ROOT, path, SERVICES_DIR)) {
                return;
            }

            Set<String> fileNames = dispatcher.listFiles(FileFilterUtils.fileFileFilter(), FolderType.ROOT, path, SERVICES_DIR);

            for(String fileName : fileNames) {
                File file = dispatcher.getWritableFile(FolderType.ROOT, path, SERVICES_DIR, fileName);
                ServiceDescription description = serializer.deserialize(file);
                ServiceName serviceName = new ServiceName(description.getEnvironment(), description.getName());
                List<FileServiceEvent> events = new ServiceEventList(Paths.get(path, EVENTS_DIR, serviceName.toString()).toString(), dispatcher);

                descriptionMap.put(serviceName, description);
                eventsMap.put(serviceName, events);
                flusherMap.put(serviceName, new ObjectFlusher<>(new ListFlushProvider<>(events), BUFFER_SIZE));
            }

            for(IObjectFlusher<FileServiceEvent> flusher : flusherMap.values()) {
                flusher.start();
            }
        } catch(Exception e) {
            throw new StorageException("Failed to load services", e);
        }
    }

    @Override
    public ServiceInfo lookupService(ServiceName serviceName) {
        try {
            this.descriptionLock.readLock().lock();

            if(descriptionMap.containsKey(serviceName)) {
                return new ServiceInfo(serviceName.toString(), serviceName);
            }
        } finally {
            this.descriptionLock.readLock().unlock();
        }

        return null;
    }

    @Override
    public void addServiceDescription(ServiceDescription description) {
        ServiceName serviceName = getServiceName(description);

        try {
            descriptionLock.writeLock().lock();

            if(descriptionMap.containsKey(serviceName)) {
                throw new StorageException("Service already exists: " + serviceName);
            }

            File serviceFile = dispatcher.createFile(FolderType.ROOT, false, path, SERVICES_DIR, serviceName.toString());

            descriptionMap.put(serviceName, description);
            serializer.serialize(description, serviceFile);

            try {
                eventsLock.writeLock().lock();
                flusherLock.writeLock().lock();

                List<FileServiceEvent> events = new ServiceEventList(Paths.get(path, EVENTS_DIR, serviceName.toString()).toString(), dispatcher);
                IObjectFlusher<FileServiceEvent> flusher = new ObjectFlusher<>(new ListFlushProvider<>(events), BUFFER_SIZE);

                eventsMap.put(serviceName, events);
                flusherMap.put(serviceName, flusher);

                flusher.start();
            } finally {
                flusherLock.writeLock().unlock();
                eventsLock.writeLock().unlock();
            }
        } catch(Exception e) {
            throw new StorageException("Failed to add service: " + serviceName, e);
        } finally {
            descriptionLock.writeLock().unlock();
        }
    }

    @Override
    public void removeServiceDescription(ServiceDescription description) {
        ServiceName serviceName = getServiceName(description);

        try {
            descriptionLock.writeLock().lock();
            eventsLock.writeLock().lock();
            flusherLock.writeLock().lock();

            if(descriptionMap.remove(serviceName) == null) {
                throw new StorageException("Service doesn't exist: " + serviceName);
            }

            eventsMap.remove(serviceName).clear();
            flusherMap.remove(serviceName).stop();
            // FIXME: removing messages breaks retrieve after BB run
            //storage.removeMessages(serviceName.toString());

            dispatcher.removeFolder(FolderType.ROOT, path, EVENTS_DIR, serviceName.toString());
            dispatcher.removeFile(FolderType.ROOT, path, SERVICES_DIR, serviceName.toString());
        } catch(WorkspaceSecurityException | IOException e) {
            throw new StorageException("Failed to remove serivce: " + serviceName, e);
        } finally {
            flusherLock.writeLock().unlock();
            eventsLock.writeLock().unlock();
            descriptionLock.writeLock().unlock();
        }
    }

    @Override
    public void updateServiceDescription(ServiceDescription description) {
        ServiceName serviceName = getServiceName(description);

        try {
            descriptionLock.writeLock().lock();

            if(!descriptionMap.containsKey(serviceName)) {
                throw new StorageException("Service doesn't exist: " + serviceName);
            }

            File serviceFile = dispatcher.createFile(FolderType.ROOT, true, path, SERVICES_DIR, serviceName.toString());

            descriptionMap.put(serviceName, description);
            serializer.serialize(description, serviceFile);
        } catch(Exception e) {
            throw new StorageException("Failed to update service: " + serviceName, e);
        } finally {
            descriptionLock.writeLock().unlock();
        }
    }

    @Override
    public List<ServiceDescription> getServiceDescriptions() {
        try {
            this.descriptionLock.readLock().lock();
            return ImmutableList.copyOf(descriptionMap.values());
        } finally {
            this.descriptionLock.readLock().unlock();
        }
    }

    @Override
    public void addServiceEvent(ServiceDescription description, ServiceEvent event) {
        ServiceName serviceName = getServiceName(description);

        try {
            flusherLock.readLock().lock();
            IObjectFlusher<FileServiceEvent> flusher = flusherMap.get(serviceName);

            if(flusher == null) {
                throw new StorageException("Service doesn't exist: " + serviceName);
            }

            flusher.add(new FileServiceEvent(event));
        } finally {
            flusherLock.readLock().unlock();
        }
    }

    @Override
    public long getEventsCount(ServiceDescription description, StorageFilter filter) {
        ServiceName serviceName = getServiceName(description);

        try {
            eventsLock.readLock().lock();
            flusherLock.readLock().lock();

            IObjectFlusher<FileServiceEvent> flusher = flusherMap.get(serviceName);

            if(flusher == null) {
                throw new StorageException("Service doesn't exist: " + serviceName);
            }

            flusher.flush();
            List<FileServiceEvent> events = eventsMap.get(serviceName);

            return events.size();
        } finally {
            flusherLock.readLock().unlock();
            eventsLock.readLock().unlock();
        }
    }

    @Override
    public StorageResult<ServiceEvent> getServiceEvents(ServiceDescription description, StorageFilter filter, int firstRecord, int size, List<SortCriterion> sorting) {
        ServiceName serviceName = getServiceName(description);

        try {
            eventsLock.readLock().lock();
            flusherLock.readLock().lock();

            IObjectFlusher<FileServiceEvent> flusher = flusherMap.get(serviceName);

            if(flusher == null) {
                throw new StorageException("Service doesn't exist: " + serviceName);
            }

            flusher.flush();

            List<FileServiceEvent> events = eventsMap.get(serviceName);
            events = events.subList(firstRecord, Math.min(firstRecord + size, events.size()));

            return new StorageResult<>(ImmutableList.copyOf(events), events.size());
        } finally {
            flusherLock.readLock().unlock();
            eventsLock.readLock().unlock();
        }
    }

    @Override
    public void removeServiceEvents(ServiceDescription description) {
        ServiceName serviceName = getServiceName(description);

        try {
            eventsLock.writeLock().lock();
            flusherLock.readLock().lock();

            List<FileServiceEvent> events = eventsMap.get(serviceName);

            if(events == null) {
                throw new StorageException("Service doesn't exist: " + serviceName);
            }

            flusherMap.get(serviceName).flush();
            events.clear();
        } finally {
            flusherLock.readLock().unlock();
            eventsLock.writeLock().unlock();
        }
    }

    @Override
    public void removeServiceEvents(Instant olderThan) {
        try {
            eventsLock.writeLock().lock();
            flusherLock.readLock().lock();

            long epochMillis = olderThan.toEpochMilli();

            for(Entry<ServiceName, List<FileServiceEvent>> e : eventsMap.entrySet()) {
                flusherMap.get(e.getKey()).flush();

                if(olderThan == null) {
                    e.getValue().clear();
                    continue;
                }

                List<FileServiceEvent> list = e.getValue();
                int toIndex = -1;

                for(int i = 0; i < list.size(); i++) {
                    FileServiceEvent event = list.get(i);

                    if(event.getLastModified() < epochMillis || event.getOccurred().getTime() < epochMillis) {
                        toIndex++;
                        continue;
                    }

                    break;
                }

                list.subList(0, toIndex + 1).clear();
            }
        } finally {
            flusherLock.readLock().unlock();
            eventsLock.writeLock().unlock();
        }
    }

    @Override
    public void clearServiceEvents() {
        removeServiceEvents((Instant)null);
    }

    @Override
    public void dispose() {
        try {
            descriptionLock.writeLock().lock();
            eventsLock.writeLock().lock();
            flusherLock.writeLock().lock();

            for(IObjectFlusher<FileServiceEvent> flusher : flusherMap.values()) {
                flusher.stop();
            }

            descriptionMap.clear();
            eventsMap.clear();
            flusherMap.clear();
        } finally {
            flusherLock.writeLock().unlock();
            eventsLock.writeLock().unlock();
            descriptionLock.writeLock().unlock();
        }
    }

    private ServiceName getServiceName(ServiceDescription description) {
        String environment = StringUtil.validateFileName(description.getEnvironment());
        String name = StringUtil.validateFileName(description.getName());
        return new ServiceName(environment, name);
    }

    private class DescriptionSerializer implements ISerializer<ServiceDescription> {
        private final ISerializer<FileService> serializer = JSONSerializer.of(FileService.class);

        @Override
        public ServiceDescription deserialize(File input) throws Exception {
            try(InputStream stream = new FileInputStream(input)) {
                return deserialize(stream);
            }
        }

        @Override
        public ServiceDescription deserialize(InputStream input) throws Exception {
            return toServiceDescription(serializer.deserialize(input));
        }

        @Override
        public void serialize(ServiceDescription object, File output) throws Exception {
            try(OutputStream stream = new FileOutputStream(output)) {
                serialize(object, stream);
            }
        }

        @Override
        public void serialize(ServiceDescription object, OutputStream output) throws Exception {
            serializer.serialize(toFileService(object), output);
        }

        private ServiceDescription toServiceDescription(FileService service) {
            ServiceName name = service.getName();
            ServiceDescription description = new ServiceDescription(service.getURI());
            IServiceSettings settings = manager.createServiceSettings(service.getURI());

            description.setName(name.getServiceName());
            description.setEnvironment(name.getEnvironment());
            description.setServiceHandlerClassName(service.getHandlerClassName());
            description.setSettings(settings);

            ServiceStorageHelper.convertMapToServiceSettings(settings, service.getParameters());

            return description;
        }

        private FileService toFileService(ServiceDescription description) {
            FileService service = new FileService();
            ServiceName name = getServiceName(description);
            Map<String, String> parameters = new HashMap<>();

            service.setName(name);
            service.setURI(description.getType());
            service.setParameters(parameters);
            service.setHandlerClassName(description.getServiceHandlerClassName());

            ServiceStorageHelper.convertServiceSettingsToMap(parameters, description.getSettings());

            return service;
        }
    }
}
