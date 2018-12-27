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
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.flywaydb.core.api.MigrationInfo;
import org.primefaces.context.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.embedded.configuration.ServiceStatus;
import com.exactpro.sf.embedded.statistics.StatisticsService;
import com.exactpro.sf.embedded.statistics.configuration.DbmsType;
import com.exactpro.sf.embedded.statistics.configuration.StatisticsServiceSettings;
import com.exactpro.sf.testwebgui.BeanUtil;
import com.exactpro.sf.testwebgui.api.TestToolsAPI;

@ManagedBean(name = "statConfigBean")
@ViewScoped
@SuppressWarnings("serial")
public class StatisticsConfigBean implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(StatisticsConfigBean.class);

    private StatisticsServiceSettings settings;
    
    private List<String> supportedDbms = new ArrayList<>();

    {
        for (DbmsType dbmsType : DbmsType.values()) {
            supportedDbms.add(dbmsType.getValue());
        }
    }

    @PostConstruct
    public void init() {

        // Clone settings
        this.settings = new StatisticsServiceSettings(BeanUtil.getSfContext().getStatisticsService().getSettings());

    }

    public boolean isConnected() {

        return BeanUtil.getSfContext().getStatisticsService().isConnected();

    }

    public List<String> getSupportedDbms() {

        return this.supportedDbms;

    }

    public boolean isMigrationRequired() {

        return BeanUtil.getSfContext().getStatisticsService().isMigrationRequired();

    }

    public void invokeMigrate() {

        try {

            BeanUtil.getSfContext().getStatisticsService().migrateDB();

            applySettings();

        } catch (Exception e) {

            logger.error(e.getMessage(), e);

            BeanUtil.addErrorMessage(e.getMessage(), e.getCause() != null ? e.getCause().getMessage() : "");

        }

    }

    public ServiceStatus getCurrentStatus() {

        return BeanUtil.getSfContext().getStatisticsService().getStatus();

    }

    public String getDbVersion() {

        if (BeanUtil.getSfContext().getStatisticsService().isConnected()) {

            MigrationInfo versionInfo = BeanUtil.getSfContext().getStatisticsService().getCurrentDbVersionInfo();

            if (versionInfo != null) {

                return versionInfo.getVersion().getVersion();

            }

        }

        return "";

    }

    public String getErrorText() {

        return BeanUtil.getSfContext().getStatisticsService().getErrorMsg();

    }
    
    private boolean isChangesMade() {
    	StatisticsService service = BeanUtil.getSfContext().getStatisticsService();
        StatisticsServiceSettings currentSettings = service.getSettings();
        
        return !currentSettings.equals(this.settings) || service.getStatus().equals(ServiceStatus.Error) 
        		|| service.getStatus().equals(ServiceStatus.Checking);
    }
    
    public void preApplySettings() {
    	
        if (isChangesMade()) {
        	
        	BeanUtil.getSfContext().getStatisticsService().preCheckConnection();
        	
        	RequestContext.getCurrentInstance().update("statConnectionStatusForm");
    		RequestContext.getCurrentInstance().update("statisticsDbForm");
        	
        } else {
        	
        	BeanUtil.addInfoMessage("No changes made", "");
        }
    }

    public void applySettings() {

    	if (isChangesMade()) {

            // Apply new settings

            try {
            	
                TestToolsAPI.getInstance().setStatisticsDBSettings(settings);
                
                BeanUtil.addInfoMessage("Applied", "");

            } catch (Exception e) {

                logger.error(e.getMessage(), e);

                BeanUtil.addErrorMessage(e.getMessage(), e.getCause() != null ? e.getCause().getMessage() : "");

            } finally {

                this.settings = new StatisticsServiceSettings(BeanUtil.getSfContext().getStatisticsService().getSettings());
                
                RequestContext.getCurrentInstance().update("statConnectionStatusForm");
        		RequestContext.getCurrentInstance().update("statisticsDbForm");
            }
        }
    }

    public StatisticsServiceSettings getSettings() {
        return settings;
    }

    public void setSettings(StatisticsServiceSettings settings) {
        this.settings = settings;
    }
    
    public boolean isServiceEnabled() {
        return settings.isServiceEnabled();
    }
    
    public void setServiceEnabled(boolean serviceEnabled) {
        settings.setServiceEnabled(serviceEnabled);
    }
}