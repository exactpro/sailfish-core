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
import java.util.Set;

import org.apache.commons.lang3.BooleanUtils;

import com.exactpro.sf.aml.CommonColumn;
import com.exactpro.sf.aml.CommonColumns;
import com.exactpro.sf.aml.CustomColumn;
import com.exactpro.sf.aml.CustomColumns;
import com.exactpro.sf.aml.generator.matrix.Column;
import com.exactpro.sf.common.impl.messages.DefaultMessageFactory;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.MessageUtil;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.configuration.ResourceAliases;
import com.exactpro.sf.scriptrunner.AbstractCaller;
import com.exactpro.sf.scriptrunner.actionmanager.ActionMethod;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.IActionContext;
import com.exactpro.sf.scriptrunner.actionmanager.actioncontext.impl.ActionContextWrapper;
import com.exactpro.sf.services.IInitiatorService;
import com.exactpro.sf.services.ISession;
import com.exactpro.sf.services.tcpip.TCPIPMessageHelper;
import com.google.common.collect.ImmutableSet;

/**
 * @author nikita.smirnov
 *
 */
@MatrixActions
@ResourceAliases({"ConnectivityActions"})
public class ConnectivityActions extends AbstractCaller {

    public final static Set<String> DEFAULT_UNCHECKED_FIELDS;

    static {
        DEFAULT_UNCHECKED_FIELDS = ImmutableSet.of(
                "header",
                "BeginString",
                "BodyLength",
                "MsgSeqNum",
                "MsgType",
                "SenderCompID",
                "TargetCompID",
                "PosDupFlag",
                "OrigSendingTime",
                "SendingTime",
                "CheckSum",
                "templateId",
                "ApplVerID",
                "SenderSubID",
                "trailer"
        );
    }

    private boolean autoConnect = true;

    @CommonColumns({
        @CommonColumn(Column.Reference),
        @CommonColumn(value = Column.ServiceName, required = true),
        @CommonColumn(Column.Timeout)
    })
    @ActionMethod
    public HashMap<?, ?> SendMessage(IActionContext actionContext, HashMap<?, ?> inputData) throws Exception {
        String serviceName = actionContext.getServiceName();
        actionContext.getLogger().info("[{}] started", serviceName);
        actionContext.getLogger().info("settings=[{}]", actionContext);

        IInitiatorService service = getClient(actionContext);
        ISession session = service.getSession();

        if(autoConnect && (session == null || session.isClosed())) {
            service.connect();
        }

        IMessage outgoingMessage = MessageUtil.convertToIMessage(inputData, DefaultMessageFactory.getFactory(),
                TCPIPMessageHelper.OUTGOING_MESSAGE_NAME_AND_NAMESPACE, TCPIPMessageHelper.OUTGOING_MESSAGE_NAME_AND_NAMESPACE);

        service.getSession().send(outgoingMessage);

        return MessageUtil.convertToHashMap(outgoingMessage);
    }

    @CommonColumns({
        @CommonColumn(Column.Reference),
        @CommonColumn(value = Column.ServiceName, required = true),
        @CommonColumn(value = Column.Timeout, required = true)
    })
    @ActionMethod
    public HashMap<?, ?> WaitMessage(IActionContext actionContext, HashMap<?, ?> mapFilter) throws Exception {
        IInitiatorService service = getClient(actionContext);

        IMessage incomingMessage = MessageUtil.convertToIMessage(mapFilter, DefaultMessageFactory.getFactory(), TCPIPMessageHelper.INCOMING_MESSAGE_NAME_AND_NAMESPACE, TCPIPMessageHelper.INCOMING_MESSAGE_NAME_AND_NAMESPACE);

        if (actionContext.getUncheckedFields() == null || actionContext.getUncheckedFields().isEmpty()) {
            ActionContextWrapper actionContextWrapper = new ActionContextWrapper(actionContext);
            actionContextWrapper.setUncheckedFields(DEFAULT_UNCHECKED_FIELDS);
            actionContext = actionContextWrapper;
        }

        incomingMessage = service.receive(actionContext, incomingMessage);

        return MessageUtil.convertToHashMap(incomingMessage);
    }

    @CommonColumns({
        @CommonColumn(Column.Reference),
        @CommonColumn(value = Column.ServiceName, required = true),
        @CommonColumn(Column.Timeout)
    })
    @CustomColumns({
        @CustomColumn("RawData")
    })
    @ActionMethod
    public IMessage SendRawData(IActionContext actionContext, HashMap<?, ?> inputData) throws Exception {
        if (!inputData.containsKey("RawData"))
            throw new Exception("RawData column hasn't been specified in current action");

        String serviceName = actionContext.getServiceName();
        actionContext.getLogger().info("[{}] started", serviceName);
        actionContext.getLogger().info("settings=[{}]", actionContext);

        IInitiatorService initiatorService = ActionUtil.getService(actionContext, IInitiatorService.class);
        ISession session = initiatorService.getSession();

        if (session == null) {
            actionContext.getLogger().error("Can not get session from service:{} (session is null)", serviceName);
            throw new EPSCommonException("Can not get session from service:" + serviceName + "(session is null)");
        }

        return session.send(inputData.get("RawData"));
    }

    @CustomColumns({
        @CustomColumn(value="AutoConnect", required=true)
    })
    @ActionMethod
    public void Configure(IActionContext actionContext, HashMap<?, ?> inputData) throws Exception {

        autoConnect = BooleanUtils.toBoolean(inputData.get("AutoConnect").toString());
    }

    private static IInitiatorService getClient(IActionContext actionContext) {
        return ActionUtil.getService(actionContext, IInitiatorService.class);
    }
}
