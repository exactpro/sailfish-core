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
package com.exactpro.sf.testwebgui.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.aml.converter.ConversionMonitor;
import com.exactpro.sf.aml.converter.IMatrixConverter;
import com.exactpro.sf.aml.converter.IMatrixConverterSettings;
import com.exactpro.sf.bigbutton.BigButtonSettings;
import com.exactpro.sf.bigbutton.RegressionRunner;
import com.exactpro.sf.center.ISFContext;
import com.exactpro.sf.center.impl.SFLocalContext;
import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.configuration.workspace.WorkspaceSecurityException;
import com.exactpro.sf.configuration.workspace.WorkspaceStructureException;
import com.exactpro.sf.embedded.mail.EMailService;
import com.exactpro.sf.embedded.mail.configuration.EMailServiceSettings;
import com.exactpro.sf.embedded.statistics.StatisticsService;
import com.exactpro.sf.embedded.statistics.configuration.StatisticsServiceSettings;
import com.exactpro.sf.embedded.statistics.entities.Tag;
import com.exactpro.sf.scriptrunner.IConnectionManager;
import com.exactpro.sf.scriptrunner.IScriptReport;
import com.exactpro.sf.scriptrunner.IServiceNotifyListener;
import com.exactpro.sf.scriptrunner.ReportWriterOptions;
import com.exactpro.sf.scriptrunner.ReportWriterOptions.Duration;
import com.exactpro.sf.scriptrunner.TestScriptDescription;
import com.exactpro.sf.scriptrunner.reportbuilder.ReportType;
import com.exactpro.sf.scriptrunner.reportbuilder.ReportWriterException;
import com.exactpro.sf.services.EnvironmentDescription;
import com.exactpro.sf.services.IService;
import com.exactpro.sf.services.IServiceSettings;
import com.exactpro.sf.services.ServiceDescription;
import com.exactpro.sf.services.ServiceException;
import com.exactpro.sf.services.ServiceMarshalManager;
import com.exactpro.sf.services.ServiceStatus;
import com.exactpro.sf.storage.IMapableSettings;
import com.exactpro.sf.storage.IMappableSettingsSerializer;
import com.exactpro.sf.storage.IMatrix;
import com.exactpro.sf.storage.IMatrixStorage;
import com.exactpro.sf.storage.IOptionsStorage;
import com.exactpro.sf.storage.StorageException;
import com.exactpro.sf.storage.util.PropertiesSettingsReaderSerializer;
import com.exactpro.sf.testwebgui.servlets.ReportTask;

public class TestToolsAPI {

	private static final Logger logger = LoggerFactory.getLogger(TestToolsAPI.class);

	private static TestToolsAPI instance = null;

	private final ISFContext context;

	private final CopyOnWriteArrayList<IMatrixListener> matrixListeners;

	private final CopyOnWriteArrayList<IServiceNotifyListener> serviceListeners;

	private final IMappableSettingsSerializer settingsWriter = new PropertiesSettingsReaderSerializer();

	private TestToolsAPI() {
		//FIXME Singletons should be removed from SF framework
		this.context = SFLocalContext.getDefault();

		this.matrixListeners = new CopyOnWriteArrayList<>();
		this.serviceListeners = new CopyOnWriteArrayList<>();
	}

	public static synchronized TestToolsAPI getInstance() {
		if (instance == null) {
			instance = new TestToolsAPI();
		}
		return instance;
	}

	public IMatrix uploadMatrix(InputStream stream, String name, String description, String creator, SailfishURI languageURI, String matrixLink, SailfishURI matrixProviderURI) {
		IMatrix matrix = this.context.getMatrixStorage().addMatrix(stream, name, description, creator, languageURI, matrixLink, matrixProviderURI);

		notifyAddMatrixListeners(new IMatrixNotifier() {
			@Override
			public void notify(IMatrixListener listener, IMatrix matrix) {
				listener.addMatrix(matrix);
			}
		}, matrix);

		return matrix;
	}

	public File createAggrigateReport(String name, Date startDate, Date endDate, boolean details, Duration duration, ReportType reportType) throws ReportWriterException, IOException {
	    ReportWriterOptions options = new ReportWriterOptions();
	    options.setCustomStart(startDate);
	    options.setCustomEnd(endDate);
	    options.setWriteDetails(details);
	    options.setSelectedDuration(duration);
	    options.setSelectedReportType(reportType);

        return createAggrigateReport(name, options);
	}

