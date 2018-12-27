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
package com.exactpro.sf.testwebgui.notifications.web;

import java.io.IOException;

import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestAsyncListener implements AsyncListener{

	private static final Logger logger = LoggerFactory.getLogger(RequestAsyncListener.class);

	@Override
	public void onComplete(AsyncEvent event) throws IOException {
		logger.debug("Request {} complete", getRequest(event));
	}

	@Override
	public void onError(AsyncEvent event) throws IOException {
		logger.error("Request {} error", getRequest(event), event.getThrowable());
	}

	@Override
	public void onStartAsync(AsyncEvent event) throws IOException {
		logger.debug("Request {} start async", getRequest(event));
	}

	@Override
	public void onTimeout(AsyncEvent event) throws IOException {
		logger.debug("Request {} timeout", getRequest(event));
	}

	private String getRequest(AsyncEvent event) {
		HttpServletRequest request = (HttpServletRequest) event.getAsyncContext().getRequest();
		return request.getRequestURL() + request.getQueryString();
	}

}
