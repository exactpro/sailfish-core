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

import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.JDBCConnectionException;
import org.hibernate.exception.SQLGrammarException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.storage.IStorage;
import com.exactpro.sf.storage.StorageException;

public class HibernateStorage implements IStorage {

	private static final Logger logger = LoggerFactory.getLogger(HibernateStorage.class);

    private final SessionFactory sessionFactory;

    private static final int BATCH_SIZE = 50;
	
	public HibernateStorage(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
		
		logger.info("HibernateStorage initialized");
	}
	
	@Override
	public void add(Object entity) throws StorageException {
		
		Session session = null;
		Transaction tx = null;
		
		try {
            session = sessionFactory.openSession();
			tx = session.beginTransaction();
			
			session.save(entity);
			tx.commit();
			
		} catch (ConstraintViolationException e) {
			
			if (tx != null) {
				tx.rollback();
			}
			throw new StorageException(e.getMessage(), e);
		
		} catch (JDBCConnectionException e) {
			
			if (tx != null) {
				tx.rollback();
			}
			throw new StorageException("JDBC connection exception", e);
		
		} catch (SQLGrammarException e) {
			
			if (tx != null) {
				tx.rollback();
			}
			throw new StorageException("SQL grammar exception", e);
		
		} catch (HibernateException e) {
			
			if (tx != null) {
				tx.rollback();
			}
			throw new StorageException("Unknown exception", e);
			
		} finally {
            if(session != null) {
                session.close();
            }
		}
		
	}
	
	public void batchAdd(List<Object> entities) throws StorageException {
        batchOperation(entities, (session, entity) -> session.save(entity));
	}

    @Override
    public void batchUpdate(List<Object> entities) throws StorageException {
        batchOperation(entities, (session, entity) -> session.update(entity));
    }

	@Override
	public void update(Object entity) throws StorageException {
		
		Session session = null;
		Transaction tx = null;
		
		try {
            session = sessionFactory.openSession();
			tx = session.beginTransaction();
			
			session.update(entity);
			tx.commit();
			
		} catch (ConstraintViolationException e) {
			
			if (tx != null) {
				tx.rollback();
			}
			throw new StorageException(e.getMessage(), e);
		
		} catch (JDBCConnectionException e) {
			
			if (tx != null) {
				tx.rollback();
			}
			throw new StorageException("JDBC connection exception", e);
		
		} catch (SQLGrammarException e) {
			
			if (tx != null) {
				tx.rollback();
			}
			throw new StorageException("SQL grammar exception", e);
		
		} catch (HibernateException e) {
			
			if (tx != null) {
				tx.rollback();
			}
			throw new StorageException("Unknown exception", e);
			
		} finally {
			if (session != null) {
				session.close();
			}
		}
		
		
	}

	@Override
	public void delete(Object entity) throws StorageException {
		
		Session session = null;
		Transaction tx = null;
		
		try {
            session = sessionFactory.openSession();
			tx = session.beginTransaction();
			
			session.delete(entity);
			tx.commit();
			
		} catch (ConstraintViolationException e) {
			
			if (tx != null) {
				tx.rollback();
			}
			throw new StorageException("Constraint violation exception", e);
		
		} catch (JDBCConnectionException e) {
			
			if (tx != null) {
				tx.rollback();
			}
			throw new StorageException("JDBC connection exception", e);
		
		} catch (SQLGrammarException e) {
			
			if (tx != null) {
				tx.rollback();
			}
			throw new StorageException("SQL grammar exception", e);
		
		} catch (HibernateException e) {
			
			if (tx != null) {
				tx.rollback();
			}
			throw new StorageException("Unknown exception", e);
			
		} finally {
			if (session != null) {
				session.close();
			}
		}
		
	}
	