	public File createAggrigateReport(String name, ReportWriterOptions reportWriterOptions) throws ReportWriterException, IOException {
        File reportFile = File.createTempFile(name, ".csv");

        List<TestScriptDescription> descrs = this.context.getScriptRunner().getDescriptions();
        this.context.getReportWriter().write(reportFile, descrs, reportWriterOptions);

        return reportFile;
    }

	public void stopScriptRun(long id){
	    logger.info("stopScript() invoked: {}", id);
	    context.getScriptRunner().stopScript(id);
	}

	public void stopAllScriptRuns(){
        logger.info("stopAllScript() invoked");
        context.getScriptRunner().stopAllScript();
    }

	public boolean containsScriptRun(long id) {
	    return context.getScriptRunner().getTestScriptDescription(id) != null;
	}

	public boolean hasConverter() {
		return !this.context.getMatrixConverterManager().getMatrixConverters().isEmpty();
	}

    public Set<SailfishURI> getMatrixConverters() {
        return this.context.getMatrixConverterManager().getMatrixConverters();
		}

    public IMatrixConverterSettings prepareConverterSettings(Long matrixId, String environment, SailfishURI converterUri) throws FileNotFoundException, WorkspaceSecurityException, WorkspaceStructureException {
        return prepareConverterSettings(matrixId, environment, converterUri, null);
    }

    public IMatrixConverterSettings prepareConverterSettings(Long matrixId, String environment, SailfishURI converterUri, String newMatrixName) throws FileNotFoundException, WorkspaceSecurityException, WorkspaceStructureException {
	    IMatrixConverterSettings settings = getMatrixConverterSettings(converterUri);
	    IMatrixStorage matrixStorage = this.context.getMatrixStorage();
        IMatrix matrix = matrixStorage.getMatrixById(matrixId);
        IWorkspaceDispatcher wd = this.context.getWorkspaceDispatcher();

        if(matrix == null) {
            throw new IllegalArgumentException("Matrix id [" + matrixId + "] was not found");
        }

        logger.info("Preparation convert started {}", matrix.getName());

        File matrixFilePath = wd.getFile(FolderType.MATRIX, matrix.getFilePath());
        if (newMatrixName == null) {
            newMatrixName = converterUri.getResourceName() + "_" + matrix.getName();
        }
        File outMatrixFilePath = wd.createFile(FolderType.MATRIX, true, matrixFilePath.getParentFile().getName(), newMatrixName);

        settings.setEnvironment(environment);
        settings.setInputFile(matrixFilePath);
        settings.setOutputFile(outMatrixFilePath);

        return settings;
	}

	//FIXME: Get converter
	@Deprecated
	public IMatrixConverter getMatrixConverter() {
	    Iterator<SailfishURI> it = this.context.getMatrixConverterManager().getMatrixConverters().iterator();
        if (it.hasNext()) {
            return getMatrixConverter(it.next());
        }
        return null;
	}

    //FIXME: Get converter
    @Deprecated
    public IMatrixConverterSettings getMatrixConverterSettings() {
        Iterator<SailfishURI> it = this.context.getMatrixConverterManager().getMatrixConverters().iterator();
        if(it.hasNext()) {
            return getMatrixConverterSettings(it.next());
        }
        return null;
    }

    public IMatrixConverter getMatrixConverter(SailfishURI uri) {
		try {
            return this.context.getMatrixConverterManager().getMatrixConverter(uri);
        } catch (RuntimeException e) {
            logger.error("Failed to create & init MatrixConverter", e);
			throw new RuntimeException("Failed to init converter", e);
		}
	}

    public IMatrixConverterSettings getMatrixConverterSettings(SailfishURI uri) {
        try {
            return this.context.getMatrixConverterManager().getMatrixConverterSettings(uri);
        } catch(RuntimeException e) {
            logger.error("Failed to create matrix converter settings", e);
            throw new RuntimeException("Failed to create matrix converter settings", e);
        }
    }

