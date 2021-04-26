/******************************************************************************
 * Copyright 2009-2021 Exactpro (Exactpro Systems Limited)
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.center.IDisposable;
import com.exactpro.sf.common.services.ServiceInfo;
import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.services.ServiceDescription;
import com.exactpro.sf.services.ServiceEvent;
import com.exactpro.sf.storage.IServiceStorage;
import com.exactpro.sf.storage.SortCriterion;
import com.exactpro.sf.storage.StorageFilter;
import com.exactpro.sf.storage.StorageResult;

public class BroadcastServiceStorage implements IServiceStorage {

    private static final Logger logger = LoggerFactory.getLogger(BroadcastServiceStorage.class);

    private static final String ERROR_MESSAGE_ON_ADD_DESCRIPTION = "Can`t add service description to all storages";
    private static final String ERROR_MESSAGE_ON_REMOVE_DESCRIPTION = "Can`t remove service description from all storages";
    private static final String ERROR_MESSAGE_ON_UPDATE_DESCRIPTION = "Can`t update service description for all storages";
    private static final String ERROR_MESSAGE_ON_ADD_EVENT = "Can`t add service event to all storages";
    private static final String ERROR_MESSAGE_ON_REMOVE_EVENT = "Can`t remove service events from all storages";
    private static final String ERROR_MESSAGE_ON_CLEAR_EVENT = "Can`t clear service events in all storages";
    private static final String ERROR_MESSAGE_ON_DISPOSE = "Can`t dispose in all storages";

    private final IServiceStorage primaryStorage;
    private final List<IServiceStorage> writableStorages;

    public BroadcastServiceStorage(IServiceStorage primaryStorage, List<IServiceStorage> secondaryStorages) {
        this.primaryStorage = Objects.requireNonNull(primaryStorage, "Primary storage can`t be null");
        List<ServiceDescription> serviceDescriptions = primaryStorage.getServiceDescriptions();
        secondaryStorages.forEach(storage -> serviceDescriptions.forEach(storage::addServiceDescription));
        writableStorages = new ArrayList<>();
        writableStorages.add(this.primaryStorage);
        writableStorages.addAll(Objects.requireNonNull(secondaryStorages, "Secondary storages can`t be null"));
    }
    public BroadcastServiceStorage(IServiceStorage primaryStorage, IServiceStorage... secondaryStorages) {
        this(primaryStorage, secondaryStorages.length > 0 ? Arrays.asList(secondaryStorages) : Collections.emptyList());
    }


    @NotNull
    @Override
    public ServiceInfo lookupService(ServiceName serviceName) {
        return primaryStorage.lookupService(serviceName);
    }

    @Override
    public void addServiceDescription(ServiceDescription description) {
        execute(serviceStorage -> serviceStorage.addServiceDescription(description), ERROR_MESSAGE_ON_ADD_DESCRIPTION);
    }

    @Override
    public void removeServiceDescription(ServiceDescription description) {
        execute(serviceStorage -> serviceStorage.removeServiceDescription(description), ERROR_MESSAGE_ON_REMOVE_DESCRIPTION);
    }

    @Override
    public void removeServiceDescriptions(Iterator<ServiceDescription> iterator) {
        execute(serviceStorage -> serviceStorage.removeServiceDescriptions(iterator), ERROR_MESSAGE_ON_REMOVE_DESCRIPTION);
    }

    @Override
    public void updateServiceDescription(ServiceDescription description) {
        execute(serviceStorage -> serviceStorage.updateServiceDescription(description), ERROR_MESSAGE_ON_UPDATE_DESCRIPTION);
    }

    @Override
    public List<ServiceDescription> getServiceDescriptions() {
        return primaryStorage.getServiceDescriptions();
    }

    @Override
    public void addServiceEvent(ServiceDescription description, ServiceEvent event) {
        execute(serviceStorage -> serviceStorage.addServiceEvent(description, event), ERROR_MESSAGE_ON_ADD_EVENT);
    }

    @Override
    public long getEventsCount(ServiceDescription description, StorageFilter filter) {
        return primaryStorage.getEventsCount(description, filter);
    }

    @Override
    public StorageResult<ServiceEvent> getServiceEvents(ServiceDescription description, StorageFilter filter, int firstRecord, int size, List<SortCriterion> sorting) {
        return primaryStorage.getServiceEvents(description, filter, firstRecord, size, sorting);
    }

    @Override
    public void removeServiceEvents(ServiceDescription description) {
        execute(serviceStorage -> serviceStorage.removeServiceEvents(description), ERROR_MESSAGE_ON_REMOVE_EVENT);
    }

    @Override
    public void clearServiceEvents() {
        execute(IServiceStorage::clearServiceEvents, ERROR_MESSAGE_ON_CLEAR_EVENT);
    }

    @Override
    public void dispose() {
        execute(IDisposable::dispose, ERROR_MESSAGE_ON_DISPOSE);
    }

    private void execute(Consumer<IServiceStorage> action, String errorMessage) {
        EPSCommonException exception = null;
        for (IServiceStorage storage : writableStorages) {
            try {
                action.accept(storage);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                if (exception == null) {
                    exception = new EPSCommonException(errorMessage);
                }
                exception.addSuppressed(e);
            }
        }

        if (exception != null && exception.getSuppressed().length > 0) {
            throw exception;
        }
    }
}
