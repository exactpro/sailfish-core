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

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
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

public class SyncScriptRunner extends AbstractScriptRunner {

	private final static Logger logger = LoggerFactory.getLogger(SyncScriptRunner.class);

	private volatile Long currentTestScript = null;

	public SyncScriptRunner(
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

		@Override
        public void run() {
		    ExecutorService compiler = null;
		    try {
		        logger.info("Thread [{}] start", Thread.currentThread().getName());
		        compiler = new ThreadPoolExecutor(1, 1, 1000, TimeUnit.MICROSECONDS, new LinkedBlockingQueue<Runnable>());

    			TestScriptDescription descr = null;
    			while (!isDisposing) {
    				try {
    					Thread.sleep(100);

                        Long testScript;
                        synchronized (addedTestScripts) {
                            testScript = addedTestScripts.poll();
                        }

    					if (testScript == null)
    						continue;


    					descr = testScripts.get(testScript);

	        			if (descr == null) {
	        				logger.warn("Can't find script [{}]. Probably it was removed", testScript);
	        				continue;
	        			}

    					if (descr.getState() == ScriptState.CANCELED) {
    						continue;
    					}

                        if (descr.isSetCancelFlag()) {
                            cancelScript(descr);
                            continue;
                        }

    					logger.info("TestScript {} is being prepared", testScript);
    					descr.scriptPreparing();
    					GeneratedScript script = prepareScript(descr);
    					compileScript(script, descr);
    					if (descr.getAutoRun()) {
    					    synchronized (preparedTestScripts) {
                                preparedTestScripts.add(testScript);
                            }
    					} else {
    					    synchronized (pendingTestScriptsToRun) {
                                pendingTestScriptsToRun.add(testScript);
                            }
    					}
                        descr.scriptReady();
    				} catch (Throwable e) {
    					scriptExceptionProcessing(descr, e);
    				}
    			}
		    } catch (Throwable e) {
		        logger.error(e.getMessage(), e);
		    } finally {
	            if (compiler != null) {
	                compiler.shutdown();
	                try {
		                if (!compiler.awaitTermination(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)) {
		                    logger.warn("Compile executor shutdown failed");
		                }
	                } catch (InterruptedException e) {
	                    logger.error(e.getMessage(), e);
	                }
	            }
	            logger.info("Thread [{}] stop", Thread.currentThread().getName());
		    }
		}
	}

	class ScriptExecutor implements Runnable {

		@Override
        public void run() {

			ExecutorService executor = null;
			boolean execute = false;

			try {
			    logger.info("Thread [{}] start", Thread.currentThread().getName());
				executor = new ThreadPoolExecutor(1, 1, 1000, TimeUnit.MICROSECONDS, new LinkedBlockingQueue<Runnable>());

				Future<Throwable> future = null;

				while (!isDisposing) {
					try {
						Thread.sleep(100);
                        if (currentTestScript != null) {
                            TestScriptDescription description = testScripts.get(currentTestScript);
                            if (shutdown || (description != null && description.isSetCancelFlag())) {
                                logger.warn("Shutdown script #{}", currentTestScript);

                                while (!future.isDone()) {
                                    future.cancel(true);

                                    Thread.sleep(100);
                                }

                                shutdown = false;
                            }
                        }

						if (execute == false) {
						    synchronized (preparedTestScripts) {
                                currentTestScript = preparedTestScripts.poll();
                            }

							if (currentTestScript == null)
								continue;

							TestScriptDescription descr = testScripts.get(currentTestScript);

		        			if (descr == null) {
		        				logger.warn("Can't find script [{}]. Probably it was removed", currentTestScript);
		        				continue;
		        			}

							if (descr.getStatus() == ScriptStatus.CANCELED) {
								continue;
							}

                            if (descr.isSetCancelFlag()) {
                                cancelScript(descr);
                                continue;
                            }

							logger.info("TestScript {} was taken from waiting queue", currentTestScript);

							try {

								logger.info("TestScript {} was prepared", currentTestScript);

									onRunStarted(descr);

									Class<? extends SailFishTestCase> testCaseClass = descr.getClassLoader().loadClass(descr.getClassName()).asSubclass(SailFishTestCase.class);

	            					DefaultScriptConfig scriptConfiguration = new DefaultScriptConfig(
	            							descr.getScriptSettings(),
	            							descr.getWorkFolder(),
	            							descr.getDescription(),
	            							descr.getScriptLogger());

									logger.info("TestScript {} is being run", currentTestScript);

									descr.getContext().setScriptConfig(scriptConfiguration);

									future = executor.submit(new InternalScript(testCaseClass, descr.getContext()));

									execute = true;

									descr.scriptRan();

							} catch (Throwable e) {

								descr.scriptInitFailed(e);
								onRunFinished(descr);

								logger.error("TestScript [{}] was failed during preparation", currentTestScript, e);
								execute = false;
								currentTestScript = -1L;

							}
						} else {
							if (future != null && future.isDone()) {

								TestScriptDescription descr = testScripts.get(currentTestScript);

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

								execute = false;

								currentTestScript = -1L;

							} else {
							    synchronized (preparedTestScripts) {
							        checkQueueOnCanceledScript(preparedTestScripts);
                                }
								Thread.sleep(DEFAULT_TIMEOUT);
							}
						}
					} catch (InterruptedException e) {
						if (isDisposing) {
							if (execute == true) {
								TestScriptDescription descr = testScripts.get(currentTestScript);


								if (future != null && future.cancel(true)) {
									descr.scriptInterrupted();
									logger.info("TestScript {} was interrupted", currentTestScript);
								} else {
									descr.scriptExecuted();
									logger.info("TestScript {} was executed", currentTestScript);
								}
                                onRunFinished(descr);

								execute = false;

								currentTestScript = -1L;
							}

                            processNotStartedScripts();
						}
					}
				}

				if (execute == true) {

					TestScriptDescription descr = testScripts.get(currentTestScript);

					if (future != null && future.cancel(true)) {
						descr.scriptInterrupted();
						logger.info("TestScript {} was interrupted", currentTestScript);
					} else {
						descr.scriptExecuted();
						logger.info("TestScript {} was executed", currentTestScript);
					}
                    onRunFinished(descr);

					execute = false;

					currentTestScript = -1L;
				}

                processNotStartedScripts();

			} catch (Throwable e) {
			    logger.error(e.getMessage(), e);
			} finally {
				if (executor != null) {
					executor.shutdown();
					try {
                        if (!executor.awaitTermination(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)) {
                            logger.warn("Script executor shutdown failed");
                        }
                    } catch (InterruptedException e) {
                        logger.error(e.getMessage(), e);
                    }
				}
				logger.info("Thread [{}] stop", Thread.currentThread().getName());
			}
		}

        private void processNotStartedScripts() {
            synchronized (preparedTestScripts) {
                for (Long tsId : preparedTestScripts) {
                    TestScriptDescription descr = testScripts.get(tsId);
                    descr.scriptNotStarted();
                    onRunFinished(descr);
                    logger.info("TestScript {} was not started", tsId);
                }
                preparedTestScripts.clear();
            }
        }
    }
}