    public Long convertMatrix(Long matrixId, String environment, SailfishURI uri) throws Exception {
        ConversionMonitor monitor = new ConversionMonitor();
        IMatrixConverter converter = getMatrixConverter(uri);
        IMatrixConverterSettings settings = prepareConverterSettings(matrixId, environment, uri);
        if(!converter.convert(settings, monitor)) {
            throw new RuntimeException(monitor.getErrors().toString());
        }
        File newMatrixFile = settings.getOutputFile();
        if (newMatrixFile.exists()) {
            String newName = newMatrixFile.getName();
            try (InputStream matrixInputStream = new FileInputStream(newMatrixFile)) {
				IMatrix aml3matrix = uploadMatrix(matrixInputStream, newName, null, "Converter", SailfishURI.unsafeParse("AML_v3"), null, null);
                return aml3matrix.getId();
            }
        } else {
            throw new IllegalStateException(newMatrixFile + " didn't exists");
        }
    }

	public long executeMatrix(IMatrix matrix, SailfishURI languageURI,
			String rangeParam, String encoding, String environment,
			String userName, boolean continueOnFailed, boolean autoStart, boolean autoRun,
			boolean ignoreAskForContinue, boolean runNetDumper, boolean skipOptional,
            List<Tag> tags, Map<String, String> staticVariables,
			Collection<IScriptReport> userListeners, String subFolder) throws FileNotFoundException, IOException {
		return this.context.getScriptRunner().enqueueScript(
				"script.xml",
				matrix.getFilePath(),
                matrix.getDescription(),
                matrix.getName(),
                rangeParam,
                continueOnFailed,
                autoStart,
                autoRun,
                ignoreAskForContinue,
                runNetDumper,
                skipOptional,
                languageURI,
                encoding,
                environment,
                userName,
                tags,
                staticVariables,
                userListeners,
                subFolder,
                context);
	}

	public void deleteMatrix(IMatrix matrix) {
		if (matrix != null) {
			notifyAddMatrixListeners(new IMatrixNotifier() {
				@Override
				public void notify(IMatrixListener listener, IMatrix matrix) {
					listener.removeMatrix(matrix);
				}
			}, matrix);
			this.context.getMatrixStorage().removeMatrix(matrix);
		}
	}

	public void deleteAllMatrix() {
		List<IMatrix> matrixList = this.context.getMatrixStorage().getMatrixList();

		if (matrixList != null) {
			IMatrixNotifier notifier = new IMatrixNotifier() {
				@Override
				public void notify(IMatrixListener listener, IMatrix matrix) {
					listener.removeMatrix(matrix);
				}
			};
			for (IMatrix matrix : matrixList) {
				notifyAddMatrixListeners(notifier , matrix);
				this.context.getMatrixStorage().removeMatrix(matrix);
			}
		}
	}

	public void addListener(IMatrixListener listener) {
		this.matrixListeners.addIfAbsent(listener);
	}

	public void removeListener(IMatrixListener listener) {
		this.matrixListeners.remove(listener);
	}

	public void addListener(IServiceNotifyListener listener) {
        this.serviceListeners.addIfAbsent(listener);
    }

    public void removeListener(IServiceNotifyListener listener) {
        this.serviceListeners.remove(listener);
    }

	public void removeService(String envName, String name, IServiceNotifyListener notifyListener) throws ExecutionException, InterruptedException {

        IConnectionManager conManager = context.getConnectionManager();

        ServiceName serviceName = new ServiceName(envName, name);

        IService service = conManager.getService(serviceName);

        if (service != null) {
        	conManager.removeService(serviceName, notifyListener).get();
        } else {
            throw new IllegalArgumentException("Service " + serviceName.toString() + " was not found");
        }
	}

    public void removeServices(String envName, IServiceNotifyListener notifyListener) throws  ExecutionException, InterruptedException {

        IConnectionManager conManager = context.getConnectionManager();
        ServiceName[] serviceNames = conManager.getServiceNames();

        for (ServiceName sName : serviceNames) {
            if (envName.equals(sName.getEnvironment()) || (ServiceName.DEFAULT_ENVIRONMENT.equals(envName) && sName.getEnvironment() == null) ) {
            	conManager.removeService(sName, notifyListener).get();
            }
        }
    }

