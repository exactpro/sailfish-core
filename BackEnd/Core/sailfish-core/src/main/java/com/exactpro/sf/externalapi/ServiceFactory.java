/*
 * Copyright 2009-2021 Exactpro (Exactpro Systems Limited)
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
 */
package com.exactpro.sf.externalapi;

import static com.exactpro.sf.configuration.suri.SailfishURIRule.REQUIRE_RESOURCE;
import static com.exactpro.sf.configuration.suri.SailfishURIUtils.getSingleMatchingURI;
import static com.exactpro.sf.externalapi.DictionaryType.MAIN;
import static com.exactpro.sf.services.ServiceStatus.CREATED;
import static com.exactpro.sf.services.ServiceStatus.DISABLED;
import static com.exactpro.sf.services.ServiceStatus.DISPOSED;
import static com.exactpro.sf.services.ServiceStatus.ERROR;
import static com.exactpro.sf.services.ServiceStatus.INITIALIZED;
import static com.exactpro.sf.services.ServiceStatus.STARTED;
import static com.exactpro.sf.services.ServiceStatus.WARNING;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.reflect.FieldUtils.getFieldsListWithAnnotation;

import java.beans.PropertyDescriptor;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.IMetadata;
import com.exactpro.sf.common.messages.MetadataProperty;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.DataManager;
import com.exactpro.sf.configuration.DefaultLoggingConfiguration;
import com.exactpro.sf.configuration.DictionaryManager;
import com.exactpro.sf.configuration.IDictionaryRegistrator;
import com.exactpro.sf.configuration.ILoggingConfiguration;
import com.exactpro.sf.configuration.ILoggingConfigurator;
import com.exactpro.sf.configuration.LoggingConfigurator;
import com.exactpro.sf.configuration.dictionary.DictionaryValidationError;
import com.exactpro.sf.configuration.dictionary.impl.DefaultDictionaryValidator;
import com.exactpro.sf.configuration.dictionary.interfaces.IDictionaryValidator;
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
import com.exactpro.sf.externalapi.impl.StrictMessageFactoryWrapper;
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
import com.google.common.collect.Sets;

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

    private final Queue<IDisposable> disposables = Collections.asLifoQueue(new LinkedList<>());

    /**
     * Creates {@link ServiceFactoryBuilder} with specified workspace layers
     *
     * @param workspaceLayers workspace layers to be used
     * @return {@link ServiceFactoryBuilder} initialized with provided {@code workspaceLayers}
     */
    public static ServiceFactoryBuilder builder(File... workspaceLayers) {
        return new ServiceFactoryBuilder(workspaceLayers);
    }

    /**
     * Creates ServiceFactory with default parameters.
     *
     * @param workspaceLayers the sequence of workspace layers {@link IWorkspaceDispatcher}
     * @throws IOException
     * @throws WorkspaceSecurityException
     * @throws SailfishURIException
     * @see IWorkspaceDispatcher
     * @deprecated use {@link ServiceFactory#builder} instead
     */
    @Deprecated
    public ServiceFactory(File... workspaceLayers) throws IOException, WorkspaceSecurityException, SailfishURIException {
        this(false, workspaceLayers);
    }

    /**
     * Creates ServiceFactory with default parameters.
     *
     * @param workspaceLayers  the sequence of workspace layers {@link IWorkspaceDispatcher}
     * @param useResourceLayer if true resource layer '{@link #ROOT_PACKAGE}' will be plugged
     * @throws IOException
     * @throws WorkspaceSecurityException
     * @throws SailfishURIException
     * @see IWorkspaceDispatcher
     * @deprecated use {@link ServiceFactory#builder} instead
     */
    @Deprecated
    public ServiceFactory(boolean useResourceLayer, File... workspaceLayers) throws IOException, WorkspaceSecurityException, SailfishURIException {
        this(0, 350, Runtime.getRuntime().availableProcessors() * 2, useResourceLayer, false, workspaceLayers);
    }

    /**
     * Creates ServiceFactory.
     *
     * @param minThreads       the number of threads to keep in the pool, even
     *                         if they are idle, unless {@code allowCoreThreadTimeOut} is set
     * @param maxThreads       the maximum number of threads to allow in the pool
     * @param scheduledThreads the number of threads to keep in the pool,
     *                         even if they are idle
     * @param useResourceLayer if true resource layer '{@link #ROOT_PACKAGE}' will be plugged
     * @param workspaceLayers  the sequence of workspace layers {@link IWorkspaceDispatcher}
     * @throws IOException
     * @throws WorkspaceSecurityException
     * @throws SailfishURIException
     * @deprecated use {@link ServiceFactory#builder} instead
     */
    @Deprecated
    public ServiceFactory(int minThreads, int maxThreads, int scheduledThreads, boolean useResourceLayer, File... workspaceLayers) throws IOException, WorkspaceSecurityException, SailfishURIException {
        this(minThreads, maxThreads, scheduledThreads, useResourceLayer, false, workspaceLayers);
    }

    /**
     * Creates ServiceFactory.
     *
     * @param minThreads        the number of threads to keep in the pool, even
     *                          if they are idle, unless {@code allowCoreThreadTimeOut} is set
     * @param maxThreads        the maximum number of threads to allow in the pool
     * @param scheduledThreads  the number of threads to keep in the pool,
     *                          even if they are idle
     * @param useResourceLayer  if true resource layer '{@link #ROOT_PACKAGE}' will be plugged
     * @param useStrictMessages if true all creating messages in services will be instance of '{@link StrictMessageWrapper}'
     * @param workspaceLayers   the sequence of workspace layers {@link IWorkspaceDispatcher}
     * @throws IOException
     * @throws WorkspaceSecurityException
     * @throws SailfishURIException
     * @deprecated use {@link ServiceFactory#builder} instead
     */
    @Deprecated
    public ServiceFactory(int minThreads, int maxThreads, int scheduledThreads, boolean useResourceLayer, boolean useStrictMessages, File... workspaceLayers) throws IOException, WorkspaceSecurityException, SailfishURIException {
        this(minThreads, maxThreads, scheduledThreads, useResourceLayer, useStrictMessages, true, workspaceLayers);
    }

    /**
     * Creates ServiceFactory.
     *
     * @param minThreads             the number of threads to keep in the pool, even
     *                               if they are idle, unless {@code allowCoreThreadTimeOut} is set
     * @param maxThreads             the maximum number of threads to allow in the pool
     * @param scheduledThreads       the number of threads to keep in the pool,
     *                               even if they are idle
     * @param useResourceLayer       if true resource layer '{@link #ROOT_PACKAGE}' will be plugged
     * @param useStrictMessages      if true all creating messages in services will be an instance of '{@link StrictMessageWrapper}'
     * @param useServiceAppenders enable individual appender that stores service logs into a dedicated file for each running service
     * @param workspaceLayers        the sequence of workspace layers {@link IWorkspaceDispatcher}
     * @throws IOException
     * @throws WorkspaceSecurityException
     * @throws SailfishURIException
     */
    ServiceFactory(
            int minThreads,
            int maxThreads,
            int scheduledThreads,
            boolean useResourceLayer,
            boolean useStrictMessages,
            boolean useServiceAppenders,
            File... workspaceLayers
    ) throws IOException, WorkspaceSecurityException, SailfishURIException {
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
                null, coreVersion, null, null, null);
        pluginLoader.load();

        this.marshalManager = new ServiceMarshalManager(staticServiceManager, dictionaryManager);
        ILoggingConfiguration loggingConfiguration = new DefaultLoggingConfiguration();
        loggingConfiguration.setAppendersEnabled(useServiceAppenders);
        ILoggingConfigurator loggingConfigurator = new LoggingConfigurator(wd, loggingConfiguration);

        ITaskExecutor taskExecutor = new TaskExecutor(minThreads, maxThreads, scheduledThreads);
        disposables.add(taskExecutor);
        IMessageStorage emptyMessageStorage = new FakeMessageStorage();
        IServiceStorage emptyServiceStorage = new EmptyServiceStorage();
        this.messageFactory = new DictionaryMessageFactory(dictionaryManager);

        this.emptyServiceContext = new DefaultServiceContext(dictionaryManager, emptyMessageStorage, emptyServiceStorage, loggingConfigurator, taskExecutor, dataManager, wd);

        this.serviceTypes = new HashSet<>(Arrays.asList(staticServiceManager.getServiceURIs()));
    }

    /**
     * Creates ServiceFactory.
     *
     * @param minThreads       the number of threads to keep in the pool, even
     *                         if they are idle, unless {@code allowCoreThreadTimeOut} is set
     * @param maxThreads       the maximum number of threads to allow in the pool
     * @param scheduledThreads the number of threads to keep in the pool,
     *                         even if they are idle
     * @param workspaceLayers  the sequence of workspace layers {@link IWorkspaceDispatcher}
     * @throws IOException
     * @throws WorkspaceSecurityException
     * @throws SailfishURIException
     * @deprecated use {@link ServiceFactoryBuilder} instead
     */
    @Deprecated
    public ServiceFactory(int minThreads, int maxThreads, int scheduledThreads, File... workspaceLayers) throws IOException, WorkspaceSecurityException, SailfishURIException {
        this(minThreads, maxThreads, scheduledThreads, false, false, workspaceLayers);
    }

    @Override
    public void close() throws Exception {
        while (!disposables.isEmpty()) {
            try {
                disposables.remove().dispose();
            } catch (RuntimeException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    @Override
    public IServiceProxy createService(InputStream setting, IServiceListener listener) throws ServiceFactoryException {
        List<ServiceDescription> importResults = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try {
            marshalManager.unmarshalServices(setting, false, importResults, errors);
        } catch (RuntimeException e) {
            throw new ServiceFactoryException("Problem during service configuration reading", e);
        }
        if (importResults.size() > 1 || errors.size() > 1 || importResults.size() + errors.size() > 1) {
            throw new IllegalArgumentException("Stream should contains only one settings set");
        }
        if (!errors.isEmpty() || importResults.size() != 1) {
            throw new ServiceFactoryException("Unmarshalling the settings set failed, reason " + errors);
        }
        ServiceDescription desc = importResults.get(0);
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
        return dictionaryManager.getDictionaryURIs();
    }

    @Override
    public IDictionaryStructure getDictionary(SailfishURI uri) {
        return dictionaryManager.getDictionary(uri);
    }

    @Override
    public IMessageFactoryProxy getMessageFactory(SailfishURI serviceType) {
        getSingleMatchingURI(serviceType, serviceTypes, "service", REQUIRE_RESOURCE);
        return messageFactory;
    }

    @Override
    public SailfishURI registerDictionary(String title, InputStream dictionary, boolean overwrite) throws ServiceFactoryException {
        try {
            IDictionaryRegistrator registrator = dictionaryManager.registerDictionary(title, overwrite);
            try (OutputStream os = new BufferedOutputStream(new FileOutputStream(wd.getFile(FolderType.ROOT, registrator.getPath())))) {
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
        getSingleMatchingURI(serviceType, serviceTypes, "service", REQUIRE_RESOURCE);

        IInitiatorService service;
        IDictionaryValidator dictionaryValidator;
        try {
            IService tmp = staticServiceManager.createService(serviceType);
            dictionaryValidator = staticServiceManager.createDictionaryValidator(serviceType);
            if (!(tmp instanceof IInitiatorService)) {
                throw new ServiceFactoryException("Only IInitiator service supported");
            }
            service = (IInitiatorService) tmp;
        } catch (RuntimeException e) {
            throw new ServiceFactoryException(String.format("Failed to instantiate service with SailfishURI %s", serviceType), e);
        }

        if (service.getStatus() == DISABLED) {
            throw new ServiceFactoryException(
                    String.format("Could not create service %s. Target service '%s' is not presented in plug-ins", serviceType, serviceType));
        }

        if (settingsProxy == null) {
            IServiceSettings settings = staticServiceManager.createServiceSettings(serviceType);
            settingsProxy = new ServiceSettingsProxy(settings);
        }

        return new ServiceProxy(name, service, dictionaryValidator, serviceType, listener, settingsProxy, emptyServiceHandler, emptyServiceMonitor, emptyServiceContext);
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
                listener.onMessage(serviceProxy, message, true, route);
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
            getSingleMatchingURI(dictionary, dictionaryManager.getDictionaryURIs(), "dictionary", REQUIRE_RESOURCE);
            IDictionaryStructure structure = dictionaryManager.getDictionary(dictionary);
            return dictionaryManager.getMessageFactory(dictionary).createMessage(name, structure.getNamespace());
        }

        @Override
        public IMessage createStrictMessage(SailfishURI dictionary, String name) {
            getSingleMatchingURI(dictionary, dictionaryManager.getDictionaryURIs(), "dictionary", REQUIRE_RESOURCE);
            IDictionaryStructure dictionaryStructure = dictionaryManager.getDictionary(dictionary);
            IMessageFactory messageFactory = dictionaryManager.getMessageFactory(dictionary);

            return new StrictMessageFactoryWrapper(messageFactory, dictionaryStructure)
                    .createMessage(name);
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
            listener.onEvent(service, event);
        }

    }

    private static class IntServiceHandler extends AbstractServiceHandler {

        private final IServiceListener listener;
        private final IServiceProxy service;

        public IntServiceHandler(IServiceProxy service, IServiceListener listener) {
            this.service = service;
            this.listener = listener;
        }

        @Override
        public void sessionOpened(ISession session) throws ServiceHandlerException {
            listener.sessionOpened(service);
        }

        @Override
        public void sessionIdle(ISession session, IdleStatus status) throws ServiceHandlerException {
            listener.sessionIdle(service, status);
        }

        @Override
        public void sessionClosed(ISession session) throws ServiceHandlerException {
            listener.sessionClosed(service);
        }

        @Override
        public void putMessage(ISession session, ServiceHandlerRoute route, IMessage message) throws ServiceHandlerException {
            listener.onMessage(service, message, false, route);
        }

        @Override
        public void exceptionCaught(ISession session, Throwable cause) {
            listener.exceptionCaught(service, cause);
        }
    }

    private static class ServiceProxy implements IServiceProxy {
        private static final Set<String> PROHIBITED_METADATA_KEYS = Stream.of(MetadataProperty.values())
                .map(MetadataProperty::getPropertyName)
                .collect(Collectors.toSet());

        private final IServiceContext serviceContext;
        private final IInitiatorService service;
        private final IDictionaryValidator dictionaryValidator;
        private final SailfishURI sURI;
        private final ServiceSettingsProxy settingsProxy;
        private final ServiceName name;
        private final IServiceHandler serviceHandler;
        private final IServiceMonitor serviceMonitor;

        public ServiceProxy(ServiceName name, IInitiatorService service, IDictionaryValidator dictionaryValidator, SailfishURI sURI, IServiceListener listener,
                            ServiceSettingsProxy serviceSettingsProxy, IServiceHandler defaultHandler, IServiceMonitor defaultMonitor,
                            IServiceContext defaultServiceContext) {
            this.name = requireNonNull(name, "'Name' can't be null");
            this.service = requireNonNull(service, "'Service' can't be null");
            this.dictionaryValidator = requireNonNull(dictionaryValidator, "'Dictionary validator' can't be null");
            this.sURI = requireNonNull(sURI, "'Service URI' can't be null");
            this.settingsProxy = requireNonNull(serviceSettingsProxy, "'Settings' can't be null");
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

            if (service.getStatus() == STARTED || service.getStatus() == WARNING) {
                service.dispose();
            } else {
                throw new IllegalStateException(String.format("Service %s not started", name));
            }
        }

        @Override
        public void start() {
            if (checkServiceState(service.getStatus(), ERROR, CREATED, DISPOSED, INITIALIZED)) {
                validateDictionary();

                service.init(serviceContext, serviceMonitor, serviceHandler, settingsProxy.getSettings(), name);
                service.start();
            } else {
                throw new IllegalStateException(String.format("Service in illegal statate: %s", service.getStatus()));
            }
        }

        private void validateDictionary() {
            if (dictionaryValidator != null) {
                for (DictionaryType dictionaryType : settingsProxy.getDictionaryTypes()) {
                    IDictionaryValidator validator = dictionaryType == MAIN ? dictionaryValidator : DefaultDictionaryValidator.INSTANCE;
                    SailfishURI dictionaryURI = settingsProxy.getDictionary(MAIN);

                    if (dictionaryURI != null) {
                        IDictionaryStructure dictionary = serviceContext.getDictionaryManager().getDictionary(dictionaryURI);
                        List<DictionaryValidationError> errors = validator.validate(dictionary, true, null);

                        if (isNotEmpty(errors)) {
                            throw new IllegalStateException(errors.stream()
                                    .map(Objects::toString)
                                    .collect(Collectors.joining("\n", "Got following errors during '" + dictionaryType + "' dictionary validation:\n", "")));
                        }
                    }
                }
            }
        }

        @Override
        public IMessage send(IMessage message) throws InterruptedException {

            ISession session = getInternalSession();
            session.send(message);

            return message;
        }

        @Override
        public void sendRaw(byte[] rawData, IMetadata extraMetadata) throws InterruptedException {
            validateMetadataKeys(extraMetadata);
            ISession session = getInternalSession();
            session.sendRaw(rawData, extraMetadata);
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

            return settingsProxy;
        }

        private ISession getInternalSession() {
            if (service.getStatus() == STARTED || service.getStatus() == WARNING) {
                return service.getSession();
            }
            throw new IllegalStateException("Service is not started");
        }

        private void validateMetadataKeys(IMetadata extraMetadata) {
            Set<String> intersection = Sets.intersection(extraMetadata.getKeys(), PROHIBITED_METADATA_KEYS);
            if (!intersection.isEmpty()) {
                throw new IllegalArgumentException("'extraMetadata' contains prohibited keys: " + intersection);
            }
        }

        private boolean checkServiceState(ServiceStatus actual, ServiceStatus... expected) {
            return ArrayUtils.contains(expected, actual);
        }
    }

    private static class ServiceSettingsProxy extends AbstractSettingsProxy {
        protected final Map<DictionaryType, String> dictionaryProperties = new EnumMap<>(DictionaryType.class);

        public ServiceSettingsProxy(IServiceSettings settings) {
            super(settings);

            for (Field field : getFieldsListWithAnnotation(settings.getClass(), DictionaryProperty.class)) {
                String name = field.getName();

                if (!descriptors.containsKey(name)) {
                    throw new IllegalStateException("Field is not a property:" + name);
                }

                if (field.getType() != SailfishURI.class) {
                    throw new IllegalStateException("Invalid dictionary property type: " + field.getType().getCanonicalName());
                }

                if (dictionaryProperties.containsValue(name)) {
                    continue; // ignore superclass annotation values (subclass fields are processed first)
                }

                DictionaryType type = field.getAnnotation(DictionaryProperty.class).type();

                if (dictionaryProperties.put(type, name) != null) {
                    throw new IllegalStateException("Duplicate dictionary property type: " + type);
                }
            }
        }

        public IServiceSettings getSettings() {
            try {
                IServiceSettings result = (IServiceSettings) settings.getClass().newInstance();

                Object value = null;
                for (PropertyDescriptor descriptor : descriptors.values()) {
                    value = descriptor.getReadMethod().invoke(settings);
                    descriptor.getWriteMethod().invoke(result, value);
                }

                return result;
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException |
                     InvocationTargetException e) {
                throw new EPSCommonException("Could not create a copy of settings", e);
            }
        }

        @Override
        public Set<DictionaryType> getDictionaryTypes() {
            return Collections.unmodifiableSet(dictionaryProperties.keySet());
        }

        @Override
        public SailfishURI getDictionary(DictionaryType dictionaryType) {
            String propertyName = requireNonNull(dictionaryProperties.get(dictionaryType), () -> "No dictionary property with type: " + dictionaryType);
            return getParameterValue(propertyName);
        }

        @Override
        public void setDictionary(DictionaryType dictionaryType, SailfishURI dictionaryUri) {
            String propertyName = requireNonNull(dictionaryProperties.get(dictionaryType), () -> "No dictionary property with type: " + dictionaryType);
            setParameterValue(propertyName, dictionaryUri);
        }
    }
}
