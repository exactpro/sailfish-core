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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.hibernate.SessionFactory;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyComponentPathImpl;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.DefaultComponentSafeNamingStrategy;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.embedded.statistics.StatisticsUtils;
import com.exactpro.sf.embedded.statistics.entities.Action;
import com.exactpro.sf.embedded.statistics.entities.ActionRun;
import com.exactpro.sf.embedded.statistics.entities.ActionRunKnownBug;
import com.exactpro.sf.embedded.statistics.entities.Environment;
import com.exactpro.sf.embedded.statistics.entities.KnownBug;
import com.exactpro.sf.embedded.statistics.entities.Matrix;
import com.exactpro.sf.embedded.statistics.entities.MatrixRun;
import com.exactpro.sf.embedded.statistics.entities.MessageType;
import com.exactpro.sf.embedded.statistics.entities.Service;
import com.exactpro.sf.embedded.statistics.entities.SfInstance;
import com.exactpro.sf.embedded.statistics.entities.Tag;
import com.exactpro.sf.embedded.statistics.entities.TagGroup;
import com.exactpro.sf.embedded.statistics.entities.TestCase;
import com.exactpro.sf.embedded.statistics.entities.TestCaseRun;
import com.exactpro.sf.embedded.statistics.entities.TestCaseRunStatus;
import com.exactpro.sf.embedded.statistics.entities.TestCaseRunTag;
import com.exactpro.sf.embedded.statistics.entities.User;
import com.exactpro.sf.embedded.storage.AbstractHibernateStorage;
import com.exactpro.sf.embedded.storage.HibernateStorageSettings;
import com.exactpro.sf.storage.StorageException;
import com.exactpro.sf.util.LRUMap;

public class StatisticsStorage extends AbstractHibernateStorage implements IStatisticsStorage {
	
	private static final Logger logger = LoggerFactory.getLogger(StatisticsStorage.class);
	
	public static final String UNKNOWN_TC_ID = "_unknown_tc_";
	
	private StatisticsReportingStorage reportingStorage;

    private final Map<String, Environment> environmentsCache = new HashMap<String, Environment>();

    private final Map<String, User> usersCache = new HashMap<String, User>();

    private final Map<String, Action> actionsCache = Collections.synchronizedMap(new LRUMap<String, Action>(40));

    private final Map<String, MessageType> msgTypesCache = Collections.synchronizedMap(new LRUMap<String, MessageType>(40));

    private final Map<String, KnownBug> knownBugCache = Collections.synchronizedMap(new LRUMap<>(40));
	
	public StatisticsStorage(HibernateStorageSettings settings) {
		super(settings);
	}
		
	@Override
	protected void configure(HibernateStorageSettings settings, Configuration configuration) {
		// Init hibernate
        configuration.addPackage("com.exactpro.sf.statistics.entities")

        .setProperty("hibernate.query.plan_cache_max_size", "64")
        .setProperty("hibernate.query.plan_parameter_metadata_max_size", "32")

	    .addAnnotatedClass(SfInstance.class)
	    .addAnnotatedClass(Matrix.class)
	    .addAnnotatedClass(MatrixRun.class)
	    .addAnnotatedClass(TestCase.class)
	    .addAnnotatedClass(TestCaseRun.class)
	    
	    .addAnnotatedClass(Action.class)
	    .addAnnotatedClass(ActionRun.class)
	    .addAnnotatedClass(Environment.class)
	    .addAnnotatedClass(MessageType.class)
	    .addAnnotatedClass(Service.class)
	    .addAnnotatedClass(User.class)
	    .addAnnotatedClass(Tag.class)
	    .addAnnotatedClass(TestCaseRunStatus.class)
        .addAnnotatedClass(TagGroup.class)
        .addAnnotatedClass(KnownBug.class)
        .addAnnotatedClass(TestCaseRunTag.class)
        .addAnnotatedClass(ActionRunKnownBug.class);
		configuration.setImplicitNamingStrategy(ImplicitNamingStrategyComponentPathOld.INSTANCE);
    }

	@Override
	protected void configure(HibernateStorageSettings settings, SessionFactory sessionFactory) {
	    this.reportingStorage = new StatisticsReportingStorage(sessionFactory, settings);	    
	}
	
	@Override
    public SfInstance loadSfInstance(String host, String port, String sfName) {

		SfInstance result;

        List<SfInstance> queryResults = loadSfInstancesBy(host, port, sfName);
		
		if(!queryResults.isEmpty()) {
			
			result = queryResults.get(0);
			
		} else {
			
			logger.info("Adding new sf instance into statistics DB");
			
			result = new SfInstance();
			
			result.setHost(host);
			result.setPort(Integer.parseInt( port ));
			result.setName(sfName);

            storage.add(result);
			
		}
		
		logger.info("{}", result);
		
		return result;
		
	}

