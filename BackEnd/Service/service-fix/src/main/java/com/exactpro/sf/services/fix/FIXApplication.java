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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.crypto.Cipher;

import org.apache.mina.util.Base64;
import org.quickfixj.CharsetSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.actions.FIXMatrixUtil;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.MsgMetaData;
import com.exactpro.sf.common.services.ServiceInfo;
import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.common.util.StringUtil;
import com.exactpro.sf.configuration.ILoggingConfigurator;
import com.exactpro.sf.services.IServiceContext;
import com.exactpro.sf.services.IServiceHandler;
import com.exactpro.sf.services.IServiceMonitor;
import com.exactpro.sf.services.ISession;
import com.exactpro.sf.services.ServiceEvent;
import com.exactpro.sf.services.ServiceEventFactory;
import com.exactpro.sf.services.ServiceHandlerException;
import com.exactpro.sf.services.ServiceHandlerRoute;
import com.exactpro.sf.services.fix.converter.MessageConvertException;
import com.exactpro.sf.services.fix.converter.dirty.DirtyQFJIMessageConverter;
import com.exactpro.sf.storage.IMessageStorage;

import quickfix.DoNotSend;
import quickfix.FieldNotFound;
import quickfix.IncorrectDataFormat;
import quickfix.IncorrectTagValue;
import quickfix.Message;
import quickfix.MessageFactory;
import quickfix.RejectLogon;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionSettings;
import quickfix.UnsupportedMessageType;
import quickfix.field.BeginSeqNo;
import quickfix.field.BeginString;
import quickfix.field.DefaultApplVerID;
import quickfix.field.EndSeqNo;
import quickfix.field.MsgSeqNum;
import quickfix.field.MsgType;
import quickfix.field.ResetSeqNumFlag;
import quickfix.field.Text;

public class FIXApplication implements FIXClientApplication {

	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName() + "@" + Integer.toHexString(hashCode()));

	public static final String ENCRYPT_PASSWORD = "EncryptPassword";
	public static final String ADD_NEXT_EXPECTED_SEQ_NUM = "AddNextExpectedMsgSeqNum";
	public static final String DefaultCstmApplVerID = "DefaultCstmApplVerID";
	private static final String Username = "Username";
	private static final String Password = "Password";
	private static final String NewPassword = "NewPassword";
	private static final String ENCRYPTION_KEY_FILE_PATH = "EncryptionKeyFilePath";
