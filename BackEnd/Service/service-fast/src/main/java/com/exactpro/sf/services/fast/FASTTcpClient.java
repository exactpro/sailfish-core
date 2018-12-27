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
package com.exactpro.sf.services.fast;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.openfast.Context;
import org.openfast.Message;
import org.openfast.MessageBlockReader;
import org.openfast.MessageOutputStream;
import org.openfast.error.FastException;
import org.openfast.session.Connection;
import org.openfast.session.Endpoint;
import org.openfast.session.FastConnectionException;
import org.openfast.session.tcp.TcpEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.MsgMetaData;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.common.util.SendMessageFailedException;
import com.exactpro.sf.services.ServiceHandlerRoute;
import com.exactpro.sf.services.ServiceStatus;
import com.exactpro.sf.services.fast.blockstream.BlockEncodedOutputStream;
import com.exactpro.sf.services.fast.blockstream.StreamBlockLengthReader;
import com.exactpro.sf.services.fast.converter.ConverterException;
import com.exactpro.sf.services.fast.converter.IMessageToFastConverter;

public class FASTTcpClient extends FASTAbstractTCPClient {

	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName() + "@" + Integer.toHexString(hashCode()));
	private MessageOutputStream msgOutStream;
	private volatile IMessageToFastConverter iMsgToFastConverter;

	public FASTTcpClient() {
		super();
		changeStatus(ServiceStatus.CREATED, "Service created", null);
	}

	@Override
	protected void initConnection() {
		super.initConnection();
		Context context = msgInStream.getContext();
		OutputStream outputStream;
		try {
			outputStream = connection.getOutputStream();
		} catch (IOException e) {
			closeSession();
			logger.error("Failed to get input stream from multicast connection", e);
			throw new EPSCommonException("Failed to get input stream from multicast connection", e);
		}
		if (getSettings().isStreamBlockEncoded()) {
			outputStream = new BlockEncodedOutputStream(outputStream);
		}
		msgOutStream = new MessageOutputStream(outputStream, context);
	}

	@Override
	protected Connection getConnection(String remoteAddr, int port,
			String interfaceAddress) throws FastConnectionException {
		Endpoint ep = new TcpEndpoint(remoteAddr, port);
		Connection connection = ep.connect();
		return connection;
	}

	@Override
	protected void doStart() {
	    if(getSettings().isAutoconnect()){
	        initConnection();
        }
	}

	@Override
	protected void send(Object message) throws InterruptedException {
		IMessageToFastConverter converter = getIMessageToFastConverter();
		IMessage iMsg = (IMessage) message;
		Message fastMsg;
		FASTClientSettings settings = getSettings();

		MsgMetaData metadata = iMsg.getMetaData();
		metadata.setFromService(getName());
		metadata.setToService(settings.getAddress() + ":" + settings.getPort());
		metadata.setRawMessage(iMsg.toString().getBytes());
		metadata.setServiceInfo(serviceInfo);

		try {
            fastMsg = converter.convert(iMsg);
            logger.debug("Converted IMessage : [{}] to FAST message : [{}]", iMsg, fastMsg);
        } catch (ConverterException e) {
            logger.warn("Failed to convert IMessage message to FAST", e);
            getServiceHandler().exceptionCaught(getSession(), e);
            throw new SendMessageFailedException("Failed to convert IMessage message to FAST", e);
        }
        try {
            msgOutStream.writeMessage(fastMsg);
        } catch (FastException e) {
            logger.error("Send message " + iMsg.getName() + " failed", e);
            throw new SendMessageFailedException("Send message " + iMsg.getName() + " failed cause: " + e.getMessage(), e);
        }

        try {
			if (iMsg.getMetaData().isAdmin()) {
				getServiceHandler().putMessage(getSession(), ServiceHandlerRoute.TO_ADMIN, iMsg);
			} else {
				getServiceHandler().putMessage(getSession(), ServiceHandlerRoute.TO_APP, iMsg);
			}
		} catch (Exception e) {
            if(e instanceof InterruptedException){
                throw (InterruptedException)e;
            }
			logger.warn("serviceHandler thrown exception",e);
		} finally {
		    logger.debug("message passed to ServericeHandler");
		}

		logger.debug("message passed to msgStorage");
        msgStorage.storeMessage(iMsg);

	}

	private IMessageToFastConverter getIMessageToFastConverter() {
		if (iMsgToFastConverter == null) {
			IDictionaryStructure dictionary = this.dictionary;
			iMsgToFastConverter = new IMessageToFastConverter(dictionary, getRegistry());
		}
		return iMsgToFastConverter;
	}

	@Override
	protected MessageBlockReader getBlockReader() {
		if (getSettings().isStreamBlockEncoded()) {
			return new StreamBlockLengthReader();
		}
		return MessageBlockReader.NULL;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this).append("name", serviceName).toString();
	}
}
