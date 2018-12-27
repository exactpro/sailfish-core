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
package com.exactpro.sf.services.loopback;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.aml.script.actions.WaitAction;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.MsgMetaData;
import com.exactpro.sf.common.services.ServiceInfo;
import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.ILoggingConfigurator;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.IActionContext;
import com.exactpro.sf.services.IInitiatorService;
import com.exactpro.sf.services.IServiceContext;
import com.exactpro.sf.services.IServiceHandler;
import com.exactpro.sf.services.IServiceMonitor;
import com.exactpro.sf.services.IServiceSettings;
import com.exactpro.sf.services.ISession;
import com.exactpro.sf.services.ServiceHandlerException;
import com.exactpro.sf.services.ServiceHandlerRoute;
import com.exactpro.sf.services.ServiceStatus;
import com.exactpro.sf.storage.IMessageStorage;

public class LoopbackService implements IInitiatorService {

	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName() + "@" + Integer.toHexString(hashCode()));

	private ServiceName serviceName;
	private IServiceHandler handler;
	private LoopbackServiceSettings settings;
	private IMessageStorage storage;

	private ILoggingConfigurator logConfigurator;

	private volatile ServiceStatus status = ServiceStatus.CREATED;
	private volatile LoopbackSession session = null;

	private ServiceInfo serviceInfo;

	@Override
	public void init(
			final IServiceContext serviceContext,
			final IServiceMonitor serviceMonitor,
			final IServiceHandler handler,
			final IServiceSettings settings,
			final ServiceName name) {
		this.serviceName = name;
		this.handler = Objects.requireNonNull(handler, "'Service handler' parameter");
		this.settings = (LoopbackServiceSettings) settings;
		this.storage = Objects.requireNonNull(serviceContext.getMessageStorage(), "'Message storage' parameter");
		this.logConfigurator = Objects.requireNonNull(serviceContext.getLoggingConfigurator(), "'Logging configurator' parameter");

        this.serviceInfo = serviceContext.lookupService(name);
		status = ServiceStatus.INITIALIZED;

	}

	@Override
	public void start() {
        logConfigurator.createIndividualAppender(this.getClass().getName() + "@" + Integer.toHexString(hashCode()),
                serviceName);
		this.session = new LoopbackSession();
		status = ServiceStatus.STARTED;
	}

	@Override
	public void dispose() {
		session = null;
		status = ServiceStatus.DISPOSED;

		if(logConfigurator != null) {
            logConfigurator.destroyIndividualAppender(this.getClass().getName() + "@" + Integer.toHexString(hashCode()),
                serviceName);
		}
	}

	@Override
	public IServiceHandler getServiceHandler() {
		return handler;
	}

	@Override
	public void setServiceHandler(IServiceHandler handler) {
		this.handler = Objects.requireNonNull(handler, "'Service handler' parameter");
	}

	@Override
	public String getName() {
		return serviceName.toString();
	}

	@Override
	public ServiceName getServiceName() {
		return serviceName;
	}

	@Override
	public ServiceStatus getStatus() {
		return status;
	}

	@Override
	public ISession getSession() {
		return session;
	}

	class LoopbackSession implements ISession {

		@Override
		public String getName() {
			return serviceName.toString();
		}

		@Override
		public IMessage send(Object message) throws InterruptedException {
			if (!(message instanceof IMessage)) {
				throw new EPSCommonException("Unsupported message type: " + message.getClass().getCanonicalName());
            }

			IMessage iMsg = (IMessage) message;

			try {
				if (iMsg.getMetaData().isAdmin()) {
					getServiceHandler().putMessage(getSession(), ServiceHandlerRoute.TO_ADMIN, iMsg);
				} else {
					getServiceHandler().putMessage(getSession(), ServiceHandlerRoute.TO_APP, iMsg);
				}
			} catch (Exception e) {
				logger.warn("serviceHandler thrown exception",e);
			}

			logger.debug("message passed to ServericeHandler");

			iMsg.getMetaData().setServiceInfo(serviceInfo);

			logger.debug("message passed to msgStorage");
            storage.storeMessage(iMsg);
			LoopbackService.this.handlerReceivedMessage(iMsg);
			return iMsg;
		}


		@Override
		public IMessage sendDirty(Object message) throws InterruptedException {
			logger.error("This service not support send dirty message. Message will be send usual method");
			return send(message);
		}

		@Override
		public void close() {
		}

		@Override
		public boolean isClosed() {
			return false;
		}

		@Override
		public boolean isLoggedOn() {
            return LoopbackService.this.getStatus() == ServiceStatus.STARTED;
		}
	}

	public void handlerReceivedMessage(IMessage iMsg) {
		logger.debug("handleReceivedMessage");
		try {
			MsgMetaData metaData = iMsg.getMetaData();

			metaData.setFromService(getName());
			metaData.setToService(getName());
			logger.debug("passing message to ServericeHandler");
			if (iMsg.getMetaData().isAdmin()) {
				metaData.setAdmin(true);
				getServiceHandler().putMessage(getSession(), ServiceHandlerRoute.FROM_ADMIN, iMsg);
			} else {
				metaData.setAdmin(false);
				getServiceHandler().putMessage(getSession(), ServiceHandlerRoute.FROM_APP, iMsg);
			}
			logger.debug("message passed to ServericeHandler");

			metaData.setServiceInfo(serviceInfo);

			logger.debug("message passed to msgStorage");
            storage.storeMessage(iMsg);

			logger.debug("message stored");

		} catch (ServiceHandlerException e) {
			logger.info("Execute service handler failed\nmsg: {})", e.getSfMessage(), e);
			getServiceHandler().exceptionCaught(e.getSession(), e);
		} catch (Exception e) {
			logger.info("Caught exception while executin fromApp or fromAdmin methods of service handler\nIMessage: {})", iMsg, e);
			getServiceHandler().exceptionCaught(getSession(), e);
		}
	}

	@Override
	public IServiceSettings getSettings() {
		return settings;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this).append("name", serviceName).toString();
	}

	@Override
	public void connect() throws Exception
	{
	}


	@Override
    public IMessage receive(IActionContext actionContext, IMessage msg) throws InterruptedException {
		return WaitAction.waitForMessage(actionContext, msg, !msg.getMetaData().isAdmin());
    }
}
