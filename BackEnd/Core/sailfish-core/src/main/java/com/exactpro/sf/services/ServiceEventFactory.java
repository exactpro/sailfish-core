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

import java.util.Arrays;

import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.services.ServiceEvent.Level;
import com.exactpro.sf.services.ServiceEvent.Type;

public class ServiceEventFactory {

	public static ServiceEvent createEventInfo(ServiceName serviceName, Type type, String message, String details) {
		return new ServiceEvent(serviceName, Level.INFO, type, message, details);
	}

	public static ServiceEvent createEventDebug(ServiceName serviceName, Type type, String message, String details) {
		return new ServiceEvent(serviceName, Level.DEBUG, type, message, details);
	}

	public static ServiceEvent createEventWarn(ServiceName serviceName, Type type, String message, String details) {
		return new ServiceEvent(serviceName, Level.WARN, type, message, details);
	}

	public static ServiceEvent createEventError(ServiceName serviceName, Type type, String message, String details, Throwable e) {

        if(e != null) {
            details += Arrays.deepToString(e.getStackTrace());
            details = e.getMessage() + "\n" + details.replaceAll(",", "\n");
        }

		return new ServiceEvent(serviceName, Level.ERROR, type, message, details);
	}

	public static ServiceEvent createEventError(ServiceName serviceName, Type type, String message, String details) {
		return new ServiceEvent(serviceName, Level.ERROR, type, message, details);
	}

	public static ServiceStatusUpdateEvent createStatusUpdateEvent(ServiceName serviceName, Level level, Type type, String message,
																   String details, Throwable e) {
		if(e != null) {
            details += Arrays.deepToString(e.getStackTrace());
            details = e.getMessage() + "\n" + details.replaceAll(",", "\n");
		}
		return new ServiceStatusUpdateEvent(serviceName, level, type, message, details);
	}

    public static ServiceChangeUpdateEvent createServiceChangeUpdateEvent(ServiceName serviceName, Level level, Type type, String message,
																   String details, Throwable e) {
		if(e != null) {
            details += Arrays.deepToString(e.getStackTrace());
            details = e.getMessage() + "\n" + details.replaceAll(",", "\n");
		}
		return new ServiceChangeUpdateEvent(serviceName, level, type, message, details);
	}

}
