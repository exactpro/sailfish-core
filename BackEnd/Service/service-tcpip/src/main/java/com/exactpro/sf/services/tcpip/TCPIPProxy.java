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
package com.exactpro.sf.services.tcpip;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.aml.script.actions.WaitAction;
import com.exactpro.sf.common.codecs.AbstractCodec;
import com.exactpro.sf.common.codecs.CodecFactory;
import com.exactpro.sf.common.impl.messages.DefaultMessageFactory;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.services.ServiceInfo;
import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.IDataManager;
import com.exactpro.sf.configuration.IDictionaryManager;
import com.exactpro.sf.configuration.ILoggingConfigurator;
import com.exactpro.sf.configuration.Rules;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.scriptrunner.ScriptRunException;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.IActionContext;
import com.exactpro.sf.services.IInitiatorService;
import com.exactpro.sf.services.IServiceContext;
import com.exactpro.sf.services.IServiceHandler;
import com.exactpro.sf.services.IServiceMonitor;
import com.exactpro.sf.services.IServiceSettings;
import com.exactpro.sf.services.ISession;
import com.exactpro.sf.services.ITaskExecutor;
import com.exactpro.sf.services.ServiceException;
import com.exactpro.sf.services.ServiceStatus;
import com.exactpro.sf.services.WrapperNioSocketAcceptor;
import com.exactpro.sf.services.WrapperNioSocketConnector;
import com.exactpro.sf.services.mina.MINASession;
import com.exactpro.sf.services.util.ServiceUtil;
import com.exactpro.sf.storage.IMessageStorage;

