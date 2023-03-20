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
package com.exactpro.sf.storage.impl;

import java.beans.PropertyDescriptor;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.google.common.collect.Streams;
import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.commons.beanutils.PropertyUtilsBean;
import org.apache.commons.lang3.ObjectUtils;
import org.hibernate.Criteria;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.type.StringType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.services.ServiceInfo;
import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.IDictionaryManager;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.suri.SailfishURIException;
import com.exactpro.sf.scriptrunner.services.IStaticServiceManager;
import com.exactpro.sf.services.IServiceSettings;
import com.exactpro.sf.services.ServiceDescription;
import com.exactpro.sf.services.ServiceEvent;
import com.exactpro.sf.services.ServiceEvent.Level;
import com.exactpro.sf.services.ServiceEvent.Type;
import com.exactpro.sf.storage.FilterCriterion;
import com.exactpro.sf.storage.FilterCriterion.Operation;
import com.exactpro.sf.storage.IMessageStorage;
import com.exactpro.sf.storage.IObjectFlusher;
import com.exactpro.sf.storage.IServiceStorage;
import com.exactpro.sf.storage.SortCriterion;
import com.exactpro.sf.storage.StorageException;
import com.exactpro.sf.storage.StorageFilter;
import com.exactpro.sf.storage.StorageResult;
import com.exactpro.sf.storage.TimestampToString;
import com.exactpro.sf.storage.entities.StoredEnvironment;
import com.exactpro.sf.storage.entities.StoredService;
import com.exactpro.sf.storage.entities.StoredServiceEvent;
import com.exactpro.sf.storage.util.ServiceStorageHelper;

public class DatabaseServiceStorage implements IServiceStorage {
    private static final int REMOVE_BATCH_SIZE = 5000;

    private static final int BUFFER_SIZE = 50;

	private static final Logger logger = LoggerFactory.getLogger(DatabaseServiceStorage.class);

	private final SessionFactory sessionFactory;

	private final IStaticServiceManager staticServiceManager;

	private final IDictionaryManager dictionaryManager;

    private final ConvertUtilsBean converter = new ConvertUtilsBean();

    private final IObjectFlusher<StoredServiceEvent> flusher;

    private final IMessageStorage messageStorage;

	/**
	 * Operations with this collections should be synchronized
	 */
	private final Map<ServiceName, ServiceDescription> descriptionMap = new HashMap<>();

	/**
	 * Will search for hibernate.cfg.xml file in folder specified by 'workFolderPath' parameter.
	 * @param staticServiceManager
	 */
    public DatabaseServiceStorage(SessionFactory sessionFactory, IStaticServiceManager staticServiceManager, IDictionaryManager dictionaryManager, IMessageStorage messageStorage) {

		this.sessionFactory = Objects.requireNonNull(sessionFactory, "sessionFactory cannot be null");
		this.staticServiceManager = Objects.requireNonNull(staticServiceManager, "staticServiceManager cannot be null");
		this.dictionaryManager = Objects.requireNonNull(dictionaryManager, "dictionaryManager cannot be null");
        this.messageStorage = Objects.requireNonNull(messageStorage, "messageStorage cannot be null");

		this.flusher = new ObjectFlusher<>(new HibernateFlushProvider<>(this.sessionFactory), BUFFER_SIZE);
        flusher.start();

		loadServiceDescriptions();
	}

