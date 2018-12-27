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

import java.io.Closeable;
import java.io.IOException;
import java.util.Objects;

import org.slf4j.Logger;

import com.exactpro.sf.services.IService;
import com.exactpro.sf.services.IServiceMonitor;
import com.exactpro.sf.services.ServiceEvent;
import com.exactpro.sf.services.ServiceEventFactory;

import quickfix.Log;

public class FIXLog implements Log, Closeable  {

	private final IService service;
	private final IServiceMonitor servMonitor;
	private final Logger logger;
	private final Log nativeLog;

	private boolean INFO_ENABLE;
	private boolean ERROR_ENABLE;

	public FIXLog(Log nativeLog, boolean logHeartbeats, final IService service, final IServiceMonitor monitor, final Logger logger) {
	    this.nativeLog = Objects.requireNonNull(nativeLog, "'Native log' can't be null");
		this.service = Objects.requireNonNull(service, "'Service' can't be null");
		this.servMonitor = Objects.requireNonNull(monitor, "'Service monitor' can't be null");
		this.logger = logger;
		INFO_ENABLE = logger.isInfoEnabled();
		ERROR_ENABLE = logger.isErrorEnabled();
	}

	@Override
	public void clear() {
        nativeLog.clear();
	}

	@Override
	public void onEvent(String text) {
		ServiceEvent event = ServiceEventFactory.createEventInfo(service.getServiceName(), ServiceEvent.Type.INFO, text, null);
		if(INFO_ENABLE) {
			logger.info(text);
		}
        nativeLog.onEvent(text);
		servMonitor.onEvent(event);
	}

    @Override
    public void onErrorEvent(String text) {
        ServiceEvent event = ServiceEventFactory.createEventError(service.getServiceName(), ServiceEvent.Type.ERROR, text, null);
		if(ERROR_ENABLE) {
			logger.error(text);
		}
        nativeLog.onErrorEvent(text);
        servMonitor.onEvent(event);
    }

    @Override
	public void onIncoming(String message) {
        nativeLog.onIncoming(message);
	}

	@Override
	public void onOutgoing(String message) {
        nativeLog.onOutgoing(message);
	}

    @Override
    public void close() throws IOException {
        if (this.nativeLog instanceof Closeable) {
            ((Closeable)this.nativeLog).close();
        }
    }

}
