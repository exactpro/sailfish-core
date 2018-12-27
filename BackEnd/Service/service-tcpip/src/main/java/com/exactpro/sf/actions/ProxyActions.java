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

import com.exactpro.sf.aml.CommonColumn;
import com.exactpro.sf.aml.CommonColumns;
import com.exactpro.sf.aml.generator.matrix.Column;
import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.scriptrunner.AbstractCaller;
import com.exactpro.sf.scriptrunner.actionmanager.ActionMethod;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.IActionContext;
import com.exactpro.sf.services.IServiceSettings;
import com.exactpro.sf.services.tcpip.TCPIPProxy;

public class ProxyActions extends AbstractCaller {
    @CommonColumns({
        @CommonColumn(value = Column.ServiceName, required = true)
    })
    @ActionMethod
    public void reinit(IActionContext actionContext) throws InterruptedException {
        ServiceName serviceName = ServiceName.parse(actionContext.getServiceName());

        try {
            IServiceSettings serviceSettings = actionContext.getServiceManager().getServiceSettings(serviceName);
            TCPIPProxy tcpipProxy = ActionUtil.getService(actionContext, TCPIPProxy.class);
            synchronized (tcpipProxy) {
                tcpipProxy.reinit(serviceSettings);
            }
        } catch (Exception e) {
            throw new EPSCommonException(e);
        }
    }
}
