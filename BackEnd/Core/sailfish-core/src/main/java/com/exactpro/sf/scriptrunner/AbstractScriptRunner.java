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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.HTMLLayout;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.aml.AML;
import com.exactpro.sf.aml.AMLException;
import com.exactpro.sf.aml.AMLSettings;
import com.exactpro.sf.aml.IPreprocessor;
import com.exactpro.sf.aml.IValidator;
import com.exactpro.sf.aml.generator.Alert;
import com.exactpro.sf.aml.generator.AlertType;
import com.exactpro.sf.aml.generator.GeneratedScript;
import com.exactpro.sf.aml.generator.JavaValidator;
import com.exactpro.sf.center.IDisposable;
import com.exactpro.sf.center.ISFContext;
import com.exactpro.sf.common.adapting.IAdapterManager;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.common.util.StringUtil;
import com.exactpro.sf.configuration.IDictionaryManager;
import com.exactpro.sf.configuration.IEnvironmentManager;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.configuration.workspace.WorkspaceSecurityException;
import com.exactpro.sf.configuration.workspace.WorkspaceStructureException;
import com.exactpro.sf.embedded.statistics.StatisticsService;
import com.exactpro.sf.embedded.statistics.entities.Tag;
import com.exactpro.sf.scriptrunner.EnvironmentSettings.ReportOutputFormat;
import com.exactpro.sf.scriptrunner.TestScriptDescription.ScriptState;
import com.exactpro.sf.scriptrunner.actionmanager.IActionManager;
import com.exactpro.sf.scriptrunner.impl.BroadcastScriptReport;
import com.exactpro.sf.scriptrunner.impl.ScriptReportWithLogs;
import com.exactpro.sf.scriptrunner.impl.StatisticScriptReport;
import com.exactpro.sf.scriptrunner.impl.XmlStreamReport;
import com.exactpro.sf.scriptrunner.impl.htmlreport.HtmlReport;
import com.exactpro.sf.scriptrunner.junit40.SFJUnitRunner;
import com.exactpro.sf.scriptrunner.languagemanager.ILanguageFactory;
import com.exactpro.sf.scriptrunner.languagemanager.LanguageManager;
import com.exactpro.sf.scriptrunner.reportbuilder.DefaultReportWriter;
import com.exactpro.sf.scriptrunner.services.IStaticServiceManager;
import com.exactpro.sf.scriptrunner.utilitymanager.IUtilityManager;
import com.exactpro.sf.storage.ITestScriptStorage;
import com.exactpro.sf.storage.ScriptRun;

public abstract class AbstractScriptRunner implements IDisposable {

    private final static Logger logger = LoggerFactory.getLogger(AbstractScriptRunner.class);

    protected final static String LS = System.getProperty("line.separator");
    protected final static String PACKAGE_NAME = "com.exactpro.sf.testscript";
    protected final static int DEFAULT_TIMEOUT = 1000;
    protected final static String forbiddenChars = " ?#;";

    protected final List<IScriptRunListener> listeners;
    protected final ScriptProgress.IScriptRunProgressListener progressListener;
    protected final IPauseListener pauseListener;
    protected final IScriptRunListener stateListener;

    protected final IWorkspaceDispatcher workspaceDispatcher;
    protected final IDictionaryManager dictionaryManager;
    protected final IActionManager actionManager;
    protected final IUtilityManager utilityManager;
    protected final LanguageManager languageManager;

    protected volatile State runnerState;
    protected volatile boolean isDisposing;
    protected volatile boolean shutdown = false;
    protected final SimpleDateFormat scriptFolderSuffix;
    protected final Map<Long, TestScriptDescription> testScripts;
    protected final Deque<Long> addedTestScripts;
    protected final List<Long> pendingTestScriptsToPrepare;
    protected final List<Long> pendingTestScriptsToRun;
    protected final Deque<Long> preparedTestScripts;

    protected Thread tScriptCompiler;
    protected Thread tScriptExecutor;
    protected final static Date startDate = new Date();

    private final ScriptRunnerSettings settings;
    private final PreprocessorLoader preprocessorLoader;
    private final ValidatorLoader validatorLoader;

