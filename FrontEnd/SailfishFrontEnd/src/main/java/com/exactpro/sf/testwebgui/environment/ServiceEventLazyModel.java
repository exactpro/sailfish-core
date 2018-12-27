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
package com.exactpro.sf.testwebgui.environment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.services.ServiceDescription;
import com.exactpro.sf.services.ServiceEvent;
import com.exactpro.sf.storage.FilterCriterion;
import com.exactpro.sf.storage.SortCriterion;
import com.exactpro.sf.storage.StorageFilter;
import com.exactpro.sf.storage.StorageResult;
import com.exactpro.sf.testwebgui.BeanUtil;

public class ServiceEventLazyModel<T extends ServiceEventModel> extends LazyDataModel<ServiceEventModel> {

	private static final long serialVersionUID = 6615645316507025683L;

	private static final Logger logger = LoggerFactory.getLogger(ServiceEventLazyModel.class);

	private ServiceName serviceName;

	public ServiceEventLazyModel(ServiceName serviceName) {
		this.serviceName = serviceName;
	}

	@Override
	public List<ServiceEventModel> load(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String,Object> filters) {

		try	{

			ServiceDescription description = BeanUtil.getSfContext().getConnectionManager().getServiceDescription(this.serviceName);

			List<SortCriterion> sorting = new ArrayList<>(1);

			boolean sortAscending = SortOrder.ASCENDING == sortOrder;

			if (sortField != null) {
				sorting.add(new SortCriterion(sortField, sortAscending));
			}

			sorting.add(new SortCriterion("id", !sortAscending));

			StorageFilter filter = new StorageFilter();
			for(String key : filters.keySet()) {
				FilterCriterion filterCriterion = new FilterCriterion(key, "%" + filters.get(key) + "%", FilterCriterion.Operation.LIKE);
				filter.addCriterion(filterCriterion);
			}

            setRowCount((int)BeanUtil.getSfContext().getServiceStorage().getEventsCount(description, filter));
            StorageResult<ServiceEvent> eventsResult = BeanUtil.getSfContext().getServiceStorage().getServiceEvents(description, filter, first, pageSize, sorting);

			List<ServiceEventModel> result = new ArrayList<>();

			for(ServiceEvent event : eventsResult.getList()) {
				result.add(new ServiceEventModel(event));
			}

			return result;

		} catch ( Exception e ) {
			logger.error("Could not get service events", e);
		}

		return new ArrayList<>();

	}

	public ServiceName getServiceName() {
		return serviceName;
	}

}
