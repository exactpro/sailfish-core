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

package com.exactpro.sf.testwebgui.configuration;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.exactpro.sf.center.ISFContext;
import com.exactpro.sf.center.IVersion;
import com.exactpro.sf.embedded.configuration.ServiceStatus;
import com.exactpro.sf.embedded.updater.UpdateService;
import com.exactpro.sf.embedded.updater.configuration.UpdateServiceSettings;
import com.exactpro.sf.testwebgui.BeanUtil;
import com.exactpro.sf.testwebgui.api.TestToolsAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ManagedBean(name = "updateBean")
@ViewScoped
public class UpdaterConfigBean implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(UpdaterConfigBean.class);

    private final String[] availableTimeUnits = new String[] {
            TimeUnit.MINUTES.name(), TimeUnit.HOURS.name(), TimeUnit.DAYS.name()
    };

    private UpdateServiceSettings settings;
    private List<ComponentInfo> currentVersions;

    @PostConstruct
    public void init() {
        ISFContext context = BeanUtil.getSfContext();
        this.settings = context.getUpdateService().getSettings().clone();
        this.currentVersions = context.getPluginVersions().stream()
                .map(ComponentInfo::new)
                .collect(Collectors.toList());
    }

    public void checkUpdate() {
        try {
            UpdateService updateService = BeanUtil.getSfContext().getUpdateService();
            updateService.checkUpdates();

            this.currentVersions.forEach(componentInfo -> componentInfo.setNewVersion(null));

            BeanUtil.addInfoMessage("Updates checked", updateService.isUpdateRequire() ? "Needs update" : "No updates");
        } catch (Exception e) {
            logger.error("Can't check for updates", e);
            BeanUtil.addErrorMessage("ERROR", "Can't check update: " + e.getMessage());
        }
    }

    public String getLastCheckTime() {
        LocalDateTime lastCheckTime = BeanUtil.getSfContext().getUpdateService().getLastCheckTime();
        return lastCheckTime == null ? "" : lastCheckTime.toString();
    }

    public String getUpdateErrorMessage() {
        return BeanUtil.getSfContext().getUpdateService().getUpdateErrorMsg();
    }

    public void restart() {
        try {
            logger.info("Restarting update service...");
            updateSettingsAndRestart();
            logger.info("Update service has been restarted");
            BeanUtil.addInfoMessage("Update service has been restarted", "");
        } catch (Exception e) {
            logger.error("Can't restart update service", e);
            BeanUtil.addErrorMessage("ERROR", "Can't restart update service: " + e.getMessage());
        }
    }

    public void update() {
        try {
            logger.info("Start Sailfish update");
            BeanUtil.getSfContext().getUpdateService().update();
        } catch (Exception e) {
            logger.error("Can't start update", e);
            BeanUtil.addErrorMessage("ERROR", "Can't start update: " + e.getMessage());
        }
    }

    public List<ComponentInfo> getComponentVersions() {
        fillNewVersions();
        return currentVersions;
    }

    public boolean isCriticalError() {
        return BeanUtil.getSfContext().getUpdateService().hasCriticalError();
    }

    private void updateSettingsAndRestart() throws Exception {
        try {
            TestToolsAPI.getInstance().setUpdateServiceSettings(settings);
        } finally {
            settings = BeanUtil.getSfContext().getUpdateService().getSettings().clone();
        }
    }

    public boolean isUpdating() {
        return BeanUtil.getSfContext().getUpdateService().isUpdating();
    }

    public void applySettings() {
        if (isChangesMade()) {
            try {
                logger.info("Applying settings...");
                updateSettingsAndRestart();
                BeanUtil.addInfoMessage("Settings applied", "");
                logger.info("Settings for Update service applied");
            } catch (Exception e) {
                logger.error("Can't apply settings", e);
                BeanUtil.addErrorMessage("ERROR", "Can't apply settings: " + e.getMessage());
            }
        } else {
            BeanUtil.addInfoMessage("No change to apply", "");
        }
    }

    private boolean isChangesMade() {
        UpdateServiceSettings currentSettings = BeanUtil.getSfContext().getUpdateService().getSettings();
        return !currentSettings.equals(settings);
    }

    private void fillNewVersions() {
        UpdateService updateService = BeanUtil.getSfContext().getUpdateService();
        if (updateService.isConnected() && updateService.isUpdateRequire()) {
            updateService.getComponentUpdateInfos()
                    .forEach(componentUpdateInfo -> currentVersions.stream()
                            .filter(componentInfo -> componentInfo.getName().equals(componentUpdateInfo.getName()))
                            .findFirst()
                            .ifPresent(componentInfo -> componentInfo.setNewVersion(componentUpdateInfo.getVersion())));
        }
    }

    public ServiceStatus getServiceStatus() {
        return BeanUtil.getSfContext().getUpdateService().getStatus();
    }

    public boolean isNeedsUpdate() {
        return BeanUtil.getSfContext().getUpdateService().isUpdateRequire();
    }

    public String getErrorMessage() {
        return BeanUtil.getSfContext().getUpdateService().getErrorMsg();
    }

    public String[] getAvailableTimeUnits() {
        return availableTimeUnits;
    }

    public UpdateServiceSettings getSettings() {
        return settings;
    }

    public void setSettings(UpdateServiceSettings settings) {
        this.settings = settings;
    }

    public class ComponentInfo {

        private String name;

        private String currentVersion;

        private String newVersion;

        public ComponentInfo(IVersion version) {
            this.name = version.getArtifactName();
            this.currentVersion = version.buildVersion();
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getCurrentVersion() {
            return currentVersion;
        }

        public void setCurrentVersion(String currentVersion) {
            this.currentVersion = currentVersion;
        }

        public String getNewVersion() {
            return newVersion;
        }

        public void setNewVersion(String newVersion) {
            this.newVersion = newVersion;
        }
    }
}
