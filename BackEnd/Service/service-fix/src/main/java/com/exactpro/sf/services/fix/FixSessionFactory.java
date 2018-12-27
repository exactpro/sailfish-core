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

import java.net.InetAddress;
import java.util.Set;

import quickfix.Application;
import quickfix.ConfigError;
import quickfix.DataDictionaryProvider;
import quickfix.FieldConvertError;
import quickfix.Initiator;
import quickfix.LogFactory;
import quickfix.MessageFactory;
import quickfix.MessageStoreFactory;
import quickfix.MessageUtils;
import quickfix.Session;
import quickfix.SessionFactory;
import quickfix.SessionID;
import quickfix.SessionSchedule;
import quickfix.SessionSettings;
import quickfix.field.ApplVerID;
import quickfix.field.DefaultApplVerID;

public class FixSessionFactory implements SessionFactory {

    private final Application application;
    private final MessageStoreFactory messageStoreFactory;
    private final LogFactory logFactory;
    private final MessageFactory messageFactory;
	private final DataDictionaryProvider dictionaryProvider;

    public FixSessionFactory(Application application, MessageStoreFactory messageStoreFactory,
            LogFactory logFactory, MessageFactory messageFactory, DataDictionaryProvider dictionaryProvider) {
        this.application = application;
        this.messageStoreFactory = messageStoreFactory;
        this.logFactory = logFactory;
        this.messageFactory = messageFactory;
        this.dictionaryProvider = dictionaryProvider;
    }

