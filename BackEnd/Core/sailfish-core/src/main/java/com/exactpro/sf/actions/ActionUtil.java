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
package com.exactpro.sf.actions;

import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.IActionContext;
import com.exactpro.sf.services.IService;
import com.exactpro.sf.services.ServiceStatus;

public class ActionUtil {
    public static <T extends IService> T getService(IActionContext actionContext, Class<T> clazz) {
		String serviceName = actionContext.getServiceName();
		Object serviceObject = actionContext.getServiceManager().getService(ServiceName.parse(serviceName));

        if(serviceObject == null) {
            throw new EPSCommonException("Service is not created: " + serviceName);
		}

        if(!clazz.isInstance(serviceObject)) {
            throw new EPSCommonException("Illegal type of service used: " + serviceObject.getClass().getCanonicalName() + ". Expected type is: " + clazz.getCanonicalName());
        }

        T service = clazz.cast(serviceObject);

        if (service.getStatus() != ServiceStatus.STARTED && service.getStatus() != ServiceStatus.WARNING) {
            throw new EPSCommonException("Service is not started: " + serviceName);
		}

        return service;
	}
}