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
package com.exactpro.sf.storage.xml.db;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.exactpro.sf.common.impl.messages.xml.XMLTransmitter;
import com.exactpro.sf.storage.FilterCriterion;
import com.exactpro.sf.storage.SortCriterion;
import com.exactpro.sf.storage.StorageException;
import com.exactpro.sf.storage.xml.DataMessage;
import com.exactpro.sf.storage.xml.DataMessagePersister;
import com.exactpro.sf.storage.xml.XmlDataMessage;

@SuppressWarnings("deprecation")
public class DBPersister implements DataMessagePersister {

	private static SessionFactory factory;

	private static XMLTransmitter transmitter;

	static {
		factory = new Configuration()
				.configure(
                        "/com/exactpro/sf/configuration/hibernate.cfg.xml")
				.buildSessionFactory();
		transmitter = XMLTransmitter.getTransmitter();
	}

	@Override
	public DataMessage getDataMessage(List<FilterCriterion> filterCriterions,
			List<SortCriterion> sortCriterions) throws StorageException {
		return deserialize(retriveUnique(filterCriterions, sortCriterions));
	}

	@Override
	public List<DataMessage> getDataMessages(
			List<FilterCriterion> filterCriterions,
			List<SortCriterion> sortCriterions) throws StorageException {
		List<XmlDataMessage> result = retrive(filterCriterions, sortCriterions);
		List<DataMessage> dataMessages = new LinkedList<>();
		for (XmlDataMessage xmlDataMessage : result) {
			dataMessages.add(deserialize(xmlDataMessage));
		}
		return dataMessages;
	}

	@Override
	public void persist(List<DataMessage> messages) throws StorageException {
		Session session = null;
		try {
		    session = factory.openSession();
    		session.beginTransaction();
    		for (DataMessage message : messages) {
    			session.saveOrUpdate(serialize(message));
    		}
    	} finally {
            if (session != null) {
                session.close();
            }
        }
	}

	@Override
	public void persist(DataMessage message) throws StorageException {
		Session session = null;
		try {
		    session = factory.openSession();
    		session.beginTransaction();
    		session.saveOrUpdate(serialize(message));
		} finally {
            if (session != null) {
                session.close();
            }
        }
	}

	@SuppressWarnings("unchecked")
	private List<XmlDataMessage> retrive(
			List<FilterCriterion> filterCriterions,
			List<SortCriterion> sortCriterions) {
		Criteria criteria = createCriteria(filterCriterions, sortCriterions);
		List<XmlDataMessage> result = criteria.list();
		return result;
	}

	private XmlDataMessage retriveUnique(
			List<FilterCriterion> filterCriterions,
			List<SortCriterion> sortCriterions) {
		Criteria criteria = createCriteria(filterCriterions, sortCriterions);
		return (XmlDataMessage) criteria.uniqueResult();
	}

	private Criteria createCriteria(List<FilterCriterion> filterCriterions,
			List<SortCriterion> sortCriterions){
		Session session = null;
		try {
		    session = factory.openSession();
    		Criteria criteria = session.createCriteria(XmlDataMessage.class);
    		if (filterCriterions != null) {
    			for (FilterCriterion criterion : filterCriterions) {
    				if (criterion.getOper() != FilterCriterion.Operation.LIKE) {
    					continue;
    				}
    				criteria.add(Restrictions.like(XmlDataMessage.RAW_DATA,
    						criterion.getValue(), MatchMode.ANYWHERE));
    			}
    		}
    		if (sortCriterions != null) {
    			for (SortCriterion criterion : sortCriterions) {
    				if (criterion.isSortAscending()) {
    					criteria.addOrder(Order.asc(criterion.getName()));
    				} else {
    					criteria.addOrder(Order.desc(criterion.getName()));
    				}
    			}
    		}
    		return criteria;
		} finally {
		    if (session != null) {
		        session.close();
		    }
		}
	}

	private XmlDataMessage serialize(DataMessage message) {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		byte[] data = null;
		try {
			transmitter.marshal(message, os);
			data = os.toByteArray();
		} catch (Exception e) {
			throw new StorageException(e);
		}

		String xml = new String(data, Charset.forName("ASCII"));
		XmlDataMessage xmlDataMessage = new XmlDataMessage();
		xmlDataMessage.setRawData(xml);
		return xmlDataMessage;
	}

	private DataMessage deserialize(XmlDataMessage message) {
		ByteArrayInputStream is = new ByteArrayInputStream(message.getRawData().getBytes(Charset.forName("ASCII")));
		try {
			return transmitter.unmarshal(DataMessage.class, is);
		} catch (Exception e) {
			throw new StorageException(e);
		}
	}

}