    @Override
	public Session create(SessionID sessionID, SessionSettings settings) throws ConfigError {
        try {
            String connectionType = null;

            if (settings.isSetting(sessionID, SessionFactory.SETTING_CONNECTION_TYPE)) {
                connectionType = settings.getString(sessionID,
                        SessionFactory.SETTING_CONNECTION_TYPE);
            }

            if (connectionType == null) {
                throw new ConfigError("Missing ConnectionType");
            }

            if (!connectionType.equals(SessionFactory.ACCEPTOR_CONNECTION_TYPE)
                    && !connectionType.equals(SessionFactory.INITIATOR_CONNECTION_TYPE)) {
                throw new ConfigError("Invalid ConnectionType");
            }

            if (connectionType.equals(SessionFactory.ACCEPTOR_CONNECTION_TYPE)
                    && settings.isSetting(sessionID, SessionSettings.SESSION_QUALIFIER)) {
                throw new ConfigError("SessionQualifier cannot be used with acceptor.");
            }

            DefaultApplVerID senderDefaultApplVerID = null;
            ApplVerID targetDefaultApplVerID = null;

            if (sessionID.isFIXT()) {
                if (!settings.isSetting(sessionID, Session.SETTING_DEFAULT_APPL_VER_ID)) {
                    throw new ConfigError(Session.SETTING_DEFAULT_APPL_VER_ID
                            + " is required for FIXT transport");
                }
                ApplVerID defaultApplVerID = toApplVerID(
                        settings.getString(sessionID, Session.SETTING_DEFAULT_APPL_VER_ID));
                
                senderDefaultApplVerID = new DefaultApplVerID(defaultApplVerID.getValue());
                if (settings.isSetting(sessionID, Session.SETTING_USE_SENDER_DEFAULT_APPL_VER_ID_AS_INITIAL_TARGET) &&
                        settings.getBool(sessionID, Session.SETTING_USE_SENDER_DEFAULT_APPL_VER_ID_AS_INITIAL_TARGET)) {
                    targetDefaultApplVerID = defaultApplVerID;
                }
            }

            FixDataDictionaryProvider dataDictionaryProvider = (FixDataDictionaryProvider) dictionaryProvider;
            dataDictionaryProvider.configure(settings, sessionID);

            int heartbeatInterval = 0;
            if (connectionType.equals(SessionFactory.INITIATOR_CONNECTION_TYPE)) {
                heartbeatInterval = (int) settings.getLong(sessionID, Session.SETTING_HEARTBTINT);
                if (heartbeatInterval <= 0) {
                    throw new ConfigError("Heartbeat must be greater than zero");
                }
            }

            boolean checkLatency = getSetting(settings, sessionID, Session.SETTING_CHECK_LATENCY,
                    true);
            int maxLatency = getSetting(settings, sessionID, Session.SETTING_MAX_LATENCY,
                    Session.DEFAULT_MAX_LATENCY);
            double testRequestDelayMultiplier = getSetting(settings, sessionID,
                    Session.SETTING_TEST_REQUEST_DELAY_MULTIPLIER,
                    Session.DEFAULT_TEST_REQUEST_DELAY_MULTIPLIER);

            boolean millisInTimestamp = getSetting(settings, sessionID,
                    Session.SETTING_MILLISECONDS_IN_TIMESTAMP, true);

            boolean microsInTimestamp = getSetting(settings, sessionID,
                    Session.SETTING_MICROSECONDS_IN_TIMESTAMP, false);

            boolean resetOnLogout = getSetting(settings, sessionID,
                    Session.SETTING_RESET_ON_LOGOUT, false);

            boolean resetOnDisconnect = getSetting(settings, sessionID,
                    Session.SETTING_RESET_ON_DISCONNECT, false);

            boolean resetOnLogon = getSetting(settings, sessionID, Session.SETTING_RESET_ON_LOGON,
                    false);

            boolean refreshAtLogon = getSetting(settings, sessionID,
                    Session.SETTING_REFRESH_ON_LOGON, false);

            boolean checkCompID = getSetting(settings, sessionID, Session.SETTING_CHECK_COMP_ID,
                    true);

            boolean redundantResentRequestAllowed = getSetting(settings, sessionID,
                    Session.SETTING_SEND_REDUNDANT_RESEND_REQUEST, false);

            boolean persistMessages = getSetting(settings, sessionID,
                    Session.SETTING_PERSIST_MESSAGES, true);

            boolean useClosedIntervalForResend = getSetting(settings, sessionID,
                    Session.SETTING_USE_CLOSED_RESEND_INTERVAL, false);

            boolean duplicateTagsAllowed = getSetting(settings, sessionID,
                    Session.DUPLICATE_TAGS_ALLOWED, false);
            
            boolean ignoreAbsenceOf141tag = getSetting(settings, sessionID,
                    Session.IGNORE_ABSENCE_OF_141_TAG, false);

            int logonTimeout = getSetting(settings, sessionID, Session.SETTING_LOGON_TIMEOUT, 10);
            int logoutTimeout = getSetting(settings, sessionID, Session.SETTING_LOGOUT_TIMEOUT, 2);

            final boolean validateSequenceNumbers = getSetting(settings, sessionID, Session.SETTING_VALIDATE_SEQUENCE_NUMBERS, true);
            final boolean validateIncomingMessage  = getSetting(settings, sessionID, Session.SETTING_VALIDATE_INCOMING_MESSAGE, true);
            final boolean resetOnError = getSetting(settings, sessionID, Session.SETTING_RESET_ON_ERROR, false);
            final boolean disconnectOnError = getSetting(settings, sessionID, Session.SETTING_DISCONNECT_ON_ERROR, false);
            final boolean disableHeartBeatCheck = getSetting(settings, sessionID, Session.SETTING_DISABLE_HEART_BEAT_CHECK, false);
            final boolean forceResendWhenCorruptedStore = getSetting(settings, sessionID, Session.SETTING_FORCE_RESEND_WHEN_CORRUPTED_STORE, false);
            final boolean enableNextExpectedMsgSeqNum = getSetting(settings, sessionID, Session.SETTING_ENABLE_NEXT_EXPECTED_MSG_SEQ_NUM, false);
            final boolean enableLastMsgSeqNumProcessed = getSetting(settings, sessionID, Session.SETTING_ENABLE_LAST_MSG_SEQ_NUM_PROCESSED, false);
            final int resendRequestChunkSize = getSetting(settings, sessionID, Session.SETTING_RESEND_REQUEST_CHUNK_SIZE, Session.DEFAULT_RESEND_RANGE_CHUNK_SIZE);

            final boolean rejectInvalidMessage = getSetting(settings, sessionID,
                    Session.SETTING_REJECT_INVALID_MESSAGE, true);

            final boolean rejectMessageOnUnhandledException = getSetting(settings, sessionID,
                    Session.SETTING_REJECT_MESSAGE_ON_UNHANDLED_EXCEPTION, false);

            final boolean requiresOrigSendingTime = getSetting(settings, sessionID,
                    Session.SETTING_REQUIRES_ORIG_SENDING_TIME, true);

            final int[] logonIntervals = getLogonIntervalsInSeconds(settings, sessionID);
            final Set<InetAddress> allowedRemoteAddresses = getInetAddresses(settings, sessionID);

            int receiveLimit = getSetting(settings, sessionID, Session.RECEIVE_LIMIT, 0);

            final Session session = new Session(application, messageStoreFactory, sessionID,
                    dataDictionaryProvider, new SessionSchedule(settings, sessionID), logFactory,
                    messageFactory, heartbeatInterval, checkLatency, maxLatency, millisInTimestamp, microsInTimestamp,
                    resetOnLogon, resetOnLogout, resetOnDisconnect, refreshAtLogon, checkCompID,
                    redundantResentRequestAllowed, persistMessages, useClosedIntervalForResend,
                    testRequestDelayMultiplier, senderDefaultApplVerID, targetDefaultApplVerID, validateSequenceNumbers,
                    logonIntervals, resetOnError, disconnectOnError, disableHeartBeatCheck,
                    rejectInvalidMessage, rejectMessageOnUnhandledException, requiresOrigSendingTime,
                    forceResendWhenCorruptedStore, allowedRemoteAddresses, validateIncomingMessage,
                    resendRequestChunkSize, enableNextExpectedMsgSeqNum, enableLastMsgSeqNumProcessed,
                    duplicateTagsAllowed, ignoreAbsenceOf141tag);

            session.setLogonTimeout(logonTimeout);
            session.setLogoutTimeout(logoutTimeout);
            session.setReceiveLimit(receiveLimit);

            //
            // Session registration and creation callback is done here instead of in
            // session constructor to eliminate the possibility of other threads
            // accessing the session before it's fully constructed.
            //

            application.onCreate(sessionID);

            return session;
        } catch (FieldConvertError e) {
            throw new ConfigError(e.getMessage());
        }
    }

