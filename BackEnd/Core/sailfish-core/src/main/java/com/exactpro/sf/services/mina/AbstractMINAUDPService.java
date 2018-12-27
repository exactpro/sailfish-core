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

import com.exactpro.sf.connectivity.mina.net.MulticastSocketConnector;
import com.exactpro.sf.services.ServiceException;
import org.apache.commons.lang3.StringUtils;
import org.apache.mina.core.future.ConnectFuture;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;

public abstract class AbstractMINAUDPService extends AbstractMINAService {
    /**
     * All operations with connector should be executed under synchronization on current service
     */
    protected MulticastSocketConnector connector;
    protected NetworkInterface networkInterface;

    @Override
    protected void internalInit() throws Exception {
        networkInterface = getNetworkInterface(getNetworkInterface());
    }

    protected abstract String getNetworkInterface();

    private NetworkInterface getNetworkInterface(String name) {
        NetworkInterface result = null;

        if((name = StringUtils.trimToNull(name)) != null) {
            try {
                result = NetworkInterface.getByName(name);
            } catch(Exception e) {
                logger.error("Failed to get network interface by name", e);
            }

            if(result == null) {
                try {
                    result = NetworkInterface.getByInetAddress(InetAddress.getByName(name));
                } catch(Exception e) {
                    logger.error("Failed to get network interface by address", e);
                }
            }
        }

        if (result == null) {
            logger.error("Network interface is incorrect: {}", name);
        } else if (logger.isInfoEnabled()) {
            logger.info("Network interface '{}' has been defined '{}'", result.getDisplayName(), name);
        }

        return result;
    }

    @Override
    protected synchronized void initConnector() throws Exception {

        super.initConnector();

        connector = new MulticastSocketConnector();
        connector.setNetworkInterface(networkInterface);
        connector.getSessionConfig().setReadBufferSize(65534);
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
