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
package com.exactpro.sf.scriptrunner.impl;

import static com.exactpro.sf.storage.util.ServiceStorageHelper.copySettings;
import static java.lang.String.format;
import static java.util.Arrays.stream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.google.common.collect.Streams;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.common.util.BeanConfigurator;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.dictionary.DictionaryValidationError;
import com.exactpro.sf.configuration.dictionary.interfaces.IDictionaryValidator;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.suri.SailfishURIException;
import com.exactpro.sf.scriptrunner.IConnectionManager;
import com.exactpro.sf.scriptrunner.IEnvironmentListener;
import com.exactpro.sf.scriptrunner.IServiceNotifyListener;
import com.exactpro.sf.scriptrunner.services.IServiceFactory;
import com.exactpro.sf.services.ChangeEnvironmentEvent;
import com.exactpro.sf.services.ChangeEnvironmentEvent.Status;
import com.exactpro.sf.services.CollectorServiceHandler;
import com.exactpro.sf.services.DefaultServiceContext;
import com.exactpro.sf.services.EnvironmentEvent;
import com.exactpro.sf.services.FilterServiceHandlerWrapper;
import com.exactpro.sf.services.IEnvironmentMonitor;
import com.exactpro.sf.services.IService;
import com.exactpro.sf.services.IServiceContext;
import com.exactpro.sf.services.IServiceHandler;
import com.exactpro.sf.services.IServiceSettings;
import com.exactpro.sf.services.OptionalServiceHandlerWrapper;
import com.exactpro.sf.services.ServiceDescription;
import com.exactpro.sf.services.ServiceEvent;
import com.exactpro.sf.services.ServiceEvent.Level;
import com.exactpro.sf.services.ServiceEvent.Type;
import com.exactpro.sf.services.ServiceEventFactory;
import com.exactpro.sf.services.ServiceException;
import com.exactpro.sf.services.ServiceHandlerRoute;
import com.exactpro.sf.services.ServiceStatus;
import com.exactpro.sf.services.util.ServiceUtil;
import com.exactpro.sf.storage.IEnvironmentStorage;
import com.exactpro.sf.storage.IServiceStorage;
import com.exactpro.sf.storage.IVariableSetStorage;
import com.exactpro.sf.storage.StorageException;
import com.exactpro.sf.storage.impl.FakeMessageStorage;
import com.exactpro.sf.storage.impl.FilterMessageStorageWrapper;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

public final class DefaultConnectionManager implements IConnectionManager {

	private static final Logger logger = LoggerFactory.getLogger(DefaultConnectionManager.class);
    private static final IServiceNotifyListener EMPTY_NOTIFY_LISTENER = new ServiceNotifyListener();

	private final ExecutorService serviceExecutor;
	private final IServiceFactory staticServiceFactory;
	private final IEnvironmentMonitor environmentMonitor;
	private final IServiceContext serviceContext;

	private final IServiceStorage storage;
	private final IEnvironmentStorage envStorage;
    private final IVariableSetStorage variableSetStorage;
	private final List<IEnvironmentListener> eventListeners;
	/**
     * Operations with this map should be locked
     */
    private final Map<ServiceName, ServiceContainer> services;

    private final ReadWriteLock lock;

    /**
	 * Operations with this collections should be synchronized
	 */
	private final Set<String> usedServices;
    private final Map<String, ServiceDescription> defaultServices;

	public DefaultConnectionManager(
            IServiceFactory staticServiceFactory,
            IServiceStorage storage,
            IEnvironmentStorage envStorage,
            IVariableSetStorage variableSetStorage,
            IServiceContext serviceContext) {

		this.staticServiceFactory = Objects.requireNonNull(staticServiceFactory, "'Static service factory' parameter");

        this.serviceContext = Objects.requireNonNull(serviceContext, "'Service context' parameter");

		this.storage = Objects.requireNonNull(storage, "'Service storage' parameter");

		this.lock = new ReentrantReadWriteLock();

        this.serviceExecutor = Executors.newFixedThreadPool(5, new ThreadFactoryBuilder().setNameFormat("connection-manager-%d").build());

        this.envStorage = Objects.requireNonNull(envStorage, "envStorage cannot be null");

        this.variableSetStorage = Objects.requireNonNull(variableSetStorage, "variableSetStorage cannot be null");

        this.eventListeners = new CopyOnWriteArrayList<>();

		this.environmentMonitor = new DefaultEnvironmentMonitor(this, storage);

		this.services = new HashMap<>();

		this.usedServices = new HashSet<>();

        this.defaultServices = new HashMap<>();

        ServiceName serviceName = null;
		for (ServiceDescription serviceDescription : this.storage.getServiceDescriptions()) {
		    serviceName = new ServiceName(serviceDescription.getEnvironment(), serviceDescription.getName());
			IService service = staticServiceFactory.createService(serviceDescription.getType());
            if(services.put(serviceName, new ServiceContainer(service, serviceDescription)) != null) {
                logger.warn("Service {} already exists", serviceName);
            }
			initService(serviceName, null);

		}
	}

