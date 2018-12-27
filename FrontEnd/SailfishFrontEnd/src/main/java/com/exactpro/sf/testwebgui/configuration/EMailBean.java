package com.exactpro.sf.testwebgui.configuration;

import java.util.Objects;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.primefaces.context.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.embedded.configuration.ServiceStatus;
import com.exactpro.sf.embedded.mail.EMailService;
import com.exactpro.sf.embedded.mail.configuration.EMailServiceSettings;
import com.exactpro.sf.testwebgui.BeanUtil;
import com.exactpro.sf.testwebgui.api.TestToolsAPI;

@ManagedBean(name="emailBean")
@ViewScoped
public class EMailBean {
    private static final Logger logger = LoggerFactory.getLogger(EMailBean.class);

    private EMailServiceSettings settings;

    @PostConstruct
    public void init() {
        this.settings = new EMailServiceSettings(BeanUtil.getSfContext().getEMailService().getSettings());
    }

    public void applySettings() {
        if (isChangesMade()) {
            try {
                logger.info("Apply mail settings [{}]", this.settings);
                TestToolsAPI.getInstance().setEMailServiceSettings(settings);
                BeanUtil.addInfoMessage("Options applied", "");
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                BeanUtil.addErrorMessage(e.getMessage(), e.getCause() != null ? e.getCause().getMessage() : "");
            } finally {
                this.settings = new EMailServiceSettings(BeanUtil.getSfContext().getEMailService().getSettings());
                RequestContext.getCurrentInstance().update("emailConnectionStatusForm");
                RequestContext.getCurrentInstance().update("emailForm");
            }
        }
    }


    public void preApplySettings() {
        if (isChangesMade()) {
            BeanUtil.getSfContext().getEMailService().preCheckConnection();
            RequestContext.getCurrentInstance().update("emailConnectionStatusForm");
            RequestContext.getCurrentInstance().update("emailForm");
        } else {
            BeanUtil.addInfoMessage("No changes made", "");
        }
    }
    private boolean isChangesMade() {
        EMailService service = BeanUtil.getSfContext().getEMailService();
        EMailServiceSettings currentSettings = service.getSettings();

        return !Objects.equals(currentSettings, this.settings) || service.getStatus().equals(ServiceStatus.Error)
                || service.getStatus().equals(ServiceStatus.Checking);
    }

    public EMailServiceSettings getSettings() {
        return settings;
    }

    public void setSettings(EMailServiceSettings settings) {
        this.settings = settings;
    }

    public ServiceStatus getStatus() {
        return BeanUtil.getSfContext().getEMailService().getStatus();
    }

    public String getErrorText() {
        return BeanUtil.getSfContext().getEMailService().getErrorMsg();
    }
}
