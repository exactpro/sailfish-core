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

import static java.lang.String.join;
import static java.lang.System.lineSeparator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.common.util.SendMessageFailedException;
import com.exactpro.sf.services.ISession;
import com.exactpro.sf.services.util.CloseSessionException;

public class TCPIPServerSession implements ISession, Runnable {

    private final Logger logger = LoggerFactory.getLogger(getClass().getName() + "@" + Integer.toHexString(hashCode()));
    private final TCPIPServer server;
    private final long sendMessageTimeout;
    private final String name;
    private final boolean loggedOn;

    public boolean isClosed;


	public TCPIPServerSession(TCPIPServer server) {
		this.server = server;
		this.sendMessageTimeout = server.getSettings().getSendMessageTimeout();
		this.name = server.getName();
		this.loggedOn = true;
        if (logger.isInfoEnabled()) {
            logger.info("{} session was created", this.server.getName());
        }
	}

	@Override
	public void run () {
		while (!isClosed)
		{
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				logger.error(e.getMessage(), e);
			}
		}
        logger.debug("isClosed");
	}

	@Override
	public String getName() {
        return name;
	}

    @Override
    public IMessage send(Object message) throws InterruptedException {
        return send(message, sendMessageTimeout);
    }

    @Override
	public IMessage send(Object message, long timeout) throws InterruptedException {
        if (timeout < 1) {
            throw new EPSCommonException("Illegal timeout value: " + timeout);
        }
        List<WriteFuture> futures = new ArrayList<>();
        Set<IoSession> errorSending = new HashSet<>();

        for (IoSession session : server.sessionMap.keySet()) {
            futures.add(session.write(message));
        }

        if (!futures.isEmpty()) {
            futures.get(futures.size() - 1).await(timeout);
            for (WriteFuture future : futures) {
                if (!future.isDone() || !future.isWritten()) {
                    errorSending.add(future.getSession());
                }
            }
        }

        if (!errorSending.isEmpty()) {
            StringBuilder errors = new StringBuilder("For sessions: ");
            for (IoSession session : errorSending) {
                errors.append(session + "\n");
            }

            throw new SendMessageFailedException("Message wasn't send during 1 second." + errors.toString().trim());
        }

        return message instanceof IMessage ? (IMessage)message : null;
    }

	@Override
	public IMessage sendDirty(Object message) throws InterruptedException {
		return null;
	}

	@Override
	public void close() {
        Collection<String> errors = new HashSet<>();

        server.sessionMap.forEach((ioSession, minaSession) -> {
            try {
                minaSession.close();
            } catch (RuntimeException e) {
                logger.warn("Failed to close session: {}", minaSession);
                errors.add(ioSession.toString());
            }
        });

        this.isClosed = true;

        if (!errors.isEmpty()) {
            throw new CloseSessionException("Failed to close following client sessions: " + lineSeparator() + join(lineSeparator(), errors));
        }
    }

	@Override
	public boolean isClosed() {
        return isClosed;
	}

	@Override
	public boolean isLoggedOn() {
        return loggedOn;
	}

	@Override
	public boolean equals(Object obj) {
		if(this == obj) {
			return true;
		}

		if(obj == null) {
			return false;
		}

		if(!(obj instanceof TCPIPServerSession)) {
			return false;
		}

		TCPIPServerSession anotherServerSession = (TCPIPServerSession) obj;

        return anotherServerSession.server != null && anotherServerSession.server.equals(server)
                && anotherServerSession.name != null && anotherServerSession.name.equals(name)
                && anotherServerSession.isClosed == isClosed
                && anotherServerSession.loggedOn == loggedOn;
	}

	@Override
	public int hashCode() {
        return server == null || name == null ? super.hashCode() : server.hashCode() + 3 * name.hashCode();
    }
}
