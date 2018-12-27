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
package com.exactpro.sf.services.fix;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.quickfixj.CharsetSupport;
import org.quickfixj.QFJException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.aml.script.actions.WaitAction;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.services.ServiceInfo;
import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.IDataManager;
import com.exactpro.sf.configuration.ILoggingConfigurator;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
import com.exactpro.sf.configuration.workspace.WorkspaceStructureException;
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
import com.exactpro.sf.services.fix.converter.dirty.DirtyQFJIMessageConverter;
import com.exactpro.sf.services.util.ServiceUtil;
import com.exactpro.sf.storage.IMessageStorage;

import quickfix.ConfigError;
import quickfix.DataDictionary;
import quickfix.FieldConvertError;
import quickfix.FileLogFactory;
import quickfix.FileStoreFactory;
import quickfix.FileUtil;
import quickfix.Initiator;
import quickfix.Log;
import quickfix.LogFactory;
import quickfix.Message;
import quickfix.MessageFactory;
import quickfix.MessageStore;
import quickfix.MessageStoreFactory;
import quickfix.Session;
import quickfix.SessionConfigError;
import quickfix.SessionFactory;
import quickfix.SessionID;
import quickfix.SessionSettings;
import quickfix.SocketInitiator;
import quickfix.mina.SessionConnector;
import quickfix.mina.ssl.SSLSupport;

/**
 *
 * @author dmitry.guriev
 *
 */
