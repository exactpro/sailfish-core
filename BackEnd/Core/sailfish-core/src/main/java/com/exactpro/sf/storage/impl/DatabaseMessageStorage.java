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

import java.io.FileNotFoundException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import java.time.Instant;

import com.exactpro.sf.common.messages.IHumanMessage;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.MessageUtil;
import com.exactpro.sf.common.messages.MsgMetaData;
import com.exactpro.sf.common.services.ServiceInfo;
import com.exactpro.sf.common.util.HexDumper;
import com.exactpro.sf.configuration.DictionaryManager;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.configuration.workspace.WorkspaceStructureException;
import com.exactpro.sf.storage.IObjectFlusher;
import com.exactpro.sf.storage.MessageFilter;
import com.exactpro.sf.storage.MessageRow;
import com.exactpro.sf.storage.ScriptRun;
import com.exactpro.sf.storage.StorageException;
import com.exactpro.sf.storage.TimestampToString;
import com.exactpro.sf.storage.entities.StoredMessage;
import com.exactpro.sf.storage.entities.StoredScriptRun;
import com.exactpro.sf.util.CHMInterner;
import com.exactpro.sf.util.Interner;

public class DatabaseMessageStorage extends AbstractMessageStorage {
    private final static int REMOVE_BATCH_SIZE = 5000;

	private final SessionFactory sessionFactory;
	private final Interner<String> interner;
    private final IObjectFlusher<StoredMessage> flusher;

	public DatabaseMessageStorage(IWorkspaceDispatcher workspaceDispatcher, SessionFactory sessionFactory, DictionaryManager dictionaryManager) throws WorkspaceStructureException, FileNotFoundException {
	    super(dictionaryManager);
	    

		this.sessionFactory = sessionFactory;
        this.interner = new CHMInterner<>();

		/*
		 * We must create this run to make sure that messagestorage could store
		 * messages during Service initialisation phase
		 */
		this.openScriptRun("Initialisation", "Initialisation of services");

        this.flusher = new ObjectFlusher<>(new HibernateFlushProvider<StoredMessage>(this.sessionFactory), BUFFER_SIZE);
        this.flusher.start();
	}


    @Override
    protected void storeMessage(IMessage message, IHumanMessage humanMessage, String jsonMessage) {
		StoredMessage strMsg = new StoredMessage();
		MsgMetaData metaData = message.getMetaData();
		ServiceInfo serviceInfo = metaData.getServiceInfo();

		if (metaData.getFromService() == null) {
			logger.debug("FromService is null");
		}

		strMsg.setAdmin(metaData.isAdmin());
		strMsg.setArrived(new Timestamp(metaData.getMsgTimestamp().getTime()));
		strMsg.setFrom(metaData.getFromService());
		strMsg.setTo(metaData.getToService());
		strMsg.setName(metaData.isRejected() ? metaData.getMsgName() + MessageUtil.MESSAGE_REJECTED_POSTFIX : metaData.getMsgName());
		strMsg.setNamespace(metaData.getMsgNamespace());
		strMsg.setHumanMessage(humanMessage.toString());
		strMsg.setJsonMessage(jsonMessage);
		strMsg.setRawMessage(metaData.getRawMessage());
		strMsg.setSubMessage(false);
		strMsg.setServiceId(serviceInfo != null ? serviceInfo.getID() : null);
		strMsg.setStoredId(metaData.getId());
		strMsg.setRejectReason(metaData.getRejectReason());

		flusher.add(strMsg);
	}

	@Override
	public synchronized ScriptRun openScriptRun(String name, String description) {
        ScriptRun scriptRun = createScriptRun(name, description);
        StoredScriptRun storedScriptRun = new StoredScriptRun();

        storedScriptRun.setDescription(scriptRun.getDescription());
        storedScriptRun.setScriptName(scriptRun.getScriptName());
        storedScriptRun.setUser(scriptRun.getUser());
        storedScriptRun.setStart(scriptRun.getStart());
        storedScriptRun.setHostName(scriptRun.getHostName());
        storedScriptRun.setMachineIP(scriptRun.getMachineIP());

		Session session = null;
		Transaction tx = null;

		try {

			session = this.sessionFactory.openSession();
			tx = session.beginTransaction();
			session.save(storedScriptRun);
			tx.commit();

		} catch (RuntimeException e) {

			if (tx != null)
				tx.rollback();

			logger.error("Could not create script run", e);

		} finally {
			if (session != null)
				session.close();
		}

        scriptRun.setId(storedScriptRun.getId());

		return scriptRun;

	}

