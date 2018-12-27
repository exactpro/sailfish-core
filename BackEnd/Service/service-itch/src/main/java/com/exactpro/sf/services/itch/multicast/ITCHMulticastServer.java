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
package com.exactpro.sf.services.itch.multicast;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.MsgMetaData;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.services.ServiceInfo;
import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.aml.script.actions.WaitAction;
import com.exactpro.sf.configuration.ILoggingConfigurator;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.IActionContext;
import com.exactpro.sf.services.IInitiatorService;
import com.exactpro.sf.services.IServiceContext;
import com.exactpro.sf.services.IServiceHandler;
import com.exactpro.sf.services.IServiceMonitor;
import com.exactpro.sf.services.IServiceSettings;
import com.exactpro.sf.services.ISession;
import com.exactpro.sf.services.ITaskExecutor;
import com.exactpro.sf.services.MessageHelper;
import com.exactpro.sf.services.ServiceException;
import com.exactpro.sf.services.ServiceHandlerRoute;
import com.exactpro.sf.services.ServiceStatus;
import com.exactpro.sf.services.itch.ITCHMessageHelper;
import com.exactpro.sf.services.util.ServiceUtil;
import com.exactpro.sf.storage.IMessageStorage;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by alexey.zarovny on 11/18/14.
 */
