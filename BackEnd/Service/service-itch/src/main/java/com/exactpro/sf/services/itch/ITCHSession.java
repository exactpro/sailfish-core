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
package com.exactpro.sf.services.itch;

import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.configuration.ILoggingConfigurator;
import com.exactpro.sf.services.mina.MINASession;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.mina.core.session.IoSession;

import java.util.concurrent.atomic.AtomicInteger;

public class ITCHSession extends MINASession {
	private AtomicInteger seqnum = new AtomicInteger(0);

	private final byte marketDataGroup;

    public ITCHSession(ServiceName serviceName, IoSession session, ILoggingConfigurator logConfigurator, byte marketDataGroup) {
        super(serviceName, session, logConfigurator);
        this.marketDataGroup = marketDataGroup;
    }

	public int incrementAndGetSequenceNumber() {
		return seqnum.incrementAndGet();
	}

	public int getSequenceNumber() {
		return seqnum.get();
	}

	public byte getMarketDataGroup() {
		return marketDataGroup;
	}

    @Override
	public String toString() {
		return new ToStringBuilder(this).
				appendSuper(super.toString()).
				append("seqNum", seqnum).
				append("marketDataGroup", marketDataGroup).
				toString();
	}
}