	@Override
    public ServiceInfo lookupService(ServiceName serviceName) {
        Session session = null;

        try {
            session = sessionFactory.openSession();
            Criteria criteria = session.createCriteria(StoredService.class);

            criteria.add(Restrictions.eq("name", serviceName.getServiceName()));
            criteria.createAlias("environment", "environment");
            criteria.add(Restrictions.eq("environment.name", serviceName.getEnvironment()));

            StoredService storedService = (StoredService)criteria.uniqueResult();

            // hotfix to load services from default environment of old DB
            if (storedService == null && serviceName.isDefault()) {
                Criteria defaultServiceCriteria = session.createCriteria(StoredService.class);
                defaultServiceCriteria.add(Restrictions.eq("name", serviceName.getServiceName()));
                defaultServiceCriteria.add(Restrictions.isNull("environment"));
                storedService = (StoredService)defaultServiceCriteria.uniqueResult();
            }

            if (storedService == null) {
                throw new StorageException("Could not get the '" + serviceName + "' service from data base");
            }
            return new ServiceInfo(storedService.getId(), serviceName);
        } catch (StorageException e) {
            throw e;
        } catch (RuntimeException e) {
            logger.error("Could not retrieve a service for name: {}", serviceName, e);
            throw new StorageException("Could not retrieve a service for name: " + serviceName, e);
        } finally {
            if(session != null) {
                session.close();
            }
        }

    }

	@Override
	public synchronized void addServiceDescription(ServiceDescription description) {

        StoredService stored = convertServiceDescription(description);

		Session session = null;
		Transaction tx = null;

		try {
            session = sessionFactory.openSession();
			tx = session.beginTransaction();
			session.setFlushMode(FlushMode.COMMIT); // do not delete this line
			// cause default mode is not declared and equal FlushMode.AUTO
			session.save(stored);

			tx.commit();

            synchronized (descriptionMap) {
                descriptionMap.put(new ServiceName(description.getEnvironment(), description.getName()), description);
            }

		} catch (ConstraintViolationException e) {

            if(tx != null) {
                tx.rollback();
            }
            String message = "Service with name " + description.getEnvironment() + ":" + description.getName() + " already exists";
			logger.error(message, e);
			throw new StorageException(message, e);

		} catch ( RuntimeException e ){

            if(tx != null) {
                tx.rollback();
            }

			String message = "Could not store a service description " + description.getEnvironment() + ":" + description.getName();
			logger.error(message, e);
			throw new StorageException(message, e);

		} finally{
            if(session != null) {
                session.close();
            }
		}

	}