    public void addService(ServiceName serviceName, SailfishURI serviceURI, IServiceNotifyListener notifyListener) throws ExecutionException, InterruptedException {
    	IConnectionManager conManager = context.getConnectionManager();
    	conManager.addService(serviceName, serviceURI, null, notifyListener).get();
	}

    public void copyService(String oldServiceName, String oldEnv, String newServiceName, String newEnv, IServiceNotifyListener notifyListener) throws ExecutionException, InterruptedException {
    	IConnectionManager conManager = context.getConnectionManager();
    	conManager.copyService(new ServiceName(oldEnv, oldServiceName), new ServiceName(newEnv, newServiceName), notifyListener).get();
	}

    public IService getService(String environment, String serviceName) {
    	IConnectionManager conManager = context.getConnectionManager();
        return conManager.getService(new ServiceName(environment, serviceName));
	}

    public Future<?> startService(ServiceName serviceName, boolean sync, IServiceNotifyListener notifyListener) throws ExecutionException, InterruptedException {
    	IConnectionManager conManager = context.getConnectionManager();
    	IService service = conManager.getService(serviceName);
        if (service.getStatus() != ServiceStatus.STARTED && service.getStatus() != ServiceStatus.WARNING
                && service.getStatus() != ServiceStatus.DISABLED) {
            logger.info("starting {} service", serviceName);
            try {
                conManager.initService(serviceName, notifyListener).get();
                Future<?> future = conManager.startService(serviceName, notifyListener);
                if (sync) {
                    future.get();
                }
                return future;
            } catch (ServiceException e){
                throw new ServiceException("Failed to start service " + serviceName + ": " + e.getMessage(), e);
            }
        } else {
            return null;
        }
	}

    public Future<?> startService(String environment, String serviceName, boolean sync, IServiceNotifyListener notifyListener) throws ExecutionException, InterruptedException {
	    ServiceName name = new ServiceName(environment, serviceName);
        Future<?> future = startService(name, false, notifyListener);
        if (sync && future != null) {
            IConnectionManager conManager = context.getConnectionManager();
            IServiceSettings serviceSettings = conManager.getServiceSettings(name);
            if (serviceSettings.getExpectedTimeOfStarting() > 0) {
                Thread.sleep(serviceSettings.getExpectedTimeOfStarting());
            }
        }
        return future;
	}

    public void startAllService(boolean sync, IServiceNotifyListener notifyListener) throws ExecutionException, InterruptedException {
        IConnectionManager conManager = context.getConnectionManager();
        ServiceName[] serviceNames = conManager.getServiceNames();
        List<Future<?>> futures = new ArrayList<>();
        long maxExpectedTimeOfStart = 0;

        for (ServiceName serviceName : serviceNames) {
            Future<?> future = startService(serviceName, false, notifyListener);
            if (sync && future != null) {
                long timeOfStart = conManager.getServiceSettings(serviceName).getExpectedTimeOfStarting();
                maxExpectedTimeOfStart = Math.max(maxExpectedTimeOfStart, timeOfStart);
                futures.add(future);
            }
        }

        if (sync) {
            for (Future<?> future : futures) {
                future.get();
            }
            if (maxExpectedTimeOfStart > 0) {
                Thread.sleep(maxExpectedTimeOfStart);
            }
        }
    }


    public void stopService(ServiceName serviceName, boolean sync, IServiceNotifyListener notifyListener) throws ExecutionException, InterruptedException {
    	IConnectionManager conManager = context.getConnectionManager();
    	IService service = conManager.getService(serviceName);
        if (service.getStatus() != ServiceStatus.DISPOSED && service.getStatus() != ServiceStatus.DISABLED) {
            logger.info("disposing {} service", serviceName);
            try {
                if (sync) {
                	conManager.disposeService(serviceName, notifyListener).get();
                } else {
                	conManager.disposeService(serviceName, notifyListener);
                }
            } catch (ServiceException e){
                throw new ServiceException("Failed to dispose service " + serviceName + ": " + e.getMessage(), e);
            }
        }
	}

    public void stopService(String environment, String serviceName, boolean sync, IServiceNotifyListener notifyListener) throws ExecutionException, InterruptedException {
        stopService(new ServiceName(environment, serviceName), sync, notifyListener);
	}

