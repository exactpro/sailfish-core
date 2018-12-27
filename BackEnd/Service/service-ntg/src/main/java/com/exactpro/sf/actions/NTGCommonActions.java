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
import com.exactpro.sf.configuration.ResourceAliases;
import com.exactpro.sf.scriptrunner.AbstractCaller;
import com.exactpro.sf.scriptrunner.ScriptRunException;
import com.exactpro.sf.scriptrunner.StatusType;
import com.exactpro.sf.scriptrunner.actionmanager.ActionMethod;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.IActionContext;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.IActionReport;
import com.exactpro.sf.services.ntg.NTGClient;

@ResourceAliases({"NTGCommonActions"})
public class NTGCommonActions extends AbstractCaller {
    @CommonColumns({
        @CommonColumn(value = Column.ServiceName, required = true)
    })
    @ActionMethod
    public void NAT_Connect(IActionContext actionContext) throws Exception
    {
        NTGClient client = getClient(actionContext);

        if(client.isConnected()) {
            return;
        }

        client.connect();
        long timeout = System.currentTimeMillis()+1000;
        while (timeout > System.currentTimeMillis() && false == client.isConnected())
        {
            Thread.sleep(10);
        }
    }

    @CommonColumns({
        @CommonColumn(value = Column.ServiceName, required = true)
    })
    @ActionMethod
    public void NAT_Disconnect(IActionContext actionContext) throws Exception
    {
        NTGClient client = getClient(actionContext);

        if(!client.isConnected()) {
            return;
        }

        client.disconnect();
        long timeout = System.currentTimeMillis()+1000;
        while (timeout > System.currentTimeMillis() && client.isConnected())
        {
            Thread.sleep(10);
        }
    }

    @CommonColumns({
        @CommonColumn(value = Column.ServiceName, required = true),
        @CommonColumn(value = Column.Timeout, required = true)
    })
    @ActionMethod
    public void NAT_WaitDisconnect (IActionContext actionContext)
        throws Exception
    {
        NTGClient client = getClient(actionContext);
        long sleepTime = actionContext.getTimeout();
        long deadlineTime = System.currentTimeMillis() + sleepTime;
        IActionReport report = actionContext.getReport();
        do {
            if (!client.isConnected()) {
                report.createVerification(StatusType.PASSED, "NTG client disconnected", "", "");
                return;
            }

        } while (deadlineTime > System.currentTimeMillis());

        report.createVerification(StatusType.FAILED, "NTG client disconnected", "", "", null, null);

        throw new ScriptRunException("[" + actionContext.getServiceName() +"] not disconnected");
    }

    @CommonColumns({
        @CommonColumn(value = Column.ServiceName, required = true)
    })
    @ActionMethod
    public void NAT_CheckConnected(IActionContext actionContext) throws Exception
    {
        NTGClient client = getClient(actionContext);
        if (!client.isConnected()) {
            throw new Exception("Client is not connected");
        }
    }

    private static NTGClient getClient(IActionContext actionContext)
    {
        return ActionUtil.getService(actionContext, NTGClient.class);
    }
}
