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
 *******************************************************************************/
package com.exactpro.sf.embedded.statistics;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.exactpro.sf.comparison.ComparisonResult;
import com.exactpro.sf.embedded.statistics.configuration.DbmsType;
import com.exactpro.sf.embedded.statistics.handlers.StatisticsReportHandlerLoader;
import com.exactpro.sf.util.DateTimeUtility;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.flywaydb.core.api.MigrationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.center.impl.SFLocalContext;
import com.exactpro.sf.embedded.IEmbeddedService;
import com.exactpro.sf.embedded.configuration.ServiceStatus;
import com.exactpro.sf.embedded.statistics.configuration.StatisticsServiceSettings;
import com.exactpro.sf.embedded.statistics.entities.ActionRun;
import com.exactpro.sf.embedded.statistics.entities.MatrixRun;
import com.exactpro.sf.embedded.statistics.entities.SfInstance;
import com.exactpro.sf.embedded.statistics.entities.Tag;
import com.exactpro.sf.embedded.statistics.entities.TestCase;
import com.exactpro.sf.embedded.statistics.entities.TestCaseRun;
import com.exactpro.sf.embedded.statistics.storage.IStatisticsStorage;
import com.exactpro.sf.embedded.statistics.storage.StatisticsReportingStorage;
import com.exactpro.sf.embedded.statistics.storage.StatisticsStorage;
import com.exactpro.sf.embedded.storage.HibernateStorageSettings;
import com.exactpro.sf.scriptrunner.StatusType;
import com.exactpro.sf.scriptrunner.TestScriptDescription;
import com.exactpro.sf.storage.IMapableSettings;
import com.exactpro.sf.util.BugDescription;

public class StatisticsService extends StatisticsReportHandlerLoader implements IEmbeddedService{

	private static final Logger logger = LoggerFactory.getLogger(StatisticsService.class);

	private BatchInsertWorker insertWorker;

	private volatile StatisticsServiceSettings settings;

	private SfInstance thisSfInstance;

	private TestCase unknownTc;

	private final Map<String, MatrixRun> runningMatrices = new HashMap<>();

	private final Map<String, TestCaseRun> runningTestCases = new HashMap<>();

	private final Map<String, ActionRunSaveTask> runningActions = new HashMap<>();

	private volatile ServiceStatus status = ServiceStatus.Disconnected;

	private volatile String errorMsg = "";

	private BlockingQueue<ActionRunSaveTask> batchInsertQueue = new LinkedBlockingQueue<>();

	private IStatisticsStorage storage;

	// Flyway

	private final StatisticsFlywayWrapper statisticsFlywayWrapper = new StatisticsFlywayWrapper();

	private volatile boolean exceptionEncountered = false;

	private SchemaVersionChecker schemaChecker;

	private void initStorage(HibernateStorageSettings storageSettings) {

		if(this.storage != null) {

			this.storage.tearDown();

		}

		this.storage = new StatisticsStorage(storageSettings);

	}

	public void preCheckConnection() {
		setStatus(ServiceStatus.Checking);
	}

	private void setStatus(ServiceStatus status) {
	    this.status = status;
	    logger.info("Statistics status {}", status);
	}

	private void setError(Exception t) {

	    setStatus(ServiceStatus.Error);

		this.errorMsg = t.getMessage();

		if(t.getCause() != null) {

			this.errorMsg += " (" + t.getCause().getMessage() + ")";

		}

	}

	public synchronized void migrateDB() {

		statisticsFlywayWrapper.migrate();

	}

	private void openMatrixRun(MatrixRun matrixRun, long scriptDescriptionId) {

		this.storage.add(matrixRun);
        TestScriptDescription testScriptRun = SFLocalContext.getDefault().getScriptRunner().getTestScriptDescription(scriptDescriptionId);
        if (testScriptRun == null) {
            throw new EPSCommonException(String.format("TestScriptDescription with [%s] id is missed", scriptDescriptionId));
        }
        testScriptRun.setMatrixRunId(matrixRun.getId());

	}

	@Override
	public boolean isConnected() {

		return this.status.equals(ServiceStatus.Connected);

	}

	public StatisticsService() {

	}

	public StatisticsService(StatisticsServiceSettings settings) {

		this.settings = setDbmsSettings(settings);

	}

