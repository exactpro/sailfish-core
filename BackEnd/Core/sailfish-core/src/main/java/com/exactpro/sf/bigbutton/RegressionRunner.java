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
package com.exactpro.sf.bigbutton;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.bigbutton.execution.CombineQueue;
import com.exactpro.sf.bigbutton.execution.ExecutionProgressMonitor;
import com.exactpro.sf.bigbutton.execution.ExecutorClient;
import com.exactpro.sf.bigbutton.execution.ProgressView;
import com.exactpro.sf.bigbutton.execution.RegressionRunnerUtils;
import com.exactpro.sf.bigbutton.importing.LibraryImportResult;
import com.exactpro.sf.bigbutton.library.Executor;
import com.exactpro.sf.bigbutton.library.Library;
import com.exactpro.sf.bigbutton.library.ScriptList;
import com.exactpro.sf.bigbutton.library.Tag;
import com.exactpro.sf.center.impl.PluginLoader;
import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.configuration.workspace.WorkspaceSecurityException;
import com.exactpro.sf.configuration.workspace.WorkspaceStructureException;
import com.exactpro.sf.embedded.mail.EMailService;
import com.exactpro.sf.embedded.statistics.StatisticsService;
import com.exactpro.sf.embedded.statistics.entities.SfInstance;
import com.exactpro.sf.services.ITaskExecutor;
import com.exactpro.sf.storage.IOptionsStorage;

public class RegressionRunner implements AutoCloseable {

	private static final Logger logger = LoggerFactory.getLogger(RegressionRunner.class);

    private final IWorkspaceDispatcher workspaceDispatcher;

	private final ITaskExecutor taskExecutor;

	private List<ExecutorClient> executorClients;

	private volatile ExecutionProgressMonitor monitor;

    private static final Semaphore lock = new Semaphore(1, true);

	private volatile boolean free = true;

    private volatile boolean pause;

	private volatile Library library;

    private final EMailService mailService;

    private final StatisticsService statisticsService;

    private BigButtonSettings settings = new BigButtonSettings();


