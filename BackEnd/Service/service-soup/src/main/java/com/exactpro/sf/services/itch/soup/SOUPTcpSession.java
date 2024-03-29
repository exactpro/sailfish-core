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

package com.exactpro.sf.services.itch.soup;

import java.util.concurrent.atomic.AtomicReference;

import org.apache.mina.core.session.IoSession;

import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.services.mina.MINASession;

public class SOUPTcpSession extends MINASession {

    private final AtomicReference<String> sessionId;

    public SOUPTcpSession(ServiceName serviceName, IoSession session, long sendMessageTimeout) {
        super(serviceName, session, sendMessageTimeout);
        this.sessionId = new AtomicReference<>();
    }

    public String getSessionId() {
        return sessionId.get();
    }

    public void setSessionId(String sessionId) {
        this.sessionId.set(sessionId);
    }
}
