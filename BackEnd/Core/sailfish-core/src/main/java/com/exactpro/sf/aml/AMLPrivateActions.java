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
package com.exactpro.sf.aml;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.aml.script.CheckPoint;
import com.exactpro.sf.common.impl.messages.BaseMessage;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.scriptrunner.IScriptReport;
import com.exactpro.sf.scriptrunner.ScriptContext;
import com.exactpro.sf.scriptrunner.StatusDescription;
import com.exactpro.sf.scriptrunner.StatusType;
import com.exactpro.sf.services.IInitiatorService;
import com.exactpro.sf.services.IService;
import com.exactpro.sf.services.IServiceHandler;
import com.exactpro.sf.services.ISession;
import com.exactpro.sf.services.ServiceHandlerRoute;

public class AMLPrivateActions {

	private static final Logger logger = LoggerFactory.getLogger(AMLPrivateActions.class);

	public static final String CHECK_UNEXPECTED_ACTION_NAME = "CheckUnexpectedMessages";

	public static final String GET_CHECKPOINT_ACTION_NAME = "GetCheckPoint";

	private AMLPrivateActions() {

	}

	public static void CheckUnexpectedMessages(ScriptContext context) {

        context.setActionName("CheckUnexpectedMessage");
        context.getScriptProgress().incrementActions();

		Set<String> usedServicesNames = context.getEnvironmentManager().getConnectionManager().getUsedServices();

		Set<Object> receivedFromWaitActions = context.getReceivedMessages();
        Set<Object> preparedMessages = prepare(receivedFromWaitActions);

		CheckPoint checkPoint = context.getTCStartCheckPoint();

		logger.debug("Recieved size: {}", receivedFromWaitActions.size());

        Set<Object> unexpectedMessages = new HashSet<>();
		for(String serviceName : usedServicesNames) {

			IService service = context.getEnvironmentManager().getConnectionManager().getService(ServiceName.parse(serviceName));

			if (service instanceof IInitiatorService) {

                IServiceHandler handler = service.getServiceHandler();

                ISession isession = ((IInitiatorService) service).getSession();
                if (isession != null) {
                    List<IMessage> messages = handler.getMessages(isession, ServiceHandlerRoute.FROM_APP, checkPoint);

                    for(Object message : messages) {
                        if (!preparedMessages.contains(message)) {
                            unexpectedMessages.add(message);
                        }
                    }
                }
			}
		}
		IScriptReport report = context.getReport();
        report.createAction("CheckUnexpectedMessage", null, "CheckUnexpectedMessage", null, "Check unexpected message",
                            null, null, null, 0, Collections.emptyList());
        if (unexpectedMessages.isEmpty()) {
            report.closeAction(new StatusDescription(StatusType.PASSED, ""), null);
        } else {
            context.addUnexpectedMessages(unexpectedMessages);
            StringBuilder builder = new StringBuilder();
            for (Object msg:unexpectedMessages){
                builder.append("Message=[");
                builder.append(msg.toString());
                builder.append("]");
                builder.append("\t");
            }
            String unexpectedMessagesString = builder.toString().replace('\001', ',');
            report.closeAction(new StatusDescription(StatusType.FAILED, unexpectedMessagesString), null);
            throw new EPSCommonException("Unexpected message recived: " + unexpectedMessagesString);
        }
	}

    private static Set<Object> prepare(Set<Object> receivedFromWaitActions) {
        Set<Object> preparedMessage = new HashSet<>();
        for(Object o:receivedFromWaitActions){
            if(o instanceof BaseMessage){
                preparedMessage.add(((BaseMessage) o).getMessage());
            } else {
                preparedMessage.add(o);
            }
        }
        return preparedMessage;
    }

    // Similar code as CommonActions.GetCheckPoint.
	public static CheckPoint GetCheckPoint(ScriptContext context) {


        IService[] clients = context.getEnvironmentManager().getConnectionManager().getStartedServices();
        CheckPoint checkPoint = new CheckPoint();
        for (IService client : clients) 	{
            if (client instanceof IInitiatorService) {
                IServiceHandler handler = client.getServiceHandler();

                if (handler != null) {
                    ISession isession = ((IInitiatorService) client).getSession();
                    if (isession != null) {
                        handler.registerCheckPoint(isession, ServiceHandlerRoute.FROM_APP, checkPoint);
                        handler.registerCheckPoint(isession, ServiceHandlerRoute.FROM_ADMIN, checkPoint);
                    }
                }
            }
        }

        return checkPoint;
    }
}
