/*******************************************************************************
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

import org.apache.mina.core.session.IoSession;

import com.exactpro.sf.common.codecs.AbstractCodec;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.MsgMetaData;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.common.util.EvolutionBatch;
import com.exactpro.sf.services.MessageHelper;
import com.exactpro.sf.services.ServiceHandlerRoute;
import com.exactpro.sf.services.itch.ITCHCodecSettings;
import com.exactpro.sf.services.itch.ITCHUdpClient;

public class SOUPUdpClient extends ITCHUdpClient {

    private MoldUdpCodecSettings codecSettings;

    @Override
    protected void internalInit() throws Exception {
        super.internalInit();
        ITCHCodecSettings originalCodecSettings = super.getCodecSettings();
        this.codecSettings = new MoldUdpCodecSettings(getSettings().isEvolutionSupportEnabled());
        codecSettings.setDictionaryURI(originalCodecSettings.getDictionaryURI());
        codecSettings.setMsgLength(originalCodecSettings.getMsgLength());
        codecSettings.setFilterValues(originalCodecSettings.getFilterValues());
        codecSettings.setChunkDelimiter(originalCodecSettings.getChunkDelimiter());
        codecSettings.setPreprocessingEnabled(originalCodecSettings.isPreprocessingEnabled());
        codecSettings.setEvolutionSupportEnabled(originalCodecSettings.isEvolutionSupportEnabled());
    }

    @Override
    protected MoldUdpCodecSettings getCodecSettings() {
        return codecSettings;
    }

    @Override
    protected Class<? extends AbstractCodec> getCodecClass() throws Exception {
        return SOUPCodec.class;
	}

	@Override
	protected MessageHelper createMessageHelper(IMessageFactory messageFactory, IDictionaryStructure dictionary) {
		MessageHelper messageHelper = new SOUPMessageHelper();
		messageHelper.init(messageFactory, dictionary);
		return messageHelper;
	}

	@Override
	public void messageReceived(IoSession session, Object message) throws Exception {
		// override MITCHAbstractClient.messageReceived: we don't use "IncludedMessages"
		logger.debug("Message {} was received", message);
        // TODO: probably we should delegate call to onMessageReceived method instead of recreating logic here

		if (!(message instanceof IMessage)) {
			throw new EPSCommonException("Received message is not instance of " + IMessage.class);
		}

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

        getServiceHandler().putMessage(getSession(), ServiceHandlerRoute.get(true, metaData.isAdmin()), msg);
	}
}
