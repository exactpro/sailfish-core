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
package com.exactpro.sf.services.mina;

import com.exactpro.sf.services.ServiceException;
import com.exactpro.sf.services.WrapperNioSocketConnector;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.session.IdleStatus;

import java.net.InetSocketAddress;

public abstract class AbstractMINATCPService extends AbstractMINAService {
    /**
     * All operations with connector should be executed under synchronization on current service
     */
    protected WrapperNioSocketConnector connector;

    @Override
    protected synchronized void initConnector() throws Exception {
        super.initConnector();

        connector = new WrapperNioSocketConnector(taskExecutor);
        connector.getSessionConfig().setIdleTime(IdleStatus.READER_IDLE, getReaderIdleTimeout());
        connector.getSessionConfig().setIdleTime(IdleStatus.WRITER_IDLE, getWriterIdleTimeout());
        connector.setHandler(this);

        initFilterChain(connector.getFilterChain());
    }

    @Override
    protected synchronized ConnectFuture getConnectFuture() throws Exception {
        if(connector == null) {
            throw new ServiceException("Connector is not initialized");
        }

        return connector.connect(new InetSocketAddress(getHostname(), getPort()));
    }

    @Override
    protected synchronized void disposeConnector() {
        if(connector != null) {
            logger.info("Disposing connector");
            connector.dispose(); // wait for dispose?
            connector = null;
        }
    }
}
