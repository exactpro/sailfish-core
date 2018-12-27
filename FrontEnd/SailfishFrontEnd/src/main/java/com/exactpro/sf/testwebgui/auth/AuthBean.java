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
package com.exactpro.sf.testwebgui.auth;

import java.io.IOException;
import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.storage.StorageException;
import com.exactpro.sf.storage.auth.PasswordHasher;
import com.exactpro.sf.storage.auth.User;
import com.exactpro.sf.testwebgui.BeanUtil;

@ManagedBean(name = "authBean")
@ViewScoped
@SuppressWarnings("serial")
public class AuthBean implements Serializable {

	private static final Logger logger = LoggerFactory.getLogger(AuthBean.class);

	private String username;
	private String password;
	private String originalURL;

	public AuthBean() {


	}

	@PostConstruct
	public void init() {
		ExternalContext externalContext = FacesContext.getCurrentInstance()
				.getExternalContext();
		originalURL = (String) externalContext.getRequestMap().get(
				RequestDispatcher.FORWARD_REQUEST_URI);

		if (originalURL == null) {
			originalURL = externalContext.getRequestContextPath()
					+ "/index.xhtml";
		} else {
			String originalQuery = (String) externalContext.getRequestMap()
					.get(RequestDispatcher.FORWARD_QUERY_STRING);

			if (originalQuery != null) {
				originalURL += "?" + originalQuery;
			}
		}
	}

	public void login() throws IOException {

		FacesContext context = FacesContext.getCurrentInstance();
		ExternalContext externalContext = context.getExternalContext();
		HttpServletRequest request = (HttpServletRequest) externalContext.getRequest();

		try {

			request.login(username, password + PasswordHasher.getSalt());

			User user = BeanUtil.getSfContext().getAuthStorage().getUser(username);

			if (user == null) {

				logger.error("User with login [{}] not found in storage!", this.username);

				BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR,
						"Invalid login/password pair", "");

				return;
			}

			externalContext.getSessionMap().put(BeanUtil.KEY_USER, user);

			externalContext.redirect(originalURL);

		} catch (ServletException e) {

			// Handle unknown username/password in request.login().
			logger.warn("Bad login attempt with username [{}]; message: {}", this.username ,e.getMessage());
			BeanUtil.showMessage(FacesMessage.SEVERITY_ERROR, "Invalid login/password pair", "");

			return;
		}

		logger.info("Successful login for user [{}]", username);
	}

	public void logout() throws IOException {
		ExternalContext externalContext = FacesContext.getCurrentInstance()
				.getExternalContext();
		externalContext.invalidateSession();
		externalContext.redirect(externalContext.getRequestContextPath()
				+ "/index.xhtml");
		externalContext.getSessionMap().remove(BeanUtil.KEY_USER);
	}

	public void saveUserSettings(User user) {

		logger.debug("Save user invoked");

		try {
			BeanUtil.getSfContext().getAuthStorage().addUser(user);
			BeanUtil.showMessage(FacesMessage.SEVERITY_INFO, "Settings saved",	"");
		} catch (StorageException e) {
			logger.error("Error save user's setting");
			BeanUtil.showMessage(FacesMessage.SEVERITY_INFO, "Settings not correct",	"");
		}

	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username != null ? username.toLowerCase() : null ;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getHumanUsername() {
	    return BeanUtil.getUser();
	}

	public boolean isLoggedIn() {
	    User user = BeanUtil.getObject(BeanUtil.KEY_USER, User.class);
	    return user != null && !user.isGuest();
	}
}