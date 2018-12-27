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
package com.exactpro.sf.services.itch;

import org.apache.mina.core.session.IoSession;

import com.exactpro.sf.common.codecs.AbstractCodec;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.services.MessageHelper;
import com.exactpro.sf.services.mina.AbstractMINAUDPService;
import com.exactpro.sf.services.mina.MINASession;
import com.exactpro.sf.services.util.ServiceUtil;

public class ITCHUdpClient extends AbstractMINAUDPService implements IITCHClient {
    private ITCHCodecSettings codecSettings;

    @Override
    protected void internalInit() throws Exception {
        super.internalInit();
        codecSettings = new ITCHCodecSettings();
        codecSettings.setMsgLength(getSettings().getMsgLength());
        codecSettings.setFilterValues(ServiceUtil.loadStringFromAlias(serviceContext.getDataManager(), getSettings().getFilterValues(), ","));
        codecSettings.setDictionaryURI(getSettings().getDictionaryName());
    }

    @Override
    protected MessageHelper createMessageHelper(IMessageFactory messageFactory, IDictionaryStructure dictionary) {
        MessageHelper messageHelper = new ITCHMessageHelper();
        messageHelper.init(messageFactory, dictionary);
        return messageHelper;
    }

    @Override
    public void messageSent(IoSession session, Object message) throws Exception {
        for (IMessage subMessage : ITCHMessageHelper.extractSubmessages(message)) {
            super.messageSent(session, subMessage);
        }
    }

    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        for (IMessage subMessage : ITCHMessageHelper.extractSubmessages(message)) {
            super.messageReceived(session, subMessage);
        }
    }

    @Override
    public ITCHClientSettings getSettings() {
        return (ITCHClientSettings) super.getSettings();
	}

    @Override
    protected Class<? extends AbstractCodec> getCodecClass() throws Exception {
        return ITCHCodec.class;
    }

    @Override
    protected ITCHCodecSettings getCodecSettings() {
        return codecSettings;
    }

    @Override
    protected String getNetworkInterface() {
        return getSettings().getNetworkInterface();
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
    protected MINASession createSession(IoSession session) {
        return new ITCHSession(getServiceName(), session, loggingConfigurator, getSettings().getMarketDataGroup()) {
            @Override
            public IMessage send(Object message) throws InterruptedException {
                throw new EPSCommonException("Cannot send message: " + message);
            }
        };
    }

    @Override
    protected void internalStart() throws Exception {
        super.internalStart();
        connect();
    }
}
