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
package com.exactpro.sf.configuration.netdumper;

import java.beans.Introspector;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.scriptrunner.IConnectionManager;
import com.exactpro.sf.scriptrunner.IEnvironmentListener;
import com.exactpro.sf.services.EnvironmentEvent;
import com.exactpro.sf.services.IServiceSettings;
import com.exactpro.sf.services.ServiceEvent;
import com.exactpro.sf.services.ServiceEvent.Type;
import com.exactpro.sf.services.ServiceStatusUpdateEvent;
import com.exactpro.sf.storage.IOptionsStorage;

public class NetDumperService implements IEnvironmentListener {

    private static final Logger logger = LoggerFactory.getLogger(NetDumperService.class);

    private static final String IS_OK = "/isOK";

    private static final ThreadLocal<DateFormat> dateFormat = new ThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            return new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss");
        }
    };

	private final IConnectionManager connectionManager;
	private final IWorkspaceDispatcher workspaceDispatcher;
	private final IOptionsStorage optionsStorage;

	private final ConcurrentMap<ServiceName, List<RESTDumperClient>> serviceRecordings;

	private final ConcurrentMap<Long, List<RESTDumperClient>> externalRecorders;

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

	private NetDumperOptions options;
    private volatile File[] recordedFiles;

    public NetDumperService(IConnectionManager connectionManager, IWorkspaceDispatcher workspaceDispatcher, IOptionsStorage optionsStorage) {

		this.connectionManager = connectionManager;
		this.workspaceDispatcher = workspaceDispatcher;
		this.optionsStorage = optionsStorage;
		this.serviceRecordings = new ConcurrentHashMap<>();
		this.externalRecorders = new ConcurrentHashMap<>();

		this.options = new NetDumperOptions();
	}

	public void init() {

		try {

            this.readWriteLock.writeLock().lock();
            this.options.fillFromMap(optionsStorage.getAllOptions());

		} catch (Exception e) {

			logger.error("Settings could not be read; connection to RESTDumper will fail");

        } finally {
            this.readWriteLock.writeLock().unlock();
		}

        this.connectionManager.subscribeForEvents(this);
	}

	@Override
	public void onEvent(ServiceEvent event) {

        try {

            this.readWriteLock.readLock().lock();

            if (!this.options.isEnabled()) {
                return;
            }

        } finally {
            this.readWriteLock.readLock().unlock();
        }

		if (event instanceof ServiceStatusUpdateEvent) {
			try {
				if (event.getType() == Type.STARTING) {

					handleStart(event.getServiceName(), event.getOccurred());

				} else {

                    if (event.getType() != Type.STARTING && event.getType() != Type.STARTED) {

                        List<RESTDumperClient> clients = this.serviceRecordings.remove(event.getServiceName());

                        if (clients != null) {
                            handleFinish(clients);
                        }
					}
				}
			}
			catch (Exception e) {
				logger.error("Error while starting NetDumper for {}", event.getServiceName(), e);
			}
		}
	}

    public NetDumperOptions getOptions() {
        try {
            this.readWriteLock.readLock().lock();
            return this.options.clone();
        } finally {
            this.readWriteLock.readLock().unlock();
        }
	}

	public File[] getRecordedFiles() throws IOException {
        if (this.recordedFiles == null) {
			updateFiles();
		}
        return this.recordedFiles;
	}

	private void updateFiles() throws IOException {
        this.recordedFiles = this.workspaceDispatcher.getFolder(FolderType.TRAFFIC_DUMP).listFiles();
	}

	public boolean checkAvailability() {

        String rootUrl;

        try {

            this.readWriteLock.readLock().lock();;
            rootUrl = this.options.getRootUrl();

        } finally {
            this.readWriteLock.readLock().unlock();
        }

		try {
            HttpGet req = new HttpGet(rootUrl + IS_OK);

            int code;

			req.setConfig(RequestConfig.custom()
				.setConnectTimeout(5000)
				.build());

            try (CloseableHttpClient http = HttpClients.custom().disableAutomaticRetries().build();
                    CloseableHttpResponse resp = http.execute(req)) {

                code = resp.getStatusLine().getStatusCode();
            }

			return code == 200;

        } catch (IOException e) {
			return false;
		}
	}

	public void applyOptions(NetDumperOptions options) {


		try {

            this.readWriteLock.writeLock().lock();
            this.options = options;

            for (Map.Entry<String, String> option : options.toMap().entrySet()) {
				this.optionsStorage.setOption(option.getKey(), option.getValue());
            }

		} catch (Exception e) {
			throw new RuntimeException(e);

        } finally {
            this.readWriteLock.writeLock().unlock();
		}
	}

    public void startRecording(long recordId, String[] services) {

        ServiceName[] svcNames = new ServiceName[services.length];

        for (int i = 0; i < services.length; i++) {
            svcNames[i] = new ServiceName(services[i]);
        }

        startRecording(recordId, svcNames);
    }

    public void startRecording(long recordId, ServiceName[] services) {
		try {
			List<RESTDumperClient> list = new ArrayList<>();
			for (ServiceName svcName : services) {
                list.addAll(startRecordingService(svcName, svcName.toString()));
			}
            this.externalRecorders.put(recordId, list);
		}
		catch (Exception e) {
			logger.error("An error occured while starting traffic recording for matrix", e);
		}
	}

	public void stopAndZip(long recordId, OutputStream out) {
		try {
			ZipOutputStream zip = new ZipOutputStream(out);
            for (RESTDumperClient cl : this.externalRecorders.get(recordId)) {
				zip.putNextEntry(new ZipEntry(cl.generateFilename()));
				cl.stopRecord(zip);
				zip.closeEntry();
			}
			zip.close();
            this.externalRecorders.remove(recordId);
		}
		catch (Exception e) {
			logger.error("An error occured while finishing traffic recording for matrix", e);
		}
	}

	public void stopAndStore(long recordId, File rootDir) {
		try {
            for (RESTDumperClient cl : this.externalRecorders.get(recordId)) {
				try (OutputStream out = new FileOutputStream(rootDir.getAbsolutePath() + File.separator + cl.generateFilename())) {
					cl.stopRecord(out);
				}
			}
            this.externalRecorders.remove(recordId);
		}
		catch (Exception e) {
			logger.error("An error occured while finishing traffic recording for matrix", e);
		}
	}

	private List<RESTDumperClient> startRecordingService(ServiceName svcName, String identifier) throws Exception {

        IServiceSettings svcSettings = this.connectionManager.getServiceDescription(svcName).getSettings();

        if (!svcSettings.isPerformDump()) {
			return new ArrayList<>();
		}

        Class<?> settingsClass = svcSettings.getClass();

        Map<Integer, CatchInfo> catchMap = new HashMap<>();

		for (Field fld : settingsClass.getDeclaredFields()) {

			if (fld.isAnnotationPresent(NetDumperListenHost.class)) {
                getCatchInfo(catchMap, fld.getAnnotation(NetDumperListenHost.class).index()).host = getProperty(svcSettings, fld);
			}

			if (fld.isAnnotationPresent(NetDumperListenPort.class)) {
                getCatchInfo(catchMap, fld.getAnnotation(NetDumperListenPort.class).index()).port = getProperty(svcSettings, fld);
			}

			if (fld.isAnnotationPresent(NetDumperListenInterface.class)) {
                getCatchInfo(catchMap, fld.getAnnotation(NetDumperListenInterface.class).index()).iface = getProperty(svcSettings, fld);
			}
		}

		List<RESTDumperClient> list = new ArrayList<>();

        String rootUrl;

        try {

            this.readWriteLock.readLock().lock();
            rootUrl = this.options.getRootUrl();

        } finally {
            this.readWriteLock.readLock().unlock();
        }

        for (Integer index : catchMap.keySet()) {

            CatchInfo catchInfo = catchMap.get(index);

            int port = Integer.valueOf(catchInfo.port);

            if (StringUtils.isEmpty(catchInfo.host) || port <= 0) {
                logger.error("Wrong RESTDumper host/port: {}:{}", catchInfo.host, catchInfo.port);
                continue;
            }

            logger.info("Starting RESTDumper for {}:{}", catchInfo.host, catchInfo.port);

            RESTDumperClient cl = new RESTDumperClient(rootUrl, catchInfo.host, port, catchInfo.iface, identifier);
            cl.start();
            list.add(cl);
		}

		return list;
	}

    private CatchInfo getCatchInfo(Map<Integer, CatchInfo> catchMap, int index) {
        if (!catchMap.containsKey(index)) {
            catchMap.put(index, new CatchInfo());
	    }
        return catchMap.get(index);
	}

    private String getProperty(IServiceSettings svcSettings, Field fld) throws Exception {
        return BeanUtils.getProperty(svcSettings, Introspector.decapitalize(fld.getName()));
    }

	private void handleStart(ServiceName svcName, Date startTime) throws Exception {
        this.serviceRecordings.put(svcName, startRecordingService(svcName, svcName.toString() + "." +
                dateFormat.get().format(startTime)));
	}

    private void handleFinish(List<RESTDumperClient> clients) throws Exception {

        for (RESTDumperClient cl : clients) {
            cl.stopRecord(this.workspaceDispatcher.createFile(FolderType.TRAFFIC_DUMP, false, cl.generateFilename()));
		}

		updateFiles();
	}

	@Override
	public void onEvent(EnvironmentEvent event) {
        // do nothing
	}

    private class CatchInfo {
        private String host;
        private String port;
        private String iface;
    }
}