	@Override
	public synchronized void init() {

		logger.info("init");

		if(this.status.equals(ServiceStatus.Connected) && settings.isServiceEnabled()) {

			throw new IllegalStateException("Already enabled");

		}

		if(settings.isServiceEnabled()) {

			this.errorMsg = "";

			try {

				// Flyway schema check
				statisticsFlywayWrapper.init(settings.getStorageSettings());

			} catch (RuntimeException t) {
				logger.error(t.getMessage(), t);
				setError(t);
				throw t;
			}

			initStorage(this.settings.getStorageSettings());

			this.thisSfInstance = this.storage.loadSfInstance(this.settings.getThisSfHost(),
					this.settings.getThisSfPort(),
					this.settings.getThisSfName());

			this.unknownTc = this.storage.loadUnknownTestCase();

			// Insert worker thread
			this.insertWorker = new BatchInsertWorker();

			Thread workerThread = new Thread(this.insertWorker, "Statistics insert worker");

			workerThread.setDaemon(true);

			workerThread.start();

			// Schema checker thread
			this.schemaChecker = new SchemaVersionChecker();

			Thread checkerThread = new Thread(this.schemaChecker, "Statistics schema checker");

			checkerThread.setDaemon(true);

			checkerThread.start();

			this.errorMsg = "";

			setStatus(ServiceStatus.Connected);

			logger.info("Statistics service initialized");

		} else {

			this.errorMsg = "";

			setStatus(ServiceStatus.Disconnected);

		}

		logger.info("{}", this.status);

	}

    @Override
	public synchronized void tearDown() {

		logger.info("tearDown");

		if(this.status.equals(ServiceStatus.Disconnected)) {

			return;

		}

		this.batchInsertQueue.clear();

		if(this.insertWorker != null) {

			this.insertWorker.stop();

			this.insertWorker = null;

		}

		if(this.schemaChecker != null) {

			this.schemaChecker.stop();

			this.schemaChecker = null;

			this.exceptionEncountered = false;

		}

		if(this.storage != null) {

			this.storage.tearDown();

			this.storage = null;

			this.errorMsg = "";

			setStatus(ServiceStatus.Disconnected);

		}

		this.runningActions.clear();
		this.runningMatrices.clear();
		this.runningTestCases.clear();

		logger.info("Statistics service disposed");

	}

	public void matrixStarted(String matrixName, String reportFolder, long sfRunId, String environmentName, String userName,
			List<Tag> tags, long scriptDescriptionId) {

		if(!this.status.equals(ServiceStatus.Connected)) {
			return;
		}

		try {

		    logger.debug("Matrix {} started", matrixName);

			MatrixRun matrixRun = new MatrixRun();
			matrixRun.setStartTime(DateTimeUtility.nowLocalDateTime());
			matrixRun.setSfInstance(thisSfInstance);
			matrixRun.setMatrix(this.storage.loadMatrix(matrixName));
			matrixRun.setSfRunId(sfRunId);
			matrixRun.setEnvironment(this.storage.getEnvironmentEntity(environmentName));
			matrixRun.setUser(this.storage.getUserEntity(userName));
			matrixRun.setReportFolder(reportFolder);
			if(tags != null) {
				matrixRun.setTags(new HashSet<>(tags));
			}

			openMatrixRun(matrixRun, scriptDescriptionId);

			this.runningMatrices.put(matrixName, matrixRun);

		} catch(Exception e) {

			logger.error(e.getMessage(), e);

			this.exceptionEncountered = true;

		}

	}

	public void matrixEception(String matrixName, Throwable cause) {

        if(!this.status.equals(ServiceStatus.Connected)) {
            return;
        }

        try {

            logger.debug("Matrix {} init/run exception", matrixName);

            MatrixRun matrixRun = this.runningMatrices.get(matrixName);

            if(matrixRun == null) {

                logger.error("Unknown matrix finashed! {}", matrixName);

                return;

            }

            matrixRun.setFailReason(cause.getMessage());

            this.storage.update(matrixRun);

        } catch(Throwable t) {

            logger.error(t.getMessage(), t);

            this.exceptionEncountered = true;

        }

    }

	public void matrixFinished(String matrixName) {

		if(!this.status.equals(ServiceStatus.Connected)) {
			return;
		}

		try {

		    logger.debug("Matrix {} finished", matrixName);

			TestCaseRun notClosedTcRun = runningTestCases.remove(matrixName);

			if(notClosedTcRun != null) {

				logger.error("TC {} not closed", notClosedTcRun);

			}

			MatrixRun matrixRun = this.runningMatrices.remove(matrixName);

			if(matrixRun == null) {

				logger.error("Unknown matrix finashed! {}", matrixName);

				return;

			}

			matrixRun.setFinishTime(DateTimeUtility.nowLocalDateTime());

			this.storage.update(matrixRun);

		} catch(Throwable t) {

			logger.error(t.getMessage(), t);

			this.exceptionEncountered = true;

		}

	}