    private StatisticsService statisticsService;
    private IEnvironmentManager environmentManager;
    private ITestScriptStorage testScriptStorage;
    private IAdapterManager adapterManager;
    private IStaticServiceManager staticServiceManager;
    private String compilerClassPath;

    protected enum State {
        INIT, DISPOSING, DISPOSED;
    }

    public AbstractScriptRunner(
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

        this.workspaceDispatcher = wd;
        this.dictionaryManager = dictionaryManager;
		this.actionManager = actionManager;
		this.utilityManager = utilityManager;
		this.languageManager = languageManager;
        this.preprocessorLoader = preprocessorLoader;
        this.validatorLoader = validatorLoader;
        this.settings = settings;
        this.statisticsService = statisticsService;
        this.environmentManager = environmentManager;
        this.testScriptStorage = testScriptStorage;
        this.adapterManager = adapterManager;
        this.staticServiceManager = staticServiceManager;
        this.compilerClassPath = compilerClassPath;

        listeners = new ArrayList<>();

        progressListener = new InternalProgressListener();

        pauseListener = new PauseListener();

        stateListener = new InternalTestScriptRunEventsListener();

        testScripts = new ConcurrentHashMap<>();

        addedTestScripts = new ArrayDeque<>();

        pendingTestScriptsToPrepare = new LinkedList<>();

        pendingTestScriptsToRun = new LinkedList<>();

        preparedTestScripts = new ArrayDeque<>();

        scriptFolderSuffix = new SimpleDateFormat("ddMMyyyy_HHmmss_SSS");

        isDisposing = false;

        runnerState = State.INIT;

    }

    public TestScriptDescription getTestScriptDescription(long id) {
        return this.testScripts.get(id);
    }

    public long enqueueScript(String scriptSettingsPath, String scriptMatrixPath, String matrixDescription,
                              String matrixFileName, String range,
                              boolean continueOnFailed, boolean autoStart,
                              boolean autoRun, boolean suppressAskForContinue,
                              boolean runNetDumper, boolean skipOptional,
                              SailfishURI languageURI, String fileEncoding, String environmentName,
            String userName, List<Tag> tags, Map<String, String> staticVariables, Collection<IScriptReport> userListeners, String subFolder, ISFContext sfContext) {
        try {
            // create directories:
            String workFolder = createAndGetDirectories(matrixFileName, scriptMatrixPath, subFolder);

            // copy files:
            WorkFolderPaths workFolderPaths = copyFilesToWorkFolder(matrixFileName, scriptMatrixPath, scriptSettingsPath, workFolder);

            TestScriptDescription scriptDescription = new TestScriptDescription(this.stateListener, new Date(),
                    scriptMatrixPath,
                    scriptSettingsPath,
                    workFolderPaths.getMatrixPath(),
                    workFolderPaths.getSettingsPath(),
                    workFolder,
                    subFolder,
                    matrixFileName,
                    range,
                    matrixDescription,
                    continueOnFailed,
                    autoRun,
                    autoStart,
                    runNetDumper,
                    suppressAskForContinue,
                    skipOptional,
                    fileEncoding,
                    userName,
                    tags,
                    staticVariables);

            scriptDescription.setStatus(TestScriptDescription.ScriptStatus.NONE);
            scriptDescription.setLanguageURI(languageURI);

            IScriptProgress scriptProgress = new ScriptProgress(scriptDescription.getId(), progressListener);
            DebugController debugModeControl = new DebugController(scriptDescription.getId(), pauseListener);

            String reportFolder = scriptDescription.getWorkFolder();

            List<IScriptReport> reportListeners = new ArrayList<>();
			// statistics:
            reportListeners.add(
					new StatisticScriptReport(statisticsService,
							scriptDescription.getWorkFolder(),
							scriptDescription.getTags()));

            List<IScriptReport> aggregateReportListeners = new ArrayList<>();
            // Write Report:
            File reportFile = workspaceDispatcher.createFile(FolderType.REPORT, true, reportFolder, "report.xml");
            // New xml report
            aggregateReportListeners.add(new XmlStreamReport(reportFile.getAbsolutePath()));
            // html report
            aggregateReportListeners.add(new HtmlReport(reportFolder, workspaceDispatcher, dictionaryManager, environmentManager.getEnvironmentSettings().getRelevantMessagesSortingMode()));
            // properties for gui
            aggregateReportListeners.add(new PropertiesReport(reportFolder, workspaceDispatcher, scriptDescription));

            BroadcastScriptReport aggregateReport = new BroadcastScriptReport(aggregateReportListeners);

            reportListeners.add(new ScriptReportWithLogs(aggregateReport, this.settings.getExcludedMessages()));
            // user-defined listeners
            if (userListeners != null) {
                reportListeners.addAll(userListeners);
            }

            ReportOutputFormat reportOutputFormat = environmentManager.getEnvironmentSettings().getReportOutputFormat();
            if(reportOutputFormat.isEnableZip()) {
                // NOTE: ZipReport must be latest
                reportListeners.add(new ZipReport(reportFolder, workspaceDispatcher, scriptDescription, reportOutputFormat));
            }
            
            BroadcastScriptReport report = new BroadcastScriptReport(reportListeners);

            ScriptContext context = new ScriptContext(sfContext, scriptProgress, report, debugModeControl, userName, scriptDescription.getId(), environmentName);

            scriptDescription.setContext(context);


            this.testScripts.put(scriptDescription.getId(), scriptDescription);

            if (scriptDescription.getAutoRun()) {
                scriptDescription.scriptInitialized();
                synchronized (addedTestScripts) {
                    this.addedTestScripts.add(scriptDescription.getId());
                }
            } else {
                scriptDescription.scriptPending();
                synchronized (pendingTestScriptsToPrepare) {
                    this.pendingTestScriptsToPrepare.add(scriptDescription.getId());
                }
            }

            logger.info("Test Script {} was added to the queue", scriptDescription.getId());

            return scriptDescription.getId();

        } catch (Throwable e) {
            logger.error("Error during script preparation", e);
            return -1; // FIXME: no way to determine reason
        }
    }

