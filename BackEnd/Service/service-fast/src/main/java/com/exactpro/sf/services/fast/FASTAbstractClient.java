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
package com.exactpro.sf.services.fast;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.openfast.Context;
import org.openfast.Message;
import org.openfast.MessageBlockReader;
import org.openfast.session.Connection;
import org.openfast.session.FastConnectionException;
import org.openfast.template.TemplateRegistry;
import org.openfast.template.loader.XMLMessageTemplateLoader;
import org.openfast.util.RecordingInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.aml.script.actions.WaitAction;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.MsgMetaData;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.services.ServiceInfo;
import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.IDataManager;
import com.exactpro.sf.configuration.IDictionaryManager;
import com.exactpro.sf.configuration.ILoggingConfigurator;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
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
import com.exactpro.sf.services.fast.converter.ConverterException;
import com.exactpro.sf.services.fast.converter.FastToIMessageConverter;
import com.exactpro.sf.services.fast.filter.IFastMessageFilter;
import com.exactpro.sf.services.fast.filter.SimpleMessageFilter;
import com.exactpro.sf.services.fast.fixup.EofCheckedStream;
import com.exactpro.sf.services.fast.fixup.EofIOException;
import com.exactpro.sf.services.util.ServiceUtil;
import com.exactpro.sf.storage.IMessageStorage;

