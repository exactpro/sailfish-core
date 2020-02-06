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

import com.exactpro.sf.center.ISFContext;
import com.exactpro.sf.configuration.workspace.ResourceCleaner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Date;
import java.util.EnumMap;
import java.util.Map;

public class CleanupService {

    private final Map<ResourceCleaner, Boolean> cleaners = new EnumMap<>(ResourceCleaner.class);
    private static final Logger logger = LoggerFactory.getLogger(CleanupService.class);
    private final CleanupConfiguration cleanupConfiguration;
    private int cleanOlderThanDays;
    private boolean autoclean;

    public CleanupService(CleanupConfiguration cleanupConfiguration) {
        this.cleanupConfiguration = cleanupConfiguration;
        setCleanReports(cleanupConfiguration.isReports());
        setCleanEvents(cleanupConfiguration.isEvents());
        setCleanLogs(cleanupConfiguration.isLogs());
        setCleanMatrices(cleanupConfiguration.isMatrices());
        setCleanML(cleanupConfiguration.isMl());
        setCleanTrafficDump(cleanupConfiguration.isTrafficDump());
        setCleanMessages(cleanupConfiguration.isMessages());
        cleanOlderThanDays = cleanupConfiguration.getCleanOlderThanDays();
        autoclean = cleanupConfiguration.isAutoclean();
    }

    /**
     *
     * @param callBack interface for specific action with a successful and an failed cleanup cleanup
     */
    public void clean(Instant olderThan, ISFContext context, CleanupServiceCallback callBack) {
        cleaners.forEach((cleaner, enabled) -> {
            if(enabled) {
                try {
                    cleaner.clean(olderThan, context);
                    callBack.success(cleaner);
                } catch(Exception e) {
                    logger.error("Failed to cleanup {}", cleaner.getName(), e);
                    callBack.error(cleaner, e);
                }
            }
        });
    }

    public void applySettings() {
        cleanupConfiguration.setEvents(isCleanEvents());
        cleanupConfiguration.setLogs(isCleanLogs());
        cleanupConfiguration.setMatrices(isCleanMatrices());
        cleanupConfiguration.setMessages(isCleanMessages());
        cleanupConfiguration.setMl(isCleanML());
        cleanupConfiguration.setReports(isCleanReports());
        cleanupConfiguration.setTrafficDump(isCleanTrafficDump());
        cleanupConfiguration.setCleanOlderThanDays(cleanOlderThanDays);
        cleanupConfiguration.setAutoclean(autoclean);
    }


    public boolean isCleanReports() {
        return cleaners.getOrDefault(ResourceCleaner.REPORTS, false);
    }

    public void setCleanReports(boolean cleanReports) {
        cleaners.put(ResourceCleaner.REPORTS, cleanReports);
    }

    public boolean isCleanML() {
        return cleaners.getOrDefault(ResourceCleaner.ML, false);
    }

    public void setCleanML(boolean cleanReports) {
        cleaners.put(ResourceCleaner.ML, cleanReports);
    }

    public boolean isCleanMatrices() {
        return cleaners.getOrDefault(ResourceCleaner.MATRICES, false);
    }

    public void setCleanMatrices(boolean cleanMatrices) {
        cleaners.put(ResourceCleaner.MATRICES, cleanMatrices);
    }

    public boolean isCleanMessages() {
        return cleaners.getOrDefault(ResourceCleaner.MESSAGES, false);
    }

    public void setCleanMessages(boolean cleanMessages) {
        cleaners.put(ResourceCleaner.MESSAGES, cleanMessages);
    }

    public boolean isCleanEvents() {
        return cleaners.getOrDefault(ResourceCleaner.EVENTS, false);
    }

    public void setCleanEvents(boolean cleanEvents) {
        cleaners.put(ResourceCleaner.EVENTS, cleanEvents);
    }

    public boolean isCleanTrafficDump() {
        return cleaners.getOrDefault(ResourceCleaner.TRAFFIC_DUMP, false);
    }

    public void setCleanTrafficDump(boolean cleanTrafficDump) {
        cleaners.put(ResourceCleaner.TRAFFIC_DUMP, cleanTrafficDump);
    }

    public boolean isCleanLogs() {
        return cleaners.getOrDefault(ResourceCleaner.LOGS, false);
    }

    public void setCleanLogs(boolean cleanLogs) {
        cleaners.put(ResourceCleaner.LOGS, cleanLogs);
    }

    public void setAutoclean(boolean autoclean) {
        this.autoclean = autoclean;
    }

    public boolean isAutoclean() {
        return autoclean;
    }

    public void setCleanOlderThanDays(int cleanOlderThanDays) {
        this.cleanOlderThanDays = cleanOlderThanDays;
    }

    public int getCleanOlderThanDays() {
        return cleanOlderThanDays;
    }
}
