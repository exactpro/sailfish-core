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
package com.exactpro.sf.scriptrunner;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.aml.generator.GeneratedScript;
import com.exactpro.sf.common.adapting.IAdapterManager;
import com.exactpro.sf.configuration.IDictionaryManager;
import com.exactpro.sf.configuration.IEnvironmentManager;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.embedded.statistics.StatisticsService;
import com.exactpro.sf.scriptrunner.TestScriptDescription.ScriptState;
import com.exactpro.sf.scriptrunner.TestScriptDescription.ScriptStatus;
import com.exactpro.sf.scriptrunner.actionmanager.IActionManager;
import com.exactpro.sf.scriptrunner.impl.DefaultScriptConfig;
import com.exactpro.sf.scriptrunner.languagemanager.LanguageManager;
import com.exactpro.sf.scriptrunner.services.IStaticServiceManager;
import com.exactpro.sf.scriptrunner.utilitymanager.IUtilityManager;
import com.exactpro.sf.storage.ITestScriptStorage;

public class AsyncScriptRunner extends AbstractScriptRunner {

	private final static Logger logger = LoggerFactory.getLogger(AsyncScriptRunner.class);

	public AsyncScriptRunner(
    		final IWorkspaceDispatcher wd,
    		final IDictionaryManager dictionaryManager,
    		final IActionManager actionManager,
    		final IUtilityManager utilityManager,
    		final LanguageManager languageManager,
    		final PreprocessorLoader preprocessorLoader,
    		final ValidatorLoader validatorLoader,
    		final ScriptRunnerSettings settings,
    		final StatisticsService statisticsService,
    		final IEnvironmentManager environmentManager,
    		final ITestScriptStorage testScriptStorage,
    		final IAdapterManager adapterManager,
    		final IStaticServiceManager staticServiceManager,
    		final String compilerClassPath) {
		super(wd, dictionaryManager, actionManager, utilityManager, languageManager, preprocessorLoader, validatorLoader, settings, statisticsService, environmentManager, testScriptStorage, adapterManager, staticServiceManager, compilerClassPath);
		tScriptCompiler = new Thread(new ScriptCompiler(), "ScriptCompiler");
		tScriptExecutor = new Thread(new ScriptExecutor(), "ScriptExecutor");
		tScriptCompiler.start();
		tScriptExecutor.start();
	}

	class ScriptCompiler implements Runnable {
		private final ExecutorService executorService = Executors.newFixedThreadPool(2);
		private final Queue<Map.Entry<Long, GeneratedScript>> scriptsForCompileQueue = new ConcurrentLinkedQueue<>();

		@Override
        public void run() {
			while (!isDisposing) {
				try {
					if (!scriptsForCompileQueue.isEmpty()) {
						final Map.Entry<Long, GeneratedScript> entry = scriptsForCompileQueue.poll();
						final TestScriptDescription descrForCompile = testScripts.get(entry.getKey());

	        			if (descrForCompile == null) {
	        				logger.warn("Can't find script [{}]. Probably it was removed", entry.getKey());
	        				continue;
	        			}

						executorService.submit(new Runnable() {
							@Override
							public void run() {
								try {
									compileScript(entry.getValue(), descrForCompile);
									if (descrForCompile.getAutoRun()) {
									    synchronized (preparedTestScripts) {
                                            preparedTestScripts.add(entry.getKey());
                                        }
									} else {
									    synchronized (pendingTestScriptsToRun) {
                                            pendingTestScriptsToRun.add(entry.getKey());
                                        }
									}
									descrForCompile.scriptReady();
								} catch (Throwable e) {
									scriptExceptionProcessing(descrForCompile, e);
								}
							}
						});
					}

					Thread.sleep(100);

                    final Long testScript;
                    synchronized (addedTestScripts) {
                        testScript = addedTestScripts.poll();
                    }

					if (testScript == null) {
						continue;
					}

					final TestScriptDescription descrForPrep = testScripts.get(testScript);

        			if (descrForPrep == null) {
        				logger.warn("Can't find script [{}]. Probably it was removed", testScript);
        				continue;
        			}

					if (descrForPrep.getState() == ScriptState.CANCELED) {
						continue;
					}

                    if (descrForPrep.isSetCancelFlag()) {
                        cancelScript(descrForPrep);
                        continue;
                    }

					logger.info("TestScript {} is being prepared", testScript);
					descrForPrep.scriptPreparing();
					executorService.submit(new Runnable() {
						@Override
						public void run() {
							try {
								GeneratedScript generatedScript = prepareScript(descrForPrep);
								Map.Entry<Long, GeneratedScript> entry = new AbstractMap.SimpleEntry<>(testScript, generatedScript);
								scriptsForCompileQueue.add(entry);
							} catch (Exception e) {
								scriptExceptionProcessing(descrForPrep, e);
							}
						}
					});
				} catch (InterruptedException e) {
					logger.error(e.getMessage(), e);
					break;
				}
			}
		}
	}

