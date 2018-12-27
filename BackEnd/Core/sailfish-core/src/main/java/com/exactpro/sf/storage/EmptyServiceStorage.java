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

import java.util.Collections;
import java.util.List;

import com.exactpro.sf.common.services.ServiceInfo;
import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.services.ServiceDescription;
import com.exactpro.sf.services.ServiceEvent;

public class EmptyServiceStorage implements IServiceStorage {

    @Override
    public void dispose() {
        // do nothing
    }

    @Override
    public ServiceInfo lookupService(ServiceName serviceName) {
        return new ServiceInfo(serviceName.toString(), serviceName);
    }

    @Override
    public void addServiceDescription(ServiceDescription description) {
        // do nothing
    }

    @Override
    public void removeServiceDescription(ServiceDescription description) {
        // do nothing
    }

    @Override
    public void updateServiceDescription(ServiceDescription description) {
        // do nothing
    }

    @Override
    public List<ServiceDescription> getServiceDescriptions() {
        return Collections.emptyList();
    }

    @Override
    public void addServiceEvent(ServiceDescription description, ServiceEvent event) {
        // do nothing
    }

    @Override
    public long getEventsCount(ServiceDescription description, StorageFilter filter) {
        return 0;
    }

    @Override
    public StorageResult<ServiceEvent> getServiceEvents(ServiceDescription description, StorageFilter filter, int firstRecord, int size,
            List<SortCriterion> sorting) {
        return StorageResult.emptyResult();
    }

    @Override
    public void removeServiceEvents(ServiceDescription description) {
        // do nothing
    }

    @Override
    public void clearServiceEvents() {
        // do nothing
    }

}
