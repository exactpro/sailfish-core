/******************************************************************************
 * Copyright 2009-2024 Exactpro (Exactpro Systems Limited)
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


import static com.exactpro.sf.actions.FIXMatrixUtil.SEQUENCE;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.crypto.Cipher;

import org.apache.commons.io.FileUtils;
import org.apache.mina.util.Base64;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.actions.FIXMatrixUtil;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.MsgMetaData;
import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.common.util.StringUtil;
import com.exactpro.sf.configuration.ILoggingConfigurator;
import com.exactpro.sf.messages.service.ErrorMessage;
import com.exactpro.sf.services.IServiceContext;
import com.exactpro.sf.services.IServiceHandler;
import com.exactpro.sf.services.IServiceMonitor;
import com.exactpro.sf.services.ISession;
import com.exactpro.sf.services.MessageHelper;
import com.exactpro.sf.services.ServiceEvent;
import com.exactpro.sf.services.ServiceEvent.Type;
import com.exactpro.sf.services.ServiceEventFactory;
import com.exactpro.sf.services.ServiceHandlerException;
import com.exactpro.sf.services.ServiceHandlerRoute;
import com.exactpro.sf.services.fix.converter.MessageConvertException;
import com.exactpro.sf.storage.IMessageStorage;

import quickfix.ConfigError;
import quickfix.DoNotSend;
import quickfix.FieldConvertError;
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
import quickfix.field.NextExpectedMsgSeqNum;
import quickfix.field.ResetSeqNumFlag;
import quickfix.field.Text;

public class FIXApplication extends AbstractApplication implements FIXClientApplication {

    private static final String BEGIN_PUBLIC_KEY = "-----BEGIN PUBLIC KEY-----";
    private static final String END_PUBLIC_KEY = "-----END PUBLIC KEY-----";
    public static final String KEY_ENCODE_ALGORITHM = "RSA";
    private final Logger logger = LoggerFactory.getLogger(ILoggingConfigurator.getLoggerName(this));

	public static final String ENCRYPT_PASSWORD = "EncryptPassword";
    public static final String ENCRYPT_METHOD = "EncryptMethod";
	public static final String ADD_NEXT_EXPECTED_SEQ_NUM = "AddNextExpectedMsgSeqNum";
	public static final String DefaultCstmApplVerID = "DefaultCstmApplVerID";
	public static final String REQUIRED_TAGS = "RequiredTags";
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
    private ILoggingConfigurator logConfigurator;
    private IServiceMonitor serviceMonitor;
	private IServiceHandler handler;
    private MessageHelper messageHelper;

	private final Map<SessionID, ISession> sessionMap;
	private IMessageStorage storage;
	private SessionSettings settings;
	private boolean sendAppReject = true;
	private boolean sendAdminReject = true;

	private Integer seqNumSender;
	private Integer seqNumTarget;
    private boolean incorrectSenderMsgSeqNum; // update seqnum on logon
    private boolean incorrectTargetMsgSeqNum; // update seqnum on logon

    private boolean useDefaultApplVerID;

    private boolean isPerformance;

	private boolean autorelogin = true;

    private FIXLatencyCalculator latencyCalculator;
    private FIXClientSettings fixSettings;

    private Pattern seqNumberRejectPattern;
    private Pattern seqNumberLogoutPattern;

    public FIXApplication() {
        this.sessionMap = new HashMap<>();
    }

    @Override
    public void init(IServiceContext serviceContext, ApplicationContext applicationContext, ServiceName serviceName) {
        super.init(serviceContext, applicationContext, serviceName);
        this.fixSettings = (FIXClientSettings) this.applicationContext.getServiceSettings();
        this.serviceStringName = serviceName.toString();
        this.serviceName = serviceName;
        this.logConfigurator = serviceContext.getLoggingConfigurator();
        this.serviceMonitor = this.applicationContext.getServiceMonitor();
        this.handler = this.applicationContext.getServiceHandler();
        this.messageHelper = this.applicationContext.getMessageHelper();
        this.storage = serviceContext.getMessageStorage();
        this.settings = this.applicationContext.getSessionSettings();

        this.sendAppReject = getBoolean(settings, SEND_APP_REJECT, true);
        this.sendAdminReject = getBoolean(settings, SEND_ADMIN_REJECT, true);

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
        this.latencyCalculator = fixSettings.isEvolutionOptimize() ? new DummyFIXLatencyCalculator(messageHelper) : new FIXLatencyCalculator(messageHelper);
        this.seqNumberRejectPattern = compilePattern(fixSettings.getSeqNumberfromRejectRegexp());
        this.seqNumberLogoutPattern = compilePattern(fixSettings.getSeqNumberfromLogoutRegexp());
    }

    @Nullable
    private Pattern compilePattern(String regexp) {
        try {
            if (regexp != null) {
                return Pattern.compile(regexp);
            }
        } catch (PatternSyntaxException e) {
            throw new EPSCommonException("Wrong regexp " + regexp + " for setting", e);
        }
        return  null;
    }

    @Override
    public void fromAdmin(Message message, SessionID sessionID) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
        logger.debug("fromAdmin: {}", message);
        processMessage(sessionID, message, ServiceHandlerRoute.FROM_ADMIN, sessionID.getTargetCompID(), serviceStringName, true);

		Session session = Session.lookupSession(sessionID);
		ISession iSession = sessionMap.get(sessionID);

		String msgType = getMessageType(message);
		MsgSeqNum msgSeqNum = new MsgSeqNum();
		message.getHeader().getField(msgSeqNum);
		logger.debug("message seqnum vs expected: {}:{}", msgSeqNum, session.getExpectedTargetNum());

        if(MsgType.REJECT.equals(msgType)){
            if (message.isSetField(Text.FIELD)) {
                String rejectText = message.getString(Text.FIELD);
                if (!extractSeqNumRejectTextRegexp(session, iSession, rejectText)) {
                    extractSeqNumRejectTextPattern(session, iSession, rejectText);
                }
            }
        } else if (MsgType.LOGOUT.equals(msgType)) {
            String textMessage = "Received Logout doesn't have text (58 tag)";
			if (message.isSetField(Text.FIELD))
			{
				logger.info("Logout received - {}", message);

				String text = message.getString(Text.FIELD);
                textMessage = "Received Logout has text (58) tag: " + text;

                try {
                    if (!extractSeqNumLogoutTextRegexp(message, text)) {
                        extractSeqNumLogoutTextPattern(message, text);
                    }
                } catch (NumberFormatException | EPSCommonException e) {
                    exceptionCaught(iSession, "Update sender / target sequence numbers via text '" + text + "' from logout message failure", e);
                }
			}
            if (serviceMonitor != null) {
                ServiceEvent event = ServiceEventFactory.createEventInfo(serviceName,
                        Type.INFO, textMessage, null);
                serviceMonitor.onEvent(event);
            }
		} else if (fixSettings.getFakeResendRequest() && MsgType.RESEND_REQUEST.equals(msgType)) {
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
			    exceptionCaught(iSession, "Filling gap from " +beginString+ " to " + endSeqNo + " message sequences using fake heartbeats failure", e);
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

    private void extractSeqNumLogoutTextPattern(Message message, String text) throws FieldNotFound {
        int seqNum = -1;
        int targSeq = -1;
        if (StringUtil.containsAll(text, "MsgSeqNum", "too low, expecting")
                || StringUtil.containsAll(text, "Wrong sequence number!", "Too small to recover. Received: ", "Expected: ", ">.")
                || StringUtil.containsAll(text, "Sequence Number", "<", "expected")
                || StringUtil.containsAll(text, "MsgSeqNum", "less than expected")) {
            incorrectSenderMsgSeqNum = true;
            // extract 4 from the text: MsgSeqNum too low, expecting 4 but received 1
            seqNum = FIXMatrixUtil.extractSeqNum(text);
            // DG: experimentally checked
            // only here set next seq num as seqMum-1.
            //seqNum = seqNum-1; // nikolay.antonov : It is seems doesn't works.
            targSeq = message.getHeader().getInt(MsgSeqNum.FIELD);
        } else if (text.startsWith("Error ! Expecting : ")) {
            incorrectTargetMsgSeqNum = true;
            // extract 1282 from the text: Error ! Expecting : 1282 but received : 1281
            seqNum = Integer.parseInt(text.split(" ")[4]);
            targSeq = message.getHeader().getInt(MsgSeqNum.FIELD);
        } else if (text.startsWith("Negative gap for the user")) {
            incorrectSenderMsgSeqNum = true;
            String num = text.substring(
                    text.lastIndexOf('[') + 1,
                    text.lastIndexOf(']'));
            seqNum = Integer.parseInt(num);

            //incorrectTargetMsgSeqNum = true; // TODO
            targSeq = message.getHeader().getInt(MsgSeqNum.FIELD); // TODO: +1 ?
        }
        if (seqNum != -1) {
            this.seqNumSender = seqNum;
        }

        if (targSeq != -1) {
            this.seqNumTarget = targSeq;
        }
    }

    private boolean extractSeqNumLogoutTextRegexp(Message message, String text) throws FieldNotFound {
        String expectedSeqNum = null;
        if (seqNumberLogoutPattern != null) {
           expectedSeqNum = FIXMatrixUtil.extractSeqNumRegexp(text, seqNumberLogoutPattern);
           if (expectedSeqNum == null) {
               String eventMessage = "Text " + text + " does not match the pattern " + seqNumberLogoutPattern;
               sendServiceEvent(eventMessage);
           } else {
               incorrectTargetMsgSeqNum = true;
               this.seqNumSender = Integer.parseInt(expectedSeqNum);
               this.seqNumTarget = message.getHeader().getInt(MsgSeqNum.FIELD);
           }
       }
        return expectedSeqNum != null;
    }

    private void extractSeqNumRejectTextPattern(Session session, ISession iSession, @NotNull String rejectText) {
        String expectedSeqNum;
        if (rejectText.startsWith("Wrong sequence number")) {
            if (!StringUtil.containsAll(rejectText, "Received:", "Expected:")) {
                logger.info("Trying to change session.setNextSenderMsgSeqNum, but text (58 tag) have unsupported format.");
                return;
            }
            expectedSeqNum = rejectText.split("Expected:")[1].replaceAll("[\\D]", "");
            setSenderSeqNum(session, iSession, rejectText, expectedSeqNum);
        }
    }

    private boolean extractSeqNumRejectTextRegexp(Session session, ISession iSession, String rejectText) {
        String expectedSeqNum = null;
        if (seqNumberRejectPattern != null) {
            try {
                expectedSeqNum = FIXMatrixUtil.extractSeqNumRegexp(rejectText, seqNumberRejectPattern);
                if (expectedSeqNum == null) {
                    String textMessage = "Text " + rejectText + " does not match the pattern " + seqNumberRejectPattern;
                    sendServiceEvent(textMessage);
                } else {
                    setSenderSeqNum(session, iSession, rejectText, expectedSeqNum);
                }
            } catch (EPSCommonException e) {
                String textMessage = "Failed group " + SEQUENCE + " in pattern " + seqNumberRejectPattern;
                exceptionCaught(iSession, textMessage, e);
                sendServiceEvent(textMessage);
            }
        }
        return expectedSeqNum != null;
    }

    private void sendServiceEvent(String textMessage) {
        if (serviceMonitor != null) {
            ServiceEvent event = ServiceEventFactory.createEventInfo(serviceName,
                    Type.INFO, textMessage, null);
            serviceMonitor.onEvent(event);
        }
    }

    private void setSenderSeqNum(Session session, ISession iSession, String rejectText, String expectedSeqNum) {
        try {
            this.seqNumSender = Integer.parseInt(expectedSeqNum);
            session.setNextSenderMsgSeqNum(seqNumSender);
            logger.info("Set session.setNextSenderMsgSeqNum = {}", seqNumSender);

        } catch (NumberFormatException | IOException e) {
            exceptionCaught(iSession, "Update sender sequence number via text '" + rejectText + "' from reject message failure", e);
        }
    }

    @Override
    public void fromApp(Message message, SessionID sessionID)
            throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        logger.debug("fromApp: {}", message);
        processMessage(sessionID, message, ServiceHandlerRoute.FROM_APP,  sessionID.getTargetCompID(), serviceStringName, false);
	}

	@Override
	public void onMessageRejected(Message message, SessionID sessionID, String reason) {
		logger.debug("onMessageRejected: {}", message);
        if (!isPerformance) {
            ISession iSession = sessionMap.get(sessionID);
            IMessage iMsg = null;
            try {
                iMsg = convert(message, sessionID.getTargetCompID(), serviceStringName, message.isAdmin(), false, false, true);
                iMsg.getMetaData().setRejectReason(reason);
                // We don't call IServiceHandler here because this message is invalid. They shouldn't reach comparator
            } catch (MessageConvertException | RuntimeException e) {
                // don't throw it to QFJ
                exceptionCaught(iSession, "Process reject of message " + message + " failure", e);

                iMsg = createErrorMessage(message, e.getMessage() + ". Supprecced reject: " + reason, null);
            } finally {
                storeMessage(iSession, iMsg);
            }

        }
	}

	@Override
	public void onCreate(SessionID sessionID)
	{
		ISession iSession = sessionMap.get(sessionID);

		Session session = Session.lookupSession(sessionID);

		try {
            if(seqNumSender != null) {
                session.setNextSenderMsgSeqNum(seqNumSender);
				logger.info("Set session.setNextSenderMsgSeqNum = {}", seqNumSender);
			}
		} catch (IOException e) {
            exceptionCaught(iSession, "Could not set specified both seqNumSender " + seqNumSender + " for the session " + iSession.getName(), e);
		}

		try {
            if(seqNumTarget != null) {
                session.setNextTargetMsgSeqNum(seqNumTarget);
                logger.info("Set session.setNextTargetMsgSeqNum = {}", seqNumTarget);
            }
        } catch (IOException e) {
            exceptionCaught(iSession, "Could not set specified both seqNumTarget " + seqNumTarget + " for the session " + iSession.getName(), e);
        }

        try {
            handler.sessionOpened(iSession);
        } catch (ServiceHandlerException e) {
            exceptionCaught(iSession, "onCreate: handler.sessionOpened(" + iSession + ") failure", e);
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
			exceptionCaught(iSession, "onLogout: handler.sessionClosed(" + iSession + ')', e);
		}
        if(!autorelogin)
		{
			Session session = Session.lookupSession(sessionID);
			session.logout("Logon After Server Logout");
		}

        latencyCalculator.removeLatency(sessionID);
	}

	@Override
	public void toAdmin(Message message, SessionID sessionID) throws DoNotSend {
	    logger.debug("toAdmin: {}", message);

		String msgType = getMessageType(message);
		ISession iSession = sessionMap.get(sessionID);

		try {
			if (MsgType.LOGON.equals(msgType))
			{
				Session session = Session.lookupSession(sessionID);
				session.getSessionState().getMessageStore().refresh();

				String userName = (String) settings.getSessionProperties(sessionID).get(Username);
				String keyFile = (String) settings.getSessionProperties(sessionID).get(ENCRYPTION_KEY_FILE_PATH);
				String settingsPassword = (String) settings.getSessionProperties(sessionID).get(Password);
				String settingsNewPassword = (String) settings.getSessionProperties(sessionID).get(NewPassword);
				boolean encrypt = "Y".equalsIgnoreCase(settings.getSessionProperties(sessionID).get(ENCRYPT_PASSWORD).toString());
				String password = null;
				String newPassword = null;
                if(encrypt) {
                    Cipher cipher = Cipher.getInstance(KEY_ENCODE_ALGORITHM);
                    cipher.init(Cipher.ENCRYPT_MODE, readPublicKey(keyFile));

                    if(settingsPassword != null) {
                        byte[] encryptedPasswordBytes = cipher.doFinal(settingsPassword.getBytes());
                        password = new String(Base64.encodeBase64(encryptedPasswordBytes));
                    }

					if(settingsNewPassword != null) {
                        byte[] encryptedNewPasswordBytes = cipher.doFinal(settingsNewPassword.getBytes());
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

                if(userName != null) {
                    message.setString(quickfix.field.Username.FIELD, userName);
                }
                if(password != null) {
                    message.setString(encrypt ? quickfix.field.EncryptedPassword.FIELD : quickfix.field.Password.FIELD, password);
                }
                if(newPassword != null) {
                    message.setString(encrypt ? quickfix.field.EncryptedNewPassword.FIELD : quickfix.field.NewPassword.FIELD, newPassword);
                }

                Integer encryptMethod = (Integer)settings.getSessionProperties(sessionID).get(ENCRYPT_METHOD);
                if (encryptMethod != null) {
                    message.setInt(quickfix.field.EncryptMethod.FIELD, encryptMethod);
                }

                if(defaultCstmApplVerID != null) {
                    message.setString(1408, defaultCstmApplVerID);
                }
                if(extExecInst != null) {
                    message.setString(8718, extExecInst);
                }
				if (resetSeqNumFlag != null)
				{
					String sResetSeqNumFlag = ((String)resetSeqNumFlag).toLowerCase();
                    if("true".equals(sResetSeqNumFlag) || "y".equals(sResetSeqNumFlag)) {
                        message.setBoolean(ResetSeqNumFlag.FIELD, true);
                    }
                    if("false".equals(sResetSeqNumFlag) || "n".equals(sResetSeqNumFlag)) {
                        message.setBoolean(ResetSeqNumFlag.FIELD, false);
                    }
                }
                if("Y".equalsIgnoreCase(addNextExpectedSeqNum)) {
                    message.setInt(NextExpectedMsgSeqNum.FIELD, nextExpectedSeqNum);
				}

                if(incorrectSenderMsgSeqNum)
				{
                    message.getHeader().setInt(MsgSeqNum.FIELD, seqNumSender);

					try {
						logger.info("set next sender MsgSeqNum after logout to: {}", seqNumSender);
						session.getSessionState().getMessageStore().refresh(); // reopen files // possible race condition ?
						session.setNextSenderMsgSeqNum(seqNumSender);
					} catch (IOException e) {
                        exceptionCaught(iSession, "Set next sender sequence number to " + seqNumSender + " failure", e);
					}
					this.incorrectSenderMsgSeqNum = false;
				}
                if(incorrectTargetMsgSeqNum)
				{

					try {
						logger.info("set next target MsgSeqNum after logout to: {}", seqNumTarget);
						session.getSessionState().getMessageStore().refresh(); // reopen files // possible race condition ?
						session.setNextTargetMsgSeqNum(seqNumTarget);
					} catch (IOException e) {
                        exceptionCaught(iSession, "Set next target sequence number to " + seqNumTarget + " failure", e);
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
                sendServiceEvent(textMessage);
			}
		} catch (Exception e) {
            exceptionCaught(iSession, "toAdmin: process the message " + message + " failure", e);
		}
	}

	@Override
	public void toApp(Message message, SessionID sessionID) throws DoNotSend {
	    logger.debug("toApp: {}", message);
		//logger.info("toApp: getExpectedTargetNum "+Session.lookupSession(sessionID).getExpectedTargetNum());

		String type = getMessageType(message);

        if(sendAppReject == false) {
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

	@Override
	public void addSessionId(SessionID sessionID, ISession iSession) {
        logger.info("add session: {} -> {} = {}", sessionID.getSenderCompID(),
                sessionID.getTargetCompID(), iSession.getName());
        sessionMap.put(sessionID, iSession);
		Session session = Session.lookupSession(sessionID);
		session.logon();
	}

	@Override
	public List<ISession> getSessions() {
        return new ArrayList<>(sessionMap.values());
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
            logConfigurator.registerLogger(this, serviceName);
        }
    }

    /**
     * @throws FieldNotFound
     */
    private void storeMessage(ISession iSession, IMessage message) {
        try {
            storage.storeMessage(message);
        } catch (RuntimeException e) {
            exceptionCaught(iSession, "Sore message " + message + " failure", e);
        }
    }

    private void processMessage(SessionID sessionID, Message message, ServiceHandlerRoute route,
                                String from, String to, boolean isAdmin) {
        if (!isPerformance) {
            ISession iSession = sessionMap.get(sessionID);
            try {
                IMessage iMsg = convert(message, from, to, isAdmin, isOutComingRoute(route));
                storeMessage(iSession, iMsg);
                handler.putMessage(iSession, route, iMsg);

                for(IMessage subMsg: handleSubMessage(iMsg)) {
                    storeMessage(iSession, subMsg);
                    handler.putMessage(iSession, route, subMsg);
                }

                if(route == ServiceHandlerRoute.FROM_ADMIN || route == ServiceHandlerRoute.FROM_APP) {
                    latencyCalculator.updateLatency(sessionID, iMsg);
                }
            } catch (ServiceHandlerException | MessageConvertException | RuntimeException e) {
                exceptionCaught(iSession, route.getAlias() + ": process message " + message + " failure", e);

                IMessage iMsg = createErrorMessage(message, e.getMessage(), route);
                storeMessage(iSession, iMsg);
            }
        }
    }

    protected @NotNull List<IMessage> handleSubMessage(IMessage message) {
        return Collections.emptyList();
    }

    private IMessage createErrorMessage(@NotNull Message qfjMessage, String error, ServiceHandlerRoute route) {
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setCause(error);

        MsgMetaData metaData = errorMessage.getMessage().getMetaData();

        // qfjMessage.getMessageData() might be null in case the message was created via code
        // FIXME: Integrate this hotfix into qfj library
        byte[] rawMessage = extractRawData(qfjMessage, isOutComingRoute(route));
        metaData.setRawMessage(rawMessage);

        return errorMessage.getMessage();
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

    @Override
    public long getLatency(SessionID sessionID) {
        return latencyCalculator.getLatency(sessionID);
    }

    private boolean getBoolean(SessionSettings settings, String key, boolean defaultValue) {
        if(settings.isSetting(key)) {
            try {
                return settings.getBool(key);
            } catch (ConfigError | FieldConvertError e) {
                logger.error(e.getMessage(), e);
            }
        }
        return defaultValue;
    }

    private void exceptionCaught(ISession iSession, String message, Throwable e) {
        RuntimeException cause = new RuntimeException(message, e);
        handler.exceptionCaught(iSession, cause);
        logger.error(cause.getMessage(), cause);
    }
    private static RSAPublicKey readPublicKey(String filePath) throws Exception {
        File file = new File(filePath);
        String key = FileUtils.readFileToString(file, Charset.defaultCharset());

        String publicKeyPEM = key
                .replace(BEGIN_PUBLIC_KEY, "")
                .replaceAll(System.lineSeparator(), "")
                .replace(END_PUBLIC_KEY, "");

        byte[] encoded = org.apache.commons.codec.binary.Base64.decodeBase64(publicKeyPEM);

        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ENCODE_ALGORITHM);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
        return (RSAPublicKey) keyFactory.generatePublic(keySpec);
    }
}