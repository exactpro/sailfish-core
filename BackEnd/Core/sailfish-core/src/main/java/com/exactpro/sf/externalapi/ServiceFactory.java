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
package com.exactpro.sf.externalapi;

import static com.exactpro.sf.services.ServiceStatus.CREATED;
import static com.exactpro.sf.services.ServiceStatus.DISABLED;
import static com.exactpro.sf.services.ServiceStatus.DISPOSED;
import static com.exactpro.sf.services.ServiceStatus.ERROR;
import static com.exactpro.sf.services.ServiceStatus.INITIALIZED;
import static com.exactpro.sf.services.ServiceStatus.STARTED;
import static com.exactpro.sf.services.ServiceStatus.WARNING;

import java.beans.PropertyDescriptor;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.center.IDisposable;
import com.exactpro.sf.center.IVersion;
import com.exactpro.sf.center.impl.CoreVersion;
import com.exactpro.sf.center.impl.PluginLoader;
import com.exactpro.sf.common.impl.messages.StrictMessageWrapper;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.DataManager;
import com.exactpro.sf.configuration.DefaultLoggingConfiguration;
import com.exactpro.sf.configuration.DictionaryManager;
import com.exactpro.sf.configuration.IDictionaryRegistrator;
import com.exactpro.sf.configuration.ILoggingConfigurator;
import com.exactpro.sf.configuration.LoggingConfigurator;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.suri.SailfishURIException;
import com.exactpro.sf.configuration.workspace.DefaultWorkspaceDispatcherBuilder;
import com.exactpro.sf.configuration.workspace.DefaultWorkspaceLayout;
import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.configuration.workspace.IWorkspaceLayout;
import com.exactpro.sf.configuration.workspace.ResourceWorkspaceLayout;
import com.exactpro.sf.configuration.workspace.WorkspaceSecurityException;
import com.exactpro.sf.externalapi.impl.ServiceFactoryException;
import com.exactpro.sf.externalapi.impl.StrictDictionaryManager;
import com.exactpro.sf.scriptrunner.impl.EmptyServiceMonitor;
import com.exactpro.sf.scriptrunner.services.DefaultStaticServiceManager;
import com.exactpro.sf.scriptrunner.utilitymanager.UtilityManager;
import com.exactpro.sf.services.AbstractServiceHandler;
import com.exactpro.sf.services.DefaultServiceContext;
import com.exactpro.sf.services.EmptyStubServiceHandler;
import com.exactpro.sf.services.IInitiatorService;
import com.exactpro.sf.services.IService;
import com.exactpro.sf.services.IServiceContext;
import com.exactpro.sf.services.IServiceHandler;
import com.exactpro.sf.services.IServiceMonitor;
import com.exactpro.sf.services.IServiceSettings;
import com.exactpro.sf.services.ISession;
import com.exactpro.sf.services.ITaskExecutor;
import com.exactpro.sf.services.IdleStatus;
import com.exactpro.sf.services.ServiceDescription;
import com.exactpro.sf.services.ServiceEvent;
import com.exactpro.sf.services.ServiceHandlerException;
import com.exactpro.sf.services.ServiceHandlerRoute;
import com.exactpro.sf.services.ServiceMarshalManager;
import com.exactpro.sf.services.ServiceStatus;
import com.exactpro.sf.services.TaskExecutor;
import com.exactpro.sf.storage.EmptyServiceStorage;
import com.exactpro.sf.storage.IMessageStorage;
import com.exactpro.sf.storage.IServiceStorage;
import com.exactpro.sf.storage.impl.FakeMessageStorage;

public class ServiceFactory implements IServiceFactory {

    private static final Logger logger = LoggerFactory.getLogger(ServiceFactory.class);
    public static final String ROOT_PACKAGE = "com/exactpro/sf/workspace";

    private final IWorkspaceDispatcher wd;
    private final DefaultStaticServiceManager staticServiceManager;
    private final UtilityManager utilManager;
    private final DictionaryManager dictionaryManager;
    private final DataManager dataManager;
    private final ServiceMarshalManager marshalManager;
    private final IMessageFactoryProxy messageFactory;

    private final IServiceContext emptyServiceContext;
    private final IServiceMonitor emptyServiceMonitor = new EmptyServiceMonitor();
    private final IServiceHandler emptyServiceHandler = new EmptyStubServiceHandler();