public abstract class FASTAbstractClient implements IInitiatorService {

	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName() + "@" + Integer.toHexString(hashCode()));

	private volatile ServiceStatus curStatus;

	private ISession session = null;

	private final AtomicBoolean sessionClosed = new AtomicBoolean(true);

	private FASTClientSettings settings;
	protected ServiceName serviceName;
	private IServiceHandler handler;
	protected IMessageStorage msgStorage;
	private IServiceMonitor monitor;
	protected ServiceInfo serviceInfo;
	protected IWorkspaceDispatcher workspaceDispatcher;

	protected IServiceContext serviceContext;
	protected IDictionaryManager dictionaryManager;
	protected IDictionaryStructure dictionary;
	protected MessageHelper messageHelper;
	protected IFastMessageFilter messageFilter;

	private TemplateRegistry registry = null;

	private FastToIMessageConverter converter;

	protected Connection connection = null;

	protected FASTMessageInputStream msgInStream;

	private Thread thread = null;

	private RecordingInputStream recordingInputStream;

	private ILoggingConfigurator logConfigurator;

    private Context fastContext;

	@Override
	public void init(
			IServiceContext serviceContext,
			final IServiceMonitor serviceMonitor,
			final IServiceHandler handler,
			final IServiceSettings settings,
			final ServiceName name) {

		logger.debug("Initializing AbstractFastClient {}", this);

		try {
		    this.changeStatus(ServiceStatus.INITIALIZING, "Service initializing", null);
            logger.info("Initializing service {} ...", this);

            this.serviceName = Objects.requireNonNull(name, "'Service name' parameter");

            this.serviceContext = Objects.requireNonNull(serviceContext, "'Service context' parameter");

            internalInit(name, handler, settings, serviceMonitor, serviceContext);

			this.changeStatus(ServiceStatus.INITIALIZED, "Service initialized", null);
			logger.debug("Abstract client initialized");

		} catch ( RuntimeException e ) {
			logger.error("Exception during service [{}] initializing", this.serviceName, e);
			this.changeStatus(ServiceStatus.ERROR, "", e);
			throw new ServiceException(e);
		}
	}

	protected void internalInit(
            final ServiceName name,
            final IServiceHandler handler,
            final IServiceSettings settings,
            final IServiceMonitor serviceMonitor,
            IServiceContext serviceContext) {

        this.monitor = Objects.requireNonNull(serviceMonitor, "'Service monitor' parameter");

        this.msgStorage = Objects.requireNonNull(this.serviceContext.getMessageStorage(), "'Message storage' parameter");

        if (settings == null) {
            throw new NullPointerException("'settings' parameter");
        }
        this.setSettings((FASTClientSettings) settings);

        this.handler = Objects.requireNonNull(handler, "'Service handler' parameter");

        this.dictionaryManager = Objects.requireNonNull(serviceContext.getDictionaryManager(), "'Dictionary manager' parameter");

        if (this.getSettings().getDictionaryName() == null) {
            throw new NullPointerException("settings.dictionaryName is null");
        }

        this.workspaceDispatcher = Objects.requireNonNull(serviceContext.getWorkspaceDispatcher(), "'Workspace dispatcher' parameter");

        this.logConfigurator = Objects.requireNonNull(this.serviceContext.getLoggingConfigurator(), "'Logging configurator' parameter");
        this.serviceInfo = serviceContext.lookupService(serviceName);

        logger.info("Initializing service [{}] ... done", this.serviceName);

        this.messageFilter = configureMessageFilter();
        SailfishURI dictionaryName = getSettings().getDictionaryName();
        this.dictionary = dictionaryManager.getDictionary(dictionaryName);

        String templateName = Objects.requireNonNull((String) dictionary.getAttributeValueByName(FASTMessageHelper.TEMPLATE_ATTRIBYTE), "'Template attribute' parameter");
        loadFastTemplates(serviceContext.getDataManager(), dictionaryName.getPluginAlias(), templateName);

        this.messageHelper = new FASTMessageHelper();
        this.messageHelper.init(this.dictionaryManager.getMessageFactory(dictionaryName), this.dictionary);
    }

	private IFastMessageFilter configureMessageFilter() {
		FASTClientSettings settigns = getSettings();
		String requiredValues = settigns.getMessageFilterExpression();
		if (requiredValues == null) {
			return new SimpleMessageFilter();
		}
		return new SimpleMessageFilter(requiredValues);
	}


	protected FastToIMessageConverter createConverter() {
		if (this.converter == null) {
			FastToIMessageConverter converter = new FastToIMessageConverter(
					dictionaryManager.getMessageFactory(getSettings().getDictionaryName()),
					dictionary.getNamespace()
			);
			this.converter = converter;
		}
		return converter;
	}

	private void loadFastTemplates(final IDataManager dataManager, String pluginAlias, String templateName) {
		XMLMessageTemplateLoader loader = new XMLMessageTemplateLoader();
		loader.setLoadTemplateIdFromAuxId(true);

		try (InputStream templateStream = dataManager.getDataInputStream(pluginAlias, FASTMessageHelper.getTemplatePath(templateName))) {
			loader.load(templateStream);
		} catch (IOException e) {
            logger.warn("Can not read template {} from resources", templateName, e);
			throw new EPSCommonException("Can not read template " + templateName + " from resources", e);
		}

        setRegistry(loader.getTemplateRegistry());
	}

	protected void changeStatus(ServiceStatus status, String message, Throwable e) {
		this.curStatus = status;
		ServiceUtil.changeStatus(this, monitor, status, message, e);
	}

	@Override
	public IServiceHandler getServiceHandler() {
		return this.handler;
	}

	@Override
	public void dispose() {
		this.changeStatus(ServiceStatus.DISPOSING, "Service is disposing", null);
		doDispose();
		this.changeStatus(ServiceStatus.DISPOSED, "Service disposed", null);

		if(logConfigurator != null) {
            logConfigurator.destroyIndividualAppender(this.getClass().getName() + "@" + Integer.toHexString(hashCode()),
                    serviceName);
		}
	}

	protected void doDispose() {
		closeSession();
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
	public void start() {
        logConfigurator.createIndividualAppender(this.getClass().getName() + "@" + Integer.toHexString(hashCode()),
            serviceName);
		this.changeStatus(ServiceStatus.STARTING, "Service is starting", null);
		doStart();
		this.changeStatus(ServiceStatus.STARTED, "Service is started", null);
	}

	protected abstract void doStart();

	@Override
	public ServiceStatus getStatus() {
		return curStatus;
	}

	@Override
	public ISession getSession() {
		if (session == null) {
            logger.error("Session is null, method getSession returned null");
			return new FASTInvalidSession(getName());
		}
		return session;
	}

    protected ISession createSession() {
        return new FASTSession(this, this.messageHelper);
    }

	protected Context createFastContext() {
		Context context = new Context();

		context.setTemplateRegistry(getRegistry());
		return context;
	}

	protected abstract void send(Object message) throws InterruptedException;

	protected boolean isLoggedOn() {
		return false;
	}

	protected long getLastActivityTime() {
		return 0;
	}

	protected void setSettings(FASTClientSettings settings) {
		this.settings = settings;
	}

	@Override
	public FASTClientSettings getSettings() {
		return settings;
	}

	public void setRegistry(TemplateRegistry registry) {
		this.registry = registry;
	}

	public TemplateRegistry getRegistry() {
		return registry;
	}

	protected void initConnection() {
		logger.debug("initConnection");
		this.session = createSession();

		int port = getSettings().getPort();
		String remoteAddr = getSettings().getAddress();
		String interfaceAddress = getSettings().getNetworkInterface();
		logger.info("Initializing connection to {}:{} from interface {}", remoteAddr, port, interfaceAddress);

		try {
			this.connection = getConnection(remoteAddr, port, interfaceAddress);
			this.sessionClosed.set(false);
		} catch (FastConnectionException e) {
			closeSession();
			logger.error("Failed to connect to {}:{}", remoteAddr, port, e);
			throw new EPSCommonException(
					"Failed to connect to " + remoteAddr + ":" + port);
		}
		fastContext = createFastContext();
		InputStream inputStream;
		try {
			inputStream = connection.getInputStream();
			inputStream = new EofCheckedStream(inputStream);
			recordingInputStream = new RecordingInputStream(inputStream);
			inputStream = recordingInputStream;
		} catch (IOException e) {
			closeSession();
			logger.error("Failed to get input stream from connection", e);
			throw new EPSCommonException("Failed to get input stream " +
					"from multicast connection", e);
		}
		msgInStream = new FASTMessageInputStream(inputStream, fastContext);
		msgInStream.setBlockReader(getBlockReader());

		createMsgReadThread();
		logger.debug("initConnection exit");

	}

	protected abstract MessageBlockReader getBlockReader();

	protected abstract Connection getConnection(String remoteAddr, int port, String interfaceAddress) throws FastConnectionException ;

	private void createMsgReadThread() {
		thread = new Thread(new Runnable() {

			@Override
			public void run() {
				logger.debug("In the message receiving thread");

				FastToIMessageConverter converter = createConverter();
				Thread currentThread = Thread.currentThread();

				while(!currentThread.isInterrupted()) {
					Message fastMsg = null;
					try {
						logger.debug("Before reading message from stream");
						fastMsg = msgInStream.readMessage(settings.getSkipInitialByteAmount());
						logger.debug("Message read from stream :{}", fastMsg);
					} catch (Exception e) {
                        if(e.getCause() instanceof EofIOException){
                            closeSession();
                            logger.error("Exception received while reading message: ", e.getMessage());
                        } else {
                            getServiceHandler().exceptionCaught(getSession(), e);
                            logger.error("Exception received while reading message: ", e);
                        }
                        recordingInputStream.clear();
						ServiceStatus status = getStatus();
						if (
								status == ServiceStatus.DISPOSING ||
								status == ServiceStatus.DISPOSED) {
							return;
						}
						if (!recoverFromInputError(msgInStream.getUnderlyingStream())) {
							return;
						}
						continue;
					}
					if (fastMsg == null) {
						ServiceStatus status = getStatus();
						if (status == ServiceStatus.DISPOSING || status == ServiceStatus.DISPOSED) {
							return;
						}
						logger.warn("Received null message");
						continue;
					}
					byte[] rawMessage = recordingInputStream.getBuffer();
					recordingInputStream.clear();
					handleReceivedMessage(fastMsg, converter, rawMessage);
				}
			}

		});
		thread.start();
		logger.debug("Message receiving thread created and started");
	}

	protected boolean recoverFromInputError(InputStream underlyingStream) {
		return false;
	}

	protected void handleReceivedMessage(Message fastMessage, FastToIMessageConverter converter, byte[] rawMessage) {
		logger.debug("handleReceivedMessage");
		IMessage iMsg = null;
		if (!messageFilter.isMessageAcceptable(fastMessage)) {
			logger.debug("Message filtered by messageFilter: {}", fastMessage);
			return;
		}
		try {
			logger.debug("Converting FAST message");
			iMsg = converter.convert(fastMessage);

			IMessageStructure structure = dictionary.getMessageStructure(iMsg.getName());
			Boolean isAdmin = (Boolean) structure.getAttributeValueByName("isAdmin");
			if (isAdmin == null) {
				isAdmin = false;
			}

			MsgMetaData metaData = iMsg.getMetaData();
			metaData.setAdmin(isAdmin);
			metaData.setFromService(getSettings().getAddress() +  ":" + getSettings().getPort());
			metaData.setToService(getName());
			metaData.setRawMessage(rawMessage);
			metaData.setServiceInfo(serviceInfo);

			//				metaData.setFromService(session.getTargetCompID());
			logger.debug("passing message to ServericeHandler");
			if (iMsg.getMetaData().isAdmin()) {
				metaData.setAdmin(true);
				getServiceHandler().putMessage(getSession(), ServiceHandlerRoute.FROM_ADMIN, iMsg);
			} else {
				metaData.setAdmin(false);
				getServiceHandler().putMessage(getSession(), ServiceHandlerRoute.FROM_APP, iMsg);
			}
			logger.debug("message passed to ServericeHandler");

//			String humanMessage = iMsg +"," +
//			"(original fast message: " + fastMessage + ")";
			logger.debug("message passed to msgStorage");
            msgStorage.storeMessage(iMsg);

			logger.debug("message stored");
            handleIMessage(iMsg);
		} catch (ConverterException e) {
			logger.info("Conversion of FAST msg to IMessage failed\nfastMsg:{})", fastMessage, e);
			getServiceHandler().exceptionCaught(getSession(), e);
		} catch (Exception e) {
			logger.info("Caught exception while executin fromApp or fromAdmin methods of service handler\nIMessage:{})", iMsg, e);
			getServiceHandler().exceptionCaught(getSession(), e);
		}
	}

    protected void handleIMessage(IMessage iMessage){

    }

    protected synchronized void closeSession() {
        if (this.sessionClosed.compareAndSet(false, true)) {
            logger.debug("Closing session");
            if (thread != null) {
                thread.interrupt();
            }

            if (connection != null) {
                connection.close();
                connection = null;
            }
            if (msgInStream != null) {
                msgInStream.close();
                msgInStream = null;
            }

            if (thread != null) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    logger.warn("Current thread interrupted while waiting for another thread", e);
                }
                thread = null;
            }

            if (session != null) {
                session.close();
            }
            logger.debug("Session closed");
        }
    }


	@Override
	public void setServiceHandler(IServiceHandler handler) {
		throw new UnsupportedOperationException("This operation not supported for this service type");
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this).append("name", serviceName).toString();
	}

    protected Context getFastContext() {
        return fastContext;
    }

    public boolean isSessionClosed() {
        return this.sessionClosed.get();
    }

	@Override
    public IMessage receive(IActionContext actionContext, IMessage msg) throws InterruptedException {
        msg = this.messageHelper.prepareMessageToEncode(msg, null);
		return WaitAction.waitForMessage(actionContext, msg, !msg.getMetaData().isAdmin());
    }
}