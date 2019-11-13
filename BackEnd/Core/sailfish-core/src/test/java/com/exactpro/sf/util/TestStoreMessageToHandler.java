/*******************************************************************************
 *   Copyright 2009-2019 Exactpro (Exactpro Systems Limited)
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 ******************************************************************************/

package com.exactpro.sf.util;

import com.exactpro.sf.common.impl.messages.MapMessage;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.services.*;
import com.exactpro.sf.services.fake.FakeClientService;
import com.exactpro.sf.services.fake.FakeSession;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class TestStoreMessageToHandler extends AbstractTest {
    private static Set<String> processedMessageTypes;
    private static IMessage message;
    private static final String MESSAGE_NAME = "NEW_ORDER_SINGLE";
    private static ISession session;
    private static CollectorServiceHandler serviceHandler;
    private static ServiceHandlerRoute serviceHandlerRoute;

    @BeforeClass
    public static void beforeClassFunction() {
        session = new FakeSession(new FakeClientService());
        message = new MapMessage("FIX_4_4", MESSAGE_NAME);
        message.addField("Max1", "");
        message.addField("Max2", "");
        message.addField("Max3", "");
        processedMessageTypes = new HashSet<>();
        processedMessageTypes.add(MESSAGE_NAME);
        serviceHandlerRoute = ServiceHandlerRoute.get(true, false);
        serviceHandler = Mockito.mock(CollectorServiceHandler.class);
    }

    @Test
    public void testIncludeMessage() throws ServiceHandlerException {

        FilterServiceHandlerWrapper filterServiceHandlerWrapper = new FilterServiceHandlerWrapper(serviceHandler, processedMessageTypes, true);
        filterServiceHandlerWrapper.putMessage(session, serviceHandlerRoute, message);
        verify(serviceHandler).putMessage(session, serviceHandlerRoute, message);
    }

    @Test
    public void testExcludeMessage() throws ServiceHandlerException {

        FilterServiceHandlerWrapper filterServiceHandlerWrapper = new FilterServiceHandlerWrapper(serviceHandler, processedMessageTypes, false);
        filterServiceHandlerWrapper.putMessage(session, serviceHandlerRoute, message);
        verify(serviceHandler, never()).putMessage(session, serviceHandlerRoute, message);
    }


}
