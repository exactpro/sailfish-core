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

import java.util.HashMap;
import java.util.Map.Entry;

import com.exactpro.sf.aml.CommonColumn;
import com.exactpro.sf.aml.CommonColumns;
import com.exactpro.sf.aml.generator.matrix.Column;
import com.exactpro.sf.aml.script.actions.WaitAction;
import com.exactpro.sf.common.impl.messages.BaseMessage;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.configuration.ResourceAliases;
import com.exactpro.sf.messages.testaml.ApplicationMessageRequest;
import com.exactpro.sf.messages.testaml.ArrayMessage;
import com.exactpro.sf.messages.testaml.ExecutionReportFIX;
import com.exactpro.sf.messages.testaml.LoginRequest;
import com.exactpro.sf.messages.testaml.LoginResponse;
import com.exactpro.sf.messages.testaml.NewOrderSingle;
import com.exactpro.sf.messages.testaml.OrderAcknowledgement;
import com.exactpro.sf.messages.testaml.OrderCancelReplaceRequest;
import com.exactpro.sf.messages.testaml.OrderEntry;
import com.exactpro.sf.messages.testaml.ReplayRequest;
import com.exactpro.sf.messages.testaml.ReplayResponse;
import com.exactpro.sf.messages.testaml.SimpleMessage;
import com.exactpro.sf.messages.testaml.SnapshotComplete;
import com.exactpro.sf.messages.testaml.SnapshotRequest;
import com.exactpro.sf.messages.testaml.SnapshotResponse;
import com.exactpro.sf.messages.testaml.Statistics;
import com.exactpro.sf.messages.testaml.TradeConfirmation;
import com.exactpro.sf.messages.testaml.UserConnection;
import com.exactpro.sf.scriptrunner.AbstractCaller;
import com.exactpro.sf.scriptrunner.actionmanager.ActionMethod;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.IActionContext;
import com.exactpro.sf.services.fake.FakeClientService;

/**
 * FAKE actions for FAKEClientService
 *
 * @author Andrey Senov
 *
 *
 */

@MatrixActions
@ResourceAliases({"FakeActions"})
public class FakeActions extends AbstractCaller
{
	@CommonColumns({
        @CommonColumn(Column.CheckPoint),
        @CommonColumn(Column.DoublePrecision),
        @CommonColumn(value = Column.MessageCount, required = true),
        @CommonColumn(value = Column.ServiceName, required = true),
        @CommonColumn(Column.SystemPrecision),
        @CommonColumn(value = Column.Timeout, required = true)
    })
	@ActionMethod
	public void FAKE_CountExecutionReportFIX(IActionContext actionContext, ExecutionReportFIX message) throws Exception
	{
		count(actionContext, message);
	}

	@CommonColumns({
        @CommonColumn(Column.Reference),
        @CommonColumn(value = Column.ServiceName, required = true),
        @CommonColumn(Column.Timeout)
    })
	@ActionMethod
	public NewOrderSingle FAKE_SendNewOrderSingle(IActionContext actionContext, NewOrderSingle message) throws Exception
	{
		return (NewOrderSingle) send(actionContext, message);
	}

	@CommonColumns({
        @CommonColumn(Column.Reference),
        @CommonColumn(value = Column.ServiceName, required = true),
        @CommonColumn(Column.Timeout)
    })
	@ActionMethod
	public OrderCancelReplaceRequest FAKE_SendOrderCancelReplaceRequest(IActionContext actionContext, OrderCancelReplaceRequest message) throws Exception
	{
		return (OrderCancelReplaceRequest) send(actionContext, message);
	}

	@CommonColumns({
        @CommonColumn(Column.Reference),
        @CommonColumn(value = Column.ServiceName, required = true),
        @CommonColumn(Column.Timeout)
    })
	@ActionMethod
	public ApplicationMessageRequest FAKE_SendApplicationMessageRequest(IActionContext actionContext, ApplicationMessageRequest message) throws Exception
	{
		return (ApplicationMessageRequest) send(actionContext, message);
	}

	@CommonColumns({
        @CommonColumn(Column.Reference),
        @CommonColumn(value = Column.ServiceName, required = true),
        @CommonColumn(Column.Timeout)
    })
	@ActionMethod
	public OrderEntry FAKE_SendOrderEntry(IActionContext actionContext, OrderEntry message) throws Exception
	{
		return (OrderEntry) send(actionContext, message);
	}

	@CommonColumns({
        @CommonColumn(Column.Reference),
        @CommonColumn(value = Column.ServiceName, required = true),
        @CommonColumn(Column.Timeout)
    })
	@ActionMethod
	public TradeConfirmation FAKE_SendTradeConfirmation(IActionContext actionContext, TradeConfirmation message) throws Exception
	{
		return (TradeConfirmation) send(actionContext, message);
	}

	@CommonColumns({
        @CommonColumn(Column.Reference),
        @CommonColumn(value = Column.ServiceName, required = true),
        @CommonColumn(Column.Timeout)
    })
	@ActionMethod
	public UserConnection FAKE_SendUserConnection(IActionContext actionContext, UserConnection message) throws Exception
	{
		return (UserConnection) send(actionContext, message);
	}

	@CommonColumns({
        @CommonColumn(Column.Reference),
        @CommonColumn(value = Column.ServiceName, required = true),
        @CommonColumn(Column.Timeout)
    })
	@ActionMethod
	public LoginRequest FAKE_LoginRequest(IActionContext actionContext, LoginRequest message) throws Exception
	{
		return (LoginRequest) send(actionContext, message);
	}

