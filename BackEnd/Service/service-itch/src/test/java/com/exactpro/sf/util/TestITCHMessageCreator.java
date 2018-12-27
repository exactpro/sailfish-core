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
package com.exactpro.sf.util;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.services.itch.ITCHMessageHelper;
import java.time.LocalDateTime;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class TestITCHMessageCreator {
	
	private IMessageFactory msgFactory;
	private String namespace;
    private ITCHMessageHelper messageHelper;
	private BigDecimal orderId= new BigDecimal("1");

    public TestITCHMessageCreator(IMessageFactory msgFactory, String namespace, ITCHMessageHelper messageHelper) {
		this.msgFactory=msgFactory;
		this.namespace=namespace;
		this.messageHelper=messageHelper;
	}

	
	public IMessage getUnitHeader(short messageCount){
		IMessage messageHeader = msgFactory.createMessage("UnitHeader", namespace);
        messageHeader.addField("Length", 60);
        messageHeader.addField("MessageCount", messageCount);
        messageHeader.addField("MarketDataGroup", "M");
        messageHeader.addField("SequenceNumber", 1L);
        return messageHeader;
	}
	
	
	public IMessage getTime(long seconds){
		 IMessage messageTime = msgFactory.createMessage("Time", namespace);
        messageTime.addField(ITCHMessageHelper.FIELD_SECONDS, Long.valueOf(seconds));
	     return messageTime;
	}
	
	public IMessage getLoginRequest(){
		IMessage message = msgFactory.createMessage("LoginRequest", namespace);
		message.addField("Password", "tnp123");
		message.addField("Username", "MADTY0");
		return message;
	}
	
	public IMessage getAddOrderOneByteLength(long nanoseconds){
		IMessage messageAddOrder = msgFactory.createMessage("AddOrderOneByteLength", namespace);
        messageAddOrder.addField(ITCHMessageHelper.FIELD_NANOSECOND, nanoseconds);
       messageAddOrder.addField("OrderID", orderId);
       messageAddOrder.addField("Side", (short)66);
       messageAddOrder.addField("Quantity", new BigDecimal(10));
       messageAddOrder.addField("InstrumentID", 10L);
       messageAddOrder.addField("Reserved1", (short)10);
       messageAddOrder.addField("Reserved2", (short)10);
       messageAddOrder.addField("Price", 10d);
       messageAddOrder.addField("Flags", (short)10);
       messageAddOrder.addField("ImpliedPrice", 10d);
       return messageAddOrder;
	}
	
	public IMessage getMessageList(List<IMessage> list){
        IMessage messageList = msgFactory.createMessage(ITCHMessageHelper.MESSAGELIST_NAME, namespace);
        messageList.addField(ITCHMessageHelper.SUBMESSAGES_FIELD_NAME, list);
        return messageList;
	}
	
	public IMessage getTicksGroup(Double lowePrice, Double upperPrice, Double priceTick){
		IMessage message = msgFactory.createMessage("TicksGroup", namespace);
		message.addField("LowePrice", lowePrice);
		message.addField("UpperPrice", upperPrice);
		message.addField("PriceTick", priceTick);
		return message;
	}
	
	public IMessage getSecurityClassTickMatrix(List<IMessage> groups){
		IMessage message = msgFactory.createMessage("SecurityClassTickMatrix",namespace);
		message.addField("TicksGroup", groups);
       message.addField("NroTicks", 2L);
       message.addField("ClassId", 1L);
       message.addField("GroupId", 1L);
       message.addField("MessageType", (short)149); //MessageType.SecurityClassTickMatrix
       return message;
	}
	
	public IMessage getAddOrder(){
		IMessage addOrder = msgFactory.createMessage("AddOrder",namespace);
		addOrder.addField("Timestamp", new BigDecimal("12345678"));
		addOrder.addField("OrderID", orderId);
		addOrder.addField("Side", (short)1);
		addOrder.addField("Size", 14.14);
		addOrder.addField("Instrument", 1L);
		addOrder.addField("InstrumentID", 1L);
		addOrder.addField("Price", 15.15);
		addOrder.addField("Yield", 16.16);
		addOrder.addField("SourceVenue", 0);
		addOrder.addField("OrderBookType", (short)1);
		addOrder.addField("Participant", "part");
		addOrder.addField("Depth", (short)3);
		addOrder.addField("SourceVenue", 1); //Side.BUY;
		return addOrder;
	}
	
	public IMessage getOrderExecuted(){
		IMessage orderExecuted= msgFactory.createMessage("OrderExecuted", namespace);
		orderExecuted.addField("Nanosecond", 10L);
		orderExecuted.addField("OrderID", orderId);
		orderExecuted.addField("ExecutedQuantity", 10L);
		orderExecuted.addField("TradeMatchID", new BigDecimal(10));
		return orderExecuted;
	}
	
	public IMessage getTestInteger(){
		IMessage message=messageHelper.getMessageFactory().createMessage("testInteger", "ITCH");
        message.addField("uInt16", 1);
		message.addField("int8", 2);
		message.addField("int16", 3);
		message.addField("int32", 4);
		message.addField("STUB", 5);
		return messageHelper.prepareMessageToEncode(message, null);
	}
	
	public IMessage getTestLong(){
		IMessage message = messageHelper.getMessageFactory().createMessage("testLong", "ITCH");
		message.addField("uInt32", (long)1);
		message.addField("uInt64", (long)2);
		message.addField("int16", (long)3);
		message.addField("int32", (long)4);
		message.addField("int64", (long)5);
		message.addField("STUB", (long)6);
		return messageHelper.prepareMessageToEncode(message, null);
	}
	
	public IMessage getTestShort(){
		IMessage message = messageHelper.getMessageFactory().createMessage("testShort", "ITCH");
		message.addField("uInt8", (short)1);
		message.addField("byte", (short)3);
		message.addField("int8", (short)4);
		message.addField("int16", (short)5);
		message.addField("STUB", (short)6);
		return messageHelper.prepareMessageToEncode(message, null);
	}
	
	public IMessage getTestByte(){
		IMessage message = messageHelper.getMessageFactory().createMessage("testByte", "ITCH");
		message.addField("byte", (byte)3);
		message.addField("int8", (byte)4);
		return messageHelper.prepareMessageToEncode(message, null);
	}
	
	public IMessage getTestString(){
		IMessage message = messageHelper.getMessageFactory().createMessage("testString", "ITCH");
		message.addField("Alpha", "ffst");
		message.addField("Time", "10:49:00");
		message.addField("Date", "Mon Jul 04 14:02:30 MSK 2016");
		message.addField("STUB", "stub");
		 return messageHelper.prepareMessageToEncode(message, null);
	}
	
	public IMessage getTestFloat(){
		IMessage message = messageHelper.getMessageFactory().createMessage("testFloat", "ITCH");
		message.addField("Price", (float)3);
		return messageHelper.prepareMessageToEncode(message, null);
	}
	
	public IMessage getTestDouble(){
		IMessage message = messageHelper.getMessageFactory().createMessage("testDouble", "ITCH");
		message.addField("Price", 3.1);
		message.addField("Size", 3.2);
		message.addField("Price4", 3.3);
		message.addField("Size4", 3.4);
		message.addField("UInt16", 3.5);
		return messageHelper.prepareMessageToEncode(message, null);
	}
	
	public IMessage getTestBigDecimal(){
		IMessage message = messageHelper.getMessageFactory().createMessage("testBigDecimal", "ITCH");
		message.addField("UInt64", new BigDecimal(10));
		message.addField("Int32", new BigDecimal(11));
		message.addField("UInt32", new BigDecimal(12));
		message.addField("Price", new BigDecimal(13));
		message.addField("Size", new BigDecimal(14));
		message.addField("UDT", new BigDecimal(0));
		return messageHelper.prepareMessageToEncode(message, null);
	}
	
	public IMessage getTestDate() throws ParseException{
		IMessage message = messageHelper.getMessageFactory().createMessage("testDate", "ITCH");
		String dateString = "20160627";
		String timeString = "10:10:11";
        TimeZone timeZone = TimeZone.getTimeZone("UTC");
	    DateFormat df = new SimpleDateFormat("yyyyMMdd");
        df.setTimeZone(timeZone);
		DateFormat tf = new SimpleDateFormat("HH:mm:ss");
        tf.setTimeZone(timeZone);
		Date startDate=df.parse(dateString);
	    Date startTime=tf.parse(timeString);
        Date days = new Date(100);
        LocalDateTime localDays = DateTimeUtility.toLocalDateTime(days);
        localDays = localDays.minusNanos(localDays.getNano());
        message.addField("Date", DateTimeUtility.toLocalDate(startDate));
        message.addField("Time", DateTimeUtility.toLocalTime(startTime));
        message.addField("Days", DateTimeUtility.toLocalDate(startDate));
        message.addField("STUB", DateTimeUtility.toLocalDateTime(days));
        // LocalDateTime localDays = DateTimeUtility.toLocalDateTime(100);
        // localDays = localDays.minusNanos(localDays.getNano());
        // message.addField("Date", DateTimeUtility.toLocalDateTime(startDate));
        // message.addField("Time", DateTimeUtility.toLocalDateTime(startTime));
        // message.addField("Days", localDays);
        // message.addField("STUB", DateTimeUtility.toLocalDateTime(100));
		return messageHelper.prepareMessageToEncode(message, null);
	}
}
