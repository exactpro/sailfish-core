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
package com.exactpro.sf.configuration;

import org.apache.commons.configuration.HierarchicalConfiguration;

import com.exactpro.sf.common.util.ICommonSettings;

/**
 * Created by alexey.zarovny on 1/30/15.
 */
public class LoggingConfiguration implements ILoggingConfiguration, ICommonSettings {

    private final static String INDIVIDUAL_APPENDER_ENABLE_PROPERTY = "IndividualAppendersEnabled";
    private final static String INDIVIDUAL_APPENDER_THRESHOLD_PROPERTY = "IndividualAppendersThreshold";

    private boolean individualAppendersEnabled;
    private final HierarchicalConfiguration config;

    public LoggingConfiguration(HierarchicalConfiguration config) {
        this.config = config;
    }

    private void updateLogConfiguration() {
        config.setProperty(INDIVIDUAL_APPENDER_ENABLE_PROPERTY, individualAppendersEnabled);
    }

    public void setIndividualAppendersEnabled(boolean individualAppendersEnabled) {
        this.individualAppendersEnabled = individualAppendersEnabled;
        updateLogConfiguration();
    }

    @Override
    public String getIndividualAppendersThereshold() {
        return config.getString(INDIVIDUAL_APPENDER_THRESHOLD_PROPERTY, "ALL");
    }

    @Override
    public void setIndividualAppendersThreshold(String threshold) {
        config.setProperty(INDIVIDUAL_APPENDER_THRESHOLD_PROPERTY, threshold);
    }

    @Override
    public boolean isIndividualAppendersEnabled() {
        return individualAppendersEnabled;
    }

    @Override
    public void load(HierarchicalConfiguration config) {
        individualAppendersEnabled = this.config.getBoolean(INDIVIDUAL_APPENDER_ENABLE_PROPERTY, true);
    }
}
