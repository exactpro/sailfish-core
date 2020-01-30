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

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.application.FacesMessage.Severity;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.center.ISFContext;
import com.exactpro.sf.center.impl.SFLocalContext;
import com.exactpro.sf.center.impl.SfInstanceInfo;
import com.exactpro.sf.common.impl.messages.xml.configuration.JavaType;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.embedded.statistics.storage.CommonReportRow;
import com.exactpro.sf.storage.auth.User;
import com.exactpro.sf.testwebgui.configuration.ConfigBean;
import com.exactpro.sf.testwebgui.context.ContextBean;
import com.exactpro.sf.testwebgui.context.PopupMessageHolder;
import com.exactpro.sf.testwebgui.environment.EnumSetContainer;
import com.exactpro.sf.testwebgui.help.HelpContentHolder;
import com.exactpro.sf.testwebgui.restapi.RESTUtil;
import com.exactpro.sf.testwebgui.restapi.machinelearning.MLPersistenceManager;
import com.exactpro.sf.testwebgui.scriptruns.MatrixHolder;
import com.exactpro.sf.testwebgui.scriptruns.ScriptRunsBean;
import com.exactpro.sf.testwebgui.servlets.ReportServlet;
import com.exactpro.sf.testwebgui.servlets.SessionModelsMapper;

public class BeanUtil {
    private static final Logger logger = LoggerFactory.getLogger(BeanUtil.class);

    public static final String WORKSPACE_DISPATCHER = "workspaceDispatcher";
    public static final String KEY_USER = "user";
    public static final String DEFAULT_USER = "guest";
    public static final String ENUM_SET_CONTAINER = "enumSetContainer";
    public static final String MATRIX_HOLDER = "matrixHolder";
    public static final String MATRIX_PROVIDER_HOLDER = "matrixProviderHolder";
    public static final String HELP_CONTENT_HOLDER = "helpContentHolder";
    public static final String SESSION_MODELS_MAPPER = "sessionModelsMapper";
    public static final String ENVIRONMENT_TRACKING_BEAN = "environmentTrackingBean";
    public static final String SESSION_ML_PERSISTENCE_MAPPER = "sessionMLPersistenceMapper";

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

    public static MLPersistenceManager getMLPersistenceManager() {
        return (MLPersistenceManager) servletContext.getAttribute(SESSION_ML_PERSISTENCE_MAPPER);
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

    public static String getReportRequest(String customReportsPath, CommonReportRow row) {
        try {
            return buildReportUrl(customReportsPath, row, true).toString();
        } catch (MalformedURLException | IllegalArgumentException e) {
            logger.error("unable to generate a report link", e);
            return "";
        }
    }

    public static String getZipReport(String customReportsPath, CommonReportRow row, boolean button) throws MalformedURLException, URISyntaxException {
        SfInstanceInfo reportSfInstance = getReportSfInstanceInfo(row);
        URI baseUri = BeanUtil.getBaseUri(customReportsPath, reportSfInstance, false);
        return baseUri.resolve(new URIBuilder()
                .setPath(row.getReportFolder())
                .addParameter("action", "simplezip")
                .build())
        .toString();
    }

    public static URL buildReportUrl(String customReportsPath, CommonReportRow row, boolean report) throws MalformedURLException {

        SfInstanceInfo reportInstance = getReportSfInstanceInfo(row);

        return buildReportUrl(
                    row.getReportFolder(),
                    customReportsPath,
                    reportInstance,
                    SFLocalContext.getDefault().getSfInstanceInfo(),
                    report,
                    row.getTestCaseId(),
                    row.getMatrixName(),
                    false
        ).toURL();

    }

    @Nullable
    private static SfInstanceInfo getReportSfInstanceInfo(CommonReportRow row) {
        SfInstanceInfo reportInstance = SfInstanceInfo.fromSfInstance(row.getSfCurrentInstance());
        if (reportInstance == null) {
            reportInstance = SfInstanceInfo.fromSfInstance(row.getSfInstance());
        }
        return reportInstance;
    }

    public static URI buildRelativeReportUrl(String reportDirectory, SfInstanceInfo instance) {

        if (instance == null) {
            instance = SFLocalContext.getDefault().getSfInstanceInfo();
        }

        return buildReportUrl(
                reportDirectory,
                null,
                instance,
                instance,
                true,
                null,
                null,
                true);
    }

    private static URI buildReportUrl(
            String reportDirectoryPath,
            String customReportsPath,
            SfInstanceInfo reportInstance,
            SfInstanceInfo mlInstance,
            boolean report,
            String testCaseId,
            String matrixName,
            boolean useRelativePath) {

        try {
            URI baseUri = getBaseUri(customReportsPath, reportInstance, useRelativePath);
            baseUri = baseUri.resolve(new URIBuilder().setPath(FilenameUtils.separatorsToUnix(reportDirectoryPath) + "/").build());

            if (report) {
                URIBuilder relativeReportFileUriBuilder = new URIBuilder("index.html");

                if (StringUtils.isNotEmpty(testCaseId)) {
                    relativeReportFileUriBuilder.setParameter("tc", testCaseId);
                }
                relativeReportFileUriBuilder.setParameter("mlapi", getMlApiPath(mlInstance).toString());

                return baseUri.resolve(relativeReportFileUriBuilder.build());

            } else {
                return baseUri.resolve(new URIBuilder().setPath(matrixName).build());
            }

        } catch (URISyntaxException | MalformedURLException e) {
            throw new EPSCommonException(String.format("unable to get report uri with custom path '%s'", customReportsPath), e);
        }
    }

    private static URI getBaseUri(String customReportsPath, SfInstanceInfo reportInstance, boolean useRelativePath) throws URISyntaxException {
        return StringUtils.isNotEmpty(customReportsPath)
                        ? (new URI(customReportsPath.endsWith("/") ? customReportsPath : (customReportsPath + "/")))

                        : (useRelativePath
                            ? (new URI(ReportServlet.REPORT_URL_PREFIX + "/"))

                            : (new URIBuilder()
                                .setScheme("http")
                                .setHost(reportInstance.getHostname())
                                .setPort(reportInstance.getPort())
                                .setPath(String.format("/%s/%s/", reportInstance.getContextPath(), ReportServlet.REPORT_URL_PREFIX))
                                .build())
                        );
    }

    private static URL getMlApiPath(SfInstanceInfo thisInstance) throws MalformedURLException, URISyntaxException {

        try {
            ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();

            return new URIBuilder()
                    .setScheme("http")
                    .setHost(externalContext.getRequestServerName())
                    .setPort(externalContext.getRequestServerPort())
                    .setPath(externalContext.getRequestContextPath() + "/")
                    .build()
                    .resolve("sfapi/machinelearning/v2/")
                    .toURL();

        } catch (Exception e) {
            logger.warn("unable to get ml api path with FacesContext - trying to use SfInstanceInfo instead", e);

            return new URIBuilder()
                    .setScheme("http")
                    .setHost(thisInstance.getHostname())
                    .setPort(thisInstance.getPort())
                    .setPath(thisInstance.getContextPath() + "/")
                    .build()
                    .resolve("sfapi/machinelearning/v2/")
                    .toURL();
        }
    }
}
