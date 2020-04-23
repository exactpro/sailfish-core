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

import static com.exactpro.sf.common.messages.structures.StructureUtils.getAttributeValue;

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
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.services.ServiceInfo;
import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.IDataManager;
import com.exactpro.sf.configuration.IDictionaryManager;
import com.exactpro.sf.configuration.ILoggingConfigurator;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.configuration.workspace.FolderType;
import com.exactpro.sf.configuration.workspace.IWorkspaceDispatcher;
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
import com.exactpro.sf.services.fix.converter.QFJIMessageConverterSettings;
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

    protected final Logger logger = LoggerFactory.getLogger(ILoggingConfigurator.getLoggerName(this));

	protected volatile ServiceStatus curStatus;
	protected IServiceMonitor monitor;

	private static final String SessionName = "SessionName";
	public static final String ResetSeqNumFlag = "ResetSeqNumFlag_custom";
    public static final String SUPPORTS_MICROSECOND_TIMESTAMPS = "SupportsMicrosecondTimestamps";
    public static final Integer SUPPORTS_MICROSECOND_TIMESTAMPS_TAG = 8820;

	protected /*final*/ FIXClientApplication application;
	protected volatile DirtyQFJIMessageConverter converter;
	protected MessageHelper messageHelper;

	private FIXClientSettings fixSettings;

	protected SessionSettings settings;

	private SocketInitiator initiator;
	protected IServiceHandler handler;

	private FIXSession session;

	protected ServiceName serviceName;
    protected ServiceInfo serviceInfo;

	private IMessageStorage msgStorage;
    private MessageFactory messageFactory;

    protected FixDataDictionaryProvider dictionaryProvider;

	protected ILoggingConfigurator logConfigurator;

    private boolean isPerformance;
	protected ITaskExecutor taskExecutor;

    protected IWorkspaceDispatcher workspaceDispatcher;

    protected IServiceContext serviceContext;
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

        changeStatus(ServiceStatus.DISPOSING, "Service disposing.", null);
        closeResources();
        changeStatus(ServiceStatus.DISPOSED, "Service disposed.", null);
	}

    private void closeResources() {
        try {
            FIXSession currentSession = session;
            Session qfjSession = null;
            if (currentSession != null) {
                if(!currentSession.isClosed()){
                    qfjSession = Session.lookupSession(currentSession.getSessionID());
                    currentSession.close();
                }
            }

            if(initiator != null) {
                initiator.stop();
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

            if(application != null) {
                application.stopLogging();
            }

        } catch (Exception e)	{
            changeStatus(ServiceStatus.ERROR, "Service failed upon closing the resources: " + e.getMessage(), e);
            logger.error("Service failed upon closing the resources", e);
        } finally {
            if(logConfigurator != null) {
                logConfigurator.destroyAppender(getServiceName());
            }
        }
    }


	@Override
	public void init(
            IServiceContext serviceContext,
            IServiceMonitor serviceMonitor,
            IServiceHandler handler,
            IServiceSettings settings,
            ServiceName serviceName) {

		logger.info("init Service {}", this);

		try {
            changeStatus(ServiceStatus.INITIALIZING, "Service initializing", null);

            baseInit(serviceContext, serviceMonitor, handler, settings, serviceName);

            initFixApplication();

            changeStatus(ServiceStatus.INITIALIZED, "Service initialized", null);

		} catch (Exception e) {
            changeStatus(ServiceStatus.ERROR, "Service initializing error", e);
			throw new ServiceException(e);
		}

		logger.info("Service {} initialization finished", this);

	}

    protected void configureSessionSettings(FIXClientApplication application, SessionID sessionID, SessionSettings sessionSettings, ServiceName serviceName, IDataManager dataManager)
            throws IOException {

        configureCommonSettings(fixSettings, sessionID, sessionSettings, serviceName);

        if(this.serviceName != null) {
            sessionSettings.setString(sessionID, "SessionName", this.serviceName.toString());
        } else {
            throw new ServiceException("Service name is not set");
        }

        sessionSettings.setString( sessionID, SessionFactory.SETTING_CONNECTION_TYPE, "initiator" );

        if(fixSettings.getUsername() != null && !"".equals(fixSettings.getUsername())) {
            sessionSettings.setString(sessionID, "Username", fixSettings.getUsername());
        }

        if(fixSettings.getPassword() != null && !"".equals(fixSettings.getPassword())) {
            sessionSettings.setString(sessionID, "Password", fixSettings.getPassword());
        }

        if(fixSettings.getNewPassword() != null && !"".equals(fixSettings.getNewPassword())) {
            sessionSettings.setString(sessionID, "NewPassword", fixSettings.getNewPassword());
        }

        if(fixSettings.getEncryptionKeyFilePath() != null && !"".equals(fixSettings.getEncryptionKeyFilePath())) {
            sessionSettings.setString(sessionID, "EncryptionKeyFilePath", fixSettings.getEncryptionKeyFilePath());
        }

        if(fixSettings.getDefaultCstmApplVerID() != null && !"".equals(fixSettings.getDefaultCstmApplVerID())) {
            sessionSettings.setString(sessionID, FIXApplication.DefaultCstmApplVerID, fixSettings.getDefaultCstmApplVerID());
        }

        if(fixSettings.getExtExecInst() != null && !"".equals(fixSettings.getExtExecInst())) {
            sessionSettings.setString(sessionID, "ExtExecInst", fixSettings.getExtExecInst());
        }

        if(fixSettings.isUseDefaultApplVerID()) {
            if(fixSettings.getDefaultApplVerID() != null && !"".equals(fixSettings.getDefaultApplVerID())) {
                sessionSettings.setString(sessionID, Session.SETTING_DEFAULT_APPL_VER_ID, fixSettings.getDefaultApplVerID());
            } else {
                throw new ServiceException("'" + Session.SETTING_DEFAULT_APPL_VER_ID + "' parameter is not set in " + serviceName);
            }
        	}

        if(fixSettings.getSocketConnectHost() != null && !"".equals(fixSettings.getSocketConnectHost())) {
            sessionSettings.setString(sessionID, Initiator.SETTING_SOCKET_CONNECT_HOST, fixSettings.getSocketConnectHost());
        } else {
            throw new ServiceException("'" + Initiator.SETTING_SOCKET_CONNECT_HOST + "' parameter is not set  in " + serviceName);
        }

        sessionSettings.setLong(sessionID, Initiator.SETTING_SOCKET_CONNECT_PORT, fixSettings.getSocketConnectPort());
        sessionSettings.setLong(sessionID, Session.SETTING_HEARTBTINT, fixSettings.getHeartBtInt());
        sessionSettings.setLong(sessionID, Session.SETTING_LOGON_TIMEOUT, fixSettings.getLogonTimeout());
        sessionSettings.setLong(sessionID, Session.SETTING_LOGOUT_TIMEOUT, fixSettings.getLogoutTimeout());
        sessionSettings.setLong(sessionID, Initiator.SETTING_RECONNECT_INTERVAL, fixSettings.getReconnectInterval());

        if(fixSettings.getStartDate() != null && !"".equals(fixSettings.getStartDate())) {
            sessionSettings.setString(sessionID, Session.SETTING_START_DAY, fixSettings.getStartDate());
        }

        if(fixSettings.getEndDate() != null && !"".equals(fixSettings.getEndDate())) {
            sessionSettings.setString(sessionID, Session.SETTING_END_DAY, fixSettings.getEndDate());
        }

        sessionSettings.setString(sessionID, ResetSeqNumFlag, fixSettings.getResetSeqNumFlag()); // Determines if sequence numbers should be reset when recieving a logon request. Acceptors only.

        sessionSettings.setBool(sessionID, Session.IGNORE_ABSENCE_OF_141_TAG, fixSettings.isIgnoreAbsenceOf141tag());
        sessionSettings.setBool(sessionID, Session.SETTING_REQUIRES_ORIG_SENDING_TIME, fixSettings.isRequiresOrigSendingTime());

        sessionSettings.setBool(sessionID, FIXApplication.ENCRYPT_PASSWORD, fixSettings.isEncryptPassword());
        sessionSettings.setBool(sessionID, FIXApplication.ADD_NEXT_EXPECTED_SEQ_NUM, fixSettings.isAddNextExpectedMsgSeqNum());

        IDictionaryStructure dictionaryStructure = dictionaryProvider.getDictionaryStructure();
        IMessageStructure logonStructure = dictionaryStructure.getMessages().get(FixMessageHelper.LOGON_MESSAGE);
        boolean sendSupportsMicros = logonStructure.getFields().values().stream()
                .map(fieldStructure -> getAttributeValue(fieldStructure, FixMessageHelper.ATTRIBUTE_TAG))
                .anyMatch(SUPPORTS_MICROSECOND_TIMESTAMPS_TAG::equals);
        
        sessionSettings.setBool( sessionID, SUPPORTS_MICROSECOND_TIMESTAMPS, sendSupportsMicros);
    }

    protected void configureCommonSettings(FIXCommonSettings commonSettings, SessionID sessionID,
                                           SessionSettings sessionSettings, ServiceName serviceName)
            throws IOException {

        checkSetting(FileStoreFactory.SETTING_FILE_STORE_PATH, commonSettings.getFileStorePath(), serviceName);
        sessionSettings.setString( sessionID, FileStoreFactory.SETTING_FILE_STORE_PATH,
                workspaceDispatcher.createFolder(FolderType.ROOT, commonSettings.getFileStorePath()).getCanonicalPath() );

        setSenderTargetIDs(sessionID, commonSettings, sessionSettings);

        checkSetting(Session.SETTING_START_TIME, commonSettings.getStartTime(), serviceName);
        sessionSettings.setString(sessionID, Session.SETTING_START_TIME, commonSettings.getStartTime());
        checkSetting(Session.SETTING_END_TIME, commonSettings.getEndTime(), serviceName);
        sessionSettings.setString(sessionID, Session.SETTING_END_TIME, commonSettings.getEndTime());

        sessionSettings.setBool(sessionID, Session.SETTING_MILLISECONDS_IN_TIMESTAMP,
                commonSettings.isMillisecondsInTimeStampFields());
        sessionSettings.setBool(sessionID, Session.SETTING_MICROSECONDS_IN_TIMESTAMP,
                commonSettings.isMicrosecondsInTimeStampFields());

        sessionSettings.setLong(sessionID, Session.RECEIVE_LIMIT, commonSettings.getReceiveLimit());
        sessionSettings.setBool(sessionID, Session.SETTING_RESET_ON_LOGOUT, commonSettings.isResetOnLogout());
        sessionSettings.setBool(sessionID, Session.SETTING_RESET_ON_DISCONNECT, commonSettings.isResetOnDisconnect());
        sessionSettings.setBool(sessionID, Session.SETTING_CHECK_LATENCY, commonSettings.isCheckLatency());
        sessionSettings.setLong(sessionID, Session.SETTING_MAX_LATENCY, commonSettings.getMaxLatency());

        sessionSettings.setBool(sessionID, Session.SETTING_VALIDATE_USER_DEFINED_FIELDS, commonSettings.isValidateUserDefinedFields() );
        sessionSettings.setBool(sessionID, Session.SETTING_VALIDATE_FIELDS_OUT_OF_ORDER, commonSettings.isValidateFieldsOutOfOrder() );
        sessionSettings.setBool(sessionID, Session.SETTING_VALIDATE_FIELDS_HAVE_VALUES, commonSettings.isValidateFieldsHaveValues() );
        sessionSettings.setBool(sessionID, FIXApplication.REQUIRED_TAGS, commonSettings.isCheckRequiredTags());
        sessionSettings.setBool(sessionID, Session.SETTING_ALLOW_UNKNOWN_MSG_FIELDS, commonSettings.isAllowUnknownMsgFields() );
        sessionSettings.setBool(sessionID, Session.DUPLICATE_TAGS_ALLOWED, commonSettings.isDuplicateTagsAllowed() );
        sessionSettings.setBool(sessionID, Session.SETTING_REJECT_INVALID_MESSAGE,commonSettings.isRejectInvalidMessage());
        sessionSettings.setBool(sessionID, Session.SETTING_USE_SENDER_DEFAULT_APPL_VER_ID_AS_INITIAL_TARGET, true);
        sessionSettings.setBool(sessionID, Session.SETTING_REJECT_MESSAGE_ON_UNHANDLED_EXCEPTION, true);
        sessionSettings.setBool(sessionID, Session.SETTING_VALIDATE_SEQUENCE_NUMBERS, commonSettings.isValidateSequenceNumbers());

        if (commonSettings.isUseSSL()) {
            sessionSettings.setBool(SSLSupport.SETTING_USE_SSL, commonSettings.isUseSSL());
            if (commonSettings.getSslKeyStore() != null) {
                sessionSettings.setString(SSLSupport.SETTING_KEY_STORE_NAME, commonSettings.getSslKeyStore()); //TODO: File from DataManager
                sessionSettings.setString(SSLSupport.SETTING_KEY_STORE_PWD, commonSettings.getSslKeyStorePassword());
            }
            if (commonSettings.getSslEnabledProtocols() != null) {
                sessionSettings.setString(SSLSupport.SETTING_ENABLE_PROTOCOLE, commonSettings.getSslEnabledProtocols());
            }
            if (commonSettings.getSslCipherSuites() != null) {
                sessionSettings.setString(SSLSupport.SETTING_CIPHER_SUITES, commonSettings.getSslCipherSuites());
            }
        }
        if(commonSettings.isUseLocalTime()) {
            sessionSettings.setString(sessionID, Session.SETTING_TIMEZONE, TimeZone.getDefault().getID());
        }
        sessionSettings.setBool( sessionID, Session.SETTING_VALIDATE_FIELDS_OUT_OF_RANGE,
                commonSettings.isValidateFieldsOutOfRange());
    }

    protected void baseInit(
            IServiceContext serviceContext,
            IServiceMonitor serviceMonitor,
            IServiceHandler handler,
            IServiceSettings serviceSettings,
            ServiceName serviceName) {
        this.serviceName = serviceName;
        this.serviceContext = Objects.requireNonNull(serviceContext, "'Service context' parameter");

        FixPropertiesReader.loadAndSetCharset(serviceContext);

        this.workspaceDispatcher = Objects.requireNonNull(serviceContext.getWorkspaceDispatcher(), "'Workspace dispatcher' parameter");
        this.taskExecutor = Objects.requireNonNull(serviceContext.getTaskExecutor(), "'Task executor' parameter");
        this.monitor = Objects.requireNonNull(serviceMonitor, "'Service monitor' parameter");

        this.logConfigurator = Objects.requireNonNull(this.serviceContext.getLoggingConfigurator(), "'Logging configurator' parameter");
        this.msgStorage = Objects.requireNonNull(this.serviceContext.getMessageStorage(), "'Message storage' parameter");
        this.serviceInfo = serviceContext.lookupService(serviceName);

        if(serviceInfo == null) {
            logger.debug("A service named {} was not found.", serviceName);
        }
        this.handler = Objects.requireNonNull(handler, "'Service handler' parameter");

        setSettings(serviceSettings);
        this.settings = new SessionSettings();

        SailfishURI dictionaryName = Objects.requireNonNull(serviceSettings.getDictionaryName(), "dictionaryName is null");
        FIXCommonSettings commonSettings = (FIXCommonSettings) serviceSettings;
        IDictionaryManager dictionaryManager = this.serviceContext.getDictionaryManager();
        IDictionaryStructure dictionary = dictionaryManager.getDictionary(dictionaryName);

        IMessageFactory messageFactory = dictionaryManager.getMessageFactory(dictionaryName);

        this.dictionaryProvider = new FixDataDictionaryProvider(dictionaryManager, dictionaryName);
        QFJIMessageConverterSettings settings = new QFJIMessageConverterSettings(dictionary, messageFactory)
                .setVerifyTags(!commonSettings.isAllowUnknownMsgFields())
                .setIncludeMilliseconds(commonSettings.isMillisecondsInTimeStampFields())
                .setIncludeMicroseconds(commonSettings.isMicrosecondsInTimeStampFields())
                .setOrderingFields(commonSettings.isOrderingFields());

        this.converter = new DirtyQFJIMessageConverter(settings);
        this.messageHelper = new FixMessageHelper();
        messageHelper.init(messageFactory, dictionary);
    }


    protected void initFixApplication() throws IOException {
        this.messageFactory = new CommonMessageFactory(dictionaryProvider);

        if(fixSettings.isPerformanceMode()) {
            this.isPerformance = true;
        }
        SessionID sessionID = new SessionID(fixSettings.getBeginString(),
                fixSettings.getSenderCompID(), fixSettings.getSenderSubID(), SessionID.NOT_SET,
                fixSettings.getTargetCompID(), fixSettings.getTargetSubID(), SessionID.NOT_SET,
                serviceName.toString().replace(':', '-'));

        this.messagesLogFile = workspaceDispatcher.createFolder(FolderType.LOGS, logConfigurator.getLogsPath(serviceName)).getCanonicalPath();
        settings.setString(sessionID, FileLogFactory.SETTING_FILE_LOG_PATH, messagesLogFile);

        configureSessionSettings(application, sessionID, settings, serviceName, serviceContext.getDataManager());

        this.application = createFixApplication();
        application.init(serviceContext,
                new ApplicationContext(monitor, handler, fixSettings, settings, messageHelper, converter,
                        dictionaryProvider, this::connectionProblem),
                serviceName);
    }

    protected void setSettings(IServiceSettings settings) {
        this.fixSettings = (FIXClientSettings) Objects.requireNonNull(settings,"'settings' parameter is not set in " + serviceName);
    }

    protected FIXClientApplication createFixApplication() {
        return new FIXApplication();
    }



    protected void setSenderTargetIDs(SessionID sessionID, FIXCommonSettings commonSettings, SessionSettings sessionSettings) {
        checkSetting(SessionSettings.SENDERCOMPID, commonSettings.getSenderCompID(), serviceName);
        sessionSettings.setString(sessionID, SessionSettings.SENDERCOMPID, commonSettings.getSenderCompID());
        checkSetting(SessionSettings.TARGETCOMPID, commonSettings.getTargetCompID(), serviceName);
        sessionSettings.setString(sessionID, SessionSettings.TARGETCOMPID, commonSettings.getTargetCompID());
    }

	@Override
	public void start() {
        logConfigurator.createAndRegister(getServiceName(), this);
        application.startLogging();
		logger.info("start service {}", this);

		try{

            changeStatus(ServiceStatus.STARTING, "Service starting", null);

            MessageStoreFactory messageStoreFactory = new FileStoreFactory(settings);

            LogFactory logFactory = new FIXLogFactory(new FileLogFactory(settings), fixSettings.isLogHeartbeats(), this, monitor, logger);

			FixSessionFactory sessionFactory = new FixSessionFactory(application, messageStoreFactory, logFactory,
					messageFactory, dictionaryProvider);

            this.initiator = new SocketInitiator(sessionFactory, settings, SessionConnector.DEFAULT_QUEUE_CAPACITY,
					taskExecutor.getThreadPool());

            if(initiator == null) {
				throw new ServiceException("'initiator' is not set");
			}

            if(fixSettings.isDoLogonOnStart()) {
				logon();
			}

            changeStatus(ServiceStatus.STARTED, "Service started", null);

		} catch ( QFJException e ) {

			logger.error("Service {} not started", this, e);

            String cause = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();

            changeStatus(ServiceStatus.ERROR, cause, e, true);
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
            changeStatus(ServiceStatus.ERROR, "Service start error", e, true);
			throw new ServiceException("Exception during service starting", e);
		}

		logger.info("Service {} started", this);

	}

	private void configErrorHandler(String message, ConfigError e){

		logger.error("not started", e);

        String cause = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();

        changeStatus(ServiceStatus.ERROR, cause, e, true);
		throw new ServiceException(message, e);
	}

	public synchronized void logon() throws ConfigError, FieldConvertError {

        SessionSettings settings = initiator.getSettings();
		logger.debug("settings = {}", settings);

		logger.info("initiator.start");
        initiator.start();
        int size = initiator.getSessions().size();
		if (size != 1) {
			throw new EPSCommonException("Config file should contain only one session, but "+size);
		}
        Iterator<SessionID> iter = initiator.getSessions().iterator();

		while (iter.hasNext())	{
			SessionID sessionID = iter.next();
            FIXSession iSession = new FIXSession(serviceName.toString(), sessionID, msgStorage, converter, messageHelper);
			String sessionName = settings.getString(sessionID, SessionName);
			iSession.setServiceInfo(serviceInfo);
			logger.info("register session: {}", sessionName);
			this.session = iSession;
            application.addSessionId(sessionID, iSession);
		}
	}

	@Override
	public IServiceHandler getServiceHandler() {
        return handler;
	}

	@Override
	public ISession getSession() {
        return session;
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
        return curStatus;
	}

	public void disconnect() {
        if(initiator != null) {
			logger.info("disconnect");
            initiator.stop();
		}
	}

	public void forcedDisconnect() {
        if(initiator != null) {
			logger.info("forced.disconnect");
            initiator.stop(true);
		}
	}

	@Override
	public void setServiceHandler(IServiceHandler handler) {

		throw new ServiceException("Unsupported operation");

	}

	@Override
	public IServiceSettings getSettings() {

		try {
            //return copy of the settings to prevent it's change
            return fixSettings != null ? (IServiceSettings)BeanUtils.cloneBean(fixSettings) : null;
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
        return WaitAction.waitForMessage(actionContext, messageHelper.prepareMessageToEncode(msg, null), !msg.getMetaData().isAdmin());
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
            return messages;
        }

    }

    protected MessageHelper getMessageHelper() {
        return messageHelper;
	}

	protected void setMessageHelper (MessageHelper messageHelper) {
		this.messageHelper = messageHelper;
	}

    private void connectionProblem(boolean isPresent, String reason) {
        if(curStatus == ServiceStatus.ERROR) {
            return;
        }

        if(isPresent && curStatus != ServiceStatus.WARNING) {
            changeStatus(ServiceStatus.WARNING, reason, null);
        } else if(!isPresent && curStatus == ServiceStatus.WARNING) {
            changeStatus(ServiceStatus.STARTED, reason, null);
        }
    }

    private void checkSetting(String settingName, String value, ServiceName serviceName) {
        if (StringUtils.isEmpty(value)) {
            throw new ServiceException(String.format("'%s' parameter is not set in %s", settingName, serviceName));
        }
    }
}