	@Override
	public void closeScriptRun(ScriptRun scriptRun) {
		Session session = null;
		Transaction tx = null;

		try {

			session = this.sessionFactory.openSession();
			tx = session.beginTransaction();
            StoredScriptRun storedScriptRun = new StoredScriptRun();

            storedScriptRun.setId(scriptRun.getId());
            storedScriptRun.setStart(scriptRun.getStart());
            storedScriptRun.setFinish(new Timestamp(new Date().getTime()));
            storedScriptRun.setScriptName(scriptRun.getScriptName());
            storedScriptRun.setDescription(scriptRun.getDescription());
            storedScriptRun.setUser(scriptRun.getUser());
            storedScriptRun.setMachineIP(scriptRun.getMachineIP());
            storedScriptRun.setHostName(scriptRun.getHostName());

			session.update(storedScriptRun);
			tx.commit();

		} catch (RuntimeException e) {

			if (tx != null)
				tx.rollback();

			logger.error("Could not close script run", e);

		} finally {
			if (session != null)
				session.close();
		}
	}

	@Override
	public void dispose() {
		this.flusher.stop();

		this.sessionFactory.close();
	}

	// FIXME: it does not work properly in case of multiple scripts running(it
	// is my fault).
	// besides it is a nasty solution to use String in place of Criteria to
	// retrieve data from a DB layer.
	@Override
	public Iterable<MessageRow> getMessages(int count, MessageFilter filter) {
		flusher.flush();
		return new LazyLoadingIterable(count, filter);
	}

	@Override
	public List<MessageRow> getMessages(int offset, int count, String where) {
		Session session = null;

		flusher.flush();

		try {
			if(count == 0) {
				return new ArrayList<>();
			}

			session = this.sessionFactory.openSession();

			String strQuery = "from StoredMessage msg ";

			if (!where.isEmpty())
				strQuery += " where " + where;
			strQuery += " order by msg.id desc, msg.arrived desc ";

			Query query = session.createQuery(strQuery);

			logger.debug("query: {}", query.getQueryString());

			query.setFirstResult(offset);

			if (count != -1)
				query.setMaxResults(count);

			List<MessageRow> result = new ArrayList<>();

            @SuppressWarnings("unchecked")
            List<StoredMessage> resultList = query.list();

			session.close();

            for(int i = 0; i < resultList.size(); i++) {
                result.add(convert(resultList.get(i), interner, true));
			}

			return result;

		} catch (RuntimeException e) {

			logger.error("Could not retrieve messages", e);
			throw new StorageException("Could not retrieve messages", e);

		} finally {
			if (session != null && session.isOpen()) {
				session.close();
			}
		}

	}

    public static MessageRow convert(StoredMessage message, Interner<String> interner, boolean hex) {
        MessageRow row = new MessageRow();

        row.setID(interner.intern(String.valueOf(message.getId())));
        row.setMsgName(interner.intern(message.getName()));
        row.setMsgNamespace(interner.intern(message.getNamespace()));
        row.setTimestamp(interner.intern(MessageRow.TIMESTAMP_FORMAT.get().format(message.getArrived())));
        row.setFrom(interner.intern(message.getFrom()));
        row.setTo(interner.intern(message.getTo()));
        row.setJson(interner.intern(message.getJsonMessage()));
        row.setContent(interner.intern(message.getHumanMessage()));
        row.setMetaDataID(interner.intern(String.valueOf(message.getStoredId())));
        row.setRejectReason(interner.intern((message.getRejectReason())));

        if(message.getRawMessage() != null) {
            if(hex) {
                HexDumper dumper = new HexDumper(message.getRawMessage());
                row.setRawMessage(interner.intern(dumper.getHexdump()));
                row.setPrintableMessage(interner.intern(dumper.getPrintableString()));
            } else {
                row.setRawMessage(interner.intern(new String(message.getRawMessage())));
            }
        } else {
            row.setRawMessage(interner.intern("null"));
        }

        return row;
    }

	@Override
    public void removeMessages(Instant olderThan) {
        while(removeMessages(olderThan, null, REMOVE_BATCH_SIZE));
	}

    @Override
    public void removeMessages(String serviceID) {
        while(removeMessages(null, serviceID, REMOVE_BATCH_SIZE));
    }

    @Override
    public void clear() {
        while(removeMessages(null, null, REMOVE_BATCH_SIZE));
    }

