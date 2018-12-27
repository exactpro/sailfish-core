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

import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.storage.auth.User;

@WebListener
public class SessionListener implements HttpSessionListener {

	private static final Logger logger = LoggerFactory.getLogger(SessionListener.class);

	public SessionListener() {

	}

	@Override
	public void sessionCreated(HttpSessionEvent arg0) {

		HttpSession session =  arg0.getSession();

    	logger.info("Session created {}", session.getId());

    	User user = (User)session.getAttribute(BeanUtil.KEY_USER);

    	if(user != null) {

    		logger.info("Session contains user instance: {}", user);

    	} else {

    		session.setAttribute(BeanUtil.KEY_USER, getDefaultUser());

    	}

	}

	@Override
	public void sessionDestroyed(HttpSessionEvent arg0) {

		HttpSession session = arg0.getSession();

    	Object user = session.getAttribute(BeanUtil.KEY_USER);

    	logger.info("Session {} of {} destroyed", session.getId(), user);

	}

	private User getDefaultUser() {

    	User user = new User();

    	user.setName(BeanUtil.DEFAULT_USER);
    	user.setGuest(true);

    	return user;

    }

}