	@CommonColumns({
        @CommonColumn(Column.Reference),
        @CommonColumn(value = Column.ServiceName, required = true),
        @CommonColumn(Column.Timeout)
    })
	@ActionMethod
	public ReplayRequest FAKE_ReplayRequest(IActionContext actionContext, ReplayRequest message) throws Exception
	{
		return (ReplayRequest) send(actionContext, message);
	}

	@CommonColumns({
        @CommonColumn(Column.Reference),
        @CommonColumn(value = Column.ServiceName, required = true),
        @CommonColumn(Column.Timeout)
    })
	@ActionMethod
	public SnapshotRequest FAKE_SnapshotRequest(IActionContext actionContext, SnapshotRequest message) throws Exception
	{
		return (SnapshotRequest) send(actionContext, message);
	}

	@CommonColumns({
        @CommonColumn(Column.Reference),
        @CommonColumn(value = Column.ServiceName, required = true),
        @CommonColumn(Column.Timeout)
    })
	@ActionMethod
	public Statistics FAKE_WaitStatistics(IActionContext actionContext, Statistics message) throws Exception
	{
		IMessage im = receive(actionContext, message);
		return new Statistics(im);
	}

	@CommonColumns({
        @CommonColumn(Column.Reference),
        @CommonColumn(value = Column.ServiceName, required = true),
        @CommonColumn(Column.Timeout)
    })
	@ActionMethod
	public ExecutionReportFIX FAKE_WaitExecutionReportFIX(IActionContext actionContext, ExecutionReportFIX message) throws Exception
	{
		IMessage im = receive(actionContext, message);
		return new ExecutionReportFIX(im);
	}

	@CommonColumns({
        @CommonColumn(Column.Reference),
        @CommonColumn(value = Column.ServiceName, required = true),
        @CommonColumn(Column.Timeout)
    })
	@ActionMethod
	public LoginResponse FAKE_WaitLoginResponse(IActionContext actionContext, LoginResponse message) throws Exception
	{
		IMessage im = receive(actionContext, message);
		return new LoginResponse(im);
	}

	@CommonColumns({
        @CommonColumn(Column.Reference),
        @CommonColumn(value = Column.ServiceName, required = true),
        @CommonColumn(Column.Timeout)
    })
	@ActionMethod
	public ReplayResponse FAKE_WaitReplayResponse(IActionContext actionContext, ReplayResponse message) throws Exception
	{
		IMessage im = receive(actionContext, message);
		return new ReplayResponse(im);
	}

	@CommonColumns({
        @CommonColumn(Column.Reference),
        @CommonColumn(value = Column.ServiceName, required = true),
        @CommonColumn(Column.Timeout)
    })
	@ActionMethod
	public SnapshotResponse FAKE_WaitSnapshotResponse(IActionContext actionContext, SnapshotResponse message) throws Exception
	{
		IMessage im = receive(actionContext, message);
		return new SnapshotResponse(im);
	}

	@CommonColumns({
        @CommonColumn(Column.Reference),
        @CommonColumn(value = Column.ServiceName, required = true),
        @CommonColumn(Column.Timeout)
    })
	@ActionMethod
	public SnapshotComplete FAKE_WaitSnapshotComplete(IActionContext actionContext, SnapshotComplete message) throws Exception
	{
		IMessage im = receive(actionContext, message);
		return new SnapshotComplete(im);
	}

	@CommonColumns({
        @CommonColumn(Column.Reference),
        @CommonColumn(value = Column.ServiceName, required = true),
        @CommonColumn(Column.Timeout)
    })
	@ActionMethod
	public OrderAcknowledgement FAKE_WaitOrderAcknowledgement(IActionContext actionContext, OrderAcknowledgement message) throws Exception
	{
		IMessage im = receive(actionContext, message);
		return new OrderAcknowledgement(im);
	}

	@CommonColumns({
        @CommonColumn(Column.Reference),
        @CommonColumn(value = Column.ServiceName, required = true),
        @CommonColumn(Column.Timeout)
    })
	@ActionMethod
    public SimpleMessage FAKE_WaitSimpleMessage(IActionContext actionContext, SimpleMessage message) throws Exception
    {
        IMessage im = receive(actionContext, message);
        return new SimpleMessage(im);
    }

	@CommonColumns({
        @CommonColumn(Column.Reference),
        @CommonColumn(value = Column.ServiceName, required = true),
        @CommonColumn(Column.Timeout)
    })
	@ActionMethod
    public ArrayMessage FAKE_WaitArrayMessage(IActionContext actionContext, ArrayMessage message) throws Exception
    {
        IMessage im = receive(actionContext, message);
        return new ArrayMessage(im);
    }

	@ActionMethod
	public HashMap<?, ?> FAKE_SendHashMap(IActionContext actionContext, HashMap<?, ?> hashMap) {

	    for (Entry<?, ?> entry : hashMap.entrySet()) {
	        System.out.printf("Name {} Value {}", entry.getKey(), entry.getValue());
        }
	    return hashMap;
	}

	private static BaseMessage send(IActionContext actionContext, BaseMessage message) throws Exception
	{
		getClient(actionContext).messageSent(message.getMessage());

		return message;
	}

	private static IMessage receive(IActionContext actionContext, BaseMessage message) throws Exception
	{
		getClient(actionContext).messageReceived(message.getMessage());

		return (IMessage) WaitAction.waitForMessage(actionContext, message.getMessage(), true);
	}

	private static void count(IActionContext actionContext, BaseMessage message) throws Exception
	{
		WaitAction.countMessages(actionContext, message.getMessage(), true);
	}

	private static FakeClientService getClient(IActionContext actionContext)
	{
		return ActionUtil.getService(actionContext, FakeClientService.class);
	}
}
