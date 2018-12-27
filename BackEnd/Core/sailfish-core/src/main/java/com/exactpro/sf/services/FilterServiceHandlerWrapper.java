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

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.messages.IMessage;

public class FilterServiceHandlerWrapper extends ServiceHandlerWrapper {
    
    private static final Logger logger = LoggerFactory.getLogger(FilterServiceHandlerWrapper.class);

    private final Set<String> processedMessageTypes;
    
    public FilterServiceHandlerWrapper(IServiceHandler serviceHandler, Set<String> processedMessageTypes) {
        super(serviceHandler);
        if (processedMessageTypes == null || processedMessageTypes.isEmpty()) {
            throw new IllegalArgumentException("Processed message types can't be empty");
        }
        this.processedMessageTypes = processedMessageTypes;
    }

    @Override
    public void putMessage(ISession session, ServiceHandlerRoute route, IMessage message) throws ServiceHandlerException {
        if (this.processedMessageTypes.contains(message.getName())) {
            super.putMessage(session, route, message);
        } else {
            logger.trace("Message {} skipped by service settings. Route {}, Session {}", message.getName(), route, session);
        }
    }
}