public class ITCHMulticastServer extends IoHandlerAdapter implements IInitiatorService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName() + "@" + Integer.toHexString(hashCode()));

    private volatile ServiceStatus curStatus;
    private ServiceName serviceName;

    private IServiceContext serviceContext;

    private IMessageStorage msgStorage;
    private ILoggingConfigurator logConfigurator;
    private ServiceInfo serviceInfo;
    private IServiceMonitor monitor;
    private ITaskExecutor taskExecutor;
    private IServiceHandler handler;
    private ITCHMulticastSettings settings;
    private IMessageFactory msgFactory;
    private IDictionaryStructure dictionary;
    private ITCHMulticastUDPSession udpSession;
    private ITCHMulticastTCPSession tcpSession;
    protected MessageHelper itchHandler;

    private ITCHMulticastCache cache;

    @Override
    public void init(
    		IServiceContext serviceContext,
    		final IServiceMonitor serviceMonitor,
    		final IServiceHandler handler,
    		final IServiceSettings settings,
    		final ServiceName name) {

        try {
            this.serviceName = name;

            if (settings == null)
                throw new NullPointerException("'settings' parameter is null");

            this.settings = (ITCHMulticastSettings) settings;

            this.serviceContext = Objects.requireNonNull(serviceContext, "'Service context' parameter is null");

            this.msgStorage = Objects.requireNonNull(this.serviceContext.getMessageStorage(), "'Storage' parameter is null");
            this.serviceInfo = serviceContext.lookupService(name);

            this.monitor = serviceMonitor;
            this.taskExecutor = Objects.requireNonNull(this.serviceContext.getTaskExecutor(), "'Task executor' parameter is null");
            this.logConfigurator = Objects.requireNonNull(this.serviceContext.getLoggingConfigurator(), "'Logging configurator' parameter is null");

            if (!((this.settings.getPrimaryAddress() != null && this.settings.getPrimaryPort() != 0)
                    || (this.settings.getSecondaryAddress() != null && this.settings.getSecondaryPort() != 0)))
                throw new NullPointerException("At least one of address/port pairs must be set");


            this.msgFactory = this.serviceContext.getDictionaryManager().getMessageFactory(this.settings.getDictionaryName());

            Objects.requireNonNull(this.settings.getDictionaryName(), "'Dictionary name' parameter incorrect");
            this.dictionary = this.serviceContext.getDictionaryManager().getDictionary(this.settings.getDictionaryName());
            Objects.requireNonNull(dictionary, "'Dictionary' parameter");

            itchHandler = new ITCHMessageHelper();
            itchHandler.init(this.msgFactory, this.dictionary);

            this.udpSession = new ITCHMulticastUDPSession(this.serviceContext, serviceName + "UDP",
                    settings.getDictionaryName(), this.settings.getMarketDataGroup(), this, itchHandler, msgFactory);
            this.tcpSession = new ITCHMulticastTCPSession(this.serviceContext,
                    serviceName + "TCP", settings.getDictionaryName(), this, itchHandler, msgFactory);

            this.handler = Objects.requireNonNull(handler, "'Service handler' parameter");

            logger.info("Initiliazing service {} ... done", this);
            this.changeStatus(ServiceStatus.INITIALIZED, "Service initialized", null);
        } catch (Throwable t) {
            logger.error("Exception during service [{}] initializing", this.serviceName, t);
            this.changeStatus(ServiceStatus.ERROR, "", t);
            throw new ServiceException("Problem during service [" + this.serviceName + "] initializing", t);
        }
    }


    @Override
    public ISession getSession() {
        return udpSession;
    }

    @Override
    public void connect() throws Exception {

    }

    @Override
    public void start() {
        logConfigurator.createIndividualAppender(this.getClass().getName() + "@" + Integer.toHexString(hashCode()),
                serviceName);
        try {
            cache = new ITCHMulticastCache(settings.getCacheSize());
            udpSession.open(settings.getPrimaryPort(), settings.getPrimaryAddress(), settings.getSecondaryPort(), settings.getSecondaryAddress(), cache);
            tcpSession.open(settings.getTcpPort(), cache);
            this.changeStatus(ServiceStatus.STARTED, "Service started", null);

        } catch (IOException e) {
            logger.error("Exception during service [{}] starting", this.serviceName, e);
            this.changeStatus(ServiceStatus.ERROR, "", e);
            throw new ServiceException("Problem during service [" + this.serviceName + "] starting", e);
        }
    }

    @Override
    public void dispose() {
        try {
            if(udpSession != null) {
                udpSession.close();
            }
            if(tcpSession != null){
                tcpSession.close();
            }
            this.changeStatus(ServiceStatus.DISPOSED, "Service disposed", null);
        } catch (Exception e) {
            logger.error("Exception during service [{}] disposing", this.serviceName, e);
            this.changeStatus(ServiceStatus.ERROR, "", e);
            throw new ServiceException("Problem during service [" + this.serviceName + "] disposing", e);
        } finally {
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
        return curStatus;
    }

    @Override
    public IServiceSettings getSettings() {
        return settings;
    }

    private void changeStatus(ServiceStatus status, String message, Throwable t) {
        this.curStatus = status;
        ServiceUtil.changeStatus(this, monitor, status, message, t);
    }

    void handleMessage(boolean isFrom, boolean isAdmin, IMessage iMessage, ISession iSession, String remoteName) {

        List<IMessage> messages;
        if (iMessage.getField(ITCHMessageHelper.SUBMESSAGES_FIELD_NAME) != null) {
            messages = iMessage.<List<IMessage>>getField(ITCHMessageHelper.SUBMESSAGES_FIELD_NAME);
        } else {
            messages = new ArrayList<>();
            messages.add(iMessage);
        }
        for (IMessage msg:messages) {
            MsgMetaData metaData = msg.getMetaData();

            if (isFrom) {
                metaData.setToService(this.serviceName.toString());
                metaData.setFromService(remoteName);
            } else {
                metaData.setFromService(this.serviceName.toString());
                metaData.setToService(remoteName);
            }

            metaData.setServiceInfo(serviceInfo);

            if (settings.isStoreMessages()) {
                this.msgStorage.storeMessage(msg);
            }
            
            if (isFrom) {
                logger.debug("Message received: {} ", msg);
            } else {
                logger.debug("Message sent: {} ", msg);
            }

            try {
                if (isAdmin && isFrom) {
                    logger.debug("Add fromAdmin: {}", msg.getName());
                    handler.putMessage(iSession, ServiceHandlerRoute.FROM_ADMIN, msg);
                } else if (!isAdmin && isFrom) {
                    logger.debug("Add fromApp: {}", msg.getName());
                    handler.putMessage(iSession, ServiceHandlerRoute.FROM_APP, msg);
                } else if (!isAdmin && !isFrom) {
                    logger.debug("Add toApp: {}", msg.getName());
                    handler.putMessage(iSession, ServiceHandlerRoute.TO_APP, msg);
                } else if (!isAdmin && !isFrom) {
                    logger.debug("Add toAdmin: {}", msg.getName());
                    handler.putMessage(iSession, ServiceHandlerRoute.TO_ADMIN, msg);
                }
            } catch (Exception e) {
                logger.error("Exception in handler", e);
            }
        }


    }

    @Override
    public IMessage receive(IActionContext actionContext, IMessage msg) throws InterruptedException {
		return (IMessage) WaitAction.waitForMessage(actionContext, msg, !msg.getMetaData().isAdmin());
    }

	public ITaskExecutor getTaskExecutor() {
		return taskExecutor;
	}

}
