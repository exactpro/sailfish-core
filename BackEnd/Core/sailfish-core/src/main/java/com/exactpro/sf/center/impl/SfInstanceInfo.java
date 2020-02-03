/******************************************************************************
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
package com.exactpro.sf.center.impl;

import com.exactpro.sf.embedded.statistics.entities.SfInstance;

import java.util.UUID;

public class SfInstanceInfo {
    private final String hostname;
    private final int port;
    private final String contextPath;

    /**
     * Unique identifier of the sailfish instance.
     * It calculates upon initialization of SfContext instance.
     * @see com.exactpro.sf.bigbutton.execution.ExecutorClient
     */
    private final String uid;

    public SfInstanceInfo(String hostname, int port, String contextPath) {
        this(hostname, port, contextPath, null);
    }

    public SfInstanceInfo(String hostname, int port, String contextPath, String uid) {
        this.hostname = hostname;
        this.port = port;
        this.contextPath = contextPath;
        this.uid = uid;
    }

    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
    }

    public String getContextPath() {
        return contextPath;
    }

    public String getUID() {
        return uid;
    }

    public static SfInstanceInfo fromSfInstance(SfInstance instance) {
        return instance != null
                ? new SfInstanceInfo(instance.getHost(), instance.getPort(), instance.getName())
                : null;
    }
}
