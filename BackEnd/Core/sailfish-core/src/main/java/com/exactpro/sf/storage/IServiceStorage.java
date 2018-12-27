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

import java.util.List;

import java.time.Instant;

import com.exactpro.sf.common.services.ServiceInfo;
import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.center.IDisposable;
import com.exactpro.sf.services.ServiceDescription;
import com.exactpro.sf.services.ServiceEvent;

public interface IServiceStorage extends IDisposable {
    ServiceInfo lookupService(ServiceName serviceName);

	void addServiceDescription(ServiceDescription description);

	void removeServiceDescription(ServiceDescription description);

	void updateServiceDescription(ServiceDescription description);

	List<ServiceDescription> getServiceDescriptions();

	void addServiceEvent(ServiceDescription description, ServiceEvent event);

	long getEventsCount(ServiceDescription description, StorageFilter filter);

	StorageResult<ServiceEvent> getServiceEvents(ServiceDescription description, StorageFilter filter, int firstRecord, int size, List<SortCriterion> sorting);

    void removeServiceEvents(ServiceDescription description);

    default void removeServiceEvents(Instant olderThan) {
        throw new UnsupportedOperationException("Removing events by timestamp is not supported");
    }

	void clearServiceEvents();
}
