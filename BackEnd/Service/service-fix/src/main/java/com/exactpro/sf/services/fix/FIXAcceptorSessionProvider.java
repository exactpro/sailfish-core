/******************************************************************************
 * Copyright (c) 2009-2018, Exactpro Systems LLC
 * www.exactpro.com
 * Build Software to Test Software
 *
 * All rights reserved.
 * This is unpublished, licensed software, confidential and proprietary
 * information which is the property of Exactpro Systems LLC or its licensors.
 ******************************************************************************/
package com.exactpro.sf.services.fix;

import static quickfix.SessionSettings.BEGINSTRING;
import static quickfix.SessionSettings.SENDERCOMPID;
import static quickfix.SessionSettings.SENDERLOCID;
import static quickfix.SessionSettings.SENDERSUBID;
import static quickfix.SessionSettings.TARGETCOMPID;
import static quickfix.SessionSettings.TARGETLOCID;
import static quickfix.SessionSettings.TARGETSUBID;
import static quickfix.mina.acceptor.DynamicAcceptorSessionProvider.WILDCARD;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.quickfixj.QFJException;

import quickfix.Application;
import quickfix.ConfigError;
import quickfix.DataDictionaryProvider;
import quickfix.LogFactory;
import quickfix.MessageFactory;
import quickfix.MessageStoreFactory;
import quickfix.Session;
import quickfix.SessionFactory;
import quickfix.SessionID;
import quickfix.SessionSettings;
import quickfix.mina.SessionConnector;
import quickfix.mina.acceptor.AcceptorSessionProvider;
import quickfix.mina.acceptor.DynamicAcceptorSessionProvider;
import quickfix.mina.acceptor.DynamicAcceptorSessionProvider.TemplateMapping;

/**
 * This is adapted copy of {@link DynamicAcceptorSessionProvider} 
 */
public class FIXAcceptorSessionProvider implements AcceptorSessionProvider {

    protected final SessionSettings settings;
    protected final SessionFactory sessionFactory;
    private final List<TemplateMapping> templateMappings;
    
    public FIXAcceptorSessionProvider(SessionSettings settings,
            List<TemplateMapping> templateMappings, Application application,
            MessageStoreFactory messageStoreFactory, LogFactory logFactory,
            MessageFactory messageFactory,
            DataDictionaryProvider dictionaryProvider) {
        this.settings = settings;
        this.templateMappings = templateMappings;
        sessionFactory = new FixSessionFactory(application, messageStoreFactory, logFactory,
                messageFactory, dictionaryProvider);
    }
    
    @Override
    public synchronized Session getSession(SessionID sessionID, SessionConnector connector) {
        Session s = Session.lookupSession(sessionID);
        if (s == null) {
            try {
                SessionID templateID = lookupTemplateID(sessionID);
                if (templateID == null) {
                    throw new ConfigError("Unable to find a session template for " + sessionID);
                }
                SessionSettings dynamicSettings = new SessionSettings();
                copySettings(dynamicSettings, settings.getDefaultProperties());
                copySettings(dynamicSettings, settings.getSessionProperties(templateID));
                dynamicSettings.setString(BEGINSTRING, sessionID.getBeginString());
                dynamicSettings.setString(SENDERCOMPID, sessionID.getSenderCompID());
                optionallySetValue(dynamicSettings, SENDERSUBID, sessionID.getSenderSubID());
                optionallySetValue(dynamicSettings, SENDERLOCID, sessionID.getSenderLocationID());
                dynamicSettings.setString(TARGETCOMPID, sessionID.getTargetCompID());
                optionallySetValue(dynamicSettings, TARGETSUBID, sessionID.getTargetSubID());
                optionallySetValue(dynamicSettings, TARGETLOCID, sessionID.getTargetLocationID());
                s = sessionFactory.create(sessionID, dynamicSettings);
                if (connector != null) {
                    connector.addDynamicSession(s);
                }
            } catch (ConfigError e) {
                throw new QFJException(e);
            }
        }
        return s;
    }
    
    protected void optionallySetValue(SessionSettings dynamicSettings, String key, String value) {
        dynamicSettings.setString(key, value);
    }

    protected SessionID lookupTemplateID(SessionID sessionID) {
        for (TemplateMapping mapping : templateMappings) {
            if (isMatching(mapping.getPattern(), sessionID)) {
                return mapping.getTemplateID();
            }
        }
        return null;
    }
    
    private boolean isMatching(SessionID pattern, SessionID sessionID) {
        return isMatching(pattern.getBeginString(), sessionID.getBeginString())
                && isMatching(pattern.getSenderCompID(), sessionID.getSenderCompID())
                && isMatching(pattern.getSenderSubID(), sessionID.getSenderSubID())
                && isMatching(pattern.getSenderLocationID(), sessionID.getSenderLocationID())
                && isMatching(pattern.getTargetCompID(), sessionID.getTargetCompID())
                && isMatching(pattern.getTargetSubID(), sessionID.getTargetSubID())
                && isMatching(pattern.getTargetLocationID(), sessionID.getTargetLocationID());
    }

    private boolean isMatching(String pattern, String value) {
        return WILDCARD.equals(pattern) || (pattern != null && pattern.equals(value));
    }

    protected void copySettings(SessionSettings settings, Properties properties) {
        for (Entry<Object, Object> e : properties.entrySet()) {
            settings.setString((String) e.getKey(), e.getValue().toString());
        }
    }
}
