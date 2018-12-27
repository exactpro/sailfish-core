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
package com.exactpro.sf.services;

/**
 * @author nikita.smirnov
 *
 */
public class ServiceHandlerException extends Exception {

    private static final long serialVersionUID = 1704744541784208811L;

    private ISession session;
    private IdleStatus idleStatus;
    private Object sfMessage;
    
    public ServiceHandlerException() {
        super();
    }
    
    public ServiceHandlerException(String message) {
        super(message);
    }

    public ServiceHandlerException(Throwable cause) {
        super(cause);
    }

    public ServiceHandlerException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public ServiceHandlerException(ISession session, IdleStatus idleStatus, Object sfMessage, String message, Throwable cause) {
        super(message, cause);
        init(session, idleStatus, sfMessage);
    }

    public ServiceHandlerException(ISession session, IdleStatus idleStatus, Object sfMessage, String message) {
        super(message);
        init(session, idleStatus, sfMessage);
    }
    
    public ServiceHandlerException(ISession session, IdleStatus idleStatus, Object sfMessage, Throwable cause) {
        super(cause);
        init(session, idleStatus, sfMessage);
    }
    
    public ServiceHandlerException(ISession session, IdleStatus idleStatus, Object sfMessage) {
        super();
        init(session, idleStatus, sfMessage);
    }
    
    public ServiceHandlerException(ISession session, String message, Throwable cause) {
        this(session, null, null, message, cause);
    }
    
    public ServiceHandlerException(ISession session, IdleStatus idleStatus, String message, Throwable cause) {
        this(session, idleStatus, null, message, cause);
    }
    
    public ServiceHandlerException(ISession session, Object sfMessage, String message, Throwable cause) {
        this(session, null, sfMessage, message, cause);
    }
    
    public ServiceHandlerException(ISession session, Throwable cause) {
        this(session, null, null, cause);
    }
    
    public ServiceHandlerException(ISession session, IdleStatus idleStatus, Throwable cause) {
        this(session, idleStatus, null, cause);
    }
    
    public ServiceHandlerException(ISession session, Object sfMessage, Throwable cause) {
        this(session, null, sfMessage, cause);
    }
    
    public ISession getSession() {
        return session;
    }

    public IdleStatus getIdleStatus() {
        return idleStatus;
    }

    public Object getSfMessage() {
        return sfMessage;
    }

    private void init(ISession session, IdleStatus idleStatus, Object sfMessage) {
        this.session = session;
        this.idleStatus = idleStatus;
        this.sfMessage = sfMessage;
    }
}
