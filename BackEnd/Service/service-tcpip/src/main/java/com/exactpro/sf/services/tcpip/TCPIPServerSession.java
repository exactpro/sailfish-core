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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.util.SendMessageFailedException;
import com.exactpro.sf.services.ISession;

public class TCPIPServerSession implements ISession, Runnable {

	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName() + "@" + Integer.toHexString(hashCode()));
	private TCPIPServer server;
	private String name;
	private boolean loggedOn = false;

	public boolean isClosed = false;


	public TCPIPServerSession(TCPIPServer server) {
		this.server = server;
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
		return this.name;
	}

	@Override
	public IMessage send(Object message) throws InterruptedException {

        List<WriteFuture> futures = new ArrayList<>();
        Set<IoSession> errorSending = new HashSet<>();

        for (IoSession session : server.sessionMap.keySet()) {
            futures.add(session.write(message));
        }

        if (!futures.isEmpty()) {
            futures.get(futures.size() - 1).await(1000);
            for (WriteFuture future : futures) {
                if (!future.isDone() || !future.isWritten()) {
                    errorSending.add(future.getSession());
                }
            }
        }

        if (!errorSending.isEmpty()) {
            StringBuilder errors = new StringBuilder("For sessions: ");
            for (IoSession session : errorSending) {
                errors.append(session.toString() + "\n");
            }

            throw new SendMessageFailedException("Message wasn't send during 1 second." + errors.toString().trim());
        }

		if(message instanceof IMessage) {
			return (IMessage) message;
		}

		return null;
	}

	@Override
	public IMessage sendDirty(Object message) throws InterruptedException {
		return null;
	}

	@Override
	public void close() {
		this.isClosed = true;
	}

	@Override
	public boolean isClosed() {
		return this.isClosed;
	}

	@Override
	public boolean isLoggedOn() {
		return this.loggedOn;
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

		return (anotherServerSession.server != null && anotherServerSession.server.equals(this.server)
				&& anotherServerSession.name != null && anotherServerSession.name.equals(this.name)
				&& anotherServerSession.isClosed == this.isClosed
				&& anotherServerSession.loggedOn == this.loggedOn);
	}

	@Override
	public int hashCode() {
		if(server == null || name == null) {
			return super.hashCode();
		}

		return server.hashCode() + 3 * name.hashCode();
	}
}