    private final Set<SailfishURI> serviceTypes;

    private final Queue<IDisposable> disposables = Collections.asLifoQueue(new LinkedList<IDisposable>());

    /**
     * Creates ServiceFactory with default parameters.
     * @param workspaceLayers the sequence of workspace layers {@link com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher}
     * @throws IOException
     * @throws WorkspaceSecurityException
     * @throws SailfishURIException
     *
     * @see IWorkspaceDispatcher
     */
    public ServiceFactory(File... workspaceLayers) throws IOException, WorkspaceSecurityException, SailfishURIException {
        this(false, workspaceLayers);
    }

    /**
     * Creates ServiceFactory with default parameters.
     * @param workspaceLayers the sequence of workspace layers {@link com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher}
     * @param useResourceLayer if true resource layer '{@link #ROOT_PACKAGE}' will be plugged
     * @throws IOException
     * @throws WorkspaceSecurityException
     * @throws SailfishURIException
     *
     * @see IWorkspaceDispatcher
     */
    public ServiceFactory(boolean useResourceLayer, File... workspaceLayers) throws IOException, WorkspaceSecurityException, SailfishURIException {
        this(0, 350, Runtime.getRuntime().availableProcessors() * 2, useResourceLayer, false, workspaceLayers);
    }

    /**
     * Creates ServiceFactory.
     * @param minThreads the number of threads to keep in the pool, even
     *        if they are idle, unless {@code allowCoreThreadTimeOut} is set
     * @param maxThreads the maximum number of threads to allow in the pool
     * @param scheduledThreads the number of threads to keep in the pool,
     *        even if they are idle
     * @param useResourceLayer if true resource layer '{@link #ROOT_PACKAGE}' will be plugged
     * @param workspaceLayers the sequence of workspace layers {@link IWorkspaceDispatcher}
     * @throws IOException
     * @throws WorkspaceSecurityException
     * @throws SailfishURIException
     */
    public ServiceFactory(int minThreads, int maxThreads, int scheduledThreads, boolean useResourceLayer, File... workspaceLayers) throws IOException, WorkspaceSecurityException, SailfishURIException {
        this(minThreads, maxThreads, scheduledThreads, useResourceLayer, false, workspaceLayers);
    }

    /**
     * Creates ServiceFactory.
     * @param minThreads the number of threads to keep in the pool, even
     *        if they are idle, unless {@code allowCoreThreadTimeOut} is set
     * @param maxThreads the maximum number of threads to allow in the pool
     * @param scheduledThreads the number of threads to keep in the pool,
     *        even if they are idle
     * @param useResourceLayer if true resource layer '{@link #ROOT_PACKAGE}' will be plugged
     * @param useStrictMessages if true all creating messages in services will be instance of '{@link StrictMessageWrapper}'
     * @param workspaceLayers the sequence of workspace layers {@link IWorkspaceDispatcher}
     * @throws IOException
     * @throws WorkspaceSecurityException
     * @throws SailfishURIException
     */
    public ServiceFactory(int minThreads, int maxThreads, int scheduledThreads, boolean useResourceLayer, boolean useStrictMessages, File... workspaceLayers) throws IOException, WorkspaceSecurityException, SailfishURIException {
        this.wd = createWorkspaceDispatcher(useResourceLayer, workspaceLayers);
        this.staticServiceManager = new DefaultStaticServiceManager();
        this.utilManager = new UtilityManager();
        if (useStrictMessages) {
            this.dictionaryManager = new StrictDictionaryManager(wd, utilManager);
        } else {
            this.dictionaryManager = new DictionaryManager(wd, utilManager);
        }
        this.dataManager = new DataManager(wd);

        IVersion coreVersion = new CoreVersion();
        PluginLoader pluginLoader = new PluginLoader(wd, staticServiceManager, null,
                dictionaryManager, null, null, null, dataManager, null, null, null, null,
                coreVersion);
        pluginLoader.load();

        this.marshalManager = new ServiceMarshalManager(staticServiceManager, dictionaryManager);
        ILoggingConfigurator loggingConfigurator = new LoggingConfigurator(wd, new DefaultLoggingConfiguration());
        ITaskExecutor taskExecutor = new TaskExecutor(minThreads, maxThreads, scheduledThreads);
        disposables.add(taskExecutor);
        IMessageStorage emptyMessageStorage = new FakeMessageStorage();
        IServiceStorage emptyServiceStorage = new EmptyServiceStorage();
        this.messageFactory = new DictionaryMessageFactory(this.dictionaryManager);

        this.emptyServiceContext = new DefaultServiceContext(dictionaryManager, emptyMessageStorage, emptyServiceStorage, loggingConfigurator, taskExecutor, dataManager, wd);

        this.serviceTypes = new HashSet<>(Arrays.asList(this.staticServiceManager.getServiceURIs()));
    }

