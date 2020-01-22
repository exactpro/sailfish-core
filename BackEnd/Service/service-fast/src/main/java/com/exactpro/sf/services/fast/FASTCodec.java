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

import com.exactpro.sf.common.codecs.AbstractCodec;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.IMessageFactory;
import com.exactpro.sf.common.messages.MsgMetaData;
import com.exactpro.sf.common.messages.structures.IDictionaryStructure;
import com.exactpro.sf.common.messages.structures.IMessageStructure;
import com.exactpro.sf.common.util.ICommonSettings;
import com.exactpro.sf.configuration.IDataManager;
import com.exactpro.sf.configuration.suri.SailfishURI;
import com.exactpro.sf.connectivity.mina.net.IoBufferWithAddress;
import com.exactpro.sf.services.IServiceContext;
import com.exactpro.sf.services.fast.converter.ConverterException;
import com.exactpro.sf.services.fast.converter.FastToIMessageConverter;
import com.exactpro.sf.services.fast.converter.IMessageToFastConverter;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.openfast.Context;
import org.openfast.IntegerValue;
import org.openfast.Message;
import org.openfast.MessageOutputStream;
import org.openfast.template.TemplateRegistry;
import org.openfast.template.type.codec.TypeCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

import static com.exactpro.sf.common.messages.structures.StructureUtils.getAttributeValue;

public class FASTCodec extends AbstractCodec {

	private static final String INPUT_CONTEXT_ATTR_NAME = "FAST_INPUT_CONTEXT";
	private static final String OUTPUT_CONTEXT_ATTR_NAME = "FAST_OUTPUT_CONTEXT";

	static final Logger logger = LoggerFactory.getLogger(FASTCodec.class);

	private IServiceContext serviceContext;
	private IDictionaryStructure msgDictionary;
	private FastToIMessageConverter converter;
	private FASTCodecSettings settings;
	private TemplateRegistry registry;
	private IMessageToFastConverter iMsgToFastConverter;
	private FastToIMessageDecoder decoder;

	@Override
	public void init(
			IServiceContext serviceContext,
			ICommonSettings settings,
			IMessageFactory msgFactory, IDictionaryStructure dictionary) {

        this.serviceContext = Objects.requireNonNull(serviceContext, "'Service context' parameter cannot be null");
        this.msgDictionary = Objects.requireNonNull(dictionary, "'dictionary' parameter cannot be null");
        String fastTemplate = Objects.requireNonNull(getAttributeValue(dictionary, FASTMessageHelper.TEMPLATE_ATTRIBYTE), "'Template attribute' parameter");
        this.settings = (FASTCodecSettings)Objects.requireNonNull(settings, "'settings' parameter cannot be null");
        SailfishURI dictionaryName = Objects.requireNonNull(this.settings.getDictionaryName(), "'Dictionary name' parameter");
        IDataManager dataManager = Objects.requireNonNull(this.serviceContext.getDataManager(), "'Data manager' parameter");
        FastTemplateLoader templateLoader = new FastTemplateLoader();
        this.registry = templateLoader.loadFastTemplates(dataManager, dictionaryName.getPluginAlias(), fastTemplate);
        this.converter = new FastToIMessageConverter(msgFactory, msgDictionary);
        this.decoder = new FastToIMessageDecoder(converter, this.settings.getSkipInitialByteAmount());
    }

	private TemplateRegistry getRegistry() {
		return registry;
	}

	protected Context createFastContext() {
		Context context = new FASTContext();

		context.setTemplateRegistry(getRegistry());
		context.setTraceEnabled(true);
		context.setDecodeTrace(new LoggingTrace());
		return context;
	}

	protected FASTCodecSettings getSettings() {
		return settings;
	}

	@Override
	protected boolean doDecode(
			IoSession session,
			IoBuffer in,
			ProtocolDecoderOutput out) throws Exception {
		if (!in.hasRemaining()) {
			return false;
		}
		int position = in.position();
		try {
			if (doRealDecode(session, in, out)) {
				return true;
			}
		} catch (Exception e) {
			logger.error("Can not decode message", e);
		}
		in.position(position);
		return false;
	}

