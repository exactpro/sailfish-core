/*******************************************************************************
 * Copyright 2009-2019 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.services.fix;

import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.services.MessageHelper;

import quickfix.SessionID;

public class FIXLatencyCalculator {
    private final Map<SessionID, Long> latencies = new ConcurrentHashMap<>();
    private final MessageHelper helper;

    public FIXLatencyCalculator(MessageHelper helper) {
        this.helper = requireNonNull(helper, "helper cannot be null");
    }

    public void updateLatency(SessionID sessionID, IMessage message) {
        requireNonNull(sessionID, "sessionID cannot be null");
        requireNonNull(message, "message cannot be null");

        long senderTime = helper.getSenderTime(message);

        if(senderTime != 0) {
            latencies.put(sessionID, System.currentTimeMillis() - senderTime);
        }
    }

    public void removeLatency(SessionID sessionID) {
        latencies.remove(requireNonNull(sessionID, "sessionID cannot be null"));
    }

    public long getLatency(SessionID sessionID) {
        return latencies.getOrDefault(requireNonNull(sessionID, "sessionID cannot be null"), 0L);
    }
}
