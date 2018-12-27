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

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;

import com.exactpro.sf.SerializeUtil;
import com.exactpro.sf.aml.AMLException;
import com.exactpro.sf.aml.generator.AlertCollector;
import com.exactpro.sf.aml.generator.AggregateAlert;
import com.exactpro.sf.aml.generator.AlertType;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.embedded.statistics.entities.Tag;


public class TestScriptDescription implements Closeable
{
	private final static AtomicLong scriptIdCounter = new AtomicLong();

	private final String originalMatrixPath;   // relative (to MATRIX folder) path to matrix
	private final String originalSettingsPath; // relative (to CFG folder) path to 'script.xml'
	private final String matrixPath;   // relative (to REPORT folder) path to matrix (copy)
	private final String settingsPath; // relative (to REPORT folder) path to script.xml (copy)

	private final String workFolder; // relative (to REPORT folder) path to work folder (where report is placed)
	private final String subFolder;

	private String className;
	private ClassLoader classLoader;
	private ScriptState state;
	private ScriptStatus status;
	private final Date enqueueTimestamp;
	private final String description;
	private Throwable cause;
	private AlertCollector alertCollector;
	private final long id;

	private final String matrixFileName;
	private final String range;
	private final boolean continueOnFailed;
	private final boolean autoStart;
	private final boolean autoRun;
	private final boolean runNetDumper;
    private final boolean skipOptional;
    private SailfishURI languageURI;
    private String progress = "";

	//Its dynamic part of running test script
	private ScriptContext context;

	private final IScriptRunListener listener;

	private final boolean suppressAskForContinue; // FIXME: rename to ignoreAscForContinue?

	private final String encoding;

	private long startedTime;

	private long finishedTime;

	private String pauseReason;

	private long pauseTimeout;

	private final String username;
	@Deprecated // description.getContext().getServiceList().toString()
	private String services;

	private final List<Tag> tags;

	// this map will be used to override values of specified variables from matrix
	private final Map<String, String> staticVariables;

    private boolean locked;

    private Logger scriptLogger;

    private ScriptSettings scriptSettings;

    private long matrixRunId;

    private boolean canceled = false;

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    private final Lock readLock = readWriteLock.readLock();

    private final Lock writeLock = readWriteLock.writeLock();

    /**
     * Constructor for restoring test script description from test script storage
     * @param listener
     * @param timestamp
     * @param workFolder
     * @param matrixFileName
     * @param range
     * @param autoStart
     * @param username
     */
    public TestScriptDescription(IScriptRunListener listener, Date timestamp,
                                 String workFolder,
                                 String matrixFileName,
                                 String range,
                                 boolean autoStart,
                                 String username) {
        this(listener, timestamp, false,
                null, null, null, null,
                workFolder, null, matrixFileName, range, null,
                false, false, autoStart, false, false, false,
                null, username, null, null);
    }

    public TestScriptDescription(IScriptRunListener listener, Date timestamp,
                                 String originalMatrixPath,
                                 String originalSettingsPath,
                                 String matrixPath,
                                 String settingsPath,
                                 String workFolder,
                                 String subFolder,
                                 String matrixFileName,
                                 String range,
                                 String description,
                                 boolean continueOnFailed,
                                 boolean autoRun,
                                 boolean autoStart,
                                 boolean runNetDumper,
                                 boolean suppressAskForContinue,
                                 boolean skipOptional,
                                 String encoding,
                                 String username,
                                 List<Tag> tags,
                                 Map<String, String> staticVariables) {
        this(listener, timestamp, true,
                originalMatrixPath, originalSettingsPath, matrixPath,
                settingsPath, workFolder, subFolder, matrixFileName,
                range, description,
                continueOnFailed, autoRun, autoStart, runNetDumper, suppressAskForContinue, skipOptional,
                encoding, username, tags, staticVariables);
    }