    @SuppressWarnings("unchecked")
	@Override
	public <Service extends IService> Service getService(ServiceName serviceName) {
        try {
            lock.readLock().lock();

            ServiceContainer serviceContainer = services.get(serviceName);

            return serviceContainer != null ? (Service) serviceContainer.getService() : null;
        } finally {
            lock.readLock().unlock();
        }
	}

	@Override
	public void dispose() {
	    try {
            lock.writeLock().lock();
    		try {

                if(!serviceExecutor.isShutdown()) {

                    serviceExecutor.shutdown();

                    if(!serviceExecutor.awaitTermination(5000, TimeUnit.MILLISECONDS)) {
    					logger.warn("Some Threads from ConnectionManager remained alive");
    				}
    			}

    		} catch (Exception e) {
    			logger.error(e.getMessage(), e);
    		}

            for(Entry<ServiceName, ServiceContainer> entry : services.entrySet()) {

    			try {

    				logger.info("Disposing [{}] service started...", entry.getKey());

                    ServiceContainer serviceContainer = entry.getValue();

                    disposeService(serviceContainer.getService());

    				logger.info("Disposing [{}] service finished", entry.getKey());

    			} catch ( Throwable e ) {
    				logger.error("Exception during service = [{}] disposing", entry.getKey(), e);
    			}
    		}
	    } finally {
            lock.writeLock().unlock();
	    }
	}