    private ApplVerID toApplVerID(String value) {
        if (isApplVerIdEnum(value)) {
            return new ApplVerID(value);
        } else {
            // value should be a beginString
            return MessageUtils.toApplVerID(value);
        }
    }

    private boolean isApplVerIdEnum(String value) {
        return value.matches("[0-9]+");
    }

    private boolean getSetting(SessionSettings settings, SessionID sessionID, String key,
            boolean defaultValue) throws ConfigError, FieldConvertError {
        return settings.isSetting(sessionID, key) ? settings.getBool(sessionID, key) : defaultValue;
    }

    private int getSetting(SessionSettings settings, SessionID sessionID, String key,
            int defaultValue) throws ConfigError, FieldConvertError {
        return settings.isSetting(sessionID, key)
                ? (int) settings.getLong(sessionID, key)
                : defaultValue;
    }

    private double getSetting(SessionSettings settings, SessionID sessionID, String key,
            double defaultValue) throws ConfigError, FieldConvertError {
        return settings.isSetting(sessionID, key)
                ? Double.parseDouble(settings.getString(sessionID, key))
                : defaultValue;
    }

    private int[] getLogonIntervalsInSeconds(SessionSettings settings, SessionID sessionID) throws ConfigError {
        if (settings.isSetting(sessionID, Initiator.SETTING_RECONNECT_INTERVAL)) {
            try {
                final String raw = settings.getString(sessionID, Initiator.SETTING_RECONNECT_INTERVAL);
                final int[] ret = SessionSettings.parseSettingReconnectInterval(raw);
                if (ret != null) return ret;
            } catch (final Throwable e) {
                throw new ConfigError(e);
            }
        }
        return new int[] { 5 }; // default value
    }

    private Set<InetAddress> getInetAddresses(SessionSettings settings, SessionID sessionID)
            throws ConfigError {
        if (settings.isSetting(sessionID, Session.SETTING_ALLOWED_REMOTE_ADDRESSES)) {
            try {
                final String raw = settings.getString(sessionID,
                        Session.SETTING_ALLOWED_REMOTE_ADDRESSES);
                return SessionSettings.parseRemoteAddresses(raw);
            } catch (final Throwable e) {
                throw new ConfigError(e);
            }
        }
        return null; // default value
    }


}