public abstract class TCPIPProxy implements IInitiatorService
{
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName() + "@" + Integer.toHexString(hashCode()));

	private Rules rules;
	private volatile ServiceStatus curStatus;
	protected TCPIPProxySettings settings;

	protected IMessageStorage storage;
	protected WrapperNioSocketConnector connector;
	protected IServiceHandler handler;
	protected ILoggingConfigurator logConfigurator;

	private ServiceName serviceName;
    private ServiceInfo serviceInfo;
	private WrapperNioSocketAcceptor acceptor;
	private IoSession session;
	private boolean haveConnection = false;
	private Class<? extends AbstractCodec> codecClass;
	private IServiceMonitor monitor;
	private Map<IoSession, MINASession> sessions = Collections.synchronizedMap(new HashMap<IoSession, MINASession>());
	private IServiceContext serviceContext;
	private ITaskExecutor taskExecutor;
	private IDataManager dataManager;
    protected IDictionaryStructure dictionary;
    protected IMessageFactory factory;

	public TCPIPProxy()
	{
		curStatus = ServiceStatus.CREATED;

		this.settings = null;

		this.session = null;

		this.handler = null;

		this.codecClass = null;

		this.rules = null;

	}

	@Override
	public void init(IServiceContext serviceContext,
			final IServiceMonitor serviceMonitor,
			final IServiceHandler handler,
			final IServiceSettings serviceSettings,
			final ServiceName serviceName)
	{
		try {
			changeStatus(ServiceStatus.INITIALIZING, "Service initializing", null);

			this.serviceName = Objects.requireNonNull(serviceName, "'Service name' parameter");

			this.serviceContext = Objects.requireNonNull(serviceContext, "'Service context' parameter");

			this.storage = Objects.requireNonNull(this.serviceContext.getMessageStorage(), "'Message storage' parameter");

			this.handler = Objects.requireNonNull(handler, "'Service handler' parameter");

			if (serviceSettings == null)
				throw new NullPointerException("settings");

			if (serviceSettings instanceof TCPIPProxySettings)
				this.settings = (TCPIPProxySettings) serviceSettings;
			else
				throw new ServiceException("Incorrect class of settings has been passed to init " + serviceSettings.getClass());

			this.monitor = serviceMonitor;

			this.logConfigurator = Objects.requireNonNull(this.serviceContext.getLoggingConfigurator(), "'Logging configurator' parameter");

			this.dataManager = Objects.requireNonNull(this.serviceContext.getDataManager(), "'Data manager' parameter");

			this.taskExecutor = this.serviceContext.getTaskExecutor();

            this.serviceInfo = serviceContext.lookupService(serviceName);

			if(StringUtils.isEmpty(settings.getCodecClassName())){
				throw new NullPointerException("Codec class name");
			}

			try {
                this.codecClass = this.getClass().getClassLoader().loadClass(this.settings.getCodecClassName()).asSubclass(AbstractCodec.class);
			} catch (ClassNotFoundException e) {
				changeStatus(ServiceStatus.ERROR, "Error while init", e);
				logger.error(e.getMessage(), e);
				throw new ScriptRunException("Could not find codec class [" + settings.getCodecClassName() + "]", e);
			}

			if ((settings.isChangeTags()) && (settings.getRulesAlias() != null)) {
				Unmarshaller u = null;
				JAXBContext jc = null;

				try {

					jc = JAXBContext.newInstance(new Class[]{Rules.class});
					u = jc.createUnmarshaller();
					InputStream rulesAliasIS = dataManager.getDataInputStream(settings.getRulesAlias());
					JAXBElement<Rules> root = u.unmarshal(new StreamSource(rulesAliasIS), Rules.class);
					this.rules = root.getValue();

				} catch (JAXBException e) {
					logger.error(e.getMessage(), e);
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
				}
			}

            SailfishURI dictionaryName = Objects.requireNonNull(this.settings.getDictionaryName(), "dictionary name cannot be null");

            this.dictionary = this.serviceContext.getDictionaryManager().getDictionary(dictionaryName);
            this.factory = this.serviceContext.getDictionaryManager().getMessageFactory(dictionaryName);

            internalInit(serviceName, this.serviceContext.getDictionaryManager(), handler, serviceSettings, storage, serviceMonitor, this.logConfigurator, taskExecutor, dataManager);

			changeStatus(ServiceStatus.INITIALIZED, "Service initialized", null);
		} catch (RuntimeException e){
			changeStatus(ServiceStatus.ERROR, "Error while initialize", e);
			throw e;
		}

	}

    public void reinit(IServiceSettings serviceSettings) {
		TCPIPProxySettings newSettings;
		if (serviceSettings instanceof TCPIPProxySettings) {
			newSettings = (TCPIPProxySettings) serviceSettings;
		} else {
			throw new ServiceException("Incorrect class of settings has been passed to init " + serviceSettings.getClass());
		}

		if ((newSettings.isChangeTags()) && (newSettings.getRulesAlias() != null)) {
			try {
				JAXBContext jc = JAXBContext.newInstance(new Class[]{Rules.class});
				Unmarshaller u = jc.createUnmarshaller();
				InputStream rulesAliasIS = dataManager.getDataInputStream(newSettings.getRulesAlias());
				JAXBElement<Rules> root = u.unmarshal(new StreamSource(rulesAliasIS),Rules.class);
				this.rules = root.getValue();
			} catch (Exception e) {
				disconnect();
				dispose();
				changeStatus(ServiceStatus.ERROR, "Error while reiniting", e);
				throw new EPSCommonException(e);
			}
		}
	}

    protected abstract void internalInit(final ServiceName serviceName,
            final IDictionaryManager dictionaryManager,
            final IServiceHandler handler,
            final IServiceSettings settings,
            final IMessageStorage storage,
            final IServiceMonitor serviceMonitor,
            final ILoggingConfigurator logConfigurator,
            final ITaskExecutor taskExecutor,
            final IDataManager dataManager);

	@Override
	public void start() {

        logConfigurator.createIndividualAppender(this.getClass().getName() + "@" + Integer.toHexString(hashCode()),
                serviceName);

		changeStatus(ServiceStatus.STARTING, "Service starting", null);

		this.connector = new WrapperNioSocketConnector(taskExecutor);
		this.acceptor = new WrapperNioSocketAcceptor(taskExecutor);

		connector.setConnectTimeoutMillis(this.settings.getTimeout());
		IMessageFactory msgFactory = DefaultMessageFactory.getFactory();

		CodecFactory codecFactory = new CodecFactory(this.serviceContext, msgFactory, null, this.codecClass, settings);

		this.connector.getFilterChain().addLast("codec", new ProtocolCodecFilter(codecFactory));
		this.acceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(codecFactory));

		IProxyIoHandler clientSideIoHandler = getProxyIoHandler();
        clientSideIoHandler.setServiceInfo(serviceInfo);
		acceptor.setHandler(clientSideIoHandler);

		try {
			connect(0);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			changeStatus(ServiceStatus.ERROR, "Error while starting", e);
		}

		changeStatus(ServiceStatus.STARTED, "Service started", null);

	}

	protected  abstract IProxyIoHandler getProxyIoHandler();

	@Override
	public String getName() {
		return serviceName.toString();
	}

	@Override
	public ServiceName getServiceName() {
		return serviceName;
	}

	@Override
	public void dispose() {

		changeStatus(ServiceStatus.DISPOSING, "Service disposing", null);

		if (this.connector != null) {
			this.connector.dispose();
			this.connector = null;
		}
		if (this.acceptor != null) {
			this.acceptor.dispose();
			this.acceptor = null;
		}

		changeStatus(ServiceStatus.DISPOSED, "Service disposed", null);

		if(logConfigurator != null) {
            logConfigurator.destroyIndividualAppender(this.getClass().getName() + "@" + Integer.toHexString(hashCode()),
                serviceName);
		}

	}


	@Override
	public IServiceHandler getServiceHandler()
	{
		return this.handler;
	}


	@Override
	public ISession getSession() {

		//List<ISession> list = new ArrayList<ISession>();

		//for (ISession session : this.sessions.values()) {
		//	list.add(session);
		//}
		if (sessions.size() > 0)
			return sessions.get(session);

		return null;
	}

	public ISession getSession(IoSession key) {
		return sessions.get(key);
	}

	public boolean connect(long timeOut) throws IOException
	{
		if (this.haveConnection) {
			return true;
		}

		this.acceptor.bind(new InetSocketAddress(this.settings.getListenPort()));

		this.haveConnection = true;
		return this.haveConnection;
	}

	public boolean disconnect()
	{
		if ( this.haveConnection )
		{
			this.session.close(true);
			this.haveConnection = false;
		}
		return true;
	}

	@Override
	public ServiceStatus getStatus()
	{
		return this.curStatus;
	}

	protected void changeStatus(ServiceStatus status, String message, Throwable e) {
		this.curStatus = status;
		ServiceUtil.changeStatus(this, monitor, status, message, e);
	}

	public void addSession(IoSession key, MINASession value)
	{
		this.session = key;
		this.sessions.put(key, value);
	}

	public Rules getRules() {
		return rules;
	}

	public void setRules(Rules rules) {
		this.rules = rules;
	}

	@Override
    public TCPIPProxySettings getSettings() {
		return settings;
	}

	public void setSettings(TCPIPProxySettings settings) {
		this.settings = settings;
	}

	@Override
	public void setServiceHandler(IServiceHandler handler) {
		// TODO Auto-generated method stub

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
		return (IMessage) WaitAction.waitForMessage(actionContext, msg, !msg.getMetaData().isAdmin());
    }
}
