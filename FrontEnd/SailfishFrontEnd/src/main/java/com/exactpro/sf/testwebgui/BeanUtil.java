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
package com.exactpro.sf.testwebgui;

import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.application.FacesMessage.Severity;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;

import com.exactpro.sf.center.ISFContext;
import com.exactpro.sf.common.impl.messages.xml.configuration.JavaType;
import com.exactpro.sf.common.util.StringUtil;
import com.exactpro.sf.embedded.statistics.entities.SfInstance;
import com.exactpro.sf.embedded.statistics.storage.CommonReportRow;
import com.exactpro.sf.storage.auth.User;
import com.exactpro.sf.testwebgui.configuration.ConfigBean;
import com.exactpro.sf.testwebgui.context.ContextBean;
import com.exactpro.sf.testwebgui.context.PopupMessageHolder;
import com.exactpro.sf.testwebgui.environment.EnumSetContainer;
import com.exactpro.sf.testwebgui.help.HelpContentHolder;
import com.exactpro.sf.testwebgui.restapi.RESTUtil;
import com.exactpro.sf.testwebgui.scriptruns.MatrixHolder;
import com.exactpro.sf.testwebgui.scriptruns.ScriptRunsBean;
import com.exactpro.sf.testwebgui.servlets.ReportServlet;
import com.exactpro.sf.testwebgui.servlets.SessionModelsMapper;

public class BeanUtil {

    public static final String WORKSPACE_DISPATCHER = "workspaceDispatcher";
    public static final String KEY_USER = "user";
    public static final String DEFAULT_USER = "guest";
    public static final String ENUM_SET_CONTAINER = "enumSetContainer";
    public static final String MATRIX_HOLDER = "matrixHolder";
    public static final String MATRIX_PROVIDER_HOLDER = "matrixProviderHolder";
    public static final String HELP_CONTENT_HOLDER = "helpContentHolder";
    public static final String SESSION_MODELS_MAPPER = "sessionModelsMapper";
    public static final String ENVIRONMENT_TRACKING_BEAN = "environmentTrackingBean";

    private static ServletContext servletContext;

    public static void setServletContext(ServletContext context) {
        if (servletContext == null) {
            servletContext = context;
        }
    }

    private static final Long GROWL_LIFE = 6000L;

    public static void showMessage(Severity severity, String message, String details) {

        FacesMessage msg = new FacesMessage(severity, message, details);
        FacesContext context = FacesContext.getCurrentInstance();

        PopupMessageHolder holder = getFacesMesageHolder();
        holder.addMessage(msg);

        for(FacesMessage m : holder.getRecentMessages(GROWL_LIFE)) {
            context.addMessage(null, m);
        }
    }

    public static void addErrorMessage(String summary, String details) {
	showMessage(FacesMessage.SEVERITY_ERROR, summary, details);
    }

    public static void addInfoMessage(String summary, String details) {
	showMessage(FacesMessage.SEVERITY_INFO, summary, details);
    }

    public static void addWarningMessage(String summary, String details) {
	showMessage(FacesMessage.SEVERITY_WARN, summary, details);
    }

    public static <T> T findBean(String beanName, Class<T> classType) {
        FacesContext context = FacesContext.getCurrentInstance();
        return context.getApplication().evaluateExpressionGet(context, "#{" + beanName + "}", classType);
    }

    public static <T> T getObject(String name, Class<T> classType) {
        Object value = FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get(name);
        return classType.isInstance(value) ? classType.cast(value) : null;
    }

    public static HttpSession getCurrentSession() {
        HttpSession session = (HttpSession) FacesContext.getCurrentInstance().getExternalContext().getSession(true);
        return session;
    }

    public static String getRequestParam(String name) {
        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        return params.get(name);
    }

    public static MatrixHolder getMatrixHolder() {
        FacesContext context = FacesContext.getCurrentInstance();
        return context.getApplication().evaluateExpressionGet(context, "#{" + MATRIX_HOLDER + "}", MatrixHolder.class);
    }

    public static SessionModelsMapper getSessionModelsMapper() {
        return (SessionModelsMapper) servletContext.getAttribute(SESSION_MODELS_MAPPER);
    }

