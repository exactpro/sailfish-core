/******************************************************************************
 * Copyright 2009-2020 Exactpro (Exactpro Systems Limited)
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

import com.exactpro.sf.common.util.ICommonSettings;
import org.apache.commons.configuration.HierarchicalConfiguration;

public class CleanupConfiguration implements ICommonSettings {

    private final HierarchicalConfiguration config;

    private static final String REPORTS = "Reports";
    private static final String MATRICES = "Matrices";
    private static final String MESSAGES = "Messages";
    private static final String EVENTS = "Events";
    private static final String TRAFFIC_DUMP = "TrafficDump";
    private static final String LOGS = "Logs";
    private static final String ML = "ML";
    private static final String AUTOCLEAN = "Autoclean";
    private static final String CLEAN_OLDER_THAN_DAYS = "cleanOlderThanDays";

    private boolean reports;
    private boolean matrices;
    private boolean messages;
    private boolean events;
    private boolean trafficDump;
    private boolean logs;
    private boolean ml;
    private boolean autoclean;
    private int cleanOlderThanDays;

    public CleanupConfiguration(HierarchicalConfiguration config) {
        this.config = config;
    }

    @Override
    public void load(HierarchicalConfiguration config) {
        reports = config.getBoolean(REPORTS, false);
        matrices = config.getBoolean(MATRICES, false);
        messages = config.getBoolean(MESSAGES, false);
        events = config.getBoolean(EVENTS, false);
        trafficDump = config.getBoolean(TRAFFIC_DUMP, false);
        logs = config.getBoolean(LOGS, false);
        ml = config.getBoolean(ML, false);
        autoclean = config.getBoolean(AUTOCLEAN, false);
        cleanOlderThanDays = config.getInt(CLEAN_OLDER_THAN_DAYS, 1);
    }

    public boolean isReports() {
        return reports;
    }

    public void setReports(boolean reports) {
        this.reports = reports;
        config.setProperty(REPORTS, reports);
    }

    public boolean isMatrices() {
        return matrices;
    }

    public void setMatrices(boolean matrices) {
        this.matrices = matrices;
        config.setProperty(MATRICES, matrices);
    }

    public boolean isMessages() {
        return messages;
    }

    public void setMessages(boolean messages) {
        this.messages = messages;
        config.setProperty(MESSAGES, messages);
    }

    public boolean isEvents() {
        return events;
    }

    public void setEvents(boolean reportEvents) {
        this.events = reportEvents;
        config.setProperty(EVENTS, events);
    }

    public boolean isTrafficDump() {
        return trafficDump;
    }

    public void setTrafficDump(boolean trafficDump) {
        this.trafficDump = trafficDump;
        config.setProperty(TRAFFIC_DUMP, trafficDump);
    }

    public boolean isLogs() {
        return logs;
    }

    public void setLogs(boolean logs) {
        this.logs = logs;
        config.setProperty(LOGS, logs);
    }

    public boolean isMl() {
        return ml;
    }

    public void setMl(boolean ml) {
        this.ml = ml;
        config.setProperty(ML, ml);
    }

    public boolean isAutoclean() {
        return autoclean;
    }

    public void setAutoclean(boolean autoclean) {
        this.autoclean = autoclean;
        config.setProperty(AUTOCLEAN, autoclean);
    }

    public int getCleanOlderThanDays() {
        return cleanOlderThanDays;
    }

    public void setCleanOlderThanDays(int cleanOlderThanDays) {
        this.cleanOlderThanDays = cleanOlderThanDays;
        config.setProperty(CLEAN_OLDER_THAN_DAYS, cleanOlderThanDays);
    }
}
