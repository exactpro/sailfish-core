/*******************************************************************************
 * Copyright 2009-2020 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.services.fix;

import static quickfix.Acceptor.SETTING_ACCEPTOR_TEMPLATE;
import static quickfix.Acceptor.SETTING_SOCKET_ACCEPT_ADDRESS;
import static quickfix.Acceptor.SETTING_SOCKET_ACCEPT_PORT;
import static quickfix.mina.acceptor.DynamicAcceptorSessionProvider.WILDCARD;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.service.spi.ServiceException;

import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.services.IAcceptorService;
import com.exactpro.sf.services.IServiceHandler;
import com.exactpro.sf.services.IServiceSettings;
import com.exactpro.sf.services.ISession;
import com.exactpro.sf.services.ServiceStatus;
import com.exactpro.sf.services.fix.converter.dirty.DirtyQFJIMessageConverter;
import com.exactpro.sf.services.util.ServiceUtil;

import quickfix.Application;
import quickfix.ConfigError;
import quickfix.DataDictionaryProvider;
import quickfix.DefaultMessageFactory;
import quickfix.FieldConvertError;
import quickfix.FileLogFactory;
import quickfix.FileStoreFactory;
import quickfix.LogFactory;
import quickfix.MessageFactory;
import quickfix.MessageStoreFactory;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionSettings;
import quickfix.SocketAcceptor;
import quickfix.mina.acceptor.DynamicAcceptorSessionProvider.TemplateMapping;

public class FIXServer extends FIXClient implements IAcceptorService {
	private final Map<InetSocketAddress, List<TemplateMapping>> dynamicSessionMappings = new HashMap<>();

	protected FIXServerApplication application;

	private FIXServerSettings fixSettings;

	private List<ISession> sessions;
	private SocketAcceptor acceptor;
    private ISession fixServerSessionsContainer;

	public FIXServer() {
		this.sessions = new ArrayList<>();
		this.curStatus = ServiceStatus.CREATED;
	}

	@Override
	public void dispose() {

        changeStatus(ServiceStatus.DISPOSING, "Service disposing.", null );


		try {
			if (acceptor != null) {
				// rm 35322 - send logout and disconnect immediately
				if(fixSettings.isForceDisconnectByDispose()) {
					for (SessionID sessionID : acceptor.getSessions()) {
						Session.lookupSession(sessionID).generateLogout();
					}
                    acceptor.stop(true);
				} else {
                    acceptor.stop();
				}
			}
            changeStatus(ServiceStatus.DISPOSED, "Service disposed.", null );
		} catch (Exception e) {
            changeStatus(ServiceStatus.DISPOSED, "Logout failed: " + e.getMessage(), e );
			logger.error(e.getMessage(), e);
		}

		if(logConfigurator != null) {
			logConfigurator.destroyAppender(getServiceName());
		}
		if (application != null) {
		    application.stopLogging();
		}

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
	public IServiceHandler getServiceHandler() {
		return handler;
	}

	@Override
	public List<ISession> getSessions() {
		this.sessions = application.getSessions();
		return sessions;
	}

    @Override
    protected void setSettings(IServiceSettings settings) {
        this.fixSettings = (FIXServerSettings) settings;
    }

    @Override
    protected void initFixApplication() throws IOException {
        SessionID sessionID = new SessionID(fixSettings.getBeginString(), WILDCARD, WILDCARD, WILDCARD, WILDCARD);

        configureCommonSettings(fixSettings, sessionID, settings, serviceName);

        settings.setBool(sessionID, SETTING_ACCEPTOR_TEMPLATE, true);
        settings.setBool(sessionID, Session.SETTING_CHECK_COMP_ID, false);

        settings.setString("SessionName", serviceName.toString());

        setString( "ConnectionType", "acceptor" );
        setString(  "FileStorePath", serviceContext.getWorkspaceDispatcher().createFolder(FolderType.ROOT, fixSettings.getFileStorePath()).getCanonicalPath() );
        setString( FileLogFactory.SETTING_FILE_LOG_PATH, workspaceDispatcher.createFolder(FolderType.LOGS,
                logConfigurator.getLogsPath(serviceName)).getCanonicalPath() );
        settings.setBool( "FileLogHeartbeats", fixSettings.isLogHeartbeats() );

        if (fixSettings.getDefaultApplVerID() != null) {
            setString( "DefaultApplVerID", fixSettings.getDefaultApplVerID() );
        }

        settings.setLong("SocketAcceptPort", fixSettings.getSocketAcceptPort() );
        settings.setBool(Session.SETTING_RESET_ON_LOGON, fixSettings.isResetOnLogon());
        settings.setBool("UseDataDictionary", fixSettings.isUseDataDictionary() );

        settings.setLong(FileStoreFactory.SETTING_FILE_STORE_MAX_CACHED_MSGS, 0);

        setString("ApplicationClassName", fixSettings.getApplicationClassName());

        this.application = createApplication(fixSettings.getApplicationClass());
        application.init(serviceContext,
                new ApplicationContext(monitor, handler, fixSettings, settings, messageHelper, converter, dictionaryProvider),
                serviceName);

        Objects.requireNonNull(application, "FixApplication cant be null");
        fixServerSessionsContainer = application.getServerSession();
    }

    @Override
    protected void setSenderTargetIDs(SessionID sessionID, FIXCommonSettings commonSettings, SessionSettings sessionSettings) {
        setString(SessionSettings.SENDERCOMPID, WILDCARD);
        setString(SessionSettings.SENDERSUBID, WILDCARD);
        setString(SessionSettings.TARGETCOMPID, WILDCARD);
        setString(SessionSettings.TARGETSUBID, WILDCARD);
    }

    private void setString(String name, String value) {
		if (value == null) {
			throw new NullPointerException("Parameter '"+name+"' is required.");
		}
        settings.setString(name, value);
	}


	private void configureDynamicSessions(SessionSettings settings, Application application,
			MessageStoreFactory messageStoreFactory, LogFactory logFactory,
			MessageFactory messageFactory,
			DataDictionaryProvider dictionaryProvider) throws ConfigError, FieldConvertError {
		//
		// If a session template is detected in the settings, then
		// set up a dynamic session provider.
		//

		Iterator<SessionID> sectionIterator = settings.sectionIterator();
		while (sectionIterator.hasNext()) {
			SessionID sessionID = sectionIterator.next();
			if (isSessionTemplate(settings, sessionID)) {
				InetSocketAddress address = getAcceptorSocketAddress(settings, sessionID);
				getMappings(address).add(new TemplateMapping(sessionID, sessionID));
			}
		}

		for (Entry<InetSocketAddress, List<TemplateMapping>> entry : dynamicSessionMappings
				.entrySet()) {
			acceptor.setSessionProvider(entry.getKey(), new FIXAcceptorSessionProvider(
					settings, entry.getValue(), application, messageStoreFactory, logFactory,
					messageFactory, dictionaryProvider));
		}
	}

	private List<TemplateMapping> getMappings(InetSocketAddress address) {
		List<TemplateMapping> mappings = dynamicSessionMappings.get(address);
		if (mappings == null) {
			mappings = new ArrayList<>();
			dynamicSessionMappings.put(address, mappings);
		}
		return mappings;
	}

	private InetSocketAddress getAcceptorSocketAddress(SessionSettings settings, SessionID sessionID)
	throws ConfigError, FieldConvertError {
		String acceptorHost = "0.0.0.0";
		if (settings.isSetting(sessionID, SETTING_SOCKET_ACCEPT_ADDRESS)) {
			acceptorHost = settings.getString(sessionID, SETTING_SOCKET_ACCEPT_ADDRESS);
		}
		int acceptorPort = (int) settings.getLong(sessionID, SETTING_SOCKET_ACCEPT_PORT);

		InetSocketAddress address = new InetSocketAddress(acceptorHost, acceptorPort);
		return address;
	}

	private boolean isSessionTemplate(SessionSettings settings, SessionID sessionID)
	throws ConfigError, FieldConvertError {
		return settings.isSetting(sessionID, SETTING_ACCEPTOR_TEMPLATE)
		&& settings.getBool(sessionID, SETTING_ACCEPTOR_TEMPLATE);
	}


	@Override
	public void start() {
        logConfigurator.createAndRegister(getServiceName(), this);
        application.startLogging();
		try {

            changeStatus(ServiceStatus.STARTING, "service starting", null);

			MessageStoreFactory messageStoreFactory = new FileStoreFactory(settings);
			LogFactory logFactory = new FIXLogFactory(new FileLogFactory(settings), fixSettings.isLogHeartbeats(), this, monitor, logger) ;
			MessageFactory messageFactory = new DefaultMessageFactory("com.exactpro.sf");
			acceptor = new SocketAcceptor(application, messageStoreFactory, settings, logFactory, messageFactory, taskExecutor.getThreadPool());
			configureDynamicSessions(settings, application, messageStoreFactory, logFactory, messageFactory, dictionaryProvider);
			acceptor.start();

            changeStatus(ServiceStatus.STARTED, "service started", null);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			changeStatus(ServiceStatus.ERROR, e.getMessage(), e);
		}
	}

	protected FIXServerApplication createApplication(Class<?> applicationClass) {

        if (applicationClass == null) {
            throw new NullPointerException("Application class should not be null.");
        }
        if (FIXServerApplication.class.isAssignableFrom(applicationClass)) {
            try {
                return (FIXServerApplication) applicationClass.newInstance();
            } catch (Exception e) {
                throw new EPSCommonException("Cannot create new instance for application: " + applicationClass.getCanonicalName(), e);
            }
        } else {
            throw new EPSCommonException("Application class '" + applicationClass.getCanonicalName() + "' should implement interface: "
                    + FIXServerApplication.class.getCanonicalName());
        }
	}


	@Override
	public ServiceStatus getStatus() {
		return curStatus;
	}

	@Override
    protected void changeStatus(ServiceStatus status, String message, Throwable e) {
		this.curStatus = status;
		ServiceUtil.changeStatus(this, monitor, status, message, e);
	}

	@Override
    public void disconnect() {
		for (ISession session : sessions) {
			session.close();
		}
	}


	@Override
	public void setServiceHandler(IServiceHandler handler) {

		throw new ServiceException("Unsupported operation");

	}

	@Override
	public IServiceSettings getSettings() {
		return fixSettings;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this).append("name", serviceName).toString();
	}

	@Override
    public DirtyQFJIMessageConverter getConverter() {
        return converter;
    }

	@Override
	public void connect() throws Exception {
		logger.warn("FIXServer does not have to connect to clients");
	}

	@Override
	public ISession getSession() {
        return fixServerSessionsContainer;
	}
}