	public void testCaseStarted(String matrixName, String tcId, String reportFile, String description, long rank, int tcHash) {

		if(!this.status.equals(ServiceStatus.Connected)) {
			return;
		}

		try {

			logger.debug("TC finished {}, {}, {}, {}", matrixName, tcId, description);

			TestCaseRun tcRun = new TestCaseRun();

			tcRun.setReportFile(reportFile);
			tcRun.setDescription(StringEscapeUtils.escapeEcmaScript(description));
			tcRun.setStartTime(DateTimeUtility.nowLocalDateTime());
			tcRun.setRank(rank);
			tcRun.setHash(tcHash);

			if(tcId != null) {

				tcRun.setTestCase(this.storage.loadTestCase(tcId));

			} else {

				tcRun.setTestCase(this.unknownTc);

			}

			tcRun.setMatrixRun(this.runningMatrices.get(matrixName));

			this.storage.add(tcRun);

			this.runningTestCases.put(matrixName, tcRun);

		} catch(Throwable t) {

			logger.error(t.getMessage(), t);

			this.exceptionEncountered = true;

		}

	}

	public void testCaseFinished(String matrixName, StatusType status, String failReason, Set<BugDescription> knownBugs) { // status, description

		if(!this.status.equals(ServiceStatus.Connected)) {
			return;
		}

		try {

			logger.debug("TC finished {}, {}, {}, {}", matrixName, status);

			TestCaseRun tcRun = this.runningTestCases.remove(matrixName);

			if(tcRun == null) {

				logger.error("Finished unknown test case! {}, {}, {}, {}", matrixName, status);
				return;

			}

			tcRun.setFinishTime(DateTimeUtility.nowLocalDateTime());

			tcRun.setStatus(status);

			tcRun.setFailReason(failReason);

            if (knownBugs != null && !knownBugs.isEmpty()) {
                tcRun.setComment(String.format("Known bugs: [%s]", StringUtils.join(knownBugs, ", ")));
            }

			this.storage.update(tcRun);

		} catch(Throwable t) {

			logger.error(t.getMessage(), t);

			exceptionEncountered = true;

		}

	}

    public void actionStarted(String matrixName, String serviceName, String actionName, String msgType,
                              String description, long rank, String tag, int hash) {

		if(!this.status.equals(ServiceStatus.Connected)) {
			return;
		}

		logger.debug("Action started {}, {}", matrixName, actionName);

		ActionRun actionRun = new ActionRun();

		actionRun.setDescription(StringEscapeUtils.escapeEcmaScript(description));

		actionRun.setRank(rank);

		actionRun.setTcRun(this.runningTestCases.get(matrixName));

        actionRun.setTag(tag);

        actionRun.setHash(hash);

		ActionRunSaveTask saveTask = new ActionRunSaveTask(actionRun, serviceName, msgType, actionName);

		this.runningActions.put(matrixName, saveTask);

	}

	public void actionFinished(String matrixName, StatusType status, String failReason) {

		if(!this.status.equals(ServiceStatus.Connected)) {
			return;
		}

		try {
    		ActionRunSaveTask task = this.runningActions.remove(matrixName);

    		if(task == null) {
    			logger.error("Unknown action finished! {}, {}, {}", matrixName, status, failReason);
    			return;
    		}

    		logger.debug("Action finished {}, {}", matrixName, task.getAction());

    		task.getActionRun().setStatus(status);
    		task.getActionRun().setFailReason(failReason);

			this.batchInsertQueue.put(task);

		} catch (InterruptedException e) {
			logger.error("Put interrupted", e);
			Thread.currentThread().interrupt();
		} catch (Throwable e) {
		    logger.error(e.getMessage(), e);
        }

	}

    public void actionVerification(String matrixName, ComparisonResult result) {
        if (result != null) {
            Set<BugDescription> allKnownBugs = result.getAllKnownBugs();
            if (!allKnownBugs.isEmpty()) {
                Set<BugDescription> reproducedBugs = result.getReproducedBugs();
                Set<BugDescription> noReproducedBugs = Sets.difference(allKnownBugs, reproducedBugs);
                ActionRunSaveTask actionRunSaveTask = this.runningActions.get(matrixName);
                if (actionRunSaveTask == null) {
                    logger.error("Unknown action create verification! {}, {}", matrixName, result);
                    return;
                }
                actionRunSaveTask.getReproducedKnownBugs().addAll(reproducedBugs);
                actionRunSaveTask.getNoReproducedKnownBugs().addAll(noReproducedBugs);
            }
        }
    }

	public StatisticsServiceSettings getSettings() {
		return settings;
	}