	@Override
	public Object getEntityById(Class<?> entityClass, Long id) {
		
		Session session = null;
		
		try {
            session = sessionFactory.openSession();
			
			return session.get(entityClass, id);
			
		} finally {
			if (session != null) {
				session.close();
			}
		}
		
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public <T> T getEntityById(Class<T> entityClass, String id) {
		
		Session session = null;
		
		try {
            session = sessionFactory.openSession();
			
			return (T) session.get(entityClass, id);
			
		} finally {
			if (session != null) {
				session.close();
			}
		}
		
	}
	
	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <T> T getEntityByField(Class<T> entityClass, String fieldName, Object value) {
		
		Session session = null;
		
		try {

            session = sessionFactory.openSession();
			
			Criteria criteria = session.createCriteria(entityClass);
			
			criteria.add(Restrictions.eq(fieldName, value));
			
			List list = criteria.list();

            return list == null || list.isEmpty() ? null : (T)list.get(0);
        } finally {
			if (session != null) {
				session.close();
			}
		}
		
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public <T> List<T> getAllEntities(Class<T> entityClass) {
		
		Session session = null;
		
		try {
            session = sessionFactory.openSession();
			
			Criteria criteria = session.createCriteria(entityClass);
			criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
			
			return criteria.list();
			
		} finally {
            if(session != null) {
                session.close();
            }
		}
		
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> List<T> getAllEntities(String className) {
		
		Session session = null;
		
		try {
            session = sessionFactory.openSession();
			
			return session.createQuery(String.format("from %s", className)).list();
			
		} finally {
            if(session != null) {
                session.close();
            }
		}
		
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public <T> List<T> getAllEntities(Class<T> entityClass, List<Criterion> criterions) {
		
		Session session = null;
		
		try {
            session = sessionFactory.openSession();
			
			Criteria criteria = session.createCriteria(entityClass);
			
			for (Criterion criterion : criterions) {
				criteria.add(criterion);
			}
			
			return criteria.list();
			
		} finally {
            if(session != null) {
                session.close();
            }
		}
		
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public <T> List<T> getAllEntities(Class<T> entityClass, List<Criterion> criterions, String orderField, boolean orderAsc) {
		
		Session session = null;
		
		try {
            session = sessionFactory.openSession();
			
			Criteria criteria = session.createCriteria(entityClass);
			
			for (Criterion criterion : criterions) {
				criteria.add(criterion);
			}

            criteria.addOrder(orderAsc ? Order.asc(orderField) : Order.desc(orderField));
            criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
			
			return criteria.list();
			
		} finally {
            if(session != null) {
                session.close();
            }
		}
		
	}
	
	@Override
	public void clear(String[] entities) {
		
		Session session = null;
		Transaction tx = null;
		
		try {
            session = sessionFactory.openSession();
			tx = session.beginTransaction();
			
			Query query;

            logger.debug("Process 'clear' has been started ...");
			
			for (String entity : entities) {
				query = session.createQuery("DELETE "+entity);
				query.executeUpdate();
				logger.debug("Entity {} has deleted", entity);
			}
			
			tx.commit();

            logger.debug("Process 'clear' has been has been ended");
			
			
		} finally {
            if(session != null) {
                session.close();
            }
		}
		
	}
	
	@Override
	public SessionFactory getSessionFactory() {
        return sessionFactory;
	}

    private void batchOperation(List<Object> entities, BiConsumer<Session, Object> sessionOperation) {
        Session session = null;
        Transaction tx = null;
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            Iterator<Object> iterator = entities.iterator();
            int count = 0;
            while (iterator.hasNext()) {
                Object entity = iterator.next();
                sessionOperation.accept(session, entity);
                if ( ++count % BATCH_SIZE == 0 ) {
                    session.flush();
                    session.clear();
                }
            }
            tx.commit();
        } catch (ConstraintViolationException e) {
            if (tx != null) {
                tx.rollback();
            }
            throw new StorageException(e.getMessage(), e);
        } catch (JDBCConnectionException e) {
            if (tx != null) {
                tx.rollback();
            }
            throw new StorageException("JDBC connection exception", e);
        } catch (SQLGrammarException e) {
            if (tx != null) {
                tx.rollback();
            }
            throw new StorageException("SQL grammar exception", e);
        } catch (HibernateException e) {
            if (tx != null) {
                tx.rollback();
            }
            throw new StorageException("Unknown exception", e);
        } finally {
            if(session != null) {
                session.close();
            }
        }
    }
}
