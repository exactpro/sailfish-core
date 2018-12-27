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

package com.exactpro.sf.services.fast;

import com.exactpro.sf.services.fast.blockstream.IPacketHandler;
import com.exactpro.sf.services.fast.blockstream.MulticastProxyConnection;
import org.openfast.Context;
import org.openfast.MessageBlockReader;
import org.openfast.session.Connection;
import org.openfast.session.FastConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FASTMulticastProxyClient extends FASTTcpClient {
    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName() + "@" + Integer.toHexString(hashCode()));
    private MulticastProxyConnection connection;


    @Override
    protected Connection getConnection(String remoteAddr, int port,
            String interfaceAddress) throws FastConnectionException {

        this.connection = new MulticastProxyConnection(remoteAddr, port,  new IPacketHandler() {
            @Override
            public void handlePacket(byte[] packetData) {
                if (getSettings().isResetContextAfterEachUdpPacket()) {
                    getReceiveContext().reset();
                    if(logger.isDebugEnabled()){
                        logger.debug("The context was cleared");
                    }
                }
            }
        });
        return connection;
    }

    @Override
    protected MessageBlockReader getBlockReader() {
        if (getSettings().isStreamBlockEncoded()) {
            return connection.getBlockReader();
        }
        return MessageBlockReader.NULL;
    }

    private Context getReceiveContext() {
        return msgInStream.getContext();
    }
}