    public TestScriptDescription(IScriptRunListener listener,
                                 Date enqueueTimestamp,
                                 boolean locked,
                                 String originalMatrixPath,
                                 String originalSettingsPath,
                                 String matrixPath,
                                 String settingsPath,
                                 String workFolder,
                                 String subFolder,
                                 String matrixFileName,
                                 String range,
                                 String description,
                                 boolean continueOnFailed,
                                 boolean autoRun,
                                 boolean autoStart,
                                 boolean runNetDumper,
                                 boolean suppressAskForContinue,
                                 boolean skipOptional,
                                 String encoding,
                                 String username,
                                 List<Tag> tags,
                                 Map<String, String> staticVariables) {
        this.id = scriptIdCounter.incrementAndGet();
        this.listener = listener;
        this.enqueueTimestamp = enqueueTimestamp;
        this.locked = locked;
        this.originalMatrixPath = originalMatrixPath;
        this.originalSettingsPath = originalSettingsPath;
        this.matrixPath = matrixPath;
        this.settingsPath = settingsPath;
        this.workFolder = workFolder;
        this.subFolder = subFolder;
        this.matrixFileName = matrixFileName;
        this.range = range;
        this.description = description;
        this.continueOnFailed = continueOnFailed;
        this.autoRun = autoRun;
        this.autoStart = autoStart;
        this.runNetDumper = runNetDumper;
        this.suppressAskForContinue = suppressAskForContinue;
        this.skipOptional = skipOptional;
        this.encoding = encoding;
        this.username = username;
        this.tags = tags;
        this.staticVariables = staticVariables;

    }

    public enum ScriptState
	{
		INITIAL(0, false),
		PREPARING(1, false),
		READY(2, false),
		RUNNING(3, false),
		FINISHED(4, true),
		CANCELED(7, true),
		PENDING(8, false),
		PAUSED(9, false);

		private int weight;
		private boolean terminate;

		ScriptState(int weight, boolean terminate) {
			this.weight = weight;
			this.terminate = terminate;
		}

		public int getWeight() {
			return weight;
		}

        public boolean isTerminateState() {
            return terminate;
        }
    }

    public enum ScriptStatus {

        INIT_FAILED("INIT FAILED", 0), // initialization failed
        INTERRUPTED("INTERRUPTED", 1), // script was interrupted
        EXECUTED("EXECUTED", 2),
        NOT_STARTED("NOT STARTED", 3),
        NONE("NONE", 4),
        CANCELED("CANCELED", 5),
        RUN_FAILED("RUN FAILED", 6);

        private String title;
		private int weight;

        private ScriptStatus(String title, int weight) {
            this.title = title;
			this.weight = weight;
		}

        public String getTitle() {
            return this.title;
        }

		public int getWeight() {
			return weight;
		}
	}

    public static void resetScriptIdCounter(){
        scriptIdCounter.set(0);
    }

	public long getId() {
		return id;
	}

	/**
	 * relative (to MATRIX folder) path to matrix
	 */
    public String getOriginalMatrixPath() {
        return originalMatrixPath;
    }

	/**
	 * relative (to CFG folder) path to 'script.xml'
	 */
    public String getOriginalSettingsPath() {
        return originalSettingsPath;
    }

	public String getClassName() {
	    try {
	        readLock.lock();
            return className;
        } finally {
	        readLock.unlock();
        }
	}

	public ClassLoader getClassLoader() {
	    try {
	        readLock.lock();
            return classLoader;
        } finally {
	        readLock.unlock();
        }
	}

    public Logger getScriptLogger() {
	    try {
	        readLock.lock();
            return scriptLogger;
        } finally {
	        readLock.unlock();
        }
    }

    public ScriptSettings getScriptSettings() {
	    try {
	        readLock.lock();
            return scriptSettings;
        } finally {
	        readLock.unlock();
        }
    }

    public void setScriptConfiguration(String className, ClassLoader classLoader, Logger scriptLogger, ScriptSettings scriptSettings) {
        try {
            writeLock.lock();
            this.className = className;
            this.classLoader = classLoader;
            this.scriptLogger = scriptLogger;
            this.scriptSettings = scriptSettings;
        } finally {
            writeLock.unlock();
        }
    }

	public Date getTimestamp() {
		return enqueueTimestamp;
	}

	public ScriptState getState() {
	    try {
	        readLock.lock();
            return state;
        } finally {
	        readLock.unlock();
        }
	}

	public void setState(ScriptState scriptState) {
	    try {
	        writeLock.lock();
            this.state = scriptState;
        } finally {
	        writeLock.unlock();
        }
	}