    public void stopAllService(boolean sync, IServiceNotifyListener notifyListener) throws ExecutionException, InterruptedException {
        IConnectionManager conManager = context.getConnectionManager();
        ServiceName[] serviceNames = conManager.getServiceNames();
        for (ServiceName serviceName : serviceNames) {
            stopService(serviceName, sync, notifyListener);
        }
	}

    public void renameEnvironment(String oldEnvName, String newEnvName) throws IllegalArgumentException, ExecutionException, InterruptedException {
    	IConnectionManager conManager = context.getConnectionManager();
    	conManager.renameEnvironment(oldEnvName, newEnvName, null).get();
    }

    public void removeEnvironment(String envName, IServiceNotifyListener notifyListener) throws IllegalArgumentException, ExecutionException, InterruptedException {
    	IConnectionManager conManager = context.getConnectionManager();
    	conManager.removeEnvironment(envName, notifyListener).get();
    }

    public void addEnvironment(String envName) throws IllegalArgumentException, ExecutionException, InterruptedException {
    	IConnectionManager conManager = context.getConnectionManager();
    	conManager.addEnvironment(envName, null).get();
    }

    public ImportServicesResult importServices(InputStream inputStream, boolean isZip, String environment, boolean replace,
    		boolean skip, boolean skipEnvDescFile, IServiceNotifyListener notifyListener) throws FileNotFoundException {

        List<ImportStatus> result = new ArrayList<>();
        IConnectionManager conManager = context.getConnectionManager();

        int skippedCount = 0;
        List<ServiceName> existServs = Arrays.asList(conManager.getServiceNames());
		ServiceMarshalManager marshalManager = new ServiceMarshalManager(context.getStaticServiceManager(), context.getDictionaryManager());
		List<String> errors = new ArrayList<>();
		List<ServiceDescription> descrs = new ArrayList<>();

        EnvironmentDescription envDesc = null;
        try {
            envDesc = marshalManager.unmarshalServices(inputStream, isZip, descrs, errors);
        } catch (RuntimeException e) {
            notifyListener.onErrorProcessing("Service can't be import. Problem during service configuration reading (" + e.getMessage() + ")");
        }

		if (!skipEnvDescFile && envDesc != null && !StringUtils.isEmpty(envDesc.getName())) {
		    environment = envDesc.getName();
		}

		try {
		    if(!getEnvNames().contains(environment) && !ServiceName.DEFAULT_ENVIRONMENT.equals(environment)) {
		        addEnvironment(environment);
		    }
		} catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }

        for (ServiceDescription descr : descrs) {
            descr.setEnvironment(environment);
        }

        if (replace) {
            deleteServicesForReplace(conManager, descrs, existServs, notifyListener);
        } else if (skip) {

            Iterator<ServiceDescription> iterator = descrs.iterator();
            while (iterator.hasNext()) {
                ServiceDescription cur = iterator.next();
                for (ServiceName serviceName : existServs) {
                    if (serviceName.getEnvironment().equals(cur.getEnvironment()) && serviceName.getServiceName().equals(cur.getName())) {
                        iterator.remove();
                        skippedCount++;
                        break;
                    }
                }
            }

        } else {
            Iterator<ServiceDescription> it = descrs.iterator();
            while (it.hasNext()) {
                ServiceDescription descr = it.next();
                try {
                    if (checkServiceDuplicates(environment, descr.getName())) {
                        it.remove();
                        ImportStatus importStatus = new ImportStatus(descr.getName(), ImportStatus.ERROR,
                                "Service " + descr.getName() + " already exist in environment " + environment);
                        result.add(importStatus);
                        throw new StorageException("Environment '" + environment + "' have service with name '" + descr.getName() + "' already");
                    }
                } catch (Exception ex) {
                    notifyListener.onErrorProcessing("Failed to import service " + descr.getName() + "\n" + ex.getMessage());
                }
            }
        }

        String lastImported = null;
        int importedCount = 0;

