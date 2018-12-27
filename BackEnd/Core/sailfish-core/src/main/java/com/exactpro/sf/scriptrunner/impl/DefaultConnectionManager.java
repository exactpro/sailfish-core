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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
import java.util.stream.Collectors;

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
import com.exactpro.sf.services.ServiceEvent.Type;
import com.exactpro.sf.services.ServiceEventFactory;
import com.exactpro.sf.services.ServiceException;
import com.exactpro.sf.services.ServiceHandlerRoute;
import com.exactpro.sf.services.ServiceStatus;
import com.exactpro.sf.services.util.ServiceUtil;
import com.exactpro.sf.storage.IEnvironmentStorage;
import com.exactpro.sf.storage.IServiceStorage;
import com.exactpro.sf.storage.StorageException;
import com.exactpro.sf.storage.impl.FakeMessageStorage;
import com.exactpro.sf.storage.impl.FilterMessageStorageWrapper;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

public final class DefaultConnectionManager implements IConnectionManager {

	private static final Logger logger = LoggerFactory.getLogger(DefaultConnectionManager.class);

	private final ExecutorService serviceExecutor;
	private final IServiceFactory staticServiceFactory;
	private final IEnvironmentMonitor environmentMonitor;
	private final IServiceContext serviceContext;

	private final IServiceStorage storage;
	private final IEnvironmentStorage envStorage;
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

	public DefaultConnectionManager(
			final IServiceFactory staticServiceFactory,
			final IServiceStorage storage,
			final IEnvironmentStorage envStorage,
			final IServiceContext serviceContext) {

		this.staticServiceFactory = Objects.requireNonNull(staticServiceFactory, "'Static service factory' parameter");

        this.serviceContext = Objects.requireNonNull(serviceContext, "'Service context' parameter");

		this.storage = Objects.requireNonNull(storage, "'Service storage' parameter");

		this.lock = new ReentrantReadWriteLock();

        this.serviceExecutor = Executors.newFixedThreadPool(5, new ThreadFactoryBuilder().setNameFormat("connection-manager-%d").build());

        this.envStorage = envStorage;

		this.eventListeners = new CopyOnWriteArrayList<>();

		this.environmentMonitor = new DefaultEnvironmentMonitor(this, storage);

		this.services = new HashMap<>();

		this.usedServices = new HashSet<>();

		ServiceName serviceName = null;
		for (ServiceDescription serviceDescription : this.storage.getServiceDescriptions()) {
		    serviceName = new ServiceName(serviceDescription.getEnvironment(), serviceDescription.getName());
			IService service = staticServiceFactory.createService(serviceDescription.getType());
            if (this.services.put(serviceName, new ServiceContainer(service, serviceDescription)) != null) {
                logger.warn("Service {} already exists", serviceName);
            }
			initService(serviceName, null);

		}
	}

    @SuppressWarnings("unchecked")
	@Override
	public <Service extends IService> Service getService(ServiceName serviceName) {
        try {
            this.lock.readLock().lock();

            final ServiceContainer serviceContainer = this.services.get(serviceName);

            return serviceContainer != null ? (Service) serviceContainer.getService() : null;
        } finally {
            this.lock.readLock().unlock();
        }
	}

	@Override
	public void dispose() {
	    try {
	        this.lock.writeLock().lock();
    		try {

    			if (!this.serviceExecutor.isShutdown()) {

    				this.serviceExecutor.shutdown();

    				if (!this.serviceExecutor.awaitTermination(5000, TimeUnit.MILLISECONDS)) {
    					logger.warn("Some Threads from ConnectionManager remained alive");
    				}
    			}

    		} catch (Exception e) {
    			logger.error(e.getMessage(), e);
    		}

            for (Entry<ServiceName, ServiceContainer> entry : this.services.entrySet()) {

    			try {

    				logger.info("Disposing [{}] service started...", entry.getKey());

                    final ServiceContainer serviceContainer = entry.getValue();

                    disposeService(serviceContainer.getService());

    				logger.info("Disposing [{}] service finished", entry.getKey());

    			} catch ( Throwable e ) {
    				logger.error("Exception during service = [{}] disposing", entry.getKey(), e);
    			}
    		}
	    } finally {
	        this.lock.writeLock().unlock();
	    }
	}

