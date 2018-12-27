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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.servlet.AsyncContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.center.impl.SFLocalContext;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class ReportServlet extends HttpServlet{

    private static final long serialVersionUID = -4289243974895258962L;

    private static final Logger logger = LoggerFactory.getLogger(ReportServlet.class);

	public static final String REPORT_URL_PREFIX = "report"; // see web.xml > servlet-mapping

	private static final String ASYNC_SUP = "org.apache.catalina.ASYNC_SUPPORTED";

	private long contextTimeout = 60000;

	private ExecutorService executor;

	@Override
	public void init(ServletConfig config) throws ServletException {

		super.init(config);

		ThreadFactory threadFactory = new ThreadFactoryBuilder()
		        .setNameFormat(ReportServlet.class.getSimpleName() + "-%d")
		        .setDaemon(true)
		        .build();
		this.executor = new ThreadPoolExecutor(4, 4, 1, TimeUnit.MINUTES, new LinkedBlockingQueue<Runnable>(), threadFactory);

	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		req.setAttribute(ASYNC_SUP, true);
		AsyncContext ctx = req.startAsync(req, resp);
//		ctx.addListener(new RequestAsyncListener());
		ctx.setTimeout(contextTimeout);
		logger.trace("Create new async context for request {}{}, session {}", req.getRequestURL(), req.getQueryString(), req.getSession().getId());
	    this.executor.execute(new ReportTask(ctx, SFLocalContext.getDefault()));
	}



	@Override
	public void destroy() {

		this.executor.shutdown();
		super.destroy();

	}

}