    public static PopupMessageHolder getFacesMesageHolder() {
        FacesContext context = FacesContext.getCurrentInstance();
        Map<String, Object> sessionMap = context.getExternalContext().getSessionMap();
        PopupMessageHolder messageHolder = (PopupMessageHolder)sessionMap.get("facesMessageHolder");
        if(messageHolder == null){
            messageHolder = new PopupMessageHolder();
            sessionMap.put("facesMessageHolder", messageHolder);
        }
        return messageHolder;
    }

    public static HelpContentHolder getHelpContentHolder() {
        FacesContext context = FacesContext.getCurrentInstance();
        return context.getApplication().evaluateExpressionGet(context, "#{" + HELP_CONTENT_HOLDER + "}", HelpContentHolder.class);
    }

    public static ConfigBean getConfigBean() {
        FacesContext context = FacesContext.getCurrentInstance();
        return context.getApplication().evaluateExpressionGet(context, "#{configBean}", ConfigBean.class);
    }

    public static ScriptRunsBean getScriptRunsBean() {
        FacesContext context = FacesContext.getCurrentInstance();
        return context.getApplication().evaluateExpressionGet(context, "#{scriptRunsBean}", ScriptRunsBean.class);
    }

    @Deprecated
    public static ContextBean getContextBean() {
        FacesContext context = FacesContext.getCurrentInstance();
        return context.getApplication().evaluateExpressionGet(context, "#{contextBean}", ContextBean.class);
    }

    public static ISFContext getSfContext() {

		return findBean("sfContext", ISFContext.class);

	}

    public static EnumSetContainer getEnumSetContainer() {
        FacesContext context = FacesContext.getCurrentInstance();
        return context.getApplication().evaluateExpressionGet(context, "#{" + ENUM_SET_CONTAINER + "}", EnumSetContainer.class);
    }

    public static String getUser() {
        String sfUser = getSfUser();
        return sfUser == null ? RESTUtil.getSystemUser() : sfUser;
    }

    public static String getSfUser() {
        User user = getObject(KEY_USER, User.class);
        return user != null && !user.isGuest() ? user.getFirstName() + " " + user.getLastName() : null;
    }

    public static String getUser(String prefixName) {
        return String.join(":", prefixName, getUser());
    }

    public static String getJavaTypeLabel(JavaType type) {
        int index = type.value().lastIndexOf(".") + 1;
        String result = type.value();
        return index != 0 ? type.value().substring(index) : result;
    }

    public static String getContextPath(String customReportsPath, boolean button) {
        return StringUtils.isEmpty(customReportsPath) ? button ? StringUtils.EMPTY : FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath()
                : customReportsPath.substring(0, customReportsPath.lastIndexOf("/"));
    }

    public static String getReportRequest(String customReportsPath, CommonReportRow row, boolean button) {
        StringBuilder sb = new StringBuilder(getContextPath(customReportsPath, button));

        sb.append("/report.xhtml?report=");
        sb.append(buildReportUrl(customReportsPath, row, true));

        return sb.toString();
    }

    public static String getZipReport(String customReportsPath, CommonReportRow row, boolean button) {
        StringBuilder sb = new StringBuilder(getContextPath(customReportsPath, button));

        sb.append("/report/");
        sb.append(row.getReportFolder());
        sb.append("?action=simplezip");

        return sb.toString();
    }

    public static String buildReportUrl(String customReportsPath, CommonReportRow row, boolean report) {

        StringBuilder sb = new StringBuilder();

        if(StringUtils.isNotEmpty(customReportsPath)) {
            sb.append(customReportsPath).append("/");
        } else {
            SfInstance instance = row.getSfCurrentInstance();
            if (instance == null) {
                instance = row.getSfInstance();
            }
            sb.append("http://");
            sb.append(instance.getHost())
                    .append(":")
                    .append(instance.getPort())
                    .append(instance.getName())
                    .append("/");
            sb.append(ReportServlet.REPORT_URL_PREFIX + "/"); // see web.xml > servlet-mapping
        }

        sb.append(StringUtil.escapeURL(row.getReportFolder())).append("/");

        if(report) {

            sb.append(row.getReportFile()).append(".html");

        } else {

            sb.append(StringUtil.escapeURL(row.getMatrixName()));

        }

        return sb.toString();

    }
}