    public List<TestScriptDescription> removeAllTestScripts(boolean deleteOnDisk){
    	return removeTestScripts(deleteOnDisk, new ArrayList<>(testScripts.keySet()));
//        TestScriptDescription.resetScriptIdCounter();
    }

    public List<TestScriptDescription> removeTestScripts(boolean deleteOnDisk, List<Long> ids) {
        List<TestScriptDescription> toRemove = new ArrayList<>();
        for (Long id : ids) {
            TestScriptDescription descr = testScripts.get(id);
            if (descr == null) {
                continue;
            }
            if (!descr.isLocked()) {
                testScripts.remove(descr.getId());

                toRemove.add(descr);
            }
        }
        return testScriptStorage.remove(deleteOnDisk, toRemove);
    }

    public void testScriptsInitFromWD(){
        List<TestScriptDescription> testScriptDescriptionList = testScriptStorage.getTestScriptList();
        for (TestScriptDescription testScriptDescription : testScriptDescriptionList) {
            testScripts.put(testScriptDescription.getId(), testScriptDescription);
            testScriptDescription.notifyListener();
        }
    }

    public void stopAllScript(){
        for(TestScriptDescription descr: testScripts.values()){
            ScriptState state = descr.getState();
			if (!state.isTerminateState()) {
				stopScript(descr.getId());
            }
        }
    }

    public void pauseAllScript(){
        for(TestScriptDescription descr: testScripts.values()){
			ScriptState state = descr.getState();
			if (!state.isTerminateState() && state != ScriptState.PAUSED) {
                pauseScript(descr.getId());
            }
        }
    }

    public void resumeAllScript(){
        for(TestScriptDescription descr: testScripts.values()){
//            ScriptState state = descr.getState();
//            if(state == ScriptState.PAUSED) {
            resumeScript(descr.getId());
//            }
        }
    }

    public void stopScript(long id) {
        logger.info("stopScript({})", id);

        TestScriptDescription descr = testScripts.get(id);
        if (descr != null) {
            if (descr.isSetCancelFlag() ||
                    descr.getState().isTerminateState()) {
                logger.info("Script {} already was marked to cancel or already terminated", id);
                return;
            }
            descr.setCancelFlag();
        }

        // move script from pending to run queue to run queue
        // it will canceled in script executor thread
        synchronized (pendingTestScriptsToRun) {
            synchronized (preparedTestScripts) {
                if (tryToForceExecution(id, pendingTestScriptsToRun, preparedTestScripts)) {
                    return;
                }
            }
        }

        // move script from pending to compile queue to compile queue
        // it will canceled in script compiler thread
        synchronized (pendingTestScriptsToPrepare) {
            synchronized (addedTestScripts) {
                if (tryToForceExecution(id, pendingTestScriptsToPrepare, addedTestScripts)) {
                    return;
                }
            }
        }
    }