    private boolean removeMessages(Instant olderThan, String serviceID, int limit) {
        flusher.flush();
        Session session = null;
        Transaction tx = null;
        boolean successful = false;

        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            String hql = "delete StoredMessage where id < :lastID";

            if(olderThan != null) {
                hql += " and arrived < :olderThan";
            }

            if(serviceID != null) {
                hql += " and serviceId = :serviceID";
            }

            Query query = session.createQuery(hql);
            query.setLong("lastID", getFirstMessageID(session, serviceID) + limit);

            if(olderThan != null) {
                query.setString("olderThan", TimestampToString.instantToString(olderThan));
            }

            if(serviceID != null) {
                query.setString("serviceID", serviceID);
            }

            successful = query.executeUpdate() > 0;
            tx.commit();
        } catch(HibernateException e) {
            if(tx != null) {
                tx.rollback();
            }

            throw new StorageException("Failed to remove messages", e);
        } finally {
            if(session != null) {
                session.close();
            }
        }

        return successful;
    }

    private long getFirstMessageID(Session session, String serviceID) {
        String hql = "select msg.id from StoredMessage msg";

        if(serviceID != null) {
            hql += " where msg.serviceId = :serviceID";
        }

        Query query = session.createQuery(hql + " order by msg.id asc");

        if(serviceID != null) {
            query.setString("serviceID", serviceID);
        }

        return (long)ObjectUtils.defaultIfNull(query.setMaxResults(1).uniqueResult(), -1L);
    }

    private class LazyLoadingIterable extends MessageRowLoaderBase<StoredMessage> {

        public LazyLoadingIterable(int count, MessageFilter filter) {
            super(filter, count);
		}

		@Override
        protected void retrieveMessages(Queue<StoredMessage> forMessages, int count, long lastID) {

            Session session = null;

            try {
                session = sessionFactory.openSession();

                Criteria criteria = session.createCriteria(StoredMessage.class);
                boolean sortOrder = true; // ascending

                if (filter.getFrom() != null) {
                    criteria.add(Restrictions.ilike("from", filter.getFrom()));
                }

                if (filter.getTo() != null) {
                    criteria.add(Restrictions.ilike("to", filter.getTo()));
                }

                if (filter.getMsgName() != null) {
                    criteria.add(Restrictions.ilike("name", filter.getMsgName()));
                }

                if (filter.getMsgNameSpace() != null) {
                    criteria.add(Restrictions.ilike("namespace", filter.getMsgNameSpace()));
                }

                if (filter.getHumanMessage() != null) {
				    criteria.add(Restrictions.ilike("humanMessage", filter.getHumanMessage()));
                }

                if (filter.getShowAdmin() != null) {
                    criteria.add(Restrictions.eq("admin", filter.getShowAdmin()));
                }

                if (filter.getStartTime() != null) {
                    criteria.add(Restrictions.ge("arrived", filter.getStartTime()));
                }

                if (filter.getFinishTime() != null) {
                    criteria.add(Restrictions.le("arrived", filter.getFinishTime()));
                }

                if (filter.getSortOrder() != null) {
                    sortOrder = filter.getSortOrder();
                }

                if (filter.getServicesIdSet() != null) {
                    Set<String> servicesId = filter.getServicesIdSet();
                    if (CollectionUtils.isNotEmpty(servicesId)) {
                        criteria.add(Restrictions.in("serviceId", servicesId));
                    } else {
                        logger.error("servicesIdSet is enpty");
                    }
                }

                if (sortOrder == false) {
                    criteria.addOrder(Order.desc("arrived"));
                    criteria.addOrder(Order.desc("id"));
                    criteria.add(Restrictions.lt("id", lastID));
                } else {
                    criteria.addOrder(Order.asc("arrived"));
                    criteria.addOrder(Order.asc("id"));
                    criteria.add(Restrictions.gt("id", lastID));
                }

                criteria.setMaxResults(count);

                for (@SuppressWarnings("unchecked")
                Iterator<StoredMessage> iterator = (Iterator<StoredMessage>) criteria.list().iterator(); iterator.hasNext();) {
                    StoredMessage message = iterator.next();
                    forMessages.add(message);
                }
            } catch (RuntimeException e) {
                logger.error("Could not retrieve messages", e);
                throw new StorageException("Could not retrieve messages", e);
            } finally {
                if (session != null && session.isOpen()) {
                    session.close();
                }
            }
        }

        @Override
        protected MessageRow convert(StoredMessage msg, Interner<String> interner) {
            return DatabaseMessageStorage.convert(msg, interner, isHex);
        }

        @Override
        protected long getID(StoredMessage msg) {
            return msg.getId();
		}
	}
}