public class FIXClient implements IInitiatorService {

	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName() + "@" + Integer.toHexString(hashCode()));

	private volatile ServiceStatus curStatus;
	private IServiceMonitor monitor;

	private static final String SessionName = "SessionName";
	public static final String ResetSeqNumFlag = "ResetSeqNumFlag_custom";
    public static final String SUPPORTS_MICROSECOND_TIMESTAMPS = "SupportsMicrosecondTimestamps";
    public static final Integer SUPPORTS_MICROSECOND_TIMESTAMPS_TAG = 8820;

	protected /*final*/ FIXClientApplication application;
	private volatile DirtyQFJIMessageConverter converter;
	private MessageHelper messageHelper;

	private FIXClientSettings fixSettings;

	private SessionSettings settings;

	private SocketInitiator initiator;
	private IServiceHandler handler;

	private FIXSession session;

	protected ServiceName serviceName;
    private ServiceInfo serviceInfo;

	private IMessageStorage msgStorage;
	private MessageFactory messageFactory = null;

    protected FixDataDictionaryProvider dictionaryProvider;

	protected ILoggingConfigurator logConfigurator;

	private boolean isPerformance = false;
	private ITaskExecutor taskExecutor;

    protected IWorkspaceDispatcher workspaceDispatcher;

    private IServiceContext serviceContext;
    private String messagesLogFile;

	public FIXClient()
	{
		this.curStatus = ServiceStatus.CREATING;

		this.handler = null;

		logger.info("init<>");

		this.session = null;

		this.curStatus = ServiceStatus.CREATED;
	}

	@Override
	public void dispose() {
		logger.info("dispose service {}", this);

        this.changeStatus(ServiceStatus.DISPOSING, "Service disposing.", null );
        this.closeResources();
        this.changeStatus(ServiceStatus.DISPOSED, "Service disposed.", null );
	}

    private void closeResources() {
        try {
            FIXSession currentSession = this.session;
            Session qfjSession = null;
            if (currentSession != null) {
                if(!currentSession.isClosed()){
                    qfjSession = Session.lookupSession(currentSession.getSessionID());
                    currentSession.close();
                }
            }

            if (this.initiator != null) {
                this.initiator.stop();
            }
            
            if (qfjSession != null) {
                MessageStore messageStore = qfjSession.getStore();
                if (messageStore instanceof Closeable) {
                    ((Closeable) messageStore).close();
                }
                Log log = qfjSession.getLog();
                if (log instanceof Closeable) {
                    ((Closeable) log).close();
                }
            }

            if (this.application != null) {
                application.stopLogging();
            }

        } catch (Throwable e)	{
            this.changeStatus(ServiceStatus.ERROR, "Service failed upon closing the resources: " + e.getMessage(), e );
            logger.error("Service failed upon closing the resources", e);
        } finally {
            if(logConfigurator != null) {
                logConfigurator.destroyIndividualAppender(this.getClass().getName() + "@" + Integer.toHexString(hashCode()),
                        serviceName);
            }
        }
    }


	@Override
	public void init(
			final IServiceContext serviceContext,
			final IServiceMonitor serviceMonitor,
			final IServiceHandler handler,
			final IServiceSettings settings,
			final ServiceName serviceName) {

		logger.info("init Service {}", this);

		try {
			this.serviceName = serviceName;

			this.serviceContext = Objects.requireNonNull(serviceContext, "'Service context' parameter");

            FixPropertiesReader.loadAndSetCharset(serviceContext);

		    this.workspaceDispatcher = Objects.requireNonNull(serviceContext.getWorkspaceDispatcher(), "'Workspace dispatcher' parameter");
			this.taskExecutor = Objects.requireNonNull(serviceContext.getTaskExecutor(), "'Task executor' parameter");
			this.monitor = Objects.requireNonNull(serviceMonitor, "'Service monitor' parameter");

			this.changeStatus(ServiceStatus.INITIALIZING, "Service initializing", null);


			this.logConfigurator = Objects.requireNonNull(this.serviceContext.getLoggingConfigurator(), "'Logging configurator' parameter");

			this.msgStorage = Objects.requireNonNull(this.serviceContext.getMessageStorage(), "'Message storage' parameter");

            serviceInfo = serviceContext.lookupService(serviceName);

            if(serviceInfo == null) {
                logger.debug("A service named {} was not found.", serviceName);
            }

			this.handler = Objects.requireNonNull(handler, "'Service handler' parameter");

			if ( settings == null )
				throw new NullPointerException("'settings' parameter is not set in " + serviceName);

			this.fixSettings = (FIXClientSettings) settings;

			SailfishURI dictionary = fixSettings.getDictionaryName();
			if (dictionary == null) {
			    throw new EPSCommonException("dictionaryURI is null");
			}

			this.dictionaryProvider = new FixDataDictionaryProvider(this.serviceContext.getDictionaryManager(), dictionary);
		    this.messageFactory = new CommonMessageFactory(dictionaryProvider);


			if(this.fixSettings.isPerformanceMode()) {
	            this.isPerformance = true;
	        }

			this.converter = new DirtyQFJIMessageConverter(this.serviceContext.getDictionaryManager().getDictionary(dictionary), this.serviceContext.getDictionaryManager().getMessageFactory(dictionary), !this.fixSettings.isAllowUnknownMsgFields(),
					this.fixSettings.isMillisecondsInTimeStampFields(),
					this.fixSettings.isMicrosecondsInTimeStampFields(),
					false,
					this.fixSettings.isOrderingFields());
			this.messageHelper = new FixMessageHelper();
			IDictionaryStructure messageDictionary = this.serviceContext.getDictionaryManager().getDictionary(fixSettings.getDictionaryName());
            this.messageHelper.init(this.serviceContext.getDictionaryManager().getMessageFactory(fixSettings.getDictionaryName()), messageDictionary);

			this.settings = new SessionSettings();

			SessionID sessionID = new SessionID( this.fixSettings.getBeginString(),
	                this.fixSettings.getSenderCompID(), this.fixSettings.getTargetCompID(), serviceName.toString().replace(':', '-'));

			this.messagesLogFile = workspaceDispatcher.createFolder(FolderType.LOGS, this.logConfigurator.getLogsPath(this.serviceName)).getCanonicalPath();
            this.settings.setString( sessionID, FileLogFactory.SETTING_FILE_LOG_PATH, this.messagesLogFile);

            this.settings.setBool( sessionID, Session.SETTING_USE_SENDER_DEFAULT_APPL_VER_ID_AS_INITIAL_TARGET, true);
            this.settings.setBool(sessionID, Session.SETTING_VALIDATE_SEQUENCE_NUMBERS, this.fixSettings.isValidateSequenceNumbers());

			configureSessionSettings(this.application, sessionID, this.settings, serviceName, this.serviceContext.getDataManager());

	        this.application = createFixApplication();
            this.application.init(serviceContext,
                    new ApplicationContext(this.monitor, this.handler, this.fixSettings, this.settings, this.messageHelper, this.converter,
                            this.dictionaryProvider, this::connectionProblem),
                    serviceName);

			this.changeStatus(ServiceStatus.INITIALIZED, "Service initialized", null);

		} catch (Exception e) {
			this.changeStatus(ServiceStatus.ERROR, "Service initializing error", e);
			throw new ServiceException(e);
		}

		logger.info("Service {} initialization finished", this);

	}

    protected void configureSessionSettings(final FIXClientApplication application, SessionID sessionID, final SessionSettings sessionSettings, final ServiceName serviceName, IDataManager dataManager)
            throws IOException, WorkspaceStructureException {

        if ( this.fixSettings.getFileStorePath() != null && !this.fixSettings.getFileStorePath().equals("") )
        	sessionSettings.setString( sessionID, FileStoreFactory.SETTING_FILE_STORE_PATH,
        			workspaceDispatcher.createFolder(FolderType.ROOT, this.fixSettings.getFileStorePath()).getCanonicalPath() );
        else
        	throw new ServiceException("'FileStorePath' parameter is not set in " + serviceName);

        sessionSettings.setBool( sessionID, Session.DUPLICATE_TAGS_ALLOWED, this.fixSettings.isDuplicateTagsAllowed() );

        sessionSettings.setLong( sessionID, Session.SETTING_MAX_LATENCY, this.fixSettings.getMaxLatency() );

        if ( this.serviceName != null)
        	sessionSettings.setString( sessionID, "SessionName", this.serviceName.toString() );
        else
        	throw new ServiceException("Service name is not set");

        sessionSettings.setString( sessionID, SessionFactory.SETTING_CONNECTION_TYPE, "initiator" );

        if ( this.fixSettings.getUsername() != null && !this.fixSettings.getUsername().equals("") )
        	sessionSettings.setString( sessionID, "Username", this.fixSettings.getUsername() );

        if ( this.fixSettings.getPassword() != null && !this.fixSettings.getPassword().equals("") )
        	sessionSettings.setString( sessionID, "Password", this.fixSettings.getPassword() );

        if ( this.fixSettings.getNewPassword() != null && !this.fixSettings.getNewPassword().equals(""))
        	sessionSettings.setString( sessionID, "NewPassword", this.fixSettings.getNewPassword() );

        if ( this.fixSettings.getEncryptionKeyFilePath() != null && !this.fixSettings.getEncryptionKeyFilePath().equals(""))
        	sessionSettings.setString( sessionID, "EncryptionKeyFilePath", this.fixSettings.getEncryptionKeyFilePath() );

        if ( this.fixSettings.getDefaultCstmApplVerID() != null && !this.fixSettings.getDefaultCstmApplVerID().equals(""))
        	sessionSettings.setString( sessionID, FIXApplication.DefaultCstmApplVerID, this.fixSettings.getDefaultCstmApplVerID() );

        if ( this.fixSettings.getExtExecInst() != null && !this.fixSettings.getExtExecInst().equals(""))
        	sessionSettings.setString( sessionID, "ExtExecInst", this.fixSettings.getExtExecInst() );

        if( this.fixSettings.isUseDefaultApplVerID() ){
        	if ( this.fixSettings.getDefaultApplVerID() != null && !this.fixSettings.getDefaultApplVerID().equals("") )
        		sessionSettings.setString( sessionID, Session.SETTING_DEFAULT_APPL_VER_ID, this.fixSettings.getDefaultApplVerID() );
        	else
        		throw new ServiceException("'"+Session.SETTING_DEFAULT_APPL_VER_ID+"' parameter is not set in " + serviceName);
        	}

        if ( this.fixSettings.getSenderCompID() != null && !this.fixSettings.getSenderCompID().equals("") )
        	sessionSettings.setString( sessionID, SessionSettings.SENDERCOMPID, this.fixSettings.getSenderCompID() );
        else
        	throw new ServiceException("'"+SessionSettings.SENDERCOMPID+"' parameter is not set  in " + serviceName);

        if ( this.fixSettings.getTargetCompID() != null && !this.fixSettings.getTargetCompID().equals("") )
        	sessionSettings.setString( sessionID, SessionSettings.TARGETCOMPID, this.fixSettings.getTargetCompID() );
        else
        	throw new ServiceException("'"+SessionSettings.TARGETCOMPID+"' parameter is not set  in " + serviceName);

        if ( this.fixSettings.getSocketConnectHost() != null && !this.fixSettings.getSocketConnectHost().equals("") )
        	sessionSettings.setString( sessionID, Initiator.SETTING_SOCKET_CONNECT_HOST, this.fixSettings.getSocketConnectHost() );
        else
        	throw new ServiceException("'"+Initiator.SETTING_SOCKET_CONNECT_HOST+"' parameter is not set  in " + serviceName);

        sessionSettings.setLong( sessionID, Initiator.SETTING_SOCKET_CONNECT_PORT, this.fixSettings.getSocketConnectPort() );
        sessionSettings.setLong( sessionID, Session.SETTING_HEARTBTINT, this.fixSettings.getHeartBtInt() );
        sessionSettings.setLong( sessionID, Session.SETTING_LOGON_TIMEOUT, this.fixSettings.getLogonTimeout() );
        sessionSettings.setLong( sessionID, Session.SETTING_LOGOUT_TIMEOUT, this.fixSettings.getLogoutTimeout() );
        sessionSettings.setLong(sessionID, Initiator.SETTING_RECONNECT_INTERVAL, this.fixSettings.getReconnectInterval());

        if ( this.fixSettings.getStartDate() != null && !this.fixSettings.getStartDate().equals("") ) {
            sessionSettings.setString(sessionID, Session.SETTING_START_DAY, this.fixSettings.getStartDate());
        }

        if ( this.fixSettings.getEndDate() != null && !this.fixSettings.getEndDate().equals("") ) {
            sessionSettings.setString(sessionID, Session.SETTING_END_DAY, this.fixSettings.getEndDate());
        }

        if ( this.fixSettings.getStartTime() != null && !this.fixSettings.getStartTime().equals("") )
        	sessionSettings.setString( sessionID, Session.SETTING_START_TIME, this.fixSettings.getStartTime());
        else
        	throw new ServiceException("'"+Session.SETTING_START_TIME+"' parameter is not set in " + serviceName);

        if ( this.fixSettings.getEndTime() != null && !this.fixSettings.getEndTime().equals("") )
        	sessionSettings.setString( sessionID, Session.SETTING_END_TIME, this.fixSettings.getEndTime());
        else
        	throw new ServiceException("'"+Session.SETTING_END_TIME+"' parameter is not set in " + serviceName);

        sessionSettings.setBool(sessionID, Session.SETTING_MILLISECONDS_IN_TIMESTAMP,
        		this.fixSettings.isMillisecondsInTimeStampFields());
        sessionSettings.setBool(sessionID, Session.SETTING_MICROSECONDS_IN_TIMESTAMP,
                this.fixSettings.isMicrosecondsInTimeStampFields());

        sessionSettings.setBool(sessionID, Session.SETTING_CHECK_LATENCY, this.fixSettings.isCheckLatency());
        sessionSettings.setString(sessionID, ResetSeqNumFlag, this.fixSettings.getResetSeqNumFlag()); // Determines if sequence numbers should be reset when recieving a logon request. Acceptors only.
        sessionSettings.setBool( sessionID, Session.SETTING_VALIDATE_USER_DEFINED_FIELDS, this.fixSettings.isValidateUserDefinedFields() );
        sessionSettings.setBool( sessionID, Session.SETTING_VALIDATE_FIELDS_OUT_OF_ORDER, this.fixSettings.isValidateFieldsOutOfOrder() );
        sessionSettings.setBool( sessionID, Session.SETTING_VALIDATE_FIELDS_HAVE_VALUES, this.fixSettings.isValidateFieldsHaveValues() );
        sessionSettings.setBool( sessionID, Session.SETTING_ALLOW_UNKNOWN_MSG_FIELDS, this.fixSettings.isAllowUnknownMsgFields() );
        sessionSettings.setBool(sessionID, Session.IGNORE_ABSENCE_OF_141_TAG, this.fixSettings.isIgnoreAbsenceOf141tag());
        sessionSettings.setLong(sessionID, Session.RECEIVE_LIMIT, this.fixSettings.getReceiveLimit());
        sessionSettings.setBool( sessionID, Session.SETTING_RESET_ON_LOGOUT, this.fixSettings.isResetOnLogout());
        sessionSettings.setBool( sessionID, Session.SETTING_RESET_ON_DISCONNECT, this.fixSettings.isResetOnDisconnect());
        sessionSettings.setBool( sessionID, Session.SETTING_REQUIRES_ORIG_SENDING_TIME, this.fixSettings.isRequiresOrigSendingTime());

        sessionSettings.setBool( sessionID, FIXApplication.ENCRYPT_PASSWORD, this.fixSettings.isEncryptPassword());
        sessionSettings.setBool( sessionID, FIXApplication.ADD_NEXT_EXPECTED_SEQ_NUM, this.fixSettings.isAddNextExpectedMsgSeqNum());
        sessionSettings.setBool(sessionID, Session.SETTING_REJECT_INVALID_MESSAGE,this.fixSettings.isRejectInvalidMessage());

        if (this.fixSettings.isUseSSL()) {
            sessionSettings.setBool(SSLSupport.SETTING_USE_SSL, this.fixSettings.isUseSSL());
            if (this.fixSettings.getSslKeyStore() != null) {
                sessionSettings.setString(SSLSupport.SETTING_KEY_STORE_NAME, this.fixSettings.getSslKeyStore()); //TODO: File from DataManager
                sessionSettings.setString(SSLSupport.SETTING_KEY_STORE_PWD, this.fixSettings.getSslKeyStorePassword());
            }
            if (this.fixSettings.getSslEnabledProtocols() != null) {
                sessionSettings.setString(SSLSupport.SETTING_ENABLE_PROTOCOLE, this.fixSettings.getSslEnabledProtocols());
            }
            if (this.fixSettings.getSslCipherSuites() != null) {
                sessionSettings.setString(SSLSupport.SETTING_CIPHER_SUITES, this.fixSettings.getSslCipherSuites());
            }
        }


        if(fixSettings.isUseLocalTime()) {
        	sessionSettings.setString(sessionID, Session.SETTING_TIMEZONE, TimeZone.getDefault().getID());
        }
        sessionSettings.setBool( sessionID, Session.SETTING_VALIDATE_FIELDS_OUT_OF_RANGE,
            this.fixSettings.isValidateFieldsOutOfRange());

        IDictionaryStructure dictionaryStructure = dictionaryProvider.getDictionaryStructure();
        IMessageStructure logonStructure = dictionaryStructure.getMessageStructure(FixMessageHelper.LOGON_MESSAGE);
        boolean sendSupportsMicros = logonStructure.getFields().stream()
            .map(fieldStructure -> fieldStructure.getAttributeValueByName(FixMessageHelper.ATTRIBUTE_TAG))
            .filter(SUPPORTS_MICROSECOND_TIMESTAMPS_TAG::equals)
            .findFirst()
            .isPresent();
        
        sessionSettings.setBool( sessionID, SUPPORTS_MICROSECOND_TIMESTAMPS, sendSupportsMicros);
    }

    protected FIXClientApplication createFixApplication() {
        return new FIXApplication();
    }

	@Override
	public void start() {
        logConfigurator.createIndividualAppender(this.getClass().getName() + "@" + Integer.toHexString(hashCode()),
            serviceName);
        application.startLogging();
		logger.info("start service {}", this);

		try{

			this.changeStatus(ServiceStatus.STARTING, "Service starting", null);

			MessageStoreFactory messageStoreFactory = new FileStoreFactory(this.settings);

            LogFactory logFactory = new FIXLogFactory(new FileLogFactory(this.settings), this.fixSettings.isLogHeartbeats(), this, monitor, this.logger);

			FixSessionFactory sessionFactory = new FixSessionFactory(application, messageStoreFactory, logFactory,
					messageFactory, dictionaryProvider);

			this.initiator = new SocketInitiator(sessionFactory, this.settings, SessionConnector.DEFAULT_QUEUE_CAPACITY,
					taskExecutor.getThreadPool());

			if (this.initiator == null) {
				throw new ServiceException("'initiator' is not set");
			}

			if (this.fixSettings.isDoLogonOnStart()) {
				logon();
			}

            this.changeStatus(ServiceStatus.STARTED, "Service started", null);

		} catch ( QFJException e ) {

			logger.error("Service {} not started", this, e);

			String cause = null;

			if ( e.getCause() != null  )
				cause = e.getCause().getMessage();
			else
				cause = e.getMessage();

			this.changeStatus(ServiceStatus.ERROR, cause, e, true);
			throw new ServiceException("Exception during service starting", e);

		} catch (SessionConfigError e ) {

			String message = "Exception during service starting";

			if(e.getSessionField().equals(Session.SETTING_DEFAULT_APPL_VER_ID)){
				message += ".Please make sure that flag useDefaultApplVerID=true, and field DefaultApplVerID contain the value";
			}

			configErrorHandler(message, e);

		} catch ( ConfigError e ) {

			configErrorHandler("Exception during service starting", e);

		} catch (Exception e) {

			logger.error("not started", e);
			this.changeStatus(ServiceStatus.ERROR, "Service start error", e, true);
			throw new ServiceException("Exception during service starting", e);
		}

		logger.info("Service {} started", this);

	}

	private void configErrorHandler(String message, ConfigError e){

		logger.error("not started", e);

		String cause = null;

		if ( e.getCause() != null  )
			cause = e.getCause().getMessage();
		else
			cause = e.getMessage();

		this.changeStatus(ServiceStatus.ERROR, cause, e, true);
		throw new ServiceException(message, e);
	}

	public synchronized void logon() throws ConfigError, FieldConvertError {

		SessionSettings settings = this.initiator.getSettings();
		logger.debug("settings = {}", settings);

		logger.info("initiator.start");
		this.initiator.start();
		int size = this.initiator.getSessions().size();
		if (size != 1) {
			throw new EPSCommonException("Config file should contain only one session, but "+size);
		}
		Iterator<SessionID> iter = this.initiator.getSessions().iterator();

		while (iter.hasNext())	{
			SessionID sessionID = iter.next();
			FIXSession iSession = new FIXSession(this.serviceName.toString(), sessionID, msgStorage, this.converter, this.messageHelper);
			String sessionName = settings.getString(sessionID, SessionName);
			iSession.setServiceInfo(serviceInfo);
			logger.info("register session: {}", sessionName);
			this.session = iSession;
			this.application.addSessionId(sessionID, iSession);
		}
	}

	@Override
	public IServiceHandler getServiceHandler() {
		return this.handler;
	}

	@Override
	public ISession getSession() {
		return this.session;
	}

	@Override
	public String getName() {
		return serviceName.toString();
	}

	@Override
	public ServiceName getServiceName() {
		return serviceName;
	}

    protected void changeStatus(ServiceStatus status, String message, Throwable e) {
        changeStatus(status, message, e, false);
    }

    protected void changeStatus(ServiceStatus status, String message, Throwable e, boolean closeResources) {
        this.curStatus = status;
        ServiceUtil.changeStatus(this, monitor, status, message, e);
        if (closeResources) {
            closeResources();
        }
    }

	@Override
	public ServiceStatus getStatus() {
		return this.curStatus;
	}

	public void disconnect() {
		if ( this.initiator != null ) {
			logger.info("disconnect");
			this.initiator.stop();
		}
	}

	public void forcedDisconnect() {
		if ( this.initiator != null ) {
			logger.info("forced.disconnect");
			this.initiator.stop(true);
		}
	}

	@Override
	public void setServiceHandler(IServiceHandler handler) {

		throw new ServiceException("Unsupported operation");

	}

	@Override
	public IServiceSettings getSettings() {

		try {
		    if (this.fixSettings != null) {
    			//return copy of the settings to prevent it's change
    			return (IServiceSettings) BeanUtils.cloneBean(this.fixSettings);
		    } else {
		        return null;
		    }
		} catch (Exception e) {
			logger.error("Could not copy settings object", e);
			throw new EPSCommonException("Could not copy settings object", e);
		}
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this).append("name", serviceName).toString();
	}

	public SocketInitiator getInitiator() {
		return initiator;
	}

	public boolean isPerformance() {
		return isPerformance;
	}

	@Override
	public void connect() throws Exception {
		logon();
	}

	@Override
    public IMessage receive(IActionContext actionContext, IMessage msg) throws InterruptedException {
        return WaitAction.waitForMessage(actionContext, this.messageHelper.prepareMessageToEncode(msg, null), !msg.getMetaData().isAdmin());
    }

    public DirtyQFJIMessageConverter getConverter() {
        return converter;
    }

    public List<IMessage> retrieve(long beginSeq, long endSeq) throws Exception {
        SessionID sessionID = ((FIXSession)session).getSessionID();
        String sessionName = FileUtil.sessionIdFileName(sessionID);
        String logPath = FileUtil.fileAppendPath(messagesLogFile, sessionName) + ".messages.log";

        String sender = "\u000149=" + sessionID.getTargetCompID() + "\u0001";
        List<IMessage> messages = new ArrayList<>();

        try(FileInputStream fis = new FileInputStream(logPath);
            InputStreamReader isr = new InputStreamReader(fis, CharsetSupport.getCharset());
            BufferedReader reader = new BufferedReader(isr)) {

            DataDictionary dictionary = dictionaryProvider.getSessionDataDictionary(sessionID.getBeginString());
            String line = null;
            while ((line = reader.readLine()) != null) {
                try {
                    if (line.contains(sender)) {
                        int seqNum = Integer.valueOf(StringUtils.substringBetween(line, "\u000134=", "\u0001"));
                        if (seqNum >= beginSeq && seqNum <= endSeq) {
                            Message message = new Message(line, dictionary);
                            IMessage convertedMessage = converter.convert(message, null, null);

                            ServiceHandlerRoute route = message.isAdmin()
                                    ? ServiceHandlerRoute.FROM_ADMIN
                                    : ServiceHandlerRoute.FROM_APP;

                            handler.putMessage(session, route, convertedMessage);
                            messages.add(convertedMessage);
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Problem during conversion fix message [{}] to IMessage", line, e);
                }
            }
        }

        return messages;
    }

	protected MessageHelper getMessageHelper() {
		return this.messageHelper;
	}

	protected void setMessageHelper (MessageHelper messageHelper) {
		this.messageHelper = messageHelper;
	}

    private void connectionProblem(boolean isPresent, String reason) {
        if (this.curStatus == ServiceStatus.ERROR) {
            return;
        }

        if (isPresent && this.curStatus != ServiceStatus.WARNING) {
            changeStatus(ServiceStatus.WARNING, reason, null);
        } else if (!isPresent && this.curStatus == ServiceStatus.WARNING) {
            changeStatus(ServiceStatus.STARTED, reason, null);
        }
    }
}