    private boolean tryToForceExecution(long id, List<Long> passive, Deque<Long> active) {
        if (passive.remove(id) ||
                active.remove(id)) {
            active.addFirst(id);
            logger.info("Script {} moved to active queue", id);
            return true;
        }
        return false;
    }

    public void resumeScript(long id) {
        TestScriptDescription descr = testScripts.get(id);
        if (descr != null) {
            descr.getContext().getDebugController().resumeScript();
        }
    }

    public void pauseScript(long id) {
        TestScriptDescription descr = testScripts.get(id);
        if (descr != null && descr.getState() != ScriptState.PAUSED) {
            descr.getContext().getDebugController().pauseScript();
        }
    }

    public void nextStep(long id) {
        TestScriptDescription descr = testScripts.get(id);
        if (descr != null && descr.getState() == ScriptState.PAUSED) {
            descr.getContext().getDebugController().pauseScriptOnNextStep();
        }
    }

    public void runCompiledScript(long id) {
        synchronized (pendingTestScriptsToRun) {
            synchronized (preparedTestScripts) {
                if (pendingTestScriptsToRun.remove(id)) {
                    preparedTestScripts.add(id);
                }
            }
        }
    }

    public void doShutdown() {
        logger.info("doShutdown");
        this.shutdown = true;
    }

    public void compileScript(long id) {
        logger.info("compileScript({})", id);
        synchronized (pendingTestScriptsToPrepare) {
            synchronized (addedTestScripts) {
                if (this.pendingTestScriptsToPrepare.remove(id)) {
                    this.addedTestScripts.add(id);
                }
            }
        }
    }

    public List<Long> getTestScripts() {
        return new ArrayList<>(testScripts.keySet());
    }

    @Override
    public void dispose() {

        logger.info("Script runner is being disposed");

        if (runnerState != State.DISPOSED && runnerState != State.DISPOSING) {
            isDisposing = true;
            runnerState = State.DISPOSING;
            joinThread(this.tScriptCompiler);
            joinThread(this.tScriptExecutor);
            runnerState = State.DISPOSED;
        }
        logger.info("Script runner was disposed");
    }

    /**
     *
     */
    protected void joinThread(Thread thread) {
        try {
            thread.join(1600);
        } catch (InterruptedException e) {
            logger.error("Joining thread {} interrupted", thread.getName(), e);
        }
        if (thread.isAlive()) {
            logger.warn("{} thread is not terminated", thread.getName());
        }
    }

    public void addScriptRunListener(IScriptRunListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }


