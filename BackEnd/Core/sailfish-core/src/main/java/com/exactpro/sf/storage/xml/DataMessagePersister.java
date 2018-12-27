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
package com.exactpro.sf.storage.xml;

import java.util.List;

import com.exactpro.sf.storage.FilterCriterion;
import com.exactpro.sf.storage.SortCriterion;
import com.exactpro.sf.storage.StorageException;

public interface DataMessagePersister {
	void persist(List<DataMessage> messages) throws StorageException;
	void persist(DataMessage message) throws StorageException;
	
	DataMessage getDataMessage(List<FilterCriterion> filterCriterions, List<SortCriterion> sortCriterions) throws StorageException; 
	List<DataMessage> getDataMessages(List<FilterCriterion> filterCriterions, List<SortCriterion> sortCriterions) throws StorageException;
}
