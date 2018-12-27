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

import com.exactpro.sf.common.impl.messages.BaseMessage;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.aml.CommonColumn;
import com.exactpro.sf.aml.CommonColumns;
import com.exactpro.sf.aml.Description;
import com.exactpro.sf.aml.generator.matrix.Column;
import com.exactpro.sf.aml.script.actions.WaitAction;
import com.exactpro.sf.comparison.ComparisonResult;
import com.exactpro.sf.configuration.ResourceAliases;
import com.exactpro.sf.scriptrunner.AbstractCaller;
import com.exactpro.sf.scriptrunner.ScriptRunException;
import com.exactpro.sf.scriptrunner.StatusType;
import com.exactpro.sf.scriptrunner.actionmanager.ActionMethod;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.IActionContext;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.IActionReport;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.impl.ActionContextWrapper;
import com.exactpro.sf.services.itch.IITCHClient;
import com.exactpro.sf.services.itch.ITCHTcpClient;
import com.exactpro.sf.services.itch.multicast.ITCHMulticastServer;
import com.exactpro.sf.services.itch.multicast.ITCHMulticastUDPSession;

/**
 *
 * @author dmitry.guriev
 *
 */
@ResourceAliases({"ITCHCommonActions"})
public class ITCHCommonActions extends AbstractCaller {
    protected final TestActions testActions;

    public ITCHCommonActions() {
        testActions = new TestActions();
    }

    @Deprecated
    @Description("This function is deprecated, please use the reconnectService() method from TestActions.")
    @CommonColumns({
        @CommonColumn(value = Column.ServiceName, required = true)
    })
    @ActionMethod
    public void ITCH_Connect(IActionContext actionContext) throws InterruptedException {

        IITCHClient client = getClient(actionContext);

        if (client instanceof ITCHTcpClient) {
            ActionContextWrapper contextWrapper = new ActionContextWrapper(actionContext);
            contextWrapper.setTimeout(5000);
            testActions.reconnectService(contextWrapper);
		} else {
			throw new ScriptRunException("[" + actionContext.getServiceName()
					+"]  Could not connect to channel, " +
                    "service is not ITCHTcpClient ");
		}

		actionContext.getLogger().info("[{}] Successfully connected to channel", actionContext.getServiceName());
	}

    @CommonColumns({
        @CommonColumn(value = Column.ServiceName, required = true)
    })
    @ActionMethod
    public void ITCH_Disconnect(IActionContext actionContext) throws InterruptedException {

        IITCHClient client = getClient(actionContext);

        if (client instanceof ITCHTcpClient) {
            ITCHTcpClient tcpClient = (ITCHTcpClient) client;
			try {
                if(tcpClient.isConnected()) {
                    tcpClient.disconnect();
                }
			} catch (InterruptedException e) {
				throw e;
			} catch (Exception e)
			{
				throw new ScriptRunException("[" + actionContext.getServiceName()
					+"]  Could not disconnect from channel ");
			}
		} else {
			throw new ScriptRunException("[" + actionContext.getServiceName()
					+"]  Could not connect to channel, " +
                    "service is not ITCHTcpClient ");
		}

		actionContext.getLogger().info("[{}] Successfully disconnected from channel", actionContext.getServiceName());

		IActionReport report = actionContext.getReport();
        report.createVerification(StatusType.PASSED, "Disconnect from ITCH server", "", "");
	}

    @CommonColumns({
        @CommonColumn(value = Column.ServiceName, required = true)
    })
    @ActionMethod
    public void ITCH_IsConnected(IActionContext actionContext)
            throws Exception
    {
        IITCHClient client = getClient(actionContext);
        String actionId = actionContext.getId();
        if (actionId != "") actionId += " ";
        IActionReport report = actionContext.getReport();
        String verificationName = actionId + String.format("Checking whether the service [%s] is connected", client.getName());
        ComparisonResult cr = new ComparisonResult("ITCH_IsConnected");
        StatusType status = null;
        String actual = null;
        if (client.isConnected()) {
            status = StatusType.PASSED;
            actual = "true";
        }
        else {
            status = StatusType.FAILED;
            actual = "false";
        }
        cr.setStatus(status);
        cr.setActual(actual);
        cr.setExpected("true");
        report.createVerification(status, verificationName, "", "", cr);

        if(status == StatusType.FAILED) {
            throw new EPSCommonException(String.format("Service [%s] is disconnected", client.getName()));
        }
    }

    @CommonColumns({
        @CommonColumn(value = Column.ServiceName, required = true)
    })
    @ActionMethod
    public void ITCH_IsDisconnected(IActionContext actionContext)
            throws Exception
    {
        IITCHClient client = getClient(actionContext);
        String actionId = actionContext.getId();
        if (actionId != "") actionId += " ";
        IActionReport report = actionContext.getReport();
        String verificationName = actionId + String.format("Checking whether the service [%s] is disconnected", client.getName());
        ComparisonResult cr = new ComparisonResult("ITCH_IsDisconnected");
        StatusType status = null;
        String actual = null;
        if(!client.isConnected()) {
            status = StatusType.PASSED;
            actual = "true";
        }
        else {
            status = StatusType.FAILED;
            actual = "false";
        }
        cr.setStatus(status);
        cr.setActual(actual);
        cr.setExpected("true");
        report.createVerification(status, verificationName, "", "", cr);

        if(status == StatusType.FAILED) {
            throw new EPSCommonException(String.format("Service [%s] is connected", client.getName()));
        }
    }

