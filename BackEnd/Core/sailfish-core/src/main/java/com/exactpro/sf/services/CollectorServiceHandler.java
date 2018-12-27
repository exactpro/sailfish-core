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
package com.exactpro.sf.services;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.aml.script.CheckPoint;
import com.exactpro.sf.common.messages.IMessage;

public class CollectorServiceHandler implements IServiceHandler {
	private static final Logger logger = LoggerFactory.getLogger(CollectorServiceHandler.class);

    private final Map<ServiceHandlerRoute, ConcurrentMap<ISession, CSHArrayList<IMessage>>> routeToMessages;

	public CollectorServiceHandler() {
        Map<ServiceHandlerRoute, ConcurrentMap<ISession, CSHArrayList<IMessage>>> map = new EnumMap<>(ServiceHandlerRoute.class);

        for (ServiceHandlerRoute route : ServiceHandlerRoute.values()) {
            map.put(route, new ConcurrentHashMap<ISession, CSHArrayList<IMessage>>());
        }

        this.routeToMessages = Collections.unmodifiableMap(map);
	}

	@Override
	public void exceptionCaught(ISession session, Throwable cause) {
		logger.error("exception caught for session: {}", session, cause);
	}

	@Override
    public void putMessage(ISession session, ServiceHandlerRoute route, IMessage message) throws ServiceHandlerException {

	    if (message.getMetaData().isRejected()) {
            logger.debug("Message [{}::{}] is rejected", message.getNamespace(), message.getName());
	        return;
	    }
	    
	    CSHArrayList<IMessage> a = getList(session, route);

        synchronized (a) {
            if (logger.isDebugEnabled()) {
                logger.debug("{}: size={} {} {} {}", route.getAlias(), a.size(), session.getName(), session.getClass().getCanonicalName(), message);
                logger.debug("{}: session hashCode={}", route.getAlias(), Integer.toHexString(session.hashCode()));
                logger.debug("{}: put message to array {}", route.getAlias(), a.getID());
            }

            a.add(message);
            a.notifyAll();
        }
    }

    private CSHArrayList<IMessage> getList(ISession session, ServiceHandlerRoute route) {
        ConcurrentMap<ISession, CSHArrayList<IMessage>> map = routeToMessages.get(route);
        CSHArrayList<IMessage> list = map.get(session);

		if (list == null) {
            CSHArrayList<IMessage> newList = new CSHArrayList<>();
			list = map.putIfAbsent(session, newList);
			//putIfAbsent returns old association of key (null) if map did not contain a key
			if (list == null) {
                logger.debug("getList: create new list: {} for session: {}", newList.getID(), session);
				return newList;
			}
		}

		return list;
	}

    //FIXME: Used only for test purposes. Think of a better way to get checkpoint index
    public int getCheckPointIndex(ISession session, ServiceHandlerRoute route, CheckPoint checkPoint) {
        CSHArrayList<IMessage> list = getList(session, route);

        synchronized(list) {
            return list.getIndex(checkPoint);
        }
    }

    @Override
    public CSHIterator<IMessage> getIterator(ISession session, ServiceHandlerRoute route, CheckPoint checkPoint) {
        CSHArrayList<IMessage> list = getList(session, route);
        return new CSHIterator<>(list, checkPoint);
    }

    @Override
    public List<IMessage> getMessages(ISession session, ServiceHandlerRoute route, CheckPoint checkPoint) {
        CSHArrayList<IMessage> list = getList(session, route);

        synchronized(list) {
            return list.subList(list.getIndex(checkPoint));
        }
    }

	@Override
	public void sessionClosed(ISession session) throws ServiceHandlerException {
		// do nothing
	}

	@Override
	public void sessionIdle(ISession session, IdleStatus status) throws ServiceHandlerException {
		// do nothing
	}

	@Override
	public void sessionOpened(ISession session) throws ServiceHandlerException {
		// do nothing
	}

    /* (non-Javadoc)
     * @see com.exactpro.sf.services.IServiceHandler#registerCheckPoint(com.exactpro.sf.services.ISession, com.exactpro.sf.services.ServiceHandlerRoute, com.exactpro.sf.aml.script.CheckPoint)
     */
    @Override
    public void registerCheckPoint(ISession session, ServiceHandlerRoute route, CheckPoint checkPoint) {
        CSHArrayList<IMessage> list = getList(session, route);

        synchronized(list) {
            list.addCheckPoint(checkPoint);
        }
    }

	@Override
    public void cleanMessages(ServiceHandlerRoute... routes) {
	    if (!ArrayUtils.isEmpty(routes)) {
            logger.debug("clean: {}", (Object) routes);

            for (ServiceHandlerRoute route : routes) {
                this.routeToMessages.get(route).clear();
            }
	    }
    }
}
