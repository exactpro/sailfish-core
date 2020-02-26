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
package com.exactpro.sf.bigbutton.execution;

import com.exactpro.sf.SFAPIClient;
import com.exactpro.sf.Service.Status;
import com.exactpro.sf.ServiceImportResult;
import com.exactpro.sf.bigbutton.BigButtonSettings;
import com.exactpro.sf.bigbutton.RegressionRunner;
import com.exactpro.sf.bigbutton.importing.ImportError;
import com.exactpro.sf.bigbutton.library.BigButtonAction;
import com.exactpro.sf.bigbutton.library.Executor;
import com.exactpro.sf.bigbutton.library.Globals;
import com.exactpro.sf.bigbutton.library.IBBActionExecutor;
import com.exactpro.sf.bigbutton.library.Library;
import com.exactpro.sf.bigbutton.library.Script;
import com.exactpro.sf.bigbutton.library.Script.ScriptRemoteInformation;
import com.exactpro.sf.bigbutton.library.ScriptList;
import com.exactpro.sf.bigbutton.library.ScriptList.ScriptListStatus;
import com.exactpro.sf.bigbutton.library.Service;
import com.exactpro.sf.bigbutton.library.ServiceList;
import com.exactpro.sf.bigbutton.library.SfApiOptions;
import com.exactpro.sf.bigbutton.library.StartMode;
import com.exactpro.sf.bigbutton.library.Tag;
import com.exactpro.sf.bigbutton.util.BigButtonUtil;
import com.exactpro.sf.center.impl.SfInstanceInfo;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.embedded.mail.EMailService;
import com.exactpro.sf.exceptions.APICallException;
import com.exactpro.sf.exceptions.APIResponseException;
import com.exactpro.sf.scriptrunner.StatusType;
import com.exactpro.sf.scriptrunner.TestScriptDescription;
import com.exactpro.sf.scriptrunner.state.ScriptState;
import com.exactpro.sf.scriptrunner.state.ScriptStatus;
import com.exactpro.sf.testwebgui.restapi.xml.XmlMatrixUploadResponse;
import com.exactpro.sf.testwebgui.restapi.xml.XmlResponse;
import com.exactpro.sf.testwebgui.restapi.xml.XmlTestScriptShortReport;
import com.exactpro.sf.testwebgui.restapi.xml.XmlTestscriptActionResponse;
import com.exactpro.sf.util.EMailUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterators;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ExecutorClient {
	
	private static final Logger logger = LoggerFactory.getLogger(ExecutorClient.class);
	
	private static final String BB_ENVIRONMENT = "bb__automation";
	
    private final IWorkspaceDispatcher workspaceDispatcher;

	private final CombineQueue<ScriptList> listsQueue;
	
	private final Library library;
	
	private final ExecutionProgressMonitor monitor;
	
	private final Executor executor;
	
	private SFAPIClient apiClient;
	
	private volatile ExecutorState state = ExecutorState.Inactive;
	
	private volatile ScriptList currentList;
	
	private ClientWorker worker;
	
    private final Set<String> errorText = Collections.synchronizedSet(new HashSet<String>());

    private final ObjectMapper mapper = new ObjectMapper();

    private volatile Boolean executorReady;
	
    private final Map<String, Service> executorServicesUploaded = new LinkedHashMap<>();

    private  final Map<BigButtonAction, BBActionConsumer> actions = new EnumMap<>(BigButtonAction.class);

    private final EMailService mailService;

    private final RegressionRunner runner;

    private final BigButtonSettings settings;

    private final SfInstanceInfo sfInstanceInfo;

    private boolean isMasterSf;

    public ExecutorClient(IWorkspaceDispatcher workspaceDispatcher, CombineQueue<ScriptList> listsQueue, Library library, ExecutionProgressMonitor monitor,
                          Executor executor, EMailService mailService, RegressionRunner runner, BigButtonSettings settings, SfInstanceInfo sfInstanceInfo) {
		
        this.workspaceDispatcher = workspaceDispatcher;
		this.listsQueue = listsQueue;
		this.library = library;
		this.monitor = monitor;
		this.executor = executor;
		this.mailService = mailService;
		this.runner = runner;
        this.settings = settings;
        this.sfInstanceInfo = sfInstanceInfo;

        actions.put(BigButtonAction.Interrupt, this::interrupt);
        actions.put(BigButtonAction.Skip, this::skipList);
        actions.put(BigButtonAction.SendEmail, this::sendEmail);
	}

    public void toErrorState(Throwable t) {
		
        toErrorState(RegressionRunnerUtils.createErrorText(t));
	}
	
    public void toErrorState(String errMessage) {

        String execErrMessage = String.format("Executor \"%s\" : error. Cause: %s", executor.getName(), errMessage);
        logger.error(execErrMessage);
        errorText.add(errMessage);
		this.state = ExecutorState.Error;
        monitor.executorClientEncounteredError(this);
        monitor.warn(execErrMessage);
        tearDown();

    }

    public void toErrorStatePreparing(ImportError errMessage) {

        logger.error(String.format("Executor \"%s\" error because of parsing errors", executor.getName()));

        for (ImportError error : errMessage.getCause()) {
            errorText.add(error.getMessage());
        }
        this.state = ExecutorState.Error;
        monitor.executorClientEncounteredError(this);

    }

    public void toWarnState(Throwable t) {

        String message = String.format("Executor \"%s\" : error. Cause: %s", executor.getName(), RegressionRunnerUtils.createErrorText(t));
        logger.warn(message);
        errorText.add(RegressionRunnerUtils.createErrorText(t));
        monitor.warn(message);
		
	}
	
	public void start() {

        if(apiClient == null) {
            createApiClient();
		}

        checkIsMaster();

		this.worker = new ClientWorker();

        Thread workerThread = new Thread(worker, "BB Executor " + executor.getName());
		
		workerThread.setDaemon(true);
		
		workerThread.start();
		
	}

    private void createApiClient() {
		try {
            this.apiClient = new SFAPIClient(URI.create(executor.getHttpUrl() + "/sfapi").normalize().toString());
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void tearDown() {

        if(worker != null) {
            worker.stop();
        }
		
	}

	public ExecutorState getState() {
		return state;
	}
	
    public boolean prepareExecutor(String xmlConfig, File loggingConfiguration) {
        if(state == ExecutorState.Error) {
            return false;
        }
        try {
            if (apiClient == null) {
                createApiClient();
            }
            apiClient.setStatisticsDBSettings(xmlConfig);

            if (settings.isCloneLoggingConfiguration()) {
                apiClient.setLoggingConfiguration(loggingConfiguration);
            }

            uploadVariableSets();
            uploadExecutorServices();

            if(prepareExecutorServices().stream().anyMatch(service -> service.getStartMode() == StartMode.EXECUTOR)) {
                logger.debug("Setting variable set for executor '{}' to '{}'", executor.getName(), executor.getVariableSet());
                setVariableSet(executor.getVariableSet());
            } else {
                logger.debug("Skipped setting variable set for executor '{}' to '{}'", executor.getName(), executor.getVariableSet());
            }

            serviceCommand(executorServicesUploaded.values().stream(), apiClient::startService, "start", StartMode.EXECUTOR);
            return true;
        } catch (Exception e) {
            toErrorState(e);
            return false;
        }
	}
	
    private void uploadExecutorServices() throws Exception {

        Set<Service> toUpload = prepareExecutorServices();

        for (Service service : toUpload) {

                if (!executorServicesUploaded.containsValue(service)) {
                    uploadService(service, executorServicesUploaded);
                }
            }
        }

    private void uploadService(Service service, Map<String, Service> uploadResult) throws Exception {

        logger.debug("Uploading {}", service);

        try (InputStream fileStream = BigButtonUtil.getStream(library.getRootFolder(), service.getPath(), workspaceDispatcher)) {

            List<ServiceImportResult> importResult = apiClient.importServices(service.getPath(), BB_ENVIRONMENT, fileStream, true, false);

            for (ServiceImportResult result : importResult) {

                if(result.getStatus() == Status.ERROR) {

                    String errMessage = String.format("Service <%s> import failed. Cause: <%s>", service.getPath(), result.getProblem());

                    throw new ServiceUploadException(errMessage);

                }

                uploadResult.put(result.getServiceName(), service);

            }
        }
    }

    private void uploadVariableSets() throws Exception {
        String variableSetsFile = library.getVariableSetsFile();

        if(variableSetsFile == null) {
            return;
        }

        logger.debug("Uploading variable sets from file '{}' to '{}'", variableSetsFile, executor.getName());

        try(InputStream variableSets = BigButtonUtil.getStream(library.getRootFolder(), variableSetsFile, workspaceDispatcher)) {
            Set<String> uploadedVariableSets = apiClient.importVariableSets(variableSetsFile, variableSets, true).getVariableSets();
            logger.debug("Uploaded variable sets '{}' to '{}'", uploadedVariableSets, executor.getName());
        }
    }

    private void setVariableSet(String name) throws Exception {
        if(name == null) {
            name = library.getGlobals().map(Globals::getVariableSet).orElse(null);
        }

        logger.debug("Setting variable set for '{}' to '{}'", executor.getName(), name);
        apiClient.setEnvironmentVariableSet(BB_ENVIRONMENT, name);
    }

    private void serviceCommand(Stream<Service> services, APIServiceConsumer command, String commandName, StartMode startMode) {
        if (startMode != null) {
            services = services.filter(service -> startMode == service.getStartMode());
        }

        Map<String, Exception> serviceExceptions = new HashMap<>();

        services.distinct().map(Service::getName).forEach(name -> {
            try {
                command.accept(BB_ENVIRONMENT, name);
            } catch(Exception e) {
                logger.error("Command '{}' was not executed for service: {}", commandName, name);
                serviceExceptions.put(name, e);
            }
        });

        if(!serviceExceptions.isEmpty()) {
            RuntimeException e = new EPSCommonException(String.format("Command '%s' was not executed for services: %s", commandName, serviceExceptions.keySet()));

            for(Exception exception : serviceExceptions.values()) {
                e.addSuppressed(exception);
            }

            throw e;
        }
    }

    private Set<Service> prepareExecutorServices() {
        Map<String, ServiceList> knownServiceLists = library.getServiceLists();

        Iterator<String> serviceIterator =
                library.getGlobals().isPresent()
                ? Iterators.concat(library.getGlobals().get().getServiceLists().iterator(), executor.getServices().iterator())
                : executor.getServices().iterator() ;

        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(serviceIterator, Spliterator.IMMUTABLE | Spliterator.NONNULL), false)
                .distinct()
                .map(serviceListName -> {
                    ServiceList serviceList = knownServiceLists.get(serviceListName);
                    if (!knownServiceLists.containsKey(serviceListName)) {
                        throw new RuntimeException("Unknown service list '" + serviceListName + "'");
            }
                    return serviceList;
                }).flatMap(serviceList -> serviceList.getServices().stream())
                .collect(Collectors.toSet());
    }
    
    public void cleanExecutorEnvironment() {
        if(state == ExecutorState.Error) {
            logger.warn(String.format("Executor \"%s\" : can not clean environment, executor is in Error state ", executor.getName()));
            return;
        }
        try {
            serviceCommand(executorServicesUploaded.values().stream(), apiClient::stopService, "stop", StartMode.EXECUTOR);
        } catch (RuntimeException e) {
            toErrorState(e);
            }
        try {
            serviceCommand(executorServicesUploaded.values().stream(), apiClient::deleteService, "delete", null);
        } catch (RuntimeException e) {
            toErrorState(e);
        }

        stopRunningTestScripts();

    }

	public void registerTags(List<Tag> tags) {
		
		this.state = ExecutorState.RegisteringTags;
		
		Set<String> knownGroups = new HashSet<>();
		
		try {

            if(apiClient == null) {
                createApiClient();
			}
		
			for(Tag tag : tags) {

                XmlResponse response;
				
				if(StringUtils.isNotEmpty(tag.getGroup())) {
					
					if(!knownGroups.contains(tag.getGroup())) {

                        response = apiClient.registerTagGroup(tag.getGroup());
						
						logger.debug("Group registration response {}: {}", response.getMessage(), response.getRootCause());
						
						if(!response.getMessage().toLowerCase().contains("success")) {
							throw new RuntimeException(response.getMessage());
						}
						
						knownGroups.add(tag.getGroup());
						
					}

                    response = apiClient.registerTagInGroup(tag.getName(), tag.getGroup());
					
				} else {

                    response = apiClient.registerTag(tag.getName());
					
				}
				
				logger.debug("Tag registration response {}: {}", response.getMessage(), response.getRootCause());
				
				if(!response.getMessage().toLowerCase().contains("success")) {
					throw new RuntimeException(response.getMessage());
				}
				
			}
		
		} catch (Exception e) {
			throw new RuntimeException("Tags registration failed. " + e.getMessage());
			
		}
		
		this.state = ExecutorState.Inactive;
		
	}
	
	private interface APIServiceConsumer {
	    void accept(String envName, String svcName) throws APICallException, APIResponseException;
	}

	private interface BBActionConsumer {
        void accept(StatusType status, String subSubject, IBBActionExecutor... parameters);
    }

	private class ClientWorker implements Runnable {
		
		private volatile boolean running = true;
		
		private String relativeListReportsFolder;

        private final Map<String, Service> scriptListServicesUploaded = new LinkedHashMap<>();

		public void stop() {
			this.running = false;
		}
		
        private Set<Service> prepareScriptListServices() {
			Map<String, ServiceList> knownServiceLists = library.getServiceLists();
            Set<Service> services = new LinkedHashSet<>();
			
            for (String key : currentList.getServiceLists()) {
				if(!knownServiceLists.containsKey(key)) {
					throw new RuntimeException("Unknown service list '" + key + "'");
				}
				
                for (Service service : knownServiceLists.get(key).getServices()) {
                    if (!executorServicesUploaded.containsValue(service)) {
                        services.add(service);
			        }
				}
				
			}
			
            return services;
				
		}
				
        private void uploadScriptListServices() throws Exception {
            Set<Service> toUpload = prepareScriptListServices();
			
            for (Service service : toUpload) {
					
				logger.debug("Uploading {}", service);
					
                uploadService(service, scriptListServicesUploaded);
			}
		}
				
					
		
		private boolean beforeListRun() {
			
			try {
                uploadScriptListServices();

                if(!prepareScriptListServices().isEmpty() &&
                        prepareExecutorServices().stream().noneMatch(service -> service.getStartMode() == StartMode.EXECUTOR)) {
                    logger.debug("Setting variable set before script list '{}' run to '{}'", currentList.getName(), currentList.getVariableSet());
                    setVariableSet(currentList.getVariableSet());
                }

                Stream<Service> uploaded = Stream.concat(
                        executorServicesUploaded.values().stream(),
                        scriptListServicesUploaded.values().stream());
                serviceCommand(uploaded, apiClient::startService, "start", StartMode.LIST);
				
				return true;
				
            } catch (Exception e) {
				logger.error(e.getMessage(), e);
                toWarnState(e);

				return false;
			}
			
		}

        private void cleanScriptListEnvironment() throws Exception {

            Stream<Service> services = Stream.concat(
                    executorServicesUploaded.values().stream(),
                    scriptListServicesUploaded.values().stream());

            serviceCommand(services, apiClient::stopService, "stop", StartMode.LIST);

            serviceCommand(scriptListServicesUploaded.values().stream(), apiClient::deleteService, "delete", null);
		}
		
		private List<String> tagsToNames(Set<Tag> tags) {
			
			List<String> result = new ArrayList<>();
			
			if(tags == null || tags.isEmpty()) {
				return result;
			}
			
			for(Tag tag : tags) {
				result.add(tag.getName());
			}
			
			return result;
			
		}

		private XmlTestScriptShortReport waitForScriptExecutionFinish(Script script, int id) throws InterruptedException {

			logger.debug("Got id {}. Monitoring status...", id);

			ScriptExecutionStatistics statistics = script.getStatistics();

			while(true) {

				try {

					if(!running) {
						logger.info("Invoking stop");
						apiClient.stopTestScriptRun(id);
						return null;
					}

					Thread.sleep(1000);

					XmlTestScriptShortReport response = apiClient.getTestScriptRunShortReport(id);

					if(response != null) {
						logger.debug("Status: {}", response);
					}

					statistics.setNumPassed(response.getPassed());
					statistics.setNumConditionallyPassed(response.getConditionallyPassed());
					statistics.setNumFailed(response.getFailed());

					statistics.setStatus(response.getStatus());

					if (!response.isLocked()) {

						script.setFinished(true);

						script.setCause(TestScriptDescription.getCauseMessage(response.getCause()));

						statistics.setTotal(response.getTotal());

						return response;

					}

				} catch (InterruptedException e) {
					throw e;
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
					throw new RuntimeException("Could not perform status check call", e); // TODO: replace with checked exception

				}

			}

		}

		private void executeOnFinish(StatusType statusType, Set<BigButtonAction> actionsToExecute, String subSubject, IBBActionExecutor... parameters) {
			logger.info("executeOnFinish was triggered");
			for (BigButtonAction action : actionsToExecute) {
				try {
					actions.get(action).accept(statusType, subSubject, parameters);
				} catch (Exception e) {
					logger.error("Exception during BBAction execute", e.getMessage());
				}
			}
		}

		private void executeOnFinishScript(Script script){
			logger.info("executeOnFinishScript was triggered");
			StatusType statusType = script.getStatusType();
			executeOnFinish(statusType, script.getActions(), StringUtils.join("Script ", statusType.name().toLowerCase()), currentList, script);
		}


		private void executeOnFinishScriptList(ScriptList scriptList){
			logger.info("executeOnFinishScriptList was triggered");
			StatusType statusType = scriptList.getStatusType();
			executeOnFinish(statusType, scriptList.getActions(), StringUtils.join("Script list ", statusType.name().toLowerCase()), scriptList);
		}

        /**
         * Upload and start compile script on slave
         * @param script
         * @throws InterruptedException
         */
        private void compileScript(Script script) throws InterruptedException {
            //SfApiOptions scriptOptions = script.getApiOptions();

            SfApiOptions scriptOptions = library.getDefaultApiOptions();
            Optional<Globals> globalOptional = library.getGlobals();

            if(globalOptional.isPresent()){
                scriptOptions = scriptOptions.mergeOptions(globalOptional.get().getApiOptions());
            }

            scriptOptions = scriptOptions.mergeOptions(executor.getApiOptions())
                            .mergeOptions(currentList.getApiOptions())
                            .mergeOptions(script.getOriginalApiOptions());


            script.setApiOptions(scriptOptions);

            try {

                logger.debug("Uploading {}", script);

                try (InputStream matrixStream = BigButtonUtil.getStream(library.getRootFolder(),
                        script.getPath(), workspaceDispatcher)) {

                    XmlMatrixUploadResponse matrix = apiClient.uploadMatrix(matrixStream, FilenameUtils.getName(script.getPath()));

                    List<String> tags = tagsToNames(scriptOptions.getTags());

                    String staticVariables = null;

                    if(!scriptOptions.getStaticVariables().isEmpty()) {
                        staticVariables = mapper.writeValueAsString(scriptOptions.getStaticVariables());
                    }

                    logger.debug("Starting {}; Tags: {}; Static vars: {}", script, tags, staticVariables);

                    String targetReportFolder = null;

                    if(library.getReportsFolder() != null) {
                        targetReportFolder = relativeListReportsFolder;
                    }

                    XmlTestscriptActionResponse response = apiClient.performMatrixAction((int)matrix.getId(),
                            "start",
                            scriptOptions.getRange(),
                            BB_ENVIRONMENT,
                            null,
                            0, // aml version is deprecated
                            scriptOptions.getContinueIfFailed(),
                            scriptOptions.getAutoStart(),
                            false,
                            scriptOptions.getIgnoreAskForContinue(),
                            scriptOptions.getRunNetDumper(),
                            scriptOptions.getSkipOptional(),
                            tags,  // tags
                            staticVariables,
                            targetReportFolder,
                            scriptOptions.getLanguage());

                    apiClient.compileTestScriptRun((int) response.getId());
                    script.setRemoteInformation(new ScriptRemoteInformation(response.getId(), matrix.getId()));
                }
            }
            catch (Exception e) {
                logger.error(e.getMessage(), e);
                throw new RuntimeException("Could not perform api call", e); // TODO: replace with checked exception

            }
        }

        /**
         * Wait while script will be compiled
         * @param testScriptId
         * @throws InterruptedException
         */
        private void waitForCompileTestScript(int testScriptId) throws InterruptedException{
            try {
                ScriptState scriptState;
                while (!(scriptState = apiClient.getTestScriptRunInfo(testScriptId).getScriptState()).isTerminateState()
                    && scriptState != ScriptState.READY) {
                    Thread.sleep(1000);
                }
            } catch (APICallException | APIResponseException e) {
                throw new EPSCommonException("Could not get scripts state for test script with id + {" + testScriptId + '}', e);
            }
        }

        /**
         * Send request to slave to start script
         * @param testScriptId
         * @throws InterruptedException
         */
        private void startScript(int testScriptId) throws InterruptedException {
            try {
                apiClient.runCompiledTestScript(testScriptId);
            } catch (APICallException | APIResponseException e) {
                throw new EPSCommonException("Can`t start test script with id [" + testScriptId + ']', e);
            }
        }

        /**
         * Wait while script will be executed, then update statistic and download report
         * @param script
         * @param scriptRunId
         * @param matrixId
         * @throws InterruptedException
         */
        private void waitForExecutingTestScriptAndCollectStatisticAndReport(Script script, long scriptRunId, long matrixId) throws InterruptedException {
            try {
                XmlTestScriptShortReport executionResult = waitForScriptExecutionFinish(script, (int)scriptRunId);
                ListExecutionStatistics statistics = currentList.getExecutionStatistics();

                if (executionResult == null
                        || executionResult.getFailed() != 0
                        || executionResult.getStatus().equals(ScriptStatus.INIT_FAILED.name())
                        || executionResult.getStatus().equals(ScriptStatus.RUN_FAILED.name())) {

                    statistics.incNumFailed();

                } else if (executionResult.getConditionallyPassed() != 0) {

                    statistics.incNumConditionallyPassed();

                } else {

                    statistics.incNumPassed();
                    statistics.setSuccessPercent(
                            RegressionRunnerUtils.calcPercent(statistics.getNumPassed(), currentList.getScripts().size()));
                }

                boolean reportDownloadNeeded = false;

                if (executionResult != null) {

                    //downloadReport(scriptRunId);

                    reportDownloadNeeded = true;

                }
                executeOnFinishScript(script);
                monitor.scriptExecuted(script, (int)scriptRunId, executor, relativeListReportsFolder,
                        reportDownloadNeeded, isMasterSf);

                apiClient.deleteMatrix((int)matrixId);
            } catch (InterruptedException e) {
                throw  e;
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                throw new RuntimeException("Could not perform api call", e); // TODO: replace with checked exception
            }
        }

        /**
         * Check BB is not running, or wait if BB is on pause
         * @return True - if BB is not running, False - if BB is running
         * @throws InterruptedException
         */
        private boolean checkIsNotRun() throws InterruptedException {
            if (!running) {
                return true;
            }

            if (runner.isPause()) {
                state = ExecutorState.Paused;

                while (runner.isPause()) {
                    Thread.sleep(1000);
                }

                state = ExecutorState.Executing;
            }

            return false;
        }

        private void runList() throws InterruptedException {

            try {
                ListExecutionStatistics statistics = currentList.getExecutionStatistics();

                currentList.setStatus(ScriptListStatus.RUNNING);

                this.relativeListReportsFolder = createRelativeListRepotsFolder();

                for (int i = 0; i < currentList.getScripts().size(); i++) {

                    if (checkIsNotRun()){
                        return;
                    }

                    compileScript(currentList.getScripts().get(i));
                }

                for(int i =0; i < currentList.getScripts().size(); i++) {//Script script : currentList.getScripts()) {

                    if (checkIsNotRun()){
                        return;
                    }

                    Script script = currentList.getScripts().get(i);

                    currentList.setCurrentScript(script);

                    if (!currentList.getStatus().isSkipped() && script.getRemoteInformation() != null) {

                        waitForCompileTestScript((int) script.getRemoteInformation().getTestScriptId());

                        startScript((int) script.getRemoteInformation().getTestScriptId());

                        waitForExecutingTestScriptAndCollectStatisticAndReport(script, script.getRemoteInformation().getTestScriptId(), script.getRemoteInformation().getMatrixId());
                    } else {
                        script.getStatistics().setStatus("SKIPPED");
                        script.setFinished(true);
                        statistics.incNumFailed();
                        try {
                            monitor.scriptExecuted(script, 0, executor, relativeListReportsFolder, false, isMasterSf);
                        } catch (Exception e) {
                            logger.error("scriptExecuted error", e);
                        }
                    }

                    statistics.setExecutionPercent(
                            RegressionRunnerUtils.calcPercent(i + 1, currentList.getScripts().size()));
                }

            } catch (InterruptedException e){
                throw  e;
            } finally {

                currentList.setCurrentScript(null);

            }

        }

		private String createRelativeListRepotsFolder() {
            return library.getReportsFolder() == null ? null : Paths.get(library.getReportsFolder(), currentList.getName()).toString();
        }
		
        @Override
		public void run() {
			
			logger.info("Executor started");
			
			long idleTime = 0;
			long msTimeout = executor.getTimeout() * 1000;
			
			RuntimeException possibleEx = null;

            while(running) {
				
				try {
					
					state = ExecutorState.Waiting;
					
					if(executorReady == null || !executorReady) {
						
						Thread.sleep(500l);
						continue;
						
					}
					
					currentList = listsQueue.poll(executor.getName(), 400, TimeUnit.MILLISECONDS); // Take list from queue
					
					if(currentList == null) {
						continue;
					}
					
					logger.info("List {} taken", currentList);
					
					monitor.listTaken(currentList, executor);
					
					state = ExecutorState.Preparing;

                    // Cleanup don't needed, we use replacing of services
                    // cleanEnvironment();

                    boolean initSucess = beforeListRun(); // Prepare slave SF

                    if(initSucess) {

						state = ExecutorState.Executing;
						
						if (msTimeout == 0) {
							try {
								runList(); // Execute scripts
								
							} catch (RuntimeException e) {
							    executorReady = false;
                                toWarnState(e);
                                transferScriptListToAnotherNode();
							}
							
						} else {
						
							while (idleTime < msTimeout) {
								try {
									
									runList(); // Execute scripts
									break;
									
								} catch (RuntimeException e) {
									
									possibleEx = e;
									
									Thread.sleep(500l);
									idleTime += 500;
									
									logger.error("Timeout: {}", idleTime);
								} 
							}
							
							if (idleTime >= msTimeout) {
								
								logger.error("Timeout", possibleEx);
								
								if (currentList != null) {
									
									try {
										
										if (executor.getDaemon() != null) {
											if (restartNode()) {
												
												logger.debug("Request to restart node {} is passed", executor.getDaemon().getName());
												
												transferScriptListToAnotherNode();
												
												state = ExecutorState.Inactive;
												
											} else {
												
												logger.debug("Request to restart node {} is failed", executor.getDaemon().getName());
												toErrorState(possibleEx);
												break;
											}
										} else {
											toErrorState(new RuntimeException("Timeout: " + idleTime, possibleEx));
											break;
										}
									
									} catch (IOException e) {
										
										logger.debug("Connection with daemon {} is refused. {}", executor.getDaemon().getName(), executor.getDaemon().getHttpUrl());
										toErrorState(possibleEx);
										break;
									}
								} else {
									toErrorState(new RuntimeException("Timeout: " + idleTime, possibleEx));
									break;
								}
							}
						}
                    } else {
                        transferScriptListToAnotherNode();
					}
					
					state = ExecutorState.Cleaning;
					
					idleTime = 0;

                    cleanScriptListEnvironment(); // Cleanup

				} catch (InterruptedException e) { //TODO ?
					
					logger.error("Interrupted", e);
					break;
					
				} catch(Throwable t) {
					
					logger.error(t.getMessage(), t);
					toErrorState(t);
					break; // Unknown exception. Executor becomes inactive 
					
				} finally {

                    scriptListServicesUploaded.clear();

					if(currentList != null) {
						monitor.listExecuted(executor, currentList);
						currentList.setStatus(ScriptListStatus.EXECUTED);
                        executeOnFinishScriptList(currentList);
						currentList = null;
					}
				}
			}
			
			try {
				apiClient.close();
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			}

            if(state != ExecutorState.Error) {
				
				state = ExecutorState.Inactive;
				
			}
			
			logger.info("Executor finished");
		}
		
		private boolean restartNode() throws IOException {
			
			state = ExecutorState.Restarting;

            HttpURLConnection con = (HttpURLConnection)new URL(executor.getDaemon().getHttpUrl() + "/" +
                    "restart?alias=" + executor.getDaemon().getName()).openConnection();
			
			con.setConnectTimeout(executor.getDaemon().getTimeout() * 1000);
			
			con.setRequestMethod("GET");
			if (con.getResponseCode() == 200) {
				return true;
			}
			
			return false;
		}

		private void transferScriptListToAnotherNode() {
			
            if (currentList.getExecutor() != null) {
                for (Script script : currentList.getScripts()) {

                    if (!script.isFinished()) {
                        script.getStatistics().setStatus("CONNECTION_FAILED");
                        script.getStatistics().setNumFailed(script.getStatistics().getNumFailed() + 1);
                        try {
                            monitor.scriptExecuted(script, 0, executor, relativeListReportsFolder, false, isMasterSf);
                        } catch (Exception e) {
                            logger.error("scriptExecuted error", e);
                        }
                        currentList.getExecutionStatistics().incNumFailed();
                    }
                }
            } else {
    			ScriptList newScriptList = new ScriptList(currentList.getName(), null, currentList.getServiceLists(),
                        currentList.getApiOptions(), currentList.getPriority(), currentList.getLineNumber(), currentList.getVariableSet());
			
			boolean firstUnfinished = true;
			
			Iterator<Script> iter = currentList.getScripts().iterator();
			
			while (iter.hasNext()) {
				
				Script script = iter.next();
				
				if (!script.isFinished()) {
					
					if (firstUnfinished) {
						firstUnfinished = false;
						script.getStatistics().setStatus("CONNECTION_FAILED");
						script.getStatistics().setNumFailed(script.getStatistics().getNumFailed() + 1);
						try {
                            monitor.scriptExecuted(script, 0, executor, relativeListReportsFolder, false, isMasterSf);
						} catch (Exception e) {
							logger.error("scriptExecuted error", e);
						}
                        currentList.getExecutionStatistics().incNumFailed();
					} else {
						newScriptList.addNested(script);
						iter.remove();
					}
				}
			}
			
			monitor.listEnqueued(newScriptList);
			monitor.decreaseTotalScriptCount(newScriptList.getScripts().size());
			listsQueue.add(newScriptList);
            }
			monitor.listExecuted(executor, currentList);
			currentList = null;
		}

	}

	public Executor getExecutor() {
		return executor;
	}

    public List<String> getErrorText() {
        return new ArrayList<>(errorText);
	}

	public Boolean getExecutorReady() {
		return executorReady;
	}

	public void setExecutorReady(Boolean executorReady) {
		this.executorReady = executorReady;
	}

    private void interrupt(StatusType status, String subSubject, IBBActionExecutor... parameters) {
        logger.info("interrupt was triggered");

        runner.interrupt("Interrupted by BB action");

        if (mailService.isConnected()) {
            mailService.send(EMailUtil.createSubSubject(settings.getEmailSubject(), "Big Button interrupt"), null,
                             EMailUtil.createHtmlText("Big Button interrupted by action."), settings.getFinalRecipients(status), null);
        }
    }

    private void skipList(StatusType status, String subSubject, IBBActionExecutor... parameters) {
        logger.info("skipList was triggered");
        currentList.setStatus(ScriptListStatus.SKIPPED);
    }

    private void sendEmail(StatusType status, String subSubject, IBBActionExecutor... parameters) {
        logger.info("sendEmail was triggered ");
        if (mailService.isConnected()) {
            StringBuilder htmlBody = new StringBuilder();

            htmlBody.append(EMailUtil.createHtmlText(settings.getEmailPrefix()));
            htmlBody.append(EMailUtil.BR);

            for (IBBActionExecutor parameter : parameters) {
                htmlBody.append(parameter.toHtmlTable());
            }

            htmlBody.append(EMailUtil.createHtmlText(settings.getEmailPostfix()));

            //Used html body for color text
            mailService.send(EMailUtil.createSubSubject(settings.getEmailSubject(), subSubject), null, htmlBody.toString(),
                             settings.getFinalRecipients(status), null);
        }
	}

    private void checkIsMaster() {
        try {
            String executorUID = apiClient.getUID();
            isMasterSf = sfInstanceInfo.getUID().equals(executorUID);
        } catch (APICallException | APIResponseException e) {
            logger.error("Could not get uid for [{}] executor", executor.getName(), e);
            toErrorState(e);
        }
    }
    private void stopRunningTestScripts() {
        try {
            // currentList may be null when BB end to running all test scripts.
            // When BB execute action Interrupt currentList is not null
            if (currentList != null) {
                currentList
                        .getScripts()
                        .stream()
                        .filter(script -> !script.isFinished())
                        .forEach(script -> {
                            try {
                                apiClient.stopTestScriptRun((int)script.getRemoteInformation().getTestScriptId());
                                apiClient.deleteTestScriptRun((int) script.getRemoteInformation().getTestScriptId());
                                apiClient.deleteMatrix((int)script.getRemoteInformation().getMatrixId());
                            } catch (APICallException | APIResponseException e) {
                                throw new EPSCommonException("Could not perform api call", e);
                            }
                        });
            }
        } catch (RuntimeException e){
            toErrorState(e);
        }
    }


}
