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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Random;

import org.apache.mina.core.future.DefaultWriteFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.DummySession;
import org.apache.mina.filter.codec.AbstractProtocolEncoderOutput;

import com.exactpro.sf.common.impl.messages.DefaultMessageFactory;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.loaders.IDictionaryStructureLoader;
import com.exactpro.sf.common.messages.structures.loaders.XmlDictionaryStructureLoader;
import com.exactpro.sf.util.AbstractTest;

public final class TestNTGHelper extends AbstractTest {

    public static final String nameSpace = "NTG";
	public static final String nameMsgMessageHeader = "MessageHeader";
	public static final String nameFldLength = "MessageLength";

	public static IMessage getHeartbeat() throws IOException {
		IMessage heartbeat = DefaultMessageFactory.getFactory().createMessage("Heartbeat", nameSpace);
		heartbeat = getMessageHelper(false).prepareMessageToEncode(heartbeat, null);
		return heartbeat;
	}

	public static IMessage getNewOrder() throws IOException {
		IMessage newOrder = DefaultMessageFactory.getFactory().createMessage("NewOrder", nameSpace);
        newOrder.addField("ClOrdID", NTGUtility.getNewClOrdID());
        newOrder.addField("ClearingAccount", 1); // com.exactpro.sf.messages.ntg.components.ClearingAccount.Client
		newOrder.addField("CommonSymbol", "VODl");
        newOrder.addField("OrderType", 1); // com.exactpro.sf.messages.ntg.components.OrderType.Market
        newOrder.addField("TimeInForce", 4); // com.exactpro.sf.messages.ntg.components.TimeInForce.FOK

        newOrder.addField("Side", 2);//com.exactpro.sf.messages.ntg.components.Side.Sell

		newOrder.addField("OrdQty", (int) rnd.nextFloat() * 1000);
		newOrder.addField("DisplayQty", (int) rnd.nextFloat() * 1000);
		//newOrder.addField("MinQty", (int)rnd.nextFloat() * 1000 );
		float random = rnd.nextFloat() * (float)1000.0;
		int i = (int) (random * 10000);
		newOrder.addField("LimitPrice", i / 10000.0);
		newOrder.addField("Capacity", 1);
		//newOrder.addField("AutoCancel", AutoCancel.Logout);
        newOrder.addField("OrderSubType", 10); // com.exactpro.sf.messages.ntg.components.OrderSubType.Order
		newOrder.addField("ExpireDateTime", (int)(System.currentTimeMillis()/1000 + 10) );
		//newOrder.addField("TargetBook", 0 );
        newOrder.addField("TraderID", NTGUtility.getRandomString(11));
        newOrder.addField("UInt32Field", 4294967295l);
        //newOrder.addField("ClientID", NTGUtility.getRandomString( 10 ));
		//newOrder.addField("ExecInstruction", 0 );
		//newOrder.addField("Anonymity", Anonymity.Named );

		return getMessageHelper(false).prepareMessageToEncode(newOrder, null);
	}

	public static IMessage getLogon() throws IOException {
		IMessage logon = DefaultMessageFactory.getFactory().createMessage("Logon", nameSpace);
		logon.addField("MessageVersion", (byte) 1);
		logon.addField("Username", "user_name");
		logon.addField("Password", "password");
		logon.addField("NewPassword", "new_password");

		return getMessageHelper(false).prepareMessageToEncode(logon, null);
	}

	public static IDictionaryStructure getDictionary() throws IOException {
        return loadDictionary("ntg.xml");
	}

	public static IDictionaryStructure getDirtyDictionary() throws IOException {
        return loadDictionary("NTG_Dirty.xsd");
	}
	
	public static IDictionaryStructure getDictionaryWithDifferentTypesMessages() throws IOException{
        return loadDictionary("ntg_types.xml");
	}
	
	public static IDictionaryStructure getDictionaryWithIncorrectMessages() throws IOException{
        return loadDictionary("ntg_InvalidMessage.xml");
	}
	
	public static IDictionaryStructure getDictionaryWithDublicateMessages() throws IOException{
        return loadDictionary("ntg_DublicateMessageTypeValue.xml");
	}
	
	public static IDictionaryStructure getDictionaryWithMissedLength() throws IOException{
        return loadDictionary("ntg_InvalidFieldLength.xml");
	}

    public static NTGMessageHelper getMessageHelper(boolean isDirty) throws IOException {
        NTGMessageHelper messageHelper = new NTGMessageHelper();
		messageHelper.init(
				DefaultMessageFactory.getFactory(),
				isDirty ? getDirtyDictionary() : getDictionary());

		return messageHelper;
	}

	public final static String getDictionaryPath()
	{
		return String.format( "%s%s%s%s%s",
				BASE_DIR, File.separator,
                "dictionaries", File.separator, "ntg.xml");
	}

	public final class MockProtocolEncoderOutput extends AbstractProtocolEncoderOutput {
		@Override
		public WriteFuture flush() {
			DummySession dummySession = new DummySession();
			DefaultWriteFuture writeFuture = new DefaultWriteFuture(dummySession);
			return writeFuture;
		}
	}

	private static Random rnd = new Random(Calendar.getInstance().getTimeInMillis());

	public static byte[] getRandomBytesArray(int length)
	{
		byte[] arrRmd = new byte[length];

		for(int i = 0 ; i < length; i++)
		{
			byte randomChar = (byte)(32 + rnd.nextInt( 125 - 32));
			arrRmd[i] = randomChar;
		}

		return arrRmd;
	}
	
	private static IDictionaryStructure loadDictionary(String fileName) throws IOException {
	    IDictionaryStructureLoader loader = new XmlDictionaryStructureLoader();
        String fileFQN = BASE_DIR + File.separator + "src" + File.separator  + "test" + File.separator + 
        		"plugin" + File.separator + "cfg" + File.separator + "dictionaries" + File.separator + fileName;
        try (InputStream inputStream = new FileInputStream( fileFQN )) {
        	IDictionaryStructure dictionary = loader.load(inputStream);
            return dictionary;
        }
	}
}