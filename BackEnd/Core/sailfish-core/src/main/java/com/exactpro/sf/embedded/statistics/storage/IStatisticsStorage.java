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
package com.exactpro.sf.embedded.statistics.storage;

import java.util.List;

import com.exactpro.sf.embedded.statistics.entities.Action;
import com.exactpro.sf.embedded.statistics.entities.Environment;
import com.exactpro.sf.embedded.statistics.entities.KnownBug;
import com.exactpro.sf.embedded.statistics.entities.Matrix;
import com.exactpro.sf.embedded.statistics.entities.MessageType;
import com.exactpro.sf.embedded.statistics.entities.Service;
import com.exactpro.sf.embedded.statistics.entities.SfInstance;
import com.exactpro.sf.embedded.statistics.entities.Tag;
import com.exactpro.sf.embedded.statistics.entities.TagGroup;
import com.exactpro.sf.embedded.statistics.entities.TestCase;
import com.exactpro.sf.embedded.statistics.entities.TestCaseRunStatus;
import com.exactpro.sf.embedded.statistics.entities.User;
import com.exactpro.sf.embedded.storage.IHibernateStorage;

public interface IStatisticsStorage extends IHibernateStorage {
	
	SfInstance loadSfInstance(String host, String port, String sfName);
	
	TestCase loadUnknownTestCase();
	
	Environment getEnvironmentEntity(String name);
	
	User getUserEntity(String name);
	
	Service getServiceEntity(String name);
	
	Action getActionEntity(String name);
	
	MessageType getMsgTypeEntity(String name);
	
	TestCase loadTestCase(String tcId);
	
	Matrix loadMatrix(String name);

	KnownBug loadKnownBug(String subject, List<String> categories);
	
	StatisticsReportingStorage getReportingStorage();
	
	List<SfInstance> getAllSfInstances();
	
	SfInstance getSfInstanceById(long id);
	
	List<TestCase> getAllTestCases();
	
	TestCase getTestCaseById(long id);
	
	List<Tag> getAllTags();
	
	Tag getTagByName(String name);

	List<TestCaseRunStatus> getAllRunStatuses();

	void updateTcrUserComments(long testCaseId, TestCaseRunComments comments);

	List<TagGroup> getAllTagGroups();

	List<Tag> getTagsWithoutGroup();

	TagGroup getGroupByName(String name);

	List<TestCase> getTestCasesContains(String namePart);

	TestCase getTestCaseByTcId(String id);

	List<Tag> getTagsContains(String namePart);

	List<TagGroup> getGroupsContains(String namePart);

    void updateSfCurrentID(long matrixRunId, long sfCurrentID);
}
