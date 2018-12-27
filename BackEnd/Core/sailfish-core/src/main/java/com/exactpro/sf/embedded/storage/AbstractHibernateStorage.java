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
package com.exactpro.sf.embedded.storage;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.storage.IStorage;
import com.exactpro.sf.storage.impl.HibernateStorage;

public abstract class AbstractHibernateStorage implements IHibernateStorage {

    private static final Logger logger = LoggerFactory.getLogger(AbstractHibernateStorage.class);
    
    protected final IStorage storage;
    
    public AbstractHibernateStorage(HibernateStorageSettings settings) {
        // Init hibernate
        Configuration cfg = new Configuration()
        .setProperty("hibernate.dialect", settings.getDialect())
        .setProperty("hibernate.connection.driver_class", settings.getDriverClass())
        .setProperty("hibernate.connection.url", settings.buildConnectionUrl())
        .setProperty("hibernate.connection.username", settings.getUsername())
        .setProperty("hibernate.connection.password", settings.getPassword())
        .setProperty("hibernate.connection.pool_size", "1")
        
        .setProperty("hibernate.hbm2ddl.auto", "none")
        
        .setProperty("show_sql", "false")
        .setProperty("format_sql", "false")
        
        .setProperty("javax.persistence.validation.mode", "none")
        
        // C3P0
        
        .setProperty("hibernate.c3p0.acquire_increment", "1")
        .setProperty("hibernate.c3p0.idle_test_period", "500")
        .setProperty("hibernate.c3p0.max_size", "5")
        .setProperty("hibernate.c3p0.max_statements", "10")
        .setProperty("hibernate.c3p0.min_size", "3")
        .setProperty("hibernate.c3p0.timeout", "100");
        
        configure(settings, cfg);
        
        ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().applySettings(cfg.getProperties()).build();

        SessionFactory sessionFactory = cfg.buildSessionFactory(serviceRegistry);
        
        this.storage = new HibernateStorage(sessionFactory);
        
        configure(settings, sessionFactory);
    }
    
    public void add(Object entity) {

        logger.debug("Try to add entity to statistic db:{}", entity);
        
        this.storage.add(entity);
        
    }
    
    public void update(Object entity) {

        logger.debug("Try to update entity to statistic db:{}", entity);
        
        this.storage.update(entity);
        
    }
    
    public synchronized void tearDown() {
        if (storage.getSessionFactory() != null) {
            storage.getSessionFactory().close();
        }
    }
    
    protected abstract void configure(HibernateStorageSettings settings, Configuration configuration);
    
    protected void configure(HibernateStorageSettings settings, SessionFactory sessionFactory) {
        //DO NOTHING
    }
}