    public void removeScriptRunListener(IScriptRunListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    protected void fireEvent(TestScriptDescription testScriptDescription) {
        synchronized (this.listeners) {
            for (IScriptRunListener listener : this.listeners) {
                try {
                    listener.onScriptRunEvent(testScriptDescription);
                } catch ( Throwable e ) {
                    logger.error("Listener {} threw exception", listener, e);
                }
            }
        }
    }

    protected void writeCSVReport(List<TestScriptDescription> descrs, File reportFile) throws Exception {

        DefaultReportWriter writer = new DefaultReportWriter(workspaceDispatcher);

        ReportWriterOptions options = new ReportWriterOptions();
        options.setWriteDetails(true);

        writer.write(reportFile, descrs, options);
    }

    public List<TestScriptDescription> getDescriptions() {
        List<TestScriptDescription> result = new ArrayList<>(testScripts.values());
        Collections.sort(result, new TimestampComparator());
        return result;
    }

    protected void onRunStarted(TestScriptDescription descr) {
        descr.setStartedTime(System.currentTimeMillis());
    }

    protected void onRunFinished(TestScriptDescription descr) {
        descr.setFinishedTime(System.currentTimeMillis());
        try {
            descr.getContext().getReport().closeReport();
        }
        catch (Exception e){
            logger.error(e.getMessage(), e);
        }
        try {
            descr.close();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        descr.unlock(); // unlock ability to remove report
    }

	protected GeneratedScript generateJavaSourcesFromMatrix(final TestScriptDescription description) throws ScriptRunException, IOException {

		File matrixFile = new File(description.getMatrixPath());

        logger.info("Generating script sources for {} matrix", matrixFile.getName());

        AMLSettings settings = new AMLSettings();

        settings.setMatrixPath(description.getMatrixPath());
        settings.setOrigMatrixPath(description.getOriginalMatrixPath());
        settings.setBaseDir(description.getWorkFolder());
        settings.setSrcDir("src");
        settings.setTestCasesRange(description.getRange());
        settings.setContinueOnFailed(description.getContinueOnFailed());
        settings.setAutoStart(description.getAutoStart());
        settings.setAutoRun(description.getAutoRun());
        settings.setSuppressAskForContinue(description.isSuppressAskForContinue());
        settings.setRunNetDumper(description.isRunNetDumper());
        settings.setSkipOptional(description.isSkipOptional());
        settings.setLanguageURI(description.getLanguageURI());
        settings.setStaticVariables(description.getStaticVariables());

        List<IPreprocessor> preprocessors = preprocessorLoader.getPreprocessors();
        for (IPreprocessor preprocessor : preprocessors)
            settings.addPreprocessor(preprocessor);

        List<IValidator> validators = validatorLoader.getValidators();
        for (IValidator validator : validators)
            settings.addValidator(validator);

        try {

            AML aml = new AML(settings,
                              workspaceDispatcher,
                              adapterManager,
                              environmentManager,
                              dictionaryManager,
                              staticServiceManager,
                              languageManager,
                              actionManager,
                              utilityManager,
                              compilerClassPath);

            aml.addProgressListener(new TestScriptProgressListener(description));

            description.setAlertCollector(aml.getAlertCollector());

            GeneratedScript script = aml.run(description.getContext(), description.getEncoding());

            logger.info("Generating script sources for {} matrix has been finished", matrixFile.getName());

            description.getContext().getReport().addAlerts(aml.getAlertCollector().aggregate(AlertType.WARNING));
            if (aml.getAlertCollector().getCount(AlertType.ERROR) != 0) {
                throw new AMLException("Errors detected", aml.getAlertCollector());
            }
            aml.cleanup();
            return script;

        } catch (InterruptedException e) {
            throw new ScriptRunException("Script canceled",e);
        } catch (IOException e) {
            throw new ScriptRunException(e);
        } catch (AMLException e) {
            StringBuilder message = new StringBuilder();
            if (e.getAlertCollector() != null && e.getAlertCollector().getCount(AlertType.ERROR) != 0) {
                logger.error("Errors detected", e);
                for (Alert error : e.getAlertCollector().getAlerts(AlertType.ERROR)) {
                    logger.error("{}", error);
                    message.append(error).append(LS);
                }
            }
            throw new ScriptRunException(message.toString(), e);
        }
    }

    protected String createAndGetDirectories(String matrixFileName, String originalMatrixPath, String subFolder) throws IOException {
        logger.debug("createDirectories script started [{}]", matrixFileName);

        File origMatrixFile = workspaceDispatcher.getFile(FolderType.MATRIX, originalMatrixPath);

        String tmpFolder = null;
        synchronized (this) {
            String folderPrefix = StringUtil.replaceChars(origMatrixFile.getName().trim(), forbiddenChars, '_');
            tmpFolder = folderPrefix + "_" + scriptFolderSuffix.format(new Date());

            if(subFolder != null && !subFolder.isEmpty()) {
                tmpFolder = subFolder + File.separator +  tmpFolder;
            }

            try {
            	workspaceDispatcher.createFolder(FolderType.REPORT, tmpFolder);
            } catch (WorkspaceStructureException ex) {
            	throw new ScriptRunException(ex);
            }
        }

        logger.debug("createDirectories script completed [{}]", matrixFileName);
        return tmpFolder;
	}

    protected WorkFolderPaths copyFilesToWorkFolder(String matrixFileName, String originalMatrixPath, String originalSettingsPath, String workFolderPath) throws FileNotFoundException, WorkspaceSecurityException {
    	logger.debug("copyFilesToWorkFolder script started [{}]", matrixFileName);

        File origMatrixFile = workspaceDispatcher.getFile(FolderType.MATRIX, originalMatrixPath);

        File scriptSettingsFile = workspaceDispatcher.getFile(FolderType.CFG, originalSettingsPath);

        File workFolder = workspaceDispatcher.getFile(FolderType.REPORT, workFolderPath);

        try {
            FileUtils.copyFileToDirectory(origMatrixFile, workFolder);
        } catch (IOException e) {
            throw new ScriptRunException("Could not copy matrix file [" + origMatrixFile.getAbsolutePath()
                    + "] to the script folder [" + workFolder.getAbsolutePath() + "]", e);
        }

        try {
            FileUtils.copyFileToDirectory(scriptSettingsFile, workFolder);
        } catch (IOException e) {
            throw new ScriptRunException("Could not copy matrix file [" + origMatrixFile.getAbsolutePath()
                    + "] to the script folder [" + workFolder.getAbsolutePath() + "]", e);
        }

        logger.debug("createDirectories script completed [{}]", matrixFileName);
        return new WorkFolderPaths(workFolderPath + File.separator + scriptSettingsFile.getName(),
                workFolderPath + File.separator + origMatrixFile.getName());
    }

    protected GeneratedScript prepareScript(final TestScriptDescription description) throws Exception {
        logger.info("Prepare script started [{}]", description.getMatrixFileName());

        GeneratedScript script = generateJavaSourcesFromMatrix(description);

        File binFolder = workspaceDispatcher.createFolder(FolderType.REPORT, description.getWorkFolder(), "bin");

        // parent class loader must be specified for new web-gui
        ILanguageFactory languageFactory = this.languageManager.getLanguageFactory(description.getLanguageURI());
        ClassLoader classLoader = languageFactory.createClassLoader(binFolder.toURI().toURL(), this.getClass().getClassLoader());

        // load Script Settings
        File scriptFile = workspaceDispatcher.getFile(FolderType.REPORT, description.getSettingsPath());
        XMLConfiguration scriptConfig = new XMLConfiguration();
        scriptConfig.setDelimiterParsingDisabled(true);
        scriptConfig.load(scriptFile);
        ScriptSettings scriptSettings = new ScriptSettings();
        scriptSettings.load(scriptConfig);
        scriptSettings.setScriptName(description.getMatrixFileName());

        // prepare script logger logger
        Logger scriptLogger = createScriptLogger(scriptSettings, description.getWorkFolder());

        description.setScriptConfiguration(AML.PACKAGE_NAME + "." + AML.CLASS_NAME, classLoader, scriptLogger, scriptSettings);
        description.setProgress(50);
        logger.info("Prepare script completed [{}]", description.getMatrixFileName());
        return script;
    }

    protected void compileScript(final GeneratedScript script, final TestScriptDescription description) throws InterruptedException {

        logger.info("Compile script #{} started (matrix {})", description.getId(), description.getMatrixFileName());
        Thread t = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    File binFolderPath = workspaceDispatcher.getFile(FolderType.REPORT, description.getWorkFolder(), "bin");
                    AML.compileScript(script, binFolderPath, description, compilerClassPath);
                    description.setProgress(100);
                } catch (InterruptedException e) {
                    logger.info("Break compile script #{} (matrix {})", description.getId(), description.getMatrixFileName());
                    cancelScript(description);
                } catch (Exception e) {
                    if (description != null) {
                        description.scriptInitFailed(e);
                        onRunFinished(description);
                        logger.error("TestScript #{} (matrix {}) was failed during preparation", description.getId(), description.getMatrixFileName(), e);
                    }
                }
            }
        }, "Script compiler #"+description.getId());
        t.setPriority(this.settings.getCompilerPriority());
        t.start();

        while (t.isAlive()) {
            if (description.isSetCancelFlag()) {
                t.interrupt();
                t.join(30000);
                if (t.isAlive()) {
                    logger.info("Test #{} (matrix {}) compilation still alive", description.getId(), description.getMatrixFileName());
                } else {
                    logger.info("Test #{} (matrix {}) compilation interrupted", description.getId(), description.getMatrixFileName());
                }
            }
            synchronized (addedTestScripts) {
                checkQueueOnCanceledScript(addedTestScripts);
            }
            t.join(DEFAULT_TIMEOUT);
        }

        // check for exception in compiler thread
        if(description.getCause() != null && description.getCause() instanceof RuntimeException) {
            throw (RuntimeException)description.getCause();
        }
        logger.info("Compile script #{} completed (matrix {})", description.getId(), description.getMatrixFileName());
    }

    protected void scriptExceptionProcessing(TestScriptDescription descr, Throwable cause) {
        if (descr != null) {
            if (cause instanceof InterruptedException) {
                // Do not throw InterruptedException here.
                // InterruptedException was thrown just for canceling compilation.
                // This is place there it should be caught.
                cancelScript(descr);
                logger.error("TestScript [{}] was canceled during preparation", descr.getId(), cause);
            } else {
                ScriptContext scriptContext = descr.getContext();

                IEnvironmentManager environmentManager = scriptContext.getEnvironmentManager();
                String scriptName = descr.getMatrixFileName();
                String description = descr.getDescription();

                ScriptRun scriptRun = environmentManager.getMessageStorage().openScriptRun("TestScript", description);

                scriptContext.setScriptRun(scriptRun);


                // Init Report
                if (description == null || description.equals("")) {
                    description = IScriptReport.NO_DESCRIPTION;
                }

                try {
                    IScriptReport report = scriptContext.getReport();

                    report.createReport(scriptContext, scriptName, description, scriptRun.getId().longValue(), scriptContext.getEnvironmentName(), scriptContext.getUserName());
                    report.createException(cause);
                } catch(ScriptRunException e) {
                    logger.error("Failed to create report", e);
                }

                descr.scriptInitFailed(cause);
                onRunFinished(descr);
                environmentManager.getMessageStorage().closeScriptRun(scriptRun);

                logger.error("TestScript [{}] was failed during preparation", descr.getId(), cause);
            }
        } else {
            logger.error("Thread [{}:{}] iteration was failed", Thread.currentThread().getId(), Thread.currentThread().getName(), cause);
        }
    }

    protected void cancelScript(final TestScriptDescription description) {
        logger.info("Script #{} (matrix {}) has been canceled", description.getId(), description.getMatrixFileName());

        ScriptContext scriptContext = description.getContext();
        String environmentName = scriptContext.getEnvironmentName();
        String userName = scriptContext.getUserName();

        try {
            IScriptReport report = scriptContext.getReport();
            report.createReport(scriptContext, description.getMatrixFileName(), description.getDescription(),
                    0, environmentName, userName);
            report.createException(new EPSCommonException(String.format("Script has been canceled by user %s", userName)));
        } catch (Exception ex) {
            logger.error("Can't create report for canceled script {}", description.getMatrixFileName(), ex);
        }

        description.scriptCanceled();
        onRunFinished(description);
    }

    protected void checkQueueOnCanceledScript(Deque<Long> queue) {
        Long scriptId = queue.peek();
        if (scriptId != null) {
            TestScriptDescription description = testScripts.get(scriptId);
            if (description != null) {
                logger.debug("Check script {}", scriptId);
                if (description.isSetCancelFlag()) {
                    Long pollScriptID = queue.poll();
                    if (!scriptId.equals(pollScriptID)) {
                        logger.error("Expected and actual script ID are different. Expected: {}; Actual: {}." +
                                " Might be script was stolen by another thread.", scriptId, pollScriptID);
                        queue.addFirst(pollScriptID);
                    } else if (!description.getState().isTerminateState()) {
                        cancelScript(description);
                    }
                }
            } else {
                logger.warn("Can't find script with id {}", scriptId);
            }
        } else {
            logger.debug("Queue is empty. Can't find any script's id to check it on canceled status");
        }
    }

    private Logger createScriptLogger(ScriptSettings scriptSettings, String reportFolder) throws IOException, WorkspaceStructureException {
        org.apache.log4j.Logger scriptLogger = org.apache.log4j.Logger.getLogger("TestScript_" + RandomStringUtils.randomAlphanumeric(10));
        scriptLogger.removeAllAppenders();

        PatternLayout layout = new PatternLayout(scriptSettings.getFileLayout());

        RollingFileAppender fileAppender = new RollingFileAppender(layout, workspaceDispatcher.createFile(FolderType.REPORT, true, reportFolder, "script.log").getPath());

        fileAppender.setName("TESTSCRIPTFILEAPPENDER");

        fileAppender.setThreshold(Level.toLevel(scriptSettings.getFileLoggerLevel()));

        fileAppender.activateOptions();

        HTMLLayout htmlLayout = new HTMLLayout();

        RollingFileAppender htmlFileAppender = new RollingFileAppender(htmlLayout, workspaceDispatcher.createFile(FolderType.REPORT, true, reportFolder, "scriptlog.html").getPath());

        htmlFileAppender.setName("HTMLTESTSCRIPTFILEAPPENDER");

        htmlFileAppender.setThreshold(Level.toLevel(scriptSettings.getFileLoggerLevel()));

        PatternLayout conLayout = new PatternLayout(scriptSettings.getConsoleLayout());

        ConsoleAppender conAppender = new ConsoleAppender(conLayout);
        conAppender.setThreshold(Level.toLevel(scriptSettings.getConsoleLoggerLevel()));
        conAppender.activateOptions();

        scriptLogger.addAppender(fileAppender);
        scriptLogger.addAppender(conAppender);
        scriptLogger.addAppender(htmlFileAppender);

        return LoggerFactory.getLogger(scriptLogger.getName());
    }

    protected class InternalProgressListener implements ScriptProgress.IScriptRunProgressListener {
        @Override
        public void onProgressChanged(long id) {
            TestScriptDescription testScriptDescription = testScripts.get(id);

            if (testScriptDescription != null) {
                fireEvent(testScriptDescription);
            } else {
                logger.warn("TestScriptRunId with specified id = [{}] doesn't exist", id);
            }
        }
    }

    protected class PauseListener implements IPauseListener {

        @Override
        public void onScriptPaused(long id, String reason, long timeout) {
            TestScriptDescription testScriptDescription = testScripts.get(id);
            if ( testScriptDescription != null ) {
                testScriptDescription.scriptPaused(reason, timeout);
            } else {
                logger.warn("TestScriptRunId with specified id = [{}] doesn't exist", id);
            }
        }

        @Override
        public void onScriptResumed(long id) {
            TestScriptDescription testScriptDescription = testScripts.get(id);
            if ( testScriptDescription != null ) {
                testScriptDescription.scriptResume();
            } else {
                logger.warn("TestScriptRunId with specified id = [{}] doesn't exist", id);
            }
        }
    }

    protected class InternalTestScriptRunEventsListener implements IScriptRunListener {
        @Override
        public void onScriptRunEvent(TestScriptDescription testScriptDescription){
            if ( testScriptDescription != null ) {
                fireEvent(testScriptDescription); // broadcast
            } else {
                logger.warn("TestScriptDescription is null");
            }
        }
    }

    protected class InternalScript implements Callable<Throwable> {

        private final Class<? extends SailFishTestCase> testCaseClass;

        private final ScriptContext scriptContext;

        InternalScript(Class<? extends  SailFishTestCase> testCaseClass, ScriptContext scriptContext)
        {
            this.testCaseClass = testCaseClass;

            this.scriptContext = scriptContext;
        }


        @Override
        public Throwable call() throws Exception
        {

            logger.info("Call started");

            SFJUnitRunner runner = new SFJUnitRunner();

            Throwable result = null;

            try
            {
                runner.run(testCaseClass, scriptContext);
            }
            catch (Throwable e)
            {
                logger.error(e.getMessage(), e);
                this.scriptContext.getScriptConfig().getLogger().error("Problem during testscript running", e);
                result = e;
            }

            logger.debug("Call finished");

            return result;
        }
    }

    private class WorkFolderPaths {
        private final String settingsPath;
        private final String matrixPath;

        private WorkFolderPaths(String settingsPath, String matrixPath) {
            this.settingsPath = settingsPath;
            this.matrixPath = matrixPath;
        }

        public String getSettingsPath() {
            return settingsPath;
        }

        public String getMatrixPath() {
            return matrixPath;
        }
    }

    private static class TimestampComparator implements Comparator<TestScriptDescription> {
        @Override
        public int compare(TestScriptDescription ts1, TestScriptDescription ts2) {
            return ts1.getTimestamp().compareTo(ts2.getTimestamp());
        }
    }
}
