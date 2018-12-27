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
package com.exactpro.sf.services.fix.handler;

import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.MsgMetaData;
import com.exactpro.sf.common.services.ServiceInfo;
import com.exactpro.sf.services.IServiceHandler;
import com.exactpro.sf.services.ServiceHandlerRoute;
import com.exactpro.sf.services.fix.converter.dirty.DirtyQFJIMessageConverter;
import com.exactpro.sf.services.tcpip.TCPIPProxy;
import com.exactpro.sf.storage.IMessageStorage;

import quickfix.DataDictionary;
import quickfix.DefaultMessageFactory;
import quickfix.Message;
import quickfix.field.BeginString;
import quickfix.field.MsgType;
import quickfix.field.SenderCompID;
import quickfix.field.TargetCompID;


/**
 * Handles the server to proxy part of the proxied connection.
 *
 */
public class ServerSideIoHandler extends AbstractProxyIoHandler {

	private final Logger logger = LoggerFactory.getLogger(ServerSideIoHandler.class);

	private IMessageStorage storage;
	private IServiceHandler handler;
	private DefaultMessageFactory factory;
	private DataDictionary appDictionary;
	private ServiceInfo serviceInfo;
	private TCPIPProxy proxyService;
	private DirtyQFJIMessageConverter converter;

	public ServerSideIoHandler(IoConnector connector, IMessageStorage storage, IServiceHandler handler, TCPIPProxy proxyService, DefaultMessageFactory factory, DataDictionary appDictionary, DirtyQFJIMessageConverter converter) {
		this.storage = storage;
		this.handler = handler;
		this.proxyService = proxyService;
		this.factory = factory;
		this.appDictionary = appDictionary;
		this.converter = converter;
	}

	@Override
	public void messageReceived(IoSession session, Object message)
	throws Exception
	{
		logger.debug("messageReceived: {}", getClass().getSimpleName());

		if(message instanceof IMessage){
			byte[] rawMessage = ((IMessage)message).getMetaData().getRawMessage();
			message = new String(rawMessage);
		}

		// TODO Create message through factory
		Message mess = new Message((String)message);
		String fixVers = mess.getHeader().getString(BeginString.FIELD);
		String msgType = mess.getHeader().getString(MsgType.FIELD);
		Message msg = factory.create(fixVers, msgType);
		msg.fromString((String)message, appDictionary, true);

		// TODO Store message to IMessage storage
		IMessage convertedMessage = converter.convert(msg);
		MsgMetaData metaData = convertedMessage.getMetaData();
		metaData.setFromService(mess.getHeader().getString(SenderCompID.FIELD));
		metaData.setToService(mess.getHeader().getString(TargetCompID.FIELD));
		metaData.setServiceInfo(serviceInfo);

		// TODO invoke IServiceHandler
		if (msg.isAdmin())
		{
			metaData.setAdmin(true);
			handler.putMessage(proxyService.getSession(), ServiceHandlerRoute.FROM_ADMIN, convertedMessage);
		}
		if (msg.isApp())
		{
			metaData.setAdmin(false);
			handler.putMessage(proxyService.getSession(), ServiceHandlerRoute.FROM_APP, convertedMessage);
		}

        storage.storeMessage(convertedMessage);

		boolean notSend = isNotSendAndRulesProcess(mess, msgType, proxyService);

		if (!notSend) {
			//Default handler
			super.messageReceived(session, mess.toString());
		} else {
			logger.debug("Message is not sent, because rules have NotSend");
		}
	}

	@Override
    public ServiceInfo getServiceInfo() {
		return serviceInfo;
	}

	@Override
    public void setServiceInfo(ServiceInfo serviceInfo) {
		this.serviceInfo = serviceInfo;
	}

}