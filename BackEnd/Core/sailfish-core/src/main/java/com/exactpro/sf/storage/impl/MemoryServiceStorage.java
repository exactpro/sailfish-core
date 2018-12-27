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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Instant;

import com.exactpro.sf.common.services.ServiceInfo;
import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.services.ServiceDescription;
import com.exactpro.sf.services.ServiceEvent;
import com.exactpro.sf.storage.IServiceStorage;
import com.exactpro.sf.storage.SortCriterion;
import com.exactpro.sf.storage.StorageException;
import com.exactpro.sf.storage.StorageFilter;
import com.exactpro.sf.storage.StorageResult;

public class MemoryServiceStorage implements IServiceStorage {

	private static final Logger logger = LoggerFactory.getLogger(MemoryServiceStorage.class);

	private final Map<ServiceName, ServiceDescription> descriptionMap;
	private final Map<ServiceName, LinkedList<ServiceEvent>> eventMap;
	private final ReadWriteLock descriptionLock = new ReentrantReadWriteLock();
	private final ReadWriteLock eventLock = new ReentrantReadWriteLock();

	private final int eventLimit;

	public MemoryServiceStorage(int eventLimit) {
		this.descriptionMap = new HashMap<>();
		this.eventMap = new HashMap<>();
		this.eventLimit = eventLimit;
	}

	public MemoryServiceStorage() {
		this(100);
	}

    @Override
    public ServiceInfo lookupService(ServiceName serviceName) {
        try {
            this.descriptionLock.readLock().lock();

            if(this.descriptionMap.containsKey(serviceName)) {
                return new ServiceInfo(serviceName.toString(), serviceName);
            }
        } finally {
            this.descriptionLock.readLock().unlock();
        }

        return null;
    }

	@Override
	public void addServiceDescription(ServiceDescription description) {
		ServiceName serviceName = new ServiceName(description.getEnvironment(), description.getName());
		try {
			this.descriptionLock.writeLock().lock();
			if (!this.descriptionMap.containsKey(serviceName)) {
				try {
					this.eventLock.writeLock().lock();
					if (!this.eventMap.containsKey(serviceName)) {
						this.descriptionMap.put(serviceName, description);
						this.eventMap.put(serviceName, new LinkedList<ServiceEvent>());
					} else {
						throw new StorageException("Service '" + serviceName + "' with the same name already exists");
					}
				} finally {
					this.eventLock.writeLock().unlock();
				}
			} else {
				throw new StorageException("Service '" + serviceName + "' with the same name already exists");
			}
		} finally {
			this.descriptionLock.writeLock().unlock();
		}
	}

	@Override
	public void removeServiceDescription(ServiceDescription description) {
		ServiceName serviceName = new ServiceName(description.getEnvironment(), description.getName());
		try {
			this.descriptionLock.writeLock().lock();
			try {
				this.eventLock.writeLock().lock();
				this.descriptionMap.remove(serviceName);
				this.eventMap.remove(serviceName);
			} finally {
				this.eventLock.writeLock().unlock();
			}
		} finally {
			this.descriptionLock.writeLock().unlock();
		}
	}

	@Override
	public void updateServiceDescription(ServiceDescription description) {
		ServiceName serviceName = new ServiceName(description.getEnvironment(), description.getName());
		try {
			this.descriptionLock.writeLock().lock();
			this.descriptionMap.put(serviceName, description);
		} finally {
			this.descriptionLock.writeLock().unlock();
		}

	}

	@Override
	public List<ServiceDescription> getServiceDescriptions() {
		try {
			this.descriptionLock.readLock().lock();
			List<ServiceDescription> result = new ArrayList<>(this.descriptionMap.values());
			return result;
		} finally {
			this.descriptionLock.readLock().unlock();
		}
	}

	@Override
	public void addServiceEvent(ServiceDescription description, ServiceEvent event) {
		ServiceName serviceName = new ServiceName(description.getEnvironment(), description.getName());
		try {
			this.eventLock.writeLock().lock();
			LinkedList<ServiceEvent> list = this.eventMap.get(serviceName);
			if (list != null) {
				while (list.size() > this.eventLimit) {
					list.removeLast();
				}
				list.addFirst(event);
			} else {
				logger.warn("Storage does not contain service {}", serviceName);
			}
		} finally {
			this.eventLock.writeLock().unlock();
		}
	}

	@Override
	public long getEventsCount(ServiceDescription description, StorageFilter filter) {
		ServiceName serviceName = new ServiceName(description.getEnvironment(), description.getName());
		try {
			this.eventLock.readLock().lock();
			List<ServiceEvent> list = this.eventMap.get(serviceName);
			return list != null ? list.size() : 0;
		} finally {
			this.eventLock.readLock().unlock();
		}
	}

	@Override
	public StorageResult<ServiceEvent> getServiceEvents(ServiceDescription description, StorageFilter filter,
			int firstRecord, int size, List<SortCriterion> sorting) {
        throw new NotImplementedException("");
	}

	@Override
	public void removeServiceEvents(ServiceDescription description) {
		ServiceName serviceName = new ServiceName(description.getEnvironment(), description.getName());
		try {
			this.eventLock.writeLock().lock();
			List<ServiceEvent> list = this.eventMap.get(serviceName);
			if (list != null) {
				list.clear();
			}
		} finally {
			this.eventLock.writeLock().unlock();
		}
	}

	@Override
    public void removeServiceEvents(Instant olderThan) {
		try {
			this.eventLock.writeLock().lock();
            long epochMillis = olderThan.toEpochMilli();

			for (List<ServiceEvent> list : this.eventMap.values()) {
                if(olderThan == null) {
                    list.clear();
                    continue;
                }

                int toIndex = -1;

                for(int i = 0; i < list.size(); i++) {
                    ServiceEvent event = list.get(i);

                    if(event.getOccurred().getTime() < epochMillis) {
                        toIndex++;
                        continue;
                    }

                    break;
                }

                list.subList(0, toIndex + 1).clear();
			}
		} finally {
			this.eventLock.writeLock().unlock();
		}
	}

	@Override
    public void clearServiceEvents() {
        removeServiceEvents((Instant)null);
    }

    @Override
	public void dispose() {
		try {
			this.descriptionLock.writeLock().lock();
			try {
				this.eventLock.writeLock().lock();
				this.descriptionMap.clear();
				this.eventMap.clear();
			} finally {
				this.eventLock.writeLock().unlock();
			}
		} finally {
			this.descriptionLock.writeLock().unlock();
		}
	}
}