    @CommonColumns({
        @CommonColumn(value = Column.ServiceName, required = true),
        @CommonColumn(value = Column.Timeout, required = true)
    })
    @ActionMethod
    public void ITCH_WaitDisconnect(IActionContext actionContext)
        throws Exception
    {
        IITCHClient client = getClient(actionContext);
        long sleepTime = actionContext.getTimeout();
        long deadlineTime = System.currentTimeMillis() + sleepTime;
        IActionReport report = actionContext.getReport();
        do {
            if(!client.isConnected()) {
                report.createVerification(StatusType.PASSED, "ITCH client disconnected", "", "");
                return;
            }

        } while (deadlineTime > System.currentTimeMillis());

        report.createVerification(StatusType.FAILED, "ITCH client disconnected", "", "", null, null);

        throw new ScriptRunException("[" + actionContext.getServiceName() +"] not disconnectd");
    }

    @CommonColumns({
        @CommonColumn(Column.CheckPoint),
        @CommonColumn(Column.DoublePrecision),
        @CommonColumn(value = Column.MessageCount, required = true),
        @CommonColumn(value = Column.ServiceName, required = true),
        @CommonColumn(value = Column.Timeout, required = true)
    })
    @ActionMethod
    public void ITCH_CountAdminTotal(IActionContext actionContext)
        throws Exception
    {
        WaitAction.countMessages(actionContext, null, false);
    }

    @CommonColumns({
        @CommonColumn(Column.CheckPoint),
        @CommonColumn(Column.DoublePrecision),
        @CommonColumn(value = Column.MessageCount, required = true),
        @CommonColumn(value = Column.ServiceName, required = true),
        @CommonColumn(value = Column.Timeout, required = true)
    })
    @ActionMethod
    public void ITCH_CountApplicationTotal(IActionContext actionContext)
        throws Exception
    {
        WaitAction.countMessages(actionContext, null, true);
    }

    @CommonColumns({
        @CommonColumn(Column.CheckPoint),
        @CommonColumn(Column.DoublePrecision),
        @CommonColumn(value = Column.MessageCount, required = true),
        @CommonColumn(value = Column.ServiceName, required = true),
        @CommonColumn(value = Column.Timeout, required = true)
    })
    @ActionMethod
    public void ITCH_CountAdminTotalWithoutUH(IActionContext actionContext)
        throws Exception
    {
            WaitAction.countMessages(actionContext);
    }


    protected BaseMessage send(IActionContext actionContext, BaseMessage message) throws InterruptedException
	{
        IITCHClient client = getClient(actionContext);

        if (!(client instanceof ITCHTcpClient)) {
			throw new ScriptRunException("[" + actionContext.getServiceName()
					+ "]  Could not send "+message.getClass().getSimpleName()+", "
                    + "service is not ITCHTcpClient ");
		}

        ITCHTcpClient tcpClient = (ITCHTcpClient) client;

		if ( tcpClient.getSession() == null )
		{
			throw new ScriptRunException("[" + actionContext.getServiceName() +"] "
					+ "Not connected;" );
		}

		if (actionContext.getTimeout() > 0) {
			Thread.sleep(actionContext.getTimeout());
		}

		try
		{
			tcpClient.sendMessage( message.getMessage());
		}
		catch (Exception err)
		{
            if(err instanceof InterruptedException){
                throw (InterruptedException)err;
            }
			throw new ScriptRunException( "[" + actionContext.getServiceName() +"] "
					+ "Can't send "+message.getClass().getSimpleName()+" to channel;", err );
		}
		return message;
	}

    protected IMessage sendMulti(IActionContext actionContext, BaseMessage message) throws InterruptedException {
        ITCHMulticastUDPSession session;
        ITCHMulticastServer service = ActionUtil.getService(actionContext, ITCHMulticastServer.class);
        session = (ITCHMulticastUDPSession) service.getSession();
        if(session == null)
            throw new ScriptRunException("[" + actionContext.getServiceName() +"] "
                    + "not runned;");
        return session.send(message.getMessage());

    }

	protected IMessage receive(
			IActionContext actionContext,
			BaseMessage messageFilter)
	throws Exception
	{
		boolean fromApp = !messageFilter.isAdmin();
		return WaitAction.waitForMessage(actionContext, messageFilter.getMessage(), fromApp);
	}

	protected void countMessages(
			IActionContext actionContext,
			BaseMessage message)
	throws Exception
	{
		boolean isApp = !message.isAdmin();
		WaitAction.countMessages(actionContext, message.getMessage(), isApp);
	}

    private IITCHClient getClient(IActionContext actionContext) {
        return ActionUtil.getService(actionContext, IITCHClient.class);
	}


}