	class ScriptExecutor implements Runnable {
	    private final int MAX_THREADS = 3;
		private final ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors() * 2);
        private final Set<String> locksServices = new HashSet<>();
        private final List<Long> prepared = new ArrayList<>();

		@Override
        public void run() {

			Map<Long, Future<Throwable>> runningScriptMap = new HashMap<>();
			try {

				while (!isDisposing) {
					try {
					    pullScripts();

						stopScripts(runningScriptMap);

						startScript(runningScriptMap);

						resultScript(runningScriptMap);

                        filterCancelledScripts();

						Thread.sleep(DEFAULT_TIMEOUT);
					} catch (InterruptedException e) {
						if (isDisposing) {
							interuptScripts(runningScriptMap);
						}
					}
				}
				interuptScripts(runningScriptMap);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			} finally {
				scheduledThreadPool.shutdown();
                try {
					scheduledThreadPool.awaitTermination(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    logger.error("", e);
                }
				logger.info("AsyncScriptRunner thread finished");
			}
		}

        private boolean skipScriptExecution(TestScriptDescription description) {
            if (description == null) {
                logger.warn("Script description is NULL");
            } else if (description.getStatus() == ScriptStatus.CANCELED) {
                logger.warn("Script {} already has state CANCELED but it shouldn't", description.getId());
            }
            return description == null
                    || description.isSetCancelFlag();
        }

        private void filterCancelledScripts() {
            for (Iterator<Long> iterator = prepared.iterator();
                    iterator.hasNext(); ) {
                Long id = iterator.next();
                TestScriptDescription description = testScripts.get(id);
                if (skipScriptExecution(description)) {
                    if (description != null) {
                        if (description.getStatus() != ScriptStatus.CANCELED) {
                            cancelScript(description);
                            logger.info("Script {} has been filtered from the prepared list and cancelled", id);
                        } else {
                            logger.info("Script {} has been filtered from the prepared list because it was already cancelled", id);
                        }
                    } else {
                        logger.warn("Can't find script {}", id);
                    }
                    iterator.remove();
                }
            }
        }

        private void  stopScripts(Map<Long, Future<Throwable>> runningScriptMap) throws InterruptedException {
			boolean localShutdown = shutdown;

			for (Entry<Long, Future<Throwable>> scriptFeature : runningScriptMap.entrySet()) {
				Long scriptId = scriptFeature.getKey();

				TestScriptDescription description = testScripts.get(scriptId);
				if (localShutdown || (description != null && description.isSetCancelFlag())) {
					logger.warn("Shutdown script {}", scriptId);
					Future<Throwable> future = scriptFeature.getValue();

					if (!future.isDone()) {
						future.cancel(true);
					}
				}
			}

			if (localShutdown) {
				shutdown = false;
			}
		}

		private void interuptScripts(Map<Long, Future<Throwable>> runningScriptMap) {
			for (Entry<Long, Future<Throwable>> scriptFeature : runningScriptMap.entrySet()) {
				Long currentTestScript = scriptFeature.getKey();
				Future<Throwable> future = scriptFeature.getValue();

				TestScriptDescription descr = testScripts.get(currentTestScript);

				if (future.cancel(true)) {
					descr.scriptInterrupted();
					logger.info("TestScript {} was interrupted", currentTestScript);
				} else {
					descr.scriptExecuted();
					logger.info("TestScript {} was executed", currentTestScript);
				}
                onRunFinished(descr);
			}

			pullScripts();
			for (Long tsId : this.prepared) {
				TestScriptDescription descr = testScripts.get(tsId);
                descr.scriptNotStarted();
                onRunFinished(descr);
				logger.info("TestScript {} was not started", tsId);
			}
			this.prepared.clear();
		}

		private void startScript(Map<Long, Future<Throwable>> runningScriptMap) throws InterruptedException {
		    if (this.prepared.isEmpty())
		    	return;
		    if (runningScriptMap.size() >= MAX_THREADS)
		    	return;

		        Iterator<Long> iterator = this.prepared.iterator();
    		    while (iterator.hasNext() && runningScriptMap.size() < MAX_THREADS) {
    		        Long currentTestScript = iterator.next();

        			TestScriptDescription descr = testScripts.get(currentTestScript);

                    logger.info("Get script {}", currentTestScript);
                    if (skipScriptExecution(descr)) {
                        continue;
                    }

        			if (tryToLockServices(descr)) {
        			    iterator.remove();
            			logger.info("TestScript {} was taken to prepare for run", currentTestScript);

            			try {
            					onRunStarted(descr);

            					Class<? extends SailFishTestCase> testCaseClass = descr.getClassLoader().loadClass(descr.getClassName()).asSubclass(SailFishTestCase.class);

            					DefaultScriptConfig scriptConfiguration = new DefaultScriptConfig(
            					        descr.getScriptSettings(),
            							descr.getWorkFolder(),
            							descr.getDescription(),
            							descr.getScriptLogger());

            					logger.info("TestScript {} is being run", currentTestScript);

            					descr.getContext().setScriptConfig(scriptConfiguration);

            					runningScriptMap.put(
            							currentTestScript,
            							scheduledThreadPool.submit(new InternalScript(testCaseClass, descr.getContext())));

            					descr.scriptRan();

            			} catch (Throwable e) {
                            descr.scriptInitFailed(e);
                            onRunFinished(descr);

            				logger.error("TestScript [{}] was failed during preparation", currentTestScript, e);
            			}
        			}
    		    }
		}

		private void resultScript(Map<Long, Future<Throwable>> runningScriptMap) throws InterruptedException {
			Iterator<Entry<Long, Future<Throwable>>> iterator = runningScriptMap.entrySet().iterator();

			while (iterator.hasNext()) {
				Entry<Long, Future<Throwable>> scriptFeature = iterator.next();
				Future<Throwable> future = scriptFeature.getValue();

				if (future.isDone()) {
					iterator.remove();
					Long currentTestScript = scriptFeature.getKey();
					TestScriptDescription descr = testScripts.get(currentTestScript);
					unlockServices(descr);

					Throwable result;
					try {
						result = future.get();
					} catch (Exception e) {
						logger.warn("Interrupt of matrix execution, reason : {}", e.getMessage(), e);
						result = e;
					}

					if (future.isCancelled()) {
						descr.scriptInterrupted();
					} else if (result != null) {
						descr.scriptRunFailed(result);
					} else {
						descr.scriptExecuted();
					}
                    onRunFinished(descr);

					logger.info("TestScript {} was executed", currentTestScript);
				}
			}
		}

        private void pullScripts() {
            Long testScriptId;
            do {
                synchronized (preparedTestScripts) {
                    testScriptId = preparedTestScripts.poll();
                    if (testScriptId != null) {
                        logger.info("TestScript {} was taken from waiting queue", testScriptId);
                        this.prepared.add(testScriptId);
                    }
                }
            }
            while (testScriptId != null);
        }

		private boolean tryToLockServices(TestScriptDescription descr) {
		    List<String> services = descr.getContext().getServiceList();
		    if (Collections.disjoint(this.locksServices, services)) {
		        this.locksServices.addAll(services);
		        logger.info("TestScript {} locked services {}", descr.getId(), services);
		        return true;
		    }
		    return false;
		}

		private void unlockServices(TestScriptDescription descr) {
            List<String> services = descr.getContext().getServiceList();
            this.locksServices.removeAll(services);
            logger.info("TestScript {} unlocked services {}", descr.getId(), services);
        }
	}

}