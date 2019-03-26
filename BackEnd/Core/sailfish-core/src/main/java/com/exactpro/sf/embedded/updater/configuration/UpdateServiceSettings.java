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

package com.exactpro.sf.embedded.updater.configuration;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;

import com.exactpro.sf.storage.IMapableSettings;

public class UpdateServiceSettings implements IMapableSettings, Serializable {

    private static final String SETTINGS_NAME = "updateService";
    private static final String STORAGE_PREFIX = SETTINGS_NAME + ".";

    private String host;
    private int port;
    private long checkUpdateTimeout = 2;
    private String timeUnit = TimeUnit.HOURS.name();

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public long getCheckUpdateTimeout() {
        return checkUpdateTimeout;
    }

    public void setCheckUpdateTimeout(long checkUpdateTimeout) {
        this.checkUpdateTimeout = checkUpdateTimeout;
    }

    public String getTimeUnit() {
        return timeUnit;
    }

    public void setTimeUnit(String timeUnit) {
        this.timeUnit = timeUnit;
    }

    @Override
    public String settingsName() {
        return SETTINGS_NAME;
    }

    @Override
    public void fillFromMap(Map<String, String> options) throws Exception {
        for(Map.Entry<String, String> entry : options.entrySet()) {
            if(entry.getKey().startsWith(STORAGE_PREFIX)) {
                BeanUtils.setProperty(this, entry.getKey().replace(STORAGE_PREFIX, ""), entry.getValue());
            }
        }
    }

    @Override
    public Map<String, String> toMap() throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, String> description = BeanUtils.describe(this);

        Map<String, String> result = new HashMap<>();
        for(Map.Entry<String, String> entry : description.entrySet()) {
            result.put(STORAGE_PREFIX + entry.getKey(), entry.getValue());
        }
        return result;
    }

    public UpdateServiceSettings clone() {
        UpdateServiceSettings clone = new UpdateServiceSettings();
        clone.host = this.host;
        clone.port = this.port;
        clone.checkUpdateTimeout = this.checkUpdateTimeout;
        clone.timeUnit = this.timeUnit;
        return clone;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UpdateServiceSettings that = (UpdateServiceSettings) o;
        return new EqualsBuilder()
                .append(host, that.host)
                .append(port, that.port)
                .append(checkUpdateTimeout, that.checkUpdateTimeout)
                .append(timeUnit, that.timeUnit)
                .isEquals();
    }
}