	public ScriptStatus getStatus() {
	    try {
	        readLock.lock();
            return status;
        } finally {
	        readLock.unlock();
        }
	}

	public void setStatus(ScriptStatus scriptStatus) {
	    try {
	        writeLock.lock();
            this.status = scriptStatus;
        } finally {
	        writeLock.unlock();
        }
	}

    public String getDescription() {
        return description;
    }

	public Throwable getCause() {
	    try {
	        readLock.lock();
            return cause;
        } finally {
	        readLock.unlock();
        }
	}

	public void setCause(Throwable cause) {
	    try {
	        writeLock.lock();
            this.cause = cause;
        } finally {
	        writeLock.unlock();
        }
	}

	public AtomicLong getScriptIdCounter() {
		return scriptIdCounter;
	}

    public String getSubFolder() {
        return subFolder;
    }

	/**
	 * relative (to REPORT folder) path to work folder (where report is placed)
	 */
    public String getWorkFolder() {
        return workFolder;
    }

	/**
	 * relative (to REPORT folder) path to matrix (copy)
	 */
    public String getMatrixPath() {
        return matrixPath;
    }

	/**
	 * relative (to REPORT folder) path to script.xml (copy)
	 */
    public String getSettingsPath() {
        return settingsPath;
    }

    public String getMatrixFileName() {
        return this.matrixFileName;
    }

    public boolean getContinueOnFailed() {
        return this.continueOnFailed;
    }

    public boolean getAutoStart() {
        return this.autoStart;
    }

    public boolean getAutoRun() {
        return this.autoRun;
    }

	public SailfishURI getLanguageURI() {
	    try {
	        readLock.lock();
            return languageURI;
        } finally {
	        readLock.unlock();
        }
	}

	public void setLanguageURI(SailfishURI languageURI) {
	    try {
	        writeLock.lock();
            this.languageURI = languageURI;
        } finally {
	        writeLock.unlock();
        }
	}

	public String getProgress() {
	    try {
	        readLock.lock();
            return progress;
        } finally {
	        readLock.unlock();
        }
	}

	public void setProgress(int progress) {
	    try {
	        writeLock.lock();
            this.progress = String.valueOf(progress);
        } finally {
	        writeLock.unlock();
        }
	}

	public void setProgress(String progress) {
	    try {
	        writeLock.lock();
            this.progress = progress;
        } finally {
	        writeLock.unlock();
        }
	}

	public ScriptContext getContext() {
	    try {
	        readLock.lock();
            return context;
        } finally {
	        readLock.unlock();
        }
	}

	public void setContext(ScriptContext context) {
	    try {
	        writeLock.lock();
            this.context = context;
        } finally {
	        writeLock.unlock();
        }
	}

    public boolean isSuppressAskForContinue() {
        return suppressAskForContinue;
    }

    public String getEncoding() {
        return encoding;
    }

	public long getStartedTime() {
	    try {
	        readLock.lock();
            return startedTime;
        } finally {
	        readLock.unlock();
        }
	}

	public void setStartedTime(long startedTime) {
	    try {
	        writeLock.lock();
            this.startedTime = startedTime;
        } finally {
	        writeLock.unlock();
        }
	}

	public long getFinishedTime() {
	    try {
	        readLock.lock();
            return finishedTime;
        } finally {
	        readLock.unlock();
        }
	}

	public void setFinishedTime(long finishedTime) {
	    try {
	        writeLock.lock();
            this.finishedTime = finishedTime;
        } finally {
	        writeLock.unlock();
        }
	}

	public void notifyListener() {
		this.listener.onScriptRunEvent(this);
	}

	public String getPauseReason() {
        try {
            readLock.lock();
            return this.pauseReason;
        } finally {
            readLock.unlock();
        }
	}

	public long getPauseTimeout() {
	    try {
	        readLock.lock();
            return this.pauseTimeout;
        } finally {
	        readLock.unlock();
        }
	}

    public String getRange() {
        return range;
    }

    public String getUsername() {
        return username;
    }

	public String getServices() {
	    try {
	        readLock.lock();
	        if (context == null) {
                return null;
            } else {
	            return context.getServiceList().toString();
            }
        } finally {
	        readLock.unlock();
        }
	}

