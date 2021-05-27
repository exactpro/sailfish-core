/******************************************************************************
 * Copyright 2009-2021 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.services.itch.soup;

import static com.exactpro.sf.services.ServiceHandlerRoute.FROM_ADMIN;
import static com.exactpro.sf.services.ServiceHandlerRoute.FROM_APP;
import static com.exactpro.sf.services.ServiceHandlerRoute.TO_ADMIN;
import static com.exactpro.sf.services.ServiceHandlerRoute.TO_APP;

import java.net.InetSocketAddress;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.nio.NioDatagramConnector;

import com.exactpro.sf.common.codecs.AbstractCodec;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.MsgMetaData;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.common.util.EvolutionBatch;
import com.exactpro.sf.services.IServiceHandler;
import com.exactpro.sf.services.MessageHelper;
import com.exactpro.sf.services.itch.ITCHClientSettings;
import com.exactpro.sf.services.itch.ITCHCodecSettings;
import com.exactpro.sf.services.itch.ITCHSession;
import com.exactpro.sf.services.mina.AbstractMINAService;
import com.exactpro.sf.services.mina.MINASession;
import com.exactpro.sf.services.util.ServiceUtil;

public class SOUPUnicastUdpClient extends AbstractMINAService {

    private ITCHCodecSettings codecSettings;
    /**
     * All operations with connector should be executed under synchronization on current service
     */
	private NioDatagramConnector connector;

	@Override
    protected void internalInit() throws Exception {
        codecSettings = new ITCHCodecSettings();
        codecSettings.setDictionaryURI(getSettings().getDictionaryName());
        codecSettings.setFilterValues(ServiceUtil.loadStringFromAlias(serviceContext.getDataManager(), getSettings().getFilterValues(), ","));
        codecSettings.setEvolutionSupportEnabled(getSettings().isEvolutionSupportEnabled());
    }
	
    @Override
    public ITCHClientSettings getSettings() {
        return (ITCHClientSettings) super.getSettings();
    }
	
	@Override
    protected Class<? extends AbstractCodec> getCodecClass() throws Exception {
        return SOUPUnicastUdpCodec.class;
	}

	@Override
    protected ITCHCodecSettings getCodecSettings() {
        return codecSettings;
    }
	
	@Override
    protected String getHostname() {
        return getSettings().getAddress();
    }

    @Override
    protected int getPort() {
        return getSettings().getPort();
    }
	
	@Override
	protected MessageHelper createMessageHelper(IMessageFactory messageFactory, IDictionaryStructure dictionary) {
		MessageHelper messageHelper = new SOUPTcpMessageHelper();
		messageHelper.init(messageFactory, dictionary);
		return messageHelper;
	}

	@Override
	protected synchronized void initConnector() throws Exception {

	    super.initConnector();

	    connector = new NioDatagramConnector();
        connector.getSessionConfig().setReadBufferSize(65534);
        connector.setHandler(this);

        initFilterChain(connector.getFilterChain());
	}

	@Override
	protected synchronized ConnectFuture getConnectFuture() throws Exception {
	    return connector.connect(new InetSocketAddress(getHostname(), getPort()));
	}

	@Override
	protected synchronized void disposeConnector() {
        if(connector != null) {
            logger.info("Disposing connector");
            connector.dispose(); // wait for dispose?
            connector = null;
        }
    }

	@Override
	public void messageSent(IoSession session, Object message) throws Exception {
		// override MITCHTcpClient.messageSent: we don't use "IncludedMessages"
		logger.debug("Message sent");

		if (!(message instanceof IMessage)) {
			throw new EPSCommonException("Received message is not" + IMessage.class);
		}

        MINASession internalSession = getSession();

		IMessage msg = (IMessage) message;
		MsgMetaData metaData = msg.getMetaData();
        metaData.setToService(getEndpointName());
		metaData.setFromService(getName());
		metaData.setServiceInfo(serviceInfo);

        storage.storeMessage(msg);

        if(logger.isDebugEnabled()) {
            logger.debug("Message sent: {}", getHumanReadable(msg));
        }

		IServiceHandler handler = getServiceHandler();
		if (handler != null) {
            handler.putMessage(internalSession, metaData.isAdmin() ? TO_ADMIN : TO_APP, msg);
		}
	}

	@Override
	public void messageReceived(IoSession session, Object message) throws Exception {
		// override MITCHAbstractClient.messageReceived: we don't use "IncludedMessages"
		logger.debug("Message {} was received", message);
        // TODO: probably we should delegate call to onMessageReceived method instead of recreating logic here

		if (!(message instanceof IMessage)) {
			throw new EPSCommonException("Received message is not instance of " + IMessage.class);
		}

        MINASession internalSession = getSession();

		IMessage msg = (IMessage) message;
		MsgMetaData metaData = msg.getMetaData();
		metaData.setToService(getName());
		metaData.setServiceInfo(serviceInfo);

        if (EvolutionBatch.MESSAGE_NAME.equals(msg.getName()) && !getSettings().isEvolutionSupportEnabled()) {
            logger.info("Skip processing {} message", EvolutionBatch.MESSAGE_NAME);
            return;
        }

        storage.storeMessage(msg);

        if(logger.isDebugEnabled()) {
            logger.debug("Message received: {} ", getHumanReadable(msg));
        }

		IServiceHandler handler = getServiceHandler();

		if (handler != null) {
			try {
                handler.putMessage(internalSession, msg.getMetaData().isAdmin() ? FROM_ADMIN : FROM_APP, msg);
			} catch (Exception e) {
				logger.error("Exception in handler", e);
			}
		}
	}

    @Override
    protected void internalStart() throws Exception {
        super.internalStart();
        connect();
    }
	
    @Override
    protected MINASession createSession(IoSession session) {
        ITCHSession itchSession = new ITCHSession(getServiceName(), session,
                getSettings().getMarketDataGroup(), getSettings().getSendMessageTimeout());
        loggingConfigurator.registerLogger(itchSession, getServiceName());
        return itchSession;
    }
}
