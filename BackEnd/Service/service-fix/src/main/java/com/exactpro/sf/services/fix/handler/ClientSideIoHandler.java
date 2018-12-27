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

import java.net.SocketAddress;

import org.apache.mina.core.RuntimeIoException;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.MsgMetaData;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.services.ServiceInfo;
import com.exactpro.sf.common.services.ServiceName;
import com.exactpro.sf.configuration.ILoggingConfigurator;
import com.exactpro.sf.services.IServiceHandler;
import com.exactpro.sf.services.ServiceHandlerRoute;
import com.exactpro.sf.services.fix.FixUtil;
import com.exactpro.sf.services.fix.QFJDictionaryAdapter;
import com.exactpro.sf.services.fix.converter.dirty.DirtyQFJIMessageConverter;
import com.exactpro.sf.services.mina.MINASession;
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
 * Handles the client to proxy part of the proxied connection.
 *
 */
public class ClientSideIoHandler extends AbstractProxyIoHandler {

	private static Logger logger = LoggerFactory.getLogger(ClientSideIoHandler.class);
	private ServerSideIoHandler connectorHandler = null;
	private final IoConnector connector;
	private final SocketAddress remoteAddress;
	private IMessageStorage storage;
	private IServiceHandler handler;
	private DefaultMessageFactory factory = new DefaultMessageFactory();
	private DataDictionary appDictionary;
	private ServiceInfo serviceInfo;
	private TCPIPProxy proxyService;
	private ILoggingConfigurator logConfigurator;
	private DirtyQFJIMessageConverter converter;
    private ServiceName serviceName;

	public ClientSideIoHandler(IoConnector connector,
            SocketAddress remoteAddress, IMessageStorage storage, IServiceHandler handler, TCPIPProxy proxyService, ILoggingConfigurator logConfigurator, IDictionaryStructure dictionary, IMessageFactory factory,
            DirtyQFJIMessageConverter converter, ServiceName serviceName) {
		this.connector = connector;
		this.remoteAddress = remoteAddress;
		this.storage = storage;
		this.handler = handler;
		this.proxyService = proxyService;
		this.logConfigurator = logConfigurator;
		this.appDictionary = new QFJDictionaryAdapter(dictionary);
		this.converter = converter;
        this.serviceName = serviceName;

		connectorHandler = new ServerSideIoHandler(connector, storage, handler, proxyService, this.factory, appDictionary, converter);
		connectorHandler.setServiceInfo(serviceInfo);
		connector.setHandler(connectorHandler);

	}

	@Override
	public void sessionOpened(final IoSession session) throws Exception {

		logger.debug("sessionOpened");

		//Store session to MAP
        MINASession mSession = new MINASession(serviceName, session, logConfigurator);
		proxyService.addSession(session, mSession);

		connector.connect(remoteAddress).addListener(new IoFutureListener<ConnectFuture>() {
			@Override
            public void operationComplete(ConnectFuture future) {
				try {
					future.getSession().setAttribute(OTHER_IO_SESSION, session);
					session.setAttribute(OTHER_IO_SESSION, future.getSession());
					IoSession session2 = future.getSession();
					session2.resumeRead();
					session2.resumeWrite();
				} catch (RuntimeIoException e) {
					session.close(true);
				} finally {
					session.resumeRead();
					session.resumeWrite();
				}
			}
		});
	}

	@Override
	public void messageReceived(IoSession session, Object message) throws Exception
	{
		logger.debug("messageReceived: {}", message.getClass().getSimpleName());
		logger.debug("messageReceived: {}", message);
		if(message instanceof IMessage){
			byte[] rawMessage = ((IMessage)message).getMetaData().getRawMessage();
			message = new String(rawMessage);
		}

		// Create message through factory
		Message mess = FixUtil.fromString((String)message);
		logger.debug("mess: {}", mess);
		String fixVers = mess.getHeader().getString(BeginString.FIELD);
		String msgType = mess.getHeader().getString(MsgType.FIELD);
		Message msg = factory.create(fixVers, msgType);
		msg.fromString((String)message, appDictionary, true);
		logger.debug("msg: {}", msg);
		logger.debug("appDictionary: {}", appDictionary.getBeginString());

		// Store message to IMessage storage
		IMessage convertedMessage = converter.convert(msg);
		MsgMetaData metaData = convertedMessage.getMetaData();
		metaData.setFromService(mess.getHeader().getString(SenderCompID.FIELD));
		metaData.setToService(mess.getHeader().getString(TargetCompID.FIELD));
		metaData.setServiceInfo(serviceInfo);

		// invoke IServiceHandler
		if (msg.isAdmin())
		{
			metaData.setAdmin(true);
			handler.putMessage(proxyService.getSession(session), ServiceHandlerRoute.FROM_ADMIN, convertedMessage);
		}
		if (msg.isApp())
		{
			metaData.setAdmin(false);
			handler.putMessage(proxyService.getSession(session), ServiceHandlerRoute.FROM_APP, convertedMessage);
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
