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
import java.time.Instant;
import java.util.Date;
import java.util.EnumMap;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.exactpro.sf.configuration.CleanupService;
import com.exactpro.sf.configuration.CleanupServiceCallback;
import com.exactpro.sf.configuration.workspace.ResourceCleaner;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.exactpro.sf.testwebgui.BeanUtil;

@ManagedBean(name = "cleanupBean")
@ViewScoped
public class CleanupBean implements Serializable {
    private static final long serialVersionUID = -3953209638699015447L;
    private static final Logger logger = LoggerFactory.getLogger(CleanupBean.class);

    private Date olderThan = new Date();
    private final Map<ResourceCleaner, Boolean> cleaners = new EnumMap<>(ResourceCleaner.class);

    private CleanupService cleanupService;

    public void clean() {
        Instant instant = Instant.ofEpochMilli(olderThan.getTime());
        cleanupService.clean(instant, BeanUtil.getSfContext(), new CleanupCallback());
        BeanUtil.addInfoMessage("Cleanup completed", StringUtils.EMPTY);
    }

    public CleanupBean(){
        cleanupService = BeanUtil.getSfContext().getCleanupService();
    }

    public void applySettings() {
        BeanUtil.getSfContext().getCleanupService().applySettings();
        BeanUtil.showMessage(FacesMessage.SEVERITY_INFO, "INFO", "Configuration changes successfully applied");
    }

    public Date getOlderThan() {
        return olderThan;
    }

    public void setOlderThan(Date olderThan) {
        this.olderThan = olderThan;
    }

    public boolean isCleanReports() {
        return cleanupService.isCleanReports();
    }

    public void setCleanReports(boolean cleanReports) {
        cleanupService.setCleanReports(cleanReports);
    }

    public boolean isCleanML() {
        return cleanupService.isCleanML();
    }

    public void setCleanML(boolean cleanML) {
        cleanupService.setCleanML(cleanML);
    }

    public boolean isCleanMatrices() {
        return cleanupService.isCleanMatrices();
    }

    public void setCleanMatrices(boolean cleanMatrices) {
        cleanupService.setCleanMatrices(cleanMatrices);
    }

    public boolean isCleanMessages() {
        return cleanupService.isCleanMessages();
    }

    public void setCleanMessages(boolean cleanMessages) {
        cleanupService.setCleanMessages(cleanMessages);
    }

    public boolean isCleanEvents() {
        return cleanupService.isCleanEvents();
    }

    public void setCleanEvents(boolean cleanEvents) {
        cleanupService.setCleanEvents(cleanEvents);
    }

    public boolean isCleanTrafficDump() {
        return cleanupService.isCleanTrafficDump();
    }

    public void setCleanTrafficDump(boolean cleanTrafficDump) {
        cleanupService.setCleanTrafficDump(cleanTrafficDump);
    }

    public boolean isCleanLogs() {
        return cleanupService.isCleanLogs();
    }

    public void setCleanLogs(boolean cleanLogs) {
        cleanupService.setCleanLogs(cleanLogs);
    }

    public void setAutoclean(boolean autoclean) {
        cleanupService.setAutoclean(autoclean);
    }

    public boolean isAutoclean() {
        return cleanupService.isAutoclean();
    }

    public void setCleanOlderThanDays(int cleanOlderThanDays) {
        cleanupService.setCleanOlderThanDays(cleanOlderThanDays);
    }

    public int getCleanOlderThanDays() {
        return cleanupService.getCleanOlderThanDays();
    }

    private static class CleanupCallback implements CleanupServiceCallback {

        @Override
        public void success(ResourceCleaner resource) {
            BeanUtil.addInfoMessage("Successfully cleaned up " + resource.getName(), StringUtils.EMPTY );
        }

        @Override
        public void error(ResourceCleaner resource, Exception e) {
            BeanUtil.addInfoMessage("Failed to cleanup " + resource.getName(), e.getMessage());
        }
    }
}
