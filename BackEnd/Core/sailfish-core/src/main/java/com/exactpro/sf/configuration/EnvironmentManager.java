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
package com.exactpro.sf.configuration;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.scriptrunner.EnvironmentSettings;
import com.exactpro.sf.scriptrunner.IConnectionManager;
import com.exactpro.sf.storage.IMessageStorage;
import com.exactpro.sf.storage.IServiceStorage;

/**
 * The class is part of ScriptContext
 *
 */
public class EnvironmentManager implements IEnvironmentManager
{
    private static final Logger logger = LoggerFactory.getLogger(EnvironmentManager.class) ;

    private final ReadWriteLock envSettingsLock = new ReentrantReadWriteLock(false);

    private final IMessageStorage messageStorage;

    private final IServiceStorage serviceStorage;

    private final IConnectionManager connectionManager;

    private final EnvironmentSettings envSettings;

    public EnvironmentManager(final IMessageStorage messageStorage, final IServiceStorage serviceStorage, final IConnectionManager connectionManager, final EnvironmentSettings envSettings) throws Exception {
		logger.info("Environment manager initializing started...");
		this.messageStorage = messageStorage;
        this.serviceStorage = serviceStorage;
		this.connectionManager = connectionManager;
		this.envSettings = envSettings;
	}

    @Override
    public void updateEnvironmentSettings(EnvironmentSettings envSettings) {
        try {
            this.envSettingsLock.writeLock().lock();
            this.envSettings.set(envSettings);
        } finally {
            this.envSettingsLock.writeLock().unlock();
        }
    }

    @Override
    public EnvironmentSettings getEnvironmentSettings() {
        try {
            this.envSettingsLock.readLock().lock();
            return envSettings.clone();
        } finally {
            this.envSettingsLock.readLock().unlock();
        }
    }

    @Override
    public IConnectionManager getConnectionManager() {
        return this.connectionManager;
    }

    @Override
    public IMessageStorage getMessageStorage() {
        return this.messageStorage;
    }

    @Override
    public IServiceStorage getServiceStorage() {
        return this.serviceStorage;
    }
}
