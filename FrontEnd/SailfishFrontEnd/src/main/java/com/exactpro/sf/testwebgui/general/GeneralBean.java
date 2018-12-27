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
package com.exactpro.sf.testwebgui.general;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import com.exactpro.sf.center.ISFContext;
import com.exactpro.sf.center.impl.SFLocalContext;
import com.exactpro.sf.testwebgui.BeanUtil;
import com.exactpro.sf.testwebgui.SFWebApplication;
import com.exactpro.sf.testwebgui.context.FacesMessageAdapter;

@ManagedBean(name="generalBean")
@ApplicationScoped
@SuppressWarnings("serial")
public class GeneralBean implements Serializable {

	private static final String ADMIN_ROLE_NAME = "admin";
	private static final String DEFAULT_GUI_DATE_FORMAT = "dd-MM-yyyy hh:mm:ss";

    private String version = "";
    private String branchName = "";

	@PostConstruct
	public void init() {
        ISFContext context = SFLocalContext.getDefault();

        if(context != null) {
            this.version = context.getVersion();
            this.branchName = context.getBranchName();
        }
    }

	public String getVersion() {
	    return this.version;
	}

	public String getBranchName() {
		return branchName;
	}

	public boolean pageNotRestricted(String name) {
		return SFWebApplication.getInstance().isPageNotRestricted(name);
	}

	public String getReleaseName() {
		return SFWebApplication.getInstance().getRelease();
	}

	public String getAdminRoleName() {
		return ADMIN_ROLE_NAME;
	}

    public List<FacesMessageAdapter> getFacesMessages() {
        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        String uri = request.getRequestURI();
        List<FacesMessageAdapter> list = new ArrayList<FacesMessageAdapter>();

        for(FacesMessageAdapter adapter : BeanUtil.getFacesMesageHolder().getMessagesList(uri)) {
            list.add(adapter);
        }

        Collections.reverse(list);
        return list;
    }

    public void clearFacesMessagesList() {
        BeanUtil.getFacesMesageHolder().clearMessagesList();
    }

	public void showGrowlMessage() {
		String severityRaw = BeanUtil.getRequestParam("Severity");
		String summary = BeanUtil.getRequestParam("Summary");
		String detail = BeanUtil.getRequestParam("Detail");

		FacesMessage.Severity severity;

		if(severityRaw.equals("WARN")) {
			severity = FacesMessage.SEVERITY_WARN;
		} else if(severityRaw.equals("ERROR")) {
			severity = FacesMessage.SEVERITY_ERROR;
		} else {
			severity = FacesMessage.SEVERITY_INFO;
		}

		BeanUtil.showMessage(severity, summary, detail);
	}

	public String abbreviateString(String string, int length) {

		if(string == null || string.length() <= length) {
			return string;
		}

		return string.substring(0, length - 3) + "...";

	}

	public String getDefaultGuiDateFormat() {
		return DEFAULT_GUI_DATE_FORMAT;
	}

	public String getContextPath() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest req = (HttpServletRequest) context.getExternalContext().getRequest();
        String url = req.getRequestURL().toString();
        String appContext = context.getExternalContext().getApplicationContextPath();
        String contextPath = url.substring(0, url.indexOf(appContext) + appContext.length());
        return contextPath;
    }
	
	public String getSfUser() {
	    return BeanUtil.getSfUser();
	}
}
