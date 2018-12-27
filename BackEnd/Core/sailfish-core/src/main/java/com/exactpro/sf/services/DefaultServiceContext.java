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

import com.exactpro.sf.common.services.ServiceInfo;
import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.configuration.IDataManager;
import com.exactpro.sf.configuration.IDictionaryManager;
import com.exactpro.sf.configuration.ILoggingConfigurator;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.storage.IMessageStorage;
import com.exactpro.sf.storage.IServiceStorage;

public class DefaultServiceContext implements IServiceContext {

    private final IDictionaryManager dictionaryManager;
    private final IMessageStorage messageStorage;
    private final IServiceStorage serviceStorage;
    private final ILoggingConfigurator loggingConfigurator;
    private final ITaskExecutor taskExecutor;
    private final IDataManager dataManager;
    private final IWorkspaceDispatcher workspaceDispatcher;

    /**
     * @param dictionaryManager
     * @param messageStorage
     * @param loggingConfigurator
     * @param taskExecutor
     * @param dataManager
     * @param workspaceDispatcher
     * @param serviceStorage
     */
    public DefaultServiceContext(IDictionaryManager dictionaryManager, IMessageStorage messageStorage, IServiceStorage serviceStorage, ILoggingConfigurator loggingConfigurator,
            ITaskExecutor taskExecutor, IDataManager dataManager, IWorkspaceDispatcher workspaceDispatcher) {
        this.dictionaryManager = dictionaryManager;
        this.messageStorage = messageStorage;
        this.serviceStorage = serviceStorage;
        this.loggingConfigurator = loggingConfigurator;
        this.taskExecutor = taskExecutor;
        this.dataManager = dataManager;
        this.workspaceDispatcher = workspaceDispatcher;
    }

    public DefaultServiceContext(IServiceContext prototype, IMessageStorage messageStorage, IServiceStorage serviceStorage) {
        this(prototype.getDictionaryManager(), messageStorage, serviceStorage, prototype.getLoggingConfigurator(), prototype.getTaskExecutor(),
                prototype.getDataManager(), prototype.getWorkspaceDispatcher());
    }

    /* (non-Javadoc)
     * @see com.exactpro.sf.services.IServiceContext#getDictionaryManager()
     */
    @Override
    public IDictionaryManager getDictionaryManager() {
        return this.dictionaryManager;
    }

    /* (non-Javadoc)
     * @see com.exactpro.sf.services.IServiceContext#getMessageStorage()
     */
    @Override
    public IMessageStorage getMessageStorage() {
        return this.messageStorage;
    }

    @Override
    public ServiceInfo lookupService(ServiceName serviceName) {
        return serviceStorage.lookupService(serviceName);
    }

    /* (non-Javadoc)
     * @see com.exactpro.sf.services.IServiceContext#getLoggingConfigurator()
     */
    @Override
    public ILoggingConfigurator getLoggingConfigurator() {
        return this.loggingConfigurator;
    }

    /* (non-Javadoc)
     * @see com.exactpro.sf.services.IServiceContext#getTaskExecutor()
     */
    @Override
    public ITaskExecutor getTaskExecutor() {
        return this.taskExecutor;
    }

    /* (non-Javadoc)
     * @see com.exactpro.sf.services.IServiceContext#getDataManager()
     */
    @Override
    public IDataManager getDataManager() {
        return this.dataManager;
    }

    /* (non-Javadoc)
     * @see com.exactpro.sf.services.IServiceContext#getWorkspaceDispatcher()
     */
    @Override
    public IWorkspaceDispatcher getWorkspaceDispatcher() {
        return this.workspaceDispatcher;
    }

}
