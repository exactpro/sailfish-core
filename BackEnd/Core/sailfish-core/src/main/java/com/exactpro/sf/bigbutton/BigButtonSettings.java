/*******************************************************************************
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

package com.exactpro.sf.bigbutton;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

import com.exactpro.sf.scriptrunner.StatusType;
import com.exactpro.sf.storage.IMapableSettings;
import com.exactpro.sf.util.EMailUtil;

public class BigButtonSettings implements IMapableSettings, Serializable {

    private static final long serialVersionUID = -5129527848807446635L;

    private static final String STORAGE_PREFIX = "bbSettings.db.";

    private static final String SETTINGS_NAME = "bbSettings";
    /**
    * Use {@link #STORAGE_PREFIX} prefix only
    */
    @Deprecated
    private static final String STORAGE_EMAIL_PREFIX = STORAGE_PREFIX + "email.";

    private String emailPrefix = "";
    private String emailPostfix = "";
    private String emailSubject = "BB Notification";
    private String emailRecipients = "";
    private String emailPassedRecipients = "";
    private String emailCondPassedRecipients = "";
    private String emailFailedRecipients = "";
    private boolean showPreCeanDialog = true;

    public BigButtonSettings() {
    }

    public BigButtonSettings(BigButtonSettings settings) {
        if (settings == null) {
            return;
        }

        this.emailPrefix = settings.emailPrefix;
        this.emailPostfix = settings.emailPostfix;
        this.emailSubject = settings.emailSubject;
        this.emailRecipients = settings.emailRecipients;
        this.emailPassedRecipients = settings.emailPassedRecipients;
        this.emailFailedRecipients = settings.emailFailedRecipients;
        this.showPreCeanDialog = settings.showPreCeanDialog;
    }

    public boolean isShowPreCeanDialog() {
        return showPreCeanDialog;
    }

    public void setShowPreCeanDialog(boolean showPreCeanDialog) {
        this.showPreCeanDialog = showPreCeanDialog;
    }

    public String getEmailPrefix() {
        return emailPrefix;
    }

    public void setEmailPrefix(String emailPrefix) {
        this.emailPrefix = StringEscapeUtils.escapeHtml4(emailPrefix);
    }

    public String getEmailPostfix() {

        return emailPostfix;
    }

    public void setEmailPostfix(String emailPostfix) {
        this.emailPostfix = StringEscapeUtils.escapeHtml4(emailPostfix);
    }

    public String getEmailSubject() {
        return emailSubject;
    }

    public void setEmailSubject(String emailSubject) {
        this.emailSubject = StringUtils.trimToEmpty(emailSubject);
    }

    public String getEmailRecipients() {
        return emailRecipients;
    }

    public void setEmailRecipients(String emailRecipients) {
        this.emailRecipients = StringUtils.trimToEmpty(emailRecipients);
    }

    public String getEmailPassedRecipients() {
        return emailPassedRecipients;
    }

    public void setEmailPassedRecipients(String emailPassedRecipients) {
        this.emailPassedRecipients = StringUtils.trimToEmpty(emailPassedRecipients);
    }

    public String getEmailFailedRecipients() {
        return emailFailedRecipients;
    }

    public void setEmailFailedRecipients(String emailFailedRecipients) {
        this.emailFailedRecipients = StringUtils.trimToEmpty(emailFailedRecipients);
    }

    @SuppressWarnings("incomplete-switch")
    public List<String> getFinalRecipients(StatusType status) {
        StringBuilder emailRecipients = new StringBuilder(this.emailRecipients);

        switch (status) {
        case FAILED:
            if (StringUtils.isNotEmpty(emailRecipients)) {
                emailRecipients.append(EMailUtil.DELIMITER);
            }
            emailRecipients.append(this.emailFailedRecipients);
            break;
        case CONDITIONALLY_PASSED:
            if (StringUtils.isNotEmpty(emailRecipients)) {
                emailRecipients.append(EMailUtil.DELIMITER);
            }
            emailRecipients.append(this.emailCondPassedRecipients);
            break;
        case PASSED:
            if (StringUtils.isNotEmpty(emailRecipients)) {
                emailRecipients.append(EMailUtil.DELIMITER);
            }
            emailRecipients.append(this.emailPassedRecipients);
            break;
        }

        return EMailUtil.parseRecipients(emailRecipients.toString());
    }

    @Override
    public String settingsName() {
        return SETTINGS_NAME;
    }

    @Override
    public void fillFromMap(Map<String, String> options) throws Exception {
        for (Map.Entry<String, String> entry : options.entrySet()) {
            if (entry.getKey().startsWith(STORAGE_EMAIL_PREFIX)) {
                BeanUtils.setProperty(this, entry.getKey().replace(STORAGE_EMAIL_PREFIX, ""), entry.getValue());
            } else if (entry.getKey().startsWith(STORAGE_PREFIX)) {
                BeanUtils.setProperty(this, entry.getKey().replace(STORAGE_PREFIX, ""), entry.getValue());
            }
        }
    }

    @Override
    public Map<String, String> toMap() throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, String> description = BeanUtils.describe(this);

        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, String> entry : description.entrySet()) {
            result.put(STORAGE_PREFIX + entry.getKey(), entry.getValue());
        }
        return result;
    }

    public String getEmailCondPassedRecipients() {
        return emailCondPassedRecipients;
    }

    public void setEmailCondPassedRecipients(String emailCondPassedRecipients) {
        this.emailCondPassedRecipients = emailCondPassedRecipients;
    }
}