	@Override
	public void decode(IoSession session, IoBuffer in,
			ProtocolDecoderOutput out) throws Exception {
		if (!session.getTransportMetadata().hasFragmentation()) {
            if(settings.isResetContextAfterEachUdpPacket()) {
                getInputContext(session).reset();
            }
		}
		super.decode(session, in, out);
	}

	private boolean doRealDecode(
			IoSession session,
			IoBuffer in,
			ProtocolDecoderOutput out) throws IOException, ConverterException {
        int startPosition = in.position();
        byte[] data = new byte[in.remaining()];
        in.get(data);
        DecodeResult decodeResult = decoder.decode(data, getInputContext(session), settings.isLengthPresent());
        boolean isSuccessDecoded = decodeResult.isSuccess();
        if (isSuccessDecoded) {
            IMessage decodedMessage = decodeResult.getDecodedMessage();
            fillMessageMetadata(session, in, decodedMessage);
            out.write(decodedMessage);
        }
        in.position(startPosition + decodeResult.getProcessedDataLength());
        return isSuccessDecoded;
	}

	private void fillMessageMetadata(
			IoSession session,
			IoBuffer in,
			IMessage imsg) {
        IMessageStructure msgStructure = msgDictionary.getMessages().get(imsg.getName());
        Boolean isAdmin = getAttributeValue(msgStructure, "IsAdmin");
		if (isAdmin == null) {
			isAdmin = false;
		}

		MsgMetaData metaData = imsg.getMetaData();
		metaData.setAdmin(isAdmin);
		//metaData.setToService(this.serviceName);

        String packetAddress = in instanceof IoBufferWithAddress ? ((IoBufferWithAddress)in).getAddress() : session.getRemoteAddress().toString();

        metaData.setFromService(packetAddress);
	}


	private Context getInputContext(IoSession session) {
		Context inputContext = (Context) session.getAttribute(INPUT_CONTEXT_ATTR_NAME);
		if (inputContext == null) {
			inputContext = createFastContext();
			session.setAttribute(INPUT_CONTEXT_ATTR_NAME, inputContext);
		}
		return inputContext;
	}

	private Context getOutputContext(IoSession session) {
		Context outputContext = (Context) session.getAttribute(OUTPUT_CONTEXT_ATTR_NAME);
		if (outputContext == null) {
			outputContext = createFastContext();
			session.setAttribute(OUTPUT_CONTEXT_ATTR_NAME, outputContext);
		}
		return outputContext;
	}

	@Override
	public void encode(
			IoSession session,
			Object message,
			ProtocolEncoderOutput out) throws Exception {

		IMessageToFastConverter converter = getIMessageToFastConverter();
		IMessage iMsg = (IMessage) message;

        Message fastMsg = converter.convert(iMsg);
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		Context oc = getOutputContext(session);
		MessageOutputStream msgOutStream = new MessageOutputStream(os, oc);
		msgOutStream.writeMessage(fastMsg);
		byte[] outputdata = os.toByteArray();
		byte[] blockHeader = TypeCodec.UINT.encode(new IntegerValue(outputdata.length));

		byte[] result = Arrays.copyOf(blockHeader, blockHeader.length + outputdata.length);
		System.arraycopy(outputdata, 0, result, blockHeader.length, outputdata.length);

		out.write(IoBuffer.wrap(result));
		if (settings.isResetContextAfterEachUdpPacket()) {
			oc.reset();
		}
	}

	private IMessageToFastConverter getIMessageToFastConverter() {
		if (iMsgToFastConverter == null) {
            IDictionaryStructure dictionary = msgDictionary;
			iMsgToFastConverter = new IMessageToFastConverter(dictionary, getRegistry());
		}
		return iMsgToFastConverter;
	}

}
