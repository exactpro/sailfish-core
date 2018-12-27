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

import java.util.Objects;

import org.slf4j.Logger;

import com.exactpro.sf.services.IService;
import com.exactpro.sf.services.IServiceMonitor;

import quickfix.Log;
import quickfix.LogFactory;
import quickfix.SessionID;

public class FIXLogFactory implements LogFactory 
{
	private final IService service;
	private final IServiceMonitor servMonitor;
	private final boolean logHeartbeats;
    private final LogFactory nativeLogFactory;
    private final Logger logger;

	public FIXLogFactory(LogFactory nativeLogFactory, boolean logHeartbeats, final IService service, final IServiceMonitor monitor, final Logger logger)
	{
		this.service = Objects.requireNonNull(service, "'Service' can't be null");
		
		this.servMonitor = Objects.requireNonNull(monitor, "'Service monitor' can't be null");
		
		this.logger = Objects.requireNonNull(logger, "'Logger' can't be null");

		this.nativeLogFactory = Objects.requireNonNull(nativeLogFactory, "'Native log factory' can't be null");

		this.logHeartbeats = logHeartbeats;
	}
	

	@Override
	public Log create() {
		throw new RuntimeException("not implemented");
	}

	
	@Override
	public Log create(SessionID sessionID) {
		return new FIXLog(this.nativeLogFactory.create(sessionID), this.logHeartbeats, this.service, this.servMonitor, this.logger);
	}

}