	@Override
    public Future<?> addService(final ServiceName serviceName, final SailfishURI uri, final IServiceSettings settings,
            final IServiceNotifyListener notifyListener) {

        return serviceExecutor.submit(new Runnable() {
            @Override
            public void run() {
                addServiceWithoutNewThread(serviceName, uri, settings, notifyListener);
            }

        });
    }

	/**
	 * @param serviceName
	 * @param type
	 * @param settings
	 * @param notifyListener
	 */
	private void addServiceWithoutNewThread(final ServiceName serviceName, final SailfishURI uri, final IServiceSettings settings,
	        final IServiceNotifyListener notifyListener) {
	    try {
	        lock.writeLock().lock();
            logger.info("Start adding service: {}", serviceName);

            if (services.containsKey(serviceName)) {
	            throw new StorageException("Service " + serviceName + " already exists");
	        }

	        IServiceSettings resultSettings = null;

	        if (settings == null) {
	            resultSettings = staticServiceFactory.createServiceSettings(uri);
	        } else {
	            resultSettings = settings;
	        }

	        IService service = staticServiceFactory.createService(uri);

	        ServiceDescription desc = new ServiceDescription(uri);
	        desc.setEnvironment(serviceName.getEnvironment());
	        desc.setName(serviceName.getServiceName());
	        desc.setSettings(resultSettings);
            desc.setServiceHandlerClassName(com.exactpro.sf.services.CollectorServiceHandler.class.getCanonicalName());

            ServiceContainer current = new ServiceContainer(service, desc);

            logger.info("Start adding service to storage: {}", serviceName);
            this.storage.addServiceDescription(desc);
            logger.info("Service was successfully  added to storage: {}", serviceName);

            logger.info("Start adding ServiceContainer: {}", serviceName);
            this.services.put(serviceName, current);
            logger.info("ServiceContainer was successfully  added: {}", serviceName);

	        try {
	            initServiceWithoutNewThread(serviceName, notifyListener);
	        } catch (ServiceException | EPSCommonException ignore) {
	            // we must ignore service exception during add
	        } finally {
	            ServiceEvent event = ServiceEventFactory.createServiceChangeUpdateEvent(serviceName, ServiceEvent.Level.INFO,
	                    ServiceEvent.Type.CREATED, "Service created", "", null);
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
	public Future<?> removeService(final ServiceName serviceName, final IServiceNotifyListener notifyListener) {
		return serviceExecutor.submit(new Runnable() {
			@Override
			public void run() {
			    removeServiceWithoutNewThread(serviceName, notifyListener);
			}
		});
	}

	protected void removeServiceWithoutNewThread(ServiceName serviceName, IServiceNotifyListener notifyListener) {

        try {
            lock.writeLock().lock();
            logger.info("Start delete service: {}", serviceName);

            ServiceContainer serviceContainer = this.services.get(serviceName);

            if (serviceContainer == null) {
                throw new ServiceException("Could not find " + serviceName + " service");
            }

            ServiceDescription description = serviceContainer.getServiceDescription();

            disposeService(serviceContainer.getService());

            try {
                ServiceEvent event = ServiceEventFactory.createServiceChangeUpdateEvent(serviceName, ServiceEvent.Level.INFO,
                        ServiceEvent.Type.DISPOSED, "Service deleted", "", null);
                environmentMonitor.onEvent(event);
            } catch (EPSCommonException ignore){}

            logger.info("Start delete service {} from storage", serviceName);
            this.storage.removeServiceDescription(description);
            logger.info("Service was successfully deleted from storage: {}", serviceName);

            logger.info("Start delete ServiceContainer {}", serviceName);
            this.services.remove(serviceName);
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
	public Future<?> updateService(final ServiceName serviceName, final IServiceSettings settings, final IServiceNotifyListener notifyListener) {

		return serviceExecutor.submit(new Runnable() {
			@Override
			public void run() {
			    try {
			        lock.writeLock().lock();
                    final ServiceContainer serviceContainer = services.get(serviceName);

                    if (serviceContainer == null) {
                        throw new StorageException("ServiceContainer " + serviceName + " does not exist");
			        }

                    ServiceDescription description = serviceContainer.getServiceDescription();
			        description.setSettings(settings);
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
	public Future<?> initService(final ServiceName serviceName, final IServiceNotifyListener notifyListener) {
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

    protected void initServiceWithoutNewThread(final ServiceName serviceName, final IServiceNotifyListener notifyListener) {
        try {
            lock.readLock().lock();
            final ServiceContainer serviceContainer = services.get(serviceName);

            if (serviceContainer == null) {
                throw new ServiceException("Could not find " + serviceName + " serviceContainer");
            }

            IService service = serviceContainer.getService();
            ServiceDescription description = serviceContainer.getServiceDescription();

            if (service == null) {
                throw new ServiceException("Could not find " + serviceName.toString() + " service");
            }
            if (description == null) {
                throw new ServiceException("Could not find " + serviceName.toString() + " ServiceDescription");
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
                                    message.append(error.toString() + "\n");
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

                synchronized (service) {
                    service.init(serviceContext, environmentMonitor, serviceContainer.getHandlerWrapper(), description.getSettings(), serviceName);
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
	public Future<?> startService(final ServiceName serviceName, final IServiceNotifyListener notifyListener) {
	    if (serviceName == null) {
            throw new ServiceException("serviceName is null");
        }

		return serviceExecutor.submit(new Runnable() {
			@Override
			public void run() {
		        try {
		            lock.readLock().lock();
                    final ServiceContainer serviceContainer = services.get(serviceName);

                    if (serviceContainer == null) {
                        throw new ServiceException("Could not find " + serviceName + " serviceContainer");
                    }

                    IService service = serviceContainer.getService();

		            if (service == null) {
		                throw new ServiceException("Could not find " + serviceName.toString() + " service");
		            }

					if (service.getStatus() == ServiceStatus.INITIALIZED) {
						synchronized (service) {
							service.start();
						}
					} else {
						throw new ServiceException("Service " + serviceName.toString() + " already started or not initialized");
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
	public Future<?> disposeService(final ServiceName serviceName, final IServiceNotifyListener notifyListener) {
	    if (serviceName == null) {
            throw new ServiceException("serviceName is null");
        }

		return serviceExecutor.submit(new Runnable() {
			@Override
			public void run() {
				try {
				    lock.readLock().lock();
                    final ServiceContainer serviceContainer = services.get(serviceName);
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
    public Future<?> removeEnvironment(final String envName, final IServiceNotifyListener notifyListener) {
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
                        EnvironmentEvent event = new ChangeEnvironmentEvent(envName, "Environment has been deleted", ChangeEnvironmentEvent.Status.DELETED);
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
            this.lock.readLock().lock();
            return envStorage.list();
        } finally {
            this.lock.readLock().unlock();
        }
    }

    @Override
    public Future<?> addEnvironment(final String envName, final IServiceNotifyListener notifyListener) {
        return serviceExecutor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    lock.writeLock().lock();
                    envStorage.add(envName);
                    EnvironmentEvent event = new ChangeEnvironmentEvent(envName, "Environment was added", ChangeEnvironmentEvent.Status.ADDED);
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
    public Future<?> renameEnvironment(final String oldEnvName, final String newEnvName, final IServiceNotifyListener notifyListener) {
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
                        final ServiceContainer serviceContainer = entry.getValue();
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
                        addServiceWithoutNewThread(new ServiceName(newEnvName, description.getName()), description.getType(), description.getSettings(), notifyListener);
                    }

                    ChangeEnvironmentEvent event = new ChangeEnvironmentEvent(oldEnvName, "Environment was renamed", ChangeEnvironmentEvent.Status.RENAMED);
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
            this.lock.readLock().lock();
            return this.services.keySet().toArray(new ServiceName[this.services.size()]);
        } finally {
            this.lock.readLock().unlock();
        }
    }

	@Override
	public IService[] getStartedServices() {
	    try {
            this.lock.readLock().lock();
            List<IService> returned = new ArrayList<>();

            for (final ServiceContainer serviceContainer : this.services.values()) {
                if (serviceContainer.getService().getStatus() == ServiceStatus.STARTED
                        || serviceContainer.getService().getStatus() == ServiceStatus.WARNING) {
                    returned.add(serviceContainer.getService());
    			}
    		}

    		return returned.toArray(new IService[returned.size()]);
	    } finally {
            this.lock.readLock().unlock();
        }
	}

	/**
	 * Remove all sent and received messages from memory
	 * before and after each test case.
	 */
	@Override
    public void cleanup(List<String> services) {
        try {
            this.lock.readLock().lock();
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
            this.lock.readLock().unlock();
        }
    }

	@Override
	public ServiceDescription[] getServicesDescriptions() {
	    try {
	        this.lock.readLock().lock();
            List<ServiceDescription> descriptions = new ArrayList<>();

            for (final ServiceContainer serviceContainer : this.services.values()) {
                descriptions.add(serviceContainer.getServiceDescription());
            }

            return descriptions.toArray(new ServiceDescription[descriptions.size()]);
	    } finally {
	        this.lock.readLock().unlock();
	    }
	}


	@Override
	public ServiceDescription getServiceDescription(ServiceName serviceName) {
	    try {
	        this.lock.readLock().lock();
            final ServiceContainer serviceContainer = services.get(serviceName);

            return serviceContainer != null ? serviceContainer.getServiceDescription() : null;
	    } finally {
	        this.lock.readLock().unlock();
	    }
	}

	@Override
	public synchronized void setServiceUsed(String[] names) throws InterruptedException {

		if (names == null) {
			throw new NullPointerException("names[] is null");
		}

		Arrays.sort(names);

		for (String name : names) {

			while (this.usedServices.contains(name)) {
                try {
                    logger.info("wait for unlock service {} begin", name);
                    wait();
                } finally {
                    logger.info("wait for unlock service {} end", name);
                }
			}

			this.usedServices.add(name);

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
			this.usedServices.remove(name);

            setStoreMessageMode(name, false);
		}

		notifyAll();
	}

	@Override
	public synchronized Set<String> getUsedServices() {
		return new HashSet<>(this.usedServices);
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
	public Future<?> copyService(final ServiceName from, final ServiceName to, final IServiceNotifyListener notifyListener) {

        return serviceExecutor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    lock.writeLock().lock();
                    if (services.containsKey(to)) {
                        throw new StorageException("Service " + to + " already exits");
                    }
                    final ServiceContainer serviceContainer = services.get(from);

                    if (serviceContainer == null) {
                        throw new ServiceException("Could not find " + from + " ServiceContainer");
                    }

                    final ServiceDescription description = serviceContainer.getServiceDescription();

                    if (description == null) {
                        throw new ServiceException("Could not find " + from + " ServiceDescription");
                    }

                    final SailfishURI serviceURI = description.getType();

                    final IServiceSettings settings = staticServiceFactory.createServiceSettings(serviceURI);

                    if (settings == null) {
                        ServiceException exception = new ServiceException("Service with URI " + serviceURI + " can not be created. Service settings class was not found");
                        exceptionNotify(notifyListener, exception);
                    }

                    BeanConfigurator.copyBean(description.getSettings(), settings);
                    addServiceWithoutNewThread(to, serviceURI, settings, notifyListener);
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
	        this.lock.readLock().lock();
    		ServiceContainer serviceContainer = this.services.get(serviceName);

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
            this.lock.readLock().unlock();
        }
	}

    /**
     * @param serviceName
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
		ServiceContainer serviceContainer = this.services.get(ServiceName.parse(serviceName));
		if (serviceContainer != null) {
            OptionalServiceHandlerWrapper handler = serviceContainer.getHandlerWrapper();
            handler.storeMessages(store);
		}
    }

    private Set<String> loadProcessedMessageTypes(ServiceDescription description) throws SailfishURIException {
        String value = description.getSettings().getStoredMessageTypes();
        if (StringUtils.isNotBlank(value)) {
            value = ServiceUtil.loadStringFromAlias(this.serviceContext.getDataManager(), value, ",");
            return Arrays.stream(value.split(","))
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
            this.handlerWrapper.setOriginServiceHandler(serviceHandler);
        }
    }
}