    @Override
    public SfInstance getSfInstance(String host, String port, String sfName) {
        List<SfInstance> instances = loadSfInstancesBy(host, port, sfName);
        if (CollectionUtils.isNotEmpty(instances)) {
            return instances.get(0);
        }
        return null;
    }

    @Override
    public TestCase loadUnknownTestCase() {

        TestCase result = storage.getEntityByField(TestCase.class, "testCaseId", UNKNOWN_TC_ID);
		
		if(result == null) {
			
			logger.info("Unknown TC not present in DB");
			
			result = new TestCase();
			
			result.setTestCaseId(UNKNOWN_TC_ID);

            storage.add(result);
			
		}
		
		return result;
		
	}
	
	@Override
    public Environment getEnvironmentEntity(String name) {

        if(environmentsCache.containsKey(name)) {

            return environmentsCache.get(name);
			
		} else {

            Environment result = storage.getEntityByField(Environment.class, "name", name);
			
			if(result == null) {
				
				logger.info("New environment entity {}", name);
				
				result = new Environment();
				
				result.setName(name);

                storage.add(result);
				
			}

            environmentsCache.put(name, result);
			
			return result;
			
		}
		
	}
	
	@Override
    public User getUserEntity(String name) {

        if(usersCache.containsKey(name)) {

            return usersCache.get(name);
			
		} else {

            User result = storage.getEntityByField(User.class, "name", name);
			
			if(result == null) {
				
				logger.info("New environment entity {}", name);
				
				result = new User();
				
				result.setName(name);

                storage.add(result);
				
			}

            usersCache.put(name, result);
			
			return result;
			
		}
		
	}
	
	@Override
    public Service getServiceEntity(String name) {

        Service result = storage.getEntityByField(Service.class, "name", name);
		
		if(result == null) {
			
			logger.info("New service entity {}", name);
			
			result = new Service();
			
			result.setName(name);

            storage.add(result);
			
		}
		
		return result;
		
	}
	
	@Override
    public Action getActionEntity(String name) {

        Action result = actionsCache.get(name);
		
		if(result != null) {
			
			logger.trace("Actions cache hit");
			
			return result;
			
		}

        result = storage.getEntityByField(Action.class, "name", name);
		
		if(result == null) {
			
			logger.info("New Action entity {}", name);
			
			result = new Action();
			
			result.setName(name);

            storage.add(result);
			
		}

        actionsCache.put(name, result);
		
		return result;
		
	}
	
	@Override
    public MessageType getMsgTypeEntity(String name) {

        MessageType result = msgTypesCache.get(name);
		
		if(result != null) {
		
			return result;
			
		}

        result = storage.getEntityByField(MessageType.class, "name", name);
		
		if(result == null) {
			
			logger.info("New MessageType entity {}", name);
			
			result = new MessageType();
			
			result.setName(name);

            storage.add(result);
			
		}

        msgTypesCache.put(name, result);
		
		return result;
		
	}
	
	@Override
    public TestCase loadTestCase(String tcId) {

        TestCase result = storage.getEntityByField(TestCase.class, "testCaseId", tcId);
		
		if(result == null) {
			
			// Add to DB
			
			result = new TestCase();
			
			result.setTestCaseId(tcId);

            storage.add(result);
			
		}
		
		return result;
		
	}
	
	@Override
    public Matrix loadMatrix(String name) {

        Matrix result = storage.getEntityByField(Matrix.class, "name", name);
		
		if(result == null) {
			
			// Add to DB
			
			result = new Matrix();
			
			result.setName(name);

            storage.add(result);
			
		}
		
		return result;
		
	}

    @Override
    public KnownBug loadKnownBug(String subject, List<String> categories) {
        String knownBugJson = StatisticsUtils.buildKnownBugJson(subject, categories);
        KnownBug result = knownBugCache.get(knownBugJson);
        if (result != null) {
            logger.info("KnownBug cache hit");
            return result;
        }
        // TODO: it's normal for single thread but for multi threads it can be reason of errors, because this logic could be executed from different threads at the same time
        // TODO: fix this logic in other methods too which loading reference information
        result = storage.getEntityByField(KnownBug.class, "knownBug", knownBugJson);
        if (result == null) {
            result = new KnownBug();
            result.setKnownBug(knownBugJson);
            storage.add(result);
        }

        knownBugCache.put(knownBugJson, result);
        return result;
    }

    @Override
	public StatisticsReportingStorage getReportingStorage() {
		return reportingStorage;
	}

