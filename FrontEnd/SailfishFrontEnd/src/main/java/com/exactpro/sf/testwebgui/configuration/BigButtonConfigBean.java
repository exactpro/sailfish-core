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

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.bigbutton.BigButtonSettings;
import com.exactpro.sf.testwebgui.BeanUtil;
import com.exactpro.sf.testwebgui.api.TestToolsAPI;

@ManagedBean(name = "bbConfig")
@ViewScoped
public class BigButtonConfigBean {
    private static final Logger logger = LoggerFactory.getLogger(BigButtonConfigBean.class);

    private BigButtonSettings settings;

    @PostConstruct
    public void init() {
        this.settings = BeanUtil.getSfContext().getRegressionRunner().getSettings();
    }

    public void applySettings() {
        try {
            logger.info("Apply bb settings [{}]", this.settings);
            TestToolsAPI.getInstance().setRegressionRunnerSettings(settings);
            BeanUtil.addInfoMessage("Options applied", "");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            BeanUtil.addErrorMessage(e.getMessage(), e.getCause() != null ? e.getCause().getMessage() : "");
        } finally {
            this.settings = BeanUtil.getSfContext().getRegressionRunner().getSettings();
        }
    }

    public void setSettings(BigButtonSettings settings) {
        this.settings = settings;
    }

    public BigButtonSettings getSettings() {
        return settings;
    }

}