    @Override
    public void removeServiceDescriptions(Iterator<ServiceDescription> iterator) {

        flusher.flush();

        Session session = null;
        Transaction tx = null;

        try {

            session = sessionFactory.openSession();
            session.setFlushMode(FlushMode.COMMIT); // do not delete this line
            // cause default mode is not declared and equal FlushMode.AUTO

            tx = session.beginTransaction();

            Session finalSession = session;
            List<ServiceName> toRemove = new ArrayList<>();

            Streams.stream(iterator).collect(Collectors.groupingBy(ServiceDescription::getEnvironment)).forEach((env, descriptions) -> {

                StoredEnvironment storedEnvironment = (StoredEnvironment) finalSession.createCriteria(StoredEnvironment.class)
                        .add(Restrictions.eq("name", env)).uniqueResult();

                descriptions.forEach(description -> {

                    synchronized (description) {
                        StoredService storedService = getStoredService(description);

                        removeServiceEvents(description);
                        // FIXME: removing messages breaks retrieve after BB run
                        //messageStorage.removeMessages(stored.getId());

                        storedEnvironment.getServices().remove(storedService);

                        toRemove.add(description.getServiceName());
                    }
                });

                finalSession.update(storedEnvironment);
            });

            tx.commit();

            synchronized (descriptionMap) {
                //will be commited all descriptions or zero
                toRemove.forEach(descriptionMap::remove);
            }

        } catch (RuntimeException e) {

            if (tx != null) {
                tx.rollback();
            }

            String message = "Could not delete a service descriptions";
            logger.error(message, e);
            throw new StorageException(message, e);

        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

	@Override
	public void removeServiceDescription(ServiceDescription description) {
        flusher.flush();

		Session session = null;
		Transaction tx = null;

		try	{
			synchronized (description) {
			    StoredService storedService = getStoredService(description);
			    session = sessionFactory.openSession();
				session.setFlushMode(FlushMode.COMMIT); // do not delete this line
				// cause default mode is not declared and equal FlushMode.AUTO

                removeServiceEvents(description);
                // FIXME: removing messages breaks retrieve after BB run
                //messageStorage.removeMessages(stored.getId());

				tx = session.beginTransaction();

                StoredEnvironment storedEnvironment = (StoredEnvironment) session.createCriteria(StoredEnvironment.class)
                        .add(Restrictions.eq("name", description.getEnvironment()))
                        .uniqueResult();

                storedEnvironment.getServices().remove(storedService);

				session.update(storedEnvironment);
				tx.commit();

                synchronized (descriptionMap) {
                    descriptionMap.remove(description.getServiceName());
                }
			}

		} catch ( RuntimeException e ) {

            if(tx != null) {
                tx.rollback();
            }

			String message = "Could not delete a service description " + description.getEnvironment() + ":" + description.getName();
			logger.error(message, e);
			throw new StorageException(message, e);

		} finally {
			if ( session != null ) {
				session.close();
			}
		}
	}

    @Override
	public void updateServiceDescription(ServiceDescription description) {

		StoredService stored = getStoredService(description);

		Map<String, String> params = new HashMap<>();

        ServiceStorageHelper.setServiceSettingsToMap(params, description.getSettings());

		stored.setParameters(params);
        stored.setVariables(description.getVariables());

		Session session = null;
		Transaction tx = null;

		try {
			synchronized (descriptionMap) {
                session = sessionFactory.openSession();
				tx = session.beginTransaction();
				session.update(stored);
				tx.commit();
                // String environment = stored.getEnvironment() == null ? null : stored.getEnvironment().getName();
                descriptionMap.put(new ServiceName(description.getEnvironment(), stored.getName()), description);
			}

		} catch (ConstraintViolationException e) {

            if(tx != null) {
                tx.rollback();
            }

			String message = "Service with name " + description.getEnvironment() + ":" + description.getName() + " already exists";
            logger.error(message, e);
            throw new StorageException(message, e);

		} catch ( RuntimeException e ) {

            if(tx != null) {
                tx.rollback();
            }

			String message = "Could not update service description " + description.getEnvironment() + ":" + description.getName();
			logger.error(message, e);
			throw new StorageException(message, e);

		} finally {
            if(session != null) {
                session.close();
            }
		}

	}

	@Override
	public List<ServiceDescription> getServiceDescriptions() {
        synchronized (descriptionMap) {
            return new ArrayList<>(descriptionMap.values());
        }
	}

	@Override
	public void addServiceEvent(ServiceDescription description, ServiceEvent event) {
		StoredServiceEvent stEvent = convertServiceEvent(description, event);
        flusher.add(stEvent);
	}

	@Override
	public long getEventsCount(ServiceDescription description, StorageFilter filter) {
        flusher.flush();
		Session session = null;

        try {
            session = sessionFactory.openSession();

			StoredService storedDescription = null;

			synchronized (description) {
				storedDescription = getStoredService(description);
			}

            if(storedDescription == null) {
                throw new StorageException("Could not find stored description for [" + description.getName() + "] service");
            }

            filter.addCriterion(new FilterCriterion("serviceId", storedDescription.getId(), Operation.EQUALS));

			String countQueryStr = "select count(msg) from StoredServiceEvent msg where ";

			Query countQuery = convertFilterToQuery(session, StoredServiceEvent.class, countQueryStr, filter, null);

            return (Long)countQuery.uniqueResult();

		} catch (RuntimeException e) {
		    String message = "Could not retrive events count for service " + description.getEnvironment() + ":" + description.getName();
			logger.error(message, e);
			throw new StorageException(message, e);
		} finally {
            if(session != null) {
                session.close();
            }
		}

    }

	@SuppressWarnings("unchecked")
	@Override
	public StorageResult<ServiceEvent> getServiceEvents(ServiceDescription description, StorageFilter filter, int firstRecord, int size, List<SortCriterion> sorting) {
        flusher.flush();
		Session session = null;

        try {
            session = sessionFactory.openSession();

            if(sorting == null) {
                sorting = new ArrayList<>();
            }

            if(sorting.isEmpty()) {
                sorting.add(new SortCriterion("occured", false));
            }

			String strQuery = "from StoredServiceEvent msg where ";

			StoredService storedDescription = null;

			synchronized (description) {
				storedDescription = getStoredService(description);
			}

            if(storedDescription == null) {
                throw new StorageException("Could not find stored description for [" + description.getName() + "] service");
            }

            filter.addCriterion(new FilterCriterion("serviceId", storedDescription.getId(), Operation.EQUALS));

			String countQueryStr = "select count(msg) from StoredServiceEvent msg where ";

			Query countQuery = convertFilterToQuery(session, StoredServiceEvent.class, countQueryStr, filter, null);

			long recordCount = (Long) countQuery.uniqueResult();

			Query query = convertFilterToQuery(session, StoredServiceEvent.class, strQuery, filter, sorting);

			query.setFirstResult(firstRecord);

			query.setMaxResults(size);

			List<StoredServiceEvent> resultList = query.list();

			List<ServiceEvent> result = new ArrayList<>(resultList.size());

			for ( int i = 0 ; i < resultList.size(); ++i ) {
				StoredServiceEvent storedServiceEvent = resultList.get(i);
				ServiceEvent serviceEvent = convertStoredServiceEvent(storedServiceEvent);
				result.add(serviceEvent);
			}

            return new StorageResult<>(result, recordCount);

		} catch (RuntimeException e) {
		    String message = "Could not retrive service events for " + description.getEnvironment() + ":" + description.getName();
			logger.error(message, e);
			throw new StorageException(message, e);
		} finally {
            if(session != null) {
                session.close();
            }
        }

    }

    @Override
    public void removeServiceEvents(ServiceDescription description) {
        StoredService storedDescription = null;

        synchronized(description) {
            storedDescription = getStoredService(description);
        }

        if(storedDescription == null) {
            throw new StorageException("Could not find stored description for [" + description.getName() + "] service");
        }

        //noinspection ControlFlowStatementWithoutBraces
        while(removeServiceEvents(null, storedDescription.getId(), REMOVE_BATCH_SIZE));
    }

    @Override
    public void removeServiceEvents(Instant olderThan) {
        //noinspection ControlFlowStatementWithoutBraces
        while(removeServiceEvents(olderThan, null, REMOVE_BATCH_SIZE));
    }

    @Override
    public void clearServiceEvents() {
        //noinspection ControlFlowStatementWithoutBraces
        while(removeServiceEvents(null, null, REMOVE_BATCH_SIZE));
    }

    private boolean removeServiceEvents(Instant olderThan, String serviceID, int limit) {
        flusher.flush();
		Session session = null;
		Transaction tx = null;
        boolean successful = false;

		try {
			session = sessionFactory.openSession();
			tx = session.beginTransaction();
            String hql = "delete StoredServiceEvent where id < :lastID";

            if(olderThan != null) {
                hql += " and occured < :olderThan";
            }

            if(serviceID != null) {
                hql += " and serviceId = :serviceID";
            }

			Query query = session.createQuery(hql);
            query.setLong("lastID", getFirstServiceEventID(session, serviceID) + limit);

            if(olderThan != null) {
                query.setString("olderThan", TimestampToString.instantToString(olderThan));
            }

            if(serviceID != null) {
                query.setString("serviceID", serviceID);
            }

            successful = query.executeUpdate() > 0;
			tx.commit();
            return successful;
        } catch(HibernateException e) {
			if (tx != null) {
				tx.rollback();
			}

            throw new StorageException("Failed to remove service events", e);
		} finally {
            if(session != null) {
				session.close();
            }
		}

    }

    private long getFirstServiceEventID(Session session, String serviceID) {
        String hql = "select event.id from StoredServiceEvent event";

        if(serviceID != null) {
            hql += " where event.serviceId = :serviceID";
        }

        Query query = session.createQuery(hql + " order by event.id asc");

        if(serviceID != null) {
            query.setString("serviceID", serviceID);
        }

        return (long)ObjectUtils.defaultIfNull(query.setMaxResults(1).uniqueResult(), -1L);
    }

	@Override
	public void dispose() {
        flusher.stop();
		if(sessionFactory != null) {
			sessionFactory.close();
		}
	}

    private StoredService convertServiceDescription(ServiceDescription description) {

		StoredService stored = null;

		synchronized (descriptionMap) {
			stored = new StoredService();

            if(description.getEnvironment() != null) {

				Session session = null;

				try {

                    session = sessionFactory.openSession();

					Criteria criteria = session.createCriteria(StoredEnvironment.class);
					criteria.add(Restrictions.eq("name", description.getEnvironment()));

					@SuppressWarnings("unchecked")
					List<StoredEnvironment> list = criteria.list();

					StoredEnvironment se = list.get(0);

					stored.setEnvironment(se);

				} finally{
                    if(session != null) {
                        session.close();
                    }
				}
			}
		}

		stored.setName(description.getName());
		stored.setType(description.getType().toString());
		stored.setServiceHandlerClassName(description.getServiceHandlerClassName());
        stored.setVariables(description.getVariables());

        ServiceStorageHelper.setServiceSettingsToMap(stored.getParameters(), description.getSettings());

		return stored;
	}

    private ServiceDescription convertStoredServiceDescription(StoredService storedDescription) throws SailfishURIException {

        ServiceDescription description = null;

        synchronized (descriptionMap) {
            String environment = storedDescription.getEnvironment() == null ? null : storedDescription.getEnvironment().getName();
            ServiceName serviceName = new ServiceName(environment, storedDescription.getName());
            description = descriptionMap.get(serviceName);

            if ( description == null ) {
                SailfishURI serviceURI = SailfishURI.parse(storedDescription.getType());
                description = new ServiceDescription(serviceURI);
                IServiceSettings serviceSettings = staticServiceManager.createServiceSettings(serviceURI);

                description.setSettings(serviceSettings);
                descriptionMap.put(serviceName, description);
            }

        }

		description.setName(storedDescription.getName());
        if(storedDescription.getEnvironment() != null) {
            description.setEnvironment(storedDescription.getEnvironment().getName());
        }
		description.setServiceHandlerClassName(storedDescription.getServiceHandlerClassName());
        description.setVariables(new HashMap<>(storedDescription.getVariables()));

        ServiceStorageHelper.setMapToServiceSettings(description.getSettings(), storedDescription.getParameters());

		return ServiceStorageHelper.processDescription(description, staticServiceManager, dictionaryManager);

	}

	private StoredServiceEvent convertServiceEvent(ServiceDescription descr, ServiceEvent event) {

		StoredService stored = null;

		synchronized (descr) {
			stored = getStoredService(descr);
		}

        if(stored == null) {
            throw new EPSCommonException("Could not find stored description for ServiceDescription [" + descr + "]");
        }

		StoredServiceEvent stEvent = new StoredServiceEvent();

		stEvent.setLevel(event.getLevel().toString());
		stEvent.setType(event.getType().toString());
		stEvent.setOccured(event.getOccurred());
		stEvent.setMessage(event.getMessage());
		stEvent.setDetails(event.getDetails());
		stEvent.setServiceId(stored.getId());

		return stEvent;
	}

	private Query convertFilterToQuery(Session session, Class<?> beanClass, String queryStr, StorageFilter filter, List<SortCriterion> sorting) {

		List<FilterCriterion> list = filter.getCriteria();

		PropertyUtilsBean beanUtils = new PropertyUtilsBean();

		PropertyDescriptor[] properties = beanUtils.getPropertyDescriptors(beanClass);

		Map<String, Class<?>> beanMap = new HashMap<>();
        StringBuilder queryBld = new StringBuilder(queryStr);

        for(PropertyDescriptor descr : properties) {
            beanMap.put(descr.getName(), descr.getPropertyType());
        }

		List<Object> params = new ArrayList<>(list.size());

		for ( int i = 0; i < list.size(); ++i ) {

			FilterCriterion crit = list.get(i);
            if(i != 0) {
                queryBld.append(" and ");
            }

            queryBld.append("msg.").append(crit.getName()).append(" ").append(convertOp(crit.getOper())).append(" :param").append(i).append(" ");

			if (crit.getOper() == Operation.LIKE) {
				params.add(crit.getValue());
			} else {
				params.add(converter.convert(crit.getValue(), beanMap.get(crit.getName())));
			}
		}

        if(sorting != null && !sorting.isEmpty()) {
            queryBld.append(" order by ")
                    .append(sorting.stream()
                            .map(sortCriterion -> new StringBuilder("msg.")
                                    .append(sortCriterion.getName())
                                    .append(sortCriterion.isSortAscending() ? " asc" : " desc")
                            )
                            .collect(Collectors.joining(", ")));
        }

		Query query = session.createQuery(queryBld.toString());

		for ( int i = 0; i < list.size(); ++i ) {
			if (list.get(i).getOper() == Operation.LIKE) {
				query.setParameter("param" + i, params.get(i), StringType.INSTANCE);
			} else {
				query.setParameter("param" + i, params.get(i));
			}
		}
		return query;
	}

    private String convertOp(Operation op) {

		switch (op) {
		case EQUALS:
			return "=";
		case GREATER:
			return ">";
		case LESSER:
			return "<";
		case NOT_EQUALS:
			return "<>";
		case LIKE:
			return "like";
		}

		throw new EPSCommonException("Could not convert operation [" + op + "]");

	}


	private ServiceEvent convertStoredServiceEvent(StoredServiceEvent storedServiceEvent) {

		String type = storedServiceEvent.getType();
		String message = storedServiceEvent.getMessage();
		String details = storedServiceEvent.getDetails();
		String level = storedServiceEvent.getLevel();
		Date date = storedServiceEvent.getOccured();

		//FIXME
		//Need to change mapping to return StoredService
        ServiceEvent serviceEvent = new ServiceEvent(null, Level.valueOf(level), Type.valueOf(type), date, message, details);

		return serviceEvent;
	}

	private StoredService getStoredService(ServiceDescription description) {

		Session session = null;

		try {

            session = sessionFactory.openSession();

			String strQuery = "select service from StoredService service left join service.environment as environment where service.name = :serviceName";

			String environment = description.getEnvironment();
			Query query;

            if(environment != null) {
				strQuery += " and environment.name = :environment";
                query =  session.createQuery(strQuery);
                query.setParameter("environment", environment);
			} else {
                strQuery += " and environment is null";
                query =  session.createQuery(strQuery);
            }

            query.setParameter("serviceName", description.getName());

			List<?> resultList = query.list();

			StoredService stored = (StoredService) resultList.get(0);

			session.update(stored);

			return stored;

		} catch ( RuntimeException e ) {
			logger.error("Could not retrieve StoredService", e);
			throw new StorageException("Could not retrieve StoredService", e);

		} finally {
            if(session != null) {
                session.close();
            }
		}
	}

	@SuppressWarnings("unchecked")
	private void loadServiceDescriptions() {
		Session session = null;

		try {

            session = sessionFactory.openSession();

			String strQuery = "from StoredService";

            if(session != null) {
                Query query = session.createQuery(strQuery);

                List<StoredService> resultList = query.list();

                for ( int i = 0 ; i < resultList.size(); ++i ) {
                    StoredService storedServiceDescription = resultList.get(i);
                    convertStoredServiceDescription(storedServiceDescription);
                }
            }

		} catch ( RuntimeException | SailfishURIException e ){
			logger.error("Could not retrieve service descriptions", e);
			throw new StorageException("Could not retrieve service descriptions", e);

		} finally {
            if(session != null) {
                session.close();
            }
		}
	}
}
