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
package com.exactpro.sf.testwebgui.restapi.editor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.exactpro.sf.aml.script.CheckPoint;
import com.exactpro.sf.common.impl.messages.BaseMessage;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.scriptrunner.ScriptContext;
import com.exactpro.sf.services.IInitiatorService;
import com.exactpro.sf.services.IService;
import com.exactpro.sf.services.IServiceHandler;
import com.exactpro.sf.services.ISession;
import com.exactpro.sf.services.ServiceHandlerRoute;

public class CollectorReceivedMessageProvider implements IReceivedMessageProvider {

	private CheckPoint testCaseStartCP;

	private final List<UnexpectedMessagesContainer> unexpected = new ArrayList<>();

	@Override
	public List<UnexpectedMessagesContainer> getUnexpectedContainer() {
		return unexpected;
	}

	public void startRecording(ScriptContext context, String testCaseName) {
		// Similar code as CommonActions.GetCheckPoint.

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

        testCaseStartCP = checkPoint;

        // context.getTestCaseName() will be set in generated code... it is not available here
        unexpected.add(new UnexpectedMessagesContainer(testCaseName));
	}


	public void stopRecording(ScriptContext context) {

		Set<String> usedServicesNames = context.getEnvironmentManager().getConnectionManager().getUsedServices();

		Set<IMessage> receivedFromWaitActions = unwrap(context.getReceivedMessages());
		Set<IMessage> unexpectedMessages = new HashSet<>();

		CheckPoint checkPoint = testCaseStartCP;

		for (String serviceName : usedServicesNames) {

			IService service = context.getEnvironmentManager().getConnectionManager().getService(ServiceName.parse(serviceName));

			if (!(service instanceof IInitiatorService)) {
				continue;
			}

            IServiceHandler handler = service.getServiceHandler();

			ISession isession = ((IInitiatorService)service).getSession();
			if (isession != null) {
				List<IMessage> messages = handler.getMessages(isession, ServiceHandlerRoute.FROM_APP, checkPoint);

				for(IMessage message : messages) {
					if (!receivedFromWaitActions.contains(message)) {
						unexpectedMessages.add(unwrap(message));
					}
				}
			}
		}

		UnexpectedMessagesContainer container = unexpected.get(unexpected.size() - 1);

		container.getAllMessages().addAll(receivedFromWaitActions);
		container.getAllMessages().addAll(unexpectedMessages);
		container.getReceivedMessages().addAll(receivedFromWaitActions);
		container.getUnexpectedMessages().addAll(unexpectedMessages);
	}

	private static Set<IMessage> unwrap(Set<Object> messages) {
		Set<IMessage> result = new HashSet<>();
		for (Object message : messages) {
			result.add(unwrap(message));
		}
		return result;
	}

	private static IMessage unwrap(Object message) {
		if (message instanceof BaseMessage) {
			return ((BaseMessage) message).getMessage();
		} else if (message instanceof IMessage) {
			return (IMessage) message;
//		} else if (message instanceof quickfix.Message){
//			throw new EPSCommonException("QuickFixJ messages are unsupported. Switch FIX-service to AML3-mode");
		} else {
			throw new EPSCommonException("Unknown message type: " + message.getClass());
		}

	}

}