    @Override
	public void setSettings(IMapableSettings settings) {
        this.settings = setDbmsSettings((StatisticsServiceSettings) settings);
        logger.debug("Set StatisticService settings {}", this.settings);
	}

    private StatisticsServiceSettings setDbmsSettings(StatisticsServiceSettings settings) {
        HibernateStorageSettings hibSettings = settings.getStorageSettings();
        DbmsType dbmsType = DbmsType.getTypeByName(hibSettings.getDbms());
        dbmsType.setDbmsSettings(hibSettings);
        return settings;
    }

    @Override
	public ServiceStatus getStatus() {
		return status;
	}

    @Override
	public String getErrorMsg() {
		return errorMsg;
	}

	public StatisticsReportingStorage getReportingStorage() {
		return this.storage.getReportingStorage();
	}

	private class ActionRunSaveTask {

		private ActionRun actionRun;

		private String service;

		private String msgType;

		private String action;

		private Set<BugDescription> reproducedKnownBugs = new HashSet<>();

		private Set<BugDescription> noReproducedKnownBugs = new HashSet<>();

		public ActionRunSaveTask(ActionRun actionRun, String service,
				String msgType, String action) {

			this.actionRun = actionRun;
			this.service   = service;
			this.msgType   = msgType;
			this.action    = action;

		}

		public ActionRun getActionRun() {
			return actionRun;
		}

		public String getService() {
			return service;
		}

		public String getMsgType() {
			return msgType;
		}

		public String getAction() {
			return action;
		}

        public Set<BugDescription> getReproducedKnownBugs() {
            return reproducedKnownBugs;
        }

        public Set<BugDescription> getNoReproducedKnownBugs() {
            return noReproducedKnownBugs;
        }
    }

	private class BatchInsertWorker implements Runnable {

		private volatile boolean running = true;

		public void stop() {

			this.running = false;

		}

		@Override
		public void run() {

			logger.info("Statistics InsertWorker started");

			while(this.running) {

				try {

					ActionRunSaveTask task = batchInsertQueue.poll();

					if(task == null) {

						Thread.sleep(700L);
						continue;
					}

					ActionRun actionRun = task.getActionRun();

					if(task.getAction() != null) {
						actionRun.setAction(storage.getActionEntity(task.getAction()));
					}

					if(task.getService() != null) {
						actionRun.setService(storage.getServiceEntity(task.getService()));
					}

					if(task.getMsgType() != null) {
						actionRun.setMsgType(storage.getMsgTypeEntity(task.getMsgType()));
					}

                    for (BugDescription bugDescription : task.getReproducedKnownBugs()) {
                        actionRun.addKnownBug(
                                storage.loadKnownBug(bugDescription.getSubject(), bugDescription.getCategories().list()), true);
                    }

                    for (BugDescription bugDescription : task.getNoReproducedKnownBugs()) {
                        actionRun.addKnownBug(
                                storage.loadKnownBug(bugDescription.getSubject(), bugDescription.getCategories().list()), false);
                    }

                    storage.add(actionRun);

				} catch (InterruptedException e) {

					logger.error("Interrupted", e);
					break;

				} catch(Throwable t) {

					logger.error(t.getMessage(), t);

					exceptionEncountered = true;

				}

			}

			logger.info("Statistics InsertWorker stopped");

		}

	}

	private class SchemaVersionChecker implements Runnable {

		private volatile boolean running = true;

		public void stop() {

			this.running = false;

		}

		@Override
		public void run() {

			logger.info("Schema checker thread started");

			while(running) {

				if(isConnected() && exceptionEncountered) {

					logger.info("Checking schema version");

					try {

						statisticsFlywayWrapper.init(settings.getStorageSettings());

					} catch (Exception e) {

						logger.error(e.getMessage(), e);

						setError(e);

					}

					exceptionEncountered = false;

				}

				try {

					Thread.sleep(6000L);

				} catch (InterruptedException e) {
					logger.error("Interrupted", e);
					break;
				}

			}

			logger.info("Schema checker thread stopped");

		}

	}

	public IStatisticsStorage getStorage() {
		return storage;
	}

	public MigrationInfo getCurrentDbVersionInfo() {
		return statisticsFlywayWrapper.getCurrentDbVersionInfo();
	}

	public MigrationInfo[] getPendingMigrationsInfo() {
		return statisticsFlywayWrapper.getPendingMigrationsInfo();
	}

	public boolean isMigrationRequired() {
		return statisticsFlywayWrapper.isMigrationRequired();
	}

	public boolean isSfUpdateRequired() {
		return statisticsFlywayWrapper.isSfUpdateRequired();
	}

    public SfInstance getThisSfInstance() {
        return thisSfInstance;
    }

}