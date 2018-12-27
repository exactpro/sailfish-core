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
import com.exactpro.sf.bigbutton.RegressionRunner;
import com.exactpro.sf.bigbutton.importing.ErrorType;
import com.exactpro.sf.bigbutton.importing.ImportError;
import com.exactpro.sf.bigbutton.importing.LibraryImportResult;
import com.exactpro.sf.bigbutton.library.Executor;
import com.exactpro.sf.bigbutton.library.Library;
import com.exactpro.sf.bigbutton.library.Script;
import com.exactpro.sf.bigbutton.library.ScriptList;
import com.exactpro.sf.scriptrunner.StatusType;
import com.exactpro.sf.embedded.statistics.entities.SfInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExecutionProgressMonitor {

	private static final Logger logger = LoggerFactory.getLogger(ExecutionProgressMonitor.class);

	private final List<ExecutorClient> allExecutors = new ArrayList<>();

	private List<ScriptList> enqueued = new ArrayList<>();

	private Map<Executor, List<ScriptList>> executed = new HashMap<>();

    private List<ScriptList> rejected = new ArrayList<>();

	private Map<Executor, ScriptList> running  = new HashMap<>();

	private long totalListsCount;

	private long executedListsCount;

	private long totalScriptsCount;

	private long executedScriptsCount;

	private final RegressionRunner runner;

	private volatile boolean finished = false;

	private BigButtonExecutionStatus status = BigButtonExecutionStatus.Inactive;

    private StatusType executionStatus = StatusType.NA;

	private String errorText;

    private List<String> warns = new ArrayList<>();

	private ExecutionReportExporter reportExporter;

	private int numExecutorsInError = 0;

	private ExecutorService taskExecutor;

	private BbExecutionStatistics executionStatistics = new BbExecutionStatistics();

	private ExecutorsStatusChecker statusChecker;

	private Library library;

    private Map<ErrorType, Set<ImportError>> importErrors;

	public ExecutionProgressMonitor(RegressionRunner runner) {

		this.runner = runner;

	}

	/*
	private List<ScriptList> getExecutedView(List<ScriptList> fullList, int limit) {

		int beginIndex = (fullList.size() > limit ? fullList.size() - 1 - limit : 0);

		return new ArrayList<>(fullList.subList(beginIndex, fullList.size()));

	}
	*/

	private void doTearDown() {

        writeReport();
        identifyExecutionStatus();

		if(this.reportExporter != null) {
			this.reportExporter.writeCompleted();
		}

		if(this.taskExecutor != null) {
			this.taskExecutor.shutdown();
		}

		if(this.statusChecker != null) {
			this.statusChecker.stop();
		}

		this.executionStatistics.setFinished(new Date());
		this.finished = true;
        this.importErrors = null;
        try {
            this.runner.close();
        } catch (Exception e) {
            logger.error("Error while closing RegressionRunner", e);
        }


	}

	public synchronized void preparing() {
		this.status = BigButtonExecutionStatus.Preparing;
	}

    public synchronized void ready(LibraryImportResult importResult) {

        if (this.status != BigButtonExecutionStatus.Error) {
            this.status = BigButtonExecutionStatus.Ready;
        }

        this.library = importResult.getLibrary();

        this.importErrors = new ConcurrentHashMap<>();

        this.importErrors.put(ErrorType.COMMON, importResult.getCommonErrors());
        this.importErrors.put(ErrorType.GLOBALS, importResult.getGlobalsErrors());
        this.importErrors.put(ErrorType.EXECUTOR, importResult.getExecutorErrors());
        this.importErrors.put(ErrorType.SCRIPTLIST, importResult.getScriptListErrors());

		this.statusChecker = new ExecutorsStatusChecker();

		Thread statusCheckerThread = new Thread(this.statusChecker, "BB slaves status checker");
		statusCheckerThread.setDaemon(true);
		statusCheckerThread.start();

	}

	public synchronized void started() {

		try {

			this.executionStatistics.setStarted(new Date());
			this.taskExecutor = Executors.newFixedThreadPool(this.allExecutors.size());
			this.reportExporter = new ExecutionReportExporter();

		} catch (IOException e) {
			throw new RuntimeException("Report exporter could not be created", e);
		}

		this.status = BigButtonExecutionStatus.Running;
	}

    public synchronized void error(String text) {
        logger.error("BigButton execution error:" + text);
        this.status = BigButtonExecutionStatus.Error;
        this.errorText = text;

        doTearDown();
	}

    public synchronized void warn(String text) {
        this.warns.add(text);
    }

	public synchronized void listEnqueued(ScriptList list) {

		this.enqueued.add(list);

		this.totalListsCount++;

		this.totalScriptsCount += list.getScripts().size();

	}

	public synchronized void decreaseTotalScriptCount(int count) {

		this.totalScriptsCount -= count;

	}

	public synchronized void listTaken(ScriptList list, Executor executor) {

		for(int i = enqueued.size() -1; i >= 0; i--) {

			if(this.enqueued.get(i).equals(list) ) {

				this.enqueued.remove(i);

				this.running.put(executor, list);

				return;

			}

		}

		throw new IllegalStateException("Taken " + list + " not found in queue!");

	}

	public synchronized void listExecuted(Executor executor, ScriptList list) {

		this.running.remove(executor);

		if(!this.executed.containsKey(executor)) {
			this.executed.put(executor, new ArrayList<ScriptList>());
		}

		this.executed.get(executor).add(0, list);

		this.executedListsCount++;

		if(this.finished) {
			return;
		}

		if(this.executedListsCount == this.totalListsCount) {
			this.status = BigButtonExecutionStatus.DownloadingReports;
			//doTearDown();
			this.taskExecutor.submit(new DownloadsCompleteTask(this));

		}

	}

	private List<ScriptList> getLimitedListView(List<ScriptList> fullList, int viewLimit) {

		List<ScriptList> enqueuedView = new ArrayList<>();

		int stopIndex = fullList.size() < viewLimit ? fullList.size(): viewLimit;

		for(int i = 0; i < stopIndex; i++) {

			enqueuedView.add(fullList.get(i));

		}

		return enqueuedView;

	}

	public synchronized ProgressView getCurrentProgressView(int inQueueLimit, int outQueueLimit) {

		ProgressView result = new ProgressView();

		List<ScriptList> enqueuedView = getLimitedListView(enqueued, inQueueLimit);
        List<ScriptList> rejectedView = getLimitedListView(rejected, inQueueLimit - enqueuedView.size() + 1);

		result.setEnqueued(enqueuedView);
		result.setNumInQueue(enqueued.size() - enqueuedView.size());
        result.setRejected(rejectedView);
        result.setNumRejected(rejected.size() - rejectedView.size());

		Map<Executor, List<ScriptList>> executedView = new HashMap<>();
		Map<Executor, Long> executedSizes = new HashMap<>();

		for(Map.Entry<Executor, List<ScriptList>> entry : this.executed.entrySet()) {

			List<ScriptList> fullList = entry.getValue();
			List<ScriptList> listView = getLimitedListView(fullList, outQueueLimit);
			long numInQueue = fullList.size() - listView.size();

			executedView.put(entry.getKey(), listView);
			executedSizes.put(entry.getKey(), numInQueue);

		}

		result.setExecuted(executedView);
		result.setNumExecuted(executedSizes);

		result.setRunning(new HashMap<>(this.running));

		result.setCurrentTotalProgressPercent(
				RegressionRunnerUtils.calcPercent(executedScriptsCount, totalScriptsCount));

		result.setAllExecutors(allExecutors);

		result.setFinished(finished);
		result.setStatus(status);
        result.setExecutionStatus(executionStatus);

		result.setErrorText(this.errorText);
        result.setWarns(this.warns);

		if(library != null) {
			result.setLibraryFileName(library.getDescriptorFileName());
		}
		result.setLibrary(library);

        result.setImportErrors(this.importErrors);

		if(this.finished && this.reportExporter != null) {
			result.setReportFile(this.reportExporter.getFile());
		}

		if(this.finished) {

			result.setExecutionStatistics(new BbExecutionStatistics(this.executionStatistics));

		}

		return result;

	}

    public synchronized void executorClientEncounteredError(ExecutorClient client) {

		this.numExecutorsInError++;

		if(numExecutorsInError == this.allExecutors.size()) {
            error("All executors gone to 'Error' state");
		}

	}

    public synchronized void interrupt(String message) {

		if(this.status == BigButtonExecutionStatus.Inactive
				|| this.status == BigButtonExecutionStatus.Finished) {

			return;

		}

        error(message);

	}

    public synchronized void runPaused() {
	    this.status = BigButtonExecutionStatus.Pause;
    }

    public synchronized void resumeRun(){
	    this.status = BigButtonExecutionStatus.Running;
    }

	public synchronized void reportsProcessingFinished() {
        doTearDown();

        this.status = BigButtonExecutionStatus.Finished;
	}

	public synchronized void scriptExecuted(Script script, int scriptRunId, Executor executor, String reportsFolder,
			 boolean downloadNeded) throws Exception {

		this.executedScriptsCount++;

        ScriptExecutionStatistics statistics = script.getStatistics();

        this.executionStatistics.incNumPassedTcs(statistics.getNumPassed());
        this.executionStatistics.incNumCondPassedTcs(statistics.getNumConditionallyPassed());
        this.executionStatistics.incNumFailedTcs(statistics.getNumFailed());

        if (statistics.isExecutionFailed()) {
			this.executionStatistics.incNumInitFailed();
		}

		if(this.finished) {
			return;
		}

        SFAPIClient apiClient = null;

        apiClient = new SFAPIClient(URI.create(executor.getHttpUrl() + "/sfapi").normalize().toString());

        SfInstance currentSfInstance = this.runner.getCurrentSfInstance();
        Long sfCurrentID = currentSfInstance == null ? null : currentSfInstance.getId();

        ReportDownloadTask task = new ReportDownloadTask(this.runner.getWorkspaceDispatcher(), scriptRunId, reportsFolder,
                apiClient, downloadNeded, sfCurrentID);

        taskExecutor.submit(task);

	}

    public synchronized void rejectScriptList(ScriptList list) {
        ListExecutionStatistics statistics = list.getExecutionStatistics();
        statistics.setRejected(true);
        for (Script script : list.getScripts()) {
            script.getStatistics().setStatus("REJECTED");
        }
        this.rejected.add(list);
    }

	public List<ExecutorClient> getAllExecutors() {
		return allExecutors;
	}

    private void writeReport() {
        try {

            writeCollection(rejected);

            for(List<ScriptList> scriptLists : executed.values()){
                writeCollection(scriptLists);
            }

            writeCollection(running.values());

            writeCollection(enqueued);

        } catch (IOException e) {
            throw new RuntimeException("Write to report failed", e);
        }
    }

    private void writeCollection(Collection<ScriptList> scriptLists) throws IOException {
        for (ScriptList list : scriptLists) {
            reportExporter.writeList(list);
        }
    }

    private void identifyExecutionStatus() {
        if (errorText != null) {
            return;
        }

        if (executionStatistics.getNumFailedTcs() > 0 || executionStatistics.getNumInitFailed() > 0) {
            executionStatus = StatusType.FAILED;
        } else if (executionStatistics.getNumCondPassedTcs() > 0) {
            executionStatus = StatusType.CONDITIONALLY_PASSED;
        } else {
            executionStatus = StatusType.PASSED;
        }
    }

	private class ExecutorsStatusChecker implements Runnable {

		private volatile boolean running = true;

		private static final long CHECK_INTERVAL = 5000l;

		public void stop() {
			this.running = false;
		}

		private boolean pingUrl(URI url, int timeout) {

		    try {

		        HttpURLConnection connection = (HttpURLConnection) url.toURL().openConnection();
		        connection.setConnectTimeout(timeout);
		        connection.setReadTimeout(timeout);
		        connection.setRequestMethod("HEAD");
		        int responseCode = connection.getResponseCode();
		        return (200 <= responseCode && responseCode <= 399);

		    } catch (IOException e) {
		        return false;
		    }

		}

		private void checkAndSetCurrentStatus(ExecutorClient client) {

			boolean available = pingUrl(URI.create(client.getExecutor().getHttpUrl() + "/sfapi/testscriptruns").normalize(), 3000);
			
			client.setExecutorReady(available);
			
		}
		
		@Override
		public void run() {
			
			while(running) {
				
				List<ExecutorClient> exClients = getAllExecutors();
				
				if(exClients != null) {
				
					for(ExecutorClient client : exClients) {
						
						checkAndSetCurrentStatus(client);
						
					}
				
				}
				
				try {
					
					Thread.sleep(CHECK_INTERVAL);
					
				} catch (InterruptedException e) {
					logger.error("Interrupted", e);
					break;
				}
				
			}
			
		}
		
	}
	
}
