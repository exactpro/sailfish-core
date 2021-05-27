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

import com.exactpro.sf.aml.Description;
import com.exactpro.sf.services.mina.AbstractMINATCPServerSettings;

public class SOUPTcpServerSettings extends AbstractMINATCPServerSettings {
    private static final long serialVersionUID = 3977996641147102669L;

    @Description("Timeout for receive heartbeat message")
    private int readHeartBeatTimeout;

    @Description("Timeout for send heartbeat message")
    private int sendHeartBeatTimeout;

    @Description("Timeout for waiting login")
    private int waitLoginTimeout;

    @Description("Send heartbeats or not")
    private boolean sendHeartBeats;

    public boolean isSendHeartBeats() {
        return sendHeartBeats;
    }

    public void setSendHeartBeats(boolean sendHeartBeats) {
        this.sendHeartBeats = sendHeartBeats;
    }

    public int getWaitLoginTimeout() {
        return waitLoginTimeout;
    }

    public void setWaitLoginTimeout(int waitLoginTimeout) {
        this.waitLoginTimeout = waitLoginTimeout;
    }

    public int getSendHeartBeatTimeout() {
        return sendHeartBeatTimeout;
    }

    public void setSendHeartBeatTimeout(int sendHeartBeatTimeout) {
        this.sendHeartBeatTimeout = sendHeartBeatTimeout;
    }

    public int getReadHeartBeatTimeout() {
        return readHeartBeatTimeout;
    }

    public void setReadHeartBeatTimeout(int readHeartBeatTimeout) {
        this.readHeartBeatTimeout = readHeartBeatTimeout;
    }
}