    public RegressionRunner(ITaskExecutor taskExecutor, IWorkspaceDispatcher workspaceDispatcher, EMailService mailService,
                            IOptionsStorage optionsStorage, StatisticsService statisticsService) {
        this.workspaceDispatcher = workspaceDispatcher;
		this.taskExecutor = taskExecutor;
		this.mailService = mailService;
        this.statisticsService = statisticsService;

        try {
            settings.fillFromMap(optionsStorage.getAllOptions());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
	}

    @Override
    public void close() throws Exception {
        try {
            cleanup();
        } finally {
            this.free = true;
            this.pause = false;
        }
    }

    public void init() {
		this.monitor = new ExecutionProgressMonitor(this);
	}

	private void cleanup() {

        if(executorClients != null) {

            for(ExecutorClient client : executorClients) {
                client.cleanExecutorEnvironment();
                client.tearDown();
			}

		}

	}

	private void startInitThread(Library library) {

		RunInitTask task = new RunInitTask(library);

        taskExecutor.addTask(task);
	}

	private void fillQueue(CombineQueue<ScriptList> listsQueue) {

		for (ScriptList list : library.getScriptLists()) {

            if (list.isRejected()) {
                monitor.rejectScriptList(list);
                continue;
            }

			monitor.listEnqueued(list);

			listsQueue.add(list.getExecutor(), list);

		}

	}

	// clear run results
	public void reset() {

		if(lock.tryAcquire()) {

			try {

                if(!free) {
					throw new IllegalStateException("Reset not allowed in this state");
				}

				this.monitor = new ExecutionProgressMonitor(this);
				this.library = null;
				this.executorClients = null;

			} finally {
				lock.release();
			}

		} else {

			throw new IllegalStateException("Runner is locked");

		}

	}

	// Set library
    public void prepare(LibraryImportResult importResult) {

		if (lock.tryAcquire()) {

			try {

                if(!free || library != null) {
					throw new IllegalStateException("Prepare not allowed in this state");
				}

				this.executorClients = new ArrayList<>();

				CombineQueue<ScriptList> listsQueue = new CombineQueue<>();

				ExecutionProgressMonitor newMonitor = new ExecutionProgressMonitor(this);

                for (Executor executor : importResult.getLibrary().getExecutors()
						.getExecutors()) {

                    listsQueue.register(executor.getName());
                    ExecutorClient client = new ExecutorClient(workspaceDispatcher, listsQueue,
                            importResult.getLibrary(), newMonitor, executor, mailService, this, settings);

					executorClients.add(client);

					//client.start();

				}
				
				newMonitor.getAllExecutors().addAll(executorClients);
				
                this.library = importResult.getLibrary();
				
				this.monitor = newMonitor;
				
                for (ExecutorClient client : executorClients) {
                    if (client.getExecutor().isRejected()) {
                        client.toErrorStatePreparing(client.getExecutor().getRejectCause());
                    }
                }

				fillQueue(listsQueue);
				
                newMonitor.ready(importResult);

			} finally {
				lock.release();
			}

		} else {
			throw new IllegalStateException("Runner is already busy");
		}
		
	}
	
	// Start library execution
	public void run() {
		
		if (lock.tryAcquire()) {
			
			try {
				
				if(!free) {
					throw new IllegalStateException("Runner is already busy");
				}

                if(library == null) {
					throw new IllegalStateException("Library was not prepared");
				}
			
				this.free = false;

                monitor.preparing();
				
				startInitThread(library);
			
			} finally {
				lock.release();
			}
		
		} else {
			throw new IllegalStateException("Runner is already busy");
		}

	}
	
	public ProgressView getProgressView(int inQueueLimit, int outQueueLimit) {

        return monitor.getCurrentProgressView(inQueueLimit, outQueueLimit);
		
	}

    public void interrupt(String message) {
        monitor.interrupt(message);
    }

	public boolean isFree() {
		return free;
	}

	public boolean isPause(){
        return pause;
    }

    public void pause(){
        pause = true;
        monitor.runPaused();
    }

    public void resume(){
        pause = false;
        monitor.resumeRun();
    }

    public IWorkspaceDispatcher getWorkspaceDispatcher() {
        return workspaceDispatcher;
    }

	private class RunInitTask implements Runnable {

        private final Library library;
		
		public RunInitTask(Library library) {
			this.library = library;
		}
		
		private void checkReportsFolder() {

            if(library.getReportsFolder() == null) {
				return;
			}
			
            try {
                workspaceDispatcher.createFolder(FolderType.REPORT, library.getReportsFolder());
            } catch (WorkspaceStructureException | WorkspaceSecurityException e) {
                throw new RuntimeException("Reports directory not created", e);
            }
		}
		
        private boolean registerTags(List<ExecutorClient> clients) {

            if(library.getTagList() == null) {
                return true;
			}

            List<Tag> tags = library.getTagList().getTags();
			
            if (tags.isEmpty()) {
                return true;
            }
				
            for (ExecutorClient client : clients) {
                if (Boolean.TRUE.equals(client.getExecutorReady())) {
                    try {
                        client.registerTags(tags);
                        return true;
                    } catch (Throwable e) {
                        continue;
                    }
				}
            }

            return false;
		}
		
		@Override
		public void run() {

            try {
                logger.info("BB initiator started");

                library.normalize();

                checkReportsFolder();

                //fillQueue();

                List<ExecutorClient> exClients = executorClients;

                String dbSettings = RegressionRunnerUtils.getStatisticsDBSettings();

                File loggingConfiguration = workspaceDispatcher.getFile(FolderType.CFG, PluginLoader.LOG4J_PROPERTIES_FILE_NAME);

                List<ExecutorClient> preparedClients = new ArrayList<>();

                logger.info("Preparing executor clients...");
                for (ExecutorClient client : exClients) {
                    if (client.prepareExecutor(dbSettings, loggingConfiguration)) {
                        preparedClients.add(client);
                    }
                }

                // TODO May be register tags in master instance, because all SF
                // should be connected to the same Statistics DB

                logger.info("Registering tags...");
                if (!registerTags(preparedClients)) {
                    monitor.error("Tags can't be registered");
                    return;
                }

                logger.info("Starting the executor clients...");
                for (ExecutorClient client : preparedClients) {
                            client.start();
                }

                monitor.started();

                logger.info("BB initiator finished");
            } catch (FileNotFoundException e) {
                logger.error("Cannot get logging configuration file",e);
                throw new RuntimeException("Cannot get logging configuration file", e);
            } catch (Exception e) {
                logger.error("Can not execute BB initiator: {}", e.getMessage(), e);
                throw e;
            }
        }
		
		@Override
		public String toString() {
		    return "BB initiator";
		}
		
	}

    public void setSettings(BigButtonSettings settings) {
        this.settings = settings;
    }

    public BigButtonSettings getSettings(){
        return new BigButtonSettings(settings);
    }

    public SfInstance getCurrentSfInstance() {
        return statisticsService.getThisSfInstance();
    }

}
