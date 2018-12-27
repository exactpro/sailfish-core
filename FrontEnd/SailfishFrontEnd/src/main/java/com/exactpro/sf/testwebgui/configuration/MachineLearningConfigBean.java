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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.center.IVersion;
import com.exactpro.sf.embedded.configuration.ServiceStatus;
import com.exactpro.sf.embedded.machinelearning.MachineLearningService;
import com.exactpro.sf.testwebgui.BeanUtil;

@ManagedBean(name = "mlConfigBean")
@ViewScoped
@SuppressWarnings("serial")
public class MachineLearningConfigBean implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(MachineLearningConfigBean.class);

    public boolean isConnected() {

        return BeanUtil.getSfContext().getMachineLearningService().isConnected();

    }

    public ServiceStatus getCurrentStatus() {

        return BeanUtil.getSfContext().getMachineLearningService().getStatus();

    }

    public String getErrorText() {

        return BeanUtil.getSfContext().getMachineLearningService().getErrorMsg();

    }

    public ServiceStatus getPredictorStatus() {
        return BeanUtil.getSfContext().getMachineLearningService().getPredictorStatus();
    }

    public String getMLVersion() {
        IVersion version = BeanUtil.getSfContext().getMachineLearningService().getMLVersion();
        return version != null ? version.buildVersion() : "";
    }

    public boolean isServiceEnabled() {
        return BeanUtil.getSfContext().getMachineLearningService().isConnected();
    }

    public void getDump() throws IOException {

        FacesContext facesContext = FacesContext.getCurrentInstance();

        HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext().getResponse();
        response.reset();
        response.setHeader("Content-Type", "application/zip");
        response.setHeader("Content-Disposition", String.format("attachment; filename=\"%s\"",
                "dump" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".zip"));

        MachineLearningService mlService = BeanUtil.getSfContext().getMachineLearningService();

        try (InputStream is = mlService.getAllSubmitsAsZip(); OutputStream os = response.getOutputStream()) {
            IOUtils.copy(is, os);
        }

        facesContext.responseComplete();
    }

}