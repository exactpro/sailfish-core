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
package com.exactpro.sf.services.fake;

import java.util.UUID;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.services.ISession;

public class FakeSession implements ISession {
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName()  + "@" + Integer.toHexString(hashCode()));
	private final String name;

	private volatile boolean closed;
	private volatile FakeClientService fakeService;

	public FakeSession(FakeClientService fakeService)
	{
		this.fakeService = fakeService;
		this.name = UUID.randomUUID().toString();
		this.closed = false;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public IMessage send(Object message)
	{
		try
		{
			this.fakeService.messageSent((IMessage)message);
		}
		catch (Exception e)
		{
			logger.error(e.getMessage(), e);
		}

		return (IMessage) message;
	}

	@Override
	public IMessage sendDirty(Object message) {
		logger.error("This service not support send dirty message. Message will be send usual method");
		return send(message);
	}

	@Override
	public void close()
	{
		closed = true;
	}

	@Override
	public boolean isClosed()
	{
		return closed;
	}

	@Override
	public boolean isLoggedOn()
	{
		return !closed;
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(this).append("name", name)
				                          .append("closed", closed)
				                          .toString();
	}
}
