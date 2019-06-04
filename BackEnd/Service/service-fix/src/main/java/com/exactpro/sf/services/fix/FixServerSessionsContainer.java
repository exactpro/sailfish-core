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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.util.SendMessageFailedException;
import com.exactpro.sf.services.ISession;

public class FixServerSessionsContainer implements ISession {

    private final IQuickfixApplication application;

    public FixServerSessionsContainer(IQuickfixApplication application) {
        this.application = application;
    }

    @Override
    public String getName() {
        return "FixServerSessions";
    }

    @Override
    public IMessage send(Object message) throws InterruptedException {
        Map<ISession, String> sentErrors = new HashMap<>();
        for(ISession session : application.getSessions()) {
            try {
                session.send(message);
            } catch (RuntimeException e) {
                String errorMsg = e.getCause() != null ? String.format("%s; cause: %s", e.getMessage(), e.getCause()) : e.getMessage();
                sentErrors.put(session, errorMsg);
            }
        }

        if (!sentErrors.isEmpty()) {
            StringBuilder errorBuilder = new StringBuilder("Not all message successfully sent: ");
            for (Entry<ISession, String> error : sentErrors.entrySet()) {
                errorBuilder.append(StringUtils.LF + String.format("%s : [%s]", error.getKey(), error.getValue()));
            }
            throw new SendMessageFailedException(errorBuilder.toString());
        }

        return message instanceof IMessage ? (IMessage)message : null;
    }

    @Override
    public IMessage sendDirty(Object message) throws InterruptedException {
        for(ISession session : application.getSessions()) {
            session.sendDirty(message);
        }
        return message instanceof IMessage ? (IMessage)message : null;
    }

    @Override
    public void close() {
        for(ISession session : application.getSessions()) {
            session.close();
        }
    }

    @Override
    public boolean isClosed() {
        for(ISession session : application.getSessions()) {
            if (!session.isClosed()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isLoggedOn() {
        throw new UnsupportedOperationException();
    }

    public void addExpectedSenderNum(int seq) {
        for (ISession session : application.getSessions()) {
            ((FIXSession)session).addExpectedSenderNum(seq);
        }
    }
}
