package com.exactpro.sf.embedded.mail.configuration;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.beanutils.BeanUtils;

import com.exactpro.sf.storage.IMapableSettings;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;

public class EMailServiceSettings implements IMapableSettings, Serializable {

    private static final long serialVersionUID = -2048992849401701406L;

    private static final String STORAGE_PREFIX = "emailSettings.db.";
    private static final String ENABLED_KEY = STORAGE_PREFIX + "serviceEnabled";
    private static final String SETTINGS_NAME = "emailSettings";

    private String smtpHost = "smtp.example.com";
    private Integer smtpPort = 25;
    private boolean useSSL = false;
    private String username = "user@example.com";
    private String password = "password";
    private String recipients = "user1@example.com;user2@example.com";
    private Integer timeout = 5000;
    private boolean serviceEnabled = true;

    public EMailServiceSettings() {
    }

    public EMailServiceSettings(EMailServiceSettings origin) {
        if (origin != null) {
            this.smtpHost = origin.getSmtpHost();
            this.smtpPort = origin.getSmtpPort();
            this.username = origin.getUsername();
            this.password = origin.getPassword();
            this.timeout = origin.getTimeout();
            this.recipients = origin.getRecipients();
            this.useSSL = origin.isUseSSL();
            this.serviceEnabled = origin.isServiceEnabled();
        }
    }

    @Override
    public String settingsName() {
        return SETTINGS_NAME;
    }

    @Override
    public void fillFromMap(Map<String, String> options) throws Exception {
        for(Map.Entry<String, String> entry : options.entrySet()) {
            if(entry.getKey().startsWith(STORAGE_PREFIX)) {
                BeanUtils.setProperty(this, entry.getKey().replace(STORAGE_PREFIX, ""), entry.getValue());
            }
        }
        this.serviceEnabled = BooleanUtils.toBoolean(ObjectUtils.defaultIfNull(options.get(ENABLED_KEY), "true"));
    }

    @Override
    public Map<String, String> toMap() throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, String> description = BeanUtils.describe(this);

        Map<String, String> result = new HashMap<>();
        for(Map.Entry<String, String> entry : description.entrySet()) {
            result.put(STORAGE_PREFIX + entry.getKey(), entry.getValue());
        }
        result.put(ENABLED_KEY, String.valueOf(serviceEnabled));
        return result;
    }

    public String getSmtpHost() {
        return smtpHost;
    }

    public void setSmtpHost(String smtpHost) {
        this.smtpHost = smtpHost;
    }

    public Integer getSmtpPort() {
        return smtpPort;
    }

    public void setSmtpPort(Integer smtpPort) {
        this.smtpPort = smtpPort;
    }

    public boolean isUseSSL() {
        return useSSL;
    }

    public void setUseSSL(boolean useSSL) {
        this.useSSL = useSSL;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRecipients() {
        return recipients;
    }

    public void setRecipients(String recipients) {
        this.recipients = recipients;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public boolean isServiceEnabled() {
        return serviceEnabled;
    }

    public void setServiceEnabled(boolean serviceEnabled) {
        this.serviceEnabled = serviceEnabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EMailServiceSettings that = (EMailServiceSettings) o;
        return Objects.equals(smtpHost, that.smtpHost) &&
                Objects.equals(smtpPort, that.smtpPort) &&
                Objects.equals(useSSL, that.useSSL) &&
                Objects.equals(username, that.username) &&
                Objects.equals(password, that.password) &&
                Objects.equals(recipients, that.recipients) &&
                Objects.equals(timeout, that.timeout) &&
                Objects.equals(serviceEnabled, that.serviceEnabled);
    }

    @Override
    public int hashCode() {
        return Objects.hash(smtpHost, smtpPort, useSSL, username, password, recipients, timeout, serviceEnabled);
    }

    @Override
    public String toString() {
        return new StringBuilder().append("EMAilServiceSettings{")
                .append("smtpHost='").append(smtpHost).append('\'')
                .append(", smtpPort=").append(smtpPort)
                .append(", SSL=").append(useSSL)
                .append(", username='").append(username).append('\'')
                .append(", password='").append(password).append('\'')
                .append(", recipients=").append(recipients)
                .append(", timeout=").append(timeout)
                .append(", serviceEnabled=").append(serviceEnabled)
                .append('}').toString();
    }
}
