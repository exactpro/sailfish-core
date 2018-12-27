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
package com.exactpro.sf.testwebgui.configuration;

import java.io.Serializable;
import java.util.Date;
import java.util.EnumMap;
import java.util.Map;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Instant;

import com.exactpro.sf.center.ISFContext;
import com.exactpro.sf.testwebgui.BeanUtil;

@ViewScoped
@ManagedBean(name = "cleanupBean")
public class CleanupBean implements Serializable {
    private static final long serialVersionUID = -3953209638699015447L;
    private static final Logger logger = LoggerFactory.getLogger(CleanupBean.class);

    private Date olderThan = new Date();
    private Map<ResourceCleaner, Boolean> cleaners = new EnumMap<>(ResourceCleaner.class);

    public void clean() {
        ISFContext context = BeanUtil.getSfContext();
        Instant instant = Instant.ofEpochMilli(olderThan.getTime());

        cleaners.forEach((cleaner, enabled) -> {
            if(enabled) {
                try {
                    cleaner.clean(instant, context);
                    BeanUtil.addInfoMessage("Successfully cleaned up " + cleaner.getName(), StringUtils.EMPTY);
                } catch(Exception e) {
                    logger.error("Failed to cleanup {}", cleaner.getName(), e);
                    BeanUtil.addErrorMessage("Failed to cleanup " + cleaner.getName(), e.getMessage());
                }
            }
        });

        BeanUtil.addInfoMessage("Cleanup completed", StringUtils.EMPTY);
    }

    public Date getOlderThan() {
        return olderThan;
    }

    public void setOlderThan(Date olderThan) {
        this.olderThan = olderThan;
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
}
