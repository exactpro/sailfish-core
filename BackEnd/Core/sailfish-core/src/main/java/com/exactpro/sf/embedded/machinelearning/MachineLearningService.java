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
package com.exactpro.sf.embedded.machinelearning;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.center.IVersion;
import com.exactpro.sf.configuration.IDataManager;
import com.exactpro.sf.configuration.IDictionaryManager;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.embedded.IEmbeddedService;
import com.exactpro.sf.embedded.configuration.ServiceStatus;
import com.exactpro.sf.embedded.machinelearning.entities.FailedAction;
import com.exactpro.sf.embedded.machinelearning.storage.MLFileStorage;
import com.exactpro.sf.storage.IMapableSettings;

public class MachineLearningService implements IEmbeddedService {

    private static final Logger logger = LoggerFactory.getLogger(MachineLearningService.class);

    private volatile ServiceStatus status = ServiceStatus.Disconnected;
    private volatile ServiceStatus predictorStatus = ServiceStatus.Disconnected;

    private volatile String errorMsg = "";

    private volatile MLFileStorage storage;

    private BatchInsertWorker insertWorker;

    private BlockingQueue<FailedAction> batchInsertQueue = new LinkedBlockingQueue<>();
    private final ExecutorService exService = Executors.newSingleThreadExecutor();

    private MLPredictor mlPredictor;
    private IVersion mlVersion;
    private final IDictionaryManager dictionaryManager;
    private final IWorkspaceDispatcher workspaceDispatcher;
    private final IDataManager dataManager;
    private final Map<String, ClassLoader> pluginClassloaders;

    public MachineLearningService(IWorkspaceDispatcher workspaceDispatcher, IDictionaryManager dictionaryManager, IDataManager dataManager, Map<String, ClassLoader> pluginClassloaders) {
        this.dictionaryManager = dictionaryManager;
        this.dataManager = dataManager;
        this.pluginClassloaders = pluginClassloaders;
        this.workspaceDispatcher  = workspaceDispatcher;
    }

    public void preCheckConnection() {
        setStatus(ServiceStatus.Checking);
    }

    @Override
    public void setSettings(IMapableSettings settings) {

        throw new UnsupportedOperationException("Machine learning service don't use settings");
    }

    public MLFileStorage getStorage() {
        return storage;
    }

    @Override
    public boolean isConnected() {
        return this.status.equals(ServiceStatus.Connected);
    }

    @Override
    public ServiceStatus getStatus() {
        return status;
    }

    @Override
    public String getErrorMsg() {
        return errorMsg;
    }

    private void initStorage() throws IOException {
        storage = new MLFileStorage(workspaceDispatcher);
    }

    private void setStatus(ServiceStatus status) {
        setStatus(status, "");
    }

    private void setStatus(ServiceStatus status, String message) {
        this.errorMsg = message;
        this.status = status;
        logger.info("Statistics status {}", status);
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

        if (this.storage != null) {
            setStatus(ServiceStatus.Disconnected);
        }

        exService.shutdown();

        logger.info("Machine Learning service disposed");
    }

    @Override
    public synchronized void init() {
        logger.info("init");

        try {
            try {
                initStorage();
            } catch (IOException e) {
                this.errorMsg = e.getMessage();
                setStatus(ServiceStatus.Disconnected);
            }

            this.insertWorker = new BatchInsertWorker();
            Thread workerThread = new Thread(this.insertWorker, "Machine Learning insert worker");
            workerThread.setDaemon(true);
            workerThread.start();

            setStatus(ServiceStatus.Connected);

            logger.info("Machine Learning service initialized");

            initPredictor();

            logger.info("{}", this.status);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void storeData(FailedAction fAction) {
        if(!this.status.equals(ServiceStatus.Connected)) {
            return;
        }

        try {
            this.batchInsertQueue.put(fAction);
        } catch (InterruptedException e) {
            logger.error("Put interrupted", e);
            Thread.currentThread().interrupt();
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void initPredictor() {

        //TODO hardcoded plugin name
        ClassLoader extensionML = pluginClassloaders.get("ml");
        if (Objects.isNull(extensionML)) {
            return;
        }

        predictorStatus = ServiceStatus.Checking;

        try {
            // FIXME: Several ML extensions in one Sailfish
            mlPredictor = ServiceLoader.load(MLPredictor.class, extensionML).iterator().next();
            mlPredictor.init(dataManager, dictionaryManager);
            mlVersion = ServiceLoader.load(IVersion.class, extensionML).iterator().next();
            predictorStatus = ServiceStatus.Connected;
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
            predictorStatus = ServiceStatus.Error;
        }
    }

    public MLPredictor getMlPredictor() {
        return mlPredictor;
    }

    public ServiceStatus getPredictorStatus() {
        return predictorStatus;
    }

    public IVersion getMLVersion() {
        return mlVersion;
    }

    public InputStream getAllSubmitsAsZip() throws IOException {

        PipedInputStream in = new PipedInputStream();
        PipedOutputStream out = new PipedOutputStream(in);
        
        Runnable task = () -> {
            try {
                storage.zipDocumentsToStream(out, 4);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        };

        exService.submit(task);
        
        return in;
    }

    private class BatchInsertWorker implements Runnable {

        private volatile boolean running = true;

        public void stop() {
            this.running = false;
        }

        @Override
        public void run() {
            logger.info("Machine Learning InsertWorker started");

            while (this.running) {
                try {
                    FailedAction fAction = batchInsertQueue.poll();

                    if(fAction == null) {
                        Thread.sleep(700l);
                        continue;
                    }

                    storage.storeFailedAction(fAction);
                } catch (InterruptedException e) {
                    logger.error("Interrupted", e);
                    break;
                } catch(Throwable t) {
                    logger.error(t.getMessage(), t);
                }
            }

            logger.info("Machine Learning InsertWorker stopped");
        }
    }
}