    /**
     * Creates ServiceFactory.
     * @param minThreads the number of threads to keep in the pool, even
     *        if they are idle, unless {@code allowCoreThreadTimeOut} is set
     * @param maxThreads the maximum number of threads to allow in the pool
     * @param scheduledThreads the number of threads to keep in the pool,
     *        even if they are idle
     * @param workspaceLayers the sequence of workspace layers {@link IWorkspaceDispatcher}
     * @throws IOException
     * @throws WorkspaceSecurityException
     * @throws SailfishURIException
     */
    public ServiceFactory(int minThreads, int maxThreads, int scheduledThreads, File... workspaceLayers) throws IOException, WorkspaceSecurityException, SailfishURIException {
        this(minThreads, maxThreads, scheduledThreads, false, false, workspaceLayers);
    }

    @Override
    public void close() throws Exception {
        while (!this.disposables.isEmpty()) {
            try {
                this.disposables.remove().dispose();
            } catch (RuntimeException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    @Override
    public IServiceProxy createService(InputStream setting, final IServiceListener listener) throws ServiceFactoryException {
        List<ServiceDescription> importResults = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try {
            this.marshalManager.unmarshalServices(setting, false, importResults, errors);
        } catch (RuntimeException e) {
            throw new ServiceFactoryException("Problem during service configuration reading", e);
        }
        if (importResults.size() > 1 || errors.size() > 1 || importResults.size() + errors.size() > 1) {
            throw new IllegalArgumentException("Stream should contains only one settings set");
        }
        if (!errors.isEmpty() || importResults.size() != 1) {
            throw new ServiceFactoryException("Unmarshalling the settings set failed, reason " + errors);
        }
        final ServiceDescription desc = importResults.get(0);
        ServiceSettingsProxy settingsProxy = new ServiceSettingsProxy(desc.getSettings());
        return createService(desc.getType(), settingsProxy, ServiceName.parse(desc.getName()), listener);
    }

    @Override
    public IServiceProxy createService(ServiceName name, SailfishURI serviceType, IServiceListener listener) throws ServiceFactoryException {
        return createService(serviceType, null, name, listener);
    }

    @Override
    public Set<SailfishURI> getServiceTypes() {

        return serviceTypes;
    }

    @Override
    public Set<SailfishURI> getDictionaries() {
        return this.dictionaryManager.getDictionaryURIs();
    }

    @Override
    public IMessageFactoryProxy getMessageFactory(SailfishURI serviceType) {
        if (!serviceTypes.contains(serviceType)) {
            throw new IllegalArgumentException("Unknown service type " + serviceType);
        }
        return this.messageFactory;
    }

    @Override
    public SailfishURI registerDictionary(String title, InputStream dictionary, boolean overwrite) throws ServiceFactoryException  {
        try {
            IDictionaryRegistrator registrator = dictionaryManager.registerDictionary(title, overwrite);
            try (OutputStream os = new BufferedOutputStream(new FileOutputStream(this.wd.getFile(FolderType.ROOT, registrator.getPath())))) {
                IOUtils.copy(dictionary, os);
            }
            return registrator.registrate();
        } catch (RuntimeException e) {
            throw new ServiceFactoryException(String.format("Could not create dictionary '%s'", title), e);
        } catch (IOException e) {
            throw new ServiceFactoryException(String.format("Could not create dictionary file '%s'", title + ".xml"), e);
        }
    }

    private IWorkspaceDispatcher createWorkspaceDispatcher(boolean useResourceLayer, File... layers) throws IOException {

        DefaultWorkspaceDispatcherBuilder builder = new DefaultWorkspaceDispatcherBuilder();
        if (useResourceLayer) {
            IWorkspaceLayout workspaceLayout = new ResourceWorkspaceLayout(ROOT_PACKAGE);
            builder.addWorkspaceLayer(new File(workspaceLayout.getPath(null, FolderType.ROOT)), workspaceLayout);
        }

        for (File layer : layers) {
            builder.addWorkspaceLayer(layer, DefaultWorkspaceLayout.getInstance());
        }

        return builder.build(true);
    }

    private IServiceProxy createService(SailfishURI serviceType, ServiceSettingsProxy settingsProxy, ServiceName name, IServiceListener listener)
            throws ServiceFactoryException {
        if (!serviceTypes.contains(serviceType)) {
            throw new IllegalArgumentException("Unknown service type " + serviceType);
        }

        IInitiatorService service;
        try {
            IService tmp = this.staticServiceManager.createService(serviceType);
            if (!(tmp instanceof IInitiatorService)) {
                throw new ServiceFactoryException("Only IInitiator service supported");
            }
            service = (IInitiatorService) tmp;
        } catch (RuntimeException e) {
            throw new ServiceFactoryException(String.format("Failed to instantiate service with SailfishURI %s", serviceType), e);
        }


        if (DISABLED == service.getStatus()) {
            throw new ServiceFactoryException(
                    String.format("Could not create service %s. Target service '%s' is not presented in plug-ins", serviceType, serviceType));
        }

        if (settingsProxy == null) {
            IServiceSettings settings = this.staticServiceManager.createServiceSettings(serviceType);
            settingsProxy = new ServiceSettingsProxy(settings);
        }

        return new ServiceProxy(name, service, serviceType, listener, settingsProxy, emptyServiceHandler, emptyServiceMonitor, emptyServiceContext);
    }

    private static class MessageStorageProxy extends FakeMessageStorage {

        private final ServiceProxy serviceProxy;
        private final IServiceListener listener;

        public MessageStorageProxy(ServiceProxy serviceProxy, IServiceListener listener) {
            this.serviceProxy = serviceProxy;
            this.listener = listener;
        }

        @Override
        public void storeMessage(IMessage message) {
            super.storeMessage(message);
            if (message.getMetaData().isRejected()) {
                ServiceHandlerRoute route = message.getMetaData().isAdmin() ? ServiceHandlerRoute.FROM_ADMIN : ServiceHandlerRoute.FROM_APP;
                this.listener.onMessage(serviceProxy, message, true, route);
            }
        }
    }

    private static class DictionaryMessageFactory implements IMessageFactoryProxy {

        private final DictionaryManager dictionaryManager;

        public DictionaryMessageFactory(DictionaryManager dictionaryManager) {
            this.dictionaryManager = dictionaryManager;
        }

        @Override
        public IMessage createMessage(SailfishURI dictionary, String name) {

            if (!dictionaryManager.getDictionaryURIs().contains(dictionary)) {
                throw new EPSCommonException(String.format("Dictionary %s not found", dictionary.getResourceName()));
            }

            IDictionaryStructure structure = dictionaryManager.getDictionary(dictionary);
            return dictionaryManager.getMessageFactory(dictionary).createMessage(name, structure.getNamespace());
        }

        @Override
        public IMessage createStrictMessage(SailfishURI dictionary, String name) {
            if (!dictionaryManager.getDictionaryURIs().contains(dictionary)) {
                throw new EPSCommonException(String.format("Dictionary %s not found", dictionary.getResourceName()));
            }

            IDictionaryStructure structure = dictionaryManager.getDictionary(dictionary);
            IMessageStructure messageStructure = structure.getMessageStructure(name);
            
            if (messageStructure == null) {
                throw new EPSCommonException(String.format("Message %s not found in dictionary %s", name, dictionary.getResourceName()));
            }
            
            return new StrictMessageWrapper(dictionaryManager.getMessageFactory(dictionary), messageStructure);
        }

    }

    private static class ListenerServiceMonitor implements IServiceMonitor {

        private final IServiceListener listener;
        private final IServiceProxy service;

        public ListenerServiceMonitor(IServiceProxy service, IServiceListener listener) {
            this.service = service;
            this.listener = listener;
        }

        @Override
        public void onEvent(ServiceEvent event) {
            this.listener.onEvent(this.service, event);
        }

    };

    private static class IntServiceHandler extends AbstractServiceHandler {

        private final IServiceListener listener;
        private final IServiceProxy service;

        public IntServiceHandler(IServiceProxy service, IServiceListener listener) {
            this.service = service;
            this.listener = listener;
        }

        @Override
        public void sessionOpened(ISession session) throws ServiceHandlerException {
            listener.sessionOpened(this.service);
        }

        @Override
        public void sessionIdle(ISession session, IdleStatus status) throws ServiceHandlerException {
            listener.sessionIdle(this.service, status);
        }

        @Override
        public void sessionClosed(ISession session) throws ServiceHandlerException {
            listener.sessionClosed(this.service);
        }

        @Override
        public void putMessage(ISession session, ServiceHandlerRoute route, IMessage message) throws ServiceHandlerException {
            listener.onMessage(this.service, message, false, route);
        }

        @Override
        public void exceptionCaught(ISession session, Throwable cause) {
            listener.exceptionCaught(this.service, cause);
        }
    };

    private static class ServiceProxy implements IServiceProxy {

        private final IServiceContext serviceContext;
        private final IInitiatorService service;
        private final SailfishURI sURI;
        private final ServiceSettingsProxy settingsProxy;
        private final ServiceName name;
        private final IServiceHandler serviceHandler;
        private final IServiceMonitor serviceMonitor;

        public ServiceProxy(ServiceName name, IInitiatorService service, SailfishURI sURI, IServiceListener listener,
                ServiceSettingsProxy serviceSettingsProxy, IServiceHandler defaultHandler, IServiceMonitor defaultMonitor,
                IServiceContext defaultServiceContext) {
            this.name = name;
            this.service = service;
            this.sURI = sURI;
            this.settingsProxy = serviceSettingsProxy;
            if (listener != null) {
                IMessageStorage messageStorage = new MessageStorageProxy(this, listener);
                IServiceStorage serviceStorage = new EmptyServiceStorage();
                this.serviceHandler = new IntServiceHandler(this, listener);
                this.serviceMonitor = new ListenerServiceMonitor(this, listener);
                this.serviceContext = new DefaultServiceContext(defaultServiceContext, messageStorage, serviceStorage);
            } else {
                this.serviceHandler = defaultHandler;
                this.serviceMonitor = defaultMonitor;
                this.serviceContext = defaultServiceContext;
            }
        }

        @Override
        public void stop() {

            if (STARTED.equals(service.getStatus()) || WARNING.equals(service.getStatus())) {
                service.dispose();
            } else {
                throw new IllegalStateException(String.format("Service %s not started", this.name));
            }
        }

        @Override
        public void start() {
            if (checkServiceState(service.getStatus(), ERROR, CREATED, DISPOSED, INITIALIZED)) {
                this.service.init(this.serviceContext, this.serviceMonitor, this.serviceHandler, this.settingsProxy.getSettings(), this.name);
                service.start();
            } else {
                throw new IllegalStateException(String.format("Service in illegal statate: %s", service.getStatus()));
            }
        }

        @Override
        public IMessage send(IMessage message) throws InterruptedException {

            if (STARTED.equals(service.getStatus()) || WARNING.equals(service.getStatus())) {
                IInitiatorService xService = service;
                xService.getSession().send(message);
            } else {
                throw new IllegalStateException("Service not started");
            }

            return message;
        }

        @Override
        public ServiceStatus getStatus() {
            return service.getStatus();
        }

        @Override
        public ServiceName getName() {
            return name;
        }

        @Override
        public SailfishURI getType() {
            return sURI;
        }

        @Override
        public ISettingsProxy getSettings() {

            return this.settingsProxy;
        }

        private boolean checkServiceState(ServiceStatus actual, ServiceStatus... expected) {
            return ArrayUtils.contains(expected, actual);
        }
    }

    private static class ServiceSettingsProxy extends AbstractSettingsProxy{

        public ServiceSettingsProxy(IServiceSettings settings) {
            super(settings);

        }

        public IServiceSettings getSettings() {
            try {
                IServiceSettings result = (IServiceSettings) this.settings.getClass().newInstance();

                Object value = null;
                for (PropertyDescriptor descriptor : descriptors.values()) {
                    value = descriptor.getReadMethod().invoke(this.settings);
                    descriptor.getWriteMethod().invoke(result, value);
                }

                return result;
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                throw new EPSCommonException("Could not create a copy of settings", e);
            }
        }

        @Override
        public SailfishURI getDictionary() {
            return ((IServiceSettings) this.settings).getDictionaryName();
        }

        @Override
        public void setDictionary(SailfishURI dictionary) {
            ((IServiceSettings) this.settings).setDictionaryName(dictionary);
        }
    }

}
