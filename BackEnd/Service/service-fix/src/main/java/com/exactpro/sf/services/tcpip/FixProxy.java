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
package com.exactpro.sf.services.tcpip;

import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.configuration.IDataManager;
import com.exactpro.sf.configuration.IDictionaryManager;
import com.exactpro.sf.configuration.ILoggingConfigurator;
import com.exactpro.sf.services.IServiceHandler;
import com.exactpro.sf.services.IServiceMonitor;
import com.exactpro.sf.services.IServiceSettings;
import com.exactpro.sf.services.ITaskExecutor;
import com.exactpro.sf.services.fix.converter.QFJIMessageConverterSettings;
import com.exactpro.sf.services.fix.converter.dirty.DirtyQFJIMessageConverter;
import com.exactpro.sf.services.fix.handler.ClientSideIoHandler;
import com.exactpro.sf.storage.IMessageStorage;

public class FixProxy extends TCPIPProxy {
    private final Logger logger = LoggerFactory.getLogger(getClass().getName() + "@" + Integer.toHexString(hashCode()));

    private DirtyQFJIMessageConverter converter;

    @Override
    protected IProxyIoHandler getProxyIoHandler() {
        return new ClientSideIoHandler(connector.getNioSocketConnector(),
                new InetSocketAddress(settings.getHost(), settings.getPort()), storage, handler, this, logConfigurator,
                        dictionary, factory, converter, getServiceName(), settings.getSendMessageTimeout());
    }

    public DirtyQFJIMessageConverter getConverter() {
        return converter;
    }

    @Override
    protected void internalInit(ServiceName serviceName, IDictionaryManager dictionaryManager, IServiceHandler handler, IServiceSettings settings,
            IMessageStorage storage, IServiceMonitor serviceMonitor, ILoggingConfigurator logConfigurator, ITaskExecutor taskExecutor,
            IDataManager dataManager) {

        QFJIMessageConverterSettings qfjiMessageConverterSettings = new QFJIMessageConverterSettings(dictionary, factory);
        converter = new DirtyQFJIMessageConverter(qfjiMessageConverterSettings);
    }
}
