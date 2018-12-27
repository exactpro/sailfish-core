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
package com.exactpro.sf.testwebgui.servlets;

import java.io.IOException;

import javax.servlet.AsyncContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.testwebgui.notifications.chanel.SFWebHandlerFactory;
import com.exactprosystems.webchannels.channel.AbstactMessageFactory;
import com.exactprosystems.webchannels.channel.AbstractHandlerFactory;
import com.exactprosystems.webchannels.channel.ChannelSettings;
import com.exactprosystems.webchannels.channel.HttpChannelProcessor;
import com.exactprosystems.webchannels.channel.JsonMessageFactory;

public class PollingServlet extends HttpServlet {
    public static final int BUFFER_SIZE = 64;

    private static final long serialVersionUID = 1L;

	private static final Logger logger = LoggerFactory.getLogger(PollingServlet.class);

	private static final String ASYNC_SUP = "org.apache.catalina.ASYNC_SUPPORTED";

	private long contextTimeout = 60000;

	private HttpChannelProcessor processor;

	@Override
	public void init(ServletConfig config) throws ServletException {

		try	{

			super.init(config);

            ChannelSettings defaultSettings = new ChannelSettings();

            ChannelSettings settings = new ChannelSettings(defaultSettings.getPollingInterval(),
                                                              defaultSettings.getHeartBeatInterval(),
                                                              defaultSettings.getMaxCountToSend(),
                                                              defaultSettings.getExecutorBatchSize(),
                                                              defaultSettings.getDisconnectTimeout(),
                                                              defaultSettings.getThreadCount(), BUFFER_SIZE,
                                                              defaultSettings.isCompressionEnabled());

			AbstractHandlerFactory handlerFactory = new SFWebHandlerFactory();
			AbstactMessageFactory messageFactory = new JsonMessageFactory();

			processor = new HttpChannelProcessor(handlerFactory, messageFactory, settings);

			config.getServletContext().addListener(processor);

			logger.info("Servlet {} initialized", config.getServletName());

		} catch ( Throwable e )	{
			logger.error("Could not initialize application", e);
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException ,IOException {

		req.setAttribute(ASYNC_SUP, true);
		AsyncContext ctx = req.startAsync(req, resp);
//		ctx.addListener(new RequestAsyncListener());
		ctx.setTimeout(contextTimeout);
		logger.trace("Create new async context for request {}{}, session {}", req.getRequestURL(), req.getQueryString(), req.getSession().getId());
		processor.processAsyncContext(ctx);

	};

	@Override
	public void destroy() {
		processor.destroy();
		super.destroy();
		logger.info("Polling servlet desroyed");

	}



}
