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

package com.exactpro.sf.services.websocket;

import com.exactpro.sf.aml.Description;
import com.exactpro.sf.services.http.HTTPClientSettings;

public class WebSocketClientSettings extends HTTPClientSettings {

    @Description("Automatic connection to server during service start")
    private boolean autoConnect = true;

    @Description("Max size of incoming WebSocket frame payload")
    private int maxWebSocketFrameSize = 1048576;

    public boolean isAutoConnect() {
        return autoConnect;
    }

    public void setAutoConnect(boolean autoConnect) {
        this.autoConnect = autoConnect;
    }

    public int getMaxWebSocketFrameSize() {
        return maxWebSocketFrameSize;
    }

    public void setMaxWebSocketFrameSize(int maxWebSocketFrameSize) {
        this.maxWebSocketFrameSize = maxWebSocketFrameSize;
    }
}