    public List<Tag> getTags() {
        return tags;
    }

    public Map<String, String> getStaticVariables() {
        return staticVariables;
    }

	public AlertCollector getAlertCollector() {
	    try {
	        readLock.lock();
            return alertCollector;
        } finally {
	        readLock.unlock();
        }
	}

	public void setAlertCollector(AlertCollector alertCollector) {
	    try {
	        writeLock.lock();
            this.alertCollector = alertCollector;
        } finally {
	        writeLock.unlock();
        }
	}

    @Override
    public String toString() {
        try {
            readLock.lock();
            return "TestScriptDescription [matrixPath=" + matrixPath
                    + ", settingsPath=" + settingsPath
                    + ", className=" + className + ", state=" + state
                    + ", status=" + status + ", timestamp=" + enqueueTimestamp
                    + ", description=" + description + ", cause=" + cause + ", id="
                    + id + ", matrixFileName=" + matrixFileName + ", range="
                    + range + ", continueOnFailed="
                    + continueOnFailed + ", autoStart=" + autoStart
                    + ", autoRun = " + autoRun
                    + ", language=" + languageURI
                    + ", suppressAskForContinue=" + suppressAskForContinue
                    + ", encoding=" + encoding + ", startedTime=" + startedTime
                    + ", finishedTime=" + finishedTime + "]";
        } finally {
            readLock.unlock();
        }
    }

    public boolean isRunNetDumper() {
        return runNetDumper;
    }

    public boolean isLocked() {
        try {
            readLock.lock();
            return locked;
        } finally {
            readLock.unlock();
        }
    }

    public void unlock() {
        try {
            writeLock.lock();
            locked = false;
        } finally {
            writeLock.unlock();
        }
    }

    public String getCauseMessage() {
        try {
            readLock.lock();
            return getCauseMessage(cause);
        } finally {
            readLock.unlock();
        }
    }

    public static String getCauseMessage(Throwable cause){
        if (cause == null || cause.getCause() == null) {
            return null;
        }

        Throwable causeCause = cause.getCause();

        if (causeCause instanceof AMLException) {
            AMLException e = (AMLException) causeCause;
            StringBuilder sb = new StringBuilder();

            if (e.getAlertCollector().getCount(AlertType.ERROR) > 0) {
                Collection<AggregateAlert> aggregatedAlerts = e.getAlertCollector().aggregate(AlertType.ERROR);
                int size = aggregatedAlerts.size();
                sb.append("Found ").append(size).append(" error(s) during preparing script:");
                for (AggregateAlert aggregatedAlert : aggregatedAlerts) {
                    sb.append(aggregatedAlert.toString()).append(System.lineSeparator());
                }
            } else {
                sb.append("Found unknown errors during preparing script.").append(System.lineSeparator()).append("Returned message is \"")
                        .append(e.getMessage()).append("\"");
            }
            return sb.toString();
        } else if (causeCause instanceof InvocationTargetException) {
            InvocationTargetException e = (InvocationTargetException) causeCause;
            Throwable target = e.getTargetException();
            if (target.getCause() != null) {
                return "Found error during running script: " + target.getCause().getMessage();
            } else {
                return "Found error during running script: " + target.getMessage();
            }
        } else {
            return causeCause.getMessage();
        }

    }

    public String getProblem(){
        try {
            readLock.lock();
            if (cause == null) {
                return null;
            }
            return cause.getMessage();
        } finally {
            readLock.unlock();
        }
    }

    public static String getCauseMessage(String encodedCause){
        if(encodedCause == null){
            return null;
        }

        Throwable cause = SerializeUtil.deserializeBase64Obj(encodedCause, Throwable.class);
        return getCauseMessage(cause);
    }

    public long getMatrixRunId() {
        try {
            readLock.lock();
            return matrixRunId;
        } finally {
            readLock.unlock();
        }
    }

    public void setMatrixRunId(long matrixRunId) {
        try {
            writeLock.lock();
            this.matrixRunId = matrixRunId;
        } finally {
            writeLock.unlock();
        }
    }

    public boolean isSkipOptional() {
        return skipOptional;
    }

