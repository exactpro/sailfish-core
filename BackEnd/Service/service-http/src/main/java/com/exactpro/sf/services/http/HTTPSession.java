/******************************************************************************
 * Copyright 2009-2021 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.services.http;

import java.util.concurrent.TimeUnit;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.services.ServiceException;
import com.exactpro.sf.services.netty.NettySession;

public class HTTPSession extends NettySession {
    
    private final HTTPClient client;

    public HTTPSession(HTTPClient client) {
        super(client);
        this.client = client;
    }

    @Override
    public IMessage send(Object message) throws InterruptedException {

        //os marks connection dead after this time
        //if not accured, possibly, response was not come
        boolean accured = client.channelBusy.tryAcquire(30, TimeUnit.SECONDS);
        if (!accured) {
            logger.warn("Cant lock session for send message. Possibly response not come. Force unlock");
            client.channelBusy.release();
            client.channelBusy.acquire();
        }

        if(!(message instanceof IMessage)) {
            throw new ServiceException("Message is not an instance of " + IMessage.class.getCanonicalName());
        }

        if(isClosed()) {
            try {
                client.connect();
            } catch(Exception e) {
                client.channelBusy.release();
                throw new ServiceException("Failed to connect service before sending a message", e);
            }
        }

        try {
            return super.send(message);
        } catch (Exception e) {
            client.channelBusy.release();
            throw e;
        }
    }

}
