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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class FASTAbstractTCPClient  extends FASTAbstractClient{
    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName() + "@" + Integer.toHexString(hashCode()));

    @Override
    public synchronized void connect(){
        logger.debug("Connecting FAST TCP Client");
        logger.debug("Initial disconnect...");
        disconnect();
        logger.debug("Initializing connection");
        initConnection();
        logger.debug("FAST TCP Client connected");
    }

    public synchronized void disconnect(){
        logger.debug("disconnecting FAST TCP");
        closeSession();
        logger.debug("FAST TCP disconnected");
    }
}
