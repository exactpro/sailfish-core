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
import com.exactpro.sf.common.messages.AttributeNotFoundException;
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
import com.exactpro.sf.services.MessageHelper;
import com.exactpro.sf.services.ServiceException;
import com.exactpro.sf.services.ServiceHandlerRoute;
import com.exactpro.sf.services.ServiceStatus;
import com.exactpro.sf.storage.IMessageStorage;

public class LoopbackService implements IInitiatorService {

    private final Logger logger = LoggerFactory.getLogger(ILoggingConfigurator.getLoggerName(this));

	private ServiceName serviceName;
	private IServiceHandler handler;
	private LoopbackServiceSettings settings;
	private IMessageStorage storage;
	private IServiceContext context;
	private ServiceInfo serviceInfo;
	private ILoggingConfigurator logConfigurator;

	private volatile ServiceStatus status = ServiceStatus.CREATED;
    private volatile LoopbackSession session;


	@Override
	public void init(
            IServiceContext serviceContext,
            IServiceMonitor serviceMonitor,
            IServiceHandler handler,
            IServiceSettings settings,
            ServiceName name) {

		this.serviceName = name;
		this.handler = Objects.requireNonNull(handler, "'Service handler' parameter");
		this.settings = (LoopbackServiceSettings) settings;
		this.storage = Objects.requireNonNull(serviceContext.getMessageStorage(), "'Message storage' parameter");
		this.logConfigurator = Objects.requireNonNull(serviceContext.getLoggingConfigurator(), "'Logging configurator' parameter");
        this.context = serviceContext;
        this.serviceInfo = Objects.requireNonNull(serviceContext.lookupService(serviceName), "serviceInfo cannot be null");

		status = ServiceStatus.INITIALIZED;
	}

	@Override
	public void start() {
        logConfigurator.createAndRegister(getServiceName(), this);
		session = new LoopbackSession();
		status = ServiceStatus.STARTED;
	}

	@Override
	public void dispose() {
		session = null;
		if (logConfigurator != null) {
            logConfigurator.destroyAppender(getServiceName());
		}
        status = ServiceStatus.DISPOSED;

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
		public IMessage send(Object message) {
			if (!(message instanceof IMessage)) {
				throw new EPSCommonException("Unsupported message type: " + message.getClass().getCanonicalName());
            }

			IMessage received = (IMessage) message;
			boolean isAdmin = false;
			try {
				isAdmin = MessageHelper.isAdmin(context.getDictionaryManager().getDictionary(((IMessage) message).getMetaData().getDictionaryURI())
                        .getMessages().get(((IMessage)message).getName()));

			} catch (AttributeNotFoundException e) {
				throw new ServiceException("Unable to get message attribute", e);
			}

			MsgMetaData metaData = received.getMetaData();
            metaData.setServiceInfo(serviceInfo);
            metaData.setAdmin(isAdmin);
            metaData.setFromService(getName());
            metaData.setToService(getName());

            try {
                getServiceHandler().putMessage(session, isAdmin ? ServiceHandlerRoute.TO_ADMIN : ServiceHandlerRoute.TO_APP, received);
                storage.storeMessage(received);

                IMessage sent = received.cloneMessage();

                getServiceHandler().putMessage(session, isAdmin ? ServiceHandlerRoute.FROM_ADMIN : ServiceHandlerRoute.FROM_APP, sent);
                storage.storeMessage(sent);

            } catch (Exception e) {
                logger.error("Unable to process message: '{}')", received, e);
                getServiceHandler().exceptionCaught(session, e);
            }

			return received;
		}


		@Override
		public IMessage sendDirty(Object message) {
			logger.warn("This service does not support dirty messages - falling back to send().");
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
            return status == ServiceStatus.STARTED;
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
	public void connect() {
    }

	@Override
    public IMessage receive(IActionContext actionContext, IMessage msg) throws InterruptedException {
		return WaitAction.waitForMessage(actionContext, msg, !msg.getMetaData().isAdmin());
    }
}
