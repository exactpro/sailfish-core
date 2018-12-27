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
package com.exactpro.sf.services.ntg;

import com.exactpro.sf.common.impl.messages.MapMessage;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.util.AbstractTest;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static com.exactpro.sf.services.ntg.NTGServerTest.MessageType.Heartbeat;

public class TestMessages extends AbstractTest
{
	private static final Logger logger = LoggerFactory.getLogger(TestMessages.class);

	@Test
	public void testMsg() throws IOException
	{
        IMessage old = new MapMessage("NTG", "MessageHeader");
        old.addField("MessageType", Heartbeat.getType());
        logger.trace("{}", old.<Object>getField("MessageType"));
		IMessage hdr = old.cloneMessage();
		String msgType = hdr.getField("MessageType");
		logger.trace(msgType);
	}

	@Test
	public void testMsgModified() throws IOException
	{
		try
		{
            IMessage old = new MapMessage("NTG", "MessageHeader");
            old.addField("MessageType", Heartbeat.getType());
			old.addField("MessageLength", 1);
			old.addField("StartOfMessage", 2); // StartOfMessage.ClientStartOfMessage

            IMessage hrtb = new MapMessage("NTG", "Heartbeat");
			hrtb.addField("MessageHeader", old);

			// This works
			IMessage header = hrtb.getField("MessageHeader");
			logger.trace((String)header.getField("MessageType"));

			IMessage msg = hrtb.cloneMessage();

			// Here we will get null
            logger.trace("{}", msg.<Object>getField("MessageType"));
		}
		catch (NullPointerException npe)
		{
			logger.trace("NullPointerException generated in getMessageType() method has been caught.");
		}
		catch (EPSCommonException eps)
		{
			eps.printStackTrace();

		}
		catch (Exception e)
		{
			e.printStackTrace();
			Assert.fail();
		}
	}

}