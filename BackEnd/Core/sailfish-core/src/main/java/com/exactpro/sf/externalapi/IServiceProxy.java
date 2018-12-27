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
package com.exactpro.sf.externalapi;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.services.ServiceStatus;

public interface IServiceProxy {
    
    /**
     * Initializes service using copy of current settings state and start service.
     * @throws IllegalStateException if incorrect service state
     * 
     * @see ServiceStatus
     */
    void start();
    
    /**
     * Stops service.
     * @throws IllegalStateException if incorrect service state
     * 
     * @see ServiceStatus
     */
    void stop();
    
    /**
     * Sends IMessage to current session.
     * @param message
     * @return
     * @throws InterruptedException
     * @throws IllegalStateException if called when service can't send messages
     */
    IMessage send(IMessage message) throws InterruptedException;
    
    /**
     * @return service name
     */
    ServiceName getName();
    
    /**
     * @return current service status
     */
    ServiceStatus getStatus();
    
    SailfishURI getType();
    
    /**
     * Settings proxy which applied after {@link #start()} .
     * @return
     */
    ISettingsProxy getSettings();

}