        for (ServiceDescription descr : descrs) {
            ImportStatus importStatus = null;
            try {
                if (checkServiceDuplicates(environment, descr.getName())) {
                    throw new Exception("Service already exists");
                }

                descr.setEnvironment(environment);
                conManager.addService(new ServiceName(descr.getEnvironment(), descr.getName()), descr.getType(), descr.getSettings(), notifyListener)
                        .get();
                importedCount++;
                lastImported = descr.getName();
                importStatus = new ImportStatus(descr.getName(), ImportStatus.OK);
            } catch (Exception ex) {
                importStatus = new ImportStatus(descr.getName(), ImportStatus.ERROR, ex.getMessage());
                notifyListener.onErrorProcessing("Failed to import service " + descr.getName() + "\n" + ex.getMessage());
            }
            result.add(importStatus);
        }

        for (String error : errors) {
            notifyListener.onErrorProcessing(error);
        }

        if (importedCount == 1) {
            if (replace) {
                notifyListener.onInfoProcessing("Service " + lastImported + " has been replaced");
            } else {
                notifyListener.onInfoProcessing("Service " + lastImported + " has been imported");
            }
        } else if (importedCount > 1) {
            notifyListener.onInfoProcessing(importedCount + " services have been imported");
        }

        if (skippedCount > 0) {
            notifyListener.onInfoProcessing(skippedCount + " services have been skipped");
        }
        return new ImportServicesResult(environment, result);
    }

    public File getTestScriptRunZip(long id) throws FileNotFoundException {
        return ReportTask.getZip(id, this.context);
    }
    private void deleteServicesForReplace(IConnectionManager conManager, List<ServiceDescription> newServs, List<ServiceName> existServiceNames, IServiceNotifyListener notifyListener) {
        try {
            List<ServiceName> newServStrings = new ArrayList<>();
            for (ServiceDescription sd : newServs) {
                newServStrings.add(new ServiceName(sd.getEnvironment(), sd.getName()));
            }

            for (ServiceName serviceName : existServiceNames) {
                if (newServStrings.contains(serviceName)) {
                    try {
                    	conManager.removeService(serviceName, notifyListener).get();
                    } catch(Exception e) {
                        notifyListener.onErrorProcessing("Failed to remove service " + serviceName + "\n" + e.getMessage());
                    }
                }
            }
        } catch (Exception e){
            throw new ServiceException(e.getMessage(), e);
        }
    }

    private boolean checkServiceDuplicates(String environment, String serviceName){
        IService service = TestToolsAPI.getInstance().getService(environment, serviceName);
        return service != null;
    }

    public void setStatisticsDBSettings(StatisticsServiceSettings settings) throws Exception {

        StatisticsService service = this.context.getStatisticsService();//BeanUtil.getSfContext().getStatisticsService();

        if(service.isConnected()) {
            service.tearDown();
        }
        service.setSettings(settings);
        service.init();

        saveSettings(settings, true, FolderType.CFG);
    }

    public void setEMailServiceSettings(EMailServiceSettings settings) throws Exception {
        EMailService service = this.context.getEMailService();

        if(service.isConnected()){
            service.tearDown();
        }

        service.setSettings(settings);
        service.init();

        saveSettings(settings, false, null);
    }
    
    public void setRegressionRunnerSettings(BigButtonSettings settings) throws Exception {
        RegressionRunner runner = this.context.getRegressionRunner();

        runner.setSettings(settings);

        saveSettings(settings, false, null);
    }

    private void saveSettings(IMapableSettings settings, boolean saveToFile, FolderType folderType, String... relativePath) throws Exception {

        Map<String, String> toSave = settings.toMap();

        IOptionsStorage optionsStorage = this.context.getOptionsStorage();

        for(Map.Entry<String, String> entry : toSave.entrySet()) {

            optionsStorage.setOption(entry.getKey(), entry.getValue());
        }

        if (saveToFile) {
            try {
                settingsWriter.writeMappableSettings(settings, context.getWorkspaceDispatcher(), folderType, relativePath);
            } catch (Exception ex) {
                logger.error("Can't save {} settings to file", settings.settingsName(), ex);
            }
        }
    }


    public List<String> getEnvNames(){
        IConnectionManager conManager = context.getConnectionManager();
        return conManager.getEnvironmentList();
    }

	private void notifyAddMatrixListeners(IMatrixNotifier notifier, IMatrix matrix) {

		for (IMatrixListener listener : this.matrixListeners) {
			try {
				notifier.notify(listener, matrix);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
	}

	private interface IMatrixNotifier {
		public void notify(IMatrixListener listener, IMatrix matrix);
	}
}
