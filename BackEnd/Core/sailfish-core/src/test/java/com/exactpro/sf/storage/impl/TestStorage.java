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
package com.exactpro.sf.storage.impl;

import java.io.FileNotFoundException;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.exactpro.sf.common.impl.messages.MapMessage;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.MsgMetaData;
import com.exactpro.sf.configuration.workspace.WorkspaceStructureException;
import com.exactpro.sf.storage.IMessageStorage;
import com.exactpro.sf.storage.MessageRow;
import com.exactpro.sf.storage.ScriptRun;
import com.exactpro.sf.util.AbstractTest;

@Ignore //Are you sure testing DB as unit test?
public class TestStorage extends AbstractTest
{

	@Test
	public void testMessageStoring() throws WorkspaceStructureException, FileNotFoundException
	{
		IMessageStorage defMsgStorage = new DatabaseMessageStorage(null, null, null);

		ScriptRun scriptRun = defMsgStorage.openScriptRun("Example", "ExampleDescription");

		IMessage message = new MapMessage("FIX_4_4", "NEW_ORDER_SINGLE");
		MsgMetaData msgMetaData = message.getMetaData();

		msgMetaData.setAdmin(false);
		msgMetaData.setFromService("FIX_FROM_BROKER");
		msgMetaData.setToService("FIX_TO_BROKER");

		message.addField("Max1", "");
		message.addField("Max2", "");
		message.addField("Max3", "");

		//FIXME:
		defMsgStorage.storeMessage(message);

		defMsgStorage.closeScriptRun(scriptRun);

        MessageRow storedMessage  = defMsgStorage.getMessages(0, 1, "").iterator().next();

        Assert.assertEquals(msgMetaData.getFromService(), storedMessage.getFrom());
        Assert.assertEquals(msgMetaData.getToService(), storedMessage.getTo());
        Assert.assertEquals(msgMetaData.getMsgName(), storedMessage.getMsgName());
        Assert.assertEquals(msgMetaData.getMsgNamespace(), storedMessage.getMsgNamespace());
    }

	@Test
	public void testComplexMessageStoring() throws WorkspaceStructureException, FileNotFoundException
	{
		IMessageStorage defMsgStorage = new DatabaseMessageStorage(null, null, null);

		ScriptRun scriptRun = defMsgStorage.openScriptRun("Example", "ExampleDescription");

		IMessage message = new MapMessage("FIX_5_0", "SPEC");
		MsgMetaData msgMetaData = message.getMetaData();

		msgMetaData.setAdmin(false);
		msgMetaData.setFromService("FIX_FROM_BROKER");
		msgMetaData.setToService("FIX_TO_BROKER");

		message.addField("Max1", "");
		message.addField("Max2", "");
		message.addField("Max3", "");

		IMessage subMessage = new MapMessage("FIX_5_0", "SUBSPEC");
		subMessage.addField("SubMax1", "");
		subMessage.addField("SubMax2", "");
		subMessage.addField("SubMax3", "");

		message.addField("SubMessage", subMessage);

		//FIXME:
		defMsgStorage.storeMessage(message);

		defMsgStorage.closeScriptRun(scriptRun);

        MessageRow storedMessage  = defMsgStorage.getMessages(0, 1, "").iterator().next();

        Assert.assertEquals(msgMetaData.getFromService(), storedMessage.getFrom());
        Assert.assertEquals(msgMetaData.getToService(), storedMessage.getTo());
        Assert.assertEquals(msgMetaData.getMsgName(), storedMessage.getMsgName());
        Assert.assertEquals(msgMetaData.getMsgNamespace(), storedMessage.getMsgNamespace());

	}

	@Test
	public void testMassStoring() throws InterruptedException, WorkspaceStructureException, FileNotFoundException
	{
		IMessageStorage defMsgStorage = new DatabaseMessageStorage(null, null, null);

		ScriptRun scriptRun = defMsgStorage.openScriptRun("Example", "ExampleDescription");

		IMessage message = new MapMessage("FIX_5_0", "SPEC");
        MsgMetaData msgMetaData = message.getMetaData();

		msgMetaData.setAdmin(false);
		msgMetaData.setFromService("FIX_FROM_BROKER");
		msgMetaData.setToService("FIX_TO_BROKER");

		message.addField("Max1", "");
        message.addField("Max2", "");
        message.addField("Max3", "");

        IMessage subMessage = new MapMessage("FIX_5_0", "SUBSPEC");
        subMessage.addField("SubMax1", "");
        subMessage.addField("SubMax2", "");
        subMessage.addField("SubMax3", "");

        message.addField("SubMessage", subMessage);

		long start = System.currentTimeMillis();
		for ( int i = 0; i < 1000; ++i )
		{
			//FIXME:
			defMsgStorage.storeMessage(message);
			Thread.sleep(3);
		}

		System.out.println(((double)(System.currentTimeMillis() - start))/10000.00);

		defMsgStorage.closeScriptRun(scriptRun);

        Iterable<MessageRow> messages = defMsgStorage.getMessages(0, 1000, "");

        for (MessageRow storedMessage:messages) {
            Assert.assertEquals(msgMetaData.getFromService(), storedMessage.getFrom());
            Assert.assertEquals(msgMetaData.getToService(), storedMessage.getTo());
            Assert.assertEquals(msgMetaData.getMsgName(), storedMessage.getMsgName());
            Assert.assertEquals(msgMetaData.getMsgNamespace(), storedMessage.getMsgNamespace());
        }

    }




}