    public boolean isSetCancelFlag() {
        try {
            readLock.lock();
            return canceled;
        } finally {
            readLock.unlock();
        }
    }

    public void setCancelFlag() {
        try {
            writeLock.lock();
            canceled = true;
        } finally {
            writeLock.unlock();
        }
    }

    public void scriptCanceled() {
        try {
            writeLock.lock();
            changeState(ScriptState.CANCELED, ScriptStatus.CANCELED);
        } finally {
            writeLock.unlock();
            notifyListener();
        }
    }

    public void scriptInitFailed(final Throwable cause) {
        try {
            writeLock.lock();
            changeState(ScriptState.FINISHED, ScriptStatus.INIT_FAILED);
            this.cause = cause;
        } finally {
            writeLock.unlock();
            notifyListener();
        }
    }

    public void scriptInitialized() {
        try {
            writeLock.lock();
            changeState(ScriptState.INITIAL, null);
        } finally {
            writeLock.unlock();
            notifyListener();
        }
    }

    public void scriptPending() {
        try {
            writeLock.lock();
            changeState(ScriptState.PENDING, null);
        } finally {
            writeLock.unlock();
            notifyListener();
        }
    }

    public void scriptPreparing() {
        try {
            writeLock.lock();
            changeState(ScriptState.PREPARING, null);
        } finally {
            writeLock.unlock();
            notifyListener();
        }
    }

    public void scriptReady() {
        try {
            writeLock.lock();
            changeState(ScriptState.READY, null);
        } finally {
            writeLock.unlock();
            notifyListener();
        }
    }

    public void scriptRan() {
        try {
            writeLock.lock();
            changeState(ScriptState.RUNNING, null);
        } finally {
            writeLock.unlock();
            notifyListener();
        }
    }

    public void scriptPaused(final String pauseReason, final long pauseTimeout) {
        try {
            writeLock.lock();
            if (changeState(ScriptState.PAUSED, null)) {
                this.pauseReason = pauseReason;
                this.pauseTimeout = pauseTimeout;
            }
        } finally {
            writeLock.unlock();
            notifyListener();
        }
    }

    public void scriptResume() {
        try {
            writeLock.lock();
            if (changeState(ScriptState.RUNNING, null)) {
                this.pauseReason = null;
                this.pauseTimeout = 0;
            }
        } finally {
            writeLock.unlock();
            notifyListener();
        }
    }

    public void scriptNotStarted() {
        try {
            writeLock.lock();
            changeState(ScriptState.FINISHED, ScriptStatus.NOT_STARTED);
        } finally {
            writeLock.unlock();
            notifyListener();
        }
    }

    public void scriptRunFailed(final Throwable cause) {
        try {
            writeLock.lock();
            changeState(ScriptState.FINISHED, ScriptStatus.RUN_FAILED);
            this.cause = cause;
        } finally {
            writeLock.unlock();
            notifyListener();
        }
    }

    public void scriptInterrupted() {
        try {
            writeLock.lock();
            changeState(ScriptState.FINISHED, ScriptStatus.INTERRUPTED);
        } finally {
            writeLock.unlock();
            notifyListener();
        }
    }

    public void scriptExecuted() {
        try {
            writeLock.lock();
            changeState(ScriptState.FINISHED, ScriptStatus.EXECUTED);
        } finally {
            writeLock.unlock();
            notifyListener();
        }
    }

    @Override
    public void close() throws IOException {
        try {
            writeLock.lock();

            // stop class loader
            if (this.classLoader != null && this.classLoader.getClass().isAssignableFrom(Closeable.class)) {
                ((Closeable) this.classLoader).close();
            }
            this.classLoader = null;

            // stop script logger
            if (this.scriptLogger != null) {
                org.apache.log4j.Logger.getLogger(this.scriptLogger.getName()).removeAllAppenders();
            }
            this.scriptLogger = null;
        } finally {
            writeLock.unlock();
        }
    }

    private boolean changeState(ScriptState state, ScriptStatus status) {
        if (this.state == null || !this.state.isTerminateState()) {
            this.state = state;
            if (status != null) {
                this.status = status;
            }
            return true;
        }
        return false;
    }
}