    @Override
    public void addDefaultService(ServiceDescription serviceDescription, IServiceNotifyListener exceptionListener) {
        serviceDescription = serviceDescription.clone();
        String serviceName = serviceDescription.getName();

        try {
            lock.writeLock().lock();

            if(defaultServices.putIfAbsent(serviceName, serviceDescription) != null) {
                throw new StorageException("Default service already exists: " + serviceName);
            }

            for(String environment : envStorage.list()) {
                if(services.containsKey(new ServiceName(environment, serviceName))) {
                    continue;
                }

                ServiceDescription clonedDescription = serviceDescription.clone();
                clonedDescription.setEnvironment(environment);

                addServiceWithoutNewThread(clonedDescription, exceptionListener);
            }
        } catch(Exception e) {
            defaultServices.remove(serviceName);
            exceptionNotify(exceptionListener, e);
            throw new ServiceException(e.getMessage(), e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void removeDefaultService(String serviceName, IServiceNotifyListener exceptionListener) {
        try {
            lock.writeLock().lock();

            if(defaultServices.remove(serviceName) == null) {
                throw new StorageException("Default service does not exist: " + serviceName);
            }
        } catch(Exception e) {
            exceptionNotify(exceptionListener, e);
            throw new ServiceException(e.getMessage(), e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Future<?> addService(ServiceDescription serviceDescription, IServiceNotifyListener notifyListener) {
        return serviceExecutor.submit(() -> addServiceWithoutNewThread(serviceDescription.clone(), notifyListener));
    }

	/**
     * @param serviceDescription
	 * @param notifyListener
	 */
    private void addServiceWithoutNewThread(ServiceDescription serviceDescription, IServiceNotifyListener notifyListener) {
        ServiceName serviceName = serviceDescription.getServiceName();

        try {
            lock.writeLock().lock();
            logger.info("Start adding service: {}", serviceName);

            if (services.containsKey(serviceName)) {
	            throw new StorageException("Service " + serviceName + " already exists");
	        }

            IServiceSettings settings = serviceDescription.getSettings();
            SailfishURI uri = serviceDescription.getType();

	        if (settings == null) {
                settings = staticServiceFactory.createServiceSettings(uri);
                serviceDescription.setSettings(settings);
	        }

            serviceDescription.setEnvironment(serviceName.getEnvironment());

            IService service = staticServiceFactory.createService(uri);

            if(serviceDescription.getServiceHandlerClassName() == null) {
                serviceDescription.setServiceHandlerClassName(CollectorServiceHandler.class.getCanonicalName());
            }

            ServiceContainer current = new ServiceContainer(service, serviceDescription);

            logger.info("Start adding service to storage: {}", serviceName);
            storage.addServiceDescription(serviceDescription);
            logger.info("Service was successfully  added to storage: {}", serviceName);

            logger.info("Start adding ServiceContainer: {}", serviceName);
            services.put(serviceName, current);
            logger.info("ServiceContainer was successfully  added: {}", serviceName);

	        try {
	            initServiceWithoutNewThread(serviceName, notifyListener);
	        } catch (ServiceException | EPSCommonException ignore) {
	            // we must ignore service exception during add
	        } finally {
                ServiceEvent event = ServiceEventFactory.createServiceChangeUpdateEvent(serviceName, Level.INFO,
                        Type.CREATED, "Service created", "", null);
	            environmentMonitor.onEvent(event);
	        }
	        logger.info("Service {} has been added", serviceName);
	    } catch (Exception e) {
	        exceptionNotify(notifyListener, e);
	        throw new ServiceException(e.getMessage(), e);
	    } finally {
	        lock.writeLock().unlock();
	    }
	}

	@Override
    public Future<?> removeService(ServiceName serviceName, IServiceNotifyListener notifyListener) {
		return serviceExecutor.submit(new Runnable() {
			@Override
			public void run() {
			    removeServiceWithoutNewThread(serviceName, notifyListener);
			}
		});
	}

    @Override
    public Future<?> removeServices(Collection<ServiceName> serviceNames, IServiceNotifyListener notifyListener) {
        return serviceExecutor.submit(() -> removeServicesWithoutNewThread(serviceNames, notifyListener));
    }

    protected ServiceContainer serviceContainerMapperFun(ServiceName serviceName) {
        logger.info("Start delete service: {}", serviceName);
        ServiceContainer serviceContainer = services.get(serviceName);
        if (serviceContainer == null) {
            logger.error("Could not find {} service", serviceContext);
        }
        return serviceContainer;
    }

    protected ServiceDescription disposeAndPrepareToDelete(ServiceContainer serviceContainer) {
        ServiceDescription description = serviceContainer.getServiceDescription();
        ServiceName serviceName = description.getServiceName();
        try {
            disposeService(serviceContainer.getService());
            ServiceEvent event = ServiceEventFactory
                    .createServiceChangeUpdateEvent(description.getServiceName(), Level.INFO, Type.DISPOSED, "Service deleted", "", null);
            environmentMonitor.onEvent(event);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        logger.info("schedule service {} delete from storage", serviceName);
        return description;
    }

	protected void removeServicesWithoutNewThread(Collection<ServiceName> serviceNames, IServiceNotifyListener notifyListener) {
        try {
            lock.writeLock().lock();

            List<ServiceDescription> toRemoveFromStorage = serviceNames.stream()
                    .map(this::serviceContainerMapperFun)
                    .filter(Objects::nonNull)
                    .map(this::disposeAndPrepareToDelete)
                    .collect(Collectors.toList());

            String deletingServices = toRemoveFromStorage.stream().map(ServiceDescription::getName).collect(Collectors.joining(", "));

            logger.info("Start delete services {} from storage", deletingServices);
            storage.removeServiceDescriptions(toRemoveFromStorage.iterator());
            logger.info("Services was successfully deleted from storage: {}", deletingServices);

            toRemoveFromStorage.stream()
                    .map(ServiceDescription::getServiceName)
                    .forEach(serviceName -> {
                        logger.info("Start delete ServiceContainer {}", serviceName);
                        services.remove(serviceName);
                        logger.info("ServiceContainer was successfully  deleted: {}", serviceName);
                    });

            logger.info("End delete services");

        } catch (Exception e) {
            exceptionNotify(notifyListener, e);
            throw new ServiceException(e.getMessage(), e);
        } finally {
            lock.writeLock().unlock();
        }
    }

	protected void removeServiceWithoutNewThread(ServiceName serviceName, IServiceNotifyListener notifyListener) {
        try {
            lock.writeLock().lock();
            logger.info("Start delete service: {}", serviceName);

            ServiceContainer serviceContainer = services.get(serviceName);

            if (serviceContainer == null) {
                throw new ServiceException("Could not find " + serviceName + " service");
            }

            ServiceDescription description = serviceContainer.getServiceDescription();

            disposeService(serviceContainer.getService());

            try {
                ServiceEvent event = ServiceEventFactory.createServiceChangeUpdateEvent(serviceName, Level.INFO,
                        Type.DISPOSED, "Service deleted", "", null);
                environmentMonitor.onEvent(event);
            } catch (EPSCommonException ignore){}

            logger.info("Start delete service {} from storage", serviceName);
            storage.removeServiceDescription(description);
            logger.info("Service was successfully deleted from storage: {}", serviceName);

            logger.info("Start delete ServiceContainer {}", serviceName);
            services.remove(serviceName);
            logger.info("ServiceContainer was successfully  deleted: {}", serviceName);

            logger.info("Service {} has been removed", serviceName);
        } catch (Exception e) {
            exceptionNotify(notifyListener, e);
            throw new ServiceException(e.getMessage(), e);
        } finally {
            lock.writeLock().unlock();
        }
	}

    @Override
    public Future<?> updateService(ServiceDescription serviceDescription, IServiceNotifyListener notifyListener) {
        return serviceExecutor.submit(new Runnable() {
			@Override
			public void run() {
                ServiceName serviceName = serviceDescription.getServiceName();

                try {
                    lock.writeLock().lock();
                    ServiceContainer serviceContainer = services.get(serviceName);

                    if (serviceContainer == null) {
                        throw new StorageException("ServiceContainer " + serviceName + " does not exist");
			        }

                    ServiceDescription description = serviceContainer.getServiceDescription();
                    description.setSettings(copySettings(serviceDescription.getSettings()));
                    description.setVariables(new HashMap<>(serviceDescription.getVariables()));
			        storage.updateServiceDescription(description);
                    messageNotify(notifyListener, serviceName + " updated");
			    } catch (Exception e) {
			        ServiceException exception = new ServiceException("Could not update service " + serviceName, e);
			        exceptionNotify(notifyListener, exception);
			        throw exception;
			    } finally {
			        lock.writeLock().unlock();
			    }
			}
		});
    }

	@Override
    public Future<?> initService(ServiceName serviceName, IServiceNotifyListener notifyListener) {
	    if (serviceName == null) {
	        throw new ServiceException("serviceName is null");
	    }

		return serviceExecutor.submit(new Runnable() {
			@Override
			public void run() {
			    initServiceWithoutNewThread(serviceName, notifyListener);
			}
		});
	}

    protected void initServiceWithoutNewThread(ServiceName serviceName, IServiceNotifyListener notifyListener) {
        try {
            lock.readLock().lock();
            ServiceContainer serviceContainer = services.get(serviceName);

            if (serviceContainer == null) {
                throw new ServiceException("Could not find " + serviceName + " serviceContainer");
            }

            IService service = serviceContainer.getService();
            ServiceDescription description = serviceContainer.getServiceDescription();

            if (service == null) {
                throw new ServiceException("Could not find " + serviceName + " service");
            }
            if (description == null) {
                throw new ServiceException("Could not find " + serviceName + " ServiceDescription");
            }
            if (description.getServiceHandlerClassName() == null) {
                throw new ServiceException("HandlerClassName is null for [" + description.getName() + "] service");
            }

            ServiceStatus curStatus = service.getStatus();

            if (curStatus != ServiceStatus.STARTED && curStatus != ServiceStatus.STARTING && curStatus != ServiceStatus.WARNING) {
                SailfishURI serviceURI = description.getType();

                IServiceContext serviceContext = this.serviceContext;
                IServiceHandler serviceHandler = staticServiceFactory.createServiceHandler(serviceURI, description.getServiceHandlerClassName());

                Set<String> processedMessageTypes = loadProcessedMessageTypes(description);
                if (!processedMessageTypes.isEmpty()) {
                    serviceHandler = new FilterServiceHandlerWrapper(serviceHandler, processedMessageTypes);
                    if (description.getSettings().isPersistMessages()) {
                        serviceContext = new DefaultServiceContext(serviceContext,
                                new FilterMessageStorageWrapper(serviceContext.getMessageStorage(), processedMessageTypes), storage);
                    }
                }
                if (!description.getSettings().isPersistMessages()) {
                    serviceContext = new DefaultServiceContext(serviceContext, new FakeMessageStorage(), storage);
                }
                serviceContainer.setOriginServiceHandler(serviceHandler);

                IDictionaryValidator validator = staticServiceFactory.createDictionaryValidator(serviceURI);

                if (validator != null) {
                    SailfishURI dictionaryURI = description.getSettings().getDictionaryName();

                    if (dictionaryURI != null) {
                        try {
                            IDictionaryStructure dictionary = serviceContext.getDictionaryManager()
                                                                            .getDictionary(dictionaryURI);
                            List<DictionaryValidationError> errors = validator.validate(dictionary, true, null);

                            if (!errors.isEmpty()) {
                                StringBuilder message = new StringBuilder(
                                        "Got following errors during dictionary validation:\n");

                                for (DictionaryValidationError error : errors) {
                                    message.append(error + "\n");
                                }

                                String eventMessage = message.toString();
                                ServiceEvent event = ServiceEventFactory.createEventError(serviceName, Type.ERROR,
                                                                                          eventMessage, "");
                                environmentMonitor.onEvent(event);
                                exceptionNotify(notifyListener, new ServiceException(eventMessage));
                            }
                        } catch (RuntimeException e) {
                            String eventMessage = e.getMessage();
                            ServiceEvent event = ServiceEventFactory.createEventError(serviceName, Type.ERROR,
                                                                                      eventMessage, "");
                            environmentMonitor.onEvent(event);
                            exceptionNotify(notifyListener, new ServiceException(eventMessage));
                        }

                    }
                }

                IServiceSettings settings = description.getSettings();
                String environmentVariableSet = envStorage.getVariableSet(serviceName.getEnvironment());

                if(environmentVariableSet != null) {
                    Map<String, String> variableSet = variableSetStorage.get(environmentVariableSet);

                    if(variableSet != null) {
                        logger.debug("Applying variable set '{}' to service '{}'", environmentVariableSet, serviceName);
                        settings = description.applyVariableSet(variableSet);
                    }
                }

                synchronized (service) {
                    service.init(serviceContext, environmentMonitor, serviceContainer.getHandlerWrapper(), settings, serviceName);
                }
            } else {
                logger.debug("Service {} is starting or already started", serviceName);
            }
        } catch (Exception e) {
            exceptionNotify(notifyListener, e);
            throw new ServiceException(e.getMessage(), e);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Future<?> startService(ServiceName serviceName, IServiceNotifyListener notifyListener) {
	    if (serviceName == null) {
            throw new ServiceException("serviceName is null");
        }

		return serviceExecutor.submit(new Runnable() {
			@Override
			public void run() {
		        try {
		            lock.readLock().lock();
                    ServiceContainer serviceContainer = services.get(serviceName);

                    if (serviceContainer == null) {
                        throw new ServiceException("Could not find " + serviceName + " serviceContainer");
                    }

                    IService service = serviceContainer.getService();

		            if (service == null) {
                        throw new ServiceException("Could not find " + serviceName + " service");
		            }

					if (service.getStatus() == ServiceStatus.INITIALIZED) {
						synchronized (service) {
							service.start();
						}
					} else {
                        throw new ServiceException("Service " + serviceName + " already started or not initialized");
					}

				} catch (Exception e) {
					exceptionNotify(notifyListener, e);
					throw new ServiceException(e.getMessage(), e);
				} finally {
				    lock.readLock().unlock();
				}
			}
		});
    }

	@Override
    public Future<?> disposeService(ServiceName serviceName, IServiceNotifyListener notifyListener) {
	    if (serviceName == null) {
            throw new ServiceException("serviceName is null");
        }

		return serviceExecutor.submit(new Runnable() {
			@Override
			public void run() {
				try {
				    lock.readLock().lock();
                    ServiceContainer serviceContainer = services.get(serviceName);
                    if (serviceContainer == null) {
                        throw new ServiceException("Could not find " + serviceName + " serviceContainer");
                    }

                    IService service = serviceContainer.getService();
					if (service == null) {
						throw new ServiceException("Could not find " + serviceName + " service");
					}

                    if (service.getStatus() == ServiceStatus.STARTED || service.getStatus() == ServiceStatus.WARNING) {

					    logger.info("Cleanup for service {} invoked", serviceName);

					    disposeService(service);

					} else {
						logger.error("Service {} is not started to be disposed", serviceName);
					}

				} catch (Exception e) {
					exceptionNotify(notifyListener, e);
					throw new ServiceException(e.getMessage(), e);
				} finally {
				    lock.readLock().unlock();
				}
			}
		});
	}

    @Override
    public Future<?> removeEnvironment(String envName, IServiceNotifyListener notifyListener) {
        return serviceExecutor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    lock.writeLock().lock();

                    if (envStorage.exists(envName)) {
                        Set<ServiceName> serviceNames = new HashSet<>(services.keySet());

                        for (ServiceName serviceName : serviceNames) {
                            if (envName.equals(serviceName.getEnvironment())) {
                                removeServiceWithoutNewThread(serviceName, notifyListener);
                            }
                        }

                        envStorage.remove(envName);
                        EnvironmentEvent event = new ChangeEnvironmentEvent(envName, "Environment has been deleted", Status.DELETED);
                        environmentMonitor.onEvent(event);
                    } else {
                        throw new IllegalArgumentException("Environment " + envName + " has not been deleted");
                    }
                } catch (Exception e) {
                    exceptionNotify(notifyListener, e);
                    throw new StorageException(e.getMessage(), e);
                } finally {
                    lock.writeLock().unlock();
                }
            }
        });
    }

    @Override
    public List<String> getEnvironmentList() {
        try {
            lock.readLock().lock();
            return envStorage.list();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Future<?> addEnvironment(String envName, IServiceNotifyListener notifyListener) {
        return serviceExecutor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    lock.writeLock().lock();
                    envStorage.add(envName);
                    EnvironmentEvent event = new ChangeEnvironmentEvent(envName, "Environment was added", Status.ADDED);
                    environmentMonitor.onEvent(event);

                    for(ServiceDescription serviceDescription : defaultServices.values()) {
                        serviceDescription = serviceDescription.clone();
                        serviceDescription.setEnvironment(envName);
                        addServiceWithoutNewThread(serviceDescription, notifyListener);
                    }
                } catch (Exception e) {
                    exceptionNotify(notifyListener, e);
                    throw new StorageException(e.getMessage(), e);
                } finally {
                    lock.writeLock().unlock();
                }
            }
        });
    }

    @Override
    public Future<?> renameEnvironment(String oldEnvName, String newEnvName, IServiceNotifyListener notifyListener) {
        return serviceExecutor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    lock.writeLock().lock();

                    if(ServiceName.DEFAULT_ENVIRONMENT.equalsIgnoreCase(oldEnvName)) {
                        throw new StorageException("Cannot rename default environment to: " + oldEnvName);
                    }

                    if(ServiceName.DEFAULT_ENVIRONMENT.equalsIgnoreCase(newEnvName)) {
                        throw new StorageException("Cannot rename to default environment: " + newEnvName);
                    }

                    if(!envStorage.exists(oldEnvName)) {
                        throw new StorageException("Environment doesn't exist: " + oldEnvName);
                    }

                    if(envStorage.exists(newEnvName)) {
                        throw new StorageException("Environment already exists: " + newEnvName);
                    }

                    Set<String> startedServices = new HashSet<>();
                    Set<ServiceName> environmentServices = new HashSet<>();
                    for (Entry<ServiceName, ServiceContainer> entry : services.entrySet()) {
                        ServiceContainer serviceContainer = entry.getValue();
                        if (entry.getKey().getEnvironment().equals(oldEnvName)) {
                            environmentServices.add(entry.getKey());
                            if (serviceContainer.getService().getStatus() == ServiceStatus.STARTED
                                    || serviceContainer.getService().getStatus() == ServiceStatus.WARNING) {
                                startedServices.add(entry.getKey().getServiceName());
                            }
                        }
                    }

                    if (!startedServices.isEmpty()) {
                        throw new StorageException("Environment " + oldEnvName + " can not be renamed, services " + startedServices + " are started");
                    }

                    List<ServiceDescription> removedServices = new ArrayList<>();

                    for (ServiceName serviceName : environmentServices) {
                        removedServices.add(services.get(serviceName).getServiceDescription());
                        removeServiceWithoutNewThread(serviceName, notifyListener);
                    }

                    envStorage.rename(oldEnvName, newEnvName);

                    for(ServiceDescription description : removedServices) {
                        description.setEnvironment(newEnvName);
                        addServiceWithoutNewThread(description, notifyListener);
                    }

                    ChangeEnvironmentEvent event = new ChangeEnvironmentEvent(oldEnvName, "Environment was renamed", Status.RENAMED);
                    event.setNewEnvName(newEnvName);
                    environmentMonitor.onEvent(event);
                } catch (Exception e) {
                    exceptionNotify(notifyListener, e);
                    throw new StorageException(e.getMessage(), e);
                } finally {
                    lock.writeLock().unlock();
                }
            }
        });
    }

	@Override
    public ServiceName[] getServiceNames() {
        try {
            lock.readLock().lock();
            return services.keySet().toArray(new ServiceName[services.size()]);
        } finally {
            lock.readLock().unlock();
        }
    }

	@Override
	public IService[] getStartedServices() {
	    try {
            lock.readLock().lock();
            List<IService> returned = new ArrayList<>();

            for(ServiceContainer serviceContainer : services.values()) {
                if (serviceContainer.getService().getStatus() == ServiceStatus.STARTED
                        || serviceContainer.getService().getStatus() == ServiceStatus.WARNING) {
                    returned.add(serviceContainer.getService());
    			}
    		}

    		return returned.toArray(new IService[returned.size()]);
	    } finally {
            lock.readLock().unlock();
        }
	}

    private List<ServiceName> getStartedServices(String environmentName) {
        return withReadLock(() ->
                getServices(environmentName)
                    .stream()
                    .filter(service -> service.getStatus() == ServiceStatus.STARTED
                                    || service.getStatus() == ServiceStatus.WARNING)
                    .map(IService::getServiceName)
                    .collect(Collectors.toList())
        );
    }

    private List<IService> getServices(String environmentName) {
        return withReadLock(() -> {
            List<IService> environmentServices = new ArrayList<>();
            services.forEach((name, container) -> {
                if(!name.getEnvironment().equals(environmentName)) {
                    return;
                }
                environmentServices.add(container.getService());
            });
            return environmentServices;
        });
    }

    /**
	 * Remove all sent and received messages from memory
	 * before and after each test case.
	 */
	@Override
    public void cleanup(List<String> services) {
        try {
            lock.readLock().lock();
            logger.debug("cleaning up services: {}", services);

            for(String serviceName : services) {
                logger.debug("cleaning up service: {}", serviceName);

                ServiceContainer serviceContainer = this.services.get(ServiceName.parse(serviceName));
                if(serviceContainer == null) {
                    logger.debug("cannot not find serviceContainer for cleanup: {}", serviceName);
                    continue;
                }

                IService service = serviceContainer.getService();
                if(service == null) {
                    logger.debug("cannot not find service for cleanup: {}", serviceName);
                    continue;
                }

                IServiceHandler handler = serviceContainer.getService().getServiceHandler();
                if(handler == null) {
                    logger.debug("null service handler for service: {}", serviceName);
                    continue;
                }

                handler.cleanMessages(ServiceHandlerRoute.values());
            }

            logger.debug("cleanup completed");
        } finally {
            lock.readLock().unlock();
        }
    }

	@Override
	public ServiceDescription[] getServicesDescriptions() {
	    try {
            lock.readLock().lock();
            List<ServiceDescription> descriptions = new ArrayList<>();

            for(ServiceContainer serviceContainer : services.values()) {
                descriptions.add(serviceContainer.getServiceDescription().clone());
            }

            return descriptions.toArray(new ServiceDescription[descriptions.size()]);
	    } finally {
            lock.readLock().unlock();
	    }
	}


	@Override
	public ServiceDescription getServiceDescription(ServiceName serviceName) {
	    try {
            lock.readLock().lock();
            ServiceContainer serviceContainer = services.get(serviceName);
            return serviceContainer != null ? serviceContainer.getServiceDescription().clone() : null;
	    } finally {
            lock.readLock().unlock();
	    }
	}

	@Override
	public synchronized void setServiceUsed(String[] names) throws InterruptedException {

		if (names == null) {
			throw new NullPointerException("names[] is null");
		}

		Arrays.sort(names);

		for (String name : names) {

            while(usedServices.contains(name)) {
                try {
                    logger.info("wait for unlock service {} begin", name);
                    wait();
                } finally {
                    logger.info("wait for unlock service {} end", name);
                }
			}

            usedServices.add(name);

            setStoreMessageMode(name, true);
		}
	}

	@Override
	public synchronized void setServiceNotUsed(String[] names) {

		if (names == null) {
			throw new NullPointerException("names[] is null");
		}

		Arrays.sort(names);

		for (String name : names) {
            usedServices.remove(name);

            setStoreMessageMode(name, false);
		}

		notifyAll();
	}

	@Override
	public synchronized Set<String> getUsedServices() {
        return new HashSet<>(usedServices);
	}

	@Override
	public void subscribeForEvents(IEnvironmentListener listener) {
		eventListeners.add(listener);
	}

	@Override
	public void unSubscribeForEvents(IEnvironmentListener listener) {
		eventListeners.remove(listener);
	}

	@Override
	public List<IEnvironmentListener> getEnvironmentListeners() {
		return eventListeners;
	}

	protected void messageNotify(IServiceNotifyListener notifyListener, String message) {
		if (notifyListener != null) {
			notifyListener.onInfoProcessing(message);
		}
	}

	protected void exceptionNotify(IServiceNotifyListener notifyListener, Exception e) {

		if (notifyListener != null) {
			notifyListener.onErrorProcessing(e.getMessage());
		}

		logger.error(e.getMessage(), e);
	}

	@Override
    public Future<?> copyService(ServiceName from, ServiceName to, IServiceNotifyListener notifyListener) {
        return serviceExecutor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    lock.writeLock().lock();
                    if (services.containsKey(to)) {
                        throw new StorageException("Service " + to + " already exits");
                    }
                    ServiceContainer serviceContainer = services.get(from);

                    if (serviceContainer == null) {
                        throw new ServiceException("Could not find " + from + " ServiceContainer");
                    }

                    ServiceDescription description = serviceContainer.getServiceDescription();

                    if (description == null) {
                        throw new ServiceException("Could not find " + from + " ServiceDescription");
                    } else {
                        description = description.clone();
                    }

                    description.setName(to.getServiceName());
                    description.setEnvironment(to.getEnvironment());

                    addServiceWithoutNewThread(description, notifyListener);
                } catch (StorageException e) {
                    throw e;
                } catch (Exception e) {
                    exceptionNotify(notifyListener, e);
                    throw new StorageException(e.getMessage(), e);
                } finally {
                    lock.writeLock().unlock();
                }
            }
        });
	}

	@Override
	public IServiceSettings getServiceSettings(ServiceName serviceName) {
	    try {
            lock.readLock().lock();
            ServiceContainer serviceContainer = services.get(serviceName);

    		if (serviceContainer == null) {
    			throw new ServiceException("Could not find " + serviceName + " ServiceContainer");
    		}

            ServiceDescription description = serviceContainer.getServiceDescription();
	        if (description == null) {
                throw new ServiceException("Could not find " + serviceName + " ServiceDescription");
            }

	        IServiceSettings result = staticServiceFactory.createServiceSettings(description.getType());

	        BeanConfigurator.copyBean(description.getSettings(), result);

	        return result;
        } finally {
            lock.readLock().unlock();
        }
	}

    @Override
    public Map<String, String> getVariableSet(String name) {
        logger.debug("Getting variable set: {}", name);
        return withReadLock(() -> variableSetStorage.get(name));
    }

    private <T> void updateVariableSet(String name, Supplier<T> updater) {
        withWriteLock(() -> {
            List<ServiceName> services = new ArrayList<>();

            for(String environmentName : getEnvironmentList()) {
                String environmentVariableSet = getEnvironmentVariableSet(environmentName);

                if(name.equalsIgnoreCase(environmentVariableSet)) {
                    for(ServiceName serviceName : getStartedServices(environmentName)) {
                        services.add(serviceName);
                    }
                }
            }

            if(!services.isEmpty()) {
                throw new StorageException(format("Cannot update variable set '%s' because it is currently used by following services: %s", name, services));
            }

            return updater.get();
        });
    }

    @Override
    public void putVariableSet(String name, Map<String, String> variableSet) {
        logger.debug("Putting variable set '{}': {}", name, variableSet);
        updateVariableSet(name, () -> {
            variableSetStorage.put(name, variableSet);
            return this;
        });
    }

    @Override
    public void removeVariableSet(String name) {
        logger.debug("Removing variable set: {}", name);
        updateVariableSet(name, () -> {
            variableSetStorage.remove(name);

            for(String environmentName : getEnvironmentList()) {
                if(name.equalsIgnoreCase(getEnvironmentVariableSet(environmentName))) {
                    setEnvironmentVariableSet(environmentName, null);
                }
            }

            return this;
        });
    }

    @Override
    public boolean isVariableSetExists(String name) {
        logger.debug("Checking existence of variable set: {}", name);
        return withReadLock(() -> variableSetStorage.exists(name));
    }

    @Override
    public Set<String> getVariableSets() {
        logger.debug("Getting list of all variable sets");
        return withReadLock(variableSetStorage::list);
    }

    @Override
    public void setEnvironmentVariableSet(String environmentName, String variableSetName) {
        if(variableSetName == null) {
            logger.debug("Removing variable set from environment: {}", environmentName);
        } else {
            logger.debug("Setting variable set for environment '{}' to '{}'", environmentName, variableSetName);
        }


        withWriteLock(() -> {
            if(variableSetName != null && !variableSetStorage.exists(variableSetName)) {
                throw new StorageException("Variable set does not exist: " + variableSetName);
            }

            List<ServiceName> services = getStartedServices(environmentName);

            if(!services.isEmpty()) {
                throw new StorageException(format("Cannot change variable set for environment '%s' because it has running services: %s", environmentName, services));
            }
            envStorage.setVariableSet(environmentName, variableSetName);
            for (IService service : getServices(environmentName)) {
                try {
                    initServiceWithoutNewThread(service.getServiceName(), EMPTY_NOTIFY_LISTENER);
                } catch (Exception e) {
                    logger.warn("Could not init service {}", service.getServiceName(), e);
                }
            }
            return this;
        });
    }

    @Override
    public String getEnvironmentVariableSet(String environmentName) {
        logger.debug("Getting variable set for environment: {}", environmentName);
        return withReadLock(() -> envStorage.getVariableSet(environmentName));
    }

    /**
     * @param service
     */
    private void disposeService(IService service) {
        try {
            synchronized (service) {
                if (service.getServiceHandler() != null) {
                	service.getServiceHandler().cleanMessages(ServiceHandlerRoute.values());
                }
            	service.dispose();
            }

        } catch (Exception e) {
            String message = "Exception during " + service.getServiceName() + " disposing";
            logger.error(message, e);
            throw new ServiceException(message, e);
        }
    }

    private void setStoreMessageMode(String serviceName, boolean store) {
        ServiceContainer serviceContainer = services.get(ServiceName.parse(serviceName));
		if (serviceContainer != null) {
            OptionalServiceHandlerWrapper handler = serviceContainer.getHandlerWrapper();
            handler.storeMessages(store);
		}
    }

    private Set<String> loadProcessedMessageTypes(ServiceDescription description) throws SailfishURIException {
        String value = description.getSettings().getStoredMessageTypes();
        if (StringUtils.isNotBlank(value)) {
            value = ServiceUtil.loadStringFromAlias(serviceContext.getDataManager(), value, ",");
            return stream(value.split(","))
                    .map(type -> type.trim())
                    .filter(StringUtils::isNoneEmpty)
                    .collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }

    private class ServiceContainer {
        private final IService service;
        private final ServiceDescription serviceDescription;
        private final OptionalServiceHandlerWrapper handlerWrapper;

		ServiceContainer(IService service, ServiceDescription serviceDescription) {
            this.service = service;
            this.serviceDescription = serviceDescription;
            this.handlerWrapper = new OptionalServiceHandlerWrapper();
        }

		IService getService() {
            return service;
        }

        ServiceDescription getServiceDescription() {
            return serviceDescription;
        }

		OptionalServiceHandlerWrapper getHandlerWrapper() {
            return handlerWrapper;
        }

        void setOriginServiceHandler(IServiceHandler serviceHandler) {
            handlerWrapper.setOriginServiceHandler(serviceHandler);
        }
    }

    private <T> T withReadLock(Supplier<T> supplier) {
        try {
            lock.readLock().lock();
            return supplier.get();
        } finally {
            lock.readLock().unlock();
        }
    }

    private <T> T withWriteLock(Supplier<T> supplier) {
        try {
            lock.writeLock().lock();
            return supplier.get();
        } finally {
            lock.writeLock().unlock();
        }
    }

    private static class ServiceNotifyListener implements IServiceNotifyListener {
        @Override
        public void onErrorProcessing(String message) {
            logger.error(message);
        }

        @Override
        public void onInfoProcessing(String message) {
            logger.info(message);
        }
    }
}