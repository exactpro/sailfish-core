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
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

import com.exactpro.sf.center.ISFContext;
import com.exactpro.sf.common.impl.messages.xml.configuration.JavaType;
import com.exactpro.sf.storage.auth.User;
import com.exactpro.sf.testwebgui.configuration.ConfigBean;
import com.exactpro.sf.testwebgui.context.ContextBean;
import com.exactpro.sf.testwebgui.context.PopupMessageHolder;
import com.exactpro.sf.testwebgui.environment.EnumSetContainer;
import com.exactpro.sf.testwebgui.help.HelpContentHolder;
import com.exactpro.sf.testwebgui.restapi.RESTUtil;
import com.exactpro.sf.testwebgui.scriptruns.MatrixHolder;
import com.exactpro.sf.testwebgui.scriptruns.ScriptRunsBean;
import com.exactpro.sf.testwebgui.servlets.SessionModelsMapper;

public class BeanUtil {

	public final static String WORKSPACE_DISPATCHER = "workspaceDispatcher";
    public final static String KEY_USER = "user";
    public final static String DEFAULT_USER = "guest";
    public final static String ENUM_SET_CONTAINER = "enumSetContainer";
    public final static String MATRIX_HOLDER = "matrixHolder";
    public final static String MATRIX_PROVIDER_HOLDER = "matrixProviderHolder";
    public final static String HELP_CONTENT_HOLDER = "helpContentHolder";
    public final static String SESSION_MODELS_MAPPER = "sessionModelsMapper";
    public final static String ENVIRONMENT_TRACKING_BEAN = "environmentTrackingBean";

    private static ServletContext servletContext;

    public static void setServletContext(ServletContext context) {
        if (servletContext == null) {
            servletContext = context;
        }
    }

    private final static Long GROWL_LIFE = 6000L;

    public static void showMessage(FacesMessage.Severity severity, String message, String details) {

        FacesMessage msg = new FacesMessage(severity, message, details);
        FacesContext context = FacesContext.getCurrentInstance();

        PopupMessageHolder holder = getFacesMesageHolder();
        holder.addMessage(msg);

        for(FacesMessage m : holder.getRecentMessages(GROWL_LIFE)) {
            context.addMessage(null, m);
        }
    }

    public static void addErrorMessage(final String summary, final String details) {
	showMessage(FacesMessage.SEVERITY_ERROR, summary, details);
    }

    public static void addInfoMessage(final String summary, final String details) {
	showMessage(FacesMessage.SEVERITY_INFO, summary, details);
    }

    public static void addWarningMessage(final String summary, final String details) {
	showMessage(FacesMessage.SEVERITY_WARN, summary, details);
    }

    public static <T> T findBean(String beanName, Class<T> classType) {
        FacesContext context = FacesContext.getCurrentInstance();
        return context.getApplication().evaluateExpressionGet(context, "#{" + beanName + "}", classType);
    }

    public static <T> T getObject(String name, Class<T> classType) {
        Object value = FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get(name);
        if (classType.isInstance(value)) {
            return classType.cast(value);
        }
        return null;
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
        if (user != null && !user.isGuest()) {
            return user.getFirstName() + " " + user.getLastName();
        }
        return null;
    }

    public static String getUser(String prefixName) {
        return String.join(":", prefixName, getUser());
    }

    public static String getJavaTypeLabel(JavaType type) {
        int index = type.value().lastIndexOf(".") + 1;
        String result = type.value();
        if (index != 0) {
            result = type.value().substring(index);
        }
        return result;
    }
}