	@Override
	public List<SfInstance> getAllSfInstances() {

        return storage.getAllEntities(SfInstance.class);
		
	}
	
	@Override
	public List<Tag> getAllTags() {

        return storage.getAllEntities(Tag.class);
		
	}
	
	@Override
	public TagGroup getGroupByName(String name) {

        return storage.getEntityByField(TagGroup.class, "name", name);
		
	}
	
	@Override
	public List<Tag> getTagsWithoutGroup() {
		
		List<Criterion> criterions = new ArrayList<>();
		
		criterions.add(Restrictions.isNull("group"));

        return storage.getAllEntities(Tag.class, criterions, "name", true);
		
	}
	
	@Override
	public List<TagGroup> getAllTagGroups() {

        return storage.getAllEntities(TagGroup.class, new ArrayList<Criterion>(), "name", true);
		
	}
	
	@Override
	public SfInstance getSfInstanceById(long id) {

        return (SfInstance)storage.getEntityById(SfInstance.class, id);
		
	}

	@Override
	public List<TestCase> getAllTestCases() {

        return storage.getAllEntities(TestCase.class);
	}

	@Override
	public TestCase getTestCaseById(long id) {

        return (TestCase)storage.getEntityById(TestCase.class, id);
	}

	@Override
	public Tag getTagByName(String name) {

        return storage.getEntityByField(Tag.class, "name", name);
		
	}
	
	@Override
	public List<TestCaseRunStatus> getAllRunStatuses() {

        return storage.getAllEntities(TestCaseRunStatus.class);
		
	}
	
	@Override
	public void updateTcrUserComments(long testCaseId, TestCaseRunComments comments) {

        TestCaseRun tcr = (TestCaseRun)storage.getEntityById(TestCaseRun.class, testCaseId);
		
		tcr.setComment(comments.getComment());
		tcr.setFixRevision(comments.getFixedVersion());
		tcr.setRunStatus(comments.getStatus());

        storage.update(tcr);
		
	}
	
	@Override
	public List<TestCase> getTestCasesContains(String namePart) {
		
		List<Criterion> criterions = new ArrayList<Criterion>();
		
		criterions.add(Restrictions.ilike("testCaseId", "%" + namePart + "%"));

        return storage.getAllEntities(TestCase.class, criterions, "testCaseId", true);
		
	}
	
	@Override
	public TestCase getTestCaseByTcId(String id) {

        return storage.getEntityByField(TestCase.class, "testCaseId", id);
		
	}
	
	@Override
	public List<Tag> getTagsContains(String namePart) {
		
		List<Criterion> criterions = new ArrayList<Criterion>();
		
		criterions.add(Restrictions.ilike("name", "%" + namePart + "%"));

        return storage.getAllEntities(Tag.class, criterions, "name", true);
		
	}
	
	@Override
	public List<TagGroup> getGroupsContains(String namePart) {
		
		List<Criterion> criterions = new ArrayList<Criterion>();
		
		criterions.add(Restrictions.ilike("name", "%" + namePart + "%"));

        return storage.getAllEntities(TagGroup.class, criterions, "name", true);
		
	}

    @Override
    public void updateSfCurrentID(long matrixRunId, long sfCurrentID) {
        MatrixRun matrixRun = (MatrixRun)storage.getEntityById(MatrixRun.class, matrixRunId);
        String message;

        if (matrixRun == null) {
            message = String.format("Could not update sf current id - MatrixRun with [%s] id is not exist in the db", matrixRunId);
            logger.error(message);
            throw new StorageException(message);
        }

        SfInstance currentSfInstance = getSfInstanceById(sfCurrentID);

        if (currentSfInstance == null) {
            message = String.format("Could not update sf current id - SfInstance with [%s] id is not exist in the db", sfCurrentID);
            logger.error(message);
            throw new StorageException(message);
        }

        matrixRun.setSfCurrentInstance(currentSfInstance);
        storage.update(matrixRun);
    }

    @Override
    public TestCaseRun getTestCaseRunById(long id) {
        return (TestCaseRun)storage.getEntityById(TestCaseRun.class, id);
    }

    @Override
    public MatrixRun getMatrixRunById(long id) {
        return (MatrixRun)storage.getEntityById(MatrixRun.class, id);
    }

    private List<SfInstance> loadSfInstancesBy(String host, String port, String sfName) {
        List<Criterion> criterions = new ArrayList<Criterion>();
        criterions.add(Restrictions.eq("host", host));
        criterions.add(Restrictions.eq("port", Integer.parseInt( port )));
        criterions.add(Restrictions.eq("name", sfName));
        return storage.getAllEntities(SfInstance.class, criterions);
    }
}