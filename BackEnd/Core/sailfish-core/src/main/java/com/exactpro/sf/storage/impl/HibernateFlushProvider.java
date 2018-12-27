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

import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import com.exactpro.sf.storage.IObjectFlusher.IFlushProvider;

public class HibernateFlushProvider<T> implements IFlushProvider<T> {
    private final SessionFactory sessionFactory;

    public HibernateFlushProvider(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public void flush(List<T> objects) throws Exception {
        Session session = null;
        Transaction tx = null;

        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();

            for(T object : objects) {
                session.save(object);
            }

            tx.commit();
        } catch(Exception e) {
            if(tx != null) {
                tx.rollback();
            }

            throw e;
        } finally {
            if(session != null) {
                session.close();
            }
        }
    }
}