//	private static final String NEXT_EXPECTED_SEQ_NUM = "NextExpectedMsgSeqNum";


	private static final String ExtExecInst = "ExtExecInst";
	private static final String SEND_APP_REJECT = "SEND_APP_REJECT";
	private static final String SEND_ADMIN_REJECT = "SEND_ADMIN_REJECT";
    private String serviceStringName;
    private ServiceName serviceName;
    private ApplicationContext applicationContext;
    private ILoggingConfigurator logConfigurator;
    private IServiceMonitor serviceMonitor;
    private ServiceInfo serviceInfo;
	private IServiceHandler handler;
	private volatile DirtyQFJIMessageConverter converter;

	private final Map<SessionID, ISession> sessionMap;
	private IMessageStorage storage;
	private SessionSettings settings;
	private boolean sendAppReject = true;
	private boolean sendAdminReject = true;

	private Integer seqNumSender;
	private Integer seqNumTarget;
	private boolean incorrectSenderMsgSeqNum = false; // update seqnum on logon
	private boolean incorrectTargetMsgSeqNum = false; // update seqnum on logon

	private boolean useDefaultApplVerID = false;

	private boolean isPerformance = false;

	private boolean autorelogin = true;

    public FIXApplication() {
        this.sessionMap = new HashMap<>();
    }

    @Override
    public void init(IServiceContext serviceContext, ApplicationContext applicationContext, ServiceName serviceName) {
        this.applicationContext = Objects.requireNonNull(applicationContext, "Application context can't be null");
        FIXClientSettings fixSettings = (FIXClientSettings) this.applicationContext.getServiceSettings();
        this.serviceStringName = serviceName.toString();
        this.serviceName = serviceName;
        this.logConfigurator = serviceContext.getLoggingConfigurator();
        this.serviceMonitor = this.applicationContext.getServiceMonitor();
        this.handler = this.applicationContext.getServiceHandler();
        this.converter = this.applicationContext.getConverter();
        this.storage = serviceContext.getMessageStorage();
        this.serviceInfo = serviceContext.lookupService(serviceName);
        this.settings = this.applicationContext.getSessionSettings();
        if (this.settings.isSetting(SEND_APP_REJECT)) {
            try {
                this.sendAppReject = this.settings.getBool(SEND_APP_REJECT);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
        if (this.settings.isSetting(SEND_ADMIN_REJECT)) {
            try {
                this.sendAdminReject = this.settings.getBool(SEND_ADMIN_REJECT);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }

        this.autorelogin = fixSettings.isAutorelogin();

        Integer seqNumSender = fixSettings.getSeqNumSender();
        Integer seqNumTarget = fixSettings.getSeqNumTarget();

        if (seqNumSender != null && !seqNumSender.equals(0)) {
            this.seqNumSender = seqNumSender;
        }
        if (seqNumTarget != null && !seqNumTarget.equals(0)) {
            this.seqNumTarget = seqNumTarget;
        }

        this.useDefaultApplVerID = fixSettings.isUseDefaultApplVerID();
        this.isPerformance = fixSettings.isPerformanceMode();
    }

    @Override
    public void fromAdmin(Message message, SessionID sessionID) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
        logger.debug("fromAdmin: {}", message);
        processMessage(sessionID, message, ServiceHandlerRoute.FROM_ADMIN, sessionID.getTargetCompID(), serviceStringName, true);

		Session session = Session.lookupSession(sessionID);

		String msgType = getMessageType(message);
		MsgSeqNum msgSeqNum = new MsgSeqNum();
		message.getHeader().getField(msgSeqNum);
		logger.debug("message seqnum vs expected: {}:{}", msgSeqNum, session.getExpectedTargetNum());

        if(MsgType.REJECT.equals(msgType)){
            if (message.isSetField(Text.FIELD)) {
                String rejectText = message.getString(Text.FIELD);
                if (rejectText.startsWith("Wrong sequence number")) {
                    if (!StringUtil.containsAll(rejectText, "Received:", "Expected:")) {
                        logger.info("Trying to change session.setNextSenderMsgSeqNum, but text (58 tag) have unsupported format.");
                        return;
                    }
                    try {
                        String expectedSeqNum = rejectText.split("Expected:")[1].replaceAll("[\\D]", "");
                        this.seqNumSender = Integer.parseInt(expectedSeqNum);
                        session.setNextSenderMsgSeqNum(seqNumSender);
                        logger.info("Set session.setNextSenderMsgSeqNum = {}", seqNumSender);
                    } catch (NumberFormatException | IOException e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
        } else if (MsgType.LOGOUT.equals(msgType))
		{
            String textMessage = "Received Logout doesn't have text (58 tag)";
			if (message.isSetField(Text.FIELD))
			{
				logger.info("Logout received - {}", message);

				int seqNum = -1;
				int targSeq = -1;
				String text = message.getString(Text.FIELD);
                textMessage = "Received Logout has text (58) tag: " + text;

				if (StringUtil.containsAll(text, "MsgSeqNum", "too low, expecting")
				        || StringUtil.containsAll(text, "Wrong sequence number!", "Too small to recover. Received: ", "Expected: ", ">."))
				{
					incorrectSenderMsgSeqNum = true;
					// extract 4 from the text: MsgSeqNum too low, expecting 4 but received 1
                    try {
                        seqNum = FIXMatrixUtil.extractSeqNum(text);
                    } catch (NumberFormatException e) {
                        logger.error(e.getMessage(), e);
                    }
					// DG: experimentally checked
					// only here set next seq num as seqMum-1.
					//seqNum = seqNum-1; // nikolay.antonov : It is seems doesn't works.
					targSeq = message.getHeader().getInt(MsgSeqNum.FIELD);
				}
				else if (text.startsWith("Error ! Expecting : "))
				{
					incorrectTargetMsgSeqNum = true;
					// extract 1282 from the text: Error ! Expecting : 1282 but received : 1281
					seqNum = Integer.parseInt(text.split(" ")[4]);
					targSeq = message.getHeader().getInt(MsgSeqNum.FIELD);
				}else if (text.startsWith("Negative gap for the user"))
				{
					incorrectSenderMsgSeqNum = true;
					String num = text.substring(
							text.lastIndexOf('[') + 1,
							text.lastIndexOf(']') );
					seqNum = Integer.parseInt(num);

					//incorrectTargetMsgSeqNum = true; // TODO
					targSeq = message.getHeader().getInt(MsgSeqNum.FIELD); // TODO: +1 ?
				}

				if (seqNum != -1)
					this.seqNumSender = new Integer(seqNum);

				if (targSeq != -1)
					this.seqNumTarget = new Integer(targSeq);
			}
            if (serviceMonitor != null) {
                ServiceEvent event = ServiceEventFactory.createEventInfo(serviceName,
                        ServiceEvent.Type.INFO, textMessage, null);
                serviceMonitor.onEvent(event);
            }
		} else if (MsgType.RESEND_REQUEST.equals(msgType)) {
			MessageFactory messageFactory = session.getMessageFactory();
			String beginString = sessionID.getBeginString();

			// send heartbeats
			int savedSeqNum = session.getExpectedSenderNum();

			int beginSeqNo = message.getInt(BeginSeqNo.FIELD);
			int endSeqNo = message.getInt(EndSeqNo.FIELD);

			if (endSeqNo == 0) {
				endSeqNo = savedSeqNum-1;
			}

			try {
				for (int i=beginSeqNo; i<=endSeqNo; i++) {
					session.getSessionState().getMessageStore().refresh(); // reopen files // possible race condition ?
					session.setNextSenderMsgSeqNum(i);
					Message heartbeat = messageFactory.create(beginString, MsgType.HEARTBEAT);
					session.send(heartbeat);
				}
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			}
//		} else if (MsgType.TEST_REQUEST.equals(msgType)) {
//			MessageFactory messageFactory = session.getMessageFactory();
//			String beginString = sessionID.getBeginString();
//
//			// send heartbeats as response for TestRequest
//
//			Message heartbeat = messageFactory.create(beginString, MsgType.TEST_REQUEST);
//			session.send(heartbeat);
		}
	}


	@Override
    public void fromApp(Message message, SessionID sessionID)
            throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        logger.debug("fromApp: {}", message);
        processMessage(sessionID, message, ServiceHandlerRoute.FROM_APP,  sessionID.getTargetCompID(), serviceStringName, false);
	}

	@Override
	public void onMessageRejected(Message message, SessionID sessionID) {
		logger.debug("onMessageRejected: {}", message);
		try {
			IMessage iMsg = convert(message, sessionID.getTargetCompID(), this.serviceStringName, message.isAdmin(), false, true);
			iMsg.getMetaData().setRejected(true);
            if (!isPerformance) {
                storeMessage(sessionID, iMsg);
            }
		    // We don't call IServiceHandler here because this message is invalid. They shouldn't reach comparator
        } catch(MessageConvertException | RuntimeException e) {
			// don't throw it to QFJ
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	public void onCreate(SessionID sessionID)
	{
		ISession iSession = sessionMap.get(sessionID);

		Session session = Session.lookupSession(sessionID);

		try {
			if (this.seqNumSender != null) {
				session.setNextSenderMsgSeqNum(this.seqNumSender);
				logger.info("Set session.setNextSenderMsgSeqNum = {}", seqNumSender);
			}
		} catch (IOException e) {
			logger.error("Could not set specified both seqNumSender={} for the session {}.",
			        this.seqNumSender, iSession.getName(), e);
		}

		try {
            if (this.seqNumTarget != null) {
                session.setNextTargetMsgSeqNum(this.seqNumTarget);
                logger.info("Set session.setNextTargetMsgSeqNum = {}", seqNumTarget);
            }
        } catch (IOException e) {
            logger.error("Could not set specified both seqNumTarget={} for the session {}.",
                    this.seqNumTarget, iSession.getName(), e);
        }

		if (handler == null) {
			logger.error("Service initialization did not complete.");
		} else {
    		try {
    			handler.sessionOpened(iSession);
    		} catch (Exception e) {
    			handler.exceptionCaught(iSession, e);
                logger.error("onCreate: can not do handler.sessionOpened({}) ", iSession, e);
    		}
		}
	}

	@Override
	public void onLogon(SessionID sessionID) {
        applicationContext.connectionProblem(false, "logon successful");
		logger.info("onLogon({} -> {})", sessionID.getSenderCompID(), sessionID.getTargetCompID());
	}

	@Override
	public void onLogout(SessionID sessionID)
	{
		logger.info("onLogout({} -> {})", sessionID.getSenderCompID(), sessionID.getTargetCompID());
		ISession iSession = sessionMap.get(sessionID);
		try {
			handler.sessionClosed(iSession);
		} catch (Exception e) {
			handler.exceptionCaught(iSession, e);
            logger.error("onLogout: can not do handler.sessionClosed({}) ", iSession, e);
		}
		if (!this.autorelogin)
		{
			Session session = Session.lookupSession(sessionID);
			session.logout("Logon After Server Logout");
		}

	}

	@Override
	public void toAdmin(Message message, SessionID sessionID) throws DoNotSend {
	    logger.debug("toAdmin: {}", message);

		String msgType = getMessageType(message);

		try {
			if (MsgType.LOGON.equals(msgType))
			{
				Session session = Session.lookupSession(sessionID);
				session.getSessionState().getMessageStore().refresh();

				String userName = (String) settings.getSessionProperties(sessionID).get(Username);
				String keyFile = (String) settings.getSessionProperties(sessionID).get(ENCRYPTION_KEY_FILE_PATH);
				String settingsPassword = (String) settings.getSessionProperties(sessionID).get(Password);
				String settingsNewPassword = (String) settings.getSessionProperties(sessionID).get(NewPassword);
				String encryptStr = settings.getSessionProperties(sessionID).get(ENCRYPT_PASSWORD).toString();
				String password = null;
				String newPassword = null;
				if(encryptStr.equalsIgnoreCase("Y")) {
				    PublicKey publicKey = null;
				    ObjectInputStream inputStream = null;
				    try {
				        inputStream = new ObjectInputStream(new FileInputStream(keyFile));
				        publicKey = (PublicKey) inputStream.readObject();
				    } finally {
				        if (inputStream != null) {
				            inputStream.close();
				        }
				    }
					Cipher cipher = Cipher.getInstance("RSA");
					cipher.init(Cipher.ENCRYPT_MODE, publicKey);
					byte[] encryptedPasswordBytes = null;
					encryptedPasswordBytes = cipher.doFinal(settingsPassword.getBytes());
					password = new String(Base64.encodeBase64(encryptedPasswordBytes));
					if(settingsNewPassword != null) {
						byte[] encryptedNewPasswordBytes = null;
						encryptedNewPasswordBytes = cipher.doFinal(settingsNewPassword.getBytes());
						newPassword = new String(Base64.encodeBase64(encryptedNewPasswordBytes));
					}
				} else {
					password = settingsPassword;
					newPassword = settingsNewPassword;
				}
				String defaultCstmApplVerID = (String) settings.getSessionProperties(sessionID).get(DefaultCstmApplVerID);

				String defaultApplVerID=null;
				String beginString = message.getHeader().getString(BeginString.FIELD);
				if ( useDefaultApplVerID &&
						("FIX.4.4".equals(beginString) || "FIX.4.2".equals(beginString))
						) {
					defaultApplVerID = (String) settings.getSessionProperties(sessionID).get(Session.SETTING_DEFAULT_APPL_VER_ID);

					message.setString(DefaultApplVerID.FIELD, defaultApplVerID);
				}

				String extExecInst = (String) settings.getSessionProperties(sessionID).get(ExtExecInst);
				Object resetSeqNumFlag = settings.getSessionProperties(sessionID).get(FIXClient.ResetSeqNumFlag);
				String addNextExpectedSeqNum = (String) settings.getSessionProperties(sessionID).get(ADD_NEXT_EXPECTED_SEQ_NUM);
				int nextExpectedSeqNum = Session.lookupSession(sessionID).getSessionState().getNextTargetMsgSeqNum();

				if (userName != null)
					message.setString(quickfix.field.Username.FIELD, userName);
				if (password != null)
					message.setString(quickfix.field.Password.FIELD, password);
				if (newPassword != null)
					message.setString(quickfix.field.NewPassword.FIELD, newPassword);
				if (defaultCstmApplVerID != null)
                    message.setString(1408, defaultCstmApplVerID);
				if (extExecInst != null)
                    message.setString(8718, extExecInst);
				if (resetSeqNumFlag != null)
				{
					String sResetSeqNumFlag = ((String)resetSeqNumFlag).toLowerCase();
					if (sResetSeqNumFlag.equals("true") || sResetSeqNumFlag.equals("y"))
						message.setBoolean(ResetSeqNumFlag.FIELD, true);
					if (sResetSeqNumFlag.equals("false") || sResetSeqNumFlag.equals("n"))
						message.setBoolean(ResetSeqNumFlag.FIELD, false);
				}
				if(addNextExpectedSeqNum.equalsIgnoreCase("Y")) {
					message.setInt(quickfix.field.NextExpectedMsgSeqNum.FIELD, nextExpectedSeqNum);
				}

				if (this.incorrectSenderMsgSeqNum)
				{
					message.getHeader().setInt(MsgSeqNum.FIELD, this.seqNumSender);

					try {
						logger.info("set next sender MsgSeqNum after logout to: {}", seqNumSender);
						session.getSessionState().getMessageStore().refresh(); // reopen files // possible race condition ?
						session.setNextSenderMsgSeqNum(seqNumSender);
					} catch (IOException e) {
						logger.error(e.getMessage(), e);
					}
					this.incorrectSenderMsgSeqNum = false;
				}
				if (this.incorrectTargetMsgSeqNum)
				{

					try {
						logger.info("set next target MsgSeqNum after logout to: {}", seqNumTarget);
						session.getSessionState().getMessageStore().refresh(); // reopen files // possible race condition ?
						session.setNextTargetMsgSeqNum(seqNumTarget);
					} catch (IOException e) {
						logger.error(e.getMessage(), e);
					}
					this.incorrectTargetMsgSeqNum = false;
				}

                boolean sendSupportsMicrosTime = settings.getBool(sessionID, FIXClient.SUPPORTS_MICROSECOND_TIMESTAMPS);
                if (sendSupportsMicrosTime) {
                    boolean microsUsed = settings.getBool(sessionID, Session.SETTING_MICROSECONDS_IN_TIMESTAMP);
                    message.setBoolean(FIXClient.SUPPORTS_MICROSECOND_TIMESTAMPS_TAG, microsUsed);
                }

                logger.debug("Username = {}, Password = {}, NewPassword = {}, DefaultCstmApplVerID = {}, ExtExecInst = {}, DefaultApplVerID = {}",
                        userName, password, newPassword, defaultCstmApplVerID, extExecInst, defaultApplVerID);
			}

			if (MsgType.LOGOUT.equals(msgType))
			{
			    String textMessage = "Sent Logout doesn't have text (58 tag)";
				if (message.isSetField(Text.FIELD))
				{
				    int targSeq = -1;
					String text = message.getString(Text.FIELD);
					textMessage = "Sent Logout has text (58) tag: " + text;

					if (text.startsWith("MsgSeqNum too low, expecting "))
					{
					    incorrectTargetMsgSeqNum = true;
						// extract 1 from the text: MsgSeqNum too low, expecting 4 but received 1
						targSeq = Integer.parseInt(text.split(" ")[7]);
					}

					if (targSeq != -1) {
	                    this.seqNumTarget = new Integer(targSeq);
					}

				}
				if (serviceMonitor != null) {
				    ServiceEvent event = ServiceEventFactory.createEventInfo(serviceName,
				            ServiceEvent.Type.INFO, textMessage, null);
				    serviceMonitor.onEvent(event);
				}
			}
		} catch (Exception e) {
			logger.error("toAdmin: Can not process the message {}", message, e);
		}
	}

	@Override
	public void toApp(Message message, SessionID sessionID) throws DoNotSend {
	    logger.debug("toApp: {}", message);
		//logger.info("toApp: getExpectedTargetNum "+Session.lookupSession(sessionID).getExpectedTargetNum());

		String type = getMessageType(message);

		if (this.sendAppReject == false) {
			if (MsgType.REJECT.equals(type)) {
				logger.info("Block appliction sending Reject message : {}", message);

				// DG: QFJ lock sender MsgSeqNum from changing before call this callback
				//setNextSenderMsgSeqNum(message, sessionID);
				throw new DoNotSend();
			}
		}
	}

	private String getMessageType(Message message)
	{
		try {
			return message.getHeader().getString(MsgType.FIELD);
		} catch (FieldNotFound e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}

	public ServiceInfo getServiceInfo() {
		return serviceInfo;
	}

	@Override
	public void addSessionId(SessionID sessionID, ISession iSession) {
        logger.info("add session: {} -> {} = {}", sessionID.getSenderCompID(),
                sessionID.getTargetCompID(), iSession.getName());
		this.sessionMap.put(sessionID, iSession);
		Session session = Session.lookupSession(sessionID);
		session.logon();
	}

	@Override
	public List<ISession> getSessions() {
		return new ArrayList<>(this.sessionMap.values());
	}

	public Integer getSeqNumSender() {
		return seqNumSender;
	}

	public Integer getSeqNumTarget() {
		return seqNumTarget;
	}

	public boolean isUseDefaultApplVerID() {
		return useDefaultApplVerID;
	}

	public boolean isPerformance() {
		return isPerformance;
	}

	public void setPerformance(boolean isPerformance) {
		this.isPerformance = isPerformance;
	}

	public boolean isAutorelogin() {
		return autorelogin;
	}

    @Override
    public void startLogging() {
        if (logConfigurator != null) {
            logConfigurator.createIndividualAppender(this.getClass().getName() + "@" + Integer.toHexString(hashCode()),
                serviceName);
        }
    }

    @Override
    public void stopLogging() {
        if (logConfigurator != null) {
            logConfigurator.destroyIndividualAppender(this.getClass().getName() + "@" + Integer.toHexString(hashCode()),
                    serviceName);
        }
    }

    /**
     * @param sessionID
     * @param message
     * @param isRejected TODO
     * @throws FieldNotFound
     */
    private void storeMessage(SessionID sessionID, IMessage message) {
        try {
            storage.storeMessage(message);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void processMessage(SessionID sessionID, Message message, ServiceHandlerRoute route,
                                String from, String to, boolean isAdmin) {
        if (!isPerformance) {
            ISession iSession = sessionMap.get(sessionID);
            IMessage iMsg = null;
            try {
                iMsg = convert(message, from, to, isAdmin, null);
                storeMessage(sessionID, iMsg);
                handler.putMessage(iSession, route, iMsg);
            } catch (ServiceHandlerException e) {
                handler.exceptionCaught(iSession, e);
                logger.error("{}: Can not put the message {} to handler", route.getAlias(), iMsg, e);
            } catch (MessageConvertException e) {
                logger.error("{}: Can not convert the message {}", route.getAlias(), message, e);
            } catch (Exception e) {
                logger.error("{}: Can not process the message {}", route.getAlias(), message, e);
            }
        }
    }


    private IMessage convert(Message message, String from, String to, boolean isAdmin, Boolean verifyTags) throws MessageConvertException {
        return convert(message, from, to, isAdmin, verifyTags, false);
    }

    private IMessage convert(Message message, String from, String to, boolean isAdmin, Boolean verifyTags, boolean isRejected) throws MessageConvertException {
        String messageData = message.getMessageData();
        byte[] rawMessage = messageData != null ? messageData.getBytes(CharsetSupport.getCharsetInstance()) : message.toString().getBytes(CharsetSupport.getCharsetInstance());
		IMessage msg = isRejected
                ? this.converter.convertDirty(message, verifyTags, false, false, true)
                : this.converter.convert(message, verifyTags, null);
		MsgMetaData meta = msg.getMetaData();

		meta.setFromService(from);
		meta.setToService(to);
		meta.setAdmin(isAdmin);
		meta.setRawMessage(rawMessage);
		meta.setServiceInfo(serviceInfo);

		return msg;
	}

    @Override
    public void onConnectionProblem(String reason) {
        applicationContext.connectionProblem(true, reason);
	}

    @Override
    public void onSendToAdmin(Message message, SessionID sessionId) {
        logger.debug("Save message toAdmin: {}", message);
        processMessage(sessionId, message, ServiceHandlerRoute.TO_ADMIN, serviceStringName, sessionId.getTargetCompID(), true);
    }

    @Override
    public void onSendToApp(Message message, SessionID sessionId) {
        logger.debug("Save message toApp: {}", message);
        processMessage(sessionId, message, ServiceHandlerRoute.TO_APP, serviceStringName, sessionId.getTargetCompID(), false);
